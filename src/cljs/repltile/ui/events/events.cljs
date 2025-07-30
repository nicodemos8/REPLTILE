(ns repltile.ui.events.events
  "Event handling for REPLTILE webview"
  (:require [clojure.string :as str]
            [repltile.ui.components.input :as input]
            [repltile.ui.state.history :as history]
            [repltile.ui.components.brackets :as brackets]
            [repltile.ui.components.search :as search]
            [repltile.ui.state.repl-state :as repl-state]
            [repltile.ui.components.output :as output]
            [repltile.core.state :as state]))

;; Track last executed command for nu/ detection  
(def last-executed-command (atom nil))

;; Forward declarations for autocomplete functions
(declare trigger-autocomplete!)
(declare navigate-autocomplete)
(declare apply-autocomplete-suggestion)
(declare hide-autocomplete!)

;; Autocomplete state management
(def autocomplete-state (atom {:active false
                               :suggestions []
                               :current-index -1
                               :current-prefix ""
                               :cursor-pos 0
                               :input-element nil
                               :completion-callback nil}))

(defn evaluate-input
  "Evaluate the input from the REPL"
  []
  (try
    (.log js/console "📝 Evaluating input...")
    (let [code (input/get-input-value)]
      (if (and code (not= (str/trim code) ""))
        (do
          (.log js/console "📝 Code to evaluate:" code)
          ;; Add to command history before evaluating
          (history/add-to-command-history! code)

          ;; Store the last executed command for nu/ detection
          (reset! last-executed-command code)

          ;; Show command in output before evaluation (like Cursive REPL)
          (let [current-ns (state/get-current-namespace)]
            (output/add-to-output (str (or current-ns "user") "=> " code) "input"))

          ;; Clear input
          (input/set-input-value! "")

          ;; Send directly to repl-tooling for evaluation
          (when (.-vscode js/window)
            (.postMessage (.-vscode js/window)
                          #js {:type "evaluate-code" :code code})))
        (.log js/console "⚠️ No code to evaluate or empty input")))
    (catch :default e
      (.error js/console "Error evaluating input:" e))))

(defn setup-input-handlers
  "Setup keyboard handlers for REPL input"
  []
  (try
    (when-let [input-element (.getElementById js/document "repl-input")]
      ;; Track input changes for history reset behavior - add this before keydown handler
      (.addEventListener input-element "input"
                         (fn [event]
                           (let [current-value (.-value (.-target event))]
                             (.log js/console "📝 Input changed, checking history reset...")
                             (history/check-input-change! current-value))))
      ;; Main keydown handler for evaluation, history, and auto-closing brackets
      (.addEventListener input-element "keydown"
                         (fn [event]
                           (let [key (.-key event)
                                 ctrl-or-cmd (or (.-ctrlKey event) (.-metaKey event))
                                 autocomplete-active? (:active @autocomplete-state)]
                             ;; Debug logging for all key events when autocomplete is active
                             (when autocomplete-active?
                               (.log js/console "🎹 Key in autocomplete:"
                                     "key=" key
                                     "autocomplete-active=" autocomplete-active?))
                             ;; Debug logging for autocomplete trigger key only
                             (when (and (= key " ") (.-ctrlKey event))
                               (.log js/console "🎹 Key event:"
                                     "key=" key
                                     "ctrl=" (.-ctrlKey event)
                                     "meta=" (.-metaKey event)))
                             (cond
                               ;; PRIORITY 1: Autocomplete handlers (highest priority when active)

                               ;; Enter in autocomplete = apply suggestion
                               (and (= key "Enter") autocomplete-active? (not ctrl-or-cmd) (not (.-shiftKey event)))
                               (do
                                 (.log js/console "🎯 Enter in autocomplete detected!")
                                 (.preventDefault event)
                                 (let [state @autocomplete-state
                                       suggestions (:suggestions state)
                                       current-index (:current-index state)]
                                   (.log js/console "🎯 Enter pressed in autocomplete - current-index:" current-index "suggestions count:" (count suggestions))
                                   (when (and (seq? suggestions)
                                              (>= current-index 0)
                                              (< current-index (count suggestions)))
                                     (let [suggestion (nth suggestions current-index)]
                                       (.log js/console "🎯 Applying suggestion from Enter:" suggestion)
                                       (apply-autocomplete-suggestion suggestion)))))

                               ;; Up arrow in autocomplete = navigate up
                               (and (= key "ArrowUp") autocomplete-active?)
                               (do
                                 (.preventDefault event)
                                 (navigate-autocomplete :up))

                               ;; Down arrow in autocomplete = navigate down
                               (and (= key "ArrowDown") autocomplete-active?)
                               (do
                                 (.preventDefault event)
                                 (navigate-autocomplete :down))

                               ;; Escape in autocomplete = hide autocomplete
                               (and (= key "Escape") autocomplete-active?)
                               (do
                                 (.preventDefault event)
                                 (hide-autocomplete!))

                               ;; PRIORITY 2: Other special keys

                               ;; Ctrl+Space = trigger autocomplete (DISABLED)
                               ;; (and (= key " ") (.-ctrlKey event))
                               ;; (do
                               ;;   (.log js/console "🎯 Ctrl+Space detected - triggering autocomplete!")
                               ;;   (.preventDefault event)
                               ;;   (trigger-autocomplete! input-element))
                               false
                               nil

                               ;; Cmd+Enter or Ctrl+Enter = evaluate
                               (and (= key "Enter") ctrl-or-cmd)
                               (do
                                 (.preventDefault event)
                                 (evaluate-input))

                               ;; PRIORITY 2.5: History navigation with cmd+arrow keys

                               ;; Cmd+Arrow Up = previous command in history 
                               (and (= key "ArrowUp") ctrl-or-cmd (not (.-shiftKey event)) (not autocomplete-active?))
                               (do
                                 (.preventDefault event)
                                 (.log js/console "📜 Navigating history UP (Cursive style)")
                                 (let [command (history/navigate-history! :up)]
                                   (input/set-input-value! command)))

                               ;; Cmd+Arrow Down = next command in history
                               (and (= key "ArrowDown") ctrl-or-cmd (not (.-shiftKey event)) (not autocomplete-active?))
                               (do
                                 (.preventDefault event)
                                 (.log js/console "📜 Navigating history DOWN (Cursive style)")
                                 (let [command (history/navigate-history! :down)]
                                   (input/set-input-value! command)))

                               ;; Escape = clear input and reset history
                               (= key "Escape")
                               (do
                                 (.preventDefault event)
                                 (input/set-input-value! "")
                                 (history/reset-history-index!)
                                 (.log js/console "⎋ Escaped - input cleared and history reset"))

                               ;; Auto-close brackets
                               (contains? #{"(" "[" "{" ")" "]" "}"} key)
                               (brackets/auto-close-brackets! input-element event)

                               ;; PRIORITY 3: Default behaviors (only when autocomplete is not active)

                               ;; Enter without modifiers = new line (default behavior)
                               (and (= key "Enter") (not (.-shiftKey event)) (not ctrl-or-cmd) (not autocomplete-active?))
                               nil ;; Let default behavior happen (new line)

                               ;; Shift+Enter = new line (default behavior)
                               (and (= key "Enter") (.-shiftKey event) (not autocomplete-active?))
                               nil

                               ;; Arrow Up/Down without autocomplete = normal cursor navigation
                               (and (or (= key "ArrowUp") (= key "ArrowDown")) (not autocomplete-active?))
                               nil ;; Let default behavior happen

                               :else nil))))

      ;; Input and scroll handlers disabled (highlighting removed)
      ;; (.addEventListener input-element "input" ...)
      ;; (.addEventListener input-element "scroll" ...)
      )
    (.log js/console "✅ Input handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up input handlers:" e))))

(defn setup-search-handlers!
  "Setup search functionality handlers"
  []
  (try
    (.log js/console "🔍 Setting up search handlers...")

    ;; Search input handler
    (when-let [search-input (.getElementById js/document "repl-search-input")]
      (.addEventListener search-input "input"
                         (fn [event]
                           (let [search-term (.-value (.-target event))]
                             (search/perform-search! search-term))))

      (.addEventListener search-input "keydown"
                         (fn [event]
                           (let [key (.-key event)]
                             (cond
                               (= key "Enter")
                               (do
                                 (.preventDefault event)
                                 (if (.-shiftKey event)
                                   (search/search-prev!)
                                   (search/search-next!)))

                               (= key "Escape")
                               (do
                                 (.preventDefault event)
                                 (search/hide-search!)))))))

    ;; Search navigation buttons
    (when-let [next-btn (.getElementById js/document "search-next-btn")]
      (.addEventListener next-btn "click" (fn [_] (search/search-next!))))

    (when-let [prev-btn (.getElementById js/document "search-prev-btn")]
      (.addEventListener prev-btn "click" (fn [_] (search/search-prev!))))

    ;; Close button
    (when-let [close-btn (.getElementById js/document "search-close-btn")]
      (.addEventListener close-btn "click" (fn [_] (search/hide-search!))))

    ;; Case sensitivity checkbox
    (when-let [case-checkbox (.getElementById js/document "search-case-sensitive")]
      (.addEventListener case-checkbox "change"
                         (fn [_]
                           (.log js/console "🔍 Case sensitivity changed to:" (.-checked case-checkbox))
                           ;; Re-run search with current term when case sensitivity changes
                           (let [current-term (:current-term @search/search-state)]
                             (when (and current-term (not= current-term ""))
                               (search/perform-search! current-term))))))

    (.log js/console "✅ Search handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up search handlers:" e))))

(defn setup-global-search-shortcut!
  "Setup global Cmd/Ctrl+F shortcut to open search"
  []
  (try
    (.log js/console "🔍 Setting up global search shortcut...")
    (.addEventListener js/document "keydown"
                       (fn [event]
                         (let [key (.-key event)
                               ctrl-or-cmd (or (.-ctrlKey event) (.-metaKey event))]
                           (when (and (= key "f") ctrl-or-cmd)
                             (.preventDefault event)
                             (if (:active @search/search-state)
                               (when-let [search-input (.getElementById js/document "repl-search-input")]
                                 (.focus search-input)
                                 (.select search-input))
                               (search/show-search!))))))
    (.log js/console "✅ Global search shortcut setup complete")
    (catch :default e
      (.error js/console "Error setting up global search shortcut:" e))))

(defn setup-button-handlers!
  "Setup handlers for UI buttons"
  []
  (try
    (.log js/console "🔧 Setting up button handlers...")

    ;; Clear Output Button
    (when-let [clear-btn (.getElementById js/document "clear-output-btn")]
      (.addEventListener clear-btn "click"
                         (fn [_]
                           (.log js/console "🗑️ Clear Output clicked")
                           (when (.-vscode js/window)
                             (.postMessage (.-vscode js/window) #js {:type "clear-output"}))
                           ;; Also clear locally
                           (let [output-div (.getElementById js/document "repl-output")]
                             (when output-div
                               (set! (.-innerHTML output-div) ""))))))

    ;; Start/Stop REPL Button
    (when-let [repl-btn (.getElementById js/document "repl-toggle-btn")]
      (.addEventListener repl-btn "click"
                         (fn [_]
                           (.log js/console "🚀 REPL toggle button clicked")
                           (.log js/console "🔍 VS Code API available:" (not (nil? (.-vscode js/window))))

                           ;; ✅ CORREÇÃO: Verificar se está conectando ou conectado
                           (let [body (.-body js/document)
                                 is-connecting? (.contains (.-classList body) "repl-connecting")
                                 is-connected? (.-replRunning js/window)]

                             (if (or is-connecting? is-connected?)
                               (do
                                 (.log js/console "🔌 Disconnecting REPL...")
                                 (if (.-vscode js/window)
                                   (do
                                     (.log js/console "📤 Sending disconnect-repl message")
                                     (.postMessage (.-vscode js/window) #js {:type "disconnect-repl"}))
                                   (.error js/console "❌ VS Code API not available for disconnect")))
                               (do
                                 (.log js/console "🔌 Connecting REPL...")
                                 ;; First set the connecting state for UI feedback
                                 (repl-state/set-connection-state! :connecting)
                                 (if (.-vscode js/window)
                                   (do
                                     (.log js/console "📤 Sending connect-repl message")
                                     (.postMessage (.-vscode js/window) #js {:type "connect-repl"}))
                                   (.error js/console "❌ VS Code API not available for connect"))))))))

    ;; Search button
    (when-let [search-btn (.getElementById js/document "search-output-btn")]
      (.addEventListener search-btn "click"
                         (fn [_]
                           (.log js/console "🔍 Search button clicked")
                           (if (:active @search/search-state)
                             (when-let [search-input (.getElementById js/document "repl-search-input")]
                               (.focus search-input)
                               (.select search-input))
                             (search/show-search!)))))

    ;; Scroll buttons
    (when-let [top-btn (.getElementById js/document "scroll-top-btn")]
      (.addEventListener top-btn "click"
                         (fn [_]
                           (.log js/console "⬆️ Scroll to top clicked")
                           (when-let [output-el (.getElementById js/document "repl-output")]
                             (set! (.-scrollTop output-el) 0)))))

    (when-let [bottom-btn (.getElementById js/document "scroll-bottom-btn")]
      (.addEventListener bottom-btn "click"
                         (fn [_]
                           (.log js/console "⬇️ Scroll to bottom clicked")
                           (when-let [output-el (.getElementById js/document "repl-output")]
                             (set! (.-scrollTop output-el) (.-scrollHeight output-el))))))

    ;; Dependencies resolution button
    (when-let [deps-btn (.getElementById js/document "resolve-deps-btn")]
      (.addEventListener deps-btn "click"
                         (fn [_]
                           (.log js/console "🔧 Dependencies button clicked")
                           (when (.-vscode js/window)
                             (.postMessage (.-vscode js/window) #js {:type "resolve-dependencies"})))))

    (.log js/console "✅ Button handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up button handlers:" e))))

(defn setup-window-handlers!
  "Setup window-level event handlers"
  []
  (try
    (.log js/console "📐 Setting up window handlers...")

    ;; Window resize handler
    (.addEventListener js/window "resize"
                       (fn [_]
                         (.log js/console "📐 Window resized")
                         ;; Future: Add any resize handling here
                         ))

    ;; Global message handler for VS Code
    (.addEventListener js/window "message"
                       (fn [event]
                         ;; This will be handled by the main webview file
                         ;; Just log for now
                         (.log js/console "📨 Received message from VS Code:" (.-type (.-data event)))))

    (.log js/console "✅ Window handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up window handlers:" e))))

(defn setup-all-handlers!
  "Setup all event handlers for the webview"
  []
  (try
    (.log js/console "⚡ Setting up all event handlers...")

    ;; Setup all handler groups
    (setup-input-handlers)
    (setup-search-handlers!)
    (setup-global-search-shortcut!)
    (setup-button-handlers!)
    (setup-window-handlers!)

    (.log js/console "✅ All event handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up all handlers:" e)))) 