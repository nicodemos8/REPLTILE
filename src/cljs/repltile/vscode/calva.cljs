(ns repltile.vscode.calva
  "Calva bridge integration - only utility functions, no direct Calva API access"
  (:require [repltile.vscode.extension :as vscode]
            [repltile.core.logger :as log]
            [repltile.repl.engine :as rt]))

(defn start-repl!
  "Solicita ao TypeScript que faça o jack-in via Calva API."
  []
  (log/info "🚀 Solicitando start-jackin para TypeScript (via calva.cljs)...")
  (vscode/send-to-vscode! {:type :start-jackin}))

(defn stop-repl!
  "Solicita ao TypeScript que pare o jack-in via Calva API."
  []
  (log/info "🛑 Solicitando stop-jackin para TypeScript (via calva.cljs)...")
  (vscode/send-to-vscode! {:type :stop-jackin}))

(defn connect-repl-tooling-to-port!
  "Connect repl-tooling to the discovered port, retrying up to 10 times every 2 seconds."
  [port]
  (let [max-attempts 120]
    (letfn [(try-connect [attempt]
              (try
                (when (= attempt 1)
                  (log/info (str "🔌 Calva - Connecting repl-tooling to port: " port))
                  (rt/connect-to-nrepl! "localhost" port))
                (js/setTimeout
                 (fn []
                   (try
                     (if (rt/connected?)
                       (log/info "✅ repl-tooling connected successfully!")
                       (if (< attempt max-attempts)
                         (do
                           (log/warn (str "Tentativa " attempt " de conexão falhou, tentando novamente em 2s..."))
                           (try-connect (inc attempt)))
                         (do
                           (log/error "❌ repl-tooling failed to connect after 120 attempts")
                           (vscode/send-webview-message! {:type :repl-connection-error
                                                          :error "repl-tooling connection failed after 10 attempts"}))))
                     (catch :default e
                       (log/error (str "❌ Error connecting repl-tooling (async): " e))
                       (vscode/send-webview-message! {:type :repl-connection-error
                                                      :error (str e)}))))
                 1000)
                (catch :default e
                  (log/error (str "❌ Error connecting repl-tooling: " e))
                  (vscode/send-webview-message! {:type :repl-connection-error
                                                 :error (str e)}))))]
      (try-connect 1))))

(defn disconnect-repl-tooling!
  []
  (log/info "🛑 Disconnecting repl-tooling...")
  (rt/disconnect!)
  (vscode/send-webview-message! {:type :repl-disconnected})) 