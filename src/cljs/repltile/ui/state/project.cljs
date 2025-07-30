(ns repltile.ui.state.project
  "Project identification and management for REPLTILE webview")

;; Current project identifier (will be set by VS Code)
(def current-project-id (atom nil))

(defn get-project-identifier []
  @current-project-id)

(defn set-project-identifier! [project-id]
  (let [old-project-id @current-project-id]
    (when (not= old-project-id project-id)
      (.log js/console "🔄 Project changed from:" old-project-id "to:" project-id)
      (reset! current-project-id project-id)
      ;; Return true if project actually changed
      (not= old-project-id project-id))))

(defn generate-project-key [base-key]
  (let [project-id (get-project-identifier)]
    (if project-id
      (str base-key "-" project-id)
      base-key)))

(defn clear-project-state! []
  (try
    (let [project-id (get-project-identifier)]
      (when project-id
        (.log js/console "🧹 Clearing state for project:" project-id)
        (let [session-key (generate-project-key "repltile-session")
              history-key (generate-project-key "repltile-history")]
          (.removeItem (.-localStorage js/window) session-key)
          (.removeItem (.-localStorage js/window) history-key)
          (.log js/console "✅ Project state cleared"))))
    (catch :default e
      (.error js/console "❌ Error clearing project state:" e))))

(defn request-project-info! []
  (when (.-vscode js/window)
    (.postMessage (.-vscode js/window) #js {:type "get-project-info"})))

(defn get-project-stats []
  (try
    (let [project-id (get-project-identifier)
          session-key (generate-project-key "repltile-session")
          history-key (generate-project-key "repltile-history")
          session-data (.getItem (.-localStorage js/window) session-key)
          history-data (.getItem (.-localStorage js/window) history-key)]
      {:project-id project-id
       :has-session (not (nil? session-data))
       :has-history (not (nil? history-data))
       :session-key session-key
       :history-key history-key})
    (catch :default e
      (.error js/console "❌ Error getting project stats:" e)
      {})))

(defn initialize-project! []
  (try
    (.log js/console "🏗️ Initializing project management...")
    (request-project-info!)
    (.log js/console "✅ Project management initialized")
    (catch :default e
      (.error js/console "❌ Error initializing project management:" e)))) 