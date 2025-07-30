(ns repltile.ui.state.session
  "Session persistence management for REPLTILE webview"
  (:require [repltile.ui.state.project :as project]))

(defn save-session-state! []
  (try
    (let [output-el (.getElementById js/document "repl-output")
          output-content (when output-el (.-innerHTML output-el))
          splitter-height (when-let [input-section (.getElementById js/document "repl-input-section")]
                            (.-height (.-style input-section)))
          session-data (clj->js {:output-content output-content
                                 :splitter-height splitter-height
                                 :timestamp (js/Date.now)})
          session-key (project/generate-project-key "repltile-session")]
      (.setItem (.-localStorage js/window) session-key (.stringify js/JSON session-data))
      (.log js/console "💾 Session state saved for project:" (project/get-project-identifier)))
    (catch :default e
      (.error js/console "❌ Error saving session state:" e))))

(defn load-session-state! []
  (try
    (let [session-key (project/generate-project-key "repltile-session")]
      (when-let [saved-data (.getItem (.-localStorage js/window) session-key)]
        (let [session-data (js->clj (.parse js/JSON saved-data) :keywordize-keys true)
              output-el (.getElementById js/document "repl-output")
              input-section (.getElementById js/document "repl-input-section")]
          ;; Restore output content
          (when (and output-el (:output-content session-data))
            (set! (.-innerHTML output-el) (:output-content session-data))
            (set! (.-scrollTop output-el) (.-scrollHeight output-el)))
          ;; Restore splitter height
          (when (and input-section (:splitter-height session-data))
            (set! (.. input-section -style -height) (:splitter-height session-data)))
          (.log js/console "🔄 Session state restored for project:" (project/get-project-identifier) "from" (js/Date. (:timestamp session-data))))))
    (catch :default e
      (.error js/console "❌ Error loading session state:" e))))

