import * as vscode from 'vscode';
import { autoConfigureCalva } from './calva-bridge';
import { cleanupStandaloneNRepl } from './standalone-nrepl';
import { logger } from './core/logger';
import { registerSupportCommands } from './core/support-commands';
import { startClojureScriptProcess, sendToClojureScript, killClojureScriptProcess } from './clojure/process-manager';
import { handleClojureScriptMessage } from './clojure/message-handler';
import { createWebviewPanel, closeWebviewPanel, getWebviewPanel } from './webview/webview-manager';
import {
  switchToCurrentNamespace,
  reloadCurrentNamespace,
  evaluateSelection,
  evaluateCurrentForm,
  evaluateTopLevelForm,
  evaluateCurrentLine,
  loadCurrentFile,
  clearReplOutput,
  runNamespaceTests,
  focusInputField,
  repeatLastCommand
} from './repl/shortcuts';

// ==============================================
// GLOBAL VARIABLES
// ==============================================

let extensionContext: vscode.ExtensionContext | null = null;

export function activate(context: vscode.ExtensionContext) {
  logger.info('🚀 REPLTILE extension is activating...');

  // Store context globally for use in message handlers
  extensionContext = context;

  // Register support commands for user debugging
  registerSupportCommands(context);

  // Log workspace information for debugging
  const workspaceFolders = vscode.workspace.workspaceFolders || [];
  const repltileWorkspace = workspaceFolders.find(folder =>
    folder.uri.fsPath.includes('REPLTILE_ui') ||
    folder.name.includes('REPLTILE')
  );

  if (!repltileWorkspace) {
    console.log('⚠️ REPLTILE workspace not found in active workspaces');
    vscode.window.showWarningMessage(
      'REPLTILE: Please open the REPLTILE_ui folder as a workspace for optimal functionality'
    );
  }

  // Register commands
  const commands = [
    vscode.commands.registerCommand('repltile.show', () => createWebviewPanel(context)),
    vscode.commands.registerCommand('repltile.kill', () => closeWebviewPanel()),

    // REPL Shortcuts Commands
    vscode.commands.registerCommand('repltile.switchToCurrentNs', () => switchToCurrentNamespace()),
    vscode.commands.registerCommand('repltile.reloadCurrentNs', () => reloadCurrentNamespace()),
    vscode.commands.registerCommand('repltile.evalSelection', () => evaluateSelection()),
    vscode.commands.registerCommand('repltile.evalCurrentForm', () => evaluateCurrentForm()),
    vscode.commands.registerCommand('repltile.evalTopLevelForm', () => evaluateTopLevelForm()),
    vscode.commands.registerCommand('repltile.evalCurrentLine', () => evaluateCurrentLine()),
    vscode.commands.registerCommand('repltile.loadCurrentFile', () => loadCurrentFile()),
    vscode.commands.registerCommand('repltile.clearRepl', () => clearReplOutput()),
    vscode.commands.registerCommand('repltile.runNamespaceTests', () => runNamespaceTests()),
    vscode.commands.registerCommand('repltile.focusInput', () => focusInputField()),
    vscode.commands.registerCommand('repltile.repeatLastCommand', () => repeatLastCommand())
  ];

  commands.forEach(cmd => context.subscriptions.push(cmd));

  // Auto-configure Calva based on project type
  autoConfigureCalva().then(() => {
    console.log('🔧 Calva auto-configuration completed');
  }).catch(error => {
    console.error('❌ Error during Calva auto-configuration:', error);
  });

  // Start ClojureScript process with message handler (dependency system will be initialized when plugin opens)
  startClojureScriptProcess(context, (data: string) => {
    const webviewPanel = getWebviewPanel();
    handleClojureScriptMessage(data, webviewPanel, context);
  });

  console.log('✅ REPLTILE activated');
}

export function deactivate() {
  console.log('🛑 REPLTILE deactivating...');

  // Cleanup standalone nREPL
  cleanupStandaloneNRepl();

  // Kill ClojureScript process
  killClojureScriptProcess();

  // Close webview panel (this will also clean up dependency watchers)
  closeWebviewPanel();

  extensionContext = null;

  console.log('✅ REPLTILE deactivated');
} 