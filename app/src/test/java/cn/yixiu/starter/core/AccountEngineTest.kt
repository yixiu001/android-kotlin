package cn.yixiu.starter.core

import com.google.gson.JsonObject
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MemoryStore : StateStore {
    var saved: VaultState? = null
    override fun load() = saved
    override fun save(state: VaultState) { saved = state }
}
class FakeTransport : ApiTransport {
    val requests = mutableListOf<WireRequest>()
    var answer: (WireRequest) -> ApiReply = { ok() }
    override suspend fun execute(request: WireRequest): ApiReply { requests += request; return answer(request) }
}
fun json(vararg fields: Pair<String, String>) = JsonObject().apply { fields.forEach { addProperty(it.first, it.second) } }
fun ok(data: JsonObject = JsonObject()) = ApiReply(200, "OK", data, "a".repeat(32))
fun tokens(version: String = "1") = json(
    "session_id" to "session", "token_type" to "Bearer",
    "access_token" to version.repeat(43), "refresh_token" to ("r" + version).repeat(22).take(43)
).apply { addProperty("access_expires_in", 7200); addProperty("refresh_expires_in", 2592000) }
fun guest() = tokens().apply { add("user", json("id" to "user-1", "account_type" to "guest")) }

class AccountEngineTest {
    private val base = "https://example.com"
    private fun engine(store: MemoryStore, api: FakeTransport) = AccountEngine(store, api, base, "1.0", "JUnit")
    private suspend fun initialized(store: MemoryStore, api: FakeTransport): AccountEngine {
        api.answer = { if (it.path == "/auth/guest") ok(guest()) else ok(json("session_id" to "session").apply { add("user", json("id" to "user-1", "account_type" to "guest")) }) }
        return engine(store, api).also { it.start() }
    }
    @Test fun unknownGuestResultSurvivesRestartWithExactRequest() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        api.answer = { throw IOException("lost response") }
        try { engine(store, api).start() } catch (_: IOException) {}
        val first = api.requests.single()
        assertNotNull(store.saved?.pending)
        api.answer = { ok(guest()) }
        val restored = engine(store, api)
        restored.recover()
        assertEquals(first, api.requests.last())
        assertNull(store.saved?.pending)
        assertEquals("user-1", restored.state.value.userId)
    }
    @Test fun restartWithSessionDoesNotCreateAnotherGuest() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        initialized(store, api); engine(store, api).start()
        assertEquals(1, api.requests.count { it.path == "/auth/guest" })
    }
    @Test fun simultaneousMeRequestsRefreshOnlyOnce() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = {
            when {
                it.path == "/auth/refresh" -> ok(tokens("2"))
                it.access == "1".repeat(43) -> ApiReply(401, "ACCESS_TOKEN_EXPIRED")
                else -> ok(json("session_id" to "session").apply { add("user", json("id" to "user-1", "account_type" to "guest")) })
            }
        }
        coroutineScope { (1..10).map { async { engine.me() } }.awaitAll() }
        assertEquals(1, api.requests.count { it.path == "/auth/refresh" })
        assertNull(api.requests.first { it.path == "/auth/refresh" }.access)
        assertEquals(1L, store.saved?.tokenVersion)
    }
    @Test fun logoutTimeoutHidesPrivateDataAndKeepsRecovery() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { throw IOException("timeout") }
        try { engine.logout() } catch (_: IOException) {}
        assertTrue(engine.state.value.privateHidden)
        assertNotNull(store.saved?.session)
        assertEquals("/auth/logout", store.saved?.pending?.request?.path)
        api.answer = { if (it.path == "/auth/logout") ok(JsonObject().apply { addProperty("logged_out", true) }) else ok(guest()) }
        engine.recover()
        assertEquals(2, api.requests.count { it.path == "/auth/guest" })
    }
    @Test fun invalidOrDisabledSessionNeverCreatesGuest() = runBlocking {
        for (code in listOf("SESSION_INVALID", "ACCOUNT_DISABLED")) {
            val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
            api.answer = { ApiReply(if (code == "ACCOUNT_DISABLED") 403 else 401, code) }
            try { engine.me() } catch (_: ApiFailure) {}
            assertEquals(1, api.requests.count { it.path == "/auth/guest" })
            assertTrue(engine.state.value.privateHidden)
        }
    }
    @Test fun environmentSwitchDoesNotForwardOldCredentials() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ok(guest()) }
        engine.switchOrigin("https://other.example.com", false)
        val request = api.requests.last()
        assertEquals("https://other.example.com", request.origin)
        assertEquals("/auth/guest", request.path); assertNull(request.access)
    }
    @Test fun uncertainOperationBlocksNewIdentityAndEnvironment() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { throw IOException() }
        try { engine.requestSms("+8613800008000") } catch (_: IOException) {}
        val count = api.requests.size
        try { engine.requestSms("+8613900009000"); fail("must block") } catch (_: PendingOperation) {}
        try { engine.switchOrigin("https://other.example.com", false); fail("must block") } catch (_: PendingOperation) {}
        assertEquals(count, api.requests.size)
    }
    @Test fun providerDisabledIsExplicitFailureNotFakeLogin() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ApiReply(503, "AUTH_CONFIGURATION_ERROR") }
        try { engine.requestSms("+8613800008000"); fail("must fail") } catch (e: ApiFailure) { assertEquals("AUTH_CONFIGURATION_ERROR", e.reply.code) }
        assertEquals("guest", engine.state.value.accountType); assertNull(store.saved?.pending)
    }
    @Test fun challengeStateAndExpiryMustMatchBeforeWechatExchange() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ok(json("challenge_id" to "c".repeat(43), "state" to "s".repeat(43), "appid" to "wx123", "scope" to "snsapi_userinfo").apply { addProperty("expires_in",300) }) }
        engine.requestWechat()
        val count = api.requests.size
        try { engine.submitWechat("wrong", "code"); fail("must reject") } catch (_: IllegalArgumentException) {}
        assertEquals(count, api.requests.size)
        assertEquals("s".repeat(43), store.saved?.wechat?.state)
    }
    @Test fun refreshTimeoutReplaysOriginalRefreshAndRotatesAtomically() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { throw IOException() }
        try { engine.refresh() } catch (_: IOException) {}
        val original = api.requests.last()
        api.answer = { ok(tokens("2")) }
        engine(store, api).recover()
        assertEquals(original, api.requests.last())
        assertEquals("2".repeat(43), store.saved?.session?.access)
        assertNull(store.saved?.pending)
    }
    @Test fun refreshPreservesUnexpiredVerificationChallenge() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { if (it.path == "/auth/sms/challenges") ok(json("challenge_id" to "c".repeat(43)).apply {
            addProperty("expires_in", 300); addProperty("resend_after", 60)
        }) else ok(tokens("2")) }
        engine.requestSms("+8613800008000")
        engine.refresh()
        assertEquals("c".repeat(43), store.saved?.sms?.id)
    }
    @Test fun rateLimitBlocksFurtherRequestsUntilRetryAfter() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ApiReply(429, "RATE_LIMITED", retryAfter = 60) }
        try { engine.requestSms("+8613800008000") } catch (_: ApiFailure) {}
        val count = api.requests.size
        try { engine.requestSms("+8613800008000"); fail("must wait") } catch (_: Exception) {}
        assertEquals(count, api.requests.size)
    }
    @Test fun oldWechatStateCannotBeUsedAfterNewChallenge() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        var state = "a".repeat(43)
        api.answer = { ok(json("challenge_id" to "c".repeat(43), "state" to state, "appid" to "wx123", "scope" to "snsapi_userinfo").apply { addProperty("expires_in",300) }) }
        engine.requestWechat()
        state = "b".repeat(43)
        engine.requestWechat()
        val count = api.requests.size
        try { engine.submitWechat("a".repeat(43), "code"); fail("must reject") } catch (_: IllegalArgumentException) {}
        assertEquals(count, api.requests.size)
    }
    @Test fun lostPhoneLoginCanRecoverWithRevokedOriginalGuestCredential() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ok(json("challenge_id" to "c".repeat(43)).apply { addProperty("expires_in", 300); addProperty("resend_after", 60) }) }
        engine.requestSms("+8613800008000")
        api.answer = { throw IOException() }
        try { engine.submitPhone("001234") } catch (_: IOException) {}
        val original = api.requests.last()
        assertEquals("/auth/login/phone", original.path)
        assertEquals("1".repeat(43), original.access)
        assertEquals("""{"challenge_id":"ccccccccccccccccccccccccccccccccccccccccccc","code":"001234"}""", original.body)
        api.answer = { ok(tokens("2").apply { add("user", json("id" to "old-member", "account_type" to "member")); addProperty("action", "merged") }) }
        val restored = engine(store, api)
        restored.recover()
        assertEquals(original, api.requests.last { it.path == "/auth/login/phone" })
        assertEquals("old-member", restored.state.value.userId)
        assertEquals("member", restored.state.value.accountType)
    }
    @Test fun memberWechatBindingUsesBindingPurposeAndPreservesAccountOnConflict() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        store.saved = VaultState(base, session = Session("1".repeat(43), "r".repeat(43), "s"),
            profile = JsonObject().apply { add("user", json("id" to "member", "account_type" to "member")) })
        val engine = engine(store, api)
        api.answer = { ok(json("challenge_id" to "c".repeat(43), "state" to "s".repeat(43), "appid" to "wx123", "scope" to "snsapi_userinfo").apply { addProperty("expires_in",300) }) }
        engine.requestWechat()
        assertEquals("""{"purpose":"bind_wechat","use_wechat_profile":true}""", api.requests.last().body)
        api.answer = { ApiReply(409, "IDENTITY_ALREADY_BOUND") }
        try { engine.submitWechat("s".repeat(43), "wechat-code"); fail() } catch (_: ApiFailure) {}
        assertEquals("/me/identities/wechat", api.requests.last().path)
        assertEquals("member", engine.state.value.userId)
        assertNull(store.saved?.pending)
    }
    @Test fun memberPhoneBindDoesNotCallGuestLogin() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        store.saved = VaultState(base, session = Session("1".repeat(43), "r".repeat(43), "s"),
            profile = JsonObject().apply { add("user", json("id" to "member", "account_type" to "member")) })
        val engine = engine(store, api)
        api.answer = { ok(json("challenge_id" to "c".repeat(43)).apply { addProperty("expires_in", 300); addProperty("resend_after", 60) }) }
        engine.requestSms("+8613800008000")
        assertTrue(api.requests.last().body!!.contains("bind_phone"))
        api.answer = { ok(JsonObject().apply {
            addProperty("bound", true)
            add("identities", JsonObject().apply { add("phone", json("masked" to "+86 138****8000").apply { addProperty("bound", true) }) })
        }) }
        engine.submitPhone("123456")
        assertEquals("/me/identities/phone", api.requests.last().path)
        assertEquals("+86 138****8000", engine.state.value.phone)
    }
    @Test fun expiredWechatChallengeDoesNotReachServer() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        var clock = 1000L
        val engine = AccountEngine(store, api, base, "1.0", "JUnit", now = { clock })
        api.answer = { ok(guest()) }; engine.start()
        api.answer = { ok(json("challenge_id" to "c".repeat(43), "state" to "s".repeat(43), "appid" to "wx123", "scope" to "snsapi_userinfo").apply { addProperty("expires_in",300) }) }
        engine.requestWechat()
        clock += 300001
        val count = api.requests.size
        try { engine.submitWechat("s".repeat(43), "code"); fail() } catch (_: IllegalArgumentException) {}
        assertEquals(count, api.requests.size)
    }
    @Test fun explicitChallengeAbandonWaitsAndNeverDiscardsLogin() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { throw IOException() }
        try { engine.requestSms("+8613800008000") } catch (_: IOException) {}
        engine.abandonChallenge()
        assertNull(store.saved?.pending)
        assertTrue(store.saved!!.notBefore > System.currentTimeMillis())
        store.saved = store.saved!!.copy(pending = Pending(Protocol.request(base, "/auth/login/phone", "{}", "a"), 1))
        try { engine(store, api).abandonChallenge(); fail() } catch (_: IllegalStateException) {}
        assertNotNull(store.saved!!.pending)
    }
    @Test fun rateLimitedRecoveryKeepsOriginalCommittedRefreshRequest() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        var clock = 1000L
        val engine = AccountEngine(store, api, base, "1.0", "JUnit", now = { clock })
        api.answer = { ok(guest()) }; engine.start()
        api.answer = { throw IOException() }
        try { engine.refresh() } catch (_: IOException) {}
        val original = api.requests.last()
        api.answer = { ApiReply(429, "RATE_LIMITED", retryAfter = 60) }
        try { engine.recover() } catch (_: ApiFailure) {}
        assertEquals(original, store.saved?.pending?.request)
        val count = api.requests.size
        try { engine.recover(); fail() } catch (_: ApiFailure) {}
        assertEquals(count, api.requests.size)
        clock += 60001
        api.answer = { ok(tokens("2")) }; engine.recover()
        assertEquals(original, api.requests.last())
        assertEquals("2".repeat(43), store.saved?.session?.access)
    }
    @Test fun replayedSmsCannotExtendOriginalExpiryOrCooldown() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport()
        var clock = 1000L
        val engine = AccountEngine(store, api, base, "1.0", "JUnit", now = { clock })
        api.answer = { ok(guest()) }; engine.start()
        api.answer = { throw IOException() }
        try { engine.requestSms("+8613800008000") } catch (_: IOException) {}
        clock += 360000
        api.answer = { ok(json("challenge_id" to "c".repeat(43)).apply { addProperty("expires_in",300); addProperty("resend_after",60) }) }
        engine.recover()
        assertTrue(engine.state.value.smsExpiresAt < clock)
        assertTrue(engine.state.value.smsResendAt < clock)
        val count = api.requests.size
        try { engine.submitPhone("123456"); fail() } catch (_: IllegalArgumentException) {}
        assertEquals(count, api.requests.size)
    }
    @Test fun recoveredLoginFetchesActualIdentityBindings() = runBlocking {
        val store = MemoryStore(); val api = FakeTransport(); val engine = initialized(store, api)
        api.answer = { ok(json("challenge_id" to "c".repeat(43)).apply { addProperty("expires_in",300); addProperty("resend_after",60) }) }
        engine.requestSms("+8613800008000")
        api.answer = { throw IOException() }
        try { engine.submitPhone("123456") } catch (_: IOException) {}
        api.answer = {
            if (it.path == "/me") ok(JsonObject().apply {
                add("user", json("id" to "member", "account_type" to "member"))
                add("identities", JsonObject().apply { add("phone", json("masked" to "+86 138****8000").apply { addProperty("bound",true) }) })
            }) else ok(tokens("2").apply { add("user", json("id" to "member", "account_type" to "member")) })
        }
        engine.recover()
        assertEquals("+86 138****8000", engine.state.value.phone)
    }
}
