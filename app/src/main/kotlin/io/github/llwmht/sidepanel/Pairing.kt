package io.github.llwmht.sidepanel

/** Persists the sessions of paired browsers, each stored as `<token>:<created, epoch ms>`. */
interface TokenStore {
    fun load(): Set<String>
    fun save(tokens: Set<String>)
}

/**
 * Pairing of browsers with a 6-digit code shown on the device. A code exists only while LAN editing is
 * on. After [MAX_WRONG] wrong guesses pairing locks until the owner asks for a new code on the device,
 * so the code cannot be brute-forced. Calls come from server threads, so every method is synchronized.
 */
class Pairing(
    private val random: (Int) -> ByteArray,
    private val store: TokenStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var code: String? = null
    private var wrongTries = 0
    private var locked = false

    /** A fresh code, asked for by the owner on the device: also clears a lock. */
    @Synchronized fun newCode(): String {
        wrongTries = 0
        locked = false
        return rotate()
    }

    private fun rotate(): String {
        val b = random(4)
        val n = ((b[0].toLong() and 0xff) shl 24) or ((b[1].toLong() and 0xff) shl 16) or
            ((b[2].toLong() and 0xff) shl 8) or (b[3].toLong() and 0xff)
        return (n % 1_000_000).toString().padStart(6, '0').also { code = it }
    }

    @Synchronized fun isLocked(): Boolean = locked

    @Synchronized fun currentCode(): String? = code

    /** A new session token for a right code, null otherwise. */
    @Synchronized fun tryPair(guess: String): String? {
        val current = code ?: return null
        if (guess != current) {
            if (++wrongTries >= MAX_WRONG) {
                code = null
                locked = true
            }
            return null
        }
        val token = random(32).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        store.save(liveSessions().map { (t, created) -> "$t:$created" }.toSet() + "$token:${now()}")
        rotate() // a code pairs one browser; the failure budget is not reset
        return token
    }

    /** True for a session paired less than [SESSION_MS] ago. */
    @Synchronized fun isValid(token: String?): Boolean = token != null && liveSessions().any { it.first == token }

    /** Stored sessions that have not expired; malformed entries count as expired. */
    private fun liveSessions(): List<Pair<String, Long>> = store.load().mapNotNull { entry ->
        val created = entry.substringAfter(':', "").toLongOrNull() ?: return@mapNotNull null
        if (now() - created in 0 until SESSION_MS) entry.substringBefore(':') to created else null
    }

    @Synchronized fun forgetAll() = store.save(emptySet())

    @Synchronized fun stop() {
        code = null
        wrongTries = 0
        locked = false
    }

    companion object {
        const val MAX_WRONG = 5
        const val SESSION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
