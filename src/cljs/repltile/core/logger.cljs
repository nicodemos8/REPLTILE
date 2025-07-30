(ns repltile.core.logger
  "Functional logging system for REPLTILE"
  (:require [clojure.string :as str]))

;; Log levels
(def log-levels
  {:debug 0
   :info 1
   :warn 2
   :error 3})

;; Current log level (can be configured)
(def current-level (atom :debug))

(defn set-log-level!
  "Set the current log level"
  [level]
  (reset! current-level level))

(defn should-log?
  "Check if message should be logged based on level"
  [level]
  (>= (get log-levels level 0)
      (get log-levels @current-level 0)))

(defn format-timestamp
  "Format current timestamp"
  []
  (.toISOString (js/Date.)))

(defn format-log-message
  "Format log message with timestamp and level"
  [level & messages]
  (let [timestamp (format-timestamp)
        level-str (str/upper-case (name level))
        message-str (str/join " " (map str messages))]
    (str "[" timestamp "] [" level-str "] " message-str)))

(defn log!
  "Core logging function - using console.log for browser compatibility"
  [level & messages]
  (when (should-log? level)
    (let [formatted (apply format-log-message level messages)]
      (cond
        (= level :error)
        (js/console.error formatted)
        (= level :warn)
        (js/console.warn formatted)
        (= level :debug)
        (js/console.debug formatted)
        :else
        (js/console.log formatted)))))

;; Convenience functions
(defn debug [& messages] (apply log! :debug messages))
(defn info [& messages] (apply log! :info messages))
(defn warn [& messages] (apply log! :warn messages))
(defn error [& messages] (apply log! :error messages))

;; Special logging for repl-tooling integration
(defn repl-tooling [level message]
  (log! level "🔧 [repl-tooling]" message))

;; Special logging for Calva integration  
(defn calva [level message]
  (log! level "🍃 [Calva]" message))

;; Special logging for webview
(defn webview [level message]
  (log! level "🌐 [WebView]" message)) 