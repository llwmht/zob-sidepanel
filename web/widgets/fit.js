// Scales a widget's base font size with its cell, so the same widget reads well at any size.
export function fitText(cell, el, ratio) {
  const apply = () => { el.style.fontSize = Math.max(8, Math.min(cell.clientHeight, cell.clientWidth * 0.6) * ratio) + 'px'; };
  apply();
  const ro = new ResizeObserver(apply);
  ro.observe(cell);
  return () => ro.disconnect();
}
