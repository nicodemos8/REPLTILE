(ns repltile.core.core
  "Main entry point for REPLTILE ClojureScript implementation - Minimal Version"
  (:require [repltile.vscode.extension :as vscode]
            [repltile.core.logger :as log]
            [repltile.vscode.commands :as commands]
            [repltile.repl.deps :as deps]))

(defn handle-vscode-message
  "Handle messages from VS Code extension - delega para dispatcher de comandos"
  [message]
  (commands/dispatch-command! message))

(defn initialize-extension!
  "Initialize the REPLTILE extension - minimal version"
  []
  (log/info "🚀 REPLTILE ClojureScript core starting (minimal version)...")

  ;; Set debug log level
  (log/set-log-level! :debug)

  ;; Setup VS Code integration
  (vscode/setup!)

  ;; Força require de commands
  (commands/get-command-availability)

  ;; Setup message handlers
  (vscode/on-message handle-vscode-message)

  ;; Initialize dependency management system
  (log/info "🔧 Initializing dependency management system...")
  (deps/initialize!)

  ;; Don't create webview automatically - let extension handle it
  (log/info "✅ REPLTILE ClojureScript core initialized - waiting for webview creation"))

(defn ^:export main
  "Main entry point called by VS Code"
  [& args]
  (log/info "🎯 REPLTILE main entry point called with args:" (pr-str args))
  (initialize-extension!)) 