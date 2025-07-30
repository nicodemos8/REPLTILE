# REPLTILE Architecture Guide

**For Contributors and Maintainers**

This document provides a comprehensive overview of REPLTILE's architecture, design decisions, and development practices for contributors who want to understand, maintain, or extend the codebase.

---

## 🏗️ High-Level Architecture

REPLTILE is built as a VS Code/Cursor extension that integrates with Calva to provide a visual REPL interface. The architecture is split into several layers:

```
┌─────────────────────────────────────────────────────────────┐
│                     VS Code/Cursor Extension                │
├─────────────────────────────────────────────────────────────┤
│                   TypeScript Layer                          │
│  • Extension lifecycle management                           │
│  • Calva integration and bridge                             │
│  • Webview management                                       │
│  • File system operations                                   │
│  • Command registration                                      │
├─────────────────────────────────────────────────────────────┤
│               ClojureScript Extension Core                   │
│  • Command dispatch and handling                            │
│  • State management                                         │
│  • REPL engine integration                                  │
│  • Dependency management                                    │
├─────────────────────────────────────────────────────────────┤
│                ClojureScript Webview UI                     │
│  • User interface components                                │
│  • Event handling                                          │
│  • Output rendering                                         │
│  • Search and history management                           │
├─────────────────────────────────────────────────────────────┤
│                     repl-tooling                           │
│  • nREPL connection management                              │
│  • Code evaluation                                         │
│  • Completion and introspection                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
REPLTILE/
├── deps.edn                    # Clojure dependencies and build config
├── shadow-cljs.edn             # ClojureScript build configuration
├── package.json                # NPM dependencies and VS Code extension manifest
├── tsconfig.json               # TypeScript configuration
├── out/                        # Compiled output (generated)
│   ├── main.js                 # ClojureScript extension core
│   ├── webview/                # Compiled webview bundle
│   └── ts/                     # Compiled TypeScript
├── src/
│   ├── cljs/repltile/          # ClojureScript source code
│   │   ├── core/               # Core functionality
│   │   │   ├── core.cljs       # Main entry point
│   │   │   ├── state.cljs      # Application state management
│   │   │   └── logger.cljs     # Logging system
│   │   ├── repl/               # REPL integration
│   │   │   ├── engine.cljs     # repl-tooling integration
│   │   │   └── deps.cljs       # Dependency management
│   │   ├── vscode/             # VS Code integration layer
│   │   │   ├── extension.cljs  # Extension bridge
│   │   │   ├── commands.cljs   # Command handlers
│   │   │   └── calva.cljs      # Calva integration
│   │   └── ui/                 # Webview UI components
│   │       ├── core.cljs       # UI initialization and main app
│   │       ├── components/     # Reusable UI components
│   │       ├── events/         # Event handling system
│   │       ├── layout/         # Styling and themes
│   │       └── state/          # UI-specific state management
│   └── ts/                     # TypeScript source code
│       ├── extension.ts        # Main extension entry point
│       ├── calva-bridge.ts     # Calva API integration
│       ├── webview/            # Webview management
│       ├── clojure/            # ClojureScript process management
│       ├── dependencies/       # Dependency system
│       ├── history/            # Command history persistence
│       ├── repl/               # REPL shortcuts implementation
│       └── core/               # Shared utilities
└── lib/                        # External libraries and resources
```

---

## 🔧 Technology Stack

### Core Technologies

| Technology | Version | Purpose | Key Features |
|------------|---------|---------|--------------|
| **TypeScript** | 4.9+ | Extension host layer | Type safety, VS Code API integration |
| **ClojureScript** | 1.11.60 | Core logic and UI | Functional programming, immutable state |
| **Shadow CLJS** | 2.20.0 | Build system | Fast compilation, hot reload |
| **repl-tooling** | 0.5.12 | REPL integration | nREPL communication, code evaluation |
| **VS Code API** | 1.80+ | Extension platform | Webviews, commands, file system |

### Key Dependencies

**ClojureScript Dependencies (deps.edn)**
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        org.clojure/tools.namespace {:mvn/version "1.4.4"}
        org.clojure/clojurescript {:mvn/version "1.11.60"}
        thheller/shadow-cljs {:mvn/version "2.20.0"}
        repl-tooling/repl-tooling {:mvn/version "0.5.12"}
        org.clojure/core.async {:mvn/version "1.6.673"}}}
