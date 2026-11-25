// Current weather and a 3-day outlook from Open-Meteo (no key needed).
import { fitText } from '../fit.js';
import { describeCode } from './weather-codes.js';

const REFRESH_MS = 15 * 60 * 1000;
const RETRY_MS = 2 * 60 * 1000;

const deg = (v) => Math.round(v) + '°';

function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}

export function mount(cell, ctx) {
  const { latitude, longitude, label } = ctx.settings;
  const root = el('div', 'w');
  cell.appendChild(root);
  const unfit = fitText(cell, root, 0.14);

  if (typeof latitude !== 'number' || typeof longitude !== 'number') {
    root.appendChild(el('div', 'w-muted', 'Set a location'));
    return unfit;
  }

  const url = 'https://api.open-meteo.com/v1/forecast?latitude=' + latitude + '&longitude=' + longitude +
    '&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min' +
    '&forecast_days=4&timezone=auto';
  let timer = null;

  const render = (data) => {
    const now = describeCode(data.current.weather_code);
    const d = data.daily;
    root.textContent = '';
    if (label) root.appendChild(el('div', 'w-muted', label));
    const top = el('div');
    top.appendChild(el('span', 'w-big', deg(data.current.temperature_2m)));
    top.lastChild.style.fontSize = '2.6em';
    top.appendChild(el('span', '', ' ' + now.icon));
    top.lastChild.style.fontSize = '1.8em';
    root.appendChild(top);
    root.appendChild(el('div', '', now.text + ' · ' + deg(d.temperature_2m_min[0]) + ' / ' + deg(d.temperature_2m_max[0])));
    const days = el('div', 'w-muted');
    days.style.marginTop = '0.6em';
    for (let i = 1; i < d.time.length; i++) {
      const name = new Date(d.time[i] + 'T12:00').toLocaleDateString(undefined, { weekday: 'short' });
      days.appendChild(el('div', '', name + '  ' + describeCode(d.weather_code[i]).icon + '  ' +
        deg(d.temperature_2m_min[i]) + ' / ' + deg(d.temperature_2m_max[i])));
    }
    root.appendChild(days);
  };

  const refresh = async () => {
    try {
      const res = await ctx.fetch(url);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      render(await res.json());
      timer = setTimeout(refresh, REFRESH_MS);
    } catch (e) {
      root.textContent = '';
      if (label) root.appendChild(el('div', 'w-muted', label));
      root.appendChild(el('div', 'w-muted', 'Weather unavailable'));
      timer = setTimeout(refresh, RETRY_MS);
    }
  };
  refresh();
  return () => { clearTimeout(timer); unfit(); };
}
