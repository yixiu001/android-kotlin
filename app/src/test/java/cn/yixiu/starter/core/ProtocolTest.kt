package cn.yixiu.starter.core

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ProtocolTest {
    @Test fun rejectsUnsafeOrigins() {
        listOf("http://example.com", "https://u:p@example.com", "https://example.com?q=1", "https://example.com/path", "https://example.com/#x").forEach {
            assertThrows(IllegalArgumentException::class.java) { Protocol.origin(it) }
        }
        assertEquals("https://example.com", Protocol.origin("https://example.com/"))
    }
    @Test fun idempotencyKeyHasRealEntropyAndProtocolSize() {
        val keys = (1..100).map { Protocol.newKey() }
        assertEquals(100, keys.toSet().size)
        keys.forEach { assertTrue(it.matches(Regex("[A-Za-z0-9_-]{43}"))); assertEquals(32, Base64.getUrlDecoder().decode(it).size) }
    }
    @Test fun guestAndRefreshCannotLeakAccessTokens() {
        for (path in listOf("/auth/guest", "/auth/refresh")) {
            val request = Protocol.request("https://example.com", path, "{}", "secret")
            assertNull(request.access)
            assertNotNull(request.key)
        }
        val me = Protocol.request("https://example.com", "/me", null, "secret")
        assertNull(me.key)
        assertEquals("secret", me.access)
    }
    @Test fun nestedSecretsAreRemovedBeforeDisplay() {
        val text = Protocol.redact("""{"data":{"access_token":"secret","phone":"+8613800008000","code":"123456","nested":[{"state":"sensitive"}]},"request_id":"safe"}""")
        assertFalse(text.contains("secret")); assertFalse(text.contains("13800008000"))
        assertFalse(text.contains("123456")); assertFalse(text.contains("sensitive"))
        assertEquals("safe", JsonParser.parseString(text).asJsonObject["request_id"].asString)
    }
}