```

**TypeScript Dependencies (package.json)**
```json
{
  "dependencies": {
    "vscode": "^1.1.37",
    "@types/node": "^18.0.0"
  }
}
```

---

## 🏛️ Architecture Layers

### 1. TypeScript Extension Layer

**Purpose**: Handle VS Code extension lifecycle and provide platform integration.

**Key Modules**:

- **`extension.ts`**: Main extension entry point
  - Registers all VS Code commands
  - Manages extension lifecycle
  - Configures Calva integration
  - Starts ClojureScript processes

- **`calva-bridge.ts`**: Calva integration
  - Detects project types (deps.edn, leiningen)
  - Configures Calva connect sequences
  - Handles Calva jack-in/connect operations
  - Manages REPL aliases and profiles

- **`webview-manager.ts`**: Webview lifecycle
  - Creates and manages webview panels
  - Handles webview messaging
  - Manages webview HTML content
  - Coordinates with dependency system

- **`process-manager.ts`**: ClojureScript process management
  - Spawns and manages ClojureScript runtime
  - Handles inter-process communication
  - Manages process lifecycle and cleanup

### 2. ClojureScript Extension Core

**Purpose**: Core business logic and state management.

**Key Modules**:

- **`core/core.cljs`**: Main application entry point
  - Initializes all subsystems
  - Sets up message handling
  - Coordinates between layers

- **`core/state.cljs`**: Centralized state management
  - Immutable application state atom
  - State change tracking and debugging
  - State persistence coordination

- **`repl/engine.cljs`**: REPL integration engine
  - repl-tooling integration
  - Code evaluation handling
  - Namespace management
  - Completion system integration

- **`vscode/commands.cljs`**: Command dispatch system
  - Maps VS Code commands to handlers
  - Manages command queue and evaluation state
  - Handles command-specific business logic

### 3. ClojureScript Webview UI

**Purpose**: User interface implementation and interaction handling.

**Key Modules**:

- **`ui/core.cljs`**: UI initialization and main app
  - DOM manipulation and setup
  - Main application loop
  - Message handling from extension

- **`ui/components/`**: Reusable UI components
  - `input.cljs`: Command input handling
  - `output.cljs`: REPL output rendering
  - `namespace.cljs`: Namespace management UI
  - `search.cljs`: Search interface

- **`ui/events/events.cljs`**: Event handling system
  - User interaction events
  - Keyboard shortcuts
  - UI state updates

### 4. repl-tooling Integration

**Purpose**: Provides robust nREPL integration and code evaluation.

**Integration Points**:
- Connection management
- Code evaluation with proper error handling
- Completion and introspection
- Namespace operations
- Output streaming

---

## 🔄 Data Flow and Communication

### Inter-Layer Communication

```
┌─────────────────┐    messages    ┌──────────────────────┐
│   TypeScript    │ ◄──────────────► │  ClojureScript Core  │
│   Extension     │                 │                      │
└─────────────────┘                 └──────────────────────┘
         │                                       │
         │ webview                               │ UI events
         │ messages                              │
         ▼                                       ▼
┌─────────────────┐                 ┌──────────────────────┐
│     Webview     │ ◄──────────────► │  ClojureScript UI    │
│     HTML        │    DOM events    │                      │
└─────────────────┘                 └──────────────────────┘
                                              │
                                              │ repl-tooling
                                              │ API calls
                                              ▼
                                    ┌──────────────────────┐
                                    │    repl-tooling      │
                                    │                      │
                                    └──────────────────────┘
```

### Message Types

**TypeScript → ClojureScript**
```typescript
interface Message {
  type: 'evaluate-code' | 'start-repl' | 'stop-repl' | 'initialize-deps' | ...;
  data?: any;
}
```

**ClojureScript → TypeScript**
```clojure
{:type :repl-started
 :port 12345
 :data {...}}
```

**Webview ↔ ClojureScript**
```javascript
// Webview to ClojureScript
{ type: 'evaluate-code', code: '(+ 1 2 3)' }

