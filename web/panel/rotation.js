// Page rotation: each page shows for its own duration; a touch pauses automatic advance.

export function createRotation({ pages, enabled, pauseAfterTouch, now }) {
  const count = pages.length;
  let index = 0;
  let pageStart = now();
  let pausedUntil = null;

  const go = (i) => {
    index = ((i % count) + count) % count;
    pageStart = now();
    return index;
  };

  return {
    current: () => index,
    next: () => go(index + 1),
    prev: () => go(index - 1),
    touch() {
      pausedUntil = now() + pauseAfterTouch * 1000;
    },
    /** Advances when the current page has run its duration. Returns true when the page changed. */
    tick() {
      const t = now();
      if (pausedUntil !== null) {
        if (t < pausedUntil) return false;
        pageStart = pausedUntil; // the page runs its full duration again after a pause
        pausedUntil = null;
      }
      if (!enabled || count < 2) return false;
      if (t - pageStart < pages[index] * 1000) return false;
      go(index + 1);
      return true;
    },
  };
}
