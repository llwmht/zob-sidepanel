// Promise wrapper over the native message channel. Wire format: docs/bridge.md.

const TIMEOUT_MS = 5000;

export function createBridge(port) {
  let nextId = 1;
  const pending = new Map();

  port.onmessage = (event) => {
    let reply;
    try { reply = JSON.parse(event.data); } catch (e) { return; }
    const call = reply && pending.get(reply.id);
    if (!call) return;
    pending.delete(reply.id);
    clearTimeout(call.timer);
    if ('error' in reply) call.reject(new Error(reply.error));
    else call.resolve(reply.result);
  };

  return {
    call(method, params) {
      const id = nextId++;
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
          pending.delete(id);
          reject(new Error('bridge timeout: ' + method));
        }, TIMEOUT_MS);
        pending.set(id, { resolve, reject, timer });
        const msg = { id, method };
        if (params !== undefined) msg.params = params;
        port.postMessage(JSON.stringify(msg));
      });
    },
  };
}

const unavailable = { call: () => Promise.reject(new Error('bridge unavailable')) };

// `sidepanelNative` is injected by the app only into pages from the panel's own origin.
export const bridge = globalThis.sidepanelNative ? createBridge(globalThis.sidepanelNative) : unavailable;
