(function () {
  "use strict";

  const WIDTH = 480;
  const HEIGHT = 336;
  const FORMAT = "xiaomi-band-watchface-project";
  const VERSION = 1;
  const imageCache = new Map();

  const DEFAULT_STATE = {
    format: FORMAT,
    version: VERSION,
    device: { model: "Xiaomi Smart Band 10 Pro", width: WIDTH, height: HEIGHT },
    mode: "watchface",
    plan: { date: new Date().toISOString().slice(0, 10), title: "下肢力量训练", duration: 30, target: "深蹲 3×12 · 步行 20 分钟", note: "保持稳定呼吸，完成后拉伸", completed: false },
    background: { color: "#10141c", imageDataUrl: null },
    palette: { primary: "#f4f7fb", accent: "#9ff36b" },
    timeStyle: "24",
    widgets: [
      { id: "time", type: "time", x: 240, y: 122, colorRole: "primary", visible: true, fontSize: 77 },
      { id: "date", type: "date", x: 240, y: 187, colorRole: "primary", visible: true, fontSize: 18 },
      { id: "steps", type: "steps", x: 126, y: 269, colorRole: "accent", visible: true, fontSize: 16 },
      { id: "heart", type: "heart", x: 353, y: 269, colorRole: "accent", visible: true, fontSize: 16 },
      { id: "text", type: "text", x: 240, y: 306, colorRole: "primary", visible: true, fontSize: 11, value: "FOCUS / 01" }
    ]
  };

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function color(value, fallback) {
    return typeof value === "string" && /^#[0-9a-f]{6}$/i.test(value) ? value : fallback;
  }

  function number(value, fallback) {
    return Number.isFinite(value) ? value : fallback;
  }

  function validateProject(project) {
    if (!project || typeof project !== "object") return { ok: false, error: "项目文件不是有效 JSON。" };
    if (project.format !== FORMAT) return { ok: false, error: "这不是表盘工坊项目文件。" };
    if (project.version !== VERSION) return { ok: false, error: "项目版本不兼容，请用当前版本重新导出。" };
    if (!project.device || project.device.width !== WIDTH || project.device.height !== HEIGHT) return { ok: false, error: "项目画布不是 480×336，无法导入。" };
    if (!project.background || typeof project.background !== "object") return { ok: false, error: "项目缺少背景配置。" };
    if (!Array.isArray(project.widgets)) return { ok: false, error: "项目缺少组件配置。" };
    const known = new Set(["time", "date", "steps", "heart", "text"]);
    for (const widget of project.widgets) {
      if (!widget || !known.has(widget.type)) return { ok: false, error: "项目包含不支持的组件。" };
      if (!Number.isFinite(widget.x) || !Number.isFinite(widget.y)) return { ok: false, error: "组件位置无效。" };
      if (typeof widget.visible !== "boolean") return { ok: false, error: "组件可见状态无效。" };
    }
    return { ok: true, state: normalizeState(project) };
  }

  function normalizeState(source) {
    const base = clone(DEFAULT_STATE);
    const incomingPlan = source.plan && typeof source.plan === "object" ? source.plan : {};
    const state = {
      ...base,
      ...source,
      device: { ...base.device, ...(source.device || {}), width: WIDTH, height: HEIGHT },
      mode: source.mode === "plan" ? "plan" : "watchface",
      plan: {
        ...base.plan,
        date: typeof incomingPlan.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(incomingPlan.date) ? incomingPlan.date : base.plan.date,
        title: typeof incomingPlan.title === "string" ? incomingPlan.title.slice(0, 22) : base.plan.title,
        duration: Number.isFinite(incomingPlan.duration) ? Math.max(1, Math.min(180, Math.round(incomingPlan.duration))) : base.plan.duration,
        target: typeof incomingPlan.target === "string" ? incomingPlan.target.slice(0, 48) : base.plan.target,
        note: typeof incomingPlan.note === "string" ? incomingPlan.note.slice(0, 60) : base.plan.note,
        completed: incomingPlan.completed === true
      },
      background: { ...base.background, ...(source.background || {}), color: color(source.background && source.background.color, base.background.color) },
      palette: { ...base.palette, ...(source.palette || {}), primary: color(source.palette && source.palette.primary, base.palette.primary), accent: color(source.palette && source.palette.accent, base.palette.accent) },
      timeStyle: source.timeStyle === "12" ? "12" : "24",
      widgets: base.widgets.map((fallback) => {
        const incoming = Array.isArray(source.widgets) ? source.widgets.find((item) => item.id === fallback.id) : null;
        if (!incoming) return fallback;
        return {
          ...fallback,
          ...incoming,
          x: Math.max(14, Math.min(WIDTH - 14, number(incoming.x, fallback.x))),
          y: Math.max(14, Math.min(HEIGHT - 14, number(incoming.y, fallback.y))),
          fontSize: Math.max(8, Math.min(120, number(incoming.fontSize, fallback.fontSize))),
          visible: incoming.visible !== false,
          colorRole: incoming.colorRole === "accent" ? "accent" : "primary",
          value: typeof incoming.value === "string" ? incoming.value.slice(0, 18) : fallback.value
        };
      })
    };
    return state;
  }

  function serializeProject(state) {
    const normalized = normalizeState(state);
    return {
      format: FORMAT,
      version: VERSION,
      device: { model: normalized.device.model, width: WIDTH, height: HEIGHT },
      mode: normalized.mode,
      plan: { ...normalized.plan },
      background: { color: normalized.background.color, imageDataUrl: normalized.background.imageDataUrl || null },
      palette: { ...normalized.palette },
      timeStyle: normalized.timeStyle,
      widgets: normalized.widgets.map((widget) => ({ ...widget }))
    };
  }

  function getImage(url) {
    if (!url) return null;
    if (imageCache.has(url)) return imageCache.get(url);
    const image = new Image();
    image.decoding = "async";
    image.src = url;
    image.addEventListener("load", () => window.dispatchEvent(new Event("watchface-image-ready")), { once: true });
    imageCache.set(url, image);
    return image;
  }

  function drawCover(ctx, image) {
    if (!image || !image.naturalWidth || !image.naturalHeight) return false;
    const scale = Math.max(WIDTH / image.naturalWidth, HEIGHT / image.naturalHeight);
    const width = image.naturalWidth * scale;
    const height = image.naturalHeight * scale;
    ctx.drawImage(image, (WIDTH - width) / 2, (HEIGHT - height) / 2, width, height);
    return true;
  }

  function nowTime(style) {
    const date = new Date();
    let hours = date.getHours();
    const suffix = hours >= 12 ? "PM" : "AM";
    if (style === "12") hours = hours % 12 || 12;
    return { time: `${String(hours).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`, suffix };
  }

  function weekday(date) {
    return ["日", "一", "二", "三", "四", "五", "六"][date.getDay()];
  }

  function findWidget(state, id) {
    return state.widgets.find((widget) => widget.id === id);
  }

  function drawWidget(ctx, state, widget) {
    if (!widget.visible) return;
    const primary = state.palette.primary;
    const accent = state.palette.accent;
    const fill = widget.colorRole === "accent" ? accent : primary;
    const date = new Date();
    ctx.save();
    ctx.translate(widget.x, widget.y);
    ctx.fillStyle = fill;
    ctx.strokeStyle = fill;
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.shadowColor = "rgba(0, 0, 0, .24)";
    ctx.shadowBlur = widget.type === "time" ? 12 : 5;
    if (widget.type === "time") {
      const current = nowTime(state.timeStyle);
      ctx.font = `700 ${widget.fontSize}px Inter, -apple-system, BlinkMacSystemFont, sans-serif`;
      ctx.letterSpacing = "-3px";
      ctx.fillText(current.time, 0, 0);
      if (state.timeStyle === "12") {
        ctx.shadowBlur = 0;
        ctx.font = "700 11px Inter, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(current.suffix, widget.fontSize * 1.18, 1);
      }
    } else if (widget.type === "date") {
      ctx.shadowBlur = 3;
      ctx.font = `500 ${widget.fontSize}px Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif`;
      ctx.fillText(`${String(date.getMonth() + 1).padStart(2, "0")}月${String(date.getDate()).padStart(2, "0")}日  周${weekday(date)}`, 0, 0);
    } else if (widget.type === "steps") {
      ctx.shadowBlur = 2;
      ctx.font = `700 ${widget.fontSize}px Inter, sans-serif`;
      ctx.textAlign = "left";
      ctx.fillText("◌  6,842", 0, 0);
      ctx.font = "500 9px Inter, sans-serif";
      ctx.globalAlpha = .66;
      ctx.fillText("STEPS", 2, 18);
    } else if (widget.type === "heart") {
      ctx.shadowBlur = 2;
      ctx.font = `700 ${widget.fontSize}px Inter, sans-serif`;
      ctx.textAlign = "right";
      ctx.fillText("♥  78", 0, 0);
      ctx.font = "500 9px Inter, sans-serif";
      ctx.globalAlpha = .66;
      ctx.fillText("BPM", -2, 18);
    } else if (widget.type === "text") {
      ctx.shadowBlur = 2;
      ctx.font = `700 ${widget.fontSize}px Inter, sans-serif`;
      ctx.globalAlpha = .7;
      ctx.fillText(widget.value || "FOCUS / 01", 0, 0);
    }
    ctx.restore();
  }

  function drawPlanCard(ctx, state) {
    const plan = state.plan;
    const primary = state.palette.primary;
    const accent = state.palette.accent;
    ctx.fillStyle = state.background.color;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);
    const image = getImage(state.background.imageDataUrl);
    if (image && image.complete && image.naturalWidth) {
      ctx.save();
      ctx.globalAlpha = .2;
      drawCover(ctx, image);
      ctx.restore();
    }
    ctx.fillStyle = "rgba(5, 8, 12, .48)";
    ctx.fillRect(0, 0, WIDTH, HEIGHT);
    ctx.fillStyle = accent;
    ctx.fillRect(24, 26, 5, 284);
    ctx.fillStyle = primary;
    ctx.textAlign = "left";
    ctx.textBaseline = "alphabetic";
    ctx.font = "700 13px Inter, -apple-system, BlinkMacSystemFont, sans-serif";
    ctx.globalAlpha = .65;
    ctx.fillText("TODAY / TRAINING PLAN", 49, 43);
    ctx.globalAlpha = 1;
    ctx.font = "700 35px Inter, -apple-system, BlinkMacSystemFont, sans-serif";
    ctx.fillText(plan.date.replaceAll("-", "."), 48, 84);
    ctx.font = "700 31px Inter, -apple-system, BlinkMacSystemFont, \"PingFang SC\", sans-serif";
    ctx.fillText(plan.title || "运动计划", 48, 132);
    ctx.fillStyle = accent;
    ctx.font = "700 15px Inter, -apple-system, BlinkMacSystemFont, sans-serif";
    ctx.fillText(`${plan.duration} MIN`, 48, 161);
    ctx.strokeStyle = "rgba(255,255,255,.18)";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(48, 185);
    ctx.lineTo(432, 185);
    ctx.stroke();
    ctx.fillStyle = primary;
    ctx.font = "600 12px Inter, -apple-system, BlinkMacSystemFont, \"PingFang SC\", sans-serif";
    ctx.fillText("目标", 48, 211);
    ctx.font = "500 18px Inter, -apple-system, BlinkMacSystemFont, \"PingFang SC\", sans-serif";
    const target = plan.target || "未设置目标";
    const targetLine = target.length > 24 ? `${target.slice(0, 24)}…` : target;
    ctx.fillText(targetLine, 48, 238);
    ctx.fillStyle = primary;
    ctx.globalAlpha = .62;
    ctx.font = "500 12px Inter, -apple-system, BlinkMacSystemFont, \"PingFang SC\", sans-serif";
    const note = plan.note || "准备好后开始";
    ctx.fillText(note.length > 38 ? `${note.slice(0, 38)}…` : note, 48, 267);
    ctx.globalAlpha = 1;
    ctx.textAlign = "right";
    ctx.fillStyle = plan.completed ? accent : primary;
    ctx.font = "700 12px Inter, -apple-system, BlinkMacSystemFont, sans-serif";
    ctx.fillText(plan.completed ? "✓ DONE" : "○ READY", 432, 306);
  }

  function renderWatchFace(ctx, incomingState) {
    const state = normalizeState(incomingState);
    ctx.clearRect(0, 0, WIDTH, HEIGHT);
    if (state.mode === "plan") {
      drawPlanCard(ctx, state);
      return;
    }
    ctx.fillStyle = state.background.color;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);
    const image = getImage(state.background.imageDataUrl);
    if (image && image.complete) {
      ctx.save();
      ctx.globalAlpha = .94;
      drawCover(ctx, image);
      ctx.restore();
      ctx.fillStyle = "rgba(4, 7, 10, .26)";
      ctx.fillRect(0, 0, WIDTH, HEIGHT);
    }
    const time = findWidget(state, "time");
    if (time && time.visible) {
      ctx.save();
      ctx.strokeStyle = state.palette.accent;
      ctx.globalAlpha = .85;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(240, 122, 67, -Math.PI * .85, -Math.PI * .2);
      ctx.stroke();
      ctx.restore();
    }
    state.widgets.forEach((widget) => drawWidget(ctx, state, widget));
    ctx.save();
    ctx.fillStyle = state.palette.primary;
    ctx.globalAlpha = .16;
    ctx.fillRect(18, 18, 2, 2);
    ctx.fillRect(WIDTH - 20, 18, 2, 2);
    ctx.restore();
  }

  window.XiaomiWatchFace = { WIDTH, HEIGHT, DEFAULT_STATE: clone(DEFAULT_STATE), normalizeState, validateProject, serializeProject, renderWatchFace };
})();
