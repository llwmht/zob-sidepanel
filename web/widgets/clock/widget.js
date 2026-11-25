import { fitText } from '../fit.js';

const pad = (n) => String(n).padStart(2, '0');

export function mount(cell, ctx) {
  const seconds = ctx.settings.seconds === true;
  const showDate = ctx.settings.date !== false;

  const root = document.createElement('div');
  root.className = 'w';
  root.style.alignItems = 'center';
  const time = document.createElement('div');
  time.className = 'w-big';
  const date = document.createElement('div');
  date.className = 'w-muted';
  date.style.fontSize = '0.18em';
  date.style.marginTop = '0.4em';
  root.append(time, date);
  cell.appendChild(root);

  const tick = () => {
    const now = new Date();
    time.textContent = pad(now.getHours()) + ':' + pad(now.getMinutes()) + (seconds ? ':' + pad(now.getSeconds()) : '');
    date.textContent = showDate ? now.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' }) : '';
  };
  tick();
  const timer = setInterval(tick, 1000);
  const unfit = fitText(cell, root, seconds ? 0.32 : 0.42);
  return () => { clearInterval(timer); unfit(); };
}
