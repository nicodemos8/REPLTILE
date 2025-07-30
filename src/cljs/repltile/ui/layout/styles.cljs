(ns repltile.ui.layout.styles
  "UI HTML and CSS generation for REPLTILE webview")

(defn create-repl-interface
  []
  (let [icon-uri (or (.-REPLTILE_ICON_URI js/window) "icon.png")]
    (str "<div class='repl-root'>
    <div class='repl-header'>
      <div class='repl-header-logo'>
        <img src='" icon-uri "' alt='REPLTILE' class='logo-icon' />
        <span class='logo-text'>REPLTILE</span>
      </div>
      <div class='repl-header-btns'>
        <button id='resolve-deps-btn' class='repl-header-btn deps-btn' title='Resolve Dependencies'>
          <svg id='icon-deps' width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'>
            <circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/>
            <path d='M10 8L14 12L18 8' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/>
            <path d='M10 16L14 20L18 16' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/>
            <circle cx='14' cy='14' r='2' fill='#22c55e'/>
          </svg>
          <span id='deps-status-text' class='deps-status-text'>Deps</span>
        </button>
        <button id='refresh-all-btn' class='repl-header-btn' title='Refresh All (reload all changed namespaces)'>
          <svg width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/><path d='M12 8L16 12L12 16' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/><path d='M8 12H16' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><circle cx='14' cy='14' r='10' stroke='#22c55e' stroke-width='1.5' stroke-dasharray='5 3'/></svg>
        </button>
        <button id='repl-toggle-btn' class='repl-header-btn' title='Start REPL'>
          <svg id='icon-play' width='28' height='28' viewBox='0 0 28 28' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='14' cy='14' r='14' fill='#22c55e' opacity='0.12'/><polygon points='11,8 22,14 11,20' fill='#22c55e'/></svg>
        </button>
      </div>
    </div>
    <div class='repl-toolbar'>
      <div class='repl-toolbar-group'>
        <input id='ns-combo' class='repl-combo' type='text' readonly disabled placeholder='Current Namespace' title='Current Namespace' />
        <button id='reload-ns-btn' class='repl-toolbar-btn' title='Require namespace with :reload'>
          <svg width='20' height='20' viewBox='0 0 20 20' fill='none' xmlns='http://www.w3.org/2000/svg'><path d='M16 10C16 13.314 13.314 16 10 16C6.686 16 4 13.314 4 10C4 6.686 6.686 4 10 4C13.314 4 16 6.686 16 10Z' stroke='#22c55e' stroke-width='1.5'/><path d='M8 7L11 10L8 13' stroke='#22c55e' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/></svg>
        </button>
        <button id='run-ns-tests-btn' class='repl-toolbar-btn' title='Run all tests for current namespace'>
          <svg width='20' height='20' viewBox='0 0 20 20' fill='none' xmlns='http://www.w3.org/2000/svg'><path d='M10 1L12.5 6.5L19 7.5L14.5 12L16 18.5L10 15.5L4 18.5L5.5 12L1 7.5L7.5 6.5L10 1Z' stroke='#22c55e' stroke-width='1.5' stroke-linejoin='round'/><path d='M7 9L9 11L13 7' stroke='#22c55e' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/></svg>
        </button>
      </div>
    </div>
    <div class='repl-main'>
      <div class='repl-input-section' id='repl-input-section'>
        <div class='repl-input-row'>
          <div class='repl-input-container'>
          <textarea id='repl-input' class='repl-input' placeholder='Enter Clojure code here...' rows='3'></textarea>
          </div>
        </div>
      </div>
      <div class='repl-controls-section' id='repl-controls-section'>
        <div class='repl-splitter-row'>
          <div class='repl-splitter' id='repl-splitter' title='Drag to resize input/output areas'>
            <div class='repl-splitter-handle'></div>
          </div>
        </div>
        <div class='repl-buttons-row'>
          <div class='repl-output-toolbar'>
            <button id='clear-output-btn' class='repl-output-btn' title='Clear Output'>
              <svg width='22' height='22' viewBox='0 0 22 22' fill='none' xmlns='http://www.w3.org/2000/svg'><rect x='5' y='8' width='12' height='9' rx='2' stroke='#22c55e' stroke-width='2'/><path d='M9 11V15' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><path d='M13 11V15' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><path d='M3 8H19' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><path d='M8 8V6C8 4.89543 8.89543 4 10 4H12C13.1046 4 14 4.89543 14 6V8' stroke='#22c55e' stroke-width='2'/></svg>
            </button>
            <button id='scroll-top-btn' class='repl-output-btn' title='Scroll to Top'>
              <svg width='22' height='22' viewBox='0 0 22 22' fill='none' xmlns='http://www.w3.org/2000/svg'><path d='M11 17V5' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><path d='M6 10L11 5L16 10' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/></svg>
            </button>
            <button id='scroll-bottom-btn' class='repl-output-btn' title='Scroll to Bottom'>
              <svg width='22' height='22' viewBox='0 0 22 22' fill='none' xmlns='http://www.w3.org/2000/svg'><path d='M11 5V17' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/><path d='M16 12L11 17L6 12' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/></svg>
            </button>
            <button id='search-output-btn' class='repl-output-btn' title='Search in Output (Cmd/Ctrl+F)'>
              <svg width='22' height='22' viewBox='0 0 22 22' fill='none' xmlns='http://www.w3.org/2000/svg'><circle cx='10' cy='10' r='7' stroke='#22c55e' stroke-width='2'/><path d='m21 21-4.35-4.35' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/></svg>
            </button>
          </div>
        </div>
      </div>
      <div class='repl-output-section' id='repl-output-section'>
        <div class='repl-search-container' id='repl-search-container'>
          <div class='repl-search-input-group'>
            <input id='repl-search-input' class='repl-search-input' type='text' placeholder='Search in output...' />
            <div class='repl-search-options'>
              <label class='repl-search-checkbox-label'>
                <input id='search-case-sensitive' class='repl-search-checkbox' type='checkbox' />
                <span class='checkbox-text'>Aa</span>
              </label>
            </div>
            <div class='repl-search-controls'>
              <button id='search-prev-btn' class='repl-search-btn' title='Previous match (Shift+Enter)'>
                <svg width='16' height='16' viewBox='0 0 16 16' fill='none' xmlns='http://www.w3.org/2000/svg'>
                  <path d='M8 3L4 7L8 11' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/>
                </svg>
              </button>
              <button id='search-next-btn' class='repl-search-btn' title='Next match (Enter)'>
                <svg width='16' height='16' viewBox='0 0 16 16' fill='none' xmlns='http://www.w3.org/2000/svg'>
                  <path d='M6 3L10 7L6 11' stroke='#22c55e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'/>
                </svg>
              </button>
              <span id='search-results-info' class='repl-search-info'></span>
              <button id='search-close-btn' class='repl-search-btn' title='Close search (Escape)'>
                <svg width='16' height='16' viewBox='0 0 16 16' fill='none' xmlns='http://www.w3.org/2000/svg'>
                  <path d='M12 4L4 12' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/>
                  <path d='M4 4L12 12' stroke='#22c55e' stroke-width='2' stroke-linecap='round'/>
                </svg>
              </button>
            </div>
          </div>
        </div>
        <div id='repl-output' class='repl-output'></div>
      </div>
    </div>
  </div>")))

(defn create-repl-styles
  []
  "<style>
@import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');

/* CSS Variables for Theme Colors */
:root {
  /* Light Theme */
  --bg-light: #ffffff;
  --text-light: #1a1a1a;
  --primary-light: #22c55e;
  --secondary-light: #f59e0b;
  --accent-light: #ef4444;
  --border-light: #22c55e;
  --surface-light: #f8f9fa;
  
  /* Dark Theme */
  --bg-dark: #000000;
  --text-dark: #22c55e;
  --primary-dark: #22c55e;
  --secondary-dark: #fbbf24;
  --accent-dark: #f87171;
  --border-dark: #22c55e;
  --surface-dark: #111111;
}

html, body {
  height: 100%;
  margin: 0;
  padding: 0;
  background: var(--bg-light);
  color: var(--text-light);
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
}

body, #app, .repl-root {
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  min-width: 100vw;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  background: var(--bg-light);
  color: var(--text-light);
}

body[data-theme='dark'], .dark-theme, .dark-theme #app, .dark-theme .repl-root {
  background: var(--bg-dark);
  color: var(--text-dark);
}

.repl-root {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  min-width: 100vw;
}

.repl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 72px;
  background: rgba(34, 197, 94, 0.10);
  box-shadow: 0 2px 12px 0 rgba(34, 197, 94, 0.10);
  border-bottom: 1.5px solid #22c55e;
  position: relative;
  z-index: 2;
  min-width: 0;
}

.dark-theme .repl-header, body[data-theme='dark'] .repl-header {
  background: rgba(34, 197, 94, 0.15);
  border-bottom: 1.5px solid #22c55e;
  box-shadow: 0 2px 12px 0 rgba(34, 197, 94, 0.20);
}

.repl-header-logo {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 1;
  min-width: 0;
}

.logo-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  image-rendering: pixelated;
  image-rendering: -moz-crisp-edges;
  image-rendering: crisp-edges;
}

