(ns repltile.vscode.commands
  "Command dispatching and handling for REPLTILE"
  (:require [repltile.core.state :as state]
            [repltile.vscode.calva :as calva]
            [repltile.repl.engine :as rt]
            [repltile.vscode.extension :as vscode]
            [repltile.core.logger :as log]
            [repltile.repl.deps :as deps]
            [clojure.string :as str]))

;; Helper functions for command status (moved to top)

(defn can-evaluate?
  "Check if we can evaluate code (repl-tooling connected) - always accept commands for queueing"
  []
  (rt/connected?))

(defn can-execute-immediately?
  "Check if we can execute command immediately (not currently evaluating)"
  []
  (and (rt/connected?)
       (not (state/get-evaluating?))))

(defn execute-command-immediately! [command-info]
  (let [{:keys [handler message description]} command-info]
    (log/info (str "⚡ Executing immediately: " description))
    ;; Set current command and evaluating state
    (state/set-current-command! command-info)
    (state/set-evaluating! true)
    (rt/start-evaluation-timeouts!)
    ;; Execute the actual command
    (handler message)))

(defn queue-or-execute-command! [command-info]
  (if (can-execute-immediately?)
    (execute-command-immediately! command-info)
    (do
      (log/info (str "📋 Queueing command: " (:description command-info)))
      (state/add-command-to-queue! command-info))))

(defn process-next-queued-command! []
  (when-let [next-command (state/get-next-queued-command!)]
    (log/info (str "▶️ Processing next queued command: " (:description next-command)))
    (execute-command-immediately! next-command)))

(defn can-start-repl?
  []
  true)

(defn can-stop-repl?
  []
  true)

;; Command availability status
(defn get-command-availability
  []
  {:start-repl (can-start-repl?)
   :stop-repl (can-stop-repl?)
   :evaluate-code (can-evaluate?)
   :evaluate-top-block (can-evaluate?)
   :evaluate-block (can-evaluate?)
   :load-file (can-evaluate?)
   :run-namespace-tests (can-evaluate?)})

;; REPL Control Commands

(defn handle-start-repl
  [_]
  (log/info "🚀 Chamando calva/start-repl! ...")
  (calva/start-repl!))

(defn handle-stop-repl
  [_]
  (log/info "🛑 Chamando calva/stop-repl! ...")
  (calva/stop-repl!))

(defn handle-connect-repl-tooling
  [message]
  (let [host (:host message "localhost")
        port (:port message)]
    (log/info (str "🔌 Connecting repl-tooling to " host ":" port))
    (rt/connect-to-nrepl! host port)))

(defn handle-repl-ready
  [message]
  (let [port (:port message)
        msg (:message message)]
    (log/info (str "✅ REPL ready: " msg))
    (vscode/send-webview-message! {:type :repl-connected
                                   :message msg
                                   :port port})))

(defn handle-repl-start-error
  [message]
  (let [error (:error message)]
    (log/error (str "❌ REPL start error: " error))
    (vscode/send-webview-message! {:type :repl-connection-error
                                   :error error})))

(defn handle-repl-connection-error
  [message]
  (let [error (:error message)]
    (log/error (str "❌ REPL connection error: " error))
    ;; Forward the complete error message to webview for user display
    (vscode/send-webview-message! (merge {:type :repl-connection-error}
                                         (select-keys message [:error :instructions :fullMessage])))))

;; REMOVED: handle-eval-code - all evaluation now goes directly through repl-tooling

(defn handle-jackin-success
  [message]
  (let [port (get message :port)]
    (log/info (str "✅ Jack-in realizado, conectar repl-tooling na porta " port))
    (calva/connect-repl-tooling-to-port! port)))

