(ns repltile.ui.core
  (:require [repltile.core.state :as state]
            [repltile.ui.state.project :as project]
            [repltile.ui.state.repl-state :as repl-state]
            [repltile.ui.state.session :as session]
            [repltile.ui.state.history :as history]
            [repltile.ui.layout.theme :as theme]
            [repltile.ui.events.events :as events]
            [repltile.ui.layout.styles :as ui]
            [repltile.ui.components.input :as input]
            [repltile.ui.components.output :as output]
            [repltile.ui.components.search :as search]
            [repltile.ui.components.namespace :as namespace]))

(def last-executed-command (atom nil))

(defn update-input-placeholder! []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (let [placeholder-text (str "Enter Clojure code here...\n\n"
                                "Input Commands:\n"
                                " • Cmd+Enter: Evaluate command\n"
                                " • Cmd+↑: Previous command in history\n"
                                " • Cmd+↓: Next command in history\n\n")]
      (set! (.-placeholder input-el) placeholder-text))))

(defn update-queue-status-display! []
  (when (state/get-evaluating?)
    (input/set-input-enabled! false))) ; This will show queue status via get-queue-status-text

(defn focus-input! []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (.focus input-el)))

(defn update-dependency-button-ui!
  "Update dependency button UI based on status"
  [status summary can-start-repl]
  (try
    (.log js/console "🔧 Updating dependency button UI - Status:" status)
    (let [deps-btn (.getElementById js/document "resolve-deps-btn")
          deps-text (.getElementById js/document "deps-status-text")
          repl-btn (.getElementById js/document "repl-toggle-btn")]

      (when deps-btn
        ;; Remove all existing status classes
        (.remove (.-classList deps-btn) "deps-resolved" "deps-unresolved" "deps-resolving")

        ;; Add appropriate class and update text based on status
        (case status
          "resolved"
          (do
            (.add (.-classList deps-btn) "deps-resolved")
            (when deps-text (set! (.-innerText deps-text) "Ready"))
            (set! (.-title deps-btn) "✅ All dependencies resolved"))

          "unresolved"
          (do
            (.add (.-classList deps-btn) "deps-unresolved")
            (when deps-text (set! (.-innerText deps-text) "Resolve"))
            (set! (.-title deps-btn) (str "❌ " summary ". Click to resolve.")))

          "resolving"
          (do
            (.add (.-classList deps-btn) "deps-resolving")
            (when deps-text (set! (.-innerText deps-text) "..."))
            (set! (.-title deps-btn) "🔄 Resolving dependencies..."))

          ;; Unknown status
          (do
            (when deps-text (set! (.-innerText deps-text) "Deps"))
            (set! (.-title deps-btn) "⚠️ No dependency configurations detected"))))

      ;; Update REPL button state based on can-start-repl
      (when repl-btn
        (if can-start-repl
          (do
            (set! (.-disabled repl-btn) false)
            (.remove (.-classList repl-btn) "deps-disabled"))
          (do
            (set! (.-disabled repl-btn) true)
            (.add (.-classList repl-btn) "deps-disabled")
            (set! (.-title repl-btn) "Dependencies must be resolved before starting REPL")))))

    (catch :default e
      (.error js/console "Error updating dependency button UI:" e))))

(defn initialize-dependency-button!
  "Initialize dependency button with default state"
  []
  (try
    (.log js/console "🔧 Initializing dependency button...")
    (update-dependency-button-ui! "unknown" "Checking dependencies..." false)
    (catch :default e
      (.error js/console "Error initializing dependency button:" e))))

