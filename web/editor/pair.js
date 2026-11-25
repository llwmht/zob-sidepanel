const form = document.getElementById('pair-form');
const error = document.getElementById('pair-error');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  error.hidden = true;
  const code = document.getElementById('code').value.trim();
  try {
    const res = await fetch('/api/pair', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Sidepanel': '1' },
      body: JSON.stringify({ code }),
    });
    if (res.ok) { location.href = 'index.html'; return; }
    const body = await res.json().catch(() => ({}));
    error.textContent = body.error || 'Pairing failed (' + res.status + ')';
  } catch (err) {
    error.textContent = 'Cannot reach the device: ' + err.message;
  }
  error.hidden = false;
});
