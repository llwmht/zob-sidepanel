package io.github.llwmht.sidepanel

/** Persists one flag. The Android store must write synchronously so the flag survives a sudden death. */
interface FlagStore {
    var pending: Boolean
}

/**
 * Safe mode when the previous start never became healthy. On Android 7 the WebView runs in the app
 * process, so renderer crashes and OOM kills never reach a crash handler; a "not yet healthy" flag
 * catches them all. Safe mode stays until reset().
 */
class StartGuard(private val store: FlagStore) {
    /** True when the current start set the flag (a normal start), false in safe mode. */
    private var ownsPending = false

    /** Returns true when this start must be in safe mode. */
    fun begin(): Boolean {
        if (store.pending) {
            ownsPending = false
            return true
        }
        store.pending = true
        ownsPending = true
        return false
    }

    /**
     * The activity is being destroyed in an orderly way (recreate, config change) before it was
     * confirmed healthy. That is not a failed start; safe mode, if active, stays.
     */
    fun stopOrderly() {
        if (ownsPending) store.pending = false
        ownsPending = false
    }

    fun confirmHealthy() {
        store.pending = false
    }

    fun reset() {
        store.pending = false
    }
}
