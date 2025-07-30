(ns repltile.repl.deps
  "Dependency detection and resolution system for REPLTILE"
  (:require [repltile.core.logger :as log]
            [repltile.vscode.extension :as vscode]
            [repltile.core.state :as state]
            [clojure.string :as str]))

;; Forward declaration for deps resolution callbacks
(declare send-deps-status-update!)

;; Dependency configuration files mapping
(def dependency-configs
  {:deps.edn {:filename "deps.edn"
              :check-dirs ["~/.m2/repository" ".cpcache"]
              :resolve-cmd "clj -P"
              :description "Clojure CLI tools"}
   :project.clj {:filename "project.clj"
                 :check-dirs ["~/.m2/repository" "target"]
                 :resolve-cmd "lein deps"
                 :description "Leiningen"}
   :shadow-cljs.edn {:filename "shadow-cljs.edn"
                     :check-dirs ["node_modules" ".shadow-cljs"]
                     :resolve-cmd "npm install"
                     :description "Shadow CLJS"}
   :build.boot {:filename "build.boot"
                :check-dirs ["~/.m2/repository"]
                :resolve-cmd "boot show -d"
                :description "Boot"}
   :package.json {:filename "package.json"
                  :check-dirs ["node_modules"]
                  :resolve-cmd "npm install"
                  :description "NPM/Node.js"}})

;; Dependency status tracking
(def deps-state
  (atom {:detected-configs []           ; List of detected config files
         :resolved-status {}            ; Map of config-type -> boolean
         :current-resolution nil        ; Currently resolving config type
         :last-check-timestamp nil      ; When deps were last checked
         :auto-resolve-enabled true     ; Whether to auto-resolve on file changes
         :pending-detection-count 0     ; Number of pending file detection requests
         :auto-resolve-pending false    ; Whether auto-resolve is waiting for detection
         :resolution-in-progress false  ; Whether any resolution is currently running
         :last-resolution-timestamp 0   ; Timestamp of last resolution to prevent duplicates
         :last-detection-timestamp 0})) ; Timestamp of last detection to prevent duplicates

;; Helper functions

(defn get-workspace-root
  "Get workspace root path from VSCode"
  []
  ;; This will be handled by TypeScript side
  (vscode/request-workspace-info!))

