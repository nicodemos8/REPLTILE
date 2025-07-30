(ns repltile.repl.engine
  "Official repl-tooling integration - following documentation exactly"
  (:require [repl-tooling.editor-integration.connection :as conn]
            [repltile.core.state :as state]
            [repltile.core.logger :as log]
            [repltile.vscode.extension :as vscode]
            [clojure.string :as str]))

(declare get-current-namespace)

;; Connection state
(def connection-atom (atom nil))

;; Output debounce system to prevent fragmented messages
(def output-debounce-delay 100) ; 100ms debounce
(def stdout-buffer-atom (atom ""))
(def stderr-buffer-atom (atom ""))
(def stdout-debounce-timeout-atom (atom nil))
(def stderr-debounce-timeout-atom (atom nil))

(defn flush-stdout-buffer!
  "Flush accumulated stdout messages"
  []
  (when-let [buffer-content @stdout-buffer-atom]
    (when (seq buffer-content)
      (log/repl-tooling :debug (str "STDOUT (bufferedW): " buffer-content))
      (vscode/send-webview-message! {:type :stdout
                                     :output buffer-content})
      (state/add-evaluation! {:type :stdout
                              :content buffer-content
                              :timestamp (js/Date.now)})
      (reset! stdout-buffer-atom ""))))

(defn flush-stderr-buffer!
  "Flush accumulated stderr messages"
  []
  (when-let [buffer-content @stderr-buffer-atom]
    (when (seq buffer-content)
      (log/repl-tooling :debug (str "STDERR (buffered): " buffer-content))
      (vscode/send-webview-message! {:type :stderr
                                     :output buffer-content})
      (state/add-evaluation! {:type :stderr
                              :content buffer-content
                              :timestamp (js/Date.now)})
      (reset! stderr-buffer-atom ""))))

(defn setup-stdout-debounce!
  "Setup debounce for stdout messages"
  []
  ;; Clear any existing timeout
  (when-let [timeout @stdout-debounce-timeout-atom]
    (js/clearTimeout timeout)
    (reset! stdout-debounce-timeout-atom nil))

  ;; Setup new timeout
  (let [timeout (js/setTimeout
                 (fn []
                   (flush-stdout-buffer!)
                   (reset! stdout-debounce-timeout-atom nil))
                 output-debounce-delay)]
    (reset! stdout-debounce-timeout-atom timeout)))

(defn setup-stderr-debounce!
  "Setup debounce for stderr messages"
  []
  ;; Clear any existing timeout
  (when-let [timeout @stderr-debounce-timeout-atom]
    (js/clearTimeout timeout)
    (reset! stderr-debounce-timeout-atom nil))

  ;; Setup new timeout
  (let [timeout (js/setTimeout
                 (fn []
                   (flush-stderr-buffer!)
                   (reset! stderr-debounce-timeout-atom nil))
                 output-debounce-delay)]
    (reset! stderr-debounce-timeout-atom timeout)))

(defn clear-output-buffers!
  "Clear all output buffers and timeouts"
  []
  ;; Clear timeouts
  (when-let [timeout @stdout-debounce-timeout-atom]
    (js/clearTimeout timeout)
    (reset! stdout-debounce-timeout-atom nil))

  (when-let [timeout @stderr-debounce-timeout-atom]
    (js/clearTimeout timeout)
    (reset! stderr-debounce-timeout-atom nil))

  ;; Clear buffers
  (reset! stdout-buffer-atom "")
  (reset! stderr-buffer-atom ""))

;; Add evaluation completion hybrid system (debounce + max timeout)
(def evaluation-debounce-atom (atom nil))
(def evaluation-max-timeout-atom (atom nil))
(def eval-result-received-atom (atom false)) ; Track if eval result (main response) arrived
(def evaluation-debounce-delay 1000) ; 1 second - when messages stop coming
(def evaluation-max-timeout-delay (* 60000 10)) ; 10 minuts - absolute maximum

(defn clear-all-evaluation-timeouts!
  "Clear both debounce and max timeout"
  []
  (when-let [debounce @evaluation-debounce-atom]
    (js/clearTimeout debounce)
    (reset! evaluation-debounce-atom nil))
  (when-let [max-timeout @evaluation-max-timeout-atom]
    (js/clearTimeout max-timeout)
    (reset! evaluation-max-timeout-atom nil)))

