(ns repltile.core.state
  "Functional state management for REPLTILE"
  (:require [repltile.core.logger :as log]
            [clojure.string :as str]))

;; Forward declaration for availability updates
(declare send-availability-update-if-available!)

;; Forward declaration for queue display updates
(declare update-queue-display-if-available!)

;; Main application state
(def app-state
  (atom {:repl-connection nil
         :calva-status :disconnected
         :repl-tooling-status :disconnected
         :current-namespace "user"
         :evaluation-history []
         :socket-repl-port nil
         :webview-ready false
         :completions []
         :namespace-vars {}
         :theme "dark"
         :log-level :info
         :tail-enabled true
         :command-history []
         :command-history-index -1
         :evaluating false
         :command-queue []        ; Queue for pending commands
         :current-command nil
         :auto-complete-command? false    ; Currently executing command info
         :deps-status :unknown            ; Dependency status: :resolved, :unresolved, :resolving, :unknown
         :deps-can-start-repl false       ; Whether REPL can start (deps resolved)
         :deps-summary ""                 ; Human readable deps status
         :deps-detected-configs []        ; List of detected config files
         :deps-resolved-status {}         ; Map of config-type -> resolved status
         :deps-auto-resolve-enabled true  ; Whether auto-resolve is enabled
         :repl-button-blocked false       ; Whether REPL button is temporarily blocked
         :repl-button-block-until 0       ; Timestamp until when button should be blocked
         :current-project-path nil        ; Current project path for REPL uniqueness
         :repl-startup-process nil        ; Track startup process to allow cancellation
         :repl-startup-cancelled false})) ; Flag to track if startup was cancelled

;; Getters
(defn get-state [] @app-state)
(defn get-repl-connection [] (:repl-connection @app-state))
(defn get-calva-status [] (:calva-status @app-state))
(defn is-auto-complete-command? [] (:auto-complete-command? @app-state))
(defn get-repl-tooling-status [] (:repl-tooling-status @app-state))
(defn get-current-namespace [] (:current-namespace @app-state))
(defn get-evaluation-history [] (:evaluation-history @app-state))
(defn get-socket-repl-port [] (:socket-repl-port @app-state))
(defn get-webview-ready? [] (:webview-ready @app-state))
(defn get-namespace-vars [] (:namespace-vars @app-state))
(defn get-tail-enabled? [] (:tail-enabled @app-state))
(defn get-command-history [] (:command-history @app-state))
(defn get-command-history-index [] (:command-history-index @app-state))
(defn get-evaluating? [] (:evaluating @app-state))
(defn get-command-queue [] (:command-queue @app-state))
(defn get-command-queue-size [] (count (:command-queue @app-state)))
(defn get-current-command [] (:current-command @app-state))

;; Dependency status getters
(defn get-deps-status [] (:deps-status @app-state))
(defn get-deps-can-start-repl? [] (:deps-can-start-repl @app-state))
(defn get-deps-summary [] (:deps-summary @app-state))
(defn get-deps-detected-configs [] (:deps-detected-configs @app-state))
(defn get-deps-resolved-status [] (:deps-resolved-status @app-state))
(defn get-deps-auto-resolve-enabled? [] (:deps-auto-resolve-enabled @app-state))

;; REPL button blocking getters
(defn get-repl-button-blocked? [] (:repl-button-blocked @app-state))
(defn get-repl-button-block-until [] (:repl-button-block-until @app-state))
(defn get-current-project-path [] (:current-project-path @app-state))
(defn get-repl-startup-process [] (:repl-startup-process @app-state))
(defn get-repl-startup-cancelled? [] (:repl-startup-cancelled @app-state))

;; Check if button should be blocked (time-based check)
(defn is-repl-button-blocked? []
  (let [now (js/Date.now)
        block-until (get-repl-button-block-until)]
    (and (get-repl-button-blocked?) (< now block-until))))

;; Setters
(defn set-repl-connection! [connection]
  (swap! app-state assoc :repl-connection connection)
  (log/info "🔌 REPL connection updated"))

(defn set-calva-status! [status]
  (swap! app-state assoc :calva-status status)
  (log/calva :info (str "Status changed to: " status))
  (send-availability-update-if-available!))