.logo-text {
  font-family: 'Press Start 2P', monospace;
  font-size: 38px;
  font-weight: bold;
  color: #ef4444;
  text-shadow: 2px 2px 0px #f59e0b;
  letter-spacing: 2px;
  margin: 0;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
  min-width: 0;
}

.dark-theme .logo-text, body[data-theme='dark'] .logo-text {
  color: #f87171;
  text-shadow: 2px 2px 0px #fbbf24;
}

.repl-header-btns {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  min-width: fit-content;
}

.repl-header-btn {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.repl-header-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.20);
}

.repl-header-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Dependency Button Styles */
.deps-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px !important;
  width: auto !important;
  min-width: 90px;
  background: none !important;
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.deps-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.20) !important;
}

/* Ready state: same as default header buttons */
.deps-btn.deps-resolved {
  background: none !important;
  border: none;
}

.deps-btn.deps-resolved:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.20) !important;
  border: none;
  box-shadow: none;
}

.deps-btn.deps-resolved svg circle {
  fill: rgba(34, 197, 94, 0.12) !important;
}

.deps-btn.deps-resolved svg path {
  stroke: #22c55e !important;
}

.deps-btn.deps-resolved svg circle:first-of-type {
  fill: rgba(34, 197, 94, 0.12) !important;
}

