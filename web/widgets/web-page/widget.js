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

  const reload = () => { frame.src = url; };
  const minutes = Number(ctx.settings.reloadMinutes) || 0;
  const timer = minutes > 0 ? setInterval(reload, minutes * 60000) : null;
  // A load that failed while offline leaves an error page in the frame: reload when the network is back.
  const onOnline = () => setTimeout(reload, 5000); // give DNS and routes a moment
  window.addEventListener('online', onOnline);
  return () => { if (timer) clearInterval(timer); window.removeEventListener('online', onOnline); };
}
