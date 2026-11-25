// Pure layout edits used by the editor. Grid limits come from the panel's layout model.
import { GRID } from '../panel/layout.js';

const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

/** A copy of the widget moved to (x, y), kept inside the grid. */
export function moveWidget(w, x, y) {
  return Object.assign({}, w, {
    x: clamp(Math.round(x), 0, GRID.cols - w.w),
    y: clamp(Math.round(y), 0, GRID.rows - w.h),
  });
}

/** A copy of the widget with a new size, at least 1x1 and inside the grid from its position. */
export function resizeWidget(w, width, height) {
  return Object.assign({}, w, {
    w: clamp(Math.round(width), 1, GRID.cols - w.x),
    h: clamp(Math.round(height), 1, GRID.rows - w.y),
  });
}

const overlaps = (a, b) => a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;

/** First top-left position (row by row) where a w x h widget overlaps nothing; the corner if none. */
export function findFreeSpot(page, w, h) {
  for (let y = 0; y + h <= GRID.rows; y++) {
    for (let x = 0; x + w <= GRID.cols; x++) {
      const box = { x, y, w, h };
      if (!page.widgets.some((o) => overlaps(box, o))) return { x, y };
    }
  }
  return { x: 0, y: 0 };
}

function uniqueId(prefix, taken) {
  let n = 1;
  while (taken.has(prefix + '-' + n)) n++;
  return prefix + '-' + n;
}

/** Adds a widget of `type` at the first free spot and returns it. */
export function addWidget(page, type, defaultSize) {
  const w = clamp(defaultSize && defaultSize.w || 4, 1, GRID.cols);
  const h = clamp(defaultSize && defaultSize.h || 2, 1, GRID.rows);
  const spot = findFreeSpot(page, w, h);
  const widget = { id: uniqueId(type, new Set(page.widgets.map((o) => o.id))), type, x: spot.x, y: spot.y, w, h, settings: {} };
  page.widgets.push(widget);
  return widget;
}

export function removeWidget(page, id) {
  page.widgets = page.widgets.filter((o) => o.id !== id);
}

export function addPage(layout) {
  const page = { id: uniqueId('page', new Set(layout.pages.map((p) => p.id))), duration: 30, widgets: [] };
  layout.pages.push(page);
  return page;
}

/** Removes page i unless it is the last one. Returns whether it removed. */
export function removePage(layout, i) {
  if (layout.pages.length < 2) return false;
  layout.pages.splice(i, 1);
  return true;
}

/** Moves page i one step (dir -1 or +1) if it can. */
export function movePage(layout, i, dir) {
  const j = i + dir;
  if (j < 0 || j >= layout.pages.length) return;
  const [p] = layout.pages.splice(i, 1);
  layout.pages.splice(j, 0, p);
}

/** Turns a form value into the type the widget manifest declares. */
export function coerceSetting(spec, raw) {
  if (spec.type === 'boolean') return raw === true || raw === 'on' || raw === 'true';
  if (spec.type === 'number') {
    const n = typeof raw === 'number' ? raw : parseFloat(String(raw).trim());
    return isFinite(n) ? n : (spec.default === undefined ? null : spec.default);
  }
  return String(raw === undefined || raw === null ? '' : raw).trim();
}
