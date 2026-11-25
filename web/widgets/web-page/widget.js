// Shows any web page that allows being framed. The frame cannot navigate the panel or open popups,
// and the native bridge is never injected into it (other origin).

export function mount(cell, ctx) {
  const url = typeof ctx.settings.url === 'string' ? ctx.settings.url.trim() : '';
  if (!/^https?:\/\//i.test(url)) {
    const msg = document.createElement('div');
    msg.className = 'w w-muted';
    msg.textContent = 'Set an http(s) URL';
    cell.appendChild(msg);
    return () => {};
  }

  const frame = document.createElement('iframe');
  frame.src = url;
  frame.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-forms');
  frame.setAttribute('referrerpolicy', 'no-referrer');
  frame.style.cssText = 'border:0;width:100%;height:100%;display:block;background:#000';
  cell.appendChild(frame);

  const minutes = Number(ctx.settings.reloadMinutes) || 0;
  const timer = minutes > 0 ? setInterval(() => { frame.src = url; }, minutes * 60000) : null;
  return () => { if (timer) clearInterval(timer); };
}
