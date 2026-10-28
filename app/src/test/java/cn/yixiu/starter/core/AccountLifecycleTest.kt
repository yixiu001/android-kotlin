package cn.yixiu.starter.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AccountLifecycleTest {
    private fun member(store: MemoryStore) {
        store.saved = VaultState("https://example.com", session = Session("a".repeat(43), "r".repeat(43), "member-session"),
            profile = JsonObject().apply {
                add("user", json("id" to "member-1", "account_type" to "member"))
                add("identities", JsonObject().apply {
                    add("phone", json("masked" to "+86138****8000"))
                    add("wechat", JsonObject().apply { addProperty("bound", true) })
                })
            })
    }
    private fun challenge(wechat: Boolean = false) = json("challenge_id" to "c".repeat(43)).apply {
        addProperty("expires_in", 300); addProperty("resend_after", 60)
        if (wechat) { addProperty("state", "s".repeat(43)); addProperty("appid", "wx-app"); addProperty("scope", "snsapi_userinfo") }
    }
    @Test fun deletionResponseLossHidesProfileAndRecoversExactRequestBeforeNewGuest() = runBlocking {
        val store = MemoryStore(); member(store)
        val api = FakeTransport(); var cleared = 0
        val engine = AccountEngine(store, api, "https://example.com", "1", "test", onAccountCleared = { cleared++ })
        api.answer = { ok(challenge()) }
        engine.requestSms("+8613800008000", deletion = true)
        assertEquals("delete", store.saved!!.sms!!.purpose)
        api.answer = { throw IOException("lost") }
        try { engine.submitPhone("123456"); fail() } catch (_: IOException) {}
        assertTrue(engine.state.value.privateHidden)
        assertEquals(0, cleared)
        val original = store.saved!!.pending!!.request
        assertEquals("/me/delete/phone", original.path)
        api.answer = { if (it.path.startsWith("/me/delete/")) ok(JsonObject().apply { addProperty("deleted", true) }) else ok(guest()) }
        val restored = AccountEngine(store, api, "https://example.com", "1", "test", onAccountCleared = { cleared++ })
        restored.recover()
        assertEquals(original, api.requests[api.requests.lastIndex - 1])
        assertEquals("guest", restored.state.value.accountType)
        assertEquals(2, cleared) // confirmed deletion and new guest each clear account-specific cache
        assertNull(store.saved!!.sms)
    }
    @Test fun wechatConsentPersistsAndDeletionNeverImportsProfile() = runBlocking {
        val store = MemoryStore(); member(store)
        val api = FakeTransport(); api.answer = { ok(challenge(true)) }
        val engine = AccountEngine(store, api, "https://example.com", "1", "test")
        engine.requestWechat(useProfile = true)
        assertTrue(store.saved!!.wechat!!.useWechatProfile)
        val restored = AccountEngine(store, api, "https://example.com", "1", "test")
        api.answer = { ok(JsonObject().apply { addProperty("bound", true); add("identities", JsonObject().apply { add("wechat", JsonObject().apply { addProperty("bound", true) }) }) }) }
        restored.submitWechat("s".repeat(43), "code")
        assertTrue(JsonParser.parseString(api.requests.last().body).asJsonObject["use_wechat_profile"].asBoolean)
        api.answer = { ok(challenge(true)) }
        restored.requestWechat(useProfile = true, deletion = true)
        assertFalse(store.saved!!.wechat!!.useWechatProfile)
        assertEquals("delete", store.saved!!.wechat!!.purpose)
    }
    @Test fun profileUpdatesPreserveSessionAndIdentityAndRejectUnsafeValues() = runBlocking {
        val store = MemoryStore(); member(store)
        val api = FakeTransport(); val engine = AccountEngine(store, api, "https://example.com", "1", "test")
        api.answer = { ok(JsonObject().apply { add("user", json("id" to "member-1", "account_type" to "member", "nickname" to "昵称", "avatar" to "ocean")) }) }
        engine.updateProfile("昵称", "ocean")
        assertEquals("昵称", engine.state.value.nickname)
        assertEquals("ocean", engine.state.value.avatar)
        assertEquals("member-session", store.saved!!.session!!.id)
        assertEquals("+86138****8000", engine.state.value.phone)
        try { engine.updateProfile("", "../../a"); fail() } catch (_: IllegalArgumentException) {}
        assertEquals(1, api.requests.size)
    }

    @Test fun rejectedDeletionKeepsAccountAndDoesNotCreateGuest() = runBlocking {
        val store = MemoryStore(); member(store)
        val api = FakeTransport(); val engine = AccountEngine(store, api, "https://example.com", "1", "test")
        api.answer = { ok(challenge()) }
        engine.requestSms("+8613800008000", deletion = true)
        api.answer = { ApiReply(422, "VERIFICATION_INVALID") }
        try { engine.submitPhone("000000"); fail() } catch (_: ApiFailure) {}
        assertEquals("member-1", engine.state.value.userId)
        assertEquals("member-session", store.saved!!.session!!.id)
        assertFalse(engine.state.value.privateHidden)
        assertFalse(api.requests.any { it.path == "/auth/guest" })
    }

    @Test fun oldChallengeDoesNotImplicitlyConsentToWechatProfile() {
        val old = """{"id":"challenge","purpose":"bind_wechat","expiresAt":9999999,"resendAt":0,"state":"state","appId":"appid","scope":"snsapi_userinfo","phone":""}"""
        val restored = com.google.gson.Gson().fromJson(old, Challenge::class.java)
        assertFalse(restored.useWechatProfile)
        assertEquals("bind_wechat", restored.purpose)
    }
}
