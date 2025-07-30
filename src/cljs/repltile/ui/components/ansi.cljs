(ns repltile.ui.components.ansi
  "ANSI color code processing for REPLTILE webview"
  (:require [clojure.string :as str]))

(defn process-ansi-codes
  "Convert ANSI color codes to HTML spans with CSS classes"
  [text]
  (when text
    (-> text
        ;; Remove ESC sequences we don't handle and clean up
        (str/replace #"\u001b\[[0-9;]*[a-zA-Z]"
                     (fn [match]
                       (let [code (str match)]
                         (cond
                           ;; Reset - close any open spans
                           (str/includes? code "0m") "</span>"
                           ;; Regular colors
                           (str/includes? code "30m") "<span class='ansi-black'>"
                           (str/includes? code "31m") "<span class='ansi-red'>"
                           (str/includes? code "32m") "<span class='ansi-green'>"
                           (str/includes? code "33m") "<span class='ansi-yellow'>"
                           (str/includes? code "34m") "<span class='ansi-blue'>"
                           (str/includes? code "35m") "<span class='ansi-magenta'>"
                           (str/includes? code "36m") "<span class='ansi-cyan'>"
                           (str/includes? code "37m") "<span class='ansi-white'>"
                           ;; Bright colors
                           (str/includes? code "90m") "<span class='ansi-bright-black'>"
                           (str/includes? code "91m") "<span class='ansi-bright-red'>"
                           (str/includes? code "92m") "<span class='ansi-bright-green'>"
                           (str/includes? code "93m") "<span class='ansi-bright-yellow'>"
                           (str/includes? code "94m") "<span class='ansi-bright-blue'>"
                           (str/includes? code "95m") "<span class='ansi-bright-magenta'>"
                           (str/includes? code "96m") "<span class='ansi-bright-cyan'>"
                           (str/includes? code "97m") "<span class='ansi-bright-white'>"
                           ;; Background colors
                           (str/includes? code "40m") "<span class='ansi-bg-black'>"
                           (str/includes? code "41m") "<span class='ansi-bg-red'>"
                           (str/includes? code "42m") "<span class='ansi-bg-green'>"
                           (str/includes? code "43m") "<span class='ansi-bg-yellow'>"
                           (str/includes? code "44m") "<span class='ansi-bg-blue'>"
                           (str/includes? code "45m") "<span class='ansi-bg-magenta'>"
                           (str/includes? code "46m") "<span class='ansi-bg-cyan'>"
                           (str/includes? code "47m") "<span class='ansi-bg-white'>"
                           ;; Text formatting
                           (str/includes? code "1m") "<span class='ansi-bold'>"
                           (str/includes? code "3m") "<span class='ansi-italic'>"
                           (str/includes? code "4m") "<span class='ansi-underline'>"
                           ;; Unknown codes - remove them
                           :else ""))))
        ;; Clean up any orphaned opening/closing spans
        (str/replace #"</span><span class='([^']+)'>" "<span class='$1'>")
        ;; Ensure we don't have orphaned closing spans at the beginning
        (str/replace #"^</span>" "")
        ;; Add closing span at the end if we have any open spans
        ((fn [result-text]
           (let [open-spans (count (re-seq #"<span class=" result-text))
                 close-spans (count (re-seq #"</span>" result-text))]
             (if (> open-spans close-spans)
               (str result-text "</span>")
               result-text)))))))

(defn add-ansi-styles!
  "Add ANSI color styles to the document"
  []
  (let [style-id "ansi-styles"
        existing-style (.getElementById js/document style-id)]

    ;; Remove existing styles
    (when existing-style
      (.remove existing-style))

    ;; Add new styles
    (let [style (.createElement js/document "style")]
      (set! (.-id style) style-id)
      (set! (.-textContent style)
            "
/* ANSI Color Support */
.repl-ansi-output {
  font-family: inherit;
  white-space: pre-wrap;
  margin: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(34, 197, 94, 0.05);
  border-left: 3px solid #22c55e;
  overflow-x: auto;
}

.dark-theme .repl-ansi-output, body[data-theme='dark'] .repl-ansi-output {
  background: rgba(34, 197, 94, 0.08);
}

/* ANSI color classes */
.ansi-black { color: #000000; }
.ansi-red { color: #ff6b6b; }
.ansi-green { color: #51cf66; }
.ansi-yellow { color: #ffd43b; }
.ansi-blue { color: #74c0fc; }
.ansi-magenta { color: #da77f2; }
.ansi-cyan { color: #22d3ee; }
.ansi-white { color: #ffffff; }

.ansi-bright-black { color: #666666; }
.ansi-bright-red { color: #ff8787; }
.ansi-bright-green { color: #69db7c; }
.ansi-bright-yellow { color: #ffe066; }
.ansi-bright-blue { color: #91a7ff; }
.ansi-bright-magenta { color: #e599f7; }
.ansi-bright-cyan { color: #66d9ef; }
.ansi-bright-white { color: #ffffff; }

.ansi-bg-black { background-color: #000000; }
.ansi-bg-red { background-color: #ff6b6b; }
.ansi-bg-green { background-color: #51cf66; }
.ansi-bg-yellow { background-color: #ffd43b; }
.ansi-bg-blue { background-color: #74c0fc; }
.ansi-bg-magenta { background-color: #da77f2; }
.ansi-bg-cyan { background-color: #22d3ee; }
.ansi-bg-white { background-color: #ffffff; }

.ansi-bold { font-weight: bold; }
.ansi-italic { font-style: italic; }
.ansi-underline { text-decoration: underline; }
")
      (.appendChild (.-head js/document) style)))) 