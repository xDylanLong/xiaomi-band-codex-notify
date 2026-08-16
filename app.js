(function () {
  "use strict";

  const api = window.XiaomiWatchFace;
  const canvas = document.getElementById("canvas");
  const ctx = canvas.getContext("2d");
  let state = api.normalizeState(api.DEFAULT_STATE);
  let drag = null;
  let statusTimer = null;

  const $ = (id) => document.getElementById(id);
  const controls = {
    backgroundColor: $("backgroundColor"),
    backgroundImage: $("backgroundImage"),
    clearImage: $("clearImage"),
    timeStyle: $("timeStyle"),
    customText: $("customText"),
    primaryColor: $("primaryColor"),
    accentColor: $("accentColor"),
    timeVisible: $("timeVisible"),
    dateVisible: $("dateVisible"),
    stepsVisible: $("stepsVisible"),
    heartVisible: $("heartVisible"),
    textVisible: $("textVisible")
  };

  function widget(id) { return state.widgets.find((item) => item.id === id); }

  function showStatus(message, isError) {
    const node = $("statusMessage");
    node.textContent = message;
    node.classList.toggle("error", Boolean(isError));
    clearTimeout(statusTimer);
    if (message) statusTimer = setTimeout(() => { node.textContent = ""; node.classList.remove("error"); }, 5000);
  }

  function render() {
    api.renderWatchFace(ctx, state);
    $("backgroundColorValue").textContent = state.background.color.toUpperCase();
  }

  function syncControls() {
    controls.backgroundColor.value = state.background.color;
    controls.timeStyle.value = state.timeStyle;
    controls.customText.value = widget("text").value || "";
    controls.primaryColor.value = state.palette.primary;
    controls.accentColor.value = state.palette.accent;
    ["time", "date", "steps", "heart", "text"].forEach((id) => { controls[`${id}Visible`].checked = widget(id).visible; });
  }

  function setVisible(id, value) { widget(id).visible = Boolean(value); render(); }

  controls.backgroundColor.addEventListener("input", (event) => { state.background.color = event.target.value; render(); });
  controls.timeStyle.addEventListener("change", (event) => { state.timeStyle = event.target.value; render(); });
  controls.customText.addEventListener("input", (event) => { widget("text").value = event.target.value.slice(0, 18); render(); });
  controls.primaryColor.addEventListener("input", (event) => { state.palette.primary = event.target.value; render(); });
  controls.accentColor.addEventListener("input", (event) => { state.palette.accent = event.target.value; render(); });
  ["time", "date", "steps", "heart", "text"].forEach((id) => controls[`${id}Visible`].addEventListener("change", (event) => setVisible(id, event.target.checked)));

  controls.backgroundImage.addEventListener("change", (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) { showStatus("请选择 PNG、JPG 或 WebP 图片。", true); return; }
    const reader = new FileReader();
    reader.addEventListener("load", () => {
      state.background.imageDataUrl = reader.result;
      render();
      showStatus("背景图已加载，画布会自动裁切为 480×336。");
    });
    reader.addEventListener("error", () => showStatus("图片读取失败，当前背景未改变。", true));
    reader.readAsDataURL(file);
  });

  controls.clearImage.addEventListener("click", () => { state.background.imageDataUrl = null; controls.backgroundImage.value = ""; render(); showStatus("已移除背景图。"); });
  $("resetButton").addEventListener("click", () => { state = api.normalizeState(api.DEFAULT_STATE); syncControls(); render(); showStatus("已恢复默认表盘。"); });

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  $("exportPng").addEventListener("click", () => {
    canvas.toBlob((blob) => {
      if (!blob) { showStatus("PNG 导出失败，请重试。", true); return; }
      downloadBlob(blob, `xiaomi-band-10-pro-${new Date().toISOString().slice(0, 10)}.png`);
      showStatus("PNG 已导出，可按右侧步骤导入小米运动健康。");
    }, "image/png");
  });

  $("exportProject").addEventListener("click", () => {
    const json = JSON.stringify(api.serializeProject(state), null, 2);
    downloadBlob(new Blob([json], { type: "application/json" }), "band-10-pro-watchface.watchface.json");
    showStatus("可编辑项目已导出，之后可以重新导入继续修改。");
  });

  $("importProject").addEventListener("change", (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.addEventListener("load", () => {
      try {
        const result = api.validateProject(JSON.parse(reader.result));
        if (!result.ok) { showStatus(result.error, true); return; }
        state = result.state;
        syncControls();
        render();
        showStatus("项目已导入。");
      } catch (error) {
        showStatus("项目文件无法解析，请选择本工具导出的 JSON。", true);
      } finally { event.target.value = ""; }
    });
    reader.addEventListener("error", () => showStatus("项目读取失败，当前内容未改变。", true));
    reader.readAsText(file);
  });

  function canvasPoint(event) {
    const rect = canvas.getBoundingClientRect();
    return { x: (event.clientX - rect.left) * api.WIDTH / rect.width, y: (event.clientY - rect.top) * api.HEIGHT / rect.height };
  }

  function hitRadius(item) {
    if (item.type === "time") return 84;
    if (item.type === "date") return 58;
    if (item.type === "text") return 75;
    return 66;
  }

  canvas.addEventListener("pointerdown", (event) => {
    const point = canvasPoint(event);
    const candidates = state.widgets.filter((item) => item.visible).slice().reverse();
    const selected = candidates.find((item) => Math.hypot(item.x - point.x, item.y - point.y) <= hitRadius(item));
    if (!selected) return;
    drag = { id: selected.id, offsetX: selected.x - point.x, offsetY: selected.y - point.y };
    canvas.setPointerCapture(event.pointerId);
  });

  canvas.addEventListener("pointermove", (event) => {
    if (!drag) return;
    const point = canvasPoint(event);
    const item = widget(drag.id);
    item.x = Math.max(18, Math.min(api.WIDTH - 18, point.x + drag.offsetX));
    item.y = Math.max(18, Math.min(api.HEIGHT - 18, point.y + drag.offsetY));
    render();
  });
  canvas.addEventListener("pointerup", () => { drag = null; });
  canvas.addEventListener("pointercancel", () => { drag = null; });
  window.addEventListener("watchface-image-ready", render);

  syncControls();
  render();
  setInterval(render, 1000);

  window.WatchFaceApp = {
    getState: () => api.serializeProject(state),
    render,
    loadProject: (project) => {
      const result = api.validateProject(project);
      if (!result.ok) throw new Error(result.error);
      state = result.state;
      syncControls();
      render();
    }
  };
})();
