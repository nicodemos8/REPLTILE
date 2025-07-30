# REPLTILE Developer Guide

**Complete Development Environment Setup and Workflow**

This guide provides everything you need to know about developing, debugging, and building REPLTILE.

---

## 🚀 Quick Start

### Prerequisites
- **Node.js** (v16 or higher)
- **Java** (v11 or higher) - for ClojureScript compilation
- **Cursor IDE** or **VS Code** - for testing
- **Clojure CLI** - for REPL testing

### Initial Setup
```bash
# Clone the repository
git clone https://github.com/nubank/REPLTILE.git
cd REPLTILE

# Install dependencies
npm install

# Compile the project
npm run compile
```

---

## 🛠️ Development Workflow

### 1. Compilation

**Main compilation command:**
```bash
npm run compile
```

This command:
- Compiles ClojureScript using Shadow-CLJS
- Transpiles TypeScript to JavaScript
- Generates the `out/` directory with compiled assets
- Creates the `lib/` directory with processed libraries

**Watch mode for development:**
```bash
npm run watch
```
- Automatically recompiles on file changes ♻️
- Ideal for active development
- Provides real-time feedback

### 2. Debugging

**Method 1: VS Code/Cursor Extension Development Host**
```bash
# Start compilation in watch mode
npm run watch

# In VS Code/Cursor:
# 1. Press F5 or Ctrl+Shift+P
# 2. Select "Debug: Start Debugging"
# 3. Choose "Launch Extension"
```

**Method 2: Manual Extension Host Launch**
```bash
# Compile first
npm run compile

# Launch extension host with logs
code --extensionDevelopmentPath=. --verbose
```

**Debug Console Commands:**
```javascript
// Check REPLTILE status
console.log(window.repltile);

// Inspect webview state
console.log(window.repltileWebview);

// Check Calva integration
console.log(vscode.extensions.getExtension('betterthantomorrow.calva'));
```

### 3. Package Generation

**Generate VSIX package:**
```bash
npm run package
```

This creates: `repltile-clojure-repl-[version].vsix`

**Custom version package:**
```bash
# Update version first
npm version 1.0.1-dev

# Then package
npm run package
```

**Pre-release package:**
```bash
npx vsce package --pre-release
```

---

## 📁 Project Structure

```
REPLTILE/
├── src/
│   ├── cljs/repltile/          # ClojureScript source
│   │   ├── core/               # Core functionality
│   │   ├── repl/               # REPL engine
│   │   ├── ui/                 # User interface
│   │   └── vscode/             # VS Code integration
│   └── ts/                     # TypeScript source
│       ├── extension.ts        # Main extension entry
│       ├── webview/            # Webview management
│       └── clojure/            # Clojure integration
├── lib/                        # Clojure libraries (generated)
├── out/                        # Compiled output (generated)
├── deps.edn                    # Clojure dependencies
├── shadow-cljs.edn            # ClojureScript build config
├── package.json               # Extension manifest
└── tsconfig.json             # TypeScript config
```

---

## 🔧 Available Scripts

### Core Development
```bash
npm run compile          # Full compilation
npm run watch           # Watch mode compilation
npm run build           # Production build
npm run clean           # Clean generated files
```

### Testing & Quality
```bash
npm run test            # Run tests
npm run lint            # Lint TypeScript/ClojureScript
npm run format          # Format code
```

### Packaging & Distribution
```bash
npm run package         # Generate VSIX
npm run publish         # Publish to marketplace (requires auth)
npm run bundle-deps     # Bundle Clojure dependencies
```

### Development Utilities
```bash
npm run dev:repl        # Start Clojure REPL
npm run dev:shadow      # Start Shadow-CLJS REPL
npm run dev:server      # Start development server
```

---

## 🐛 Debugging Tips

### Common Issues

**1. ClojureScript Compilation Fails**
```bash
# Clean and reinstall
rm -rf node_modules .shadow-cljs out lib
npm install
npm run compile
```

**2. Extension Not Loading**
```bash
# Check extension host logs
code --log trace --extensionDevelopmentPath=.

# Or check Developer Tools
# Help > Toggle Developer Tools > Console
```

**3. Webview Not Displaying**
```javascript
// In browser console:
window.addEventListener('message', console.log);
```

### Debug Configurations

