(ns repltile.ui.components.namespace
  "Namespace management for REPLTILE webview"
  (:require [clojure.string :as str]
            [repltile.core.state :as state]
            [repltile.ui.components.input :as input]
            [repltile.ui.components.output :as output]))

;; Namespace management functions
(defn display-namespace-change!
  "Display namespace change in output"
  [new-ns]
  (output/add-to-output
   (str "🔄 Switched to namespace: " new-ns)
   "repl-status"))

(defn update-namespace-ui!
  "Update UI elements when namespace changes"
  [new-ns]
  (let [ns-combo (.getElementById js/document "ns-combo")]
    (when ns-combo
      (set! (.-value ns-combo) new-ns))
    (input/update-input-placeholder!)
    (display-namespace-change! new-ns)))

(defn set-current-namespace!
  "Set the current namespace and update UI"
  [new-ns]
  (state/set-current-namespace! new-ns)
  (update-namespace-ui! new-ns)
  (.log js/console "📁 Namespace changed to:" new-ns))

(defn reload-current-namespace!
  "Reload current namespace"
  []
  (let [current-ns (state/get-current-namespace)]
    (if (and current-ns (not= current-ns "user"))
      (do
        (.log js/console "🔄 Reloading namespace:" current-ns)
        (output/add-to-output
         (str "🔄 Reloading namespace: " current-ns)
         "repl-status")
        (when (.-vscode js/window)
          (.postMessage (.-vscode js/window)
                        #js {:type "reload-namespace" :namespace current-ns})))
      (.log js/console "⚠️ Cannot reload user namespace"))))

(defn refresh-all-namespaces!
  "Refresh all changed namespaces"
  []
  (.log js/console "🔄 Refreshing all namespaces")
  (output/add-to-output
   "🔄 Refreshing all changed namespaces..."
   "repl-status")
  (when (.-vscode js/window)
    (.postMessage (.-vscode js/window)
                  #js {:type "refresh-all"})))

(defn update-namespace-buttons-state!
  "Update button states based on current namespace"
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
  "Update the namespace display field with current namespace from REPL"
  []
  (let [ns-combo (.getElementById js/document "ns-combo")]
    (when ns-combo
      (let [current-ns (state/get-current-namespace)]
        (.log js/console "🔧 Updating namespace display to:" current-ns)
        (set! (.-value ns-combo) (or current-ns "user"))
        (input/update-input-placeholder!)
        (update-namespace-buttons-state! current-ns)))))

(defn run-namespace-tests!
  "Run tests for current namespace"
  []
  (let [current-ns (state/get-current-namespace)]
    (if (and current-ns (not= current-ns "user"))
      (do
        (.log js/console "🧪 Running tests for namespace:" current-ns)
        (output/add-to-output
         (str "🧪 Running tests for namespace: " current-ns)
         "repl-status")
        (when (.-vscode js/window)
          (.postMessage (.-vscode js/window)
                        #js {:type "run-namespace-tests" :namespace current-ns})))
      (.log js/console "⚠️ Cannot run tests for user namespace"))))

(defn suggest-namespace
  "Suggest namespace based on file path"
  [file-path]
  (when file-path
    (let [normalized-path (str/replace file-path #"\\" "/")
          src-match (re-find #"src/(?:clj|cljs|cljc)?/?(.*)" normalized-path)
          path-without-src (if src-match (second src-match) normalized-path)
          namespace-path (-> path-without-src
                             (str/replace #"\.clj[sc]?$" "")
                             (str/replace #"/" ".")
                             (str/replace #"_" "-"))]
      (when (seq namespace-path)
        namespace-path))))

(defn validate-namespace-name
  "Validate namespace name"
  [ns-name]
  (when ns-name
    (and (string? ns-name)
         (not (str/blank? ns-name))
         (re-matches #"^[a-zA-Z][a-zA-Z0-9\-\.]*[a-zA-Z0-9]$" ns-name))))

(defn get-namespace-info
  "Get information about current namespace"
  []
  (let [current-ns (state/get-current-namespace)]
    {:current-namespace current-ns
     :is-user-namespace (= current-ns "user")
     :can-reload (and current-ns (not= current-ns "user"))
     :can-run-tests (and current-ns (not= current-ns "user"))}))

(defn setup-namespace-handlers!
  "Setup namespace-related button handlers"
  []
  (try
    (.log js/console "📁 Setting up namespace handlers...")

    ;; Reload namespace button
    (when-let [reload-btn (.getElementById js/document "reload-ns-btn")]
      (.addEventListener reload-btn "click"
                         (fn [_]
                           (.log js/console "🔄 Reload namespace button clicked")
                           (reload-current-namespace!))))

    ;; Refresh all button
    (when-let [refresh-btn (.getElementById js/document "refresh-all-btn")]
      (.addEventListener refresh-btn "click"
                         (fn [_]
                           (.log js/console "🔄 Refresh all button clicked")
                           (refresh-all-namespaces!))))

    ;; Run tests button
    (when-let [tests-btn (.getElementById js/document "run-ns-tests-btn")]
      (.addEventListener tests-btn "click"
                         (fn [_]
                           (.log js/console "🧪 Run tests button clicked")
                           (run-namespace-tests!))))

    (.log js/console "✅ Namespace handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up namespace handlers:" e))))

(defn initialize-namespace-management!
  "Initialize namespace management"
  []
  (try
    (.log js/console "📁 Initializing namespace management...")

    ;; Setup handlers
    (setup-namespace-handlers!)

    ;; Set initial namespace
    (set-current-namespace! "user")

    (.log js/console "✅ Namespace management initialized")
    (catch :default e
      (.error js/console "❌ Error initializing namespace management:" e)))) 