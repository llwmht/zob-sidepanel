import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isValidWidgetType } from '../../web/panel/widget-type.js';

test('accepts lower-case names with digits and dashes', () => {
  for (const t of ['clock', 'web-page', 'a', 'a1-b2']) assert.equal(isValidWidgetType(t), true, t);
});

test('rejects anything that could change the import path', () => {
  const bad = ['Clock', '../x', 'a/b', 'a\\b', '', '1abc', '-a', 'a'.repeat(33), null, undefined, 42, '../../shared/bridge'];
  for (const t of bad) assert.equal(isValidWidgetType(t), false, String(t));
});
