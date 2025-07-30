import * as vscode from 'vscode';
import { handleCalvaBridgeMessage } from '../calva-bridge';
import { logger } from '../core/logger';
import { sendToClojureScript } from './process-manager';
import { checkFileExists, checkDirsExist, resolveDependencies, getWorkspaceInfo } from '../dependencies/dependency-manager';

let messageBuffer = '';

export function handleClojureScriptMessage(data: string, webviewPanel: vscode.WebviewPanel | null, extensionContext: vscode.ExtensionContext) {
  try {
    // Add data to buffer
    messageBuffer += data;

    // Process complete JSON messages
    const lines = messageBuffer.split('\n');

    // Keep the last incomplete line in buffer
    messageBuffer = lines.pop() || '';

    for (const line of lines) {
      if (line.trim()) {
        try {
          const message = JSON.parse(line);

          // DEBUG: Log all incoming messages to track dependencies
          if (message.type && message.type.includes('deps') || message.type.includes('check') || message.type.includes('resolve')) {
            logger.info(`🔍 [DEBUG] Received message type: ${message.type} | Full message: ${JSON.stringify(message)}`);
          }

          // Only delegate REPL lifecycle to Calva bridge, NOT evaluation
          if (
            message.type === 'start-jackin' ||
            message.type === 'stop-jackin'
          ) {
            handleCalvaBridgeMessage(message, sendToClojureScript, extensionContext);
          } else {
            // Handle non-Calva messages
            if (message.type === 'execute-command') {
              vscode.commands.executeCommand(message.command, ...(message.args || []));
            } else if (message.type === 'webview-message' && webviewPanel) {
              webviewPanel.webview.postMessage(message.message);
            } else if (message.type === 'notification') {
              const level = message.level || 'info';
              switch (level) {
                case 'error':
                  vscode.window.showErrorMessage(message.message);
                  break;
                case 'warn':
                  vscode.window.showWarningMessage(message.message);
                  break;
                default:
                  vscode.window.showInformationMessage(message.message);
              }
            }
            // Dependency management messages - WITH DETAILED LOGGING
            else if (message.type === 'check-file-exists') {
              logger.info(`📁 [DEPS-EXT] ✅ MATCHED check-file-exists | Checking file: ${message.filename} for config: ${message['config-type']}`);
              checkFileExists(message.filename, message['config-type']);
            } else if (message.type === 'check-dirs-exist') {
              logger.info(`📋 [DEPS-EXT] ✅ MATCHED check-dirs-exist | Checking directories for config: ${message['config-type']}`);
              checkDirsExist(message.dirs, message['config-type']);
            } else if (message.type === 'resolve-dependencies') {
              logger.info(`🔧 [DEPS-EXT] ✅ MATCHED resolve-dependencies | Resolving dependencies for config: ${message['config-type']}`);
              logger.info(`🔧 [DEPS-EXT] Command: ${message.command}`);
              resolveDependencies(message['config-type'], message.command, message.description);
            } else if (message.type === 'get-workspace-info') {
              logger.info(`📋 [DEPS-EXT] ✅ MATCHED get-workspace-info`);
              getWorkspaceInfo();
            } else {
              // Log unmatched messages for debugging  
              if (message.type && typeof message.type === 'string') {
                logger.info(`❓ [DEBUG] Unmatched message type: "${message.type}" | Available types: execute-command, webview-message, notification, check-file-exists, check-dirs-exist, resolve-dependencies, get-workspace-info`);
              }
            }
          }
        } catch (parseError) {
          // Try to parse as part of a multi-line JSON
          if (line.includes('{') || line.includes('}')) {
            messageBuffer = line + '\n' + messageBuffer;
          }
        }
      }
    }

    // Try to parse the buffer if it looks like complete JSON
    if (messageBuffer.trim()) {
      try {
        // Count braces to see if we have a complete JSON object
        const openBraces = (messageBuffer.match(/\{/g) || []).length;
        const closeBraces = (messageBuffer.match(/\}/g) || []).length;

        if (openBraces === closeBraces && openBraces > 0) {
          const message = JSON.parse(messageBuffer.trim());

          if (message.type === 'webview-message' && webviewPanel) {
            webviewPanel.webview.postMessage(message.message);
          }

          // Clear buffer after successful parse
          messageBuffer = '';
        }
      } catch (bufferParseError) {
        // Keep buffer for next iteration
      }
    }
  } catch (error) {
    console.error('❌ Error handling ClojureScript message:', error);
    // Reset buffer on error
    messageBuffer = '';
  }
} 