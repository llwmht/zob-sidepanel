// Layout document: { schema: 1, rotation: {enabled, pauseAfterTouch}, pages: [{id, duration, widgets: [...]}] }.
// A widget is { id, type, x, y, w, h, settings } on a 12 x 8 grid. See docs/PLAN.md, section 5.

import { parseTime } from './night.js';

export const GRID = { cols: 12, rows: 8 };

const MIN_DURATION = 5;
const isInt = (v) => Number.isInteger(v);

function checkWidget(w) {
  const id = w && typeof w.id === 'string' && w.id ? w.id : '(no id)';
  if (!w || typeof w !== 'object') return 'widget is not an object';
  if (typeof w.id !== 'string' || !w.id) return 'a widget has no id';
  if (typeof w.type !== 'string' || !w.type) return 'widget ' + id + ' has no type';
  if (![w.x, w.y, w.w, w.h].every(isInt)) return 'widget ' + id + ' needs integer x, y, w, h';
  if (w.x < 0 || w.y < 0 || w.w < 1 || w.h < 1) return 'widget ' + id + ' has a negative position or an empty size';
  if (w.x + w.w > GRID.cols || w.y + w.h > GRID.rows) return 'widget ' + id + ' is outside the ' + GRID.cols + 'x' + GRID.rows + ' grid';
  return null;
}

/** Parses and checks a layout. Returns { layout, error }; layout is null whenever error is set. */
export function parseLayout(text) {
  let doc;
  try { doc = JSON.parse(text); } catch (e) { return { layout: null, error: 'layout is not valid JSON' }; }
  const fail = (error) => ({ layout: null, error });

  if (!doc || typeof doc !== 'object') return fail('layout is not an object');
  if (doc.schema !== 1) return fail('unsupported layout schema: ' + doc.schema);
  if (!Array.isArray(doc.pages) || doc.pages.length === 0) return fail('layout has no pages');

  const r = doc.rotation && typeof doc.rotation === 'object' ? doc.rotation : {};
  const pause = r.pauseAfterTouch;
  const rotation = Object.assign({}, r, {
    enabled: r.enabled !== false,
    pauseAfterTouch: typeof pause === 'number' && isFinite(pause) && pause >= 0 ? pause : 60,
  });
  const pages = [];
  for (const page of doc.pages) {
    if (!page || typeof page !== 'object') return fail('a page is not an object');
    const widgets = page.widgets;
    if (!Array.isArray(widgets) || widgets.length === 0) return fail('page ' + (page.id || '(no id)') + ' has no widgets');
    const seen = new Set();
    for (const w of widgets) {
      const problem = checkWidget(w);
      if (problem) return fail(problem);
      if (seen.has(w.id)) return fail('duplicate widget id ' + w.id + ' in page ' + page.id);
      seen.add(w.id);
    }
    const duration = typeof page.duration === 'number' ? Math.max(MIN_DURATION, page.duration) : 30;
    pages.push(Object.assign({}, page, {
      duration,
      widgets: widgets.map((w) => Object.assign({}, w, { settings: w.settings || {} })),
    }));
  }
  return { layout: Object.assign({}, doc, { rotation, pages, settings: parseSettings(doc.settings) }), error: null };
}

/** Device settings; anything malformed falls back to its default instead of rejecting the layout. */
function parseSettings(s) {
  const src = s && typeof s === 'object' ? s : {};
  const n = src.night && typeof src.night === 'object' ? src.night : {};
  const time = (v, d) => (parseTime(v) === null ? d : v);
  return {
    night: {
      enabled: n.enabled === true,
      from: time(n.from, '00:00'),
      to: time(n.to, '08:00'),
      mode: n.mode === 'off' ? 'off' : 'dim',
    },
    burnInGuard: src.burnInGuard !== false,
  };
}
