import { spawn, ChildProcess } from 'child_process';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';

export class StandaloneNRepl {
  private process: ChildProcess | null = null;
  private extensionPath: string;
  private port: number = 0;
  private currentProjectPath: string | null = null;
  private startupPromiseResolve: ((value: number) => void) | null = null;
  private startupPromiseReject: ((reason: any) => void) | null = null;
  private startupTimeout: NodeJS.Timeout | null = null;
  private isStartingUp: boolean = false;

  constructor(extensionPath: string) {
    this.extensionPath = extensionPath;
  }

  private detectProjectType(projectPath: string): 'deps.edn' | 'Leiningen' | null {
    const depsEdnPath = path.join(projectPath, 'deps.edn');
    const projectCljPath = path.join(projectPath, 'project.clj');

    if (fs.existsSync(depsEdnPath)) {
      console.log(`📝 Found deps.edn project: ${depsEdnPath}`);
      return 'deps.edn';
    } else if (fs.existsSync(projectCljPath)) {
      console.log(`📝 Found Leiningen project: ${projectCljPath}`);
      return 'Leiningen';
    }

    console.log(`📝 No project file found in ${projectPath}`);
    return null;
  }

  private async getProjectClasspath(projectPath: string, projectType: 'deps.edn' | 'Leiningen'): Promise<string> {
    return new Promise((resolve, reject) => {
      let command: string;
      let args: string[];

      if (projectType === 'deps.edn') {
        command = 'clojure';
        args = ['-Spath'];
      } else if (projectType === 'Leiningen') {
        command = 'lein';
        args = ['classpath'];
      } else {
        reject(new Error(`Unsupported project type: ${projectType}`));
        return;
      }

      console.log(`🔍 Getting project classpath: ${command} ${args.join(' ')}`);

      const process = spawn(command, args, {
        cwd: projectPath,
        stdio: ['pipe', 'pipe', 'pipe']
      });

      let stdout = '';
      let stderr = '';

      process.stdout?.on('data', (data) => {
        stdout += data.toString();
      });

      process.stderr?.on('data', (data) => {
        stderr += data.toString();
      });

      process.on('close', (code) => {
        if (code === 0) {
          const classpath = stdout.trim();
          console.log(`✅ Project classpath obtained (${classpath.length} chars)`);
          resolve(classpath);
        } else {
          console.error(`❌ Failed to get project classpath: ${stderr}`);
          reject(new Error(`Failed to get project classpath: ${stderr}`));
        }
      });

      process.on('error', (error) => {
        console.error(`❌ Error running ${command}:`, error);
        reject(error);
      });
    });
  }

  private buildClasspath(projectPath: string): string {
    const libDir = path.join(this.extensionPath, 'lib');

    if (!fs.existsSync(libDir)) {
      throw new Error(`Library directory not found: ${libDir}. Run 'npm run bundle-deps' first.`);
    }

    const jarFiles = fs.readdirSync(libDir).filter(f => f.endsWith('.jar'));

    if (jarFiles.length === 0) {
      throw new Error(`No JAR files found in ${libDir}. Run 'npm run bundle-deps' first.`);
    }

    const separator = os.platform() === 'win32' ? ';' : ':';
    const repltileClasspath = jarFiles.map(jar => path.join(libDir, jar)).join(separator);

    console.log(`📚 REPLTILE classpath built with ${jarFiles.length} JARs`);
    console.log(`📂 JARs: ${jarFiles.join(', ')}`);

    return repltileClasspath;
  }

  private async buildFullClasspath(projectPath: string): Promise<string> {
    // Get REPLTILE standalone classpath
    const repltileClasspath = this.buildClasspath(projectPath);

    // Try to get project classpath
    const projectType = this.detectProjectType(projectPath);

    if (projectType) {
      try {
        const projectClasspath = await this.getProjectClasspath(projectPath, projectType);
        const separator = os.platform() === 'win32' ? ';' : ':';

        // REPLTILE dependencies first, then project dependencies
        const fullClasspath = `${repltileClasspath}${separator}${projectClasspath}`;
        console.log(`🔗 Combined classpath: REPLTILE (${repltileClasspath.split(separator).length} JARs) + Project (${projectClasspath.split(separator).length} entries)`);
        return fullClasspath;
      } catch (error) {
        console.warn(`⚠️ Failed to get project classpath, using standalone only: ${error}`);
        return repltileClasspath;
      }
    } else {
      console.log(`📝 No project dependencies found, using standalone classpath only`);
      return repltileClasspath;
    }
  }

