// Night schedule and burn-in guard helpers. Times are "HH:MM" in the device's local time.

const TIME = /^([01]\d|2[0-3]):([0-5]\d)$/;

/** Minutes since midnight, or null when not a valid "HH:MM". */
export function parseTime(text) {
  const m = typeof text === 'string' ? TIME.exec(text) : null;
  return m ? Number(m[1]) * 60 + Number(m[2]) : null;
}

/** True when `date` falls in [from, to). The window may cross midnight; equal or invalid times mean no night. */
export function isNight(date, from, to) {
  const a = parseTime(from);
  const b = parseTime(to);
  if (a === null || b === null || a === b) return false;
  const t = date.getHours() * 60 + date.getMinutes();
  return a < b ? t >= a && t < b : t >= a || t < b;
}

// A small closed loop around the origin: every step moves the panel by a few pixels, never more than 4.
const CYCLE = [[0, 0], [2, 1], [4, 2], [2, 4], [0, 3], [-2, 4], [-4, 2], [-2, 1]];

/** Panel offset in pixels for the n-th burn-in step. */
export function burnInOffset(step) {
  const [x, y] = CYCLE[((step % CYCLE.length) + CYCLE.length) % CYCLE.length];
  return { x, y };
}