(defn finish-evaluation!
  "Mark evaluation as complete and cleanup"
  []
  (log/repl-tooling :info "✅ Evaluation completed - re-enabling input")

  ;; Flush any remaining output buffers before finishing
  (flush-stdout-buffer!)
  (flush-stderr-buffer!)

  (state/set-evaluating! false)
  (state/clear-current-command!)
  (reset! eval-result-received-atom false) ; Reset for next evaluation
  (vscode/send-webview-message! {:type :evaluation-finished})
  (clear-all-evaluation-timeouts!)

  ;; Process next command in queue if any  
  (js/setTimeout
   (fn []
     (when-let [process-next-fn (resolve 'repltile.vscode.commands/process-next-queued-command!)]
       (process-next-fn)))
   100)) ; Small delay to ensure state is settled

(defn setup-evaluation-debounce!
  "Setup debounce timeout - only if eval result already received"
  []
  ;; Only start debounce if we already received the main eval result
  (when @eval-result-received-atom
    ;; Clear any existing debounce
    (when-let [debounce @evaluation-debounce-atom]
      (js/clearTimeout debounce))

    ;; Setup new debounce
    (let [debounce (js/setTimeout
                    (fn []
                      (log/repl-tooling :info "⏱️ No additional messages for 1s after eval result - evaluation complete")
                      (finish-evaluation!))
                    evaluation-debounce-delay)]
      (reset! evaluation-debounce-atom debounce))))

(defn start-post-eval-debounce!
  "Start debounce after receiving main eval result"
  []
  (log/repl-tooling :info "🎯 Main eval result received - starting 1s debounce for additional messages")
  (reset! eval-result-received-atom true)
  (setup-evaluation-debounce!))

(defn start-evaluation-timeouts!
  "Start maximum timeout system - debounce only starts after eval result"
  []
  (clear-all-evaluation-timeouts!)
  (clear-output-buffers!) ; Clear any previous output buffers
  (reset! eval-result-received-atom false) ; Reset eval result flag

  ;; Setup absolute maximum timeout (never renewed) - this is the safety net
  (let [max-timeout (js/setTimeout
                     (fn []
                       (log/repl-tooling :info "⏱️ Maximum timeout reached (45s) - forcing evaluation complete")
                       (finish-evaluation!))
                     evaluation-max-timeout-delay)]
    (reset! evaluation-max-timeout-atom max-timeout)
    (log/repl-tooling :info "⏳ Waiting for main eval result (max 45s)...")))

;; Dynamic editor state for repl-tooling
(def editor-state-atom (atom {:filename "current-file.clj"
                              :contents "(+ 1 2 3)"
                              :range [[0 0] [0 9]]
                              :namespace "user"}))

;; Connection getter function
(defn get-connection
  "Get current repl-tooling connection"
  []
  @connection-atom)

;; Editor data for repl-tooling
(defn get-current-editor-state
  "Provide current editor state to repl-tooling"
  []
  (let [state (state/get-state)
        editor-state @editor-state-atom]
    (merge editor-state
           {:namespace (:current-namespace state)})))

;; Function to update editor state dynamically
(defn set-current-editor-data!
  "Set current editor data for repl-tooling evaluation"
  [code & [namespace filename]]
  (let [current-ns (or namespace (get-current-namespace))
        current-filename (or filename "temp-eval.clj")
        code-lines (str/split-lines code)
        end-line (dec (count code-lines))
        end-col (count (last code-lines))]
    (reset! editor-state-atom
            {:filename current-filename
             :contents code
             :range [[0 0] [end-line end-col]]
             :namespace current-ns})))

;; Project configuration for repl-tooling
(defn get-project-config
  "Provide project configuration to repl-tooling"
  []
  {:project-paths ["."]
   :eval-mode :prefer-clj})

;; Callbacks for repl-tooling integration

(defn on-stdout-callback
  "Handle stdout from repl-tooling"
  [output]
  (log/repl-tooling :debug (str "STDOUT (raw): " output))

  ;; Only renew debounce if eval result already received (stdout is additional)
  (when @eval-result-received-atom
    (setup-evaluation-debounce!))

  ;; Add to buffer and setup debounce instead of sending immediately
  (swap! stdout-buffer-atom str output)
  (setup-stdout-debounce!))

(defn on-stderr-callback
  "Handle stderr from repl-tooling"
  [output]
  (log/repl-tooling :debug (str "STDERR (raw): " output))

  ;; Only renew debounce if eval result already received (stderr is additional)
  (when @eval-result-received-atom
    (setup-evaluation-debounce!))

  ;; Add to buffer and setup debounce instead of sending immediately
  (swap! stderr-buffer-atom str output)
  (setup-stderr-debounce!))

;; Add function to clean UNREPL bad-keyword issues
(defn clean-unrepl-result
  "Clean UNREPL bad-keyword artifacts from result string"
  [result-str]
  (when result-str
    (-> result-str
        ;; Convert #unrepl/bad-keyword [nil "keyword-name"] back to :keyword-name
        (str/replace #"#unrepl/bad-keyword \[nil \"([^\"]+)\"\]" ":$1")
        ;; Convert #unrepl/bad-symbol [nil "symbol-name"] back to symbol-name
        (str/replace #"#unrepl/bad-symbol \[nil \"([^\"]+)\"\]" "$1")
        ;; Remove #repl-tooling/literal-render and keep only the content inside quotes
        ;; This regex handles escaped quotes within the content
        (str/replace #"#repl-tooling/literal-render \"((?:[^\"\\]|\\.)*)\"" "$1")
        ;; Clean Java object representations - remove hex codes and keep only the useful part
        ;; Handle escaped quotes specifically
        (str/replace #"#object\[[^\s]+ 0x[a-fA-F0-9]+ \\\"([^\\\"]+)\\\"\]" "\"$1\"")
        ;; Handle regular quotes
        (str/replace #"#object\[[^\s]+ 0x[a-fA-F0-9]+ \"([^\"]+)\"\]" "\"$1\"")
        ;; Clean Java object representations without quotes
        (str/replace #"#object\[([^\s]+) 0x[a-fA-F0-9]+\]" "$1")
        ;; Remove escaped quotes - convert \" back to "
        (str/replace #"\\\"" "\""))))

(defn on-eval-callback
  "Handle evaluation results from repl-tooling"
  [result]
  (log/repl-tooling :info (str "EVAL RESULT: " (pr-str result)))

  ;; This is the main eval result - start debounce for additional messages
  (if-not @eval-result-received-atom
    (start-post-eval-debounce!)
    ;; If somehow we get multiple eval results, renew debounce
    (setup-evaluation-debounce!))

  ;; Handle namespace changes from in-ns command
  (let [form-str (str (:form result))
        result-data (:result result)
        result-str (str (:result result-data))
        as-text-str (str (:as-text result-data))
        editor-data (:editor-data result)
        editor-contents (str (:contents editor-data))]


    (when (or (str/includes? form-str "in-ns")
              (str/includes? editor-contents "in-ns"))

      (let [;; Try to match namespace from result first (standard case)
            ns-match-1 (re-find #"#namespace\[([^\]]+)\]" result-str)
            ns-match-2 (re-find #"#namespace\[([^\]]+)\]" as-text-str)
            ns-match-3 (re-find #"#repl-tooling/literal-render \"#namespace\[([^\]]+)\]\"" as-text-str)
            result-ns-match (or ns-match-1 ns-match-2 ns-match-3)

            ;; If result doesn't contain namespace info, extract from command itself
            ;; This handles cases where (in-ns 'namespace) returns nil
            command-ns-match (when (not result-ns-match)
                               (let [code-to-check (if (and (or (empty? form-str) (= form-str "nil"))
                                                            (seq editor-contents))
                                                     editor-contents
                                                     form-str)]
                                 (log/repl-tooling :debug (str "🔍 [namespace-detection] Extracting from code: " code-to-check))
                                 (or (re-find #"in-ns\s+'([^)]+)" code-to-check)
                                     (re-find #"in-ns\s+\'\s*([^)]+)" code-to-check)
                                     (re-find #"in-ns\s+\(\s*quote\s+([^)]+)" code-to-check))))

            final-ns-match (or result-ns-match command-ns-match)]

        (log/repl-tooling :debug (str "🔍 [namespace-detection] result-ns-match: " result-ns-match))
        (log/repl-tooling :debug (str "🔍 [namespace-detection] command-ns-match: " command-ns-match))
        (log/repl-tooling :debug (str "🔍 [namespace-detection] final-ns-match: " final-ns-match))

        (when (and final-ns-match (second final-ns-match))
          (let [namespace (second final-ns-match)]
            (log/repl-tooling :info (str "📦 Namespace switched to: " namespace))
            (state/set-current-namespace! namespace)
            ;; Notify webview of namespace change
            (vscode/send-webview-message! {:type :namespace-changed
                                           :namespace namespace}))))))


  ;; Only send evaluation-result if it's not an internal operation
  (let [form-str (str (:form result))
        editor-data (:editor-data result)
        editor-contents (str (:contents editor-data))
        ;; Use editor-contents if form-str is empty
        code-to-check (if (and (or (empty? form-str) (= form-str "nil"))
                               (seq editor-contents))
                        editor-contents
                        form-str)
        is-internal-op? (or (str/includes? code-to-check "ns-publics")
                            (str/includes? code-to-check "var-sym")
                            (str/includes? code-to-check "compliment")
                            (str/includes? code-to-check "resolve '")
                            (str/includes? code-to-check "var-name")
                            (str/includes? code-to-check "var-meta")
                            (str/includes? code-to-check ":arglists")
                            (str/includes? code-to-check ":type")
                            (str/includes? code-to-check ":namespace")
                            (str/includes? code-to-check ":doc")
                            (str/includes? code-to-check "var-get")
                            ;; Only consider find-ns internal if used for namespace completion or introspection
                            (and (str/includes? code-to-check "find-ns")
                                 (not (str/includes? code-to-check "(nil? (find-ns"))
                                 (not (str/includes? code-to-check "(do")))
                            (str/includes? code-to-check "(name (ns-name *ns*))")
                            (str/includes? code-to-check "completion/completions"))]
    ;; Handle completion results (internal operation)
    (when (str/includes? code-to-check "completion/completions")
      (log/repl-tooling :info "📝 Completions received from nREPL native completion")
      (let [result-data (:result result)
            completion-result (:result result-data)]
        (log/repl-tooling :info (str "🔍 DEBUG result-data: " (pr-str result-data)))
        (log/repl-tooling :info (str "🔍 DEBUG completion-result: " (pr-str completion-result)))
        (log/repl-tooling :info (str "🔍 DEBUG completion-result type: " (type completion-result)))
        (log/repl-tooling :info (str "🔍 DEBUG is vector?: " (vector? completion-result)))
        (log/repl-tooling :info (str "🔍 DEBUG is sequential?: " (sequential? completion-result)))

        (let [completions-data (if completion-result
                                 ;; Result already comes structured from repl-tooling
                                 (if (sequential? completion-result)  ; Use sequential? instead of vector?
                                   ;; nREPL completion format: [{:candidate "map", :type :function, :ns "clojure.core", ...}]
                                   (map (fn [item]
                                          (if (map? item)
                                            ;; Use the item as-is, it's already in the correct format
                                            item
                                            ;; Fallback for simple strings
                                            {:candidate (str item) :type "function"}))
                                        completion-result)
                                   [])
                                 [])]
          (log/repl-tooling :info (str "📝 Parsed completions: " (count completions-data) " items"))
          (log/repl-tooling :info (str "📝 First completion item: " (first completions-data)))
          (state/set-completions! completions-data)
          (vscode/send-webview-message! {:type :completion-response
                                         :completions completions-data}))))

    (when-not is-internal-op?
      ;; Create cleaned result with fixed as-text
      (let [cleaned-result (if-let [result-data (:result result)]
                             (if-let [as-text (:as-text result-data)]
                               (assoc-in result [:result :as-text] (clean-unrepl-result as-text))
                               result)
                             result)]
        (vscode/send-webview-message! {:type :evaluation-result
                                       :result cleaned-result}))))
  (state/add-evaluation! {:type :evaluation
                          :result result
                          :timestamp (js/Date.now)})

  ;; Handle specific evaluation types
  (let [result-value (:result result)
        form-str (str (:form result))]

    ;; DEBUG: Log detailed result structure for namespace vars detection
    (when (and result-value (:result result-value) (vector? (:result result-value)))
      (log/repl-tooling :info (str "🔧 DEBUG: Vector result detected - " (pr-str (:result result-value))))
      (log/repl-tooling :info (str "🔧 DEBUG: Form content check for ns-publics: " (str/includes? form-str "ns-publics"))))))

(defn on-notify-callback
  "Handle notifications from repl-tooling"
  [notification]
  (log/repl-tooling :info (str "NOTIFICATION: " (pr-str notification)))

  ;; Filter out temporary connection errors that might resolve themselves
  (let [message (:message notification)
        is-temp-connection-error? (and (= (:type notification) :error)
                                       (or (str/includes? message "Unknown error while connecting to the REPL:")
                                           (str/includes? message "Connection refused")))]

    ;; Only show non-temporary errors or if repl-tooling status is not connecting
    (when (or (not is-temp-connection-error?)
              (not= (state/get-repl-tooling-status) :connecting))
      (vscode/show-notification! (:message notification)
                                 (case (:type notification)
                                   :info :info
                                   :warn :warn
                                   :error :error
                                   :info)))

    ;; Always log for debugging purposes
    (when is-temp-connection-error?
      (log/repl-tooling :debug (str "🔄 Temporary connection error (filtered): " message)))))


;; Command functions using repl-tooling
(defn evaluate-code!
  "Evaluate arbitrary code using repl-tooling"
  [code namespace & {:keys [internal?] :or {internal? false}}]
  (log/repl-tooling :info (str "🔍 DEBUG: code = " code))
  (log/repl-tooling :info (str "🔍 DEBUG: namespace = " namespace))
  (let [ns-exec (cond
                  (str/starts-with? code "in-ns") "user"
                  (str/starts-with? code "(in-ns") "user"
                  (str/starts-with? code "require") "user"
                  (str/starts-with? code "(require") "user"
                  (str/starts-with? code "use") "user"
                  (str/starts-with? code "(use") "user"
                  (str/starts-with? code "import") "user"
                  (str/starts-with? code "(import") "user"
                  :else (or namespace (get-current-namespace)))]
    (when-let [connection (get-connection)]
      (when-let [connection-deref @connection]
        (when-let [commands (:editor/commands connection-deref)]

          ;; Show the command being evaluated in the output only if not internal

          (let [display-namespace ns-exec]
            (if internal?
              (log/repl-tooling :info
                                (str "[internal command] 
                                                :namespace " display-namespace " 
                                                :code " code))
              (do
                (log/repl-tooling :info (str "[external command] 
                                                        :namespace " display-namespace " 
                                                        :code " code))
                (vscode/send-webview-message! {:type :evaluation-command
                                               :namespace display-namespace
                                               :code code}))))

          ;; Update editor state with the code to be evaluated
          (set-current-editor-data! code ns-exec)
          (log/repl-tooling :info (str "📝 [evaluate-code!] current-editor-data: " (get-current-editor-state)))

          ;; Then evaluate - repl-tooling will use get-current-editor-state to get the code
          (when-let [eval-fn (:evaluate-selection commands)]
            (log/repl-tooling :info (str "📝 [evaluate-code!] eval-fn: " (pr-str (:command eval-fn))))
            ((:command eval-fn))))))))

(defn evaluate-top-block!
  "Evaluate top-block using repl-tooling"
  [& [code namespace]]
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (log/repl-tooling :info "📝 Evaluating top-block")
        ;; If code is provided, set editor data
        (when code
          (set-current-editor-data! code namespace))
        (when-let [eval-fn (:evaluate-top-block commands)]
          ((:command eval-fn)))))))

(defn evaluate-block!
  "Evaluate current block using repl-tooling"
  [& [code namespace]]
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (log/repl-tooling :info "📝 Evaluating block")
        ;; If code is provided, set editor data
        (when code
          (set-current-editor-data! code namespace))
        (when-let [eval-fn (:evaluate-block commands)]
          ((:command eval-fn)))))))

(defn get-current-namespace []
  (state/get-current-namespace))

(defn get-completions!
  "Get completions from nREPL using native completion functions"
  [prefix namespace]
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (log/repl-tooling :info (str "🎯 Getting completions for prefix: '" prefix "' in namespace: '" namespace "'"))

        ;; Use nREPL's native completion functions - this will trigger on-eval-callback
        (let [completion-code (str "(do "
                                   "  (require '[nrepl.util.completion :as completion]) "
                                   "  (completion/completions \"" prefix "\" (symbol \"" namespace "\") {:extra-metadata #{:doc :arglists :ns :type}}))")]

          ;; Set editor data for the completion request
          (set-current-editor-data! completion-code namespace "completion-request.clj")

          ;; Execute completion request
          (when-let [eval-fn (:evaluate-selection commands)]
            (log/repl-tooling :info "📝 Executing completion request")
            (state/set-auto-complete-command! true)
            ((:command eval-fn))))))))

(defn load-file!
  "Load file using repl-tooling"
  [filename]
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (log/repl-tooling :info (str "📂 Loading file: " filename))
        (when-let [load-fn (:load-file commands)]
          ((:command load-fn)))))))

(defn connect-to-nrepl!
  "Connect to nREPL using official repl-tooling library"
  [host port]
  (log/repl-tooling :info (str "🔌 Connecting to nREPL: " host ":" port))
  (state/set-repl-tooling-status! :connecting)

  ;; Use repl-tooling API - conn/connect! works for both Socket REPL and nREPL
  (let [connection-promise
        (conn/connect! host port
                       {:on-stdout on-stdout-callback
                        :on-stderr on-stderr-callback
                        :on-eval on-eval-callback
                        :editor-data get-current-editor-state
                        :get-config get-project-config
                        :notify on-notify-callback})]

    ;; Handle connection promise
    (.then connection-promise
           (fn [repl-connection]
             (log/repl-tooling :info "✅ repl-tooling connected to nREPL successfully!")
             (when repl-connection
               (reset! connection-atom repl-connection)
               ;; Get available commands from repl-tooling - with null check
               (when-let [connection-deref @repl-connection]
                 (when-let [commands (:editor/commands connection-deref)]
                   (state/set-current-namespace! "user")

                   (log/repl-tooling :info (str "📋 Available commands: " (keys commands)))
                   (vscode/send-webview-message! {:type :repl-connected
                                                  :message "REPLTILE is connected — go ahead and smile crocodile!"})
                   (vscode/send-to-vscode! {:type :repl-tooling-ready
                                            :commands (keys commands)})
                   (state/set-repl-tooling-status! :connected)
                   (state/set-repl-connection! repl-connection))))

             ;; Return connection for chaining
             repl-connection)

           (fn [error]
             (log/repl-tooling :error (str "❌ nREPL Connection failed: " error))
             (state/set-repl-tooling-status! :error)

             ;; Provide more detailed error information
             (let [error-msg (cond
                               (str/includes? (str error) "Connection refused")
                               "nREPL connection refused - make sure Calva REPL is running on the specified port"

                               (str/includes? (str error) "timeout")
                               "nREPL connection timeout - the REPL might be starting up"

                               (empty? (str error))
                               "Empty nREPL error - this might be a temporary connection issue"

                               :else
                               (str "Unexpected nREPL error: " error))]

               (log/repl-tooling :error (str "📋 Detailed nREPL error: " error-msg))
               (vscode/show-notification! (str "Failed to connect to nREPL: " error-msg) :error)
               (vscode/send-webview-message! {:type :repl-connection-error
                                              :error error-msg}))))))

(defn disconnect!
  "Disconnect from repl-tooling"
  []
  (when-let [connection (get-connection)]
    (log/repl-tooling :info "🔌 Disconnecting from repl-tooling")
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (when-let [disconnect-fn (:disconnect commands)]
          ((:command disconnect-fn)))))
    (reset! connection-atom nil)
    (state/set-repl-tooling-status! :disconnected)
    (state/set-repl-connection! nil)))

;; Status checks
(defn connected?
  "Check if repl-tooling is connected"
  []
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [_commands (:editor/commands connection-deref)]
        (= (state/get-repl-tooling-status) :connected)))))

(defn get-connection-info
  "Get detailed information about current repl-tooling connection"
  []
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      {:connected? true
       :commands (keys (:editor/commands connection-deref))
       :connection-type (type connection-deref)
       :connection-state (select-keys connection-deref [:host :port :impl-type])})))

(defn get-available-commands
  "Get list of available repl-tooling commands"
  []
  (when-let [connection (get-connection)]
    (when-let [connection-deref @connection]
      (when-let [commands (:editor/commands connection-deref)]
        (keys commands)))))