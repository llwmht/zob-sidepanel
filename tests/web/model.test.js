import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  moveWidget, resizeWidget, findFreeSpot, addWidget, removeWidget,
  addPage, removePage, movePage, coerceSetting,
} from '../../web/editor/model.js';

const w = (over = {}) => ({ id: 'a', type: 'clock', x: 2, y: 2, w: 4, h: 3, settings: {}, ...over });

test('move clamps into the grid on every edge', () => {
  assert.deepEqual([moveWidget(w(), -3, -1).x, moveWidget(w(), -3, -1).y], [0, 0]);
  assert.deepEqual([moveWidget(w(), 20, 20).x, moveWidget(w(), 20, 20).y], [8, 5]);
  assert.deepEqual([moveWidget(w(), 5, 1).x, moveWidget(w(), 5, 1).y], [5, 1]);
});

test('move rounds fractional positions', () => {
  const m = moveWidget(w(), 3.6, 1.2);
  assert.deepEqual([m.x, m.y], [4, 1]);
});

test('resize keeps at least 1x1 and stays inside the grid', () => {
  assert.deepEqual([resizeWidget(w(), 0, -2).w, resizeWidget(w(), 0, -2).h], [1, 1]);
  assert.deepEqual([resizeWidget(w(), 50, 50).w, resizeWidget(w(), 50, 50).h], [10, 6]);
});

test('move and resize do not mutate the input', () => {
  const orig = w();
  moveWidget(orig, 0, 0); resizeWidget(orig, 1, 1);
  assert.deepEqual(orig, w());
});

test('findFreeSpot finds the first free top-left position', () => {
  const page = { widgets: [w({ x: 0, y: 0, w: 6, h: 8 })] };
  assert.deepEqual(findFreeSpot(page, 3, 2), { x: 6, y: 0 });
  assert.deepEqual(findFreeSpot({ widgets: [] }, 3, 2), { x: 0, y: 0 });
});

test('findFreeSpot falls back to the corner when the page is full', () => {
  const page = { widgets: [w({ x: 0, y: 0, w: 12, h: 8 })] };
  assert.deepEqual(findFreeSpot(page, 2, 2), { x: 0, y: 0 });
});

test('addWidget gives unique ids and clamps the default size', () => {
  const page = { widgets: [] };
  const a = addWidget(page, 'clock', { w: 20, h: 3 });
  const b = addWidget(page, 'clock', { w: 2, h: 2 });
  assert.equal(page.widgets.length, 2);
  assert.notEqual(a.id, b.id);
  assert.equal(a.w, 12);
  assert.deepEqual(a.settings, {});
});

test('removeWidget removes by id', () => {
  const page = { widgets: [w({ id: 'a' }), w({ id: 'b' })] };
  removeWidget(page, 'a');
  assert.deepEqual(page.widgets.map((x) => x.id), ['b']);
});

test('pages: add, never remove the last, move', () => {
  const layout = { pages: [{ id: 'p1', duration: 30, widgets: [] }] };
  assert.equal(removePage(layout, 0), false);
  const p = addPage(layout);
  assert.equal(layout.pages.length, 2);
  assert.notEqual(p.id, 'p1');
  movePage(layout, 1, -1);
  assert.equal(layout.pages[0].id, p.id);
  movePage(layout, 0, -1); // already first: no change
  assert.equal(layout.pages[0].id, p.id);
  assert.equal(removePage(layout, 0), true);
  assert.deepEqual(layout.pages.map((x) => x.id), ['p1']);
});

test('coerceSetting follows the manifest type', () => {
  assert.equal(coerceSetting({ type: 'boolean' }, true), true);
  assert.equal(coerceSetting({ type: 'boolean' }, 'on'), true);
  assert.equal(coerceSetting({ type: 'boolean' }, ''), false);
  assert.equal(coerceSetting({ type: 'number', default: 5 }, '12.75'), 12.75);
  assert.equal(coerceSetting({ type: 'number', default: 5 }, 'abc'), 5);
  assert.equal(coerceSetting({ type: 'number', default: null }, ''), null);
  assert.equal(coerceSetting({ type: 'string' }, '  hi '), 'hi');
});
