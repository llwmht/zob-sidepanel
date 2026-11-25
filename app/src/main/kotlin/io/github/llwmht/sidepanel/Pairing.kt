package io.github.llwmht.sidepanel

/** Persists the session tokens of paired browsers. */
interface TokenStore {
    fun load(): Set<String>
    fun save(tokens: Set<String>)
}

/**
 * Pairing of browsers with a 6-digit code shown on the device. A code exists only while LAN editing is
 * on; five wrong guesses replace it. Calls come from server threads, so every method is synchronized.
 */
class Pairing(private val random: (Int) -> ByteArray, private val store: TokenStore) {
    private var code: String? = null
    private var wrongTries = 0

    @Synchronized fun newCode(): String {
        val b = random(4)
        val n = ((b[0].toLong() and 0xff) shl 24) or ((b[1].toLong() and 0xff) shl 16) or
            ((b[2].toLong() and 0xff) shl 8) or (b[3].toLong() and 0xff)
        wrongTries = 0
        return (n % 1_000_000).toString().padStart(6, '0').also { code = it }
    }

    @Synchronized fun currentCode(): String? = code

    /** A new session token for a right code, null otherwise. */
    @Synchronized fun tryPair(guess: String): String? {
        val current = code ?: return null
        if (guess != current) {
            if (++wrongTries >= MAX_WRONG) newCode()
            return null
        }
        val token = random(32).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        store.save(store.load() + token)
        newCode() // a code pairs one browser
        return token
    }

    @Synchronized fun isValid(token: String?): Boolean = token != null && token in store.load()

    @Synchronized fun forgetAll() = store.save(emptySet())

    @Synchronized fun stop() {
        code = null
        wrongTries = 0
    }

    companion object {
        const val MAX_WRONG = 5
    }
}