(defn setup-auto-save! []
  (let [save-throttled (atom nil)]
    ;; Save on output changes
    (when-let [output-el (.getElementById js/document "repl-output")]
      (let [observer (js/MutationObserver.
                      (fn [_mutations _observer]
                        (when @save-throttled
                          (js/clearTimeout @save-throttled))
                        (reset! save-throttled
                                (js/setTimeout save-session-state! 1000))))]
        (.observe observer output-el #js {:childList true :subtree true})))

    ;; Save on window events
    (.addEventListener js/window "beforeunload" save-session-state!)
    (.addEventListener js/window "visibilitychange"
                       (fn [] (when (= "hidden" (.-visibilityState js/document))
                                (save-session-state!))))

    ;; Save periodically
    (js/setInterval save-session-state! 30000))) ; Save every 30 seconds

(defn clear-all-persistence! []
  (try
    (let [session-key (project/generate-project-key "repltile-session")
          history-key (project/generate-project-key "repltile-history")]
      (.removeItem (.-localStorage js/window) session-key)
      (.removeItem (.-localStorage js/window) history-key)
      (.log js/console "🧹 All persistent data cleared for project:" (project/get-project-identifier)))
    (catch :default e
      (.error js/console "❌ Error clearing persistent data:" e))))

(defn export-session-data []
  (try
    (let [session-key (project/generate-project-key "repltile-session")
          history-key (project/generate-project-key "repltile-history")
          session-data (.getItem (.-localStorage js/window) session-key)
          history-data (.getItem (.-localStorage js/window) history-key)
          export-data (clj->js {:session session-data
                                :history history-data
                                :project-id (project/get-project-identifier)
                                :timestamp (js/Date.now)})]
      (.stringify js/JSON export-data))
    (catch :default e
      (.error js/console "❌ Error exporting session data:" e)
      nil)))

(defn import-session-data! [json-data]
  (try
    (let [import-data (js->clj (.parse js/JSON json-data) :keywordize-keys true)
          session-key (project/generate-project-key "repltile-session")
          history-key (project/generate-project-key "repltile-history")]
      (when (:session import-data)
        (.setItem (.-localStorage js/window) session-key (:session import-data)))
      (when (:history import-data)
        (.setItem (.-localStorage js/window) history-key (:history import-data)))
      (.log js/console "📥 Session data imported successfully for project:" (project/get-project-identifier))
      true)
    (catch :default e
      (.error js/console "❌ Error importing session data:" e)
      false)))

(defn get-session-stats []
  (try
    (let [session-key (project/generate-project-key "repltile-session")
          history-key (project/generate-project-key "repltile-history")
          session-data (.getItem (.-localStorage js/window) session-key)
          history-data (.getItem (.-localStorage js/window) history-key)
          total-size (+ (count (or session-data "")) (count (or history-data "")))
          session-parsed (when session-data (js->clj (.parse js/JSON session-data) :keywordize-keys true))
          history-parsed (when history-data (js->clj (.parse js/JSON history-data) :keywordize-keys true))]
      {:project-id (project/get-project-identifier)
       :has-session (not (nil? session-data))
       :has-history (not (nil? history-data))
       :total-size-bytes total-size
       :session-timestamp (when session-parsed (:timestamp session-parsed))
       :history-timestamp (when history-parsed (:timestamp history-parsed))
       :history-commands-count (when history-parsed (count (:commands history-parsed)))})
    (catch :default e
      (.error js/console "❌ Error getting session stats:" e)
      {})))

(defn setup-splitter-persistence! []
  (let [splitter-el (.getElementById js/document "repl-splitter")
        input-section (.getElementById js/document "repl-input-section")
        output-section (.getElementById js/document "repl-output-section")
        main-el (when input-section (.-parentElement input-section))]
    (when (and splitter-el input-section output-section main-el)
      (let [is-dragging (atom false)
            start-y (atom 0)
            start-input-height (atom 0)
            start-output-height (atom 0)]

        ;; Mouse down on splitter
        (.addEventListener splitter-el "mousedown"
                           (fn [e]
                             (.log js/console "📐 Splitter mousedown")
                             (.preventDefault e)
                             (reset! is-dragging true)
                             (reset! start-y (.-clientY e))
                             (reset! start-input-height (.-offsetHeight input-section))
                             (reset! start-output-height (.-offsetHeight output-section))
                             (.add (.-classList (.-body js/document)) "no-select")))

        ;; Mouse move
        (.addEventListener js/document "mousemove"
                           (fn [e]
                             (when @is-dragging
                               (.preventDefault e)
                               (let [delta-y (- (.-clientY e) @start-y)
                                     main-height (.-offsetHeight main-el)
                                     controls-height (.-offsetHeight (.getElementById js/document "repl-controls-section"))
                                     min-input-height 80
                                     min-output-height 150
                                     available-height (- main-height controls-height)
                                     max-input-height (- available-height min-output-height)
                                     new-input-height (max min-input-height
                                                           (min max-input-height
                                                                (+ @start-input-height delta-y)))
                                     ;; Extra safety: ensure input never exceeds safe maximum
                                     safe-input-height (min new-input-height (- main-height controls-height 20))
                                     input-percentage (* (/ safe-input-height main-height) 100)
                                     height-str (str input-percentage "%")]
                                 (.log js/console "📐 Splitter resize - main:" main-height "controls:" controls-height "safe-input:" safe-input-height "percentage:" input-percentage)
                                 (set! (.. input-section -style -height) height-str)))))

        ;; Mouse up
        (.addEventListener js/document "mouseup"
                           (fn [_]
                             (when @is-dragging
                               (.log js/console "📐 Splitter mouseup")
                               (reset! is-dragging false)
                               (.remove (.-classList (.-body js/document)) "no-select")
                               ;; Save splitter position when done resizing
                               (save-session-state!))))

        ;; Touch events for mobile support
        (.addEventListener splitter-el "touchstart"
                           (fn [e]
                             (.preventDefault e)
                             (reset! is-dragging true)
                             (reset! start-y (.-clientY (aget (.-touches e) 0)))
                             (reset! start-input-height (.-offsetHeight input-section))
                             (reset! start-output-height (.-offsetHeight output-section))))

        (.addEventListener js/document "touchmove"
                           (fn [e]
                             (when @is-dragging
                               (.preventDefault e)
                               (let [delta-y (- (.-clientY (aget (.-touches e) 0)) @start-y)
                                     main-height (.-offsetHeight main-el)
                                     controls-height (.-offsetHeight (.getElementById js/document "repl-controls-section"))
                                     min-input-height 80
                                     min-output-height 150
                                     available-height (- main-height controls-height)
                                     max-input-height (- available-height min-output-height)
                                     new-input-height (max min-input-height
                                                           (min max-input-height
                                                                (+ @start-input-height delta-y)))
                                     ;; Extra safety: ensure input never exceeds safe maximum
                                     safe-input-height (min new-input-height (- main-height controls-height 20))
                                     input-percentage (* (/ safe-input-height main-height) 100)]
                                 (set! (.. input-section -style -height) (str input-percentage "%"))))))

        (.addEventListener js/document "touchend"
                           (fn [_]
                             (when @is-dragging
                               (reset! is-dragging false)
                               ;; Save splitter position when done resizing on mobile
                               (save-session-state!))))))))

(defn initialize-session! []
  (try
    (.log js/console "💾 Initializing session management...")

    ;; Load existing session state
    (load-session-state!)

    ;; Setup automatic saving
    (setup-auto-save!)

    ;; Setup splitter persistence
    (setup-splitter-persistence!)

    (.log js/console "✅ Session management initialized")
    (catch :default e
      (.error js/console "❌ Error initializing session management:" e)))) 