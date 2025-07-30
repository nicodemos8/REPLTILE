(ns repltile.ui.state.repl-state
  "REPL state management for REPLTILE webview"
  (:require [repltile.core.state :as state]
            [repltile.ui.components.input :as input]
            [repltile.ui.components.output :as output]))

;; REPL State management functions for CSS classes
(defn apply-repl-state-class!
  [repl-state]
  (let [body (.-body js/document)]
    ;; Remove all REPL state classes first
    (.remove (.-classList body) "repl-disconnected" "repl-connecting" "repl-evaluating")

    ;; Apply the appropriate class (disconnected state class removed)
    (case repl-state
      :disconnected nil ; No special class for disconnected state
      :connecting (.add (.-classList body) "repl-connecting")
      :evaluating (.add (.-classList body) "repl-evaluating")
      nil) ; :connected doesn't need special class

    (.log js/console (str "🎨 Applied REPL state class: " repl-state))))

(defn set-controls-disabled! [disabled? & [message]]
  (let [_ns-combo (.getElementById js/document "ns-combo")
        reload-ns-btn (.getElementById js/document "reload-ns-btn")
        refresh-all-btn (.getElementById js/document "refresh-all-btn")
        run-ns-tests-btn (.getElementById js/document "run-ns-tests-btn")]
    (.log js/console "🔧 Setting controls disabled:" disabled? "message:" message)

    ;; Apply evaluating state class when disabled for evaluation
    (when disabled?
      (apply-repl-state-class! :evaluating))

    ;; Remove evaluating state when enabling (but keep connection state if any)
    (when (not disabled?)
      ;; Only remove evaluating class, preserve connection states
      (let [body (.-body js/document)]
        (.remove (.-classList body) "repl-evaluating")))

    ;; Use queue-aware input state management  
    (if disabled?
      (input/set-input-enabled! false message) ; Will show queue status if no custom message
      (input/set-input-enabled! true))

    ;; ns-combo is always disabled as it's now read-only display
    (when reload-ns-btn
      (set! (.-disabled reload-ns-btn) disabled?))
    (when refresh-all-btn
      (set! (.-disabled refresh-all-btn) disabled?))
    (when run-ns-tests-btn
      (set! (.-disabled run-ns-tests-btn) disabled?))
    (.log js/console (str "Controls " (if disabled? "disabled" "enabled")))))

(defn update-namespace-buttons-state!
  [current-ns]
  (let [reload-ns-btn (.getElementById js/document "reload-ns-btn")
        refresh-all-btn (.getElementById js/document "refresh-all-btn")
        run-ns-tests-btn (.getElementById js/document "run-ns-tests-btn")
        is-user-namespace? (= current-ns "user")]
    (.log js/console "🔧 Updating namespace buttons state. Current ns:" current-ns "Is user namespace:" is-user-namespace?)
    ;; Disable reload, refresh-all, and run-tests buttons when in user namespace
    (when reload-ns-btn
      (set! (.-disabled reload-ns-btn) is-user-namespace?))
    (when refresh-all-btn
      (set! (.-disabled refresh-all-btn) is-user-namespace?))
    (when run-ns-tests-btn
      (set! (.-disabled run-ns-tests-btn) is-user-namespace?))))

(defn update-namespace-display!
  []
  (let [ns-combo (.getElementById js/document "ns-combo")]
    (when ns-combo
      (let [current-ns (state/get-current-namespace)]
        (.log js/console "🔧 Updating namespace display to:" current-ns)
        (set! (.-value ns-combo) (or current-ns "user"))
        (input/update-input-placeholder!)
        (update-namespace-buttons-state! current-ns)))))

