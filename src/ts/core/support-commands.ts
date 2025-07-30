import * as vscode from 'vscode';
import * as path from 'path';
import * as os from 'os';
import { logger } from './logger';

// ==============================================
// USER SUPPORT COMMANDS
// ==============================================

export function registerSupportCommands(context: vscode.ExtensionContext) {
  // Command to show logs
  const showLogsCmd = vscode.commands.registerCommand('repltile.showLogs', () => {
    logger.show();
    logger.info('Logs opened by user command');
  });

  // Command to open log file location
  const openLogLocationCmd = vscode.commands.registerCommand('repltile.openLogLocation', () => {
    const logPath = logger.getLogFilePath();
    if (logPath) {
      const logDir = path.dirname(logPath);
      vscode.env.openExternal(vscode.Uri.file(logDir));
      logger.info('Log directory opened', { path: logDir });
    } else {
      vscode.window.showWarningMessage('Log file not available');
    }
  });

  // Command to copy debug info
  const copyDebugInfoCmd = vscode.commands.registerCommand('repltile.copyDebugInfo', () => {
    const debugInfo = {
      timestamp: new Date().toISOString(),
      vscodeVersion: vscode.version,
      platform: `${os.platform()} ${os.arch()} ${os.release()}`,
      nodeVersion: process.version,
      extensionVersion: context.extension.packageJSON.version,
      workspaceInfo: {
        folders: vscode.workspace.workspaceFolders?.map(f => f.name) || [],
        rootPath: vscode.workspace.rootPath
      },
      settings: {
        enableDebugLogging: vscode.workspace.getConfiguration('repltile').get('enableDebugLogging'),
        // Add other relevant settings
      },
      replState: {
        // Will be set when standaloneNRepl is available
        isRunning: false,
        currentProject: 'none',
        // Add other relevant state
      }
    };

    const debugText = `=== REPLTILE Debug Information ===\n${JSON.stringify(debugInfo, null, 2)}\n\nLog file: ${logger.getLogFilePath() || 'not available'}\n`;

    vscode.env.clipboard.writeText(debugText);
    vscode.window.showInformationMessage('Debug information copied to clipboard. You can paste this when reporting issues.');

    logger.info('Debug information copied by user', debugInfo);
  });

  context.subscriptions.push(showLogsCmd, openLogLocationCmd, copyDebugInfoCmd);
} 