// ClojureScript to Webview
{ type: 'evaluation-result', result: {...} }
```

---

## 🏃‍♂️ Build System and Development

### Shadow CLJS Configuration

**Build Targets** (shadow-cljs.edn):

```clojure
{:builds
 {:extension {:target :node-script              ; Extension core
              :output-to "out/main.js"
              :main repltile.core.core/main}
              
  :webview {:target :browser                    ; Webview UI
            :output-dir "out/webview"
            :modules {:main {:init-fn repltile.ui.core/init}}}}}
```

### Development Workflow

**1. Setup Development Environment**
```bash
# Install dependencies
npm install
clj -M:deps

# Start Shadow CLJS watch
npx shadow-cljs watch extension webview

# In another terminal, start VS Code extension host
npm run dev
```

**2. Development Commands**
```bash
# Build for production
npm run build

# Run tests
npm run test

# Package extension
npm run package

# Lint code
npm run lint
```

**3. Hot Reload Development**
- ClojureScript changes reload automatically
- TypeScript changes require restart
- Webview UI updates immediately
- Extension changes require host restart

### Build Process

**Development Build**:
1. Shadow CLJS compiles ClojureScript to JavaScript
2. TypeScript compiler processes TS files
3. VS Code extension host loads compiled files
4. Hot reload maintains state during development

**Production Build**:
1. Clean previous builds
2. Compile ClojureScript with optimizations
3. Bundle and minify assets
4. Package as .vsix file
5. Validate extension manifest

---

## 🧪 Testing Strategy

### Test Categories

**1. Unit Tests**
- Pure functions in utility modules
- State management operations
- Data transformation functions
- Individual component logic

**2. Integration Tests**
- repl-tooling integration
- VS Code API integration
- Webview communication
- File system operations

**3. End-to-End Tests**
- Complete user workflows
- REPL connection and evaluation
- Dependency resolution
- UI interaction scenarios

### Testing Tools

| Tool | Purpose | Location |
|------|---------|----------|
| **Midje** | ClojureScript unit tests | `test/cljs/` |
| **Jest** | TypeScript unit tests | `test/ts/` |
| **Playwright** | E2E testing | `test/e2e/` |

### Test Execution

```bash
# Run ClojureScript tests
clj -M:test

# Run TypeScript tests
npm run test:ts

# Run E2E tests
npm run test:e2e

# Run all tests
npm run test:all
```

---

## 🔧 State Management

### Application State Structure

```clojure
{:repl-connection nil              ; repl-tooling connection
 :calva-status :disconnected       ; Calva integration status
 :repl-tooling-status :disconnected ; REPL connection status
 :current-namespace "user"         ; Active namespace
 :evaluation-history []            ; Past evaluations
 :webview-ready false              ; UI initialization state
 :completions []                   ; Completion results
 :namespace-vars {}                ; Namespace introspection
 :command-history []               ; Command history
 :command-history-index -1         ; History navigation
 :evaluating false                 ; Evaluation in progress
 :command-queue []                 ; Pending commands
 :deps-status :unknown             ; Dependency resolution state
 :deps-resolved-status {}          ; Per-config resolution state
 :current-project-path nil}        ; Project path for isolation
```

### State Management Patterns

**1. Centralized State Atom**
- Single source of truth for application state
- Immutable updates using `swap!` and `update`
- State watchers for debugging and change tracking

**2. Functional State Updates**
```clojure
(defn set-evaluating! [evaluating?]
  (swap! app-state assoc :evaluating evaluating?))

(defn add-evaluation! [evaluation]
  (swap! app-state update :evaluation-history conj evaluation))
```

**3. State Synchronization**
- Periodic state sync between layers
- Optimistic updates with rollback capability
- Persistence for critical state (history, preferences)

---

## 🔌 Extension Points and Customization

### Plugin Architecture

**Command System**
- All functionality exposed as commands
- Command handlers registered in `vscode/commands.cljs`
- Easy to add new commands and shortcuts

**Event System**
- Event-driven architecture for UI updates
- Pluggable event handlers
- Custom event types for extensions

**Theming System**
- CSS custom properties for theming
- Automatic theme detection
- Override points for custom styling

### Adding New Features

**1. New Command Example**
```clojure
;; In vscode/commands.cljs
(defn handle-new-feature [message]
  (log/info "Handling new feature...")
  ;; Implementation here
  )

