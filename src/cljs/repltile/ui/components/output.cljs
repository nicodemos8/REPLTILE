(ns repltile.ui.components.output
  "Output management for REPLTILE webview"
  (:require [clojure.string :as str]
            [repltile.ui.components.ansi :as ansi]))

(defn highlight-clojure-syntax [code]
  (-> code
      ;; 1. Comments (first, to avoid interfering with other patterns)
      (str/replace #";[^\r\n]*" "<span class='clj-comment'>$&</span>")

      ;; 2. Strings (second, to avoid interfering with keywords inside strings)
      (str/replace #"\"(?:[^\"\\\\]|\\\\.)*\"" "<span class='clj-string'>$&</span>")

      ;; 3. Keywords - :keyword or :keyword-with-dashes (must come before symbols)
      (str/replace #"(?<=[\s\(\[\{,]|^):[a-zA-Z][a-zA-Z0-9_\-]*(?=[\s\)\]\},]|$)" "<span class='clj-keyword'>$&</span>")

      ;; 4. Numbers - including decimals with both dot and comma separators (signs must be followed by digit)
      (str/replace #"(?<=[\s\(\[\{,]|^)(?:[\+\-]\d+[.,]?\d*|\d+[.,]?\d*|\.\d+|,\d+)(?:[eE][\+\-]?\d+)?(?:[MN]|r\d+)?s?(?=[\s\)\]\},]|$)" "<span class='clj-number'>$&</span>")

      ;; 5. Boolean literals and nil - before special forms to avoid conflicts
      (str/replace #"(?<=[\s\(\[\{,]|^)(?:true|false|nil)(?=[\s\)\]\},]|$)" "<span class='clj-literal'>$&</span>")

      ;; 6. Special forms - only complete words with proper boundaries
      (str/replace #"(?<=[\s\(\[\{]|^)(?:def|defn|defn-|defmacro|defmulti|defmethod|defstruct|defonce|let|letfn|binding|if|if-not|when|when-not|cond|condp|case|do|loop|recur|fn|try|catch|finally|throw|ns|in-ns|require|use|import|refer)(?=[\s\)\]\}]|$)" "<span class='clj-special'>$&</span>")

      ;; 7. Common core functions - only complete words with proper boundaries  
      (str/replace #"(?<=[\s\(\[\{]|^)(?:map|mapv|reduce|filter|remove|keep|apply|comp|partial|juxt|complement|constantly|identity|get|get-in|assoc|assoc-in|dissoc|update|update-in|conj|cons|into|concat|first|ffirst|second|last|rest|next|take|drop|nth|count|empty\\?|seq|vec|list|set|hash-map|nil\\?|some\\?|every\\?|not|and|or|true\\?|false\\?|zero\\?|pos\\?|neg\\?|even\\?|odd\\?)(?=[\s\)\]\}]|$)" "<span class='clj-function'>$&</span>")

      ;; 8. Brackets and delimiters
      (str/replace #"[\(\)\[\]\{\}]" "<span class='clj-bracket'>$&</span>")))

(defn clean-unrepl-display-text
  [text]
  (when text
    (-> text
        ;; Convert #unrepl/bad-keyword [nil "keyword-name"] back to :keyword-name
        (str/replace #"#unrepl/bad-keyword \[nil \"([^\"]+)\"\]" ":$1")
        ;; Convert #unrepl/bad-symbol [nil "symbol-name"] back to symbol-name  
        (str/replace #"#unrepl/bad-symbol \[nil \"([^\"]+)\"\]" "$1")
        ;; Remove escaped quotes - convert \" back to "
        (str/replace #"\\\"" "\"")
        ;; Add line breaks after ", " for better object formatting
        (str/replace #", " ",\n")
        ;; Add line breaks between different data structures for better visualization
        (str/replace #"\} \{" "} \n{")  ; map to map
        (str/replace #"\] \[" "] \n[")  ; vector to vector
        (str/replace #"\) \(" ") \n(")  ; list to list
        (str/replace #"\} \[" "} \n[")  ; map to vector
        (str/replace #"\] \{" "] \n{")  ; vector to map
        (str/replace #"\} \(" "} \n(")  ; map to list
        (str/replace #"\) \{" ") \n{")  ; list to map
        (str/replace #"\) \[" ") \n[")  ; list to vector
        (str/replace #"\[ \(" "[ \n(")  ; vector to list
        ;; Also handle cases without space between structures
        (str/replace #"\}\{" "}\n{")    ; map to map (no space)
        (str/replace #"\]\[" "]\n[")    ; vector to vector (no space)
        (str/replace #"\)\(" ")\n(")    ; list to list (no space)
        (str/replace #"\}\[" "}\n[")    ; map to vector (no space)
        (str/replace #"\]\{" "]\n{")    ; vector to map (no space)
        (str/replace #"\}\(" "}\n(")    ; map to list (no space)
        (str/replace #"\)\{" ")\n{")    ; list to map (no space)
        (str/replace #"\)\[" ")\n[")    ; list to vector (no space)
        (str/replace #"\[\(" "[\n(")    ; vector to list (no space)
        )))

(defn add-to-output
  [content css-class]
  (try
    (let [output-div (.getElementById js/document "repl-output")]
      (when output-div
        (let [new-div (.createElement js/document "div")]
          (when (and css-class (not (str/blank? css-class)))
            (.add (.-classList new-div) css-class))
          ;; Apply ANSI processing and syntax highlighting for output
          (let [processed-content (-> content
                                      str
                                      ;; First process ANSI codes
                                      ansi/process-ansi-codes
                                      ;; Apply Clojure syntax highlighting for repl-result and repl-stdout
                                      ((fn [text]
                                         (if (and (string? text)
                                                  (or (= css-class "repl-result")
                                                      (= css-class "repl-stdout")))
                                           (highlight-clojure-syntax text)
                                           text))))]
            (set! (.-innerHTML new-div)
                  (str/replace processed-content #"\n" "<br>")))
          (.appendChild output-div new-div)
          (set! (.-scrollTop output-div) (.-scrollHeight output-div)))))
    (catch js/Error e
      (.error js/console "Error adding to output:" e))))

(defn clear-console
  []
  (try
    (.log js/console "🗑️ Clearing console...")
    (let [output-div (.getElementById js/document "repl-output")]
      (when output-div
        (set! (.-innerHTML output-div) "")))
    (catch :default e
      (.error js/console "Error clearing console:" e))))

(defn scroll-output-to-top! []
  (when-let [output-el (.getElementById js/document "repl-output")]
    (set! (.-scrollTop output-el) 0)))

(defn scroll-output-to-bottom! []
  (when-let [output-el (.getElementById js/document "repl-output")]
    (set! (.-scrollTop output-el) (.-scrollHeight output-el))))

(defn add-syntax-highlighting-styles!
  []
  (let [style-id "syntax-highlighting-styles"
        existing-style (.getElementById js/document style-id)]

    ;; Remove existing styles
    (when existing-style
      (.remove existing-style))

    ;; Add new styles
    (let [style (.createElement js/document "style")]
      (set! (.-id style) style-id)
      (set! (.-textContent style)
            "
/* Clojure Syntax Highlighting (for OUTPUT only) */
.clj-keyword { color: #0891b2; font-weight: 600; }
.clj-string { color: #059669; }
.clj-number { color: #dc2626; }
.clj-comment { color: #6b7280; font-style: italic; }
.clj-function { color: #2563eb; font-weight: 500; }
.clj-special { color: #7c3aed; font-weight: 600; }
.clj-bracket { color: #4b5563; font-weight: bold; }
.clj-symbol { color: #374151; }
.clj-literal { color: #0369a1; font-weight: 600; }

.dark-theme .clj-keyword, body[data-theme='dark'] .clj-keyword { color: #22d3ee; }
.dark-theme .clj-string, body[data-theme='dark'] .clj-string { color: #34d399; }
.dark-theme .clj-number, body[data-theme='dark'] .clj-number { color: #f87171; }
.dark-theme .clj-comment, body[data-theme='dark'] .clj-comment { color: #9ca3af; }
.dark-theme .clj-function, body[data-theme='dark'] .clj-function { color: #60a5fa; }
.dark-theme .clj-special, body[data-theme='dark'] .clj-special { color: #c084fc; }
.dark-theme .clj-bracket, body[data-theme='dark'] .clj-bracket { color: #d1d5db; }
.dark-theme .clj-symbol, body[data-theme='dark'] .clj-symbol { color: #e5e7eb; }
.dark-theme .clj-literal, body[data-theme='dark'] .clj-literal { color: #0ea5e9; }

/* Additional output styles */
.input, .repl-result, .repl-command, .repl-ansi-output, .repl-stdout {
  background: rgba(32, 26, 46, 0.025);
}
.dark-theme .input, body[data-theme='dark'] .input,
.dark-theme .repl-result, body[data-theme='dark'] .repl-result,
.dark-theme .repl-command, body[data-theme='dark'] .repl-command,
.dark-theme .repl-ansi-output, body[data-theme='dark'] .repl-ansi-output,
.dark-theme .repl-stdout, body[data-theme='dark'] .repl-stdout {
  background: rgba(255,255,255,0.025);
}
.repl-error, .repl-stderr {
  background: rgba(239, 68, 68, 0.025);
}
.dark-theme .repl-error, body[data-theme='dark'] .repl-error,
.dark-theme .repl-stderr, body[data-theme='dark'] .repl-stderr {
  background: rgba(239, 68, 68, 0.045);
}
.repl-status, .repl-loading {
  background: rgba(245, 158, 11, 0.025);
}
.dark-theme .repl-status, body[data-theme='dark'] .repl-status,
.dark-theme .repl-loading, body[data-theme='dark'] .repl-loading {
  background: rgba(251, 191, 36, 0.045);
}
.repl-help {
  background: rgba(5, 150, 105, 0.025);
}
.dark-theme .repl-help, body[data-theme='dark'] .repl-help {
  background: rgba(16, 185, 129, 0.045);
}
")
      (.appendChild (.-head js/document) style)))) 