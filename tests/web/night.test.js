import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseTime, isNight, burnInOffset } from '../../web/panel/night.js';
import { parseLayout } from '../../web/panel/layout.js';

const at = (h, m) => new Date(2026, 10, 26, h, m);

test('parseTime', () => {
  assert.equal(parseTime('00:00'), 0);
  assert.equal(parseTime('08:30'), 510);
  assert.equal(parseTime('23:59'), 1439);
  for (const bad of ['24:00', '25:00', '8:00', '08:60', 'x', '', null, 480]) assert.equal(parseTime(bad), null, String(bad));
});

test('night window that crosses midnight', () => {
  assert.equal(isNight(at(23, 0), '22:00', '07:00'), true);
  assert.equal(isNight(at(3, 0), '22:00', '07:00'), true);
  assert.equal(isNight(at(7, 0), '22:00', '07:00'), false);
  assert.equal(isNight(at(12, 0), '22:00', '07:00'), false);
  assert.equal(isNight(at(22, 0), '22:00', '07:00'), true);
});

test('night window within a day', () => {
  assert.equal(isNight(at(13, 30), '13:00', '14:00'), true);
  assert.equal(isNight(at(14, 0), '13:00', '14:00'), false);
  assert.equal(isNight(at(12, 59), '13:00', '14:00'), false);
});

test('equal or invalid times mean no night', () => {
  assert.equal(isNight(at(3, 0), '08:00', '08:00'), false);
  assert.equal(isNight(at(3, 0), '25:00', '08:00'), false);
});

test('burn-in offsets stay within 4 px and cycle', () => {
  const seen = new Set();
  for (let i = 0; i < 16; i++) {
    const o = burnInOffset(i);
    assert.ok(Math.abs(o.x) <= 4 && Math.abs(o.y) <= 4);
    seen.add(o.x + ',' + o.y);
  }
  assert.ok(seen.size > 1);
  assert.deepEqual(burnInOffset(0), burnInOffset(8));
});

const base = (settings) => JSON.stringify({ schema: 1, settings, pages: [{ id: 'p', widgets: [{ id: 'c', type: 'clock', x: 0, y: 0, w: 1, h: 1 }] }] });

test('settings defaults', () => {
  const s = parseLayout(base(undefined)).layout.settings;
  assert.deepEqual(s, { night: { enabled: false, from: '00:00', to: '08:00', mode: 'dim' }, burnInGuard: true });
});

test('valid settings are kept', () => {
  const s = parseLayout(base({ night: { enabled: true, from: '22:30', to: '06:45', mode: 'off' }, burnInGuard: false })).layout.settings;
  assert.deepEqual(s, { night: { enabled: true, from: '22:30', to: '06:45', mode: 'off' }, burnInGuard: false });
});

test('malformed settings fall back to defaults without rejecting the layout', () => {
  const r = parseLayout(base({ night: { enabled: 'yes', from: '25:00', to: 7, mode: 'nap' }, burnInGuard: 'no' }));
  assert.equal(r.error, null);
  assert.deepEqual(r.layout.settings, { night: { enabled: false, from: '00:00', to: '08:00', mode: 'dim' }, burnInGuard: true });
  assert.equal(parseLayout(base({ night: 'yes' })).layout.settings.night.enabled, false);
});
