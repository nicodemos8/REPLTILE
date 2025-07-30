(ns repltile.ui.components.brackets
  "Bracket auto-completion for REPLTILE webview")

;; Bracket matching pairs
(def bracket-pairs
  {"(" ")"
   "[" "]"
   "{" "}"})

(defn find-matching-bracket [text cursor-pos]
  (when (and (>= cursor-pos 0) (< cursor-pos (count text)))
    (let [char-at-cursor (str (nth text cursor-pos))
          matching-char (get bracket-pairs char-at-cursor)]
      (when matching-char
        (let [is-opening? (contains? #{"(" "[" "{"} char-at-cursor)
              _direction (if is-opening? 1 -1)
              step (if is-opening? 1 -1)]
          (loop [pos (+ cursor-pos step)
                 level 1]
            (cond
              (or (< pos 0) (>= pos (count text))) nil
              (= level 0) (- pos step)
              :else
              (let [current-char (str (nth text pos))]
                (cond
                  (= current-char char-at-cursor) (recur (+ pos step) (inc level))
                  (= current-char matching-char) (recur (+ pos step) (dec level))
                  :else (recur (+ pos step) level))))))))))

(defn auto-close-brackets! [input-element event]
  (when (not (.-defaultPrevented event)) ; Only process if event hasn't been handled
    (let [key (.-key event)
          cursor-pos (.-selectionStart input-element)
          current-value (.-value input-element)
          char-after-cursor (when (< cursor-pos (count current-value))
                              (str (nth current-value cursor-pos)))
          has-selection? (not= (.-selectionStart input-element) (.-selectionEnd input-element))]
      (cond
        ;; Opening brackets - auto-close them (only if no text is selected)
        (and (contains? #{"(" "[" "{"} key) (not has-selection?))
        (let [closing-bracket (get bracket-pairs key)
              before-cursor (subs current-value 0 cursor-pos)
              after-cursor (subs current-value cursor-pos)]
          ;; Only auto-close if appropriate context
          (when (or (nil? char-after-cursor)
                    (contains? #{")" "]" "}" " " "\n" "\t" "\r"} char-after-cursor)
                    (= cursor-pos (count current-value))) ; end of input
            ;; Prevent default to avoid double insertion
            (.preventDefault event)
            ;; Insert both opening and closing bracket
            (set! (.-value input-element) (str before-cursor key closing-bracket after-cursor))
            ;; Position cursor between brackets
            (set! (.-selectionStart input-element) (inc cursor-pos))
            (set! (.-selectionEnd input-element) (inc cursor-pos))
            ;; Return true to indicate bracket was auto-closed
            true))

        ;; Closing brackets - skip over them if they match
        (and (contains? #{")" "]" "}"} key) (not has-selection?))
        (when (= key char-after-cursor)
          ;; Prevent default insertion
          (.preventDefault event)
          ;; Skip over the existing closing bracket
          (set! (.-selectionStart input-element) (inc cursor-pos))
          (set! (.-selectionEnd input-element) (inc cursor-pos))
          ;; Return true to indicate bracket was skipped
          true)

        ;; Return false if no bracket operation was performed
        :else false))))

(defn is-bracket-pair? [open-char close-char]
  (= (get bracket-pairs open-char) close-char))

(defn balance-brackets [text]
  (let [brackets (filter #(contains? (set (concat (keys bracket-pairs) (vals bracket-pairs))) %) text)]
    (loop [stack []
           remaining brackets]
      (if (empty? remaining)
        (empty? stack)
        (let [char (first remaining)]
          (cond
            (contains? (set (keys bracket-pairs)) char)
            (recur (conj stack char) (rest remaining))

            (contains? (set (vals bracket-pairs)) char)
            (if (and (seq stack) (= (get bracket-pairs (peek stack)) char))
              (recur (pop stack) (rest remaining))
              false)

            :else (recur stack (rest remaining))))))))

(defn get-bracket-context [text cursor-pos]
  (let [balanced? (balance-brackets text)
        char-at-cursor (when (and (>= cursor-pos 0) (< cursor-pos (count text)))
                         (str (nth text cursor-pos)))
        char-before-cursor (when (> cursor-pos 0)
                             (str (nth text (dec cursor-pos))))
        is-opening-bracket? (contains? (set (keys bracket-pairs)) char-at-cursor)
        is-closing-bracket? (contains? (set (vals bracket-pairs)) char-at-cursor)
        matching-pos (when (or is-opening-bracket? is-closing-bracket?)
                       (find-matching-bracket text cursor-pos))]
    {:balanced? balanced?
     :char-at-cursor char-at-cursor
     :char-before-cursor char-before-cursor
     :is-opening-bracket? is-opening-bracket?
     :is-closing-bracket? is-closing-bracket?
     :matching-position matching-pos
     :has-matching-bracket? (not (nil? matching-pos))})) 