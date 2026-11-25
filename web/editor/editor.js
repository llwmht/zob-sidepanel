// Browser editor: edits the layout of pages and widgets, previews it on the device and saves it.
import { parseLayout, GRID } from '../panel/layout.js';
import { renderPage } from '../shared/render.js';
import { bridge } from '../shared/bridge.js';
import {
  moveWidget, resizeWidget, addWidget, removeWidget, addPage, removePage, movePage, coerceSetting,
} from './model.js';

const $ = (id) => document.getElementById(id);
const state = { layout: null, page: 0, selected: null, catalog: [], unmount: null };

async function api(method, path, body) {
  const opts = { method, headers: {} };
  if (method !== 'GET') opts.headers['X-Sidepanel'] = '1';
  if (body !== undefined) { opts.headers['Content-Type'] = 'application/json'; opts.body = body; }
  const res = await fetch(path, opts);
  if (res.status === 401) { location.href = 'pair.html'; throw new Error('not paired'); }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || 'HTTP ' + res.status);
  }
  return res.status === 204 ? null : res.json();
}

function status(text, bad) {
  $('status').textContent = text;
  $('status').className = bad ? 'error' : 'muted';
}

const currentPage = () => state.layout.pages[state.page];
const manifestOf = (type) => (state.catalog.find((c) => c.type === type) || {}).manifest || { name: type, settings: {} };

/* ---------- canvas ---------- */

function place(el, w) {
  el.style.gridColumn = (w.x + 1) + ' / span ' + w.w;
  el.style.gridRow = (w.y + 1) + ' / span ' + w.h;
}

function drawPreview() {
  if (state.unmount) state.unmount();
  const preview = $('stage').querySelector('.preview');
  state.unmount = renderPage(preview, currentPage(),
    (w) => ({ settings: w.settings, fetch: (url, opts) => fetch(url, opts), bridge }));
}

function drawOverlay() {
  const overlay = $('stage').querySelector('.overlay');
  overlay.textContent = '';
  for (const w of currentPage().widgets) {
    const box = document.createElement('div');
    box.className = 'box' + (w.id === state.selected ? ' selected' : '');
    box.dataset.id = w.id;
    place(box, w);
    const label = document.createElement('span');
    label.className = 'label';
    label.textContent = manifestOf(w.type).name;
    const grip = document.createElement('div');
    grip.className = 'grip';
    box.append(label, grip);
    overlay.appendChild(box);
    wireDrag(box, w.id, false);
    wireDrag(grip, w.id, true);
  }
}

/** Drag a box to move it, or its grip to resize it; snaps to grid cells. */
function wireDrag(el, id, resizing) {
  el.addEventListener('pointerdown', (e) => {
    e.stopPropagation();
    e.preventDefault();
    select(id);
    el.setPointerCapture(e.pointerId);
    const rect = $('stage').getBoundingClientRect();
    const cellW = rect.width / GRID.cols;
    const cellH = rect.height / GRID.rows;
    const page = currentPage();
    const start = page.widgets.find((w) => w.id === id);
    const x0 = e.clientX;
    const y0 = e.clientY;
    const onMove = (ev) => {
      const dx = (ev.clientX - x0) / cellW;
      const dy = (ev.clientY - y0) / cellH;
      const next = resizing ? resizeWidget(start, start.w + dx, start.h + dy) : moveWidget(start, start.x + dx, start.y + dy);
      page.widgets = page.widgets.map((w) => (w.id === id ? next : w));
      for (const sel of ['.overlay .box.selected', '.preview .cell[data-widget-id="' + CSS.escape(id) + '"]']) {
        const target = $('stage').querySelector(sel);
        if (target) place(target, next);
      }
    };
    // A drag ends on release, and also when the pointer is lost (window switch, cancelled touch).
    const ends = ['pointerup', 'pointercancel', 'lostpointercapture'];
    const onUp = () => {
      el.removeEventListener('pointermove', onMove);
      for (const t of ends) el.removeEventListener(t, onUp);
      drawOverlay();
    };
    el.addEventListener('pointermove', onMove);
    for (const t of ends) el.addEventListener(t, onUp);
  });
}

function drawStage(device) {
  const stage = $('stage');
  stage.textContent = '';
  const main = stage.parentElement;
  const ratio = device.width / device.height;
  const fit = () => {
    const w = Math.min(main.clientWidth - 32, (main.clientHeight - 32) * ratio);
    stage.style.width = w + 'px';
    stage.style.height = (w / ratio) + 'px';
  };
  fit();
  window.addEventListener('resize', fit);
  const guides = document.createElement('div');
  guides.className = 'grid guides';
  for (let i = 0; i < GRID.cols * GRID.rows; i++) guides.appendChild(document.createElement('div'));
  const preview = document.createElement('div');
  preview.className = 'grid preview';
  const overlay = document.createElement('div');
  overlay.className = 'grid overlay';
  overlay.addEventListener('pointerdown', () => select(null));
  stage.append(guides, preview, overlay);
}

/* ---------- side panels ---------- */

function drawTabs() {
  const tabs = $('tabs');
  tabs.textContent = '';
  state.layout.pages.forEach((p, i) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = (i + 1) + '. ' + p.id + ' · ' + p.duration + ' s';
    if (i === state.page) b.className = 'current';
    b.addEventListener('click', () => { state.page = i; state.selected = null; drawAll(); });
    tabs.appendChild(b);
  });
  $('duration').value = currentPage().duration;
  $('rotate').checked = state.layout.rotation.enabled;
  $('pause').value = state.layout.rotation.pauseAfterTouch;
}

