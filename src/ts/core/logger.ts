import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';

// ==============================================
// LOGGING SYSTEM FOR USER DEBUGGING
// ==============================================

export class REPLTILELogger {
  private outputChannel: vscode.OutputChannel;
  private logFile: string | null = null;

  constructor() {
    this.outputChannel = vscode.window.createOutputChannel('REPLTILE');
    this.setupLogFile();
  }

  private setupLogFile() {
    try {
      // Create logs directory in user's home
      const homeDir = os.homedir();
      const logDir = path.join(homeDir, '.repltile', 'logs');

      if (!fs.existsSync(logDir)) {
        fs.mkdirSync(logDir, { recursive: true });
      }

      const timestamp = new Date().toISOString().split('T')[0];
      this.logFile = path.join(logDir, `repltile-${timestamp}.log`);

      // Write session start
      this.writeToFile(`\n=== REPLTILE Session Started: ${new Date().toISOString()} ===\n`);

    } catch (error) {
      console.error('Failed to setup log file:', error);
    }
  }

  private writeToFile(message: string) {
    if (this.logFile) {
      try {
        fs.appendFileSync(this.logFile, message);
      } catch (error) {
        console.error('Failed to write to log file:', error);
      }
    }
  }

  private formatMessage(level: string, message: string, data?: any): string {
    const timestamp = new Date().toISOString();
    const dataStr = data ? ` | Data: ${JSON.stringify(data, null, 2)}` : '';
    return `[${timestamp}] ${level.padEnd(5)} | ${message}${dataStr}\n`;
  }

  info(message: string, data?: any) {
    const formatted = this.formatMessage('INFO', message, data);
    this.outputChannel.appendLine(`ℹ️ ${message}${data ? ` | ${JSON.stringify(data)}` : ''}`);
    this.writeToFile(formatted);
    console.log(`[REPLTILE] ${message}`, data || '');
  }

  warn(message: string, data?: any) {
    const formatted = this.formatMessage('WARN', message, data);
    this.outputChannel.appendLine(`⚠️ ${message}${data ? ` | ${JSON.stringify(data)}` : ''}`);
    this.writeToFile(formatted);
    console.warn(`[REPLTILE] ${message}`, data || '');
  }

  error(message: string, error?: any) {
    const formatted = this.formatMessage('ERROR', message, error);
    this.outputChannel.appendLine(`❌ ${message}${error ? ` | ${error.message || error}` : ''}`);
    this.writeToFile(formatted);
    console.error(`[REPLTILE] ${message}`, error || '');

    // Show error to user with option to view logs
    vscode.window.showErrorMessage(
      `REPLTILE: ${message}`,
      'View Logs',
      'Copy Log Path'
    ).then(selection => {
      if (selection === 'View Logs') {
        this.outputChannel.show();
      } else if (selection === 'Copy Log Path' && this.logFile) {
        vscode.env.clipboard.writeText(this.logFile);
        vscode.window.showInformationMessage('Log file path copied to clipboard');
      }
    });
  }

  debug(message: string, data?: any) {
    // Always show debug logs for dependency system, otherwise respect user setting
    const isDependencyLog = message.includes('[DEPS') || message.includes('Dependencies');
    const debugEnabled = vscode.workspace.getConfiguration('repltile').get('enableDebugLogging', false);

    if (isDependencyLog || debugEnabled) {
      const formatted = this.formatMessage('DEBUG', message, data);
      this.outputChannel.appendLine(`🐛 ${message}${data ? ` | ${JSON.stringify(data)}` : ''}`);
      this.writeToFile(formatted);
      console.debug(`[REPLTILE] ${message}`, data || '');
    }
  }

  show() {
    this.outputChannel.show();
  }

  getLogFilePath(): string | null {
    return this.logFile;
  }
}

// Global logger instance
export const logger = new REPLTILELogger(); 