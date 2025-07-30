import * as vscode from 'vscode';
import * as path from 'path';
import { sendToClojureScript } from '../clojure/process-manager';
import { getWebviewPanel } from '../webview/webview-manager';

// ===== REPL SHORTCUTS IMPLEMENTATION =====

export function switchToCurrentNamespace() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const namespace = extractNamespace(editor.document.getText());
  if (!namespace) {
    vscode.window.showWarningMessage('No namespace found in current file');
    return;
  }

  // Save the file first if it has changes
  if (editor.document.isDirty) {
    editor.document.save();
  }

  const filePath = editor.document.uri.fsPath;
  const fileName = path.basename(filePath);

  // Alternative approach: Execute load-file check and in-ns separately for better namespace detection
  const loadCommand =
    `(when (nil? (find-ns '${namespace}))
             (load-file "${filePath}")
             (println "Loaded file: ${fileName}"))`;

  const switchCommand = `(in-ns '${namespace})`;

  // Execute load command first
  executeInRepl(loadCommand, `Loading file if needed: ${fileName}`);

  // Then execute switch command after a short delay to ensure proper namespace detection
  setTimeout(() => {
    executeInRepl(switchCommand, `Switching to namespace: ${namespace}`);
  }, 500);
}

export function reloadCurrentNamespace() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  // Save the file first if it has changes
  if (editor.document.isDirty) {
    editor.document.save();
  }

  const filePath = editor.document.uri.fsPath;
  const fileName = path.basename(filePath);

  // Use load-file like Cursive does, instead of require with :reload
  const command = `(load-file "${filePath}")`;
  executeInRepl(command, `Reloading file: ${fileName}`);
}

export function evaluateSelection() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const selection = editor.selection;
  if (selection.isEmpty) {
    vscode.window.showWarningMessage('Please select some code first');
    return;
  }

  const selectedText = editor.document.getText(selection).trim();
  if (!selectedText) {
    vscode.window.showWarningMessage('No code selected');
    return;
  }

  executeInRepl(selectedText, 'Evaluating selection');
}

export function evaluateCurrentForm() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const position = editor.selection.active;
  const form = findCurrentForm(editor.document, position);

  if (!form) {
    vscode.window.showWarningMessage('No form found at cursor position');
    return;
  }

  executeInRepl(form.text, 'Evaluating current form');
}

export function evaluateTopLevelForm() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const position = editor.selection.active;
  const form = findTopLevelForm(editor.document, position);

  if (!form) {
    vscode.window.showWarningMessage('No top-level form found');
    return;
  }

  executeInRepl(form.text, 'Evaluating top-level form');
}

export function evaluateCurrentLine() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const position = editor.selection.active;
  const line = editor.document.lineAt(position.line);
  const lineText = line.text.trim();

  if (!lineText || lineText.startsWith(';')) {
    vscode.window.showWarningMessage('No code found on current line');
    return;
  }

  executeInRepl(lineText, 'Evaluating current line');
}

export function loadCurrentFile() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  // Save the file first if it has changes
  if (editor.document.isDirty) {
    editor.document.save();
  }

  const filePath = editor.document.uri.fsPath;
  const command = `(load-file "${filePath}")`;
  executeInRepl(command, `Loading file: ${path.basename(filePath)}`);
}

export function clearReplOutput() {
  console.log('🧹 Clearing REPL output...');
  sendToClojureScript({ type: 'clear-output' });
}

export function runNamespaceTests() {
  console.log('🧪 Running namespace tests...');

  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== 'clojure') {
    vscode.window.showWarningMessage('Please open a Clojure file first');
    return;
  }

  const namespace = extractNamespace(editor.document.getText());
  if (!namespace) {
    vscode.window.showWarningMessage('No namespace found in current file');
    return;
  }

  // Send the command with the specific namespace from the current file
  sendToClojureScript({
    type: 'run-namespace-tests',
    ns: namespace,
    source: 'keyboard-shortcut'  // Identificar que veio do atalho/command palette
  });
}

