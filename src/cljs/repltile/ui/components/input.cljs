(ns repltile.ui.components.input
  "Input management for REPLTILE webview"
  (:require [clojure.string :as str]
            [repltile.core.state :as state]))

(defn get-input-value []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (.-value input-el)))

(defn set-input-value! [value]
  (when-let [input-el (.getElementById js/document "repl-input")]
    (set! (.-value input-el) value)))

(defn set-input-enabled! [enabled? & [message]]
  (when-let [input-el (.getElementById js/document "repl-input")]
    (set! (.-disabled input-el) (not enabled?))
    (if enabled?
      ;; When enabling, only clear if it contains status messages (not user input)
      ;; NEVER clear when autocomplete dropdown is visible
      (let [current-value (.-value input-el)
            autocomplete-dropdown (.getElementById js/document "autocomplete-dropdown")
            autocomplete-active? (not (nil? autocomplete-dropdown))
            is-status-message? (or (str/includes? current-value "⏳")
                                   (str/includes? current-value "pending")
                                   (str/includes? current-value "{:REPL")
                                   (str/includes? current-value "connection-status")
                                   (str/includes? current-value "disconnected"))]
        (.log js/console "🔧 set-input-enabled: enabled=" enabled?
              "autocomplete-active=" autocomplete-active?
              "is-status-message=" is-status-message?
              "current-value=" current-value)
        (when (and (or (nil? message) is-status-message?)
                   (not autocomplete-active?))
          (when-not (state/is-auto-complete-command?)
            (.log js/console "🔧 Clearing input because command is not auto-complete")
            (set! (.-value input-el) ""))))

      ;; When disabling, show queue status or custom message
      (let [status-msg (or message (state/get-queue-status-text))]
        (set! (.-value input-el) status-msg)))))

(defn update-input-placeholder! []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (let [_current-ns (state/get-current-namespace)
          placeholder-text (str "Enter Clojure code here...\n\n"
                                "Input Commands:\n"
                                " • Cmd+Enter: Evaluate command\n"
                                " • Cmd+↑: Previous command in history\n"
                                " • Cmd+↓: Next command in history\n\n")]
      (set! (.-placeholder input-el) placeholder-text))))

(defn focus-input! []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (.focus input-el)))

(defn get-cursor-position []
  (when-let [input-el (.getElementById js/document "repl-input")]
    (.-selectionStart input-el)))

(defn set-cursor-position! [pos]
  (when-let [input-el (.getElementById js/document "repl-input")]
    (set! (.-selectionStart input-el) pos)
    (set! (.-selectionEnd input-el) pos))) 