(defn handle-jackin-stopped
  [message]
  (let [cancelled? (get message :cancelled false)
        stop-message (get message :message "Jack-in stopped successfully")]
    (if cancelled?
      (log/info "🚫 Jack-in startup cancelled: " stop-message)
      (log/info "🛑 Jack-in stopped successfully: " stop-message))
    (rt/disconnect!)
    (vscode/send-webview-message! {:type :repl-disconnected
                                   :message stop-message
                                   :cancelled cancelled?})))

(defn handle-eval-result
  [message]
  (let [result (get message :result)
        code (get message :code)]
    (log/info (str "✅ Resultado da avaliação: " result))
    (vscode/send-webview-message! {:type :eval-result :code code :result result})))

(defn handle-eval-error
  [message]
  (let [error (get message :error)
        code (get message :code)]
    (log/error (str "❌ Erro na avaliação: " error))
    (vscode/send-webview-message! {:type :eval-error :code code :error error})))

(defn handle-setup-tap-listener
  [message]
  (let [port (:port message)]
    (log/info (str "🎯 Setting up tap> listener on port: " port))
    ;; TODO: Implement tap> listener setup
    (vscode/send-webview-message! {:type :tap-listener-ready
                                   :port port})))

(defn execute-evaluate-code [message]
  (let [code (:code message)
        namespace (:namespace message)]
    (log/info (str "📝 Executing code: " namespace "/" code))
    (vscode/send-webview-message! {:type :evaluation-started
                                   :message (str "⏳ Evaluating: " code)})
    (rt/evaluate-code! code namespace)))

(defn execute-evaluate-top-block [_message]
  (log/info "📝 Executing top-block via repl-tooling")
  (vscode/send-webview-message! {:type :evaluation-started
                                 :message "⏳ Evaluating top-level form..."})
  (rt/evaluate-top-block!))

(defn execute-evaluate-block [_message]
  (log/info "📝 Executing block via repl-tooling")
  (vscode/send-webview-message! {:type :evaluation-started
                                 :message "⏳ Evaluating current form..."})
  (rt/evaluate-block!))

(defn execute-load-file [message]
  (let [filename (:filename message "current-file.clj")]
    (log/info (str "📂 Loading file: " filename))
    (vscode/send-webview-message! {:type :evaluation-started
                                   :message (str "⏳ Loading file: " filename)})
    (rt/load-file! filename)))

(defn execute-require-ns-reload [message]
  (let [ns (:ns message)]
    (log/info (str "🔄 Require NS with reload: " ns))
    (vscode/send-webview-message! {:type :evaluation-started
                                   :message (str "⏳ Reloading namespace: " ns)})
    (rt/evaluate-code! (str "(require '" ns " :reload)") "user" :internal? false)))

(defn execute-refresh-all [_message]
  (log/info "🔄 Refresh-all: reloading all changed namespaces")
  (vscode/send-webview-message! {:type :evaluation-started
                                 :message "⏳ Refreshing all namespaces..."})
  (rt/evaluate-code! "(do (require 'clojure.tools.namespace.repl) (clojure.tools.namespace.repl/clear) (clojure.tools.namespace.repl/refresh-all))" "user" :internal? false))

