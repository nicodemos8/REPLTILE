;; Validate project dependencies for REPLTILE

;; 1. Check if we have Clojure
(println "=== REPLTILE DEPENDENCY VALIDATION ===\n")
(println "1. Checking for Clojure installation...")
(try
  (println (str "  ✅ Clojure version: " (clojure-version)))
  (catch Exception e
    (println "  ❌ Error checking Clojure:" (.getMessage e))))

;; 2. Check classpath for important libraries
(println "\n2. Checking classpath for essential libraries...")
(let [cp (System/getProperty "java.class.path")]
  (println (str "  nREPL: " (if (.contains cp "nrepl") "✅ Found" "❌ Not found")))
  (println (str "  cider-nrepl: " (if (.contains cp "cider-nrepl") "✅ Found" "❌ Not found")))
  (println (str "  tools.namespace: " (if (.contains cp "tools.namespace") "✅ Found" "❌ Not found"))))

;; 3. Check for repl-tooling related libraries
(println "\n3. Checking for repl-tooling libraries...")
(let [cp (System/getProperty "java.class.path")]
  (println (str "  repl-tooling: " (if (.contains cp "repl-tooling") "✅ Found" "❌ Not found"))))

;; 4. Check user.dir and validate project configuration
(println "\n4. Checking project configuration...")
(let [user-dir (System/getProperty "user.dir")
      deps-edn-path (str user-dir "/deps.edn")]
  (println (str "  Project directory: " user-dir))
  (if (.exists (java.io.File. deps-edn-path))
    (let [content (slurp deps-edn-path)]
      (if (.contains content ":repltile")
        (if (and (.contains content "cider/cider-nrepl")
                 (.contains content ":repltile"))
          (println "  ✅ :repltile alias found and configured correctly")
          (println "  ⚠️ :repltile alias found but may be incomplete"))
        (println "  ❌ :repltile alias NOT found in deps.edn")))
    (println "  ❌ deps.edn file not found")))

;; 5. Instructions if something is wrong
(println "\n💡 IF SOMETHING IS NOT WORKING:")
(println "  For deps.edn, add:")
(println "  :aliases {:repltile {:extra-deps {nrepl/nrepl {:mvn/version \"1.0.0\"}")
(println "                                  cider/cider-nrepl {:mvn/version \"0.28.5\"}}")
(println "                     :main-opts [\"-m\" \"nrepl.cmdline\" \"--middleware\" \"[cider.nrepl/cider-middleware]\"]")
(println "                     :jvm-opts [\"-XX:-OmitStackTraceInFastThrow\"]}}")
(println "")
(println "  Restart REPL using: clojure -A:repltile")

(println "\n=== END OF VALIDATION ===\n") 