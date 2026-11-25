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

    /** Returns true when this start must be in safe mode. */
    fun begin(): Boolean {
        if (store.pending) return true
        store.pending = true
        return false
    }

    fun confirmHealthy() {
        store.pending = false
    }

    fun reset() {
        store.pending = false
    }
}
