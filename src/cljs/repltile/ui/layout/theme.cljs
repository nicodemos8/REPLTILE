(ns repltile.ui.layout.theme
  "Theme detection and application for REPLTILE webview"
  (:require [clojure.string :as str]))

(defn rgb-to-luminance
  "Calculate relative luminance of an RGB color"
  [r g b]
  (let [rs (if (<= (/ r 255.0) 0.03928)
             (/ (/ r 255.0) 12.92)
             (js/Math.pow (/ (+ (/ r 255.0) 0.055) 1.055) 2.4))
        gs (if (<= (/ g 255.0) 0.03928)
             (/ (/ g 255.0) 12.92)
             (js/Math.pow (/ (+ (/ g 255.0) 0.055) 1.055) 2.4))
        bs (if (<= (/ b 255.0) 0.03928)
             (/ (/ b 255.0) 12.92)
             (js/Math.pow (/ (+ (/ b 255.0) 0.055) 1.055) 2.4))]
    (+ (* 0.2126 rs) (* 0.7152 gs) (* 0.0722 bs))))

(defn parse-rgb-color
  "Parse RGB color string and return [r g b] or nil"
  [color-str]
  (when color-str
    (let [color-str (str/trim color-str)]
      (cond
        ;; rgb(r, g, b)
        (str/starts-with? color-str "rgb(")
        (let [matches (re-find #"rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)" color-str)]
          (when matches
            [(js/parseInt (nth matches 1))
             (js/parseInt (nth matches 2))
             (js/parseInt (nth matches 3))]))

        ;; #rrggbb
        (and (str/starts-with? color-str "#") (= (count color-str) 7))
        (let [hex (subs color-str 1)
              r (js/parseInt (subs hex 0 2) 16)
              g (js/parseInt (subs hex 2 4) 16)
              b (js/parseInt (subs hex 4 6) 16)]
          [r g b])

        ;; #rgb (shorthand)
        (and (str/starts-with? color-str "#") (= (count color-str) 4))
        (let [hex (subs color-str 1)
              r (js/parseInt (str (first hex) (first hex)) 16)
              g (js/parseInt (str (second hex) (second hex)) 16)
              b (js/parseInt (str (nth hex 2) (nth hex 2)) 16)]
          [r g b])))))

(defn detect-theme-by-colors
  "Detect theme by analyzing background/foreground colors"
  [bg-color fg-color]
  (let [bg-rgb (parse-rgb-color bg-color)
        fg-rgb (parse-rgb-color fg-color)]
    (.log js/console "🎨 Parsed colors - BG RGB:" bg-rgb "FG RGB:" fg-rgb)

    (cond
      ;; Both colors available - calculate luminance
      (and bg-rgb fg-rgb)
      (let [bg-luminance (apply rgb-to-luminance bg-rgb)
            fg-luminance (apply rgb-to-luminance fg-rgb)
            is-dark? (< bg-luminance fg-luminance)]
        (.log js/console (str "🎨 Luminance - BG: " bg-luminance " FG: " fg-luminance " -> " (if is-dark? "DARK" "LIGHT")))
        is-dark?)

      ;; Only background available
      bg-rgb
      (let [bg-luminance (apply rgb-to-luminance bg-rgb)
            is-dark? (< bg-luminance 0.5)]
        (.log js/console (str "🎨 BG Luminance: " bg-luminance " -> " (if is-dark? "DARK" "LIGHT")))
        is-dark?)

      ;; Only foreground available  
      fg-rgb
      (let [fg-luminance (apply rgb-to-luminance fg-rgb)
            is-dark? (> fg-luminance 0.5)] ; If text is bright, background is probably dark
        (.log js/console (str "🎨 FG Luminance: " fg-luminance " -> " (if is-dark? "DARK" "LIGHT")))
        is-dark?)

      ;; No color information
      :else
      (do
        (.log js/console "🎨 No color information available, checking DOM classes")
        nil))))

(defn detect-theme-by-dom
  "Detect theme by checking DOM classes and data attributes"
  []
  (let [body (.-body js/document)
        html (.-documentElement js/document)
        body-classes (.-className body)
        html-classes (.-className html)
        body-theme (.-dataset.theme body)
        html-theme (.-dataset.theme html)]

    (.log js/console "🎨 DOM analysis - Body classes:" body-classes "HTML classes:" html-classes)
    (.log js/console "🎨 DOM analysis - Body theme:" body-theme "HTML theme:" html-theme)

    (cond
      ;; Explicit theme attributes
      (or (= body-theme "dark") (= html-theme "dark"))
      (do (.log js/console "🎨 Found explicit dark theme attribute") true)

      (or (= body-theme "light") (= html-theme "light"))
      (do (.log js/console "🎨 Found explicit light theme attribute") false)

      ;; Common dark theme class patterns
      (or (str/includes? (str body-classes) "dark")
          (str/includes? (str html-classes) "dark")
          (str/includes? (str body-classes) "vs-dark")
          (str/includes? (str html-classes) "vs-dark"))
      (do (.log js/console "🎨 Found dark theme classes") true)

      ;; Common light theme class patterns  
      (or (str/includes? (str body-classes) "light")
          (str/includes? (str html-classes) "light")
          (str/includes? (str body-classes) "vs-light")
          (str/includes? (str html-classes) "vs-light"))
      (do (.log js/console "🎨 Found light theme classes") false)

      ;; No DOM indicators
      :else
      (do (.log js/console "🎨 No DOM theme indicators found") nil))))

(defn detect-vscode-theme
  "Intelligent VS Code/Cursor theme detection with multiple fallbacks"
  []
  (try
    (let [computed-style (.getComputedStyle js/window (.-body js/document))
          bg-color (.getPropertyValue computed-style "--vscode-editor-background")
          fg-color (.getPropertyValue computed-style "--vscode-editor-foreground")
          panel-bg (.getPropertyValue computed-style "--vscode-panel-background")
          title-bg (.getPropertyValue computed-style "--vscode-titleBar-activeBackground")
          sidebar-bg (.getPropertyValue computed-style "--vscode-sideBar-background")]

      (.log js/console "🎨 Theme detection starting...")
      (.log js/console (str "🎨 CSS vars - Editor BG: " bg-color " | FG: " fg-color))
      (.log js/console (str "🎨 CSS vars - Panel BG: " panel-bg " | Title BG: " title-bg " | Sidebar BG: " sidebar-bg))

      ;; Try multiple detection methods
      (let [color-result (detect-theme-by-colors bg-color fg-color)
            dom-result (detect-theme-by-dom)
            panel-result (when panel-bg (detect-theme-by-colors panel-bg nil))
            sidebar-result (when sidebar-bg (detect-theme-by-colors sidebar-bg nil))

            ;; Combine results with priority - treat false as valid result (light theme)
            final-result (cond
                           ;; Priority 1: editor colors (if available)
                           (not (nil? color-result)) color-result
                           ;; Priority 2: panel colors (if available)
                           (not (nil? panel-result)) panel-result
                           ;; Priority 3: sidebar colors (if available) 
                           (not (nil? sidebar-result)) sidebar-result
                           ;; Priority 4: DOM classes (if available)
                           (not (nil? dom-result)) dom-result
                           ;; Default: dark theme
                           :else true)]

        (.log js/console (str "🎨 Detection results - Colors: " color-result " | DOM: " dom-result " | Panel: " panel-result " | Sidebar: " sidebar-result))
        (.log js/console (str "🎨 Final theme detected: " (if final-result "DARK" "LIGHT")))
        final-result))

    (catch :default e
      (.log js/console "⚠️ Theme detection failed, defaulting to dark:" e)
      true)))

(defn apply-theme!
  "Apply the detected theme to the document"
  [is-dark?]
  (let [body (.-body js/document)]
    (if is-dark?
      (do
        (.log js/console "🌙 Applying DARK theme")
        (.setAttribute body "data-theme" "dark")
        (.add (.-classList body) "dark-theme"))
      (do
        (.log js/console "☀️ Applying LIGHT theme")
        (.removeAttribute body "data-theme")
        (.remove (.-classList body) "dark-theme")))))

(defn setup-theme!
  "Setup theme detection and application - only on startup"
  []
  (.log js/console "🎨 Setting up theme detection...")

  ;; Apply initial theme only once
  (let [is-dark? (detect-vscode-theme)]
    (apply-theme! is-dark?))

  (.log js/console "✅ Theme detection setup complete")) 