/* Warning state: orange color */
.deps-btn.deps-unresolved {
  background: rgba(245, 158, 11, 0.1) !important;
  border: 1.5px solid rgba(245, 158, 11, 0.3);
  border-radius: 8px;
}

.deps-btn.deps-unresolved:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.2) !important;
  border: 1.5px solid #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.deps-btn.deps-unresolved svg circle {
  fill: rgba(245, 158, 11, 0.15) !important;
}

.deps-btn.deps-unresolved svg path {
  stroke: #f59e0b !important;
}

/* Resolving state: keep current animation */
.deps-btn.deps-resolving {
  animation: pulse-resolving 1.5s infinite;
}

.deps-status-text {
  font-size: 12px;
  font-weight: 600;
  color: #22c55e;
  white-space: nowrap;
}

.deps-resolved .deps-status-text {
  color: #22c55e;
}

.deps-unresolved .deps-status-text {
  color: #f59e0b;
}

/* Dark theme styles for deps button */
.dark-theme .deps-btn, body[data-theme='dark'] .deps-btn {
  background: none !important;
}

.dark-theme .deps-btn.deps-resolved, body[data-theme='dark'] .deps-btn.deps-resolved {
  background: none !important;
}

.dark-theme .deps-btn.deps-unresolved, body[data-theme='dark'] .deps-btn.deps-unresolved {
  background: rgba(245, 158, 11, 0.15) !important;
}

/* Animations */
@keyframes pulse-resolving {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.02);
  }
}

/* REPL button disabled by dependencies */
.repl-header-btn.deps-disabled {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
  position: relative;
}

.repl-header-btn.deps-disabled:hover {
  background: rgba(34, 197, 94, 0.05) !important;
}

.repl-header-btn.deps-disabled::after {
  content: '🔒';
  position: absolute;
  top: -2px;
  right: -2px;
  font-size: 12px;
  background: #ef4444;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 8px;
}

.repl-toolbar {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 32px 0 32px;
  background: transparent;
  flex-wrap: wrap;
  width: 100%;
  box-sizing: border-box;
}

.repl-toolbar-group {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  flex: 1;
  min-width: 0;
}

.repl-toolbar-btn {
  background: none;
  border: 1.5px solid #22c55e;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border 0.2s, background 0.2s;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  background: var(--bg-light);
}

.repl-toolbar-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.10);
  border: 1.5px solid #16a34a;
}

