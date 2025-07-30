import * as vscode from 'vscode';
import { findCurrentProject } from '../dependencies/dependency-manager';
import { getWebviewPanel } from '../webview/webview-manager';

// ==============================================
// HISTORY FILE MANAGEMENT SYSTEM  
// ==============================================

export async function createREPLTILEDirectory(): Promise<void> {
  try {
    const workspaceFolders = vscode.workspace.workspaceFolders || [];
    const currentProject = findCurrentProject(workspaceFolders);

    if (currentProject) {
      const repltileDir = vscode.Uri.joinPath(currentProject.uri, '.REPLTILE');

      try {
        await vscode.workspace.fs.stat(repltileDir);
        console.log(`📁 .REPLTILE directory already exists in ${currentProject.name}`);
      } catch {
        await vscode.workspace.fs.createDirectory(repltileDir);
        console.log(`📁 Created .REPLTILE directory in ${currentProject.name}`);
      }
    }
  } catch (error) {
    console.error('❌ Error creating .REPLTILE directory:', error);
  }
}

export async function saveHistoryFile(historyData: any, projectId: string): Promise<void> {
  try {
    const workspaceFolders = vscode.workspace.workspaceFolders || [];
    const currentProject = findCurrentProject(workspaceFolders);

    if (currentProject) {
      const repltileDir = vscode.Uri.joinPath(currentProject.uri, '.REPLTILE');
      const historyFile = vscode.Uri.joinPath(repltileDir, 'history.edn');

      // Ensure .REPLTILE directory exists
      await createREPLTILEDirectory();

      // Convert to EDN format
      const edn = historyDataToEdn(historyData);
      const content = new TextEncoder().encode(edn);

      await vscode.workspace.fs.writeFile(historyFile, content);
      console.log(`📜 History saved to ${currentProject.name}/.REPLTILE/history.edn`);
    }
  } catch (error) {
    console.error('❌ Error saving history file:', error);
  }
}

export async function loadHistoryFile(projectId: string): Promise<void> {
  try {
    const workspaceFolders = vscode.workspace.workspaceFolders || [];
    const currentProject = findCurrentProject(workspaceFolders);

    if (currentProject) {
      const historyFile = vscode.Uri.joinPath(currentProject.uri, '.REPLTILE', 'history.edn');

      try {
        const content = await vscode.workspace.fs.readFile(historyFile);
        const ednContent = new TextDecoder().decode(content);
        const historyData = parseHistoryEdn(ednContent);

        console.log(`📜 History loaded from ${currentProject.name}/.REPLTILE/history.edn`);

        // Send back to webview
        const webviewPanel = getWebviewPanel();
        if (webviewPanel) {
          webviewPanel.webview.postMessage({
            type: 'history-file-loaded',
            commands: historyData.commands || []
          });
        }
      } catch {
        console.log(`📜 No history file found for ${currentProject.name}, starting with empty history`);
        // Send empty history back to webview
        const webviewPanel = getWebviewPanel();
        if (webviewPanel) {
          webviewPanel.webview.postMessage({
            type: 'history-file-loaded',
            commands: []
          });
        }
      }
    }
  } catch (error) {
    console.error('❌ Error loading history file:', error);
  }
}

export async function clearHistoryFile(projectId: string): Promise<void> {
  try {
    const workspaceFolders = vscode.workspace.workspaceFolders || [];
    const currentProject = findCurrentProject(workspaceFolders);

    if (currentProject) {
      const historyFile = vscode.Uri.joinPath(currentProject.uri, '.REPLTILE', 'history.edn');

      try {
        await vscode.workspace.fs.delete(historyFile);
        console.log(`🧹 History file cleared for ${currentProject.name}`);
      } catch {
        console.log(`📜 No history file to clear for ${currentProject.name}`);
      }
    }
  } catch (error) {
    console.error('❌ Error clearing history file:', error);
  }
}

function historyDataToEdn(data: any): string {
  // Convert history data to simple EDN format
  const commands = data.commands || [];
  const timestamp = data.timestamp || Date.now();
  const project = data.project || '';

  let edn = '{\n';
  edn += ` :commands [\n`;

  commands.forEach((cmd: string, index: number) => {
    // Proper EDN string escaping - escape backslashes first, then quotes, then newlines
    const escapedCmd = cmd
      .replace(/\\/g, '\\\\')     // Escape backslashes
      .replace(/"/g, '\\"')       // Escape quotes  
      .replace(/\n/g, '\\n')      // Escape newlines
      .replace(/\r/g, '\\r')      // Escape carriage returns
      .replace(/\t/g, '\\t');     // Escape tabs
    edn += `  "${escapedCmd}"`;
    if (index < commands.length - 1) edn += '\n';
  });

  edn += '\n ]\n';
  edn += ` :timestamp ${timestamp}\n`;
  edn += ` :project "${project}"\n`;
  edn += '}';

  return edn;
}

function parseHistoryEdn(edn: string): any {
  try {
    // Robust EDN parsing for our history format
    // Extract commands array using more careful parsing
    const commandsMatch = edn.match(/:commands\s*\[(.*?)\]/s);
    let commands: string[] = [];

    if (commandsMatch) {
      const commandsStr = commandsMatch[1].trim();

      // Parse quoted strings more carefully - handle escaped quotes and newlines
      let pos = 0;
      while (pos < commandsStr.length) {
        // Skip whitespace
        while (pos < commandsStr.length && /\s/.test(commandsStr[pos])) pos++;

        if (pos >= commandsStr.length) break;

        // Expect a quote
        if (commandsStr[pos] !== '"') {
          pos++;
          continue;
        }

        pos++; // Skip opening quote
        let cmdStr = '';

        // Extract the string content, handling escapes
        while (pos < commandsStr.length) {
          const char = commandsStr[pos];

          if (char === '\\' && pos + 1 < commandsStr.length) {
            // Handle escape sequences
            const nextChar = commandsStr[pos + 1];
            switch (nextChar) {
              case 'n': cmdStr += '\n'; break;
              case 'r': cmdStr += '\r'; break;
              case 't': cmdStr += '\t'; break;
              case '\\': cmdStr += '\\'; break;
              case '"': cmdStr += '"'; break;
              default: cmdStr += nextChar; break;
            }
            pos += 2;
          } else if (char === '"') {
            // End of string
            pos++;
            break;
          } else {
            cmdStr += char;
            pos++;
          }
        }

        if (cmdStr.trim()) {
          commands.push(cmdStr);
        }
      }
    }

    // Extract timestamp
    const timestampMatch = edn.match(/:timestamp\s+(\d+)/);
    const timestamp = timestampMatch ? parseInt(timestampMatch[1]) : Date.now();

    // Extract project
    const projectMatch = edn.match(/:project\s+"([^"]*)"/);
    const project = projectMatch ? projectMatch[1] : '';

    console.log(`📜 Parsed ${commands.length} commands from history file`);
    return { commands, timestamp, project };
  } catch (error) {
    console.error('❌ Error parsing history EDN:', error);
    return { commands: [], timestamp: Date.now(), project: '' };
  }
} 