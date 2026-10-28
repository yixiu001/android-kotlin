package cn.yixiu.starter.debug

import cn.yixiu.starter.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ResetAccountBridgeTest {
    @Test fun bridgeRejectsChangedIdentityOriginAndGeneration() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        api.answer = { ok(guest()) }
        val engine = AccountEngine(store, api, "https://example.com", "1", "test")
        engine.start()
        var called = false
        for ((origin, user, generation) in listOf(Triple("https://other.com", "user-1", 1L), Triple("https://example.com", "other", 1L), Triple("https://example.com", "user-1", 999L))) {
            try { engine.withTestAccount(origin, user, generation) { _, _ -> called = true }; fail("changed context accepted") } catch (_: IllegalStateException) {}
        }
        assertFalse(called)
    }
    @Test fun prepareSafelyRefreshesExpiredAccessBeforePassingToken() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        api.answer = { ok(guest()) }
        val engine = AccountEngine(store, api, "https://example.com", "1", "test")
        engine.start()
        var reads = 0
        api.answer = {
            when (it.path) {
                "/me" -> if (reads++ == 0) ApiReply(401, "ACCESS_TOKEN_EXPIRED") else ok(guest())
                "/auth/refresh" -> ok(tokens("2"))
                else -> error("unexpected path")
            }
        }
        engine.withTestAccount("https://example.com", "user-1", prepare = true) { access, _ -> assertEquals("2".repeat(43), access) }
        assertEquals(2, reads)
        assertNull(store.saved!!.pending)
    }
}
