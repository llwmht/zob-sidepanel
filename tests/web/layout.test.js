import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseLayout, GRID } from '../../web/panel/layout.js';

const widget = (over = {}) => ({ id: 'w1', type: 'clock', x: 0, y: 0, w: 4, h: 2, ...over });
const doc = (pages) => JSON.stringify({ schema: 1, pages });

test('grid is 12 by 8', () => {
  assert.deepEqual(GRID, { cols: 12, rows: 8 });
});

test('valid minimal layout gets defaults', () => {
  const { layout, error } = parseLayout(doc([{ id: 'p1', widgets: [widget()] }]));
  assert.equal(error, null);
  assert.equal(layout.pages[0].duration, 30);
  assert.equal(layout.rotation.enabled, true);
  assert.equal(layout.rotation.pauseAfterTouch, 60);
  assert.deepEqual(layout.pages[0].widgets[0].settings, {});
});

test('not JSON', () => {
  assert.deepEqual(parseLayout('{{'), { layout: null, error: 'layout is not valid JSON' });
});

test('wrong schema', () => {
  const { layout, error } = parseLayout(JSON.stringify({ schema: 2, pages: [] }));
  assert.equal(layout, null);
  assert.match(error, /schema/);
});

test('no pages', () => {
  assert.deepEqual(parseLayout(doc([])), { layout: null, error: 'layout has no pages' });
});

test('widget outside the grid names the widget', () => {
  const { layout, error } = parseLayout(doc([{ id: 'p1', widgets: [widget({ id: 'big', x: 10, w: 4 })] }]));
  assert.equal(layout, null);
  assert.match(error, /big/);
});

test('widget below the grid is rejected', () => {
  const { error } = parseLayout(doc([{ id: 'p1', widgets: [widget({ y: 7, h: 2 })] }]));
  assert.match(error, /w1/);
});

test('non-integer size is rejected', () => {
  const { error } = parseLayout(doc([{ id: 'p1', widgets: [widget({ w: 1.5 })] }]));
  assert.match(error, /w1/);
});

test('zero size is rejected', () => {
  const { error } = parseLayout(doc([{ id: 'p1', widgets: [widget({ h: 0 })] }]));
  assert.match(error, /w1/);
});

test('duplicate widget ids in a page are rejected', () => {
  const { error } = parseLayout(doc([{ id: 'p1', widgets: [widget(), widget({ x: 6 })] }]));
  assert.match(error, /duplicate/);
});

test('a page without widgets is rejected', () => {
  assert.match(parseLayout(doc([{}])).error, /no widgets/);
  assert.match(parseLayout(doc([{ id: 'p1', widgets: {} }])).error, /p1 has no widgets/);
  assert.match(parseLayout(doc([{ id: 'p1', widgets: [] }])).error, /p1 has no widgets/);
});

test('rotation settings are checked', () => {
  const page = [{ id: 'p1', widgets: [widget()] }];
  const rot = (rotation) => parseLayout(JSON.stringify({ schema: 1, rotation, pages: page })).layout.rotation;
  assert.deepEqual(rot({ pauseAfterTouch: 'x' }), { enabled: true, pauseAfterTouch: 60 });
  assert.deepEqual(rot({ pauseAfterTouch: -5 }), { enabled: true, pauseAfterTouch: 60 });
  assert.deepEqual(rot({ pauseAfterTouch: 0 }), { enabled: true, pauseAfterTouch: 0 });
  assert.deepEqual(rot({ enabled: 'false' }), { enabled: true, pauseAfterTouch: 60 });
  assert.deepEqual(rot({ enabled: false }), { enabled: false, pauseAfterTouch: 60 });
  assert.deepEqual(rot('nope'), { enabled: true, pauseAfterTouch: 60 });
});

test('short duration is clamped to 5 seconds', () => {
  const { layout } = parseLayout(doc([{ id: 'p1', duration: 2, widgets: [widget()] }]));
  assert.equal(layout.pages[0].duration, 5);
});

test('unknown fields are kept', () => {
  const { layout } = parseLayout(JSON.stringify({ schema: 1, note: 'x', pages: [{ id: 'p1', widgets: [widget({ extra: 1 })] }] }));
  assert.equal(layout.note, 'x');
  assert.equal(layout.pages[0].widgets[0].extra, 1);
});

test('default layout parses', () => {
  const text = readFileSync(new URL('../../web/panel/default-layout.json', import.meta.url), 'utf8');
  assert.equal(parseLayout(text).error, null);
});

test('starter templates parse and have a name', () => {
  for (const name of ['clock', 'clock-weather', 'web-page']) {
    const text = readFileSync(new URL('../../web/panel/templates/' + name + '.json', import.meta.url), 'utf8');
    const r = parseLayout(text);
    assert.equal(r.error, null, name);
    assert.equal(typeof r.layout.name, 'string', name);
  }
});
