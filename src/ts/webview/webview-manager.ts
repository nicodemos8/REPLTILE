import * as vscode from 'vscode';
import { sendToClojureScript } from '../clojure/process-manager';
import { findCurrentProject, initializeDependencyWatchers, cleanupDependencyWatchers } from '../dependencies/dependency-manager';
import { createREPLTILEDirectory, saveHistoryFile, loadHistoryFile, clearHistoryFile } from '../history/history-manager';

let webviewPanel: vscode.WebviewPanel | null = null;

export function createWebviewPanel(context?: vscode.ExtensionContext) {
  if (webviewPanel) {
    webviewPanel.reveal();
    return;
  }

  webviewPanel = vscode.window.createWebviewPanel(
    'repltile',
    'REPLTILE',
    vscode.ViewColumn.Two,
    {
      enableScripts: true,
      retainContextWhenHidden: true
    }
  );

  // Load webview HTML
  if (context?.extensionUri) {
    webviewPanel.webview.html = getWebviewContent(webviewPanel.webview, context.extensionUri);
  } else {
    // Fallback - use relative path (may not work but won't crash)
    webviewPanel.webview.html = getWebviewContent(webviewPanel.webview, vscode.Uri.file(''));
  }

  // Initialize dependency management system when plugin opens
  console.log('🔧 [DEPS] Initializing dependency watchers when plugin opens...');
  initializeDependencyWatchers();

  // Initialize dependency system after a delay to allow ClojureScript to start
  setTimeout(() => {
    sendToClojureScript({ type: 'initialize-deps' });
  }, 1000);

  // Clean up dependency watchers when webview is disposed
  webviewPanel.onDidDispose(() => {
    console.log('🧹 [DEPS] Cleaning up dependency watchers when plugin closes...');
    cleanupDependencyWatchers();
    webviewPanel = null;
  });

  // Handle webview messages - delegate everything to ClojureScript
  webviewPanel.webview.onDidReceiveMessage(async message => {
    // Special handling for get-project-info messages
    if (message.type === 'get-project-info') {
      // Get current project information
      const workspaceFolders = vscode.workspace.workspaceFolders || [];
      const currentProject = findCurrentProject(workspaceFolders);

      if (currentProject) {
        // Send project info back to webview
        webviewPanel!.webview.postMessage({
          type: 'project-info',
          projectPath: currentProject.uri.fsPath,
          projectName: currentProject.name
        });
      } else {
        // Send default project info
        webviewPanel!.webview.postMessage({
          type: 'project-info',
          projectPath: vscode.workspace.rootPath || 'unknown',
          projectName: 'unknown'
        });
      }
    } else if (message.type === 'create-repltile-directory') {
      // Create .REPLTILE directory in current project
      await createREPLTILEDirectory();
    } else if (message.type === 'save-history-file') {
      // Save command history to .REPLTILE/history.edn file
      await saveHistoryFile(message.data, message.projectId);
    } else if (message.type === 'load-history-file') {
      // Load command history from .REPLTILE/history.edn file
      await loadHistoryFile(message.projectId);
    } else if (message.type === 'clear-history-file') {
      // Clear command history file for project
      await clearHistoryFile(message.projectId);
    } else if (message.type === 'evaluate-code') {
      sendToClojureScript(message);
    } else if (message.type === 'resolve-dependencies') {
      console.log('🔧 [DEPS-EXT] Resolve dependencies button clicked in webview');
      sendToClojureScript(message);
    } else {
      // Forward all other messages to ClojureScript
      sendToClojureScript(message);
    }
  });

  // Handle webview disposal
  webviewPanel.onDidDispose(() => {
    webviewPanel = null;
  });

  // Notify ClojureScript that webview is ready
  setTimeout(() => {
    sendToClojureScript({ type: 'webview-ready' });
  }, 500);
}

function getWebviewContent(webview: vscode.Webview, extensionUri: vscode.Uri): string {
  // Get the URI for the webview/main.js file
  const mainJsUri = webview.asWebviewUri(vscode.Uri.joinPath(extensionUri, 'out', 'webview', 'main.js'));

  // Get the URI for the icon.png file
  const iconUri = webview.asWebviewUri(vscode.Uri.joinPath(extensionUri, 'icon.png'));

  return `<!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>REPLTILE</title>
        <script>
          window.REPLTILE_ICON_URI = "${iconUri}";
        </script>
    </head>
    <body>
        <div id="app"><div id="loading-msg" style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:'JetBrains Mono',monospace;font-size:22px;color:#22c55e;">Loading REPLTILE...</div></div>
        <script src="${mainJsUri}"></script>
    </body>
    </html>`;
}

export function closeWebviewPanel() {
  if (webviewPanel) {
    webviewPanel.dispose();
    // webviewPanel will be set to null in the onDidDispose handler
  }
}

export function getWebviewPanel(): vscode.WebviewPanel | null {
  return webviewPanel;
}

export function showLogs() {
  vscode.window.showInformationMessage('Check the console for REPLTILE logs');
} 