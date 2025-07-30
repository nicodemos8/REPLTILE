import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import { spawn } from 'child_process';
import { logger } from '../core/logger';
import { sendToClojureScript } from '../clojure/process-manager';

// ==============================================
// DEPENDENCY MANAGEMENT SYSTEM
// ==============================================

/**
 * Dependency configuration files to watch
 */
const DEPENDENCY_FILES = [
  'deps.edn',
  'project.clj',
  'shadow-cljs.edn',
  'build.boot',
  'package.json'
];

// Debounce system to prevent multiple simultaneous resolutions
let resolutionInProgress = false;
let lastResolutionTime = 0;
let dependencyFileWatchers: vscode.FileSystemWatcher[] = [];

/**
 * Initialize dependency file watchers
 */
export function initializeDependencyWatchers() {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  if (!workspaceFolders) {
    return;
  }

  // Create watchers for each dependency file type
  DEPENDENCY_FILES.forEach(filename => {
    const pattern = `**/${filename}`;
    const watcher = vscode.workspace.createFileSystemWatcher(pattern);

    // Watch for changes, creation, and deletion
    watcher.onDidChange(uri => handleDependencyFileChange(uri, 'changed'));
    watcher.onDidCreate(uri => handleDependencyFileChange(uri, 'created'));
    watcher.onDidDelete(uri => handleDependencyFileChange(uri, 'deleted'));

    dependencyFileWatchers.push(watcher);
  });
}

/**
 * Handle dependency file changes
 */
function handleDependencyFileChange(uri: vscode.Uri, changeType: string) {
  const filename = path.basename(uri.fsPath);
  logger.info(`📁 [DEPS-EXT] Dependency file ${changeType}: ${filename}`);

  // Notify ClojureScript about file change
  sendToClojureScript({
    type: 'file-change',
    filename: filename,
    fullPath: uri.fsPath,
    changeType: changeType
  });
}

/**
 * Check if a file exists in the user's project workspace (not REPLTILE workspace)
 */
export function checkFileExists(filename: string, configType: string) {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  logger.info(`🔍 [DEPS-DEBUG] Checking file: ${filename} for config: ${configType}`);
  logger.info(`🔍 [DEPS-DEBUG] Available workspaces: ${workspaceFolders?.map(w => w.name).join(', ') || 'none'}`);

  if (!workspaceFolders) {
    logger.warn(`📁 Config check ${configType} (${filename}): no workspace folders found`);
    sendToClojureScript({
      type: 'config-detected',
      'config-type': configType,
      exists: false
    });
    return;
  }

  // Find the current project workspace (not REPLTILE workspace)
  const currentProject = findCurrentProject(workspaceFolders);
  logger.info(`🔍 [DEPS-DEBUG] Current project found: ${currentProject?.name || 'none'}`);
  logger.info(`🔍 [DEPS-DEBUG] Project path: ${currentProject?.uri.fsPath || 'none'}`);

  if (!currentProject) {
    logger.warn(`📁 Config check ${configType} (${filename}): no project workspace found`);
    sendToClojureScript({
      type: 'config-detected',
      'config-type': configType,
      exists: false
    });
    return;
  }

  // Check if file exists in the project workspace
  const filePath = path.join(currentProject.uri.fsPath, filename);
  logger.info(`🔍 [DEPS-DEBUG] Checking full path: ${filePath}`);

  const exists = fs.existsSync(filePath);
  logger.info(`📁 [DEPS] Config check ${configType} (${filename}): ${exists ? 'FOUND' : 'not found'} in ${currentProject.name}`);

  // List all files in project root for debugging
  try {
    const files = fs.readdirSync(currentProject.uri.fsPath);
    logger.info(`🔍 [DEPS-DEBUG] Files in project root: ${files.join(', ')}`);
  } catch (e) {
    logger.debug(`🔍 [DEPS-DEBUG] Could not read project directory: ${e}`);
  }

  // Always send response to avoid ClojureScript hanging
  sendToClojureScript({
    type: 'config-detected',
    'config-type': configType,
    exists: exists
  });
}

/**
 * Check if dependency directories exist (indicates resolved dependencies)
 */
