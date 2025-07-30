(ns repltile.ui.components.syntax
  "Syntax highlighting utilities for Clojure code"
  (:require [clojure.string :as str]))

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