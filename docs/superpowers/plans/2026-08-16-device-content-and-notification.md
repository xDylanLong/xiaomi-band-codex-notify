# 设备内容与通知能力扩展 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an exportable exercise-plan card mode and honest notification/reply capability guidance to the existing Band 10 Pro editor.

**Architecture:** Extend the existing serializable state with `mode` and `plan` fields. Keep Canvas as the single rendering source: watchface mode uses the current widget renderer, plan mode uses a dedicated card renderer. The right-hand handoff panel becomes the capability truth surface.

**Tech Stack:** Existing dependency-free HTML, CSS, vanilla JavaScript, Canvas 2D.

## Global Constraints

- Keep the target canvas exactly 480×336.
- Do not claim computer-to-band push without an Android notification bridge.
- Do not claim third-party message replies or voice replies; Xiaomi’s official FAQ says notifications are view-only.
- Keep screenshot/document image import local-only.

---

### Task 1: Extend state and Canvas rendering

**Files:**
- Modify: `renderer.js`

- [ ] Add `mode` and serializable `plan` fields to `DEFAULT_STATE`.
- [ ] Validate and normalize plan strings, date, duration, and completion state.
- [ ] Add a `renderPlanCard(ctx, state)` branch with date, title, duration, target, note, and completion marker.
- [ ] Include mode and plan in `serializeProject` and `validateProject`.

### Task 2: Add exercise-plan controls and capability UI

**Files:**
- Modify: `index.html`
- Modify: `styles.css`

- [ ] Add mode selector and plan form fields while preserving existing watchface controls.
- [ ] Add copy describing screenshot / Feishu image import.
- [ ] Add capability cards for phone notifications, computer/Codex direct push, and WeChat voice reply.
- [ ] Make the new controls responsive with the existing mobile layout.

### Task 3: Wire interactions and documentation

**Files:**
- Modify: `app.js`
- Modify: `README.md`
- Modify: `docs/research/xiaomi-band-10-pro-watchface-capabilities.md`

- [ ] Bind mode and plan controls to state and rerender.
- [ ] Restore plan values on project import and reset them on reset.
- [ ] Document the official notification setup path and the unimplemented Android bridge.
- [ ] Document that exported plan cards and screenshots are static images.

### Task 4: Verify and commit

**Files:**
- No new files.

- [ ] Run `node --check renderer.js` and `node --check app.js`.
- [ ] Run `git diff --check`.
- [ ] Browser-test mode switching, plan editing, PNG export, JSON import/export, and mobile layout.
- [ ] Commit the feature as one focused change.
