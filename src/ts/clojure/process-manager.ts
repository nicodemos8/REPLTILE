import * as vscode from 'vscode';
import { spawn, ChildProcess } from 'child_process';
import * as path from 'path';
import { logger } from '../core/logger';

// Global process instance
let clojureProcess: ChildProcess | null = null;

export function startClojureScriptProcess(context: vscode.ExtensionContext, messageHandler: (data: string) => void) {
  const mainJsPath = path.join(context.extensionPath, 'out', 'main.js');

  clojureProcess = spawn('node', [mainJsPath], {
    cwd: context.extensionPath,
    stdio: ['pipe', 'pipe', 'pipe']
  });

  // Handle process communication
  clojureProcess.stdout?.on('data', (data) => {
    messageHandler(data.toString());
  });

  clojureProcess.stderr?.on('data', (data) => {
    console.error('❌ ClojureScript stderr:', data.toString());
  });

  clojureProcess.on('error', (error) => {
    console.error('❌ ClojureScript process error:', error);
    vscode.window.showErrorMessage(`REPLTILE ClojureScript process failed: ${error.message}`);
  });

  clojureProcess.on('exit', (code) => {
    console.log(`🔄 ClojureScript process exited with code: ${code}`);
    clojureProcess = null;
  });
}

export function sendToClojureScript(message: any) {
  if (clojureProcess && clojureProcess.stdin) {
    const data = JSON.stringify(message) + '\n';
    clojureProcess.stdin.write(data);
  }
}

export function killClojureScriptProcess() {
  if (clojureProcess) {
    clojureProcess.kill();
    clojureProcess = null;
  }
}

export function isClojureScriptProcessRunning(): boolean {
  return clojureProcess !== null;
} 