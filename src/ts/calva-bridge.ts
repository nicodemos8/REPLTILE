import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
import { getStandaloneNRepl } from './standalone-nrepl';

// Ponte entre ClojureScript (processo filho) e Calva API (processo de extensão)
// Este arquivo deve ser importado e usado pelo extension.ts

let calvaApi: any = null;

// Detecta o tipo de projeto baseado nos arquivos na raiz
function detectProjectType(rootPath: string): 'deps.edn' | 'Leiningen' | null {
    const depsEdnPath = path.join(rootPath, 'deps.edn');
    const projectCljPath = path.join(rootPath, 'project.clj');

    if (fs.existsSync(depsEdnPath)) {
        console.log('[calva-bridge] Project deps.edn detected');
        return 'deps.edn';
    } else if (fs.existsSync(projectCljPath)) {
        console.log('[calva-bridge] Project Leiningen detected');
        return 'Leiningen';
    }

    console.log('[calva-bridge] Project type not detected');
    return null;
}

// Verifica se o projeto tem o alias :repltile configurado corretamente
function checkProjectDependencies(rootPath: string, projectType: 'deps.edn' | 'Leiningen'): { hasREPLTILEAlias: boolean, errorMessage?: string, instructions?: string, detectedProfile?: string } {
    try {
        if (projectType === 'deps.edn') {
            const depsPath = path.join(rootPath, 'deps.edn');
            if (fs.existsSync(depsPath)) {
                const content = fs.readFileSync(depsPath, 'utf8');
                console.log(`[calva-bridge] Analyzing deps.edn: ${depsPath}`);

                // Procura especificamente pelo alias :repltile
                const repltileAliasMatch = content.match(/:repltile\s*\{[\s\S]*?cider\/cider-nrepl[\s\S]*?\}/);
                const hasREPLTILEAlias = !!repltileAliasMatch;

                console.log(`[calva-bridge] Alias :repltile found: ${hasREPLTILEAlias}`);

                if (hasREPLTILEAlias) {
                    console.log('[calva-bridge] ✅ Project correctly configured with :repltile alias');
                    return { hasREPLTILEAlias: true };
                } else {
                    const errorMessage = 'Alias :repltile not found in deps.edn';
                    const instructions =
                        `To use REPLTILE, add this alias to your deps.edn:
:aliases {
  :repltile {:extra-deps {nrepl/nrepl {:mvn/version "1.0.0"}
                        cider/cider-nrepl {:mvn/version "0.28.5"}}
            :main-opts ["-m" "nrepl.cmdline" "--middleware" "[cider.nrepl/cider-middleware]"]
            :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}}

Then restart REPLTILE to use the :repltile alias automatically.`;

                    console.log(`[calva-bridge] ❌ ${errorMessage}`);
                    return { hasREPLTILEAlias: false, errorMessage, instructions };
                }
            } else {
                const errorMessage = 'deps.edn file not found';
                const instructions = `Create a deps.edn file in the project root with the :repltile alias:

{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        org.clojure/tools.namespace {:mvn/version "1.4.4"}}
 :aliases {:repltile {:extra-deps {nrepl/nrepl {:mvn/version "1.0.0"}
                                 cider/cider-nrepl {:mvn/version "0.28.5"}}
                    :main-opts ["-m" "nrepl.cmdline" "--middleware" "[cider.nrepl/cider-middleware]"]
                    :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}}}`;

                return { hasREPLTILEAlias: false, errorMessage, instructions };
            }
        } else if (projectType === 'Leiningen') {
            const projectPath = path.join(rootPath, 'project.clj');
            if (fs.existsSync(projectPath)) {
                const content = fs.readFileSync(projectPath, 'utf8');
                console.log(`[calva-bridge] Analyzing project.clj: ${projectPath}`);

                // Look for profiles with proper cider-nrepl configuration
                let profilesContent = '';
                let detectedProfile: string | undefined = undefined;
                let hasProperConfig = false;

                // Primary method: Manual brace counting (more robust)
                const profilesIndex = content.indexOf(':profiles');
                if (profilesIndex !== -1) {
                    console.log(`[calva-bridge] Found :profiles keyword at position ${profilesIndex}`);

                    // Find the opening brace after :profiles
                    const afterProfiles = content.substring(profilesIndex);
                    const openBraceIndex = afterProfiles.indexOf('{');

                    if (openBraceIndex !== -1) {
                        console.log(`[calva-bridge] Found opening brace at position ${openBraceIndex} after :profiles`);

                        // Extract from after the opening brace
                        const startContent = afterProfiles.substring(openBraceIndex + 1);

                        // Count braces to find the matching closing brace
                        let braceCount = 1;
                        let endIndex = 0;

                        for (let i = 0; i < startContent.length && braceCount > 0; i++) {
                            if (startContent[i] === '{') {
                                braceCount++;
                            } else if (startContent[i] === '}') {
                                braceCount--;
                            }
                            endIndex = i;
                        }

                        if (braceCount === 0) {
                            profilesContent = startContent.substring(0, endIndex);
                            console.log(`[calva-bridge] Successfully extracted profiles content (${profilesContent.length} chars)`);
                            console.log(`[calva-bridge] First 500 chars of profiles: ${profilesContent.substring(0, 500)}...`);
                        } else {
                            console.log(`[calva-bridge] Could not find matching closing brace for :profiles (braceCount=${braceCount})`);
                        }
                    } else {
                        console.log(`[calva-bridge] Could not find opening brace after :profiles`);
                    }
                } else {
                    console.log(`[calva-bridge] No :profiles keyword found in project.clj`);
                }

                // Fallback to regex if manual method failed
                if (!profilesContent) {
                    console.log(`[calva-bridge] Manual extraction failed, trying regex fallback...`);
                    const profilesMatch = content.match(/:profiles\s*\{([\s\S]*?)\}\s*(?:\s*:|\s*$)/);
                    if (profilesMatch) {
                        profilesContent = profilesMatch[1];
                        console.log(`[calva-bridge] Regex extracted profiles content (${profilesContent.length} chars)`);
                        console.log(`[calva-bridge] First 500 chars of profiles: ${profilesContent.substring(0, 500)}...`);
                    } else {
                        console.log(`[calva-bridge] Regex also failed to extract profiles content`);
                    }
                }

                // Process the extracted profiles content
                if (profilesContent) {
                    // Define preferred profiles in order (most specific first)
                    const preferredProfiles = ['repltile', 'dev', 'development'];

                    for (const profileName of preferredProfiles) {
                        console.log(`[calva-bridge] Looking for profile :${profileName}...`);

                        // Improved detection: look for the profile start and find its content
                        const profileStartPattern = new RegExp(`:${profileName}\\s*\\{`);
                        const profileStartMatch = profilesContent.search(profileStartPattern);

                        if (profileStartMatch !== -1) {
                            console.log(`[calva-bridge] Found profile :${profileName} at position ${profileStartMatch}`);

                            // Extract content after profile start to analyze
                            const afterProfileStart = profilesContent.substring(profileStartMatch);

                            // Look for key indicators in a reasonable section after the profile
                            const sectionToAnalyze = afterProfileStart.substring(0, 2000); // Analyze first 2000 chars

                            const hasCiderNrepl = sectionToAnalyze.includes('cider/cider-nrepl');
                            const hasNreplMiddleware = sectionToAnalyze.includes('cider.nrepl/cider-middleware') ||
                                sectionToAnalyze.includes('nrepl-middleware');

                            console.log(`[calva-bridge] Profile :${profileName} - has cider-nrepl: ${hasCiderNrepl}, has middleware: ${hasNreplMiddleware}`);
                            console.log(`[calva-bridge] Section analyzed (first 300 chars): ${sectionToAnalyze.substring(0, 300)}...`);

                            if (hasCiderNrepl && hasNreplMiddleware) {
                                detectedProfile = profileName;
                                hasProperConfig = true;
                                console.log(`[calva-bridge] ✅ Found properly configured profile: :${profileName}`);
                                break;
                            } else if (hasCiderNrepl) {
                                console.log(`[calva-bridge] ⚠️ Profile :${profileName} has cider-nrepl but may be missing middleware config`);
                                if (!detectedProfile) {
                                    detectedProfile = profileName; // Fallback option
                                }
                            }
                        } else {
                            console.log(`[calva-bridge] Profile :${profileName} not found`);
                        }
                    }

                    if (!detectedProfile) {
                        console.log(`[calva-bridge] No profile detected. Trying simpler detection...`);
                        // Fallback: look for any profile with cider-nrepl
                        if (profilesContent.includes('cider/cider-nrepl')) {
                            console.log(`[calva-bridge] Found cider-nrepl somewhere in profiles`);
                            // Try to find which profile contains it
                            const lines = profilesContent.split('\n');
                            let currentProfile = null;
                            for (const line of lines) {
                                const profileMatch = line.match(/:([a-zA-Z0-9-]+)\s*\{/);
                                if (profileMatch) {
                                    currentProfile = profileMatch[1];
                                }
                                if (line.includes('cider/cider-nrepl') && currentProfile) {
                                    console.log(`[calva-bridge] Found cider-nrepl in profile :${currentProfile}`);
                                    detectedProfile = currentProfile;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    console.log(`[calva-bridge] No profiles content extracted - cannot detect profiles`);
                }

                // Also check if tools.namespace is available (in main deps or any profile)
                const hasToolsNamespace = content.includes('org.clojure/tools.namespace');
                console.log(`[calva-bridge] project.clj has tools.namespace: ${hasToolsNamespace}`);

                if (hasProperConfig && hasToolsNamespace) {
                    console.log(`[calva-bridge] ✅ Leiningen project correctly configured with profile :${detectedProfile}`);
                    return { hasREPLTILEAlias: true, detectedProfile };
                } else {
                    const errorMessage = hasProperConfig ?
                        'tools.namespace not found in project.clj' :
                        'No properly configured cider-nrepl profile found in project.clj';

                    const missing = [];
                    if (!hasProperConfig) missing.push('cider-nrepl profile with middleware');
                    if (!hasToolsNamespace) missing.push('org.clojure/tools.namespace');

                    const instructions =
                        `To use REPLTILE with Leiningen, ensure your project.clj has:
:profiles 
{ [... your existing profiles ...]
:repltile {:dependencies [[nrepl/nrepl "1.0.0"]
                                    [cider/cider-nrepl "0.28.5"]]
                    :plugins [[cider/cider-nrepl "0.28.5"]]
                    :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}
                    }
 :aliasess 
 { [... your existing aliases ...]
  "repltile"         ["with-profile" "+repltile" "repl"]
 }


${detectedProfile ? `Detected profile: :${detectedProfile} (will be used automatically)` : ''}
Missing: ${missing.join(', ')}`;

                    console.log(`[calva-bridge] ❌ ${errorMessage}`);
                    return { hasREPLTILEAlias: detectedProfile ? true : false, errorMessage, instructions, detectedProfile };
                }
            } else {
                const errorMessage = 'project.clj file not found';
                const instructions = `Create a project.clj file in the project root:

(defproject my-project "0.1.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.namespace "1.4.4"]]
  :profiles {:repltile {:dependencies [[nrepl/nrepl "1.0.0"]
                                      [cider/cider-nrepl "0.28.5"]]
                       :plugins [[cider/cider-nrepl "0.28.5"]]
                       :repl-options {:nrepl-middleware [cider.nrepl/cider-middleware]}
                       :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}})`;

                return { hasREPLTILEAlias: false, errorMessage, instructions };
            }
        }

        return { hasREPLTILEAlias: false, errorMessage: 'Project type not supported' };
    } catch (error) {
        console.error('[calva-bridge] Error checking project dependencies:', error);
        return {
            hasREPLTILEAlias: false,
            errorMessage: 'Error analyzing project files',
            instructions: 'Please ensure your deps.edn or project.clj files are valid.'
        };
    }
}

// Configura as sequências de conexão do Calva baseado no alias :repltile
async function configureCalvaConnectSequences(projectType: 'deps.edn' | 'Leiningen', rootPath: string, hasREPLTILEAlias: boolean, detectedProfile?: string) {
    const config = vscode.workspace.getConfiguration();

    console.log(`[calva-bridge] Configuring REPL for project in: ${rootPath}`);
    console.log(`[calva-bridge] Project type: ${projectType}`);
    console.log(`[calva-bridge] Has :repltile alias: ${hasREPLTILEAlias}`);
    console.log(`[calva-bridge] Detected profile: ${detectedProfile || 'none'}`);

    let customJackInCommandLine = '';

    if (projectType === 'deps.edn') {
        // Usa o alias :repltile do projeto
        customJackInCommandLine = `clojure -A:repltile`;
        console.log('[calva-bridge] ✅ Using project :repltile alias');
    } else if (projectType === 'Leiningen') {
        customJackInCommandLine = `lein with-profile +repltile repl`;
        console.log(`[calva-bridge] ✅ Using Leiningen with profile :${detectedProfile}`);
    }

    const connectSequences = [
        {
            "projectType": projectType,
            "name": `REPLTILE-JackIn ${projectType} REPL (start) - ${path.basename(rootPath)}`,
            "autoSelectForJackIn": true,
            "projectRootPath": ["."],
            "cljsType": "none",
            "customJackInCommandLine": customJackInCommandLine,
            "menuSelections": {
                "cljAliases": ["repltile"],
                "leinProfiles": [
                    "+repltile"
                ],
                "leinAlias": "repltile"
            }
        },
        {
            "projectType": projectType,
            "name": `REPLTILE-Connect ${projectType} REPL (connect) - ${path.basename(rootPath)}`,
            "autoSelectForConnect": true,
            "projectRootPath": ["."],
            "cljsType": "none"
        }
    ];

    // Log the menu selections for debugging
    const menuSelections = connectSequences[0].menuSelections;
    if (menuSelections && projectType === 'deps.edn' && menuSelections.cljAliases && menuSelections.cljAliases.length > 0) {
        console.log(`[calva-bridge] ✅ Auto-selecting deps.edn aliases: ${menuSelections.cljAliases.join(', ')}`);
    }
    if (menuSelections && projectType === 'Leiningen' && menuSelections.leinProfiles && menuSelections.leinProfiles.length > 0) {
        console.log(`[calva-bridge] ✅ Auto-selecting Leiningen profiles: ${menuSelections.leinProfiles.join(', ')}`);
    }

    try {
        await config.update('calva.replConnectSequences', connectSequences, vscode.ConfigurationTarget.Workspace);
        await config.update('calva.autoConnectRepl', true, vscode.ConfigurationTarget.Workspace);
        console.log(`[calva-bridge] ✅ Calva settings updated for project ${projectType}`);
        console.log(`[calva-bridge] ✅ Command: ${customJackInCommandLine}`);
    } catch (error) {
        console.error('[calva-bridge] ❌ Error configuring Calva sequences:', error);
    }
}

function getCalvaApi() {
    if (calvaApi) return calvaApi;
    const calvaExt = vscode.extensions.getExtension('betterthantomorrow.calva');
    if (calvaExt) {
        if (!calvaExt.isActive) {
            // Ativa a extensão Calva se necessário
            calvaExt.activate();
        }
        calvaApi = calvaExt.exports?.v1;
        return calvaApi;
    }
    return null;
}

async function getCalvaReplPortFromFile(projectRootPath: string): Promise<number | null> {
    const nreplPortFile = path.join(projectRootPath, '.nrepl-port');

    if (!fs.existsSync(nreplPortFile)) {
        console.log(`[calva-bridge] .nrepl-port file not found: ${nreplPortFile}`);
        return null;
    }

    try {
        const content = fs.readFileSync(nreplPortFile, 'utf8').trim();
        const port = parseInt(content, 10);

        if (isNaN(port) || port <= 0) {
            console.log(`[calva-bridge] Invalid content in .nrepl-port: "${content}"`);
            return null;
        }

        console.log(`[calva-bridge] nREPL port found in .nrepl-port: ${port}`);
        return port;
    } catch (error) {
        console.log(`[calva-bridge] Error reading .nrepl-port: ${error}`);
        return null;
    }
}

function deleteNreplPortFile(projectRootPath: string): void {
    const nreplPortFile = path.join(projectRootPath, '.nrepl-port');

    if (fs.existsSync(nreplPortFile)) {
        try {
            fs.unlinkSync(nreplPortFile);
            console.log(`[calva-bridge] .nrepl-port file deleted: ${nreplPortFile}`);
        } catch (error) {
            console.log(`[calva-bridge] Error deleting .nrepl-port: ${error}`);
        }
    } else {
        console.log(`[calva-bridge] .nrepl-port file does not exist for deletion: ${nreplPortFile}`);
    }
}

// Adiciona alias temporário ao deps.edn para incluir cider-nrepl
function addTemporaryAlias(projectRootPath: string): boolean {
    const depsPath = path.join(projectRootPath, 'deps.edn');

    if (!fs.existsSync(depsPath)) {
        console.log(`[calva-bridge] deps.edn not found: ${depsPath}`);
        return false;
    }

    try {
        const content = fs.readFileSync(depsPath, 'utf8');

        // Verifica se já tem o alias temporário
        if (content.includes(':repltile-temp')) {
            console.log('[calva-bridge] Alias :repltile-temp already exists in deps.edn');
            return true;
        }

        // Procura pela seção :aliases ou cria uma nova
        let newContent: string;

        if (content.includes(':aliases')) {
            // Adiciona o alias à seção existente
            // Procura por :aliases { e adiciona após a abertura
            const aliasesRegex = /(:aliases\s*\{)/;
            const match = content.match(aliasesRegex);

            if (match) {
                const aliasEntry = '\n  :repltile-temp {:extra-deps {nrepl/nrepl {:mvn/version "1.0.0"} cider/cider-nrepl {:mvn/version "0.28.5"}}}';
                newContent = content.replace(aliasesRegex, `$1${aliasEntry}`);
            } else {
                // Fallback: adiciona no final do arquivo
                newContent = content.replace(/}$/, `  :aliases {:repltile-temp {:extra-deps {nrepl/nrepl {:mvn/version "1.0.0"} cider/cider-nrepl {:mvn/version "0.28.5"}}}}\n}`);
            }
        } else {
            // Adiciona nova seção :aliases
            newContent = content.replace(/}$/, `  :aliases {:repltile-temp {:extra-deps {nrepl/nrepl {:mvn/version "1.0.0"} cider/cider-nrepl {:mvn/version "0.28.5"}}}}\n}`);
        }

        // Faz backup do arquivo original
        fs.writeFileSync(`${depsPath}.repltile-backup`, content, 'utf8');

        // Escreve o novo conteúdo
        fs.writeFileSync(depsPath, newContent, 'utf8');

        console.log('[calva-bridge] ✅ Temporary :repltile-temp alias added to deps.edn');
        console.log('[calva-bridge] ✅ Backup created in deps.edn.repltile-backup');

        return true;
    } catch (error) {
        console.error(`[calva-bridge] ❌ Error adding temporary alias: ${error}`);
        return false;
    }
}

// Remove alias temporário e restaura o deps.edn original
function removeTemporaryAlias(projectRootPath: string): void {
    const depsPath = path.join(projectRootPath, 'deps.edn');
    const backupPath = `${depsPath}.repltile-backup`;

    if (fs.existsSync(backupPath)) {
        try {
            const backupContent = fs.readFileSync(backupPath, 'utf8');
            fs.writeFileSync(depsPath, backupContent, 'utf8');
            fs.unlinkSync(backupPath);
            console.log('[calva-bridge] ✅ deps.edn restored from backup');
        } catch (error) {
            console.error(`[calva-bridge] ❌ Error restoring deps.edn: ${error}`);
        }
    } else {
        console.log('[calva-bridge] deps.edn backup not found for restoration');
    }
}

// Handler para mensagens vindas do ClojureScript
export async function handleCalvaBridgeMessage(message: any, sendToCljs: (msg: any) => void, context: vscode.ExtensionContext) {
    if (!message || !message.type) return;

    // Only handle REPL lifecycle, NOT evaluation
    if (message.type === 'start-jackin') {
        try {
            console.log('[calva-bridge] 🚀 Starting standalone nREPL...');

            // Encontra o workspace do projeto
            const projectRootPath = findProjectWorkspace();
            if (!projectRootPath) {
                sendToCljs({ type: 'repl-connection-error', error: 'No project workspace found' });
                return;
            }

            const standaloneNRepl = getStandaloneNRepl(context.extensionPath);

            // Check current REPL status
            const status = standaloneNRepl.getStatus();
            console.log(`[calva-bridge] Current REPL status:`, status);

            // If already starting for same project, reject
            if (status.starting && status.project === projectRootPath) {
                console.log(`[calva-bridge] ⚠️ REPL already starting for project: ${projectRootPath}`);
                sendToCljs({
                    type: 'repl-connection-error',
                    error: 'REPL startup already in progress for this project. Please wait or stop current startup first.'
                });
                return;
            }

            // If running for same project, return existing port
            if (status.running && status.project === projectRootPath) {
                console.log(`[calva-bridge] ✅ REPL already running for project: ${projectRootPath} on port ${status.port}`);
                sendToCljs({ type: 'jackin-success', port: status.port });
                return;
            }

            // Deleta o arquivo .nrepl-port antes do start-jackin
            deleteNreplPortFile(projectRootPath);

            // ✅ NOVA ABORDAGEM: Usar nREPL standalone empacotado
            console.log('[calva-bridge] 🔧 Using standalone nREPL with bundled dependencies');
            console.log(`[calva-bridge] 📁 Project path: ${projectRootPath}`);
            console.log(`[calva-bridge] 📦 Extension path: ${context.extensionPath}`);

            const port = await standaloneNRepl.startREPL(projectRootPath);

            console.log(`[calva-bridge] ✅ Standalone REPLTILE started successfully on port ${port}`);
            sendToCljs({ type: 'jackin-success', port });

        } catch (error) {
            console.error('[calva-bridge] ❌ Standalone REPLTILE failed:', error);

            // Check if error is due to cancellation or other reason
            let errorMessage = 'Failed to start standalone nREPL';
            let isCancellation = false;

            if (error instanceof Error) {
                errorMessage = error.message;
                isCancellation = error.message.includes('cancelled') || error.message.includes('already in progress');
            }

            sendToCljs({
                type: 'repl-connection-error',
                error: errorMessage,
                cancelled: isCancellation,
                instructions: isCancellation ?
                    'REPL startup was cancelled or already in progress.' :
                    'Make sure Java is installed and try again. Check Output > REPLTILE for more details.'
            });
        }
    } else if (message.type === 'stop-jackin') {
        try {
            console.log('[calva-bridge] 🛑 Stopping standalone REPLTILE...');
            const standaloneNRepl = getStandaloneNRepl(context.extensionPath);

            const status = standaloneNRepl.getStatus();
            console.log(`[calva-bridge] REPL status before stop:`, status);

            if (status.starting) {
                console.log('[calva-bridge] 🚫 Cancelling REPLTILE startup in progress...');
                standaloneNRepl.cancelStartup();
                sendToCljs({
                    type: 'jackin-stopped',
                    message: 'REPLTILE startup cancelled',
                    cancelled: true
                });
            } else if (status.running) {
                console.log('[calva-bridge] 🛑 Stopping running REPLTILE...');
                await standaloneNRepl.stopAndWait();
                sendToCljs({
                    type: 'jackin-stopped',
                    message: 'REPLTILE has been disconnected - see you later alligator! ✌️'
                });
            } else {
                console.log('[calva-bridge] ℹ️ No REPL process to stop');
                sendToCljs({
                    type: 'jackin-stopped',
                    message: 'No REPLTILE process was running'
                });
            }
        } catch (e) {
            console.error('[calva-bridge] ❌ Error stopping REPLTILE:', e);
            sendToCljs({ type: 'repl-connection-error', error: String(e) });
        }
    }
    // REMOVED: eval-code handler - all evaluation now done by repl-tooling!
}

// Função para encontrar o workspace do projeto atual (não o do plugin)
function findProjectWorkspace(): string | null {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
        return null;
    }

    // Se há um editor ativo, tenta encontrar o workspace baseado no arquivo ativo
    const activeEditor = vscode.window.activeTextEditor;
    if (activeEditor) {
        const activeFilePath = activeEditor.document.uri.fsPath;
        console.log(`[calva-bridge] Active file: ${activeFilePath}`);

        // Encontra o workspace folder que contém o arquivo ativo
        for (const folder of workspaceFolders) {
            if (activeFilePath.startsWith(folder.uri.fsPath)) {
                // Verifica se não é o workspace do plugin REPLTILE
                if (!folder.uri.fsPath.includes('REPLTILE_ui') && !folder.name.includes('REPLTILE')) {
                    console.log(`[calva-bridge] Project workspace found: ${folder.uri.fsPath}`);
                    return folder.uri.fsPath;
                }
            }
        }
    }

    // Se não encontrou baseado no arquivo ativo, procura o primeiro workspace que não seja o REPLTILE
    for (const folder of workspaceFolders) {
        if (!folder.uri.fsPath.includes('REPLTILE_ui') && !folder.name.includes('REPLTILE')) {
            console.log(`[calva-bridge] Project workspace found (fallback): ${folder.uri.fsPath}`);
            return folder.uri.fsPath;
        }
    }

    console.log('[calva-bridge] No project workspace found (only REPLTILE)');
    return null;
}

// Função exportada para configurar automaticamente o Calva na ativação da extensão
export async function autoConfigureCalva() {
    const projectRootPath = findProjectWorkspace();
    if (!projectRootPath) {
        console.log('[calva-bridge] No project workspace found for automatic configuration');
        return;
    }

    const projectType = detectProjectType(projectRootPath);

    if (projectType) {
        console.log(`[calva-bridge] Automatically configuring Calva for project ${projectType} in ${projectRootPath}...`);
        const { hasREPLTILEAlias, detectedProfile } = checkProjectDependencies(projectRootPath, projectType);
        await configureCalvaConnectSequences(projectType, projectRootPath, hasREPLTILEAlias, detectedProfile);
    } else {
        console.log('[calva-bridge] Automatic configuration: Project type not detected');
    }
} 