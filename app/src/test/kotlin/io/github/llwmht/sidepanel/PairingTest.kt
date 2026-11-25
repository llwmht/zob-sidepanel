package io.github.llwmht.sidepanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PairingTest {
    private val rnd = Random(42)
    private val store = object : TokenStore {
        var saved = emptySet<String>()
        override fun load() = saved
        override fun save(tokens: Set<String>) { saved = tokens }
    }
    private val pairing = Pairing({ n -> ByteArray(n).also { rnd.nextBytes(it) } }, store)

    @Test fun codeIsSixDigits() = assertTrue(Regex("""\d{6}""").matches(pairing.newCode()))

    @Test fun rightCodeGivesAValidToken() {
        val token = pairing.tryPair(pairing.newCode())
        assertNotNull(token)
        assertTrue(Regex("""[0-9a-f]{64}""").matches(token!!))
        assertTrue(pairing.isValid(token))
        assertEquals(setOf(token), store.saved)
    }

    @Test fun wrongCodeGivesNothing() {
        val code = pairing.newCode()
        assertNull(pairing.tryPair(if (code == "000000") "000001" else "000000"))
    }

    @Test fun fiveWrongTriesRotateTheCode() {
        val code = pairing.newCode()
        val wrong = if (code == "000000") "000001" else "000000"
        repeat(5) { pairing.tryPair(wrong) }
        assertNotEquals(code, pairing.currentCode())
        assertNull(pairing.tryPair(code))
    }

    @Test fun noCodeBeforeNewCodeOrAfterStop() {
        assertNull(pairing.currentCode())
        assertNull(pairing.tryPair("123456"))
        val code = pairing.newCode()
        pairing.stop()
        assertNull(pairing.tryPair(code))
    }

    @Test fun forgetAllInvalidatesTokens() {
        val token = pairing.tryPair(pairing.newCode())
        pairing.forgetAll()
        assertFalse(pairing.isValid(token))
        assertTrue(store.saved.isEmpty())
    }

    @Test fun nullOrUnknownTokenIsInvalid() {
        assertFalse(pairing.isValid(null))
        assertFalse(pairing.isValid("nope"))
    }
}