;; Register handler
(def command-handlers
  (assoc command-handlers :new-feature handle-new-feature))
```

**2. New UI Component Example**
```clojure
;; In ui/components/new-component.cljs
(ns repltile.ui.components.new-component
  (:require [repltile.core.state :as state]))

(defn render-new-component! []
  ;; Component implementation
  )
```

**3. New TypeScript Integration**
```typescript
// In extension.ts
export function newFeature() {
  // TypeScript implementation
  sendToClojureScript({ type: 'new-feature', data: {} });
}

// Register command
const commands = [
  vscode.commands.registerCommand('repltile.newFeature', newFeature)
];
```

---

## 🛡️ Error Handling and Debugging

### Error Handling Strategy

**1. Graceful Degradation**
- Non-critical errors don't crash the extension
- Fallback behavior for missing features
- User-friendly error messages

**2. Error Boundaries**
- Try-catch blocks around critical operations
- Error isolation between components
- Recovery mechanisms where possible

**3. Comprehensive Logging**
```clojure
;; Different log levels
(log/debug "Detailed debug information")
(log/info "General information")
(log/warn "Warning conditions")
(log/error "Error conditions")
```

### Debugging Tools

**1. Debug Logging System**
- Configurable log levels
- File-based persistent logging
- VS Code output channel integration

**2. State Inspection**
- State watchers for change tracking
- Debug commands for state inspection
- Development-time state visualization

**3. Performance Monitoring**
- Timing critical operations
- Memory usage tracking
- Performance bottleneck identification

---

## 🚀 Performance Considerations

### Optimization Strategies

**1. Lazy Loading**
- Components loaded on demand
- Deferred initialization of heavy features
- Progressive enhancement

**2. Efficient State Updates**
- Minimal state mutations
- Batched updates where possible
- Selective re-rendering

**3. Memory Management**
- Regular cleanup of old evaluations
- Bounded history and cache sizes
- Proper resource disposal

### Performance Monitoring

**Key Metrics**:
- Extension activation time
- REPL connection time
- Evaluation response time
- Memory usage over time
- UI responsiveness

**Profiling Tools**:
- Shadow CLJS built-in profiling
- VS Code extension profiler
- Custom timing instrumentation

---

## 📦 Dependency Management

### External Dependencies

**Runtime Dependencies**:
- repl-tooling for REPL integration
- VS Code API for platform integration
- Shadow CLJS for build system

**Development Dependencies**:
- Testing frameworks (Midje, Jest)
- Linting tools (clj-kondo, ESLint)
- Build tools and utilities

### Dependency Strategy

**1. Minimal Dependencies**
- Prefer built-in functionality over external libraries
- Evaluate cost/benefit of each dependency
- Regular dependency audits and updates

**2. Version Pinning**
- Pin critical dependencies to specific versions
- Test thoroughly before version updates
- Document breaking changes

**3. Bundle Size Optimization**
- Tree shaking for unused code
- Minimize bundle size for better performance
- Split bundles where appropriate

---

## 🔄 Release Process

### Version Management

**Semantic Versioning**:
- Major: Breaking changes
- Minor: New features, backward compatible
- Patch: Bug fixes, backward compatible

**Release Workflow**:
1. Feature development on feature branches
2. Integration testing on develop branch
3. Release candidate testing
4. Production release
5. Post-release monitoring

### Build and Package

**Automated Build Pipeline**:
```bash
# 1. Clean previous builds
npm run clean

# 2. Install dependencies
npm install
clj -M:deps

# 3. Run tests
npm run test

# 4. Build for production
npm run build

# 5. Package extension
npm run package