.repl-toolbar-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dark-theme .repl-toolbar-btn, body[data-theme='dark'] .repl-toolbar-btn {
  background: var(--bg-dark);
  border: 1.5px solid #22c55e;
}

.dark-theme .repl-toolbar-btn:hover:not(:disabled), body[data-theme='dark'] .repl-toolbar-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.15);
  border: 1.5px solid #16a34a;
}

.repl-combo {
  font-family: inherit;
  font-size: 15px;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1.5px solid #22c55e;
  background: var(--bg-light);
  color: var(--text-light);
  outline: none;
  flex: 1;
  min-width: 0;
  width: auto;
  transition: border 0.2s;
  margin-bottom: 8px;
  box-sizing: border-box;
}

.dark-theme .repl-combo, body[data-theme='dark'] .repl-combo {
  background: var(--bg-dark);
  color: var(--text-dark);
  border: 1.5px solid #22c55e;
}

.repl-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 32px 32px 32px;
  background: transparent;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  margin-right: 0;
  box-sizing: border-box;
}

/* Resizable sections */
.repl-input-section {
  display: flex;
  flex-direction: column;
  min-height: 80px;
  height: 30%;
  width: 100%;
  max-height: calc(100vh - 300px);
}

.repl-controls-section {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 56px;
  width: 100%;
  background: rgba(34, 197, 94, 0.08);
  border-radius: 10px;
  margin: 8px 0;
  padding: 8px 12px;
  box-sizing: border-box;
  position: relative;
  z-index: 2;
  pointer-events: auto;
}

.dark-theme .repl-controls-section, body[data-theme='dark'] .repl-controls-section {
  background: rgba(34, 197, 94, 0.12);
}

.repl-splitter-row {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  margin-bottom: 8px;
}

.repl-splitter {
  flex: 1;
  height: 10px;
  background: rgba(34, 197, 94, 0.15);
  cursor: row-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, box-shadow 0.2s;
  user-select: none;
  border-radius: 8px;
  box-shadow: 0 1px 4px 0 rgba(34, 197, 94, 0.12);
}

.repl-splitter:hover {
  background: rgba(34, 197, 94, 0.25);
  box-shadow: 0 2px 8px 0 rgba(34, 197, 94, 0.18);
}

.repl-splitter-handle {
  width: 60px;
  height: 4px;
  background: #22c55e;
  border-radius: 4px;
  transition: all 0.2s;
}

.repl-splitter:hover .repl-splitter-handle {
  width: 80px;
  height: 4px;
}

.dark-theme .repl-splitter, body[data-theme='dark'] .repl-splitter {
  background: rgba(34, 197, 94, 0.20);
}

.dark-theme .repl-splitter:hover, body[data-theme='dark'] .repl-splitter:hover {
  background: rgba(34, 197, 94, 0.30);
}

.repl-buttons-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  min-width: 0;
  padding-right: 12px;
}

.repl-output-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.repl-output-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 150px;
  overflow: hidden;
  flex-shrink: 0;
}

.repl-input-row {
  display: flex;
  align-items: stretch;
  gap: 0;
  margin-top: 32px;
  margin-bottom: 12px;
  width: 100%;
  min-width: 0;
  height: 100%;
  position: static !important;
  z-index: auto !important;
}

.repl-input-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.repl-input {
  width: 100%;
  height: 100%;
  border: 2px solid #22c55e;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px 0 rgba(34, 197, 94, 0.08);
  box-sizing: border-box;
  background: var(--bg-light);
  color: var(--text-light);
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 80px;
  outline: none;
  white-space: pre-wrap;
  word-wrap: break-word;
  position: relative;
  caret-color: #22c55e;
}

.repl-input::placeholder {
  color: #22c55e;
  opacity: 0.7;
  font-size: 13px;
  line-height: 1.4;
  white-space: pre-line;
}

.repl-input:disabled {
  color: #22c55e;
  opacity: 0.8;
  cursor: not-allowed;
  background: #f8f9fa;
}

.repl-input:focus {
  border-color: #16a34a;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
}

.dark-theme .repl-input, body[data-theme='dark'] .repl-input {
  background: var(--bg-dark);
  border: 2px solid #22c55e;
  color: var(--text-dark);
}

.dark-theme .repl-input:disabled, body[data-theme='dark'] .repl-input:disabled {
  background: #111111;
}

