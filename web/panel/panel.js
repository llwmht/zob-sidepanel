// Panel runtime: loads the layout, mounts widgets on the grid, rotates and swipes between pages.
import { bridge } from '../shared/bridge.js';
import { parseLayout } from './layout.js';
import { createRotation } from './rotation.js';
import { renderPage } from '../shared/render.js';

const BANNER_MS = 30000;
const SWIPE_PX = 60;

function showBanner(text) {
  const b = document.getElementById('banner');
  b.textContent = text;
  b.hidden = false;
  setTimeout(() => { b.hidden = true; }, BANNER_MS);
}

async function loadLayout() {
  let stored = null;
  try { stored = await bridge.call('getLayout'); } catch (e) { /* outside the app or no bridge: use the default */ }
  if (typeof stored === 'string') {
    const parsed = parseLayout(stored);
    if (parsed.layout) return parsed.layout;
    showBanner('Layout ignored: ' + parsed.error + '. Showing the default layout.');
  }
  const fallback = parseLayout(await (await fetch('default-layout.json')).text());
  return fallback.layout;
}

function render(layout) {
  const root = document.getElementById('pages');
  const makeCtx = (w) => ({ settings: w.settings, fetch: (url, opts) => fetch(url, opts), bridge });
  return layout.pages.map((page) => {
    const el = document.createElement('section');
    el.className = 'page';
    renderPage(el, page, makeCtx);
    root.appendChild(el);
    return el;
  });
}

function show(pageEls, index) {
  pageEls.forEach((el, i) => el.classList.toggle('current', i === index));
}

function wireSwipes(rotation, pageEls) {
  for (const edge of document.querySelectorAll('.edge')) {
    let startX = null;
    edge.addEventListener('pointerdown', (e) => { startX = e.clientX; edge.setPointerCapture(e.pointerId); });
    edge.addEventListener('pointerup', (e) => {
      if (startX === null) return;
      const dx = e.clientX - startX;
      startX = null;
      if (Math.abs(dx) < SWIPE_PX) return;
      show(pageEls, dx < 0 ? rotation.next() : rotation.prev());
      rotation.touch();
    });
  }
}

async function start() {
  const layout = await loadLayout();
  const pageEls = render(layout);
  const rotation = createRotation({
    pages: layout.pages.map((p) => p.duration),
    enabled: layout.rotation.enabled,
    pauseAfterTouch: layout.rotation.pauseAfterTouch,
    now: () => Date.now(),
  });
  show(pageEls, rotation.current());
  document.addEventListener('pointerdown', () => rotation.touch());
  // Sent by the app for every touch, including touches inside framed pages that the panel cannot see.
  window.addEventListener('sidepanel:touch', () => rotation.touch());
  wireSwipes(rotation, pageEls);
  setInterval(() => { if (rotation.tick()) show(pageEls, rotation.current()); }, 1000);
}

start().catch((e) => showBanner('Panel failed to start: ' + (e && e.message ? e.message : e)));
