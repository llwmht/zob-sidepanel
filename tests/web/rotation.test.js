import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createRotation } from '../../web/panel/rotation.js';

function setup(pages, opts = {}) {
  let t = 0;
  const r = createRotation({ pages, enabled: true, pauseAfterTouch: 60, now: () => t, ...opts });
  return { r, at: (s) => { t = s * 1000; } };
}

test('advances after the page duration, not before', () => {
  const { r, at } = setup([10, 20]);
  at(9.9); assert.equal(r.tick(), false); assert.equal(r.current(), 0);
  at(10); assert.equal(r.tick(), true); assert.equal(r.current(), 1);
});

test('uses each page\'s own duration and wraps', () => {
  const { r, at } = setup([10, 20]);
  at(10); r.tick();
  at(29); r.tick(); assert.equal(r.current(), 1);
  at(30); r.tick(); assert.equal(r.current(), 0);
});

test('a single page never changes', () => {
  const { r, at } = setup([5]);
  at(100); assert.equal(r.tick(), false); assert.equal(r.current(), 0);
});

test('disabled rotation never auto-advances but next() works', () => {
  const { r, at } = setup([5, 5], { enabled: false });
  at(100); assert.equal(r.tick(), false);
  assert.equal(r.next(), 1);
});

test('touch pauses, then the page runs its full duration', () => {
  const { r, at } = setup([10, 10]);
  at(5); r.touch();
  at(64); assert.equal(r.tick(), false);
  at(65); assert.equal(r.tick(), false); // pause just ended: page timer restarts here
  at(74); assert.equal(r.tick(), false);
  at(75); assert.equal(r.tick(), true); assert.equal(r.current(), 1);
});

test('next and prev wrap and restart the page timer', () => {
  const { r, at } = setup([10, 10, 10]);
  assert.equal(r.prev(), 2);
  at(3); assert.equal(r.next(), 0);
  at(12); assert.equal(r.tick(), false);
  at(13); assert.equal(r.tick(), true);
});
