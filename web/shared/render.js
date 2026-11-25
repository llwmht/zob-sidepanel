// Renders a page of widgets into a grid element. Shared by the panel and the editor's preview.
import { isValidWidgetType } from '../panel/widget-type.js';

export function cellError(cell, text) {
  cell.textContent = '';
  const div = document.createElement('div');
  div.className = 'cell-error';
  div.textContent = text;
  cell.appendChild(div);
}

/** Mounts one widget; resolves to its unmount function. A failure only affects this cell. */
export async function mountWidget(cell, widget, ctx) {
  if (!isValidWidgetType(widget.type)) {
    cellError(cell, 'Unknown widget type: ' + widget.type);
    return () => {};
  }
  try {
    const mod = await import('../widgets/' + widget.type + '/widget.js');
    const unmount = await mod.mount(cell, ctx);
    return typeof unmount === 'function' ? unmount : () => {};
  } catch (e) {
    cellError(cell, 'Widget ' + widget.type + ' failed: ' + (e && e.message ? e.message : e));
    return () => {};
  }
}

/** Fills `pageEl` with one cell per widget. Returns a function that unmounts every widget and empties it. */
export function renderPage(pageEl, page, makeCtx) {
  const mounted = page.widgets.map((w) => {
    const cell = document.createElement('div');
    cell.className = 'cell';
    cell.dataset.widgetId = w.id;
    cell.style.gridColumn = (w.x + 1) + ' / span ' + w.w;
    cell.style.gridRow = (w.y + 1) + ' / span ' + w.h;
    pageEl.appendChild(cell);
    return mountWidget(cell, w, makeCtx(w));
  });
  return () => {
    for (const p of mounted) p.then((unmount) => { try { unmount(); } catch (e) { /* a widget's cleanup must not stop the others */ } });
    pageEl.textContent = '';
  };
}