**VS Code launch.json:**
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Launch REPLTILE Extension",
            "type": "extensionHost",
            "request": "launch",
            "runtimeExecutable": "${execPath}",
            "args": [
                "--extensionDevelopmentPath=${workspaceFolder}",
                "--disable-extensions"
            ],
            "outFiles": ["${workspaceFolder}/out/**/*.js"]
        }
    ]
}
```

---

## 🧪 Testing

### Manual Testing Checklist

**Basic Functionality:**
- [ ] Extension loads without errors
- [ ] REPLTILE panel opens (`Ctrl+Shift+P` > "Open REPLTILE Panel")
- [ ] Can connect to nREPL (port 7888 default)
- [ ] Code evaluation works
- [ ] History navigation works
- [ ] Dependency detection works

**Advanced Features:**
- [ ] Test runner integration
- [ ] Multiple REPL types (Clojure, ClojureScript, Babashka)
- [ ] Webview rendering
- [ ] Command palette integration
- [ ] Keyboard shortcuts

### Automated Testing
```bash
# Run all tests
npm test

# Run specific test suites
npm run test:unit
npm run test:integration
npm run test:e2e
```

---

## 🎯 Development Best Practices

### Code Style
- **ClojureScript**: Follow standard Clojure conventions
- **TypeScript**: Use ESLint configuration
- **File naming**: kebab-case for Clojure, camelCase for TypeScript

### Git Workflow
```bash
# Feature development
git checkout -b feature/new-feature
git commit -m "feat: add new feature"
git push origin feature/new-feature

# Bug fixes
git checkout -b fix/bug-description
git commit -m "fix: resolve bug description"
```

### Performance Tips
- Use `npm run watch` during active development
- Clear Shadow-CLJS cache if seeing stale compilation: `rm -rf .shadow-cljs`
- Use Chrome DevTools for webview performance analysis

---

## 🔌 Extension Integration

### Calva Integration
```clojure
;; Test Calva connection
(require '[clojure.repl :as repl])
(repl/doc +)
```

### nREPL Testing
```bash
# Start nREPL server
lein repl :headless :host localhost :port 7888

# Or with CLI tools
clj -M:nrepl
```

### Shadow-CLJS Testing
```bash
# Start Shadow-CLJS
npx shadow-cljs watch app

# Connect to browser REPL
# Visit: http://localhost:8080
```

---

## 📦 Build Targets

### Development Build
```bash
npm run compile
# Creates: out/extension.js, lib/ directory
```

### Production Build
```bash
npm run build
# Optimized, minified output
```

### VSIX Package
```bash
npm run package
# Creates: repltile-clojure-repl-[version].vsix
```

---

## 🚨 Troubleshooting

### Extension Host Issues
```bash
# Reset extension host
code --disable-extensions --extensionDevelopmentPath=.

# Clear VS Code workspace state
rm -rf ~/.vscode/extensions/ms-vscode.vscode-*
```

### Shadow-CLJS Issues
```bash
# Clear cache
rm -rf .shadow-cljs

# Restart with clean state
npx shadow-cljs stop
npx shadow-cljs watch app
```

### Node.js Issues
```bash
# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install
```

---

## 🌐 Environment Variables

```bash
# Development mode
export NODE_ENV=development

# Debug logging
export DEBUG=repltile:*

# Shadow-CLJS port
export SHADOW_CLJS_PORT=9630
```

---

## 📚 Resources

### Documentation
- **Shadow-CLJS**: https://shadow-cljs.github.io/docs/
- **VS Code Extension API**: https://code.visualstudio.com/api
- **Calva Documentation**: https://calva.io/

### Community
- **Clojurians Slack**: #shadow-cljs, #calva, #beginners
- **GitHub Discussions**: Project repository discussions
- **Stack Overflow**: Tag with `clojurescript`, `vscode-extensions`

---

## 🤝 Contributing

### Before Contributing
1. ✅ Read the codebase and understand the architecture
2. ✅ Set up development environment
3. ✅ Run tests to ensure everything works
4. ✅ Check existing issues and discussions

### Contribution Workflow
1. **Fork** the repository
2. **Create** feature branch
3. **Develop** with tests
4. **Test** thoroughly
5. **Submit** pull request

### Pull Request Template
```markdown
## Description
Brief description of changes

## Testing
- [ ] Manual testing completed
- [ ] Automated tests pass
- [ ] No regressions identified

## Checklist
- [ ] Code follows project style
- [ ] Documentation updated
- [ ] Tests added/updated
```

---

## 📝 Changelog Management

When making changes, update the appropriate sections:

- **Features**: New functionality
- **Bug Fixes**: Error corrections
- **Breaking Changes**: API modifications
- **Documentation**: Guide updates

---

*Happy coding! 🎉 For questions or support, reach out via GitHub issues or community channels.* 