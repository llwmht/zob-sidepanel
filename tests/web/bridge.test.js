import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createBridge } from '../../web/shared/bridge.js';

function fakePort() {
  const sent = [];
  const port = { postMessage: (m) => sent.push(JSON.parse(m)), onmessage: null };
  const feed = (obj) => port.onmessage({ data: typeof obj === 'string' ? obj : JSON.stringify(obj) });
  return { port, sent, feed };
}

test('resolves with result', async () => {
  const { port, sent, feed } = fakePort();
  const p = createBridge(port).call('a', { x: 1 });
  assert.deepEqual(sent[0], { id: 1, method: 'a', params: { x: 1 } });
  feed({ id: 1, result: 7 });
  assert.equal(await p, 7);
});

test('rejects with error', async () => {
  const { port, feed } = fakePort();
  const p = createBridge(port).call('a');
  feed({ id: 1, error: 'bad' });
  await assert.rejects(p, { message: 'bad' });
});

test('matches replies by id', async () => {
  const { port, feed } = fakePort();
  const b = createBridge(port);
  const p1 = b.call('a');
  const p2 = b.call('b');
  feed({ id: 2, result: 'two' });
  feed({ id: 1, result: 'one' });
  assert.equal(await p1, 'one');
  assert.equal(await p2, 'two');
});

test('ignores unknown ids and junk', async () => {
  const { port, feed } = fakePort();
  const p = createBridge(port).call('a');
  assert.doesNotThrow(() => feed('nope'));
  assert.doesNotThrow(() => feed({ id: 99, result: 1 }));
  let settled = false;
  p.then(() => { settled = true; }, () => { settled = true; });
  await new Promise((r) => setImmediate(r));
  assert.equal(settled, false);
  feed({ id: 1, result: null }); // settle so the test can end
  await p;
});

test('times out', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { port } = fakePort();
  const p = createBridge(port).call('a');
  t.mock.timers.tick(5000);
  await assert.rejects(p, { message: 'bridge timeout: a' });
});
