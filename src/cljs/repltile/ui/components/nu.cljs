(ns repltile.ui.components.nu
  "Nu output processing utilities"
  (:require [clojure.string :as str]))

(defn is-nu-tap-message?
  "Check if text is a nu/tap message (contains 'nu' and '<=')"
  [text]
  (when text
    (let [text-str (str text)
          ;; Simplified check: just look for both patterns anywhere in the text
          has-nu? (or (str/includes? text-str " nu ")
                      (str/includes? text-str "nu ")
                      (str/includes? text-str "<span class='ansi-bold'> nu </span>")
                      (str/includes? text-str "[1m nu "))
          has-arrow? (or (str/includes? text-str "<=")
                         (str/includes? text-str "<span class='ansi-bold'><=</span>")
                         (str/includes? text-str "[1m<="))
          result (and has-nu? has-arrow?)]
      (.log js/console "🔍 [is-nu-tap-message?] Debug:")
      (.log js/console "  - Original text:" text-str)
      (.log js/console "  - Has nu?:" has-nu?)
      (.log js/console "  - Has arrow?:" has-arrow?)
      (.log js/console "  - Final result:" result)
      result)))

(defn process-nu-output
  [text last-executed-command]
  (when text
    (.log js/console "🔍 [process-nu-output] Called with text:" text)
    (let [text-str (str text)
          last-cmd last-executed-command
          is-nu-command? (and last-cmd (str/includes? last-cmd "nu/"))
          ;; Check for nu prefix in both plain text and HTML
          has-nu-prefix? (or (str/starts-with? (str/trim text-str) "nu ")
                             (str/includes? text-str "<span class='ansi-bold'> nu </span>")
                             (str/includes? text-str "> nu <"))
          is-nu-tap? (is-nu-tap-message? text-str)]

      (.log js/console "🔍 [process-nu-output] Analysis:")
      (.log js/console "  - Last command:" last-cmd)
      (.log js/console "  - Is nu command:" is-nu-command?)
      (.log js/console "  - Has nu prefix:" has-nu-prefix?)
      (.log js/console "  - Is nu/tap message:" is-nu-tap?)
      (.log js/console "  - Text (trimmed):" (str/trim text-str))

      (if (or is-nu-command? has-nu-prefix?)
        ;; This is nu output - process it specially
        (let [;; Simply look for the specific magenta background pattern that contains " nu "
              nu-magenta-pattern #"<span class='ansi-bg-magenta'><span class='ansi-bold'>\s*nu\s*</span></span>"
              nu-match (re-find nu-magenta-pattern text-str)]

          (.log js/console "🔍 [process-nu-output] Nu magenta pattern match:" nu-match)

          (if nu-match
            ;; Split the text at the nu pattern
            (let [nu-part (first nu-match)
                  ;; Find where the nu part ends and split there
                  nu-end-idx (+ (.indexOf text-str nu-part) (count nu-part))
                  before-and-nu (subs text-str 0 nu-end-idx)
                  after-nu (subs text-str nu-end-idx)
                  result (str "<span class='nu-output-prefix'>" before-and-nu "</span>"
                              "<span class='nu-output-content'>" after-nu "</span>")]

              (.log js/console "🎨 Processing nu/ output:")
              (.log js/console "  - Nu part:" nu-part)
              (.log js/console "  - Before and nu:" before-and-nu)
              (.log js/console "  - After nu:" after-nu)
              (.log js/console "  - Final result:" result)
              result)
            ;; If it's a nu command but doesn't have nu prefix, still wrap in nu-output-content
            (if is-nu-command?
              (let [result (str "<span class='nu-output-content'>" text-str "</span>")]
                (.log js/console "🔍 [process-nu-output] Wrapping in nu-output-content:" result)
                result)
              text-str)))
        ;; Not nu output - return as is
        (do
          (.log js/console "🔍 [process-nu-output] Not nu output, returning as is")
          text-str)))))