  private findJavaExecutable(): string {
    // Tentar encontrar Java no PATH
    const javaExecutable = os.platform() === 'win32' ? 'java.exe' : 'java';

    // Verificar se Java está no PATH
    try {
      const { execSync } = require('child_process');
      execSync(`${javaExecutable} -version`, { stdio: 'pipe' });
      return javaExecutable;
    } catch (error) {
      throw new Error('Java not found in PATH. Please install Java 8+ and ensure it\'s in your PATH.');
    }
  }

  async startREPL(projectPath: string): Promise<number> {
    // Check if we're trying to start REPL for a different project
    if (this.process && this.currentProjectPath && this.currentProjectPath !== projectPath) {
      console.log(`🔄 Switching projects: ${this.currentProjectPath} -> ${projectPath}`);
      console.log('🛑 Stopping existing nREPL for different project...');
      await this.stopAndWait();
    }
    // Check if same project REPL is already running
    else if (this.process && this.currentProjectPath === projectPath) {
      console.log(`✅ nREPL already running for project: ${projectPath} on port ${this.port}`);
      return this.port;
    }
    // Check if startup is already in progress for this project
    else if (this.isStartingUp && this.currentProjectPath === projectPath) {
      console.log(`⏳ nREPL startup already in progress for project: ${projectPath}`);
      throw new Error('nREPL startup already in progress for this project');
    }
    // Stop any existing process if switching projects or if there's a stale process
    else if (this.process) {
      console.log('🔄 Stopping existing nREPL process...');
      await this.stopAndWait();
    }

    this.currentProjectPath = projectPath;
    this.isStartingUp = true;

    try {
      const classpath = await this.buildFullClasspath(projectPath);
      const javaExecutable = this.findJavaExecutable();

      console.log(`🚀 Starting standalone nREPL in project: ${projectPath}`);
      console.log(`☕ Using Java: ${javaExecutable}`);

      // Clean up any existing .nrepl-port file first
      const nreplPortFile = path.join(projectPath, '.nrepl-port');
      if (fs.existsSync(nreplPortFile)) {
        console.log(`🧹 Removing existing ${nreplPortFile}`);
        fs.unlinkSync(nreplPortFile);
      }

      // Argumentos do processo Java
      const javaArgs = [
        '-cp', classpath,
        '-Dfile.encoding=UTF-8',
        '-Duser.dir=' + projectPath,  // Garantir que o working directory é o projeto
        '-XX:-OmitStackTraceInFastThrow',
        '-Dclojure.compile.path=' + path.join(projectPath, 'target', 'classes'),
        'clojure.main',  // Usar clojure.main como main class
        '-m', 'nrepl.cmdline',  // Invocar namespace nrepl.cmdline
        '--middleware', '[cider.nrepl/cider-middleware]',
        '--port', '0'  // Porta automática
      ];

      console.log(`📋 Java command: ${javaExecutable} ${javaArgs.join(' ')}`);

      // Iniciar processo Java
      this.process = spawn(javaExecutable, javaArgs, {
        cwd: projectPath,
        stdio: ['pipe', 'pipe', 'pipe'],
        env: {
          ...process.env,
          // Garantir que o nREPL use o diretório do projeto
          'user.dir': projectPath
        }
      });

      return new Promise((resolve, reject) => {
        let output = '';
        let errorOutput = '';

        // Store promise handlers for potential cancellation
        this.startupPromiseResolve = resolve;
        this.startupPromiseReject = reject;

        this.startupTimeout = setTimeout(() => {
          console.log('⏱️ nREPL startup timeout after 30s');
          this.cancelStartup();
          reject(new Error('nREPL startup timeout (30s). Check Java installation and project permissions.'));
        }, 30000);

        this.process!.stdout?.on('data', (data) => {
          const chunk = data.toString();
          output += chunk;
          console.log(`📤 nREPL stdout: ${chunk.trim()}`);

          // Detectar porta do nREPL - pattern mais robusto
          const portMatches = [
            /nREPL server started.*port (\d+)/i,
            /nREPL.*started.*port (\d+)/i,
            /started.*port (\d+)/i,
            /port (\d+)/i
          ];

          for (const pattern of portMatches) {
            const match = chunk.match(pattern);
            if (match) {
              const port = parseInt(match[1]);
              this.port = port;
              this.isStartingUp = false;

              if (this.startupTimeout) {
                clearTimeout(this.startupTimeout);
                this.startupTimeout = null;
              }

              // Criar arquivo .nrepl-port no projeto
              fs.writeFileSync(nreplPortFile, port.toString());

              console.log(`✅ Standalone nREPL started successfully on port ${port} for project: ${projectPath}`);
              console.log(`📝 Created ${nreplPortFile}`);

              // Clear startup promise handlers
              this.startupPromiseResolve = null;
              this.startupPromiseReject = null;

              resolve(port);
              return;
            }
          }
        });

        this.process!.stderr?.on('data', (data) => {
          const chunk = data.toString();
          errorOutput += chunk;
          console.error(`📥 nREPL stderr: ${chunk.trim()}`);
        });

        this.process!.on('error', (error) => {
          console.error('❌ nREPL process error:', error);
          this.isStartingUp = false;
          if (this.startupTimeout) {
            clearTimeout(this.startupTimeout);
            this.startupTimeout = null;
          }
          this.startupPromiseResolve = null;
          this.startupPromiseReject = null;
          reject(new Error(`Failed to start nREPL process: ${error.message}`));
        });

        this.process!.on('exit', (code, signal) => {
          console.log(`🔄 nREPL process exited with code ${code}, signal ${signal}`);
          this.isStartingUp = false;
          if (this.startupTimeout) {
            clearTimeout(this.startupTimeout);
            this.startupTimeout = null;
          }

          // Only reject if we haven't successfully resolved yet
          if (this.startupPromiseReject && code !== 0) {
            this.startupPromiseReject(new Error(`nREPL process exited with code ${code}. Error output: ${errorOutput}`));
          }

          this.startupPromiseResolve = null;
          this.startupPromiseReject = null;
        });
      });

    } catch (error) {
      this.isStartingUp = false;
      this.startupPromiseResolve = null;
      this.startupPromiseReject = null;
      console.error('❌ Failed to start standalone nREPL:', error);
      throw error;
    }
  }