export function focusInputField() {
  console.log('🎯 Focusing input field...');

  const webviewPanel = getWebviewPanel();
  if (!webviewPanel) {
    vscode.window.showWarningMessage('REPLTILE panel is not open. Use "Open REPLTILE Panel" first.');
    return;
  }

  // Show the webview panel first
  webviewPanel.reveal();

  // Send focus input command to ClojureScript
  sendToClojureScript({ type: 'focus-input' });
}

export function repeatLastCommand() {
  console.log('🔄 Repeating last command...');

  const webviewPanel = getWebviewPanel();
  if (!webviewPanel) {
    vscode.window.showWarningMessage('REPLTILE panel is not open. Use "Open REPLTILE Panel" first.');
    return;
  }

  // Show the webview panel first
  webviewPanel.reveal();

  // Send repeat last command to ClojureScript
  sendToClojureScript({ type: 'repeat-last-command' });
}

// Utility function to execute code in REPL
function executeInRepl(code: string, description?: string) {
  const webviewPanel = getWebviewPanel();
  if (!webviewPanel) {
    vscode.window.showWarningMessage('REPLTILE panel is not open. Use "Open REPLTILE Panel" first.');
    return;
  }

  // Show the webview panel
  webviewPanel.reveal();

  // Send code to be executed
  webviewPanel.webview.postMessage({
    type: 'execute-code',
    code: code,
    description: description || 'Executing code'
  });

  if (description) {
    console.log(`🔄 ${description}: ${code}`);
  }
}

// Função para extrair namespace de um arquivo Clojure
function extractNamespace(text: string): string | null {
  const match = text.match(/\(ns\s+([^\s)]+)/);
  return match ? match[1] : null;
}

// Find the current form at cursor position
function findCurrentForm(document: vscode.TextDocument, position: vscode.Position): { text: string, range: vscode.Range } | null {
  const text = document.getText();
  const offset = document.offsetAt(position);

  // Simple implementation: find the nearest opening paren backwards and closing paren forwards
  let start = offset;
  let openParens = 0;

  // Find start of form (go backwards until we find matching opening paren)
  while (start > 0) {
    const char = text[start];
    if (char === ')') {
      openParens++;
    } else if (char === '(') {
      if (openParens === 0) {
        break;
      }
      openParens--;
    }
    start--;
  }

  if (start === 0 && text[0] !== '(') {
    return null;
  }

  // Find end of form (go forwards until we find matching closing paren)
  let end = start + 1;
  openParens = 1;

  while (end < text.length && openParens > 0) {
    const char = text[end];
    if (char === '(') {
      openParens++;
    } else if (char === ')') {
      openParens--;
    }
    end++;
  }

  if (openParens !== 0) {
    return null;
  }

  const startPos = document.positionAt(start);
  const endPos = document.positionAt(end);
  const range = new vscode.Range(startPos, endPos);
  const formText = document.getText(range);

  return { text: formText.trim(), range };
}

// Find top-level form containing the cursor
function findTopLevelForm(document: vscode.TextDocument, position: vscode.Position): { text: string, range: vscode.Range } | null {
  const text = document.getText();
  const lines = text.split('\n');
  const currentLine = position.line;

  // Find the start of the top-level form
  let startLine = currentLine;
  while (startLine >= 0) {
    const line = lines[startLine].trim();
    if (line.startsWith('(') && !line.startsWith(';;')) {
      break;
    }
    startLine--;
  }

  if (startLine < 0) {
    return null;
  }

  // Find the end of the top-level form
  let endLine = startLine;
  let openParens = 0;
  let foundStart = false;

  for (let i = startLine; i < lines.length; i++) {
    const line = lines[i];
    for (let j = 0; j < line.length; j++) {
      const char = line[j];
      if (char === '(') {
        openParens++;
        foundStart = true;
      } else if (char === ')') {
        openParens--;
        if (foundStart && openParens === 0) {
          endLine = i;
          const startPos = new vscode.Position(startLine, 0);
          const endPos = new vscode.Position(endLine, line.length);
          const range = new vscode.Range(startPos, endPos);
          const formText = document.getText(range);
          return { text: formText.trim(), range };
        }
      }
    }
  }

  return null;
} 