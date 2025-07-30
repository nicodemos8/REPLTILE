(ns repltile.ui.state.history
  "Command history management for REPLTILE webview - Cursive IDE style"
  (:require [clojure.string :as str]
            [repltile.ui.state.project :as project]))

;; Command history state - Cursive IDE style
(def command-history (atom []))
(def history-index (atom -1))
(def last-input-content (atom "")) ; Track last input to detect manual changes

;; Cursive-style behavior:
;; - Arrow Up: go backwards in history (most recent first)
;; - Arrow Down: go forwards in history  
;; - If user modifies input manually, reset index to -1
;; - History is per-project and saved to .REPLTILE folder

(defn create-repltile-directory! []
  (when (.-vscode js/window)
    (.postMessage (.-vscode js/window) #js {:type "create-repltile-directory"})))

(defn save-command-history-to-file! [commands]
  (try
    (let [commands-vec (vec (take-last 100 commands)) ; Keep last 100 commands
          history-data {:commands commands-vec
                        :timestamp (js/Date.now)
                        :project (project/get-project-identifier)}
          project-id (project/get-project-identifier)]
      (when (.-vscode js/window)
        (.postMessage (.-vscode js/window)
                      #js {:type "save-history-file"
                           :data (clj->js history-data)
                           :project-id project-id}))
      (.log js/console "📜 Command history saved to file:" (count commands-vec) "commands for project:" project-id))
    (catch :default e
      (.error js/console "❌ Error saving command history to file:" e))))

(defn load-command-history-from-file! []
  (try
    (let [project-id (project/get-project-identifier)]
      (when (and (.-vscode js/window) project-id)
        (.postMessage (.-vscode js/window)
                      #js {:type "load-history-file"
                           :project-id project-id}))
      (.log js/console "📜 Requested command history load for project:" project-id))
    (catch :default e
      (.error js/console "❌ Error requesting command history load:" e))))

(defn clean-command-list [commands]
  (->> commands
       (map str/trim)
       (filter #(and (not (str/blank? %))
                     (> (count %) 0)
                     ;; Don't keep incomplete commands
                     (not (re-find #"^\s*[)\]}]\s*$" %))
                     ;; Don't keep commands that are just quotes
                     (not (re-matches #"^\s*\".*\"\s*$" %))))
       (distinct)
       (vec)))

(defn handle-history-file-loaded [commands]
  (try
    (let [raw-commands (or commands [])
          cleaned-commands (clean-command-list raw-commands)
          invalid-count (- (count raw-commands) (count cleaned-commands))]
      (reset! command-history cleaned-commands)
      (reset! history-index -1)
      (.log js/console "📜 Command history loaded from file:" (count cleaned-commands) "valid commands")
      (when (> invalid-count 0)
        (.log js/console "🧹 Cleaned" invalid-count "invalid commands from history")
        ;; Save the cleaned history back to file
        (save-command-history-to-file! cleaned-commands)))
    (catch :default e
      (.error js/console "❌ Error handling loaded history:" e))))

(defn add-to-command-history! [command]
  (let [trimmed-command (str/trim (str command))]
    (when (and (not (str/blank? trimmed-command))
               (> (count trimmed-command) 0)
               ;; Don't add incomplete commands (missing closing parens/brackets/braces)
               (not (re-find #"^\s*[)\]}]\s*$" trimmed-command))
               ;; Don't add commands that are just quotes
               (not (re-matches #"^\s*\".*\"\s*$" trimmed-command))
               ;; Must be different from last command
               (not= trimmed-command (last @command-history)))
      (swap! command-history conj trimmed-command)
      ;; Keep only last 100 commands
      (when (> (count @command-history) 100)
        (reset! command-history (vec (take-last 100 @command-history))))
      ;; Reset to start position (most recent = index 0 when going up)
      (reset! history-index -1)
      ;; Save to file
      (save-command-history-to-file! @command-history)
      (.log js/console "📜 Added to history:" trimmed-command "| Total:" (count @command-history)))))

(defn reset-history-index! []
  (reset! history-index -1)
  (.log js/console "📜 History index reset due to manual input change"))

(defn navigate-history! [direction]
  (let [history @command-history
        current-idx @history-index
        history-size (count history)]
    (if (zero? history-size)
      "" ; No history available
      (let [new-idx (case direction
                      :up
                      ;; Going up in history (backwards in time, towards older commands)
                      (cond
                        (= current-idx -1) 0 ; Start from most recent
                        (< current-idx (dec history-size)) (inc current-idx) ; Go to older command
                        :else current-idx) ; Stay at oldest

                      :down
                      ;; Going down in history (forwards in time, towards newer commands)  
                      (cond
                        (<= current-idx 0) -1 ; Go to "no history" state
                        :else (dec current-idx)) ; Go to newer command

                      current-idx)]

        (reset! history-index new-idx)
        (.log js/console "📜 Navigate" direction "| Index:" current-idx "->" new-idx "| Size:" history-size)

        (if (>= new-idx 0)
          ;; Return command from history (history[0] = oldest, history[n-1] = newest)
          ;; Index 0 in navigation = most recent = history[n-1]
          (let [history-pos (- history-size 1 new-idx)
                command (nth history history-pos)]
            (reset! last-input-content command)
            (.log js/console "📜 Returning command at pos" history-pos ":" command)
            command)
          ;; Index -1 = empty input
          (do
            (reset! last-input-content "")
            (.log js/console "📜 Returning empty (no history position)")
            ""))))))

(defn check-input-change! [current-input]
  (let [last-content @last-input-content]
    (when (and (not= current-input last-content)
               (>= @history-index 0)) ; Only reset if we were in history mode
      (.log js/console "📜 Input changed manually:" last-content "->" current-input)
      (reset-history-index!)
      (reset! last-input-content current-input))))

(defn clear-command-history! []
  (try
    (reset! command-history [])
    (reset! history-index -1)
    (reset! last-input-content "")
    (let [project-id (project/get-project-identifier)]
      (when (.-vscode js/window)
        (.postMessage (.-vscode js/window)
                      #js {:type "clear-history-file"
                           :project-id project-id}))
      (.log js/console "🧹 Command history cleared for project:" project-id))
    (catch :default e
      (.error js/console "❌ Error clearing command history:" e))))

(defn fix-corrupted-history! []
  (try
    (let [cleaned-commands (clean-command-list @command-history)
          removed-count (- (count @command-history) (count cleaned-commands))]
      (reset! command-history cleaned-commands)
      (reset! history-index -1)
      (save-command-history-to-file! cleaned-commands)
      (.log js/console "🔧 Fixed corrupted history - removed" removed-count "invalid commands")
      (.log js/console "✅ History now contains" (count cleaned-commands) "valid commands"))
    (catch :default e
      (.error js/console "❌ Error fixing corrupted history:" e))))

(defn get-history-stats []
  {:total-commands (count @command-history)
   :current-index @history-index
   :project (project/get-project-identifier)
   :in-history-mode (>= @history-index 0)
   :last-input @last-input-content})

(defn initialize-history! []
  (try
    (.log js/console "📜 Initializing command history...")
    (reset! command-history [])
    (reset! history-index -1)
    (reset! last-input-content "")
    ;; Request history from file
    (load-command-history-from-file!)
    (.log js/console "📜 History initialization requested for project:" (project/get-project-identifier))
    (catch :default e
      (.error js/console "❌ Error initializing history:" e))))

(defn reset-history-for-project! []
  (reset! command-history [])
  (reset! history-index -1)
  (reset! last-input-content "")
  (.log js/console "🔄 Command history reset for project change"))

;; Export main functions for webview integration
(defn get-command-history [] @command-history)
(defn get-history-index [] @history-index)
(defn is-in-history-mode? [] (>= @history-index 0)) 