(ns repltile.vscode.extension
  "VS Code API bindings and integration"
  (:require [repltile.core.logger :as log]
            [clojure.core.async :as async]))

;; Node.js requires for process communication
(def process (js/require "process"))

;; Message passing setup
(def message-channel (async/chan 100))

;; Message buffer for handling incomplete JSON data
(def message-buffer (atom ""))

(defn handle-stdin-data
  "Handle data from VS Code extension via stdin"
  [data]
  (try
    ;; Add data to buffer
    (swap! message-buffer str (.toString data))

    ;; Process complete JSON messages
    (let [buffer-content @message-buffer
          lines (.split buffer-content "\n")]

      ;; Keep the last incomplete line in buffer
      (reset! message-buffer (last lines))

      ;; Process all complete lines except the last one
      (doseq [line (butlast lines)]
        (when (and line (not= (.trim line) ""))
          (try
            (let [raw-message (js->clj (js/JSON.parse line) :keywordize-keys true)
                  ;; Ensure type is always a keyword for consistent handling
                  message (update raw-message :type keyword)]
              (log/debug "📥 [VSCODE] Received message:" (pr-str message))
              (async/put! message-channel message))
            (catch :default parse-error
              (log/error "❌ [VSCODE] Error parsing JSON line:" line)
              (log/error "❌ [VSCODE] Parse error:" parse-error))))))
    (catch :default e
      (log/error "❌ [VSCODE] Error handling stdin data:" e)
      ;; Reset buffer on error
      (reset! message-buffer ""))))

(defn setup!
  "Setup VS Code integration"
  []
  (log/info "🔧 Setting up VS Code integration...")

  ;; Setup stdin/stdout communication with extension  
  (when (.-stdin process)
    (.on (.-stdin process) "data" handle-stdin-data))

  (log/info "✅ VS Code integration ready"))

(defn send-to-vscode!
  "Send message to VS Code extension via stdout"
  [message]
  (try
    (let [json-str (js/JSON.stringify (clj->js message))]
      (log/debug "📤 Sending to VS Code:" (pr-str message))
      (.write (.-stdout process) (str json-str "\n")))
    (catch :default e
      (log/error "❌ Error sending to VS Code:" e))))

(defn on-message
  "Setup message handler"
  [handler-fn]
  (async/go-loop []
    (when-let [message (async/<! message-channel)]
      (try
        (handler-fn message)
        (catch :default e
          (log/error "❌ Error handling message:" e)))
      (recur))))

(defn execute-command!
  "Execute VS Code command with workspace context"
  [command & args]
  (log/debug "🎯 Executing VS Code command:" command "with args:" (pr-str args))

  ;; For calva.jackIn, request workspace context
  (if (= command "calva.jackIn")
    (send-to-vscode! {:type :execute-command
                      :command command
                      :args (vec args)
                      :requireWorkspaceContext true})
    (send-to-vscode! {:type :execute-command
                      :command command
                      :args (vec args)})))

(defn create-webview-panel!
  "Request VS Code extension to create webview panel"
  [options]
  (log/debug "🌐 Requesting webview panel creation with options:" (pr-str options))
  (send-to-vscode! {:type :create-webview
                    :options options}))

(defn send-webview-message!
  "Send message to webview"
  [message]
  (send-to-vscode! {:type :webview-message
                    :message message}))

(defn show-notification!
  "Show VS Code notification"
  [message level]
  (send-to-vscode! {:type :notification
                    :message message
                    :level (or level :info)}))

(defn send-message-to-extension!
  "Send message to TypeScript extension (alias for send-to-vscode!)"
  [message]
  (send-to-vscode! message))

(defn request-workspace-info!
  "Request workspace information from VS Code extension"
  []
  (send-to-vscode! {:type :get-workspace-info})) 