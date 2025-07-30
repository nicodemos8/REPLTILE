# REPLTILE - Visual Clojure REPL Interface

**Version 1.0.0 Beta** - A modern visual REPL interface that integrates with the powerful Calva ecosystem for VS Code and Cursor IDE.

---

## 🌟 Overview

**REPLTILE is a visual REPL interface that utilizes the powerful Calva ecosystem**, designed to provide a plug-and-play, Cursive-familiar experience for Clojure development. Rather than being a standalone REPL, REPLTILE serves as a visual interface that uses all of Calva's robust functionality while offering the familiar REPL experience that Cursive users know and love.

**What REPLTILE Really Is:**
- 🎨 **Visual REPL Interface** - A modern UI that works with Calva's powerful engine
- 🔌 **Calva Integration** - Utilizes Calva's complete Clojure tooling ecosystem under the hood
- 🎯 **Cursive-Familiar Experience** - Familiar workflow and shortcuts for former Cursive users
- 📚 **Zero Configuration** - Plug-and-play experience without complex setup

**Key Features:**
- 🖥️ **Visual REPL Interface** - Modern webview-based REPL with rich output formatting
- 🔧 **Visual Dependency Management** - Automatic detection and resolution for multiple build tools
- ⌨️ **Cursive-Style Shortcuts** - Familiar shortcuts and command history navigation
- 🎨 **Theme Integration** - Seamless dark/light mode support with VS Code/Cursor themes
- 📋 **Persistent Command History** - Per-project history with intelligent filtering
- 🔍 **Real-time Search** - Live search with highlighting in REPL output
- 🧪 **Testing Integration** - Visual test execution with Midje support

⚠️ **Beta Release**: This is the first public release. All core features are functional, with additional features planned based on community feedback.

🔥 **Important**: REPLTILE **requires Calva** to function properly. It provides a visual REPL experience using Calva's capabilities.

---

## 📦 Installation Guide

### Step 1: Install Calva (Required)

REPLTILE requires Calva to function. Install Calva first:

**Cursor Marketplace**
1. Open Cursor IDE
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (macOS)
3. Type "Extensions: Install Extensions"
4. Search for "Calva" by `betterthantomorrow.calva`
5. Click "Install"

**Direct Link**: https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva

### Step 2: Install REPLTILE

**⚠️ Important**: REPLTILE is not available on the marketplace. You must install it manually using the VSIX file.

