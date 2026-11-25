// Layout document: { schema: 1, rotation: {enabled, pauseAfterTouch}, pages: [{id, duration, widgets: [...]}] }.
// A widget is { id, type, x, y, w, h, settings } on a 12 x 8 grid. See docs/PLAN.md, section 5.

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

  const rotation = Object.assign({ enabled: true, pauseAfterTouch: 60 }, doc.rotation);
  const pages = [];
  for (const page of doc.pages) {
    if (!page || typeof page !== 'object') return fail('a page is not an object');
    const widgets = Array.isArray(page.widgets) ? page.widgets : [];
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
  return { layout: Object.assign({}, doc, { rotation, pages }), error: null };
}