(defn set-repl-tooling-status! [status]
  (swap! app-state assoc :repl-tooling-status status)
  (log/repl-tooling :info (str "Status changed to: " status))
  (send-availability-update-if-available!))

(defn set-current-namespace! [ns]
  (swap! app-state assoc :current-namespace ns)
  (log/info "📦 Current namespace:" ns))

(defn set-socket-repl-port! [port]
  (swap! app-state assoc :socket-repl-port port)
  (log/info "🔌 Socket REPL port:" port))

(defn set-webview-ready! [ready?]
  (swap! app-state assoc :webview-ready ready?)
  (log/webview :info (str "WebView ready: " ready?)))

(defn set-tail-enabled! [enabled?]
  (swap! app-state assoc :tail-enabled enabled?)
  (log/info (str "🎯 Tail output enabled: " enabled?)))

(defn set-evaluating! [evaluating?]
  (swap! app-state assoc :evaluating evaluating?)
  (log/info (str "⏳ Evaluating: " evaluating?)))

(defn set-auto-complete-command! [auto-complete-command?]
  (swap! app-state assoc :auto-complete-command? auto-complete-command?)
  (log/info (str "🔄 Auto-complete command: " auto-complete-command?)))

;; Dependency status setters
(defn set-deps-status! [status]
  (swap! app-state assoc :deps-status status)
  (log/info (str "🔧 Dependencies status: " status)))

(defn set-deps-can-start-repl! [can-start?]
  (swap! app-state assoc :deps-can-start-repl can-start?)
  (log/info (str "🚀 Can start REPL: " can-start?)))

(defn set-deps-summary! [summary]
  (swap! app-state assoc :deps-summary summary)
  (log/info (str "📋 Deps summary: " summary)))

(defn set-deps-detected-configs! [configs]
  (swap! app-state assoc :deps-detected-configs configs)
  (log/info (str "📁 Detected configs: " configs)))

(defn set-deps-resolved-status! [resolved-status]
  (swap! app-state assoc :deps-resolved-status resolved-status)
  (log/info (str "✅ Resolved status: " resolved-status)))

(defn set-deps-auto-resolve-enabled! [enabled?]
  (swap! app-state assoc :deps-auto-resolve-enabled enabled?)
  (log/info (str "🔄 Auto-resolve enabled: " enabled?)))

(defn update-deps-state!
  [updates]
  (swap! app-state merge updates)
  (log/info "📊 Dependencies state updated"))

;; Command Queue Management

(defn add-command-to-queue! [command-info]
  (swap! app-state update :command-queue conj command-info)
  (log/info (str "📋 Added to queue: " (:description command-info) " | Queue size: " (get-command-queue-size)))
  (update-queue-display-if-available!))

(defn set-current-command! [command-info]
  (swap! app-state assoc :current-command command-info)
  (log/info (str "🔄 Current command: " (:description command-info)))
  (update-queue-display-if-available!))

(defn get-next-queued-command! []
  (let [queue (get-command-queue)]
    (when (seq queue)
      (let [next-command (first queue)]
        (swap! app-state update :command-queue rest)
        (log/info (str "▶️ Next queued command: " (:description next-command) " | Remaining: " (get-command-queue-size)))
        (update-queue-display-if-available!)
        next-command))))

(defn clear-current-command! []
  (swap! app-state assoc :current-command nil)
  (log/info "✅ Current command cleared")
  (update-queue-display-if-available!))

(defn get-queue-status-text []
  (let [current (get-current-command)
        queue-size (get-command-queue-size)]
    (if current
      (if (> queue-size 0)
        (str "⏳ " (:description current) " (" queue-size " pending)")
        (str "⏳ " (:description current)))
      "Enter Clojure code...")))

(defn truncate-command-description [description max-length]
  (if (> (count description) max-length)
    (str (subs description 0 (- max-length 3)) "...")
    description))

(defn add-to-command-history! [command]
  (when (and (not (str/blank? command))
             (not= command (last (:command-history @app-state))))
    (swap! app-state
           (fn [state]
             (let [history (:command-history state)
                   new-history (conj history command)
                   ;; Keep only last 100 commands
                   trimmed-history (if (> (count new-history) 100)
                                     (vec (take-last 100 new-history))
                                     new-history)]
               (assoc state
                      :command-history trimmed-history
                      :command-history-index -1))))))