  stop(): void {
    if (this.process) {
      console.log('🛑 Stopping standalone nREPL...');

      // Tentar parar graciosamente primeiro
      this.process.kill('SIGTERM');

      // Forçar parada após 5 segundos
      setTimeout(() => {
        if (this.process && !this.process.killed) {
          console.log('🔨 Force killing nREPL process...');
          this.process.kill('SIGKILL');
        }
      }, 5000);

      this.process = null;
      this.port = 0;
      console.log('✅ Standalone nREPL stopped');
    }

    // Clear startup state
    this.isStartingUp = false;
    this.currentProjectPath = null;
    if (this.startupTimeout) {
      clearTimeout(this.startupTimeout);
      this.startupTimeout = null;
    }
    this.startupPromiseResolve = null;
    this.startupPromiseReject = null;
  }

  stopAndWait(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.process) {
        resolve();
        return;
      }

      console.log('🛑 Stopping standalone nREPL and waiting...');

      const cleanup = () => {
        this.process = null;
        this.port = 0;
        this.isStartingUp = false;
        this.currentProjectPath = null;
        if (this.startupTimeout) {
          clearTimeout(this.startupTimeout);
          this.startupTimeout = null;
        }
        this.startupPromiseResolve = null;
        this.startupPromiseReject = null;
        console.log('✅ Standalone nREPL stopped and cleaned up');
        resolve();
      };

      // Listen for process exit
      this.process.on('exit', cleanup);

      // Tentar parar graciosamente primeiro
      this.process.kill('SIGTERM');

      // Forçar parada após 3 segundos para operações rápidas
      setTimeout(() => {
        if (this.process && !this.process.killed) {
          console.log('🔨 Force killing nREPL process...');
          this.process.kill('SIGKILL');
          // Give it another second, then cleanup anyway
          setTimeout(cleanup, 1000);
        }
      }, 3000);
    });
  }

  cancelStartup(): void {
    console.log('🚫 Cancelling nREPL startup...');

    if (this.startupTimeout) {
      clearTimeout(this.startupTimeout);
      this.startupTimeout = null;
    }

    if (this.startupPromiseReject) {
      this.startupPromiseReject(new Error('nREPL startup was cancelled'));
    }

    this.stop(); // This will clean up everything
  }

  isRunning(): boolean {
    return this.process !== null && !this.process.killed;
  }

  isInStartupMode(): boolean {
    return this.isStartingUp;
  }

  getCurrentProjectPath(): string | null {
    return this.currentProjectPath;
  }

  getPort(): number {
    return this.port;
  }

  getStatus(): { running: boolean; starting: boolean; project: string | null; port: number } {
    return {
      running: this.isRunning(),
      starting: this.isStartingUp,
      project: this.currentProjectPath,
      port: this.port
    };
  }

  // Método para limpar recursos ao fechar extensão
  cleanup(): void {
    this.stop();
  }
}

// Singleton para gerenciar a instância global
let standaloneNReplInstance: StandaloneNRepl | null = null;

export function getStandaloneNRepl(extensionPath: string): StandaloneNRepl {
  if (!standaloneNReplInstance) {
    standaloneNReplInstance = new StandaloneNRepl(extensionPath);
  }
  return standaloneNReplInstance;
}

export function cleanupStandaloneNRepl(): void {
  if (standaloneNReplInstance) {
    standaloneNReplInstance.cleanup();
    standaloneNReplInstance = null;
  }
} 