(defn set-connection-state! [state]
  (let [ns-combo (.getElementById js/document "ns-combo")
        reload-ns-btn (.getElementById js/document "reload-ns-btn")
        refresh-all-btn (.getElementById js/document "refresh-all-btn")
        run-ns-tests-btn (.getElementById js/document "run-ns-tests-btn")
        play-btn (.getElementById js/document "repl-toggle-btn")]
    (.log js/console "🔄 Setting connection state to:" state)
    (.log js/console "🔄 Current time:" (js/Date.now))
    (case state
      :disconnected
      (do
        ;; Remove the apply-repl-state-class! call for disconnected state
        (state/set-current-namespace! "user")
        (set! (.-replRunning js/window) false)
        (input/set-input-enabled! false)
        (input/set-input-value! "{:REPL {:connection-status :disconnected}}")

        (when ns-combo
          (set! (.-value ns-combo) "REPL disconnected"))
        (when reload-ns-btn
          (set! (.-disabled reload-ns-btn) true))
        (when refresh-all-btn
          (set! (.-disabled refresh-all-btn) true))
        (when run-ns-tests-btn
          (set! (.-disabled run-ns-tests-btn) true))
        (when play-btn
          (set! (.-disabled play-btn) false)
          (set! (.-title play-btn) "Start REPL")
          (set! (.-innerHTML play-btn)
                "<svg id='icon-play' width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/><polygon points='11,8 22,14 11,20' fill='#22c55e'/></svg>")))

      :connecting
      (do
        (apply-repl-state-class! :connecting)
        (state/set-current-namespace! "user")
        (input/set-input-enabled! false)
        (input/set-input-value! "{:REPL {:connection-status :connecting}}")
        (when ns-combo
          (set! (.-value ns-combo) "REPL connecting..."))
        (when reload-ns-btn
          (set! (.-disabled reload-ns-btn) true))
        (when refresh-all-btn
          (set! (.-disabled refresh-all-btn) true))
        (when run-ns-tests-btn
          (set! (.-disabled run-ns-tests-btn) true))
        (when play-btn
          ;; ✅ CORREÇÃO: Manter o botão habilitado durante conexão
          (set! (.-disabled play-btn) false)
          (set! (.-title play-btn) "Stop REPL")
          (set! (.-innerHTML play-btn)
                "<svg id='icon-loading' width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/><circle cx='14' cy='14' r='8' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-dasharray='25' stroke-dashoffset='25'><animateTransform attributeName='transform' type='rotate' values='0 14 14;360 14 14' dur='1s' repeatCount='indefinite'/></circle></svg>")))

      :connected
      (do
        (apply-repl-state-class! nil) ; Clear special state classes
        (set! (.-replRunning js/window) true)
        (input/set-input-enabled! true)
        (input/set-input-value! "")
        (input/update-input-placeholder!)
        (when ns-combo
          (let [current-ns (state/get-current-namespace)]
            (set! (.-value ns-combo) (or current-ns "user"))))
        (when reload-ns-btn
          (set! (.-disabled reload-ns-btn) false))
        (when refresh-all-btn
          (set! (.-disabled refresh-all-btn) false))
        (when run-ns-tests-btn
          (set! (.-disabled run-ns-tests-btn) false))
        (when play-btn
          (set! (.-disabled play-btn) false)
          (set! (.-title play-btn) "Stop REPL")
          (set! (.-innerHTML play-btn)
                "<svg id='icon-stop' width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/><rect x='10' y='10' width='8' height='8' rx='2' fill='#22c55e'/></svg>"))))
    (.log js/console (str "Connection state set to: " state))))

(defn setup-repl-functions
  []
  (try
    ;; Create global repltile object
    (when-not (.-repltile js/window)
      (set! (.-repltile js/window) #js {}))

    ;; Add REPL functions
    (set! (.-connectToRepl (.-repltile js/window))
          (fn []
            (.log js/console "🔌 Connecting to REPL...")
            (when (.-vscode js/window)
              (.postMessage (.-vscode js/window) #js {:type "connect-repl"}))))

    (set! (.-disconnectRepl (.-repltile js/window))
          (fn []
            (.log js/console "🔌 Disconnecting from REPL...")
            (when (.-vscode js/window)
              (.postMessage (.-vscode js/window) #js {:type "disconnect-repl"}))))

    (set! (.-clearConsole (.-repltile js/window)) output/clear-console)

    (set! (.-showHelp (.-repltile js/window))
          (fn []
            (output/add-to-output
             "<div class='repl-output-line'>
                <h4>🚀 REPLTILE Help</h4>
                <p><strong>Keyboard Shortcuts:</strong></p>
                <ul>
                  <li><kbd>Enter</kbd> - Evaluate current expression</li>
                  <li><kbd>Shift+Enter</kbd> - New line</li>
                  <li><kbd>↑</kbd>/<kbd>↓</kbd> - Command history (coming soon)</li>
                </ul>
                <p><strong>Commands:</strong></p>
                <ul>
                  <li><code>(+ 1 2 3)</code> - Basic arithmetic</li>
                  <li><code>(def x 42)</code> - Define variables</li>
                  <li><code>(println \"Hello World\")</code> - Print output</li>
                </ul>
              </div>" "repl-help")))

    (.log js/console "✅ REPL functions setup complete")
    (catch :default e
      (.error js/console "Error setting up REPL functions:" e))))

(defn update-queue-status-display! []
  (when (state/get-evaluating?)
    (input/set-input-enabled! false))) ; This will show queue status via get-queue-status-text

(defn get-repl-state []
  {:connected (.-replRunning js/window)
   :current-namespace (state/get-current-namespace)
   :evaluating (state/get-evaluating?)
   :queue-status (state/get-queue-status-text)
   :input-enabled (when-let [input-el (.getElementById js/document "repl-input")]
                    (not (.-disabled input-el)))}) 