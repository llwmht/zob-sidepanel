import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync, existsSync, statSync } from 'node:fs';
import { isValidWidgetType } from '../../web/panel/widget-type.js';

const root = new URL('../../web/widgets/', import.meta.url);
const folders = existsSync(root) ? readdirSync(root).filter((n) => statSync(new URL(n, root)).isDirectory()) : [];

test('at least one widget exists', () => {
  assert.ok(folders.length > 0);
});

for (const name of folders) {
  test('widget ' + name + ' follows the contract', () => {
    assert.ok(isValidWidgetType(name), 'folder name is a valid widget type');
    const manifest = JSON.parse(readFileSync(new URL(name + '/manifest.json', root), 'utf8'));
    assert.equal(typeof manifest.name, 'string');
    assert.ok(Number.isInteger(manifest.defaultSize.w) && Number.isInteger(manifest.defaultSize.h));
    assert.equal(typeof manifest.settings, 'object');
    for (const [key, s] of Object.entries(manifest.settings)) {
      assert.ok(['boolean', 'string', 'number'].includes(s.type), key + ' has a known type');
      assert.equal(typeof s.label, 'string', key + ' has a label');
    }
    assert.ok(existsSync(new URL(name + '/widget.js', root)), 'widget.js exists');
  });
}