(defn normalize-path
  "Normalize a path for cross-platform compatibility"
  [path]
  (-> path
      (str/replace #"~" (or js/process.env.HOME "/"))
      (str/replace #"\\" "/")))

(defn config-exists?
  "Check if a config file exists in workspace"
  [config-type]
  (let [config (get dependency-configs config-type)
        filename (:filename config)]
    ;; Increment pending count
    (swap! deps-state update :pending-detection-count inc)
    (log/info (str "🔍 [DEPS] Checking config: " config-type " (" filename ") - pending count now: " (:pending-detection-count @deps-state)))
    ;; Send request to TypeScript to check file existence
    (vscode/send-message-to-extension!
     {:type :check-file-exists
      :filename filename
      :config-type config-type})))

(defn check-resolved-dirs
  "Check if dependency directories exist (indicates resolved deps)"
  [config-type]
  (let [config (get dependency-configs config-type)
        dirs (:check-dirs config)]
    ;; Send request to TypeScript to check directories
    (vscode/send-message-to-extension!
     {:type :check-dirs-exist
      :dirs (map normalize-path dirs)
      :config-type config-type})))

;; Main API functions

(defn detect-project-configs!
  "Detect which dependency configuration files exist in the project"
  []
  (let [current-time (js/Date.now)
        state @deps-state
        last-detection-time (:last-detection-timestamp state 0)
        time-since-last (- current-time last-detection-time)
        pending-count (:pending-detection-count state)]

    (cond
      ;; Debounce protection: prevent multiple calls within 1 second
      (< time-since-last 1000)
      (log/warn (str "🔄 [DEPS] Ignoring duplicate detection call (debounce protection) - " time-since-last "ms since last"))

      ;; If detection is already in progress, ignore
      (> pending-count 0)
      (log/warn (str "🔄 [DEPS] Detection already in progress (" pending-count " pending), ignoring duplicate call"))

      ;; Proceed with detection
      :else
      (do
        (log/info "🔍 [DEPS] Starting dependency detection...")
        (swap! deps-state assoc :last-detection-timestamp current-time)
        (doseq [[config-type _] dependency-configs]
          (config-exists? config-type))
        ;; Results will be handled by callbacks from TypeScript
        ))))

(defn check-all-dependencies-resolved!
  "Check if all detected dependencies are resolved"
  []
  (log/info "📋 [DEPS] Checking dependency resolution status...")
  (let [detected-configs (:detected-configs @deps-state)
        current-project (state/get-current-project-path)]
    (if (empty? detected-configs)
      (do
        (log/warn (str "⚠️ [DEPS] No dependency configurations detected in project: "
                       (or current-project "unknown path")
                       " | Searched for: " (str/join ", " (map #(str (:filename (get dependency-configs %))) (keys dependency-configs)))))
        (swap! deps-state assoc :resolved-status {}))
      (doseq [config-type detected-configs]
        (check-resolved-dirs config-type))))
  (send-deps-status-update!))

(defn resolve-dependencies!
  "Resolve dependencies for a specific config type or all detected configs"
  ([config-type]
   (log/info (str "🔧 [DEPS] Resolving dependencies for " (name config-type)))
   (let [config (get dependency-configs config-type)]
     (if config
       (do
         (swap! deps-state assoc :current-resolution config-type :resolution-in-progress true)
         (send-deps-status-update!)
         ;; Send command to TypeScript to execute resolution
         (vscode/send-message-to-extension!
          {:type :resolve-dependencies
           :config-type config-type
           :command (:resolve-cmd config)
           :description (:description config)}))
       (log/error (str "❌ [DEPS] Unknown config type: " config-type)))))
  ([]
   ;; Resolve all detected configs with debounce protection
   (let [detected-configs (:detected-configs @deps-state)
         pending-count (:pending-detection-count @deps-state)
         resolution-in-progress? (:resolution-in-progress @deps-state)
         current-time (js/Date.now)
         last-resolution-time (:last-resolution-timestamp @deps-state)
         time-since-last (- current-time last-resolution-time)]

     (log/info (str "🔧 [DEPS] === RESOLVE-DEPENDENCIES CALLED ==="))
     (log/info (str "🔧 [DEPS] Detected configs: " detected-configs))
     (log/info (str "🔧 [DEPS] Pending count: " pending-count))
     (log/info (str "🔧 [DEPS] Resolution in progress: " resolution-in-progress?))
     (log/info (str "🔧 [DEPS] Time since last resolution: " time-since-last "ms"))

     (cond
       ;; Debounce protection: prevent multiple calls within 2 seconds
       (< time-since-last 2000)
       (log/warn "🔄 [DEPS] Ignoring duplicate resolution request (debounce protection)")

       ;; If resolution is already in progress, ignore
       resolution-in-progress?
       (log/warn "🔄 [DEPS] Resolution already in progress, ignoring duplicate request")

       ;; If there are pending detections, wait for them
       (> pending-count 0)
       (do
         (log/warn "⏳ [DEPS] Dependency detection still in progress, please wait...")
         (vscode/send-webview-message!
          {:type :dependency-status-update
           :status :unknown
           :summary "⏳ Detecting dependencies, please wait..."
           :can-start-repl false
           :detected-configs []
           :resolved-status {}}))

       ;; If no configs detected, try re-detecting
       (empty? detected-configs)
       (do
         (let [current-project (state/get-current-project-path)]
           (log/warn (str "⚠️ [DEPS] No dependency configurations detected in project: "
                          (or current-project "unknown path")
                          " | Re-scanning for: " (str/join ", " (map #(str (:filename (get dependency-configs %))) (keys dependency-configs))))))
         (vscode/send-webview-message!
          {:type :dependency-status-update
           :status :unknown
           :summary "🔍 Re-scanning project for dependency configurations..."
           :can-start-repl false
           :detected-configs []
           :resolved-status {}})

         ;; Reset state and re-detect
         (swap! deps-state assoc
                :detected-configs []
                :pending-detection-count 0)

         ;; Trigger new detection
         (log/info "🔍 [DEPS] Starting new detection scan...")
         (detect-project-configs!)

         ;; After a delay, try resolving again if configs were found
         (js/setTimeout
          (fn []
            (let [new-detected-configs (:detected-configs @deps-state)]
              (log/info (str "🔍 [DEPS] Re-scan complete. Found configs: " new-detected-configs))
              (if (empty? new-detected-configs)
                (log/warn "⚠️ [DEPS] Still no dependency configurations found after re-scan")
                (do
                  (log/info (str "✅ [DEPS] Found configurations after re-scan: " new-detected-configs))
                  (doseq [config-type new-detected-configs]
                    (resolve-dependencies! config-type))))))
          2000))

       ;; We have configs, resolve them
       :else
       (do
         (log/info (str "✅ [DEPS] Proceeding to resolve " (count detected-configs) " configurations"))
         ;; Update timestamp to prevent duplicates
         (swap! deps-state assoc :last-resolution-timestamp current-time)
         (doseq [config-type detected-configs]
           (log/info (str "🔧 [DEPS] Resolving: " config-type))
           (resolve-dependencies! config-type)))))))

(defn get-overall-deps-status
  "Get overall dependency status - :resolved, :unresolved, :resolving, or :unknown"
  []
  (let [{:keys [detected-configs resolved-status current-resolution]} @deps-state]
    (cond
      current-resolution :resolving
      (empty? detected-configs) :unknown
      (every? #(get resolved-status % false) detected-configs) :resolved
      :else :unresolved)))

(defn can-start-repl?
  "Check if REPL can be started (dependencies resolved)"
  []
  (= :resolved (get-overall-deps-status)))

(defn get-deps-summary
  "Get human-readable summary of dependency status"
  []
  (let [{:keys [detected-configs resolved-status current-resolution]} @deps-state
        status (get-overall-deps-status)]
    (case status
      :resolving (str "🔄 Resolving " (name current-resolution) " dependencies...")
      :resolved "✅ All dependencies resolved"
      :unresolved (let [unresolved (filter #(not (get resolved-status % false)) detected-configs)]
                    (str "❌ Dependencies not resolved for: " (str/join ", " (map name unresolved))))
      :unknown (str "⚠️ No dependency configurations detected in: "
                    (or (state/get-current-project-path) "unknown project")))))

;; Callback handlers (called from TypeScript responses)

(defn handle-config-detected!
  "Handle config file detection response from TypeScript"
  [message]
  (let [config-type (keyword (:config-type message))
        exists? (:exists message)
        current-configs (:detected-configs @deps-state)]
    (log/info (str "📁 [DEPS] Config " (name config-type) ": " (if exists? "DETECTED" "not found")))

    ;; Update detected configs - PREVENT DUPLICATES
    (when (and exists? (not (some #{config-type} current-configs)))
      (log/info (str "✅ [DEPS] Adding new config: " (name config-type)))
      (swap! deps-state update :detected-configs conj config-type))

    (when (and exists? (some #{config-type} current-configs))
      (log/warn (str "🔄 [DEPS] Ignoring duplicate detection of: " (name config-type))))

    ;; Decrement pending count
    (swap! deps-state update :pending-detection-count dec)

    ;; Check if all detections are complete and auto-resolve is pending
    (let [state @deps-state
          pending-count (:pending-detection-count state)
          auto-resolve-pending? (:auto-resolve-pending state)]

      (log/info (str "📊 [DEPS] Detection progress: " pending-count " pending, auto-resolve pending: " auto-resolve-pending?))
      (log/info (str "📊 [DEPS] Current detected configs: " (:detected-configs state)))

      (when (and auto-resolve-pending? (<= pending-count 0))
        (log/info "🔄 [DEPS] All detections complete, starting auto-resolve...")
        (log/info "🚀 [DEPS] AUTO-RESOLVING on startup to get real dependency status")
        (swap! deps-state assoc :auto-resolve-pending false)
        ;; Start auto-resolve with a short delay to ensure state is fully updated
        (js/setTimeout
         (fn []
           (log/info "🔄 [DEPS] Executing delayed auto-resolve...")
           (resolve-dependencies!))
         500))

      (send-deps-status-update!))))

(defn handle-dirs-checked!
  "Handle directory existence check response from TypeScript"
  [message]
  (let [config-type (keyword (:config-type message))
        resolved? (:resolved message)]
    (log/info (str "📋 Dependencies for " (name config-type) ": " (if resolved? "resolved" "unresolved")))
    (swap! deps-state assoc-in [:resolved-status config-type] resolved?)
    (send-deps-status-update!)))

(defn handle-resolution-complete!
  "Handle dependency resolution completion from TypeScript"
  [message]
  (let [config-type (keyword (:config-type message))
        success? (:success message)
        output (:output message)
        error (:error message)]
    (log/info (str "🔧 [DEPS] Resolution complete for " (name config-type) ": " (if success? "success" "failed")))

    ;; Clear resolution flags
    (swap! deps-state assoc :current-resolution nil :resolution-in-progress false)

    ;; Update resolved status based on success/failure (FIXED: always update, not just on success)
    (swap! deps-state assoc-in [:resolved-status config-type] success?)

    ;; Send status to webview
    (vscode/send-webview-message!
     {:type :dependency-resolution-complete
      :config-type config-type
      :success success?
      :output output
      :error error})

    (send-deps-status-update!)))

(defn handle-file-change!
  "Handle file change notification for dependency configs"
  [message]
  (let [filename (:filename message)]
    (log/info (str "📁 [DEPS] Config file changed: " filename))
    (when (:auto-resolve-enabled @deps-state)
      (log/info "🔄 [DEPS] Auto-resolving dependencies due to config change...")

      ;; Clear previous detected configs and reset counters
      (swap! deps-state assoc
             :detected-configs []
             :pending-detection-count 0
             :auto-resolve-pending true)

      ;; Re-detect configs (this will set pending count and auto-resolve flag)
      (detect-project-configs!)

      ;; Don't call resolve-dependencies! here - it will be called automatically
      ;; when all detections are complete (handled in handle-config-detected!)
      (log/info "🔄 [DEPS] Config detection started, waiting for responses before auto-resolve..."))))

;; Status broadcasting

(defn send-deps-status-update!
  "Send dependency status update to webview and extension"
  []
  (let [status (get-overall-deps-status)
        summary (get-deps-summary)
        can-start? (can-start-repl?)
        detected (:detected-configs @deps-state)
        resolved (:resolved-status @deps-state)]

    ;; Update timestamp
    (swap! deps-state assoc :last-check-timestamp (js/Date.now))

    ;; Send to webview
    (vscode/send-webview-message!
     {:type :dependency-status-update
      :status status
      :summary summary
      :can-start-repl can-start?
      :detected-configs detected
      :resolved-status resolved})

    ;; Only log when there are actual changes or issues
    (when (or (= status :unknown) (= status :unresolved))
      (log/info (str "📊 [DEPS] Status update: " summary)))))

;; Public API

(defn initialize!
  "Initialize dependency system with automatic resolution"
  []
  (log/info "🚀 [DEPS] Initializing dependency management system...")
  (log/info "🔄 [DEPS] Will auto-resolve dependencies after detection to get real status")

  ;; Set auto-resolve flag so that when configs are detected, they are automatically resolved
  (swap! deps-state assoc :auto-resolve-pending true)

  ;; Start detection - this will trigger auto-resolve when complete
  (detect-project-configs!)

  ;; Remove the old superficial directory check - we'll get real status from resolution
  ;; (js/setTimeout check-all-dependencies-resolved! 1000)
  )

(defn get-state
  "Get current dependency state"
  []
  @deps-state)

(defn set-auto-resolve!
  "Enable/disable auto-resolve on file changes"
  [enabled?]
  (swap! deps-state assoc :auto-resolve-enabled enabled?)
  (log/info (str "🔄 Auto-resolve " (if enabled? "enabled" "disabled"))))