# 6. Validate package
npm run validate
```

### Distribution

**VS Code Marketplace**:
- Automated publishing via CI/CD
- Staged rollout to minimize risk
- Rollback capability for critical issues

**GitHub Releases**:
- Source code archives
- Pre-built VSIX packages
- Release notes and changelog

---

## 🤝 Contributing Guidelines

### Code Style and Standards

**ClojureScript Standards**:
- Follow standard Clojure style guide
- Use meaningful function and variable names
- Document public APIs with docstrings
- Prefer pure functions over stateful operations

**TypeScript Standards**:
- Use TypeScript strict mode
- Explicit type annotations for public APIs
- Consistent naming conventions
- Proper error handling

### Development Workflow

**1. Setup Development Environment**
```bash
git clone https://github.com/nubank/REPLTILE.git
cd REPLTILE
npm install
clj -M:deps
```

**2. Create Feature Branch**
```bash
git checkout -b feature/new-feature-name
```

**3. Development Process**
- Write tests first (TDD approach)
- Implement feature with proper error handling
- Update documentation as needed
- Test thoroughly across different scenarios

**4. Submit Pull Request**
- Clear description of changes
- Reference any related issues
- Include screenshots for UI changes
- Ensure all tests pass

### Code Review Process

**Review Criteria**:
- Code quality and style adherence
- Test coverage and quality
- Documentation completeness
- Performance implications
- Security considerations

**Review Workflow**:
1. Automated checks (tests, linting)
2. Peer code review
3. Integration testing
4. Final approval and merge

---

## 🔮 Future Architecture Considerations

### Scalability Plans

**1. Plugin System**
- Extensible architecture for third-party plugins
- Well-defined plugin APIs
- Isolated plugin execution environments

**2. Multi-REPL Support**
- Support for multiple concurrent REPL connections
- REPL session management
- Connection switching and state isolation

**3. Remote REPL Integration**
- Secure remote REPL connections
- SSH tunnel support
- Cloud-based REPL environments

### Technology Evolution

**Potential Upgrades**:
- ClojureScript compiler improvements
- VS Code API enhancements
- Modern web standards adoption
- Performance optimization techniques

**Migration Strategies**:
- Backward compatibility maintenance
- Gradual migration approaches
- Feature flags for experimental features
- User communication and documentation

---

## 📚 Additional Resources

### Documentation

- **README.md**: User-facing documentation
- **API.md**: API reference documentation
- **CONTRIBUTING.md**: Detailed contributing guidelines
- **CHANGELOG.md**: Version history and changes

### External Resources

- [VS Code Extension API](https://code.visualstudio.com/api)
- [Shadow CLJS User Guide](https://shadow-cljs.github.io/docs/UsersGuide.html)
- [repl-tooling Documentation](https://github.com/mauricioszabo/repl-tooling)
- [Calva User Guide](https://calva.io/)

### Community

- **GitHub Issues**: Bug reports and feature requests
- **Discussions**: Architecture discussions and Q&A
- **Clojure Community**: General Clojure development help
- **VS Code Community**: Extension development resources

---

## 🎯 Architecture Decision Records (ADRs)

### ADR-001: ClojureScript for Core Logic

**Decision**: Use ClojureScript for core business logic instead of TypeScript.

**Rationale**:
- Functional programming paradigm fits REPL domain well
- Immutable data structures for reliable state management
- Hot reload capabilities speed up development
- Leverage existing Clojure ecosystem tools

**Consequences**:
- Additional build complexity with Shadow CLJS
- Learning curve for TypeScript developers
- Excellent development experience once setup
- Superior state management capabilities

### ADR-002: repl-tooling for REPL Integration

**Decision**: Use repl-tooling library for nREPL communication.

**Rationale**:
- Mature and battle-tested REPL integration
- Handles complex edge cases in nREPL protocol
- Active maintenance and community support
- Proven integration with various editors

**Consequences**:
- External dependency for critical functionality
- Some customization limitations
- Excellent stability and feature completeness
- Faster development time

### ADR-003: Webview for UI Implementation

**Decision**: Use VS Code webview for main UI instead of native extension UI.

**Rationale**:
- Rich UI capabilities with HTML/CSS/JavaScript
- Better user experience for REPL interface
- Easier theming and customization
- More familiar web development patterns

**Consequences**:
- Additional complexity in webview communication
- Some performance overhead
- Superior user interface capabilities
- Easier UI development and maintenance

---

**© 2025 REPLTILE Team - Built with ❤️ for the Clojure community** 