export function checkDirsExist(dirs: string[], configType: string) {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  if (!workspaceFolders) {
    logger.warn(`📋 Dependencies check ${configType}: no workspace folders found`);
    sendToClojureScript({
      type: 'dirs-checked',
      'config-type': configType,
      resolved: false
    });
    return;
  }

  // Find the current project workspace (not REPLTILE workspace)
  const currentProject = findCurrentProject(workspaceFolders);
  if (!currentProject) {
    logger.warn(`📋 Dependencies check ${configType}: no project workspace found`);
    sendToClojureScript({
      type: 'dirs-checked',
      'config-type': configType,
      resolved: false
    });
    return;
  }

  let resolved = false;

  // Check each directory
  for (const dir of dirs) {
    let normalizedDir = dir;

    // Handle home directory (~)
    if (normalizedDir.startsWith('~')) {
      normalizedDir = normalizedDir.replace('~', os.homedir());
    }

    // If it's a relative path, check in the project workspace
    if (!path.isAbsolute(normalizedDir)) {
      const fullPath = path.join(currentProject.uri.fsPath, normalizedDir);
      if (fs.existsSync(fullPath)) {
        resolved = true;
        break;
      }
    } else {
      // Absolute path (like ~/.m2/repository)
      if (fs.existsSync(normalizedDir)) {
        resolved = true;
        break;
      }
    }
  }

  logger.info(`📋 Dependencies check ${configType}: ${resolved ? 'resolved' : 'unresolved'} in ${currentProject.name}`);

  sendToClojureScript({
    type: 'dirs-checked',
    'config-type': configType,
    resolved: resolved
  });
}

/**
 * Execute dependency resolution command with debounce protection
 */
export function resolveDependencies(configType: string, command: string, description: string) {
  const currentTime = Date.now();
  const timeSinceLastResolution = currentTime - lastResolutionTime;

  logger.info(`🔧 [DEPS-EXT] === STARTING DEPENDENCY RESOLUTION ===`);
  logger.info(`🔧 [DEPS-EXT] Config Type: ${configType}`);
  logger.info(`🔧 [DEPS-EXT] Command: ${command}`);
  logger.info(`🔧 [DEPS-EXT] Description: ${description}`);
  logger.info(`🔧 [DEPS-EXT] Resolution in progress: ${resolutionInProgress}`);
  logger.info(`🔧 [DEPS-EXT] Time since last resolution: ${timeSinceLastResolution}ms`);

  // Debounce protection: prevent multiple calls within 3 seconds
  if (timeSinceLastResolution < 3000) {
    logger.warn(`🔄 [DEPS-EXT] IGNORING duplicate resolution request (debounce protection)`);
    return;
  }

  // If resolution is already in progress, ignore
  if (resolutionInProgress) {
    logger.warn(`🔄 [DEPS-EXT] IGNORING resolution request - already in progress`);
    return;
  }

  // Mark resolution as in progress
  resolutionInProgress = true;
  lastResolutionTime = currentTime;

  const workspaceFolders = vscode.workspace.workspaceFolders;
  if (!workspaceFolders) {
    logger.error('❌ [DEPS-EXT] No workspace folder found for dependency resolution');
    resolutionInProgress = false;
    sendToClojureScript({
      type: 'resolution-complete',
      'config-type': configType,
      success: false,
      error: 'No workspace folder found'
    });
    return;
  }

  // Find the current project workspace (not REPLTILE workspace)
  const currentProject = findCurrentProject(workspaceFolders);
  if (!currentProject) {
    logger.error('❌ [DEPS-EXT] No project workspace found for dependency resolution');
    resolutionInProgress = false;
    sendToClojureScript({
      type: 'resolution-complete',
      'config-type': configType,
      success: false,
      error: 'No project workspace found'
    });
    return;
  }

  const workspaceRoot = currentProject.uri.fsPath;
  logger.info(`🔧 [DEPS-EXT] Using project workspace: ${currentProject.name}`);
  logger.info(`🔧 [DEPS-EXT] Working directory: ${workspaceRoot}`);

  // Show progress notification
  vscode.window.withProgress({
    location: vscode.ProgressLocation.Notification,
    title: `Resolving ${description} dependencies...`,
    cancellable: true
  }, async (progress, token) => {

    return new Promise<void>((resolve) => {
      // Parse command for shell execution
      const parts = command.split(' ');
      const cmd = parts[0];
      const args = parts.slice(1);

      logger.info(`📦 [DEPS-EXT] Executing command: ${cmd}`);
      logger.info(`📦 [DEPS-EXT] Arguments: ${args.join(' ')}`);

      const child = spawn(cmd, args, {
        cwd: workspaceRoot,
        shell: true,
        stdio: ['ignore', 'pipe', 'pipe']
      });

      let output = '';
      let error = '';

      child.stdout?.on('data', (data) => {
        const chunk = data.toString();
        output += chunk;
        logger.debug(`📦 [DEPS-EXT] STDOUT: ${chunk.trim()}`);
      });

      child.stderr?.on('data', (data) => {
        const chunk = data.toString();
        error += chunk;
        logger.debug(`📦 [DEPS-EXT] STDERR: ${chunk.trim()}`);
      });

      child.on('close', (code) => {
        const success = code === 0;
        logger.info(`📦 [DEPS-EXT] === RESOLUTION COMPLETE ===`);
        logger.info(`📦 [DEPS-EXT] Config: ${configType}`);
        logger.info(`📦 [DEPS-EXT] Exit code: ${code}`);
        logger.info(`📦 [DEPS-EXT] Success: ${success}`);

        if (output.trim()) {
          logger.info(`📦 [DEPS-EXT] Output length: ${output.length} chars`);
        }

        if (error.trim()) {
          logger.info(`📦 [DEPS-EXT] Error length: ${error.length} chars`);
        }

        // Clear resolution flag
        resolutionInProgress = false;

        // Send result back to ClojureScript
        sendToClojureScript({
          type: 'resolution-complete',
          'config-type': configType,
          success: success,
          output: output.trim(),
          error: error.trim()
        });

        // Show result notification
        if (success) {
          vscode.window.showInformationMessage(
            `✅ ${description} dependencies resolved successfully`
          );
        } else {
          vscode.window.showErrorMessage(
            `❌ Failed to resolve ${description} dependencies: ${error.trim() || 'Unknown error'}`
          );
        }

        resolve();
      });

      child.on('error', (err) => {
        logger.error(`📦 [DEPS-EXT] Process error for ${configType}:`, err);

        // Clear resolution flag
        resolutionInProgress = false;

        sendToClojureScript({
          type: 'resolution-complete',
          'config-type': configType,
          success: false,
          error: err.message
        });

        vscode.window.showErrorMessage(
          `❌ Failed to execute dependency resolution: ${err.message}`
        );

        resolve();
      });

      // Handle cancellation
      token.onCancellationRequested(() => {
        logger.info(`🛑 [DEPS-EXT] User cancelled ${configType} dependency resolution`);
        child.kill();

        // Clear resolution flag
        resolutionInProgress = false;

        sendToClojureScript({
          type: 'resolution-complete',
          'config-type': configType,
          success: false,
          error: 'Cancelled by user'
        });
        resolve();
      });
    });
  });
}

