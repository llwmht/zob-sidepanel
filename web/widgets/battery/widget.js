import { fitText } from '../fit.js';

const REFRESH_MS = 60000;

export function mount(cell, ctx) {
  const root = document.createElement('div');
  root.className = 'w';
  const line = document.createElement('div');
  line.style.fontSize = '0.4em';
  line.style.whiteSpace = 'nowrap';
  root.appendChild(line);
  cell.appendChild(root);

  const refresh = async () => {
    try {
      const b = await ctx.bridge.call('battery');
      line.textContent = 'Battery ' + b.level + '%' + (b.charging ? ' · charging' : '');
      line.className = '';
    } catch (e) {
      line.textContent = 'Battery unavailable';
      line.className = 'w-muted';
    }
  };
  refresh();
  const timer = setInterval(refresh, REFRESH_MS);
  const unfit = fitText(cell, root, 0.22);
  return () => { clearInterval(timer); unfit(); };
}