(defn execute-run-namespace-tests [message]
  (let [ns (:ns message (state/get-current-namespace))]
    (log/info (str "🧪 Running all tests for namespace: " ns))
    (vscode/send-webview-message! {:type :evaluation-started
                                   :message (str "⏳ Running tests for namespace: " ns "\n")})
    (rt/evaluate-code! (str "(do
                               (require 'clojure.test)
                               (try
                                 (if (find-ns '" ns ")
                                   (let [ns-obj (find-ns '" ns ")]
                                     (println \"🧪 Running tests for namespace:\" '" ns "')
                                     
                                     ;; Get all public vars in the namespace
                                     (let [all-vars (ns-publics ns-obj)
                                           
                                           ;; Find standard clojure.test tests (with :test metadata)
                                           standard-test-vars (filter #(:test (meta (second %))) all-vars)
                                           
                                                                                       ;; Find deftest by name pattern (additional safety check)
                                            deftest-vars (filter #(let [var-name (str (first %))
                                                                         var-meta (meta (second %))]
                                                                     (and (or (.startsWith var-name \"test-\")
                                                                              (.endsWith var-name \"-test\"))
                                                                          (not (:test var-meta))
                                                                          (fn? @(second %))))
                                                                  all-vars)
                                            
                                            ;; Additional check for any function that might be a test
                                            ;; Look for functions with 'test' in their name
                                            additional-test-vars (filter #(let [var-name (str (first %))
                                                                                var-meta (meta (second %))]
                                                                            (and (.contains var-name \"test\")
                                                                                 (not (:test var-meta))
                                                                                 (not (.startsWith var-name \"test-\"))
                                                                                 (not (.endsWith var-name \"-test\"))
                                                                                 (fn? @(second %))))
                                                                          all-vars)
                                           
                                           ;; Find defflow tests and other custom functions ending with -test
                                           flow-test-vars (filter #(let [var-name (str (first %))
                                                                          var-meta (meta (second %))]
                                                                      (and (.endsWith var-name \"-test\")
                                                                           (not (:test var-meta))
                                                                           (not (.startsWith var-name \"test-\"))
                                                                           (fn? @(second %))))
                                                                   all-vars)
                                           
                                                                                                                                   ;; Combine all clojure.test compatible tests
                                             all-test-vars (concat standard-test-vars deftest-vars additional-test-vars)]
                                         
                                         ;; Debug: Show found tests
                                         (println \"🔍 Found tests:\")
                                         (when (seq standard-test-vars)
                                           (println \"  ✅ Standard clojure.test tests (with :test metadata):\" (count standard-test-vars))
                                           (doseq [[test-name _] standard-test-vars]
                                             (println \"    -\" test-name)))
                                         (when (seq deftest-vars)
                                           (println \"  ✅ Pattern-based tests (test-*/*-test):\" (count deftest-vars))
                                           (doseq [[test-name _] deftest-vars]
                                             (println \"    -\" test-name)))
                                         (when (seq additional-test-vars)
                                           (println \"  ✅ Additional tests (containing 'test'):\" (count additional-test-vars))
                                           (doseq [[test-name _] additional-test-vars]
                                             (println \"    -\" test-name)))
                                         (when (seq flow-test-vars)
                                           (println \"  ✅ Custom/defflow tests:\" (count flow-test-vars))
                                           (doseq [[test-name _] flow-test-vars]
                                             (println \"    -\" test-name)))
                                         
                                                                                  ;; Run clojure.test compatible tests
                                         (when (seq all-test-vars)
                                           (println \"▶️ Running\" (count all-test-vars) \"clojure.test compatible tests...\")
                                           (if (seq standard-test-vars)
                                             ;; Use run-tests for standard tests (more comprehensive reporting)
                                             (do
                                               (println \"  📊 Running standard clojure.test tests with run-tests...\")
                                               (clojure.test/run-tests '" ns "))
                                             ;; Use test-vars for individual deftest functions found by name pattern
                                             (do
                                               (println \"  📊 Running pattern-based tests with test-vars...\")
                                               (clojure.test/test-vars (map second all-test-vars))))
                                           ;; Also run any additional tests found by pattern but not by metadata
                                           (let [non-standard-tests (concat deftest-vars additional-test-vars)]
                                             (when (and (seq non-standard-tests) (seq standard-test-vars))
                                               (println \"  📊 Running additional pattern-based tests...\")
                                               (clojure.test/test-vars (map second non-standard-tests)))))
                                       
                                       ;; Run defflow and other custom -test functions
                                       (when (seq flow-test-vars)
                                         (println \"▶️ Running\" (count flow-test-vars) \"defflow/custom tests...\")
                                         (doseq [[test-name test-var] flow-test-vars]
                                           (try
                                             (println \"  🧪 Running:\" test-name)
                                             (test-var)
                                             (println \"  ✅ Completed:\" test-name)
                                             (catch Exception e
                                               (println \"  ❌ Failed:\" test-name \"-\" (.getMessage e))))))
                                       
                                       ;; Summary
                                       (let [total-tests (+ (count all-test-vars) (count flow-test-vars))]
                                         (println \"✅ Test run completed for\" '" ns "' \"- Total:\" total-tests \"tests\"))))
                                   (do
                                     (println \"⚠️ Namespace\" '" ns "' \"not found. Running all available tests...\")
                                     (clojure.test/run-all-tests)))
                                 (catch Exception e
                                   (println \"❌ Error running tests:\" (.getMessage e))
                                   (println \"Stack trace:\" (with-out-str (clojure.stacktrace/print-stack-trace e))))))")
                       "user" :internal? false)))

;; Updated command handlers using the queue system

(defn handle-evaluate-code
  "Handle evaluate arbitrary code - uses queue system"
  [message]
  (if (can-evaluate?)
    (let [code (:code message)
          description (state/truncate-command-description code 30)]
      (queue-or-execute-command! {:handler execute-evaluate-code
                                  :message message
                                  :description description}))
    (log/warn "❌ Cannot evaluate - repl-tooling not connected")))

(defn handle-evaluate-top-block
  "Handle evaluate top-block - uses queue system"
  [message]
  (if (can-evaluate?)
    (queue-or-execute-command! {:handler execute-evaluate-top-block
                                :message message
                                :description "evaluate-top-block"})
    (log/warn "❌ Cannot evaluate top-block - repl-tooling not connected")))

(defn handle-evaluate-block
  "Handle evaluate block - uses queue system"
  [message]
  (if (can-evaluate?)
    (queue-or-execute-command! {:handler execute-evaluate-block
                                :message message
                                :description "evaluate-block"})
    (log/warn "❌ Cannot evaluate block - repl-tooling not connected")))

(defn handle-load-file
  "Handle load file - uses queue system"
  [message]
  (if (can-evaluate?)
    (let [filename (:filename message "current-file.clj")]
      (queue-or-execute-command! {:handler execute-load-file
                                  :message message
                                  :description (str "load-file: " filename)}))
    (log/warn "❌ Cannot load file - repl-tooling not connected")))

(defn handle-require-ns-reload
  "Handle namespace require with reload - uses queue system"
  [message]
  (if (can-evaluate?)
    (let [ns (:ns message)]
      (queue-or-execute-command! {:handler execute-require-ns-reload
                                  :message message
                                  :description (str "reload: " ns)}))
    (log/warn "❌ Cannot require namespace - repl-tooling not connected")))

(defn handle-refresh-all
  "Handle refresh-all command - uses queue system"
  [message]
  (if (can-evaluate?)
    (queue-or-execute-command! {:handler execute-refresh-all
                                :message message
                                :description "refresh-all"})
    (log/warn "❌ Cannot refresh-all - repl-tooling not connected")))

(defn handle-run-namespace-tests
  "Handle run all tests for current namespace - uses queue system"
  [message]
  (if (can-evaluate?)
    (let [ns (:ns message (state/get-current-namespace))]
      (queue-or-execute-command! {:handler execute-run-namespace-tests
                                  :message message
                                  :description (str "run-tests: " ns)}))
    (log/warn "❌ Cannot run tests - repl-tooling not connected")))

;; WebView Commands
(defn handle-webview-ready
  "Handle webview ready signal"
  [_]
  (log/webview :info "🌐 WebView ready signal received")
  (state/set-webview-ready! true)

  ;; Send current state to webview
  (vscode/send-webview-message! {:type :state-sync
                                 :state (state/get-state)}))

(defn handle-debug-connection
  "Handle debug connection info request"
  [_]
  (log/info "🔍 Debug connection info requested")
  (let [connection-info (rt/get-connection-info)
        available-commands (rt/get-available-commands)]
    (log/info "📊 Connection Info:" (pr-str connection-info))
    (log/info "📋 Available Commands:" (pr-str available-commands))
    (vscode/send-webview-message! {:type :debug-info
                                   :connection-info connection-info
                                   :available-commands available-commands})))

(defn handle-clear-output
  "Handle clear output command"
  [_]
  (log/info "🧹 Clearing output")
  (vscode/send-webview-message! {:type :clear-output-response}))

(defn handle-add-to-history
  "Handle adding command to history"
  [message]
  (let [command (:command message)]
    (log/info (str "📜 Adding to history: " command))
    (state/add-to-command-history! command)))

(defn handle-navigate-history
  "Handle navigating command history"
  [message]
  (let [direction (keyword (:direction message))
        command (state/navigate-command-history! direction)]
    (log/info (str "📜 Navigate history " direction ": " command))
    (when command
      (vscode/send-webview-message! {:type :history-command
                                     :command command}))))

(defn handle-get-current-namespace
  "Handle get current namespace from REPL directly using (name (ns-name *ns*))"
  [_]
  (if (can-evaluate?)
    (do
      (log/info "📦 Getting current namespace directly from REPL using (name (ns-name *ns*))")
      ;; Execute the command to get current namespace from REPL
      (rt/get-current-namespace))
    (log/warn "❌ Cannot get current namespace - repl-tooling not connected")))

(defn handle-completion-request
  "Handle autocomplete completion request"
  [message]
  (if (can-evaluate?)
    (let [prefix (:prefix message "")
          ns (:ns message "user")
          _options (:options message {})]
      (log/info (str "🎯 Processing completion request for prefix: '" prefix "' in namespace: '" ns "'"))
      ;; Use repl-tooling to get real completions
      (rt/get-completions! prefix ns))
    (do
      (log/warn "❌ Cannot get completions - repl-tooling not connected")
      (vscode/send-webview-message! {:type :completion-response
                                     :completions []}))))

;; Dependency Management Commands

(defn handle-initialize-deps
  "Handle initialization of dependency management system"
  [_]
  (log/info "🚀 [DEPS] Initializing dependency management system")
  (deps/initialize!))

(defn handle-check-dependencies
  "Handle check dependencies status"
  [_]
  (log/info "📋 [DEPS] Checking dependencies status")
  (deps/check-all-dependencies-resolved!))

(defn handle-resolve-dependencies
  "Handle resolve all dependencies"
  [message]
  (let [config-type (when-let [ct (:config-type message)]
                      (keyword ct))]
    (log/info (str "🔧 [DEPS] === RESOLVE COMMAND RECEIVED ==="))
    (log/info (str "🔧 [DEPS] Config type: " config-type))
    (log/info (str "🔧 [DEPS] Message: " (pr-str message)))
    (if config-type
      (deps/resolve-dependencies! config-type)
      (deps/resolve-dependencies!))))

(defn handle-toggle-auto-resolve
  "Handle toggle auto-resolve setting"
  [message]
  (let [enabled? (:enabled message)]
    (log/info (str "🔄 [DEPS] Toggling auto-resolve: " enabled?))
    (deps/set-auto-resolve! enabled?)))

;; Dependency callback handlers (from TypeScript)

(defn handle-config-detected
  "Handle config file detection response"
  [message]
  (log/info (str "📁 [DEPS] Config detected callback: " (pr-str message)))
  (deps/handle-config-detected! message))

(defn handle-dirs-checked
  "Handle directory existence check response"
  [message]
  (deps/handle-dirs-checked! message))

(defn handle-resolution-complete
  "Handle dependency resolution completion"
  [message]
  (log/info (str "🔧 [DEPS] Resolution complete callback: " (pr-str message)))
  (deps/handle-resolution-complete! message))

(defn handle-file-change
  "Handle dependency config file change"
  [message]
  (log/info (str "📁 [DEPS] File change callback: " (pr-str message)))
  (deps/handle-file-change! message))

(defn handle-dependency-status-update
  "Handle dependency status update from deps system"
  [message]
  (let [{:keys [status summary can-start-repl detected-configs resolved-status]} message]
    ;; Update main state
    (state/update-deps-state!
     {:deps-status status
      :deps-can-start-repl can-start-repl
      :deps-summary summary
      :deps-detected-configs detected-configs
      :deps-resolved-status resolved-status})))

(defn handle-state-sync-ack
  "Handle state sync acknowledgment - prevents unknown command warnings"
  [_message]
  ;; This is just an acknowledgment, no action needed
  ;; Log at debug level to avoid noise
  (log/debug "🔄 [STATE] State sync acknowledged"))

;; New interface utility commands

(defn handle-focus-input
  "Handle focus input field command - focuses the REPL input from anywhere"
  [_]
  (log/info "🎯 Focus input field requested")
  (vscode/send-webview-message! {:type :focus-input-response}))

(defn handle-repeat-last-command
  "Handle repeat last command - gets last command from history and executes it"
  [_]
  (if (can-evaluate?)
    (do
      (log/info "🔄 Repeat last command requested")
      (vscode/send-webview-message! {:type :repeat-last-command-response}))
    (log/warn "❌ Cannot repeat last command - repl-tooling not connected")))

;; Command dispatch map - AGORA DEFINIDO APÓS TODAS AS FUNÇÕES
(def command-handlers
  {:start-repl handle-start-repl
   :stop-repl handle-stop-repl
   :connect-repl-tooling handle-connect-repl-tooling
   :evaluate-code handle-evaluate-code
   :evaluate-top-block handle-evaluate-top-block
   :evaluate-block handle-evaluate-block
   :load-file handle-load-file
   :webview-ready handle-webview-ready
   :debug-connection handle-debug-connection
   :require-ns-reload handle-require-ns-reload
   :refresh-all handle-refresh-all
   :run-namespace-tests handle-run-namespace-tests
   :add-to-history handle-add-to-history
   :navigate-history handle-navigate-history
   :clear-output handle-clear-output
   :repl-ready handle-repl-ready
   :repl-start-error handle-repl-start-error
   :repl-connection-error handle-repl-connection-error
   :eval-result handle-eval-result
   :eval-error handle-eval-error
   :setup-tap-listener handle-setup-tap-listener
   :jackin-success handle-jackin-success
   :jackin-stopped handle-jackin-stopped
   :get-current-namespace handle-get-current-namespace
   :completion-request handle-completion-request
   ;; Dependency management commands
   :initialize-deps handle-initialize-deps
   :check-dependencies handle-check-dependencies
   :resolve-dependencies handle-resolve-dependencies
   :toggle-auto-resolve handle-toggle-auto-resolve
   ;; Dependency callback handlers
   :config-detected handle-config-detected
   :dirs-checked handle-dirs-checked
   :resolution-complete handle-resolution-complete
   :file-change handle-file-change
   :dependency-status-update handle-dependency-status-update
   ;; State sync handlers  
   :state-sync-ack handle-state-sync-ack
   ;; New interface utility commands
   :focus-input handle-focus-input
   :repeat-last-command handle-repeat-last-command})

;; Main command dispatcher

(defn dispatch-command!
  "Main command dispatcher - routes commands to appropriate handlers"
  [message]
  (let [command-type (keyword (:type message))
        handler (get command-handlers command-type)]
    ;; Only log for dependency-related commands
    (when (some #(str/includes? (name command-type) (name %)) [:deps :config :resolve :file-change])
      (log/info (str "🎯 [DEPS] Dispatching command: " command-type)))

    (if handler
      (try
        (handler message)
        (catch :default e
          (log/error (str "❌ Error handling command " command-type ": " e))))
      (log/warn (str "⚠️ Unknown command type: " command-type)))))