(defn navigate-command-history! [direction]
  (let [current-state @app-state
        history (:command-history current-state)
        current-index (:command-history-index current-state)
        history-size (count history)]
    (when (pos? history-size)
      (let [new-index (case direction
                        :up (min (dec history-size) (inc current-index))
                        :down (max -1 (dec current-index)))]
        (swap! app-state assoc :command-history-index new-index)
        (if (>= new-index 0)
          (nth history (- history-size 1 new-index))
          "")))))

;; History management
(defn add-evaluation! [evaluation]
  (swap! app-state update :evaluation-history conj evaluation)
  (log/debug "📝 Added evaluation to history"))

(defn clear-evaluation-history! []
  (swap! app-state assoc :evaluation-history [])
  (log/info "🗑️ Cleared evaluation history"))

;; Completions
(defn set-completions! [completions]
  (swap! app-state assoc :completions completions)
  (log/debug "💡 Updated completions:" (count completions) "items"))

;; Namespace vars
(defn set-namespace-vars! [namespace vars]
  (swap! app-state assoc-in [:namespace-vars namespace] vars)
  (log/debug "📦 Updated vars for namespace" namespace ":" (count vars) "vars"))

;; REPL button blocking setters
(defn set-repl-button-blocked! [blocked?]
  (swap! app-state assoc :repl-button-blocked blocked?)
  (log/info "🔒 REPL button blocked:" blocked?))

(defn block-repl-button-for-seconds! [seconds]
  (let [now (js/Date.now)
        block-until (+ now (* seconds 1000))]
    (swap! app-state assoc
           :repl-button-blocked true
           :repl-button-block-until block-until)
    (log/info (str "🔒 REPL button blocked for " seconds " seconds until " block-until))
    ;; Auto-unblock after timeout
    (js/setTimeout #(when (<= block-until (js/Date.now))
                      (swap! app-state assoc :repl-button-blocked false)
                      (log/info "🔓 REPL button auto-unblocked"))
                   (* seconds 1000))))

(defn set-current-project-path! [path]
  (swap! app-state assoc :current-project-path path)
  (log/info "📁 Current project path:" path))

(defn set-repl-startup-process! [process]
  (swap! app-state assoc :repl-startup-process process)
  (log/info "🚀 REPL startup process:" (if process "set" "cleared")))

(defn set-repl-startup-cancelled! [cancelled?]
  (swap! app-state assoc :repl-startup-cancelled cancelled?)
  (log/info "🚫 REPL startup cancelled:" cancelled?))

;; Initialization
(defn init!
  []
  (log/info "🏗️ Initializing application state...")
  (reset! app-state {:repl-connection nil
                     :calva-status :disconnected
                     :repl-tooling-status :disconnected
                     :current-namespace "user"
                     :evaluation-history []
                     :socket-repl-port nil
                     :webview-ready false
                     :completions []
                     :namespace-vars {}
                     :theme "dark"
                     :log-level :info
                     :tail-enabled true
                     :command-history []
                     :command-history-index -1
                     :evaluating false
                     :command-queue []
                     :current-command nil})
  (log/info "✅ Application state initialized"))

;; State watchers for debugging
(defn watch-state!
  []
  (add-watch app-state :debug-watcher
             (fn [_key _ref old-state new-state]
               (let [changed-keys (filter #(not= (get old-state %) (get new-state %)) (keys new-state))]
                 (when (seq changed-keys)
                   (log/debug "🔄 State changed:" (pr-str changed-keys)))))))

;; Initialize watcher in development
(when ^boolean goog.DEBUG
  (watch-state!))

;; Availability update function (will be set by core)
(def availability-update-fn (atom nil))
;; Queue display update function (will be set by webview)
(def queue-display-update-fn (atom nil))

(defn set-availability-update-fn! [fn]
  (reset! availability-update-fn fn))

(defn set-queue-display-update-fn! [fn]
  (reset! queue-display-update-fn fn))

(defn send-availability-update-if-available!
  []
  (when-let [update-fn @availability-update-fn]
    (update-fn)))

(defn update-queue-display-if-available!
  []
  (when-let [update-fn @queue-display-update-fn]
    (update-fn))) 