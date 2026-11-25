# Native bridge

Web code in the panel talks to the Android shell through a message channel injected by
`WebViewCompat.addWebMessageListener` under the name `sidepanelNative`. It is injected only into
frames whose origin is the panel's own (`http://127.0.0.1:8770`), so pages shown inside a widget
can never reach it. Use it through `web/shared/bridge.js`:

```js
import { bridge } from '../shared/bridge.js';
const info = await bridge.call('info');
```

Outside the app (a desktop browser) every call rejects with `bridge unavailable`.

## Wire format

Request, as a JSON string:

```json
{"id": 1, "method": "setBrightness", "params": {"level": 128}}
```

`params` is optional and defaults to `{}`. Reply:

```json
{"id": 1, "result": null}
{"id": 1, "error": "unknown method: zzz"}
```

A message that is not JSON or has no `id` or `method` gets `{"id": null, "error": "invalid message"}`.
`bridge.js` rejects a call with no reply after 5 seconds (`bridge timeout: <method>`).

## Methods

| method | params | result |
|---|---|---|
| `info` | – | `{appVersion: string, webviewMajor: number, sdk: number}` |
| `battery` | – | `{level: number 0–100, charging: boolean}` |
| `setBrightness` | `{level: number}` (−1 = system default, 0–255) | null |
| `setKeepAwake` | `{on: boolean}` | null |
| `listApps` | – | `[{label: string, package: string}]`, sorted by label, this app excluded |
| `launchApp` | `{package: string}` | boolean (false when the package has no launch intent) |
| `openDrawer` | – | null |
| `getLayout` | – | string: the unsaved preview from the editor if there is one, else `layout.json` from app storage; null when there is none |
