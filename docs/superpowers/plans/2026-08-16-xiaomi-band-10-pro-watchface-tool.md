# 小米手环 10 Pro 自定义表盘工具 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dependency-free browser app that designs, previews, exports, and re-imports 480×336 Xiaomi Smart Band 10 Pro photo-watch-face projects.

**Architecture:** A static HTML/CSS/JavaScript app stores all state in one serializable object. Canvas rendering is the single source of truth for both the live preview and exported PNG. A JSON project file preserves the editable state for later Android/Vela integration, while the UI explains the currently verified Mi Fitness photo-watch-face path.

**Tech Stack:** HTML5, CSS3, vanilla JavaScript, Canvas 2D, FileReader, Blob/download APIs.

## Global Constraints

- Target canvas is exactly 480×336 pixels.
- No server, build tool, external CDN, or runtime dependency is required.
- The app must distinguish official photo-watch-face installation from experimental community `.bin`/Vela paths.
- Exported project JSON must be validated before import.
- Preserve user work when an image or project import fails.

---

### Task 1: Create the static application shell

**Files:**
- Create: `index.html`
- Create: `styles.css`

**Interfaces:**
- `index.html` provides IDs `#app`, `#backgroundColor`, `#backgroundImage`, `#resetButton`, `#exportPng`, `#exportProject`, `#importProject`, `#statusMessage`, `#canvas`, and all widget controls used by `app.js`.
- `styles.css` provides the responsive editor layout, dark workbench styling, device frame, form controls, and accessible focus states.

- [ ] **Step 1: Write the semantic page shell** with Chinese headings, editor controls, preview region, export actions, installation guide, and a hidden file input for project import.
- [ ] **Step 2: Add responsive CSS** so the three-column desktop layout collapses into a single-column mobile layout without changing canvas aspect ratio.
- [ ] **Step 3: Verify the shell** by opening `index.html` directly and confirming controls and canvas region render without console errors.

### Task 2: Implement state validation and Canvas rendering

**Files:**
- Create: `renderer.js`

**Interfaces:**
- `DEFAULT_STATE` is the initial serializable state.
- `renderWatchFace(ctx, state, options)` draws the state to a 480×336 canvas.
- `serializeProject(state)` returns a validated JSON-safe object.
- `validateProject(project)` returns `{ ok: true, state }` or `{ ok: false, error }`.

- [ ] **Step 1: Define `DEFAULT_STATE`** with background color, optional image data URL, time/date/steps/heart widgets, positions, colors, and visibility.
- [ ] **Step 2: Implement the renderer** for background, image crop, time, date, steps, heart rate, grid-safe text, and subtle chrome.
- [ ] **Step 3: Implement strict project validation** for format, version, 480×336 dimensions, color strings, finite coordinates, and known widget types.
- [ ] **Step 4: Verify rendering and validation** with a small browser console smoke check: default render succeeds, malformed format is rejected, and unknown widget type is rejected.

### Task 3: Wire interactions and exports

**Files:**
- Create: `app.js`

**Interfaces:**
- `window.WatchFaceApp` exposes `getState()`, `render()`, and `loadProject(project)` for smoke testing.

- [ ] **Step 1: Bind form controls** to state changes and call the single render function after every change.
- [ ] **Step 2: Implement image loading** with FileReader, preserving the prior background when loading fails and showing a status message.
- [ ] **Step 3: Implement pointer drag** for visible widgets on the canvas, updating coordinates with canvas-to-device scale conversion and clamping them inside the canvas.
- [ ] **Step 4: Implement PNG export** using `canvas.toBlob`, with a filename containing the current date.
- [ ] **Step 5: Implement JSON export and import** using Blob/download and FileReader plus `validateProject`.
- [ ] **Step 6: Verify interaction flows** manually: toggle a widget, change colors, drag a widget, upload an image, export/import JSON, and export a 480×336 PNG.

### Task 4: Add research and usage documentation

**Files:**
- Create: `README.md`
- Create: `docs/research/xiaomi-band-10-pro-watchface-capabilities.md`

**Interfaces:**
- README documents the zero-build launch command and export workflow.
- Research document records official, community, and Vela capabilities with source links and confidence labels.

- [ ] **Step 1: Document local launch** with `open index.html` and a simple local HTTP server alternative.
- [ ] **Step 2: Document the official Mi Fitness installation path** and the exact distinction between PNG/photo face and native third-party watch-face package.
- [ ] **Step 3: Document follow-up Android/Vela adapter work** without claiming it is implemented or verified.

### Task 5: Run verification and package the handoff

**Files:**
- Modify: `README.md` if verification reveals inaccurate instructions.

- [ ] **Step 1: Run syntax checks** with `node --check app.js` and `node --check renderer.js`.
- [ ] **Step 2: Run a local HTTP server** and use a browser smoke test to confirm the app loads and exports.
- [ ] **Step 3: Inspect the generated PNG metadata** to confirm width 480 and height 336.
- [ ] **Step 4: Inspect Git diff/status** and report exact runnable files and known device limitations.
