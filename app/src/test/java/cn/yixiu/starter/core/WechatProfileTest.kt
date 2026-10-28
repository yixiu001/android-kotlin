package cn.yixiu.starter.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class WechatProfileTest {
    @Test fun expiredConfirmationReceiptRetiresUnknownWriteAndAllowsMeReconciliation()=runBlocking {
        val(s,a)=setup(true);val e=authorize(s,a,manual=true);val session=s.saved!!.session
        a.answer={throw IOException()}
        try {e.confirmWechatProfile(true,false)}catch(_:IOException){}
        a.answer={ApiReply(409,"AUTH_RESULT_EXPIRED")}
        try {engine(s,a).recover();fail("result unavailable must be explained")}catch(error:IllegalStateException){assertTrue(error.message!!.contains("更新账号信息"))}
        val restored=engine(s,a)
        assertEquals(session,s.saved!!.session);assertNull(s.saved!!.pending);assertNull(s.saved!!.wechatProfile)
        assertFalse(restored.state.value.privateHidden);assertNull(s.saved!!.blocked)
        a.answer={ok(JsonObject().apply {add("user",json("id" to "final","account_type" to "member","nickname" to "actually-applied"))})}
        val writes=a.requests.count {it.path=="/me/profile/wechat"};restored.me()
        assertEquals("actually-applied",restored.state.value.nickname)
        assertEquals(writes,a.requests.count {it.path=="/me/profile/wechat"});assertEquals("/me",a.requests.last().path)
    }
    @Test fun expiredConfirmationReceiptDoesNotSuppressGenuineSessionInvalidation()=runBlocking {
        val(s,a)=setup(true);val e=authorize(s,a,manual=true)
        a.answer={ApiReply(409,"AUTH_RESULT_EXPIRED")}
        try {e.confirmWechatProfile(true,false)}catch(_:IllegalStateException){}
        a.answer={ApiReply(401,"SESSION_INVALID")}
        try {e.me();fail("must block genuinely invalid session")}catch(error:ApiFailure){assertEquals("SESSION_INVALID",error.reply.code)}
        assertEquals("SESSION_INVALID",s.saved!!.blocked);assertTrue(e.state.value.privateHidden)
    }
    @Test fun loginReceiptExpiryKeepsExistingInvalidSessionRecoveryPolicy()=runBlocking {
        val(s,a)=setup();val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat()
        a.answer={ApiReply(409,"AUTH_RESULT_EXPIRED")}
        try {e.submitWechat("s".repeat(43),"code");fail("auth receipt expiry must remain blocked")}catch(error:ApiFailure){assertEquals("AUTH_RESULT_EXPIRED",error.reply.code)}
        assertEquals("/auth/login/wechat",s.saved!!.pending!!.request.path)
        assertEquals("AUTH_RESULT_EXPIRED",s.saved!!.blocked);assertTrue(e.state.value.privateHidden)
    }
    private val time = 1800000000000L
    private fun engine(s: MemoryStore, a: FakeTransport) = AccountEngine(s,a,"https://example.com","1","test",now={time})
    private fun setup(member: Boolean = false): Pair<MemoryStore,FakeTransport> {
        val s=MemoryStore()
        s.saved=VaultState("https://example.com",session=Session("a".repeat(43),"r".repeat(43),"session"),profile=JsonObject().apply {
            add("user",json("id" to "final","account_type" to if(member) "member" else "guest"))
        })
        return s to FakeTransport()
    }
    private fun candidate()=json("preview_id" to "p".repeat(43),"user_id" to "final","nickname" to "微信名字",
        "avatar_url" to "https://thirdwx.qlogo.cn/mmopen/test/0","expires_at" to java.time.Instant.ofEpochMilli(time+600000).toString())
    private fun challenge()=json("challenge_id" to "c".repeat(43),"state" to "s".repeat(43),"appid" to "wx","scope" to "snsapi_userinfo").apply { addProperty("expires_in",300) }
    private suspend fun authorize(s:MemoryStore,a:FakeTransport,manual:Boolean=false,name:String="",avatar:String="",malformed:Boolean=false):AccountEngine {
        val e=engine(s,a); a.answer={ok(challenge())}; e.requestWechat(manualProfile=manual)
        a.answer={r ->
            val user=json("id" to "final","account_type" to "member","nickname" to name,"avatar" to avatar)
            val data=if(r.path=="/me/identities/wechat") JsonObject().apply { addProperty("bound",true);add("identities",JsonObject()) } else tokens()
            data.add("user",user)
            if(malformed) data.addProperty("wechat_profile","broken") else data.add("wechat_profile",candidate())
            ok(data)
        }
        e.submitWechat("s".repeat(43),"code");return e
    }
    @Test fun finalUserControlsAutomaticFieldsAndSkipSurvivesRestart()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a,name="老账号")
        assertEquals("老账号",e.state.value.nickname)
        assertFalse(e.state.value.wechatProfile!!.canNickname);assertTrue(e.state.value.wechatProfile!!.canAvatar)
        e.skipWechatProfile();a.answer={ok(s.saved!!.profile!!.deepCopy())};engine(s,a).me()
        assertNull(engine(s,a).state.value.wechatProfile)
    }
    @Test fun completeOrMalformedPreviewNeverInterruptsLogin()=runBlocking {
        for(broken in listOf(false,true)) { val(s,a)=setup();val e=authorize(s,a,name="名字",avatar="mint",malformed=broken)
            assertEquals("member",e.state.value.accountType);assertNull(e.state.value.wechatProfile);assertNull(s.saved!!.pending)
        }
    }
    @Test fun manualReplacementHasIndependentStrictWireFields()=runBlocking {
        val(s,a)=setup(true);val e=authorize(s,a,manual=true,name="旧名",avatar="mint")
        assertTrue(e.state.value.wechatProfile!!.overwrite)
        a.answer={ok(JsonObject().apply {add("user",json("id" to "final","account_type" to "member","nickname" to "微信名字","avatar" to "mint"))})}
        e.confirmWechatProfile(true,false)
        val body=JsonParser.parseString(a.requests.last().body).asJsonObject
        assertEquals(setOf("preview_id","use_nickname","use_avatar","overwrite"),body.keySet())
        assertTrue(body["overwrite"].asBoolean);assertFalse(body["use_avatar"].asBoolean)
        assertEquals("mint",e.state.value.avatar);assertNull(e.state.value.wechatProfile)
    }
    @Test fun unknownConfirmationReplaysExactRequestAndSuccessConsumesCandidate()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a);a.answer={throw IOException()}
        try{e.confirmWechatProfile(true,false)}catch(_:IOException){}
        val request=s.saved!!.pending!!.request;assertNotNull(s.saved!!.wechatProfile)
        a.answer={ok(JsonObject().apply {add("user",json("id" to "final","account_type" to "member","nickname" to "微信名字"))})}
        engine(s,a).recover();assertEquals(request,a.requests.last());assertNull(s.saved!!.wechatProfile)
    }
    @Test fun terminalCandidateErrorsKeepLogin()=runBlocking {
        for(code in listOf("WECHAT_PROFILE_EXPIRED","WECHAT_PROFILE_INVALID","WECHAT_PROFILE_CHANGED")) {
            val(s,a)=setup();val e=authorize(s,a);a.answer={ApiReply(410,code)}
            try{e.confirmWechatProfile(true,false)}catch(_:ApiFailure){}
            assertNull(s.saved!!.wechatProfile);assertNotNull(s.saved!!.session)
        }
    }
    @Test fun manualIntentSurvivesUnknownChallengeWithoutWireMetadata()=runBlocking {
        val(s,a)=setup(true);a.answer={throw IOException()}
        try{engine(s,a).requestWechat(manualProfile=true)}catch(_:IOException){}
        assertFalse(s.saved!!.pending!!.request.body!!.contains("manual"))
        a.answer={ok(challenge())};engine(s,a).recover();assertTrue(s.saved!!.wechat!!.manualProfile)
    }
    @Test fun refreshPreservesPreviewAndGenerationMismatchHidesIt()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a);a.answer={ok(tokens("2"))};e.refresh();assertNotNull(e.state.value.wechatProfile)
        s.saved=s.saved!!.copy(generation=s.saved!!.generation+1);assertNull(engine(s,a).state.value.wechatProfile)
    }
    @Test fun oldVaultAndRedactionAreSafe() {
        assertNull(Gson().fromJson("{\"origin\":\"https://example.com\"}",VaultState::class.java).wechatProfile)
        val text=Protocol.redact("{\"preview_id\":\"secret\",\"wechat_profile\":{\"nickname\":\"private\",\"avatar_url\":\"url\"}}")
        assertFalse(text.contains("secret"));assertFalse(text.contains("private"));assertFalse(text.contains("url"))
    }
    @Test fun automaticModeRejectsReplacingExistingFieldsBeforeNetwork()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a,name="已有昵称");val count=a.requests.size
        try { e.confirmWechatProfile(true,false);fail("must reject existing name") } catch(_:IllegalArgumentException) {}
        assertEquals(count,a.requests.size)
    }
    @Test fun expiryDoesNotInvalidateLoginOrSendConfirmation()=runBlocking {
        val(s,a)=setup();authorize(s,a)
        s.saved=s.saved!!.copy(wechatProfile=s.saved!!.wechatProfile!!.copy(expiresAt=time-1))
        val e=engine(s,a);val count=a.requests.size
        try { e.confirmWechatProfile(true,false);fail("must expire") } catch(error:ApiFailure) { assertEquals("WECHAT_PROFILE_EXPIRED",error.reply.code) }
        assertEquals(count,a.requests.size);assertNotNull(s.saved!!.session);assertNull(s.saved!!.wechatProfile)
    }
    @Test fun rateLimitedConfirmationRetainsExactRequestAndCandidate()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a);a.answer={ApiReply(429,"RATE_LIMITED")}
        try {e.confirmWechatProfile(false,true)}catch(_:ApiFailure){}
        assertNotNull(s.saved!!.pending);assertNotNull(s.saved!!.wechatProfile)
        assertEquals("/me/profile/wechat",s.saved!!.pending!!.request.path)
        assertNull(e.state.value.wechatProfile)
    }
    @Test fun transientConfirmationFailureRetainsExactRequest()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a);a.answer={ApiReply(503,"SERVER_ERROR")}
        try {e.confirmWechatProfile(false,true)}catch(_:ApiFailure){}
        assertNotNull(s.saved!!.pending);assertNotNull(s.saved!!.wechatProfile)
    }
    @Test fun userAndSessionMismatchNeverExposeCandidate()=runBlocking {
        val(s,a)=setup();authorize(s,a)
        s.saved=s.saved!!.copy(session=s.saved!!.session!!.copy(id="other"))
        assertNull(engine(s,a).state.value.wechatProfile)
    }
    @Test fun canceledOldAuthCannotRemoveNewManualChallenge()=runBlocking {
        val(s,a)=setup(true);a.answer={ok(challenge())};val e=engine(s,a)
        e.requestWechat(manualProfile=true);e.cancelWechat("old-state")
        assertNotNull(s.saved!!.wechat);e.cancelWechat("s".repeat(43));assertNull(s.saved!!.wechat)
    }

    @Test fun localSkipIsAvailableDuringNetworkCooldown()=runBlocking {
        val(s,a)=setup();authorize(s,a)
        s.saved=s.saved!!.copy(notBefore=time+60000)
        engine(s,a).skipWechatProfile();assertNull(s.saved!!.wechatProfile)
    }
    @Test fun startingNewAuthInvalidatesOldChallengeEvenWhenNewRequestFails()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat(manualProfile=true)
        a.answer={ApiReply(503,"AUTH_CONFIGURATION_ERROR")}
        try {e.requestWechat()}catch(_:ApiFailure){}
        val count=a.requests.size
        try {e.submitWechat("s".repeat(43),"old-code");fail("old auth must be invalidated")}catch(_:IllegalArgumentException){}
        assertEquals(count,a.requests.size);assertNull(s.saved!!.wechat)
    }
    @Test fun manualIntentSurvivesGsonAndExchangeRecoveryAndDoesNotLeakToNextAuth()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat(manualProfile=true)
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={throw IOException()}
        try {engine(s,a).submitWechat("s".repeat(43),"code")}catch(_:IOException){}
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        val original=s.saved!!.pending!!.request
        a.answer={ok(JsonObject().apply {addProperty("bound",true);add("identities",JsonObject());add("wechat_profile",candidate())})}
        val restored=engine(s,a);restored.recover()
        assertEquals(original,a.requests.last());assertTrue(restored.state.value.wechatProfile!!.overwrite)
        restored.skipWechatProfile();a.answer={ok(challenge())};restored.requestWechat()
        assertFalse(s.saved!!.wechat!!.manualProfile)
    }
    @Test fun missingAndPartialProviderCandidatesDoNotFailLogin()=runBlocking {
        for(field in listOf("all","nickname","avatar_url","user_id","preview_id","expires_at")) {
            val(s,a)=setup();val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat()
            a.answer={ok(tokens().apply {
                add("user",json("id" to "final","account_type" to "member"))
                if(field!="all") add("wechat_profile",candidate().apply {remove(field)})
            })}
            e.submitWechat("s".repeat(43),"code")
            assertEquals("member",e.state.value.accountType);assertNull(s.saved!!.pending)
            when(field) {
                "nickname" -> {assertFalse(e.state.value.wechatProfile!!.canNickname);assertTrue(e.state.value.wechatProfile!!.canAvatar)}
                "avatar_url" -> {assertTrue(e.state.value.wechatProfile!!.canNickname);assertFalse(e.state.value.wechatProfile!!.canAvatar)}
                else -> assertNull(e.state.value.wechatProfile)
            }
        }
    }
    @Test fun skipPersistsThroughGsonRoundTripAndMeRefresh()=runBlocking {
        val(s,a)=setup();val e=authorize(s,a);e.skipWechatProfile()
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={ok(s.saved!!.profile!!.deepCopy().apply {add("wechat_profile",candidate())})}
        val restored=engine(s,a);restored.me();assertNull(restored.state.value.wechatProfile)
    }
    @Test fun mismatchedCandidateUserIsIgnoredAndPrivateStateHidesValidCandidate()=runBlocking {
        val(s,a)=setup();authorize(s,a)
        s.saved=s.saved!!.copy(wechatProfile=s.saved!!.wechatProfile!!.copy(userId="other"))
        assertNull(engine(s,a).state.value.wechatProfile)
        s.saved=s.saved!!.copy(wechatProfile=s.saved!!.wechatProfile!!.copy(userId="final"),blocked="SESSION_INVALID")
        assertNull(engine(s,a).state.value.wechatProfile)
    }
    @Test fun environmentSwitchAndLogoutRemoveCandidate()=runBlocking {
        for(switch in listOf(false,true)) {
            val(s,a)=setup();val e=authorize(s,a)
            a.answer={r -> if(r.path=="/auth/logout") ok(JsonObject().apply {addProperty("logged_out",true)}) else ok(guest())}
            if(switch)e.switchOrigin("https://other.example.com",false) else e.logout()
            assertNull(s.saved!!.wechatProfile);assertNull(s.saved!!.wechat)
        }
    }

    @Test fun manualChallengeResumesAfterUnknownRefreshAndGsonRestart()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a)
        a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
        try {e.requestWechat(manualProfile=true)}catch(_:IOException){}
        val refresh=a.requests.last();assertEquals("/auth/refresh",refresh.path)
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={r -> if(r.path=="/auth/refresh")ok(tokens("2")) else ok(challenge())}
        val restored=engine(s,a);restored.recover()
        assertEquals(refresh,a.requests[a.requests.lastIndex-1])
        assertEquals("/auth/wechat/challenges",a.requests.last().path)
        assertTrue(s.saved!!.wechat!!.manualProfile);assertEquals("2".repeat(43),a.requests.last().access)
        assertFalse(a.requests.last().body!!.contains("manualProfile"))
        a.answer={ok(JsonObject().apply {addProperty("bound",true);add("identities",JsonObject());add("wechat_profile",candidate())})}
        restored.submitWechat("s".repeat(43),"code");assertTrue(restored.state.value.wechatProfile!!.overwrite)
        restored.skipWechatProfile();a.answer={ok(challenge())};restored.requestWechat();assertFalse(s.saved!!.wechat!!.manualProfile)
    }
    @Test fun manualExchangeAndConfirmationResumeAfterInterruptedRefresh()=runBlocking {
        for(confirm in listOf(false,true)) {
            val(s,a)=setup(true);var e=engine(s,a)
            if(confirm)e=authorize(s,a,manual=true) else {a.answer={ok(challenge())};e.requestWechat(manualProfile=true)}
            a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
            try {if(confirm)e.confirmWechatProfile(true,false) else e.submitWechat("s".repeat(43),"oauth-code")}catch(_:IOException){}
            val refresh=a.requests.last();val expired=a.requests[a.requests.lastIndex-1]
            s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
            a.answer={r -> if(r.path=="/auth/refresh")ok(tokens("2")) else ok(JsonObject().apply {
                add("user",json("id" to "final","account_type" to "member","nickname" to "imported"))
                if(!confirm){addProperty("bound",true);add("identities",JsonObject());add("wechat_profile",candidate())}
            })}
            val restored=engine(s,a);restored.recover()
            assertEquals(refresh,a.requests[a.requests.lastIndex-1]);assertEquals(expired.path,a.requests.last().path)
            assertEquals(expired.body,a.requests.last().body);assertNotEquals(expired.key,a.requests.last().key)
            assertEquals("2".repeat(43),a.requests.last().access);assertNull(s.saved!!.pending)
            if(confirm)assertNull(restored.state.value.wechatProfile) else assertTrue(restored.state.value.wechatProfile!!.overwrite)
        }
    }
    @Test fun recoveredLoginContinuationStillRefreshesFinalUser()=runBlocking {
        val(s,a)=setup();val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat()
        a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
        try {e.submitWechat("s".repeat(43),"code")}catch(_:IOException){}
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={r -> when(r.path) {
            "/auth/refresh" ->ok(tokens("2"))
            "/auth/login/wechat" ->ok(tokens("3").apply {add("user",json("id" to "final","account_type" to "member"));add("wechat_profile",candidate())})
            else ->ok(JsonObject().apply {add("user",json("id" to "final","account_type" to "member","nickname" to "from-me"))})
        }}
        val restored=engine(s,a);restored.recover();assertEquals("/me",a.requests.last().path);assertEquals("from-me",restored.state.value.nickname)
    }
    @Test fun deletionDoesNotRequestCandidateOrAddUnknownExchangeFields()=runBlocking {
        val(s,a)=setup(true)
        s.saved!!.profile!!.add("identities",JsonObject().apply { add("wechat",JsonObject().apply {addProperty("bound",true)}) })
        val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat(deletion=true)
        val challengeBody=JsonParser.parseString(a.requests.last().body).asJsonObject
        assertEquals("delete",challengeBody["purpose"].asString);assertFalse(challengeBody["use_wechat_profile"].asBoolean)
        a.answer={r -> if(r.path=="/me/delete/wechat")ok(JsonObject().apply {addProperty("deleted",true)}) else ok(guest())}
        e.submitWechat("s".repeat(43),"code")
        val exchange=JsonParser.parseString(a.requests.first {it.path=="/me/delete/wechat"}.body).asJsonObject
        assertEquals(setOf("challenge_id","state","code"),exchange.keySet());assertNull(s.saved!!.wechatProfile)
    }

    @Test fun continuedWriteDoesNotRefreshTwice()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a)
        a.answer={r -> if(r.path=="/auth/refresh")ok(tokens("2")) else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
        try {e.requestWechat(manualProfile=true);fail("second expiry must stop")}catch(error:ApiFailure) {assertEquals("ACCESS_TOKEN_EXPIRED",error.reply.code)}
        assertEquals(1,a.requests.count {it.path=="/auth/refresh"});assertEquals(2,a.requests.count {it.path=="/auth/wechat/challenges"})
        assertNull(s.saved!!.pending)
    }
    @Test fun resumedUnknownExchangeReplaysExactRequestWithoutRepeatingRefresh()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat(manualProfile=true)
        a.answer={r -> when {r.path=="/auth/refresh" ->ok(tokens("2"));r.access=="a".repeat(43) ->ApiReply(401,"ACCESS_TOKEN_EXPIRED");else ->throw IOException()} }
        try {e.submitWechat("s".repeat(43),"oauth-code")}catch(_:IOException){}
        val resumed=s.saved!!.pending!!.request;assertEquals("2".repeat(43),resumed.access)
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={ok(JsonObject().apply {addProperty("bound",true);add("identities",JsonObject());add("wechat_profile",candidate())})}
        engine(s,a).recover();assertEquals(resumed,a.requests.last());assertEquals(1,a.requests.count {it.path=="/auth/refresh"})
        assertNull(s.saved!!.pending);assertTrue(s.saved!!.wechatProfile!!.overwrite)
    }
    @Test fun continuationCannotResumeAcrossGenerationOrSessionBoundary()=runBlocking {
        for(changeSession in listOf(false,true)) {
            val(s,a)=setup(true);val e=engine(s,a)
            a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
            try {e.requestWechat(manualProfile=true)}catch(_:IOException){}
            if(!changeSession)s.saved=s.saved!!.copy(generation=s.saved!!.generation+1)
            a.answer={ok(tokens("2").apply {if(changeSession)addProperty("session_id","different-session")})}
            val count=a.requests.size
            try {engine(s,a).recover();fail("must not resume into another context")}catch(error:ApiFailure){assertEquals("SOURCE_ACCOUNT_CHANGED",error.reply.code)}
            assertEquals(count+if(changeSession)1 else 0,a.requests.size);assertNull(s.saved!!.pending);assertNull(s.saved!!.wechat)
        }
    }
    @Test fun continuationRejectsChangedLocalSessionBeforeRefreshReplay()=runBlocking {
        val(s,a)=setup(true);val e=engine(s,a)
        a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
        try {e.requestWechat(manualProfile=true)}catch(_:IOException){}
        s.saved=s.saved!!.copy(session=s.saved!!.session!!.copy(id="different-local-session"))
        a.answer={r ->if(r.path=="/auth/refresh")ok(tokens("2"))else ok(challenge())}
        val count=a.requests.size
        try {engine(s,a).recover();fail("local session changed")}catch(error:ApiFailure){assertEquals("SOURCE_ACCOUNT_CHANGED",error.reply.code)}
        assertEquals(count,a.requests.size)
    }
    @Test fun recoveredDeletionContinuationCreatesNewGuest()=runBlocking {
        val(s,a)=setup(true)
        s.saved!!.profile!!.add("identities",JsonObject().apply {add("wechat",JsonObject().apply {addProperty("bound",true)})})
        val e=engine(s,a);a.answer={ok(challenge())};e.requestWechat(deletion=true)
        a.answer={r -> if(r.path=="/auth/refresh")throw IOException() else ApiReply(401,"ACCESS_TOKEN_EXPIRED")}
        try {e.submitWechat("s".repeat(43),"code")}catch(_:IOException){}
        s.saved=Gson().fromJson(Gson().toJson(s.saved),VaultState::class.java)
        a.answer={r -> when(r.path) {"/auth/refresh" ->ok(tokens("2"));"/me/delete/wechat" ->ok(JsonObject().apply {addProperty("deleted",true)});else ->ok(guest())}}
        val restored=engine(s,a);restored.recover();assertEquals("/auth/guest",a.requests.last().path);assertEquals("guest",restored.state.value.accountType)
    }

}
