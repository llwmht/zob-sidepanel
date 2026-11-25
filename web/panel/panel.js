// Placeholder panel for M1: a clock plus a few bridge calls to prove the shell works.
import { bridge } from '../shared/bridge.js';

const $ = (id) => document.getElementById(id);
const pad = (n) => String(n).padStart(2, '0');

function tick() {
  const now = new Date();
  $('clock').textContent = pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
  $('date').textContent = now.toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' });
}
tick();
setInterval(tick, 1000);

async function showDevice() {
  try {
    const [info, battery] = await Promise.all([bridge.call('info'), bridge.call('battery')]);
    $('device').textContent = 'v' + info.appVersion + ' · Chromium ' + info.webviewMajor + ' · Android SDK ' + info.sdk +
      ' · battery ' + battery.level + '%' + (battery.charging ? ' (charging)' : '');
  } catch (e) {
    $('device').textContent = e.message === 'bridge unavailable' ? 'Running outside the app' : 'Bridge error: ' + e.message;
  }
}
showDevice();
setInterval(showDevice, 60000);

const report = (e) => { $('device').textContent = 'Bridge error: ' + e.message; };
$('brightness').addEventListener('input', (e) => bridge.call('setBrightness', { level: Number(e.target.value) }).catch(report));
$('auto').addEventListener('click', () => bridge.call('setBrightness', { level: -1 }).catch(report));
$('apps').addEventListener('click', () => bridge.call('openDrawer').catch(report));