.dark-theme .repl-input:focus, body[data-theme='dark'] .repl-input:focus {
  border-color: #16a34a;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
}

/* REPL State-specific animations */
.repl-input:disabled {
  /* Base styles already defined above - no animation here */
}

/* LIGHT THEME - Evaluating state (with animation) */
.repl-evaluating .repl-input:disabled {
  background-image: linear-gradient(45deg, transparent 35%, rgba(34, 197, 94, 0.1) 35%, rgba(34, 197, 94, 0.1) 65%, transparent 65%);
  background-size: 20px 20px;
  animation: loading-stripes 1s linear infinite;
}

/* LIGHT THEME - Connecting state (with pulsing animation) */
.repl-connecting .repl-input:disabled {
  background-image: linear-gradient(90deg, #f8f9fa 0%, rgba(34, 197, 94, 0.08) 50%, #f8f9fa 100%);
  background-size: 200% 100%;
  animation: connecting-pulse 2s ease-in-out infinite;
}

/* DARK THEME - Evaluating state (with animation) */
.dark-theme.repl-evaluating .repl-input:disabled,
body[data-theme='dark'].repl-evaluating .repl-input:disabled {
  background-image: linear-gradient(45deg, transparent 35%, rgba(34, 197, 94, 0.15) 35%, rgba(34, 197, 94, 0.15) 65%, transparent 65%);
  background-size: 20px 20px;
  animation: loading-stripes-dark 1s linear infinite;
}

/* DARK THEME - Connecting state (with pulsing animation) */
.dark-theme.repl-connecting .repl-input:disabled,
body[data-theme='dark'].repl-connecting .repl-input:disabled {
  background-image: linear-gradient(90deg, #111111 0%, rgba(34, 197, 94, 0.12) 50%, #111111 100%);
  background-size: 200% 100%;
  animation: connecting-pulse-dark 2s ease-in-out infinite;
}

@keyframes loading-stripes {
  0% { background-position: 0 0; }
  100% { background-position: 20px 0; }
}

@keyframes loading-stripes-dark {
  0% { background-position: 0 0; }
  100% { background-position: 20px 0; }
}

@keyframes connecting-pulse {
  0% { background-position: -200% 0; }
  50% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes connecting-pulse-dark {
  0% { background-position: -200% 0; }
  50% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Scrollbar customizado */
.repl-input::-webkit-scrollbar,
.repl-output::-webkit-scrollbar {
  width: 8px;
  background: transparent;
}

.repl-input::-webkit-scrollbar-thumb,
.repl-output::-webkit-scrollbar-thumb {
  background: rgba(34, 197, 94, 0.3);
  border-radius: 6px;
}

.repl-input::-webkit-scrollbar-thumb:hover,
.repl-output::-webkit-scrollbar-thumb:hover {
  background: #22c55e;
}

/* Firefox */
.repl-input,
.repl-output {
  scrollbar-width: thin;
  scrollbar-color: rgba(34, 197, 94, 0.3) transparent;
}

.repl-output-btn {
  background: rgba(34, 197, 94, 0.2);
  border: 1px solid #22c55e;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.repl-output-btn:hover {
  background: rgba(34, 197, 94, 0.4);
}

.repl-output {
  flex: 1;
  width: 100%;
  background: var(--bg-light);
  border: 2px solid #22c55e;
  border-radius: 12px;
  box-shadow: 0 2px 8px 0 rgba(34, 197, 94, 0.08);
  min-width: 0;
  height: 100%;
  min-height: 200px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  margin-right: 24px;
  padding: 12px;
  box-sizing: border-box;
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-light);
}

.dark-theme .repl-output, body[data-theme='dark'] .repl-output {
  background: var(--bg-dark);
  border: 2px solid #22c55e;
  color: var(--text-dark);
}

.repl-prompt {
  color: #22c55e;
  font-weight: bold;
  margin-right: 6px;
}

/* Input commands shown in output */
.input {
  color: #16a34a;
  font-weight: bold;
  margin: 8px 0;
  padding: 4px 0;
  border-left: 3px solid #22c55e;
  padding-left: 8px;
  background: rgba(34, 197, 94, 0.08);
}

.dark-theme .input, body[data-theme='dark'] .input {
  color: #4ade80;
  background: rgba(34, 197, 94, 0.12);
}

/* REPL Output Styles */
.repl-entry {
  margin-bottom: 8px;
  line-height: 1.5;
}

.repl-input-line {
  color: #22c55e;
  font-weight: bold;
  margin-bottom: 4px;
}

.repl-output-line {
  color: var(--text-light);
  margin-bottom: 4px;
}

.dark-theme .repl-output-line, body[data-theme='dark'] .repl-output-line {
  color: var(--text-dark);
}

.repl-result,
.repl-stdout {
  color: #16a34a;
  font-weight: bold;
  margin: 8px 0;
  padding: 4px 0;
  border-left: 3px solid #22c55e;
  padding-left: 8px;
  background: rgba(34, 197, 94, 0.08);
}

.dark-theme .repl-result, body[data-theme='dark'] .repl-result,
.dark-theme .repl-stdout, body[data-theme='dark'] .repl-stdout {
  color: #4ade80;
  background: rgba(34, 197, 94, 0.12);
}

.repl-error {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 2px 0;
  font-weight: 500;
}

.repl-stdout {
  color: #059669;
  background: rgba(5, 150, 105, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 2px 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.repl-stderr {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.035);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 2px 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.dark-theme .repl-stderr, body[data-theme='dark'] .repl-stderr {
  color: #f87171;
  background: rgba(239, 68, 68, 0.055);
}

.repl-command {
  color: #0ea5e9;
  background: rgba(14, 165, 233, 0.1);
  padding: 4px 8px;
  border-left: 3px solid #0ea5e9;
  border-radius: 4px;
  margin: 2px 0;
  font-weight: 600;
}

.repl-status {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 2px 0;
  font-weight: 500;
  border-left: 3px solid #f59e0b;
}

.dark-theme .repl-status, body[data-theme='dark'] .repl-status {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
  border-left-color: #fbbf24;
}

.repl-loading {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 2px 0;
  font-weight: 500;
  border-left: 3px solid #fbbf24;
}

.dark-theme .repl-loading, body[data-theme='dark'] .repl-loading {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
  border-left-color: #fbbf24;
}

.repl-help {
  color: #059669;
  background: rgba(5, 150, 105, 0.05);
  padding: 8px 12px;
  border-radius: 8px;
  margin: 4px 0;
  border-left: 3px solid #059669;
}

/* Disabled states */
.repl-combo:disabled {
  opacity: 0.6;
  background: #f5f5f5 !important;
  color: #888 !important;
  cursor: not-allowed;
  border-color: #ddd !important;
}

.dark-theme .repl-combo:disabled, body[data-theme='dark'] .repl-combo:disabled {
  background: #222222 !important;
  color: #666 !important;
  border-color: #555 !important;
}

/* Search Container Styles */
.repl-search-container {
  display: none;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid #22c55e;
  border-radius: 8px;
  margin-bottom: 8px;
  padding: 8px;
  position: relative;
  z-index: 10;
}

.repl-search-container.active {
  display: block;
}

.dark-theme .repl-search-container, body[data-theme='dark'] .repl-search-container {
  background: rgba(34, 197, 94, 0.12);
}

.repl-search-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.repl-search-input {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid #22c55e;
  border-radius: 6px;
  background: var(--bg-light);
  color: var(--text-light);
  font-family: inherit;
  font-size: 14px;
  outline: none;
  min-width: 0;
}

.repl-search-options {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.repl-search-checkbox-label {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background-color 0.2s;
  user-select: none;
}

.repl-search-checkbox-label:hover {
  background: rgba(34, 197, 94, 0.1);
}

.repl-search-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.checkbox-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-light);
  min-width: 20px;
  text-align: center;
}

.dark-theme .checkbox-text, body[data-theme='dark'] .checkbox-text {
  color: var(--text-dark);
}

.repl-search-input:focus {
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
}

.dark-theme .repl-search-input, body[data-theme='dark'] .repl-search-input {
  background: var(--bg-dark);
  color: var(--text-dark);
}

.repl-search-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.repl-search-btn {
  background: rgba(34, 197, 94, 0.2);
  border: 1px solid #22c55e;
  padding: 4px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.repl-search-btn:hover {
  background: rgba(34, 197, 94, 0.4);
}

.repl-search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.repl-search-info {
  color: var(--text-light);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  min-width: 60px;
  text-align: center;
}

.dark-theme .repl-search-info, body[data-theme='dark'] .repl-search-info {
  color: var(--text-dark);
}

/* Search Highlight Styles */
.search-highlight {
  background: rgba(251, 191, 36, 0.6);
  color: #000;
  border-radius: 2px;
  padding: 1px 2px;
}

.search-highlight.current {
  background: rgba(245, 158, 11, 0.8);
  color: #fff;
  font-weight: bold;
}

.dark-theme .search-highlight, body[data-theme='dark'] .search-highlight {
  background: rgba(251, 191, 36, 0.7);
  color: #000;
}

.dark-theme .search-highlight.current, body[data-theme='dark'] .search-highlight.current {
  background: rgba(245, 158, 11, 0.9);
  color: #fff;
}

/* Animation keyframes */
@keyframes pulse-input {
  0% { border-color: #22c55e; box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4); }
  50% { border-color: #16a34a; box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.2); }
  100% { border-color: #22c55e; box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4); }
}

@keyframes pulse-evaluating {
  0% { border-color: #fbbf24; box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.4); }
  50% { border-color: #f59e0b; box-shadow: 0 0 0 4px rgba(251, 191, 36, 0.2); }
  100% { border-color: #fbbf24; box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.4); }
}

/* REPL State Animations */
.repl-connecting .repl-input {
  animation: pulse-input 2s ease-in-out infinite;
  border-color: #22c55e;
}

.repl-evaluating .repl-input {
  animation: pulse-evaluating 1.5s ease-in-out infinite;
  border-color: #fbbf24;
}

.dark-theme .repl-connecting .repl-input,
body[data-theme='dark'] .repl-connecting .repl-input {
  animation: pulse-input 2s ease-in-out infinite;
  border-color: #22c55e;
}

.dark-theme .repl-evaluating .repl-input,
body[data-theme='dark'] .repl-evaluating .repl-input {
  animation: pulse-evaluating 1.5s ease-in-out infinite;
  border-color: #fbbf24;
}

/* Utility classes for splitter */
.no-select {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

/* Responsive design */
@media (max-width: 900px) {
  .repl-header, .repl-toolbar, .repl-main { padding-left: 8px; padding-right: 8px; }
  .logo-text { font-size: 22px; }
  .logo-icon { width: 32px; height: 32px; }
  .repl-header-logo { gap: 8px; }
  .repl-input-row { flex-direction: column; gap: 8px; }
  .repl-input-editor, .repl-output-editor { font-size: 15px; }
  .repl-combo { min-width: 0; flex: 1; }
  .repl-toolbar-group { width: 100%; }
  .repl-input-section { min-height: 60px; }
  .repl-output-section { min-height: 150px; }
}

@media (max-width: 768px) {
  .repl-header { padding: 0 16px; height: 60px; }
  .logo-text { font-size: 28px; letter-spacing: 1px; }
  .logo-icon { width: 28px; height: 28px; }
  .repl-splitter { height: 12px; }
  .repl-splitter-handle { width: 40px; height: 4px; }
}

@media (max-width: 600px) {
  .repl-header { padding: 0 12px; height: 56px; }
  .repl-header-btns { gap: 6px; }
  .logo-text { font-size: 22px; letter-spacing: 1px; }
  .logo-icon { width: 24px; height: 24px; }
  .repl-header-logo { gap: 6px; }
  .repl-toolbar { flex-direction: column; gap: 8px; padding-top: 8px; }
  .repl-main { padding: 0 4px 8px 4px; }
  .repl-input-row { margin-top: 12px; }
  .repl-output-editor { min-height: 120px; }
  .repl-input-section { min-height: 50px; height: 25%; }
  .repl-output-section { min-height: 120px; }
  .repl-splitter { height: 16px; }
  .repl-splitter-handle { width: 50px; height: 5px; }
}

@media (max-width: 400px) {
  .repl-header { padding: 0 8px; height: 52px; }
  .logo-text { font-size: 18px; letter-spacing: 0.5px; }
  .logo-icon { width: 20px; height: 20px; }
  .repl-input-section { height: 20%; }
  .repl-splitter { height: 20px; }
}
</style>")

(defn update-app-content
  "Update the main app content safely"
  [content]
  (try
    (when-let [app-element (.getElementById js/document "app")]
      (set! (.-innerHTML app-element) content))
    (catch :default e
      (.log js/console "Error updating app content:" e)))) 