**Download and Install VSIX Package:**
1. Go to the [REPLTILE GitHub repository](https://github.com/nubank/REPLTILE)
2. Navigate to the "Releases" section
3. Download the latest `.vsix` file (e.g., `repltile-clojure-repl-1.0.0-beta.vsix`)
4. In Cursor IDE, press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (macOS)
5. Type "Extensions: Install from VSIX..."
6. Select the downloaded `.vsix` file
7. Click "Install"
8. Restart Cursor IDE when prompted

### Step 3: Verify Installation

1. Open any Clojure project (or create an empty folder)
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (macOS)
3. Type "REPLTILE: Open REPLTILE Panel"
4. If successful, the REPLTILE interface should open in a new panel

### Required Extensions Summary

| Extension | Publisher | Status | Installation Method |
|-----------|-----------|---------|-------------|
| **Calva** | `betterthantomorrow.calva` | 🔴 **Required** | Available on Cursor marketplace |
| **REPLTILE** | `repltile.repltile-clojure-repl` | ✅ **This Plugin** | Manual installation via VSIX file only |

**Installation Order**: Install Calva first (provides the core engine), then install REPLTILE via VSIX file (provides the visual REPL interface).

---

## 🚀 Quick Start

1. **Open** any Clojure project (or empty folder)
2. **Launch** REPLTILE: `Ctrl+Shift+P` → "REPLTILE: Open REPLTILE Panel"
3. **Start** REPL: Click the ▶️ button in the REPLTILE interface
4. **Begin** coding: Type expressions in the input field and press `Enter`
5. **Enjoy** the visual REPL experience with Calva's power!

---

## 🎮 Complete Interface Guide

### Header Controls

| Button | Icon | Function | Visual States |
|--------|------|----------|---------------|
| **Dependencies** | 📦 | Auto-detects and resolves project dependencies | Ready (purple), Resolve (yellow), Resolving (animated) |
| **Refresh All** | 🔄 | Reloads all changed namespaces | Enabled when connected |
| **Start/Stop REPL** | ▶️ | Toggles REPL connection | Play (▶️), Stop (⏹️), Loading (🔄) |

### Toolbar Controls

| Control | Icon | Function | Notes |
|---------|------|----------|-------|
| **Current Namespace** | 📂 | Shows active namespace (read-only) | Updates automatically based on evaluations |
| **Reload Namespace** | 🔄 | Require current namespace with `:reload` | Disabled in "user" namespace |
| **Run Tests** | ✅ | Execute all tests in current REPL namespace | Different from file-based testing shortcuts |

### Output Controls

| Button | Icon | Function | Shortcut |
|--------|------|----------|----------|
| **Clear Output** | 🗑️ | Clear REPL output history | `Cmd/Ctrl+K` |
| **Scroll to Top** | ⬆️ | Navigate to output beginning | - |
| **Scroll to Bottom** | ⬇️ | Navigate to output end | - |
| **Search in Output** | 🔍 | Open search interface with highlighting | `Cmd/Ctrl+F` |

### Search Interface (when active)

| Control | Icon | Function | Shortcut |
|---------|------|----------|----------|
| **Search Input** | 📝 | Enter search terms with live highlighting | - |
| **Case Sensitive** | Aa | Toggle case-sensitive matching | - |
| **Previous Match** | ⬅️ | Navigate to previous search result | `Shift+Enter` |
| **Next Match** | ➡️ | Navigate to next search result | `Enter` |
| **Match Counter** | 🔢 | Shows "1 of 5" style match information | - |
| **Close Search** | ❌ | Close search interface | `Escape` |

---

## ⌨️ Complete Keyboard Shortcuts

### Code Evaluation

| Shortcut | macOS | Windows/Linux | Action | Context |
|----------|-------|---------------|--------|---------|
| `Cmd+Enter` | ✅ | `Ctrl+Enter` | Evaluate current form | Form at cursor position |
| `Cmd+Shift+Enter` | ✅ | `Ctrl+Shift+Enter` | Evaluate top-level form | Root expression containing cursor |
| `Cmd+Alt+Enter` | ✅ | `Ctrl+Alt+Enter` | Evaluate current line | Entire line at cursor |
| `Cmd+Shift+E` | ✅ | `Ctrl+Shift+E` | Evaluate selection | Highlighted text only |

### File and Namespace Operations

| Shortcut | macOS | Windows/Linux | Action | Notes |
|----------|-------|---------------|--------|-------|
| `Cmd+Shift+K` | ✅ | `Ctrl+Shift+K` | Load current file | Loads entire file into REPL |
| `Cmd+Shift+L` | ✅ | `Ctrl+Shift+L` | Reload current namespace | Uses `:reload` flag |
| `Cmd+Shift+N` | ✅ | `Ctrl+Shift+N` | Switch to current namespace | Sets REPL namespace to file's namespace |

### Testing

| Shortcut | macOS | Windows/Linux | Action | Difference from UI Button |
|----------|-------|---------------|--------|--------------------------|
| `Cmd+Shift+T` | ✅ | `Ctrl+Shift+T` | Run tests for current **file's** namespace | UI button runs tests for current **REPL** namespace |

### Interface Control

| Shortcut | macOS | Windows/Linux | Action | Alternative |
|----------|-------|---------------|--------|-------------|
| `Cmd+K` | ✅ | `Ctrl+K` | Clear REPL output | Clear button |
| `Cmd+F` | ✅ | `Ctrl+F` | Open search interface | Search button |
| `Cmd+Shift+M` | ✅ | `Ctrl+Shift+M` | Focus input field | - |
| `Cmd+R` | ✅ | `Ctrl+R` | Repeat last command | - |

### Command History (Cursive Style)

| Shortcut | macOS | Windows/Linux | Action | Notes |
|----------|-------|---------------|--------|-------|
| `Cmd+Up` | ✅ | `Ctrl+Up` | Previous command in history | Navigate backwards in time |
| `Cmd+Down` | ✅ | `Ctrl+Down` | Next command in history | Navigate forwards in time |

### Input Field Controls

| Shortcut | Action | Notes |
|----------|--------|-------|
| `Enter` | Evaluate current expression | Single-line mode behavior |
| `Shift+Enter` | New line | Multi-line editing |
| `Escape` | Clear input and reset history | Returns to empty state |

### Search Interface Controls

| Shortcut | Action | When Available |
|----------|--------|----------------|
| `Enter` | Next match | When search is active |
| `Shift+Enter` | Previous match | When search is active |
| `Escape` | Close search | When search is active |

---

## 📋 Complete Command Palette Reference

Access via `Cmd+Shift+P` (macOS) or `Ctrl+Shift+P` (Windows/Linux):

### Panel Management
- **`REPLTILE: Open REPLTILE Panel`** - Opens the main REPLTILE interface in a webview panel
- **`REPLTILE: Close REPLTILE Panel`** - Closes the REPLTILE interface and cleans up resources

### Debug and Diagnostics
- **`REPLTILE: Show Debug Logs`** - Displays debug information in the "REPLTILE Debug" output channel
- **`REPLTILE: Open Log Files Location`** - Opens the folder containing REPLTILE log files in system file manager
- **`REPLTILE: Copy Debug Information`** - Copies comprehensive debug info to clipboard for bug reports

### Code Evaluation Commands
- **`REPLTILE: Evaluate Selection`** - Evaluates currently highlighted text (same as `Cmd/Ctrl+Shift+E`)
- **`REPLTILE: Evaluate Current Form`** - Evaluates the expression at cursor position (same as `Cmd/Ctrl+Enter`)
- **`REPLTILE: Evaluate Top Level Form`** - Evaluates the root-level expression containing cursor (same as `Cmd/Ctrl+Shift+Enter`)
- **`REPLTILE: Evaluate Current Line`** - Evaluates the entire line where cursor is located (same as `Cmd/Ctrl+Alt+Enter`)

### Namespace Management
- **`REPLTILE: Switch to Current Namespace`** - Changes REPL namespace to match current file's namespace (same as `Cmd/Ctrl+Shift+N`)
- **`REPLTILE: Reload Current Namespace`** - Reloads current namespace with `:reload` flag (same as `Cmd/Ctrl+Shift+L`)
- **`REPLTILE: Load Current File`** - Loads the entire current file into REPL (same as `Cmd/Ctrl+Shift+K`)

### Testing Commands
- **`REPLTILE: Run All Tests for Current File's Namespace`** - Executes all tests in the namespace of the currently open file (same as `Cmd/Ctrl+Shift+T`)

### Utility Commands
- **`REPLTILE: Clear REPL Output`** - Clears all output from the REPL interface (same as `Cmd/Ctrl+K`)

### Interface Commands
- **`REPLTILE: Focus Input Field`** - Moves cursor to REPL input field from anywhere (same as `Cmd/Ctrl+Shift+M`)
- **`REPLTILE: Repeat Last Command`** - Executes the last command from history automatically (same as `Cmd/Ctrl+R`)

> **Important Distinction**: Command palette and keyboard shortcut testing commands run tests for the **current file's namespace**, while the test button in the REPLTILE UI runs tests for the **current REPL namespace**. This design allows you to test the file you're working on regardless of which namespace the REPL is currently operating in.

---

## 🔧 Smart Dependency Management

REPLTILE automatically detects and manages dependencies for multiple build tools without any configuration:

### Supported Build Tools

| Build Tool | Config File | Detection Method | Resolution Command |
|------------|-------------|------------------|-------------------|
| **Clojure CLI** | `deps.edn` | File presence in project root | `clj -P` (download dependencies) |
| **Leiningen** | `project.clj` | File presence in project root | `lein deps` (resolve dependencies) |
| **Shadow CLJS** | `shadow-cljs.edn` | File presence + npm detection | `npm install` (install Node dependencies) |

### Dependency Status Indicators

| Status | Color | Icon | Meaning | Action Required |
|--------|-------|------|---------|-----------------|
| **Ready** | 🟣 Purple | 📦 | All dependencies resolved | None - ready to start REPL |
| **Resolve** | 🟡 Yellow | 📦 | Dependencies need resolution | Click to resolve automatically |
| **Resolving** | 🔄 Animated | 📦 | Currently resolving dependencies | Wait for completion |
| **Error** | 🔴 Red | ⚠️ | Resolution failed | Check logs and retry |

### Automatic Features

- **Auto-Detection**: Scans project root for known configuration files
- **Smart Resolution**: Chooses appropriate tool commands based on project type
- **Background Monitoring**: Watches for changes to dependency files
- **Status Persistence**: Remembers resolution status between sessions
- **Error Reporting**: Provides detailed error messages for troubleshooting

### Manual Control

- **Resolve Button**: Click the 📦 button when it shows "Resolve" status
- **Auto-Resolve Toggle**: Enable/disable automatic resolution
- **Force Refresh**: Restart REPLTILE to re-detect project structure

---

## 🧪 Testing Integration

REPLTILE provides comprehensive testing support with visual feedback:

### Test Execution Methods

| Method | Scope | Namespace | Access |
|--------|-------|-----------|---------|
| **UI Test Button** | Current REPL namespace | What REPL is currently in | Click ✅ button in toolbar |
| **Keyboard Shortcut** | Current file's namespace | File you're editing | `Cmd/Ctrl+Shift+T` |
| **Command Palette** | Current file's namespace | File you're editing | "Run All Tests..." |

### Supported Test Frameworks

- **Clojure.test** - Standard Clojure testing framework
- **Midje** - Popular behavior-driven testing framework
- **Custom Frameworks** - Any framework that outputs to stdout/stderr

### Test Output Features

- **Real-time Results** - See test output as it runs
- **Error Highlighting** - Failed tests clearly marked
- **Stack Traces** - Full error details for debugging
- **Summary Statistics** - Pass/fail counts and timing

---

## 📊 Command History & Session Management

### Persistent History

- **Per-Project Storage**: Each project maintains separate command history
- **Session Persistence**: History survives editor restarts
- **Automatic Cleanup**: Old entries removed to maintain performance
- **Smart Filtering**: Duplicates and internal commands filtered out

### History Navigation

| Action | Shortcut | Behavior |
|--------|----------|----------|
| **Previous Command** | `Cmd/Ctrl+Up` | Move backwards through history |
| **Next Command** | `Cmd/Ctrl+Down` | Move forward through history |
| **Clear Input** | `Escape` | Clear current input and reset position |
| **Repeat Last** | `Cmd/Ctrl+R` | Execute most recent command |

### Session State

REPLTILE automatically saves and restores:
- Current namespace
- Command history
- REPL connection status
- Dependency resolution state
- UI preferences (search settings, etc.)

---

## 🎨 Theming & Customization

### Theme Integration

REPLTILE automatically adapts to your Cursor theme:
- **Dark Mode**: Optimized for dark themes with high contrast
- **Light Mode**: Clean appearance for light themes
- **Automatic Switching**: Follows system/editor theme changes
- **Syntax Highlighting**: Consistent with Calva's highlighting

### Customizable Elements

- **Font Family**: Inherits from editor settings
- **Font Size**: Scales with editor zoom level
- **Color Scheme**: Matches editor theme colors
- **Output Formatting**: ANSI color support for rich output

---

## 🔍 Search & Filtering

### Real-time Search

- **Live Highlighting**: Results highlighted as you type
- **Case Sensitivity**: Toggle with Aa button
- **Match Navigation**: Jump between results with Enter/Shift+Enter
- **Match Counter**: Shows current position (e.g., "3 of 15")
- **Context Preservation**: Maintains search when adding new output

### Search Shortcuts

| Shortcut | Action |
|----------|--------|
| `Cmd/Ctrl+F` | Open search interface |
| `Enter` | Next match |
| `Shift+Enter` | Previous match |
| `Escape` | Close search |

---

## 🔧 Troubleshooting

### Common Issues

**REPL Won't Start**
- Ensure Calva is installed and enabled
- Check that dependencies are resolved (📦 button should be purple)
- Verify project has valid `deps.edn` or `project.clj`

**Commands Not Working**
- Make sure REPL is connected (▶️ button should show ⏹️)
- Check current namespace is correct
- Try `Cmd/Ctrl+Shift+M` to focus input field

**Dependency Resolution Fails**
- Check internet connection
- Verify build tool is installed (lein, clj, npm)
- Look at "REPLTILE Debug" output channel for details

**Performance Issues**
- Clear output history with `Cmd/Ctrl+K`
- Restart REPLTILE panel
- Check for large objects in REPL output

### Debug Information

Access debug tools via Command Palette:
- **Show Debug Logs**: View internal logging
- **Open Log Files Location**: Access log files directly
- **Copy Debug Information**: Get system info for bug reports

### Getting Help

1. **Check Debug Logs**: Command Palette → "REPLTILE: Show Debug Logs"
2. **Read Documentation**: This README and architecture docs
3. **Report Issues**: Use GitHub issues with debug information
4. **Community**: Join Clojure community channels for assistance

---

## 📋 Changelog

### Version 1.0.0 Beta
- Initial public release
- Core REPL functionality with Calva integration
- Visual dependency management
- Persistent command history
- Real-time search and filtering
- Testing integration with visual feedback
- Complete keyboard shortcut support
- Theme integration and customization

---

## 🤝 Contributing

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed information about:
- Project structure and organization
- Development setup and build process
- Architecture decisions and patterns
- Contributing guidelines and standards

---

## 📄 License

MIT License - see LICENSE file for details.

---

## 🙏 Acknowledgments

- **Calva Team**: For providing the robust Clojure tooling foundation
- **repl-tooling**: For the excellent REPL integration library
- **Clojure Community**: For feedback and support during development
- **Cursive Users**: For inspiring the familiar workflow design 