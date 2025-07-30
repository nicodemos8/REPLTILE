(ns repltile.ui.components.search
  "Search functionality for REPLTILE webview"
  (:require [clojure.string :as str]))

;; Search functionality atoms
(def search-state (atom {:active false
                         :current-term ""
                         :case-sensitive false
                         :matches []
                         :current-index -1}))

(defn escape-regex [text]
  (-> text
      (str/replace #"\\" "\\\\")
      (str/replace #"\." "\\.")
      (str/replace #"\*" "\\*")
      (str/replace #"\+" "\\+")
      (str/replace #"\?" "\\?")
      (str/replace #"\^" "\\^")
      (str/replace #"\$" "\\$")
      (str/replace #"\{" "\\{")
      (str/replace #"\}" "\\}")
      (str/replace #"\(" "\\(")
      (str/replace #"\)" "\\)")
      (str/replace #"\|" "\\|")
      (str/replace #"\[" "\\[")
      (str/replace #"\]" "\\]")))

(defn clear-search-highlights!
  []
  (when-let [output-el (.getElementById js/document "repl-output")]
    (let [highlighted-elements (.querySelectorAll output-el ".search-highlight")]
      (doseq [el (array-seq highlighted-elements)]
        (let [parent (.-parentNode el)
              text-content (.-textContent el)]
          (.replaceChild parent (.createTextNode js/document text-content) el)
          ;; Normalize whitespace in parent
          (.normalize parent))))))

(defn update-search-info!
  []
  (when-let [info-el (.getElementById js/document "search-results-info")]
    (let [{:keys [matches current-index]} @search-state
          total (count matches)]
      (if (pos? total)
        (set! (.-textContent info-el) (str (inc current-index) "/" total))
        (set! (.-textContent info-el) "0/0")))))

(defn navigate-to-match!
  [index]
  (let [{:keys [matches]} @search-state
        total (count matches)]
    (when (and (pos? total) (>= index 0) (< index total))
      ;; Clear current highlight
      (doseq [match matches]
        (.remove (.-classList match) "current"))

      ;; Highlight current match
      (let [current-match (nth matches index)]
        (.add (.-classList current-match) "current")

        ;; Scroll to current match
        (.scrollIntoView current-match #js {:behavior "smooth"
                                            :block "center"})

        ;; Update state and info
        (swap! search-state assoc :current-index index)
        (update-search-info!)))))

(defn find-all-matches
  [text search-term case-sensitive?]
  (when (and text search-term (not= search-term ""))
    (let [text-to-search (if case-sensitive? text (str/lower-case text))
          search-len (count search-term)
          matches (atom [])]

      (loop [start-pos 0]
        (let [search-to-find (if case-sensitive? search-term (str/lower-case search-term))
              match-pos (.indexOf text-to-search search-to-find start-pos)]
          (if (>= match-pos 0)
            (do
              ;; Extract the original text (preserving case) at this position
              (let [original-match (subs text match-pos (+ match-pos search-len))]
                (swap! matches conj {:start match-pos
                                     :end (+ match-pos search-len)
                                     :text original-match}))
              (recur (inc match-pos)))
            @matches))))))

(defn create-highlighted-fragment
  "Create a document fragment with highlighted matches"
  [original-text matches]
  (let [fragment (.createDocumentFragment js/document)
        sorted-matches (sort-by :start matches)
        highlighted-spans (atom [])]

    (loop [current-pos 0
           remaining-matches sorted-matches]

      (if (empty? remaining-matches)
        ;; Add remaining text
        (when (< current-pos (count original-text))
          (.appendChild fragment (.createTextNode js/document (subs original-text current-pos))))

        ;; Process next match
        (let [match (first remaining-matches)
              {:keys [start end text]} match]

          ;; Add text before match
          (when (> start current-pos)
            (.appendChild fragment (.createTextNode js/document (subs original-text current-pos start))))

          ;; Add highlighted match
          (let [highlight-span (.createElement js/document "span")]
            (.add (.-classList highlight-span) "search-highlight")
            (set! (.-textContent highlight-span) text)
            (.appendChild fragment highlight-span)
            (swap! highlighted-spans conj highlight-span))

          ;; Continue with remaining text
          (recur end (rest remaining-matches)))))

    {:fragment fragment
     :spans @highlighted-spans}))

(defn highlight-search-results!
  "Highlight search results in the output using improved algorithm"
  [search-term]
  (when (and search-term (not= search-term ""))
    (when-let [output-el (.getElementById js/document "repl-output")]
      (let [case-sensitive? (:case-sensitive @search-state)
            walker (.createTreeWalker js/document output-el
                                      (.-SHOW_TEXT js/NodeFilter) nil false)
            text-nodes (atom [])
            all-matches (atom [])]

        ;; Clear existing highlights first
        (clear-search-highlights!)

        ;; Collect all text nodes
        (loop [node (.nextNode walker)]
          (when node
            (swap! text-nodes conj node)
            (recur (.nextNode walker))))

        ;; Process each text node
        (doseq [text-node @text-nodes]
          (let [text (.-textContent text-node)
                parent (.-parentNode text-node)
                matches (find-all-matches text search-term case-sensitive?)]

            (when (seq matches)
              (let [{:keys [fragment spans]} (create-highlighted-fragment text matches)]
                ;; Replace the original text node with highlighted fragment
                (.replaceChild parent fragment text-node)
                ;; Collect all highlighted spans
                (swap! all-matches concat spans)))))

        ;; Update search state
        (swap! search-state assoc :matches @all-matches :current-index -1)
        (update-search-info!)

        ;; Highlight first match if available
        (when (seq @all-matches)
          (navigate-to-match! 0))))))

(defn search-next!
  "Navigate to next search result"
  []
  (let [{:keys [matches current-index]} @search-state
        total (count matches)]
    (when (pos? total)
      (let [next-index (if (>= current-index (dec total)) 0 (inc current-index))]
        (navigate-to-match! next-index)))))

(defn search-prev!
  "Navigate to previous search result"
  []
  (let [{:keys [matches current-index]} @search-state
        total (count matches)]
    (when (pos? total)
      (let [prev-index (if (<= current-index 0) (dec total) (dec current-index))]
        (navigate-to-match! prev-index)))))

(defn perform-search!
  "Perform search with the current term"
  [search-term]
  (when search-term
    ;; Update case sensitivity from checkbox
    (when-let [case-checkbox (.getElementById js/document "search-case-sensitive")]
      (swap! search-state assoc :case-sensitive (.-checked case-checkbox)))

    (swap! search-state assoc :current-term search-term)
    (if (= search-term "")
      (do
        (clear-search-highlights!)
        (swap! search-state assoc :matches [] :current-index -1)
        (update-search-info!))
      (highlight-search-results! search-term))))

(defn show-search!
  "Show the search container and focus the input"
  []
  (when-let [search-container (.getElementById js/document "repl-search-container")]
    (.add (.-classList search-container) "active")
    (swap! search-state assoc :active true)
    (.log js/console "🔍 Search activated")
    (when-let [search-input (.getElementById js/document "repl-search-input")]
      (js/setTimeout #(.focus search-input) 100))))

(defn hide-search!
  "Hide the search container and clear highlights"
  []
  (when-let [search-container (.getElementById js/document "repl-search-container")]
    (.remove (.-classList search-container) "active")
    (swap! search-state assoc :active false)
    (clear-search-highlights!)
    (when-let [search-input (.getElementById js/document "repl-search-input")]
      (set! (.-value search-input) ""))
    (.log js/console "🔍 Search deactivated")))

(defn toggle-search!
  "Toggle search visibility"
  []
  (if (:active @search-state)
    (hide-search!)
    (show-search!)))

(defn setup-search-handlers!
  "Setup search functionality handlers"
  []
  (try
    (.log js/console "🔍 Setting up search handlers...")

    ;; Search input handler
    (when-let [search-input (.getElementById js/document "repl-search-input")]
      (.addEventListener search-input "input"
                         (fn [event]
                           (let [search-term (.-value (.-target event))]
                             (perform-search! search-term))))

      (.addEventListener search-input "keydown"
                         (fn [event]
                           (let [key (.-key event)]
                             (cond
                               (= key "Enter")
                               (do
                                 (.preventDefault event)
                                 (if (.-shiftKey event)
                                   (search-prev!)
                                   (search-next!)))

                               (= key "Escape")
                               (do
                                 (.preventDefault event)
                                 (hide-search!)))))))

    ;; Search navigation buttons
    (when-let [next-btn (.getElementById js/document "search-next-btn")]
      (.addEventListener next-btn "click" (fn [_] (search-next!))))

    (when-let [prev-btn (.getElementById js/document "search-prev-btn")]
      (.addEventListener prev-btn "click" (fn [_] (search-prev!))))

    ;; Close button
    (when-let [close-btn (.getElementById js/document "search-close-btn")]
      (.addEventListener close-btn "click" (fn [_] (hide-search!))))

    ;; Case sensitivity checkbox
    (when-let [case-checkbox (.getElementById js/document "search-case-sensitive")]
      (.addEventListener case-checkbox "change"
                         (fn [_]
                           (.log js/console "🔍 Case sensitivity changed to:" (.-checked case-checkbox))
                           ;; Re-run search with current term when case sensitivity changes
                           (let [current-term (:current-term @search-state)]
                             (when (and current-term (not= current-term ""))
                               (perform-search! current-term))))))

    (.log js/console "✅ Search handlers setup complete")
    (catch :default e
      (.error js/console "Error setting up search handlers:" e))))

(defn get-search-stats []
  (let [{:keys [matches current-index active]} @search-state]
    {:active active
     :total-matches (count matches)
     :current-index current-index
     :has-matches (pos? (count matches))})) 