/**
 * Get workspace information
 */
export function getWorkspaceInfo() {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  const info = {
    type: 'workspace-info',
    folders: workspaceFolders?.map(folder => ({
      name: folder.name,
      uri: folder.uri.toString(),
      fsPath: folder.uri.fsPath
    })) || [],
    activeEditor: vscode.window.activeTextEditor ? {
      fileName: vscode.window.activeTextEditor.document.fileName,
      languageId: vscode.window.activeTextEditor.document.languageId
    } : null
  };

  sendToClojureScript(info);
}

export function findCurrentProject(workspaceFolders: readonly vscode.WorkspaceFolder[]): vscode.WorkspaceFolder | null {
  // If there's an active text editor, try to find the workspace folder that contains it
  const activeEditor = vscode.window.activeTextEditor;
  if (activeEditor) {
    const activeFilePath = activeEditor.document.uri.fsPath;

    // Find the workspace folder that contains the active file
    for (const folder of workspaceFolders) {
      if (activeFilePath.startsWith(folder.uri.fsPath)) {
        // Skip the REPLTILE plugin workspace
        if (!folder.uri.fsPath.includes('REPLTILE_ui') && !folder.name.includes('REPLTILE')) {
          return folder;
        }
      }
    }
  }

  // If no active file or no matching workspace, return the first non-REPLTILE workspace
  for (const folder of workspaceFolders) {
    if (!folder.uri.fsPath.includes('REPLTILE_ui') && !folder.name.includes('REPLTILE')) {
      return folder;
    }
  }

  // If only REPLTILE workspace exists, still try to use it (for development/testing)
  console.log('🔍 Only REPLTILE workspace found, using it for dependency detection');
  return workspaceFolders.length > 0 ? workspaceFolders[0] : null;
}

/**
 * Cleanup dependency watchers
 */
export function cleanupDependencyWatchers() {
  if (dependencyFileWatchers.length > 0) {
    console.log('🧹 Cleaning up dependency file watchers...');
    dependencyFileWatchers.forEach(watcher => watcher.dispose());
    dependencyFileWatchers = [];
  }
} 