function drawPalette() {
  const palette = $('palette');
  palette.textContent = '';
  for (const c of state.catalog) {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = '+ ' + c.manifest.name;
    b.addEventListener('click', () => {
      const w = addWidget(currentPage(), c.type, c.manifest.defaultSize);
      for (const [key, spec] of Object.entries(c.manifest.settings || {})) {
        if (spec.default !== undefined && spec.default !== null && spec.default !== '') w.settings[key] = spec.default;
      }
      state.selected = w.id;
      drawAll();
    });
    palette.appendChild(b);
  }
}

function drawInspector() {
  const box = $('inspector');
  box.textContent = '';
  const w = currentPage().widgets.find((x) => x.id === state.selected);
  if (!w) {
    box.innerHTML = '<p class="muted">Select a widget to change its settings.</p>';
    return;
  }
  const m = manifestOf(w.type);
  const title = document.createElement('h2');
  title.textContent = m.name + ' (' + w.id + ')';
  box.appendChild(title);
  for (const [key, spec] of Object.entries(m.settings || {})) {
    const input = document.createElement('input');
    const field = document.createElement('label');
    if (spec.type === 'boolean') {
      input.type = 'checkbox';
      input.checked = w.settings[key] === undefined ? spec.default === true : w.settings[key] === true;
      field.className = 'field check';
      field.append(input, document.createTextNode(' ' + spec.label));
    } else {
      input.type = spec.type === 'number' ? 'number' : 'text';
      if (spec.type === 'number') input.step = 'any';
      const v = w.settings[key] === undefined ? spec.default : w.settings[key];
      input.value = v === null || v === undefined ? '' : v;
      field.className = 'field';
      field.append(document.createTextNode(spec.label), input);
    }
    input.addEventListener('change', () => {
      w.settings[key] = coerceSetting(spec, spec.type === 'boolean' ? input.checked : input.value);
      drawPreview();
    });
    box.appendChild(field);
  }
  const del = document.createElement('button');
  del.type = 'button';
  del.textContent = 'Remove widget';
  del.addEventListener('click', () => { removeWidget(currentPage(), w.id); state.selected = null; drawAll(); });
  box.appendChild(del);
}

/** Changes the selection without rebuilding the boxes (a drag may hold pointer capture on one). */
function select(id) {
  if (state.selected === id) return;
  state.selected = id;
  for (const box of $('stage').querySelectorAll('.overlay .box')) box.classList.toggle('selected', box.dataset.id === id);
  drawInspector();
}

function drawAll() {
  drawTabs();
  drawPreview();
  drawOverlay();
  drawInspector();
}

/* ---------- actions ---------- */

function checked() {
  const parsed = parseLayout(JSON.stringify(state.layout));
  if (parsed.error) { status(parsed.error, true); return null; }
  return JSON.stringify(state.layout, null, 2);
}

async function send(method, path, okText) {
  const body = method === 'DELETE' ? undefined : checked();
  if (body === null) return;
  try { await api(method, path, body); status(okText); } catch (e) { status(e.message, true); }
}

function wireControls() {
  $('add-page').addEventListener('click', () => { addPage(state.layout); state.page = state.layout.pages.length - 1; state.selected = null; drawAll(); });
  $('remove-page').addEventListener('click', () => {
    if (!removePage(state.layout, state.page)) return status('A layout needs at least one page.', true);
    state.page = Math.max(0, state.page - 1);
    state.selected = null;
    drawAll();
  });
  $('page-left').addEventListener('click', () => { movePage(state.layout, state.page, -1); state.page = Math.max(0, state.page - 1); drawTabs(); });
  $('page-right').addEventListener('click', () => { movePage(state.layout, state.page, 1); state.page = Math.min(state.layout.pages.length - 1, state.page + 1); drawTabs(); });
  $('duration').addEventListener('change', (e) => { currentPage().duration = Math.max(5, Number(e.target.value) || 30); drawTabs(); });
  $('rotate').addEventListener('change', (e) => { state.layout.rotation.enabled = e.target.checked; });
  $('pause').addEventListener('change', (e) => { state.layout.rotation.pauseAfterTouch = Math.max(0, Number(e.target.value) || 0); });
  $('preview').addEventListener('click', () => send('PUT', '/api/preview', 'Previewing on the device (not saved).'));
  $('discard').addEventListener('click', () => send('DELETE', '/api/preview', 'Preview discarded.'));
  $('save').addEventListener('click', () => send('PUT', '/api/layout', 'Saved.'));
}

async function start() {
  const [stored, catalog, device] = await Promise.all([api('GET', '/api/layout'), api('GET', '/api/widgets'), api('GET', '/api/device')]);
  state.catalog = catalog;
  let parsed = stored.layout ? parseLayout(stored.layout) : { layout: null, error: null };
  if (!parsed.layout) {
    if (parsed.error) status('Stored layout ignored: ' + parsed.error, true);
    parsed = parseLayout(await (await fetch('../panel/default-layout.json')).text());
  }
  state.layout = parsed.layout;
  drawStage(device);
  drawPalette();
  wireControls();
  drawAll();
}

start().catch((e) => status('Editor failed to start: ' + e.message, true));