(defn handle-extension-message
  "Handle messages from the VS Code extension"
  [event]
  (try
    (let [message (.-data event)
          message-type (.-type message)]

      (case message-type

        "calva-status-update"
        (let [status (aget message "status")]
          (case status
            "starting"
            (do
              (repl-state/set-connection-state! :connecting)
              (output/add-to-output (str "🔄 " "Starting Calva REPL...") "repl-status"))
            "stopping"
            (do
              (repl-state/set-connection-state! :disconnected)
              (output/add-to-output (str "✅ " "Stopping Calva REPL...") "repl-status"))
            "connecting"
            (do
              (repl-state/set-connection-state! :connecting)
              (output/add-to-output (str "🔌 " "Connecting to REPL...") "repl-status"))
            "connected"
            (do
              (repl-state/set-connection-state! :connected)
              (output/add-to-output "{:REPL {:connection-status :connected}}" "repl-status"))))

        "repl-connected"
        (do
          (repl-state/set-connection-state! :connected)
          (output/add-to-output (str (aget message "message")) "repl-status")
          (namespace/update-namespace-display!))

        "repl-connection-error"
        (do
          (repl-state/set-connection-state! :disconnected)
          (let [error-msg (aget message "error")
                instructions (aget message "instructions")]
            (if (and instructions (not= instructions ""))
              (output/add-to-output (str "❌ REPL connection error: " error-msg "\n\n" instructions) "repl-error")
              (output/add-to-output (str "❌ REPL connection error: " error-msg) "repl-error"))))

        "status-update"
        (do
          ;; Load the complete REPL interface
          (ui/update-app-content (str (ui/create-repl-styles) (ui/create-repl-interface)))
          ;; Setup REPL functionality
          (js/setTimeout repl-state/setup-repl-functions 100)
          (js/setTimeout events/setup-input-handlers 200)
          ;; Focus on input
          (js/setTimeout #(when-let [input (.getElementById js/document "repl-input")]
                            (.focus input)) 300)
          (when-let [loading-el (.getElementById js/document "loading-msg")]
            (.remove loading-el)))

        "eval-result"
        (do
          (output/add-to-output (str "=> " (.-result message)) "repl-result")
          (namespace/update-namespace-display!))

        "eval-error"
        (output/add-to-output (str "❌ " (.-error message)) "repl-error")

        "repl-disconnected"
        (do
          (repl-state/set-connection-state! :disconnected)
          (let [stop-message (aget message "message")
                cancelled? (aget message "cancelled")]
            (if cancelled?
              (output/add-to-output (str "🚫 " (or stop-message "REPL startup was cancelled")) "repl-status")
              (output/add-to-output (or stop-message "REPLTILE has been disconnected - see you later alligator! ✌️") "repl-status"))))

        "stdout"
        (let [output (aget message "output")]
          (output/add-to-output output "repl-stdout"))

        "stderr"
        (let [output (aget message "output")]
          (output/add-to-output output "repl-stderr"))

        "evaluation-result"
        (let [result (aget message "result")
              result-data (aget result "result")
              as-text (aget result-data "as-text")
              ;; Clean UNREPL artifacts from display
              cleaned-text (output/clean-unrepl-display-text as-text)]
          ;; Use cleaned text for display
          (output/add-to-output (str "=> " (or cleaned-text as-text)) "repl-result")
          (namespace/update-namespace-display!))

        "namespace-changed"
        (let [new-namespace (aget message "namespace")]
          ;; Update the state in the webview
          (state/set-current-namespace! new-namespace)
          ;; Update the display
          (namespace/update-namespace-display!))

        "current-namespace-result"
        (when-let [ns-combo (.getElementById js/document "ns-combo")]
          (let [current-ns (aget message "namespace")]
            (set! (.-value ns-combo) (or current-ns "user"))))

        "clear-output-response"
        (do
          (output/clear-console)
          ;; Clear saved session output as well
          (session/clear-all-persistence!))

        "var-inspection-result"
        (let [command (aget message "command")
              doc (aget message "doc")
              ;; Build the command with doc comment if available
              final-command (if (and doc (not= doc ""))
                              (str ";; " doc "\n" command)
                              command)]
          (input/set-input-value! final-command)
          (focus-input!))

        "history-command"
        (let [command (aget message "command")]
          (input/set-input-value! command)
          (focus-input!))

        "evaluation-command"
        (let [code (aget message "code")]
          (reset! last-executed-command code))

        "evaluation-started"
        (let [eval-message (or (aget message "message") "⏳ Evaluating...")]
          (repl-state/set-controls-disabled! true eval-message))

        "evaluation-finished"
        (repl-state/set-controls-disabled! false)

        "execute-code"
        (let [code (aget message "code")]

          (if (.-replRunning js/window)
            (do
              (reset! last-executed-command code)

              (output/add-to-output (str (state/get-current-namespace) "=> " code) "repl-command")
              (when (.-vscode js/window)
                (.postMessage (.-vscode js/window) #js {:type "evaluate-code" :code code}))
              (js/setTimeout namespace/update-namespace-display! 1000))
            (output/add-to-output "❌ Cannot execute command: REPL is not connected. Please start the REPL first." "repl-error")))

        "clear-output"
        (do
          (output/clear-console)
          (session/clear-all-persistence!))

        "state-sync"
        (when (.-vscode js/window)
          (.postMessage (.-vscode js/window) #js {:type "state-sync-ack"}))

        "project-info"
        (let [project-path (aget message "projectPath")
              project-name (aget message "projectName")]
          (.log js/console  "[WEBVIEW-DEPS] project-info")
          (.log js/console (str "[WEBVIEW-DEPS] project-path: " project-path))
          (.log js/console (str "[WEBVIEW-DEPS] project-name: " project-name))

          (state/set-current-project-path! project-path)
          (when (project/set-project-identifier! project-path)
            (when-let [output-el (.getElementById js/document "repl-output")]
              (set! (.-innerHTML output-el) ""))
            (history/reset-history-for-project!)
            (session/load-session-state!)
            (history/initialize-history!)))

        "dependency-status-update"
        (do
          (.log js/console "[WEBVIEW-DEPS] Received dependency status update")
          (let [status (aget message "status")
                summary (aget message "summary")
                can-start-repl (aget message "can-start-repl")
                detected-configs (aget message "detected-configs")]
            (.log js/console (str "[WEBVIEW-DEPS] Status: " status ", Summary: " summary))
            (.log js/console (str "[WEBVIEW-DEPS] Can start REPL: " can-start-repl))
            (.log js/console (str "[WEBVIEW-DEPS] Detected configs: " detected-configs))
            (update-dependency-button-ui! status summary can-start-repl)))

        "dependency-resolution-complete"
        (do
          (.log js/console "[WEBVIEW-DEPS] Dependency resolution completed")
          (let [config-type (aget message "config-type")
                success (aget message "success")
                output (aget message "output")
                error (aget message "error")]
            (.log js/console (str "[WEBVIEW-DEPS] Config: " config-type ", Success: " success))
            (if success
              (do
                (output/add-to-output (str "✅ Dependencies resolved for " config-type) "repl-status")
                (when (and output (not= output ""))
                  (output/add-to-output output "repl-stdout")))
              (output/add-to-output (str "❌ Failed to resolve dependencies for " config-type ": " error) "repl-error"))))

        "history-file-loaded"
        (do
          (.log js/console "📜 History file loaded from extension")
          (let [commands (aget message "commands")]
            (.log js/console "📜 Received" (count commands) "commands from history file")
            (history/handle-history-file-loaded (array-seq commands))))

        "focus-input-response"
        (do
          (.log js/console "🎯 Focus input field requested")
          (focus-input!))

        "repeat-last-command-response"
        (do
          (.log js/console "🔄 Repeat last command requested")
          (let [history-commands (history/get-command-history)]
            (if (and (seq history-commands) (.-replRunning js/window))
              (let [last-command (last history-commands)]
                (.log js/console "🔄 Repeating last command:" last-command)
                (input/set-input-value! last-command)
                (events/evaluate-input))
              (.log js/console "❌ No command history available or REPL not connected"))))

        (.log js/console "[WEBVIEW] ❓ Unknown message type:" message-type "message:" message)))
    (catch :default e
      (.error js/console "[WEBVIEW] Error handling extension message:" e))))


(def search-state (atom {:active false
                         :current-term ""
                         :case-sensitive false
                         :matches []
                         :current-index -1}))


(defn setup-global-search-shortcut!
  []
  (try
    (.log js/console "🔍 Setting up global search shortcut...")
    (.addEventListener js/document "keydown"
                       (fn [event]
                         (let [key (.-key event)
                               ctrl-or-cmd (or (.-ctrlKey event) (.-metaKey event))]
                           (when (and (= key "f") ctrl-or-cmd)
                             (.preventDefault event)
                             (if (:active @search-state)
                               (when-let [search-input (.getElementById js/document "repl-search-input")]
                                 (.focus search-input)
                                 (.select search-input))
                               (search/show-search!))))))
    (.log js/console "✅ Global search shortcut setup complete")
    (catch :default e
      (.error js/console "Error setting up global search shortcut:" e))))

(defn ^:export init []
  (let [win js/window
        doc (.-document js/window)
        app-el (.getElementById doc "app")
        loading-el (.getElementById doc "loading-msg")
        vscode (or (.-vscode win)
                   (when (exists? (.-acquireVsCodeApi win))
                     (.acquireVsCodeApi win)))]
    (.log js/console "🚀 REPLTILE initialization starting...")
    (set! (.-vscode win) vscode)

    (.log js/console "📄 Mounting REPL interface HTML...")
    (set! (.-innerHTML app-el) (str (ui/create-repl-styles) (ui/create-repl-interface)))

    (.log js/console "🎨 Setting up theme detection...")
    (theme/setup-theme!)

    (.log js/console "🎨 Setting up syntax highlighting styles...")
    (output/add-syntax-highlighting-styles!)

    (.log js/console "🎨 Setting up simple input/output system...")

    (.log js/console "🔧 Initializing dependency system...")
    (when (.-vscode win)
      (.postMessage (.-vscode win) #js {:type "initialize-deps"}))

    (js/setTimeout initialize-dependency-button! 500)

    (let [play-btn (.getElementById doc "repl-toggle-btn")
          refresh-all-btn (.getElementById doc "refresh-all-btn")
          clear-btn (.getElementById doc "clear-output-btn")
          top-btn (.getElementById doc "scroll-top-btn")
          bottom-btn (.getElementById doc "scroll-bottom-btn")
          ns-combo (.getElementById doc "ns-combo")
          reload-ns-btn (.getElementById doc "reload-ns-btn")
          run-ns-tests-btn (.getElementById doc "run-ns-tests-btn")]

      (.log js/console "🔧 Setting up UI controls...")
      (when loading-el (set! (.-innerText loading-el) "Wiring up REPL controls..."))
      (set! (.-replRunning win) false)

      ;; Load session state and initialize history
      (when loading-el (set! (.-innerText loading-el) "Loading session..."))
      (history/initialize-history!)
      (session/load-session-state!)

      ;; Setup auto-save
      (session/setup-auto-save!)

      (.log js/console "⚡ Setting up input handlers...")
      (events/setup-input-handlers)
      (.log js/console "🔍 Setting up search functionality...")
      (search/setup-search-handlers!)
      (setup-global-search-shortcut!)
      (repl-state/set-connection-state! :disconnected)
      (update-input-placeholder!)

      (letfn [(set-repl-icon [running?]
                (.log js/console "🔄 Setting REPL running state to:" running?)
                (set! (.-replRunning win) running?)
                (if running?
                  (repl-state/set-connection-state! :connected)
                  (repl-state/set-connection-state! :disconnected)))]

        (when loading-el (set! (.-innerText loading-el) "Setting up controls..."))

        (.log js/console "🎮 Setting up Play/Stop REPL button...")
        (.addEventListener play-btn "click"
                           (fn [_]
                             (.log js/console "🎮 REPL toggle button clicked, running state:" (.-replRunning win))
                             (.log js/console "🔒 Button blocked?:" (state/is-repl-button-blocked?))

                             ;; Check if button is blocked
                             (if (state/is-repl-button-blocked?)
                               (do
                                 (.log js/console "⚠️ REPL button is blocked, ignoring click")
                                 (output/add-to-output "⚠️ Please wait 5 seconds between REPL operations" "repl-warning"))
                               ;; Proceed with REPL operation
                               (do
                                 ;; Block button for 5 seconds
                                 (state/block-repl-button-for-seconds! 5)

                                 ;; Visual feedback - disable button temporarily
                                 (set! (.-disabled play-btn) true)
                                 (js/setTimeout #(set! (.-disabled play-btn) false) 5000)

                                 (if (.-replRunning win)
                                   (do
                                     (.log js/console "🛑 Stopping REPL...")
                                     (set-repl-icon false)
                                     (.postMessage vscode #js {:type "stop-repl"}))
                                   (do
                                     (.log js/console "🚀 Starting REPL...")
                                     (repl-state/set-connection-state! :connecting)
                                     (.postMessage vscode #js {:type "start-repl"})))))))

        (.log js/console "🔧 Setting up Dependencies button...")
        (when-let [deps-btn (.getElementById doc "resolve-deps-btn")]
          (.addEventListener deps-btn "click"
                             (fn [_]
                               (.log js/console "🔧 Dependencies button clicked")
                               (.postMessage vscode #js {:type "resolve-dependencies"}))))

        (.log js/console "🔄 Setting up Refresh All button...")
        (.addEventListener refresh-all-btn "click"
                           (fn [_]
                             (.log js/console "🔄 Refresh All clicked")
                             (.postMessage vscode #js {:type "refresh-all"})))

        (.log js/console "🗑️ Setting up Clear Output button...")
        (.addEventListener clear-btn "click"
                           (fn [_]
                             (.log js/console "🗑️ Clear Output clicked")
                             (.postMessage vscode #js {:type "clear-output"})))

        (.log js/console "⬆️ Setting up Scroll Top button...")
        (.addEventListener top-btn "click"
                           (fn [_]
                             (.log js/console "⬆️ Scroll to top clicked")
                             (output/scroll-output-to-top!)))

        (.log js/console "⬇️ Setting up Scroll Bottom button...")
        (.addEventListener bottom-btn "click"
                           (fn [_]
                             (.log js/console "⬇️ Scroll to bottom clicked")
                             (output/scroll-output-to-bottom!)))

        (.log js/console "🔍 Setting up Search button...")
        (when-let [search-btn (.getElementById doc "search-output-btn")]
          (.addEventListener search-btn "click"
                             (fn [_]
                               (.log js/console "🔍 Search button clicked")
                               (if (:active @search-state)
                                 (when-let [search-input (.getElementById js/document "repl-search-input")]
                                   (.focus search-input)
                                   (.select search-input))
                                 (search/show-search!)))))

        (set! (.-tailing win) true)

        (.log js/console "🔄 Setting up Reload NS button...")
        (.addEventListener reload-ns-btn "click"
                           (fn [_]
                             (.log js/console "🔄 Reload NS clicked")
                             (let [ns-val (.-value ns-combo)]
                               (when (and ns-val (not= ns-val ""))
                                 (.postMessage vscode #js {:type "require-ns-reload" :ns ns-val})))))

        (.log js/console "🧪 Setting up Run NS Tests button...")
        (.addEventListener run-ns-tests-btn "click"
                           (fn [_]
                             (.log js/console "🧪 Run NS Tests clicked")
                             (let [ns-val (.-value ns-combo)]
                               (when (and ns-val (not= ns-val "") (not= ns-val "user"))
                                 (.postMessage vscode #js {:type "run-namespace-tests" :ns ns-val})))))

        (.log js/console "📐 Splitter will be set up by session management...")
        (session/initialize-session!)

        (.log js/console "📐 Setting up window resize handler...")
        (.addEventListener js/window "resize"
                           (fn [_]
                             (.log js/console "📐 Window resized")
                             nil))

        (.log js/console "✅ Removing loading indicator...")
        (when loading-el (.remove loading-el))
        (.log js/console "🎉 REPLTILE initialization complete!")))))

(.addEventListener js/window "message" handle-extension-message)

(when-not (.-repltile js/window)
  (set! (.-repltile js/window) #js {}))
(when-not (.-webview (.-repltile js/window))
  (set! (.-webview (.-repltile js/window)) #js {}))
(set! (.-init (.-webview (.-repltile js/window))) init)

(defn try-init-loading []
  (let [doc (.-document js/window)
        app-el (.getElementById doc "app")
        loading-el (.getElementById doc "loading-msg")]
    (if (and doc app-el)
      (do
        (when loading-el (set! (.-innerText loading-el) "Mounting REPLTILE interface..."))
        (init))
      (do
        (when loading-el (set! (.-innerText loading-el) "Waiting for DOM..."))
        (js/setTimeout try-init-loading 100)))))

(when-not (.-repltile-initialized js/window)
  (set! (.-repltile-initialized js/window) true)
  (try-init-loading))

(defn setup-queue-display-updates!
  "Setup automatic queue display updates"
  []
  (state/set-queue-display-update-fn! update-queue-status-display!)
  (.log js/console "🔧 Queue display updates configured"))


(defn main []
  (.log js/console "🚀 REPLTILE WebView Starting...")

  (state/init!)

  (setup-queue-display-updates!)

  (.addEventListener js/window "message" handle-extension-message)

  (when (.-vscode js/window)
    (.postMessage (.-vscode js/window) #js {:type "webview-ready"}))

  (project/initialize-project!)

  (.log js/console "✅ REPLTILE WebView initialized"))

(main)