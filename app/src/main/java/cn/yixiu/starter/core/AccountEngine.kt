package cn.yixiu.starter.core

import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class AccountEngine(private val store: StateStore, private val api: ApiTransport, origin: String,
                    private val version: String, private val deviceName: String,
                    private val allowSavedOrigin: Boolean = true,
                    private val onAccountCleared: () -> Unit = {},
                    private val now: () -> Long = System::currentTimeMillis) {
    private val gate = Mutex()
    private var vault = store.load() ?: VaultState(Protocol.origin(origin))
    private val view = MutableStateFlow(AccountView(vault.origin))
    val state: StateFlow<AccountView> = view.asStateFlow()
    init {
        require(version.toByteArray().size in 1..32)
        check(allowSavedOrigin || vault.origin == Protocol.origin(origin)) {
            "产品接口地址已改变，请明确清除旧应用数据后重新连接，避免混用账号"
        }
        publish()
    }
    // 串行协调读写：包括 GET，旧响应处理完毕后才允许切换账号或环境。
    // 发送后的事务不因页面离开而取消；最长受传输超时限制，结果必须落盘。
    private suspend fun <T> exclusive(block: suspend () -> T): T =
        gate.withLock { withContext(NonCancellable + Dispatchers.IO) { block() } }
    private fun commit(next: VaultState) { store.save(next); vault = next; publish() }
    private fun publish() {
        val hidden = vault.blocked != null || vault.pending?.request?.path == "/auth/logout" || vault.pending?.request?.path?.startsWith("/me/delete/") == true
        val user = vault.profile?.getAsJsonObject("user")
        val identities = vault.profile?.getAsJsonObject("identities")
        view.value = AccountView(vault.origin,
            if (hidden) "" else user?.get("id")?.asString.orEmpty(),
            if (hidden) "" else user?.get("account_type")?.asString.orEmpty(),
            if (hidden) "" else identities?.getAsJsonObject("phone")?.get("masked")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
            !hidden && (identities?.getAsJsonObject("wechat")?.get("bound")?.asBoolean == true),
            vault.session != null, vault.pending?.request?.path, hidden, vault.blocked,
            vault.sms?.resendAt ?: 0, vault.sms?.expiresAt ?: 0, if (hidden) "" else vault.sms?.phone.orEmpty(), vault.notBefore,
            if (hidden) "" else user?.get("nickname")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
            if (hidden) "" else user?.get("avatar")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
            if (hidden) "" else user?.get("avatar_url")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
            vault.sms?.purpose.orEmpty(), if (hidden || vault.pending != null) null else validWechatProfile())
    }
    private fun validWechatProfile(): WechatProfile? = vault.wechatProfile?.takeIf {
        it.origin == vault.origin && it.generation == vault.generation && it.sessionId == vault.session?.id &&
            it.userId == vault.profile?.getAsJsonObject("user")?.get("id")?.asString && vault.session != null && vault.blocked == null
    }
    private fun parseWechatProfile(data: JsonObject, state: VaultState, manual: Boolean): WechatProfile? = runCatching {
        val candidate = data.getAsJsonObject("wechat_profile") ?: return@runCatching null
        fun field(key: String) = candidate.get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
        val id = field("preview_id")
        val user = state.profile?.getAsJsonObject("user") ?: return@runCatching null
        val userId = user.get("id").asString
        if (!id.matches(Regex("[A-Za-z0-9_-]{43}")) || field("user_id") != userId) return@runCatching null
        val expires = java.time.Instant.parse(field("expires_at")).toEpochMilli()
        if (expires <= now()) return@runCatching null
        fun missing(key: String) = user.get(key)?.takeUnless { it.isJsonNull }?.asString.isNullOrBlank()
        val nickname = field("nickname")
        val avatar = field("avatar_url")
        val canName = nickname.isNotBlank() && (manual || missing("nickname"))
        val canAvatar = avatar.isNotBlank() && (manual || (missing("avatar") && missing("avatar_url")))
        if (!canName && !canAvatar) return@runCatching null
        WechatProfile(id, userId, state.session!!.id, state.origin, state.generation, expires, nickname, avatar,
            manual, canName, canAvatar)
    }.getOrNull()
    suspend fun skipWechatProfile() = exclusive {
        if (vault.pending != null) throw PendingOperation()
        vault.blocked?.let { throw ApiFailure(ApiReply(401, it)) }
        commit(vault.copy(wechatProfile = null))
    }
    suspend fun confirmWechatProfile(useNickname: Boolean, useAvatar: Boolean) = exclusive {
        ready()
        val preview = validWechatProfile() ?: throw IllegalArgumentException("本次微信资料选择已结束，登录仍然有效")
        if (preview.expiresAt <= now()) {
            commit(vault.copy(wechatProfile = null))
            throw ApiFailure(ApiReply(410, "WECHAT_PROFILE_EXPIRED"))
        }
        require((useNickname || useAvatar) && (!useNickname || preview.canNickname) && (!useAvatar || preview.canAvatar)) { "请选择可用的微信资料" }
        writeLocked("/me/profile/wechat", JsonObject().apply {
            addProperty("preview_id", preview.previewId); addProperty("use_nickname", useNickname)
            addProperty("use_avatar", useAvatar); addProperty("overwrite", preview.overwrite)
        }.toString())
    }
    private fun ready(checkBlocked: Boolean = true) {
        if (vault.pending != null) throw PendingOperation()
        if (checkBlocked) vault.blocked?.let { throw ApiFailure(ApiReply(401, it)) }
        if (now() < vault.notBefore) throw ApiFailure(ApiReply(429, "RATE_LIMITED", retryAfter = (vault.notBefore - now() + 999) / 1000))
    }
    private fun body(vararg fields: Pair<String, String>) = JsonObject().apply {
        fields.forEach { addProperty(it.first, it.second) }
    }.toString()
    private fun purpose(provider: String) = if (view.value.accountType == "member") "bind_" + provider else "login"
    private fun session() = vault.session ?: throw ApiFailure(ApiReply(401, "SESSION_INVALID"))
    /** Debug bridge; lock spans identity validation, network operation and local clear. */
    suspend fun <T> withTestAccount(origin: String, user: String, generation: Long? = null,
                                   prepare: Boolean = false,
                                   block: suspend (String, Long) -> T): T = exclusive {
        check(cn.yixiu.starter.BuildConfig.DEBUG && cn.yixiu.starter.BuildConfig.TEST_TOOLS_ENABLED)
        ready()
        check(origin == vault.origin && user.isNotBlank() && user == view.value.userId &&
            (generation == null || generation == vault.generation)) { "Account or environment changed" }
        if (prepare) meLocked()
        check(user == view.value.userId)
        block(session().access, vault.generation)
    }
    suspend fun start() = exclusive {
        ready()
        if (vault.session == null) guestLocked() else meLocked()
    }
    private suspend fun guestLocked() {
        writeLocked("/auth/guest", body("installation_id" to vault.installationId, "platform" to "android",
            "app_version" to version, "device_name" to deviceName.take(40)))
    }
    suspend fun me() = exclusive { ready(); meLocked() }
    private suspend fun meLocked() {
        var reply = api.execute(Protocol.request(vault.origin, "/me", null, session().access))
        if (reply.code == "ACCESS_TOKEN_EXPIRED") {
            refreshLocked()
            reply = api.execute(Protocol.request(vault.origin, "/me", null, session().access))
        }
        ensureSuccess(reply)
        if (!reply.data.has("user")) throw IOException("账号响应缺少用户资料")
        commit(vault.copy(profile = reply.data.deepCopy()))
    }
    suspend fun refresh() = exclusive { ready(); refreshLocked() }
    private suspend fun refreshLocked() { writeLocked("/auth/refresh", body("refresh_token" to session().refresh), false) }
    suspend fun logout() = exclusive {
        ready(checkBlocked = false)
        if (vault.blocked == "ACCOUNT_DISABLED") throw ApiFailure(ApiReply(403, "ACCOUNT_DISABLED"))
        writeLocked("/auth/logout", body("refresh_token" to session().refresh), false)
        guestLocked()
    }
    suspend fun updateProfile(nickname: String?, avatar: String?) = exclusive {
        ready()
        require(nickname != null || avatar != null) { "请先修改资料" }
        nickname?.let { require(it.isNotBlank() && it.codePointCount(0, it.length) <= 64 && !it.any(Char::isISOControl)) { "昵称需要 1–64 个字符，不能包含控制字符" } }
        avatar?.let { require(it in Protocol.avatars) { "请选择内置头像" } }
        val input = JsonObject().apply { nickname?.let { addProperty("nickname", it.trim()) }; avatar?.let { addProperty("avatar", it) } }
        writeLocked("/me/profile", input.toString())
    }
    suspend fun requestSms(phone: String, deletion: Boolean = false) = exclusive {
        ready()
        if (deletion) require(view.value.accountType == "member" && view.value.phone.isNotBlank()) { "请使用当前账号已绑定的手机号验证注销" }
        require(phone.matches(Regex("\\+861[3-9][0-9]{9}"))) { "请输入中国大陆手机号" }
        require(now() >= (vault.sms?.resendAt ?: 0)) { "请等待短信倒计时结束" }
        writeLocked("/auth/sms/challenges", body("phone" to phone, "purpose" to if (deletion) "delete" else purpose("phone")))
    }
    suspend fun submitPhone(code: String) = exclusive {
        ready()
        require(code.matches(Regex("[0-9]{6}"))) { "验证码须为 6 位数字" }
        val challenge = vault.sms ?: throw IllegalArgumentException("请先获取验证码")
        require(challenge.expiresAt > now()) { "验证码已过期，请重新获取" }
        val deleting = challenge.purpose == "delete"
        writeLocked(if (deleting) "/me/delete/phone" else if (challenge.purpose == "login") "/auth/login/phone" else "/me/identities/phone",
            body("challenge_id" to challenge.id, "code" to code))
        if (deleting) guestLocked()
        deleting
    }
    suspend fun requestWechat(useProfile: Boolean = true, deletion: Boolean = false, manualProfile: Boolean = false): Challenge = exclusive {
        ready()
        if (deletion) require(view.value.accountType == "member" && view.value.wechatBound) { "请使用当前账号已绑定的微信验证注销" }
        // A fresh intent invalidates previous callbacks even if acquiring its challenge fails.
        commit(vault.copy(wechat = null))
        val input = JsonObject().apply {
            addProperty("purpose", if (deletion) "delete" else purpose("wechat"))
            addProperty("use_wechat_profile", !deletion)
        }
        writeLocked("/auth/wechat/challenges", input.toString(), manualProfile = manualProfile && view.value.accountType == "member")
        vault.wechat ?: throw IOException("微信授权参数缺失")
    }
    suspend fun submitWechat(state: String, code: String) = exclusive {
        ready()
        val challenge = vault.wechat ?: throw IllegalArgumentException("微信授权已失效，请重新发起")
        require(challenge.state == state && challenge.expiresAt > now()) { "微信授权状态不匹配或已过期" }
        require(code.matches(Regex("[A-Za-z0-9_-]{1,256}"))) { "无效的微信授权结果" }
        val deleting = challenge.purpose == "delete"
        val input = JsonObject().apply {
            addProperty("challenge_id", challenge.id); addProperty("state", state); addProperty("code", code)
            if (!deleting) addProperty("use_wechat_profile", true)
        }
        writeLocked(if (deleting) "/me/delete/wechat" else if (challenge.purpose == "login") "/auth/login/wechat" else "/me/identities/wechat", input.toString(), manualProfile = challenge.manualProfile)
        if (deleting) guestLocked()
        deleting
    }
    suspend fun cancelWechat(state: String? = null) = exclusive {
        if (vault.pending == null && (state == null || vault.wechat?.state == state)) commit(vault.copy(wechat = null))
    }
    suspend fun abandonChallenge() = exclusive {
        val pending = vault.pending ?: return@exclusive
        check(pending.request.path in setOf("/auth/sms/challenges", "/auth/wechat/challenges")) {
            "登录、刷新或退出的原请求不能被当作验证码挑战放弃"
        }
        // 只有用户明确确认才放弃发送结果；至少等待原请求 60 秒并保留服务端限流。
        commit(vault.copy(pending = null, sms = null, wechat = null,
            notBefore = maxOf(vault.notBefore, pending.createdAt + 60000)))
    }
    suspend fun recover() = exclusive {
        val pending = vault.pending ?: return@exclusive
        if (now() < pending.notBefore) throw ApiFailure(ApiReply(429, "RATE_LIMITED", retryAfter = (pending.notBefore - now() + 999) / 1000))
        val recoveredPath = pending.continuation?.path ?: pending.request.path
        val logout = recoveredPath == "/auth/logout" || recoveredPath.startsWith("/me/delete/")
        val login = recoveredPath in setOf("/auth/login/phone", "/auth/login/wechat")
        sendPendingLocked()
        if (logout) guestLocked()
        if (login) meLocked()
    }
    suspend fun switchOrigin(origin: String, abandonPending: Boolean) = exclusive {
        check(allowSavedOrigin) { "正式版本不支持切换服务器" }
        val validated = Protocol.origin(origin)
        if (vault.pending != null && !abandonPending) throw PendingOperation()
        commit(VaultState(validated, vault.installationId, vault.generation + 1))
        onAccountCleared()
        guestLocked()
    }
    suspend fun restartInvalidSession() = exclusive {
        check(vault.blocked in setOf("SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED")) {
            "当前状态不能放弃会话"
        }
        commit(VaultState(vault.origin, vault.installationId, vault.generation + 1))
        onAccountCleared()
        guestLocked()
    }
    suspend fun health() = exclusive {
        val reply = api.execute(Protocol.request(vault.origin, "/", null, null))
        ensureSuccess(reply)
    }
    private suspend fun writeLocked(path: String, json: String, mayRefresh: Boolean = true, manualProfile: Boolean = false) {
        if (vault.pending != null) throw PendingOperation()
        val access = if (path == "/auth/logout") null else vault.session?.access
        val request = Protocol.request(vault.origin, path, json, access)
        commit(vault.copy(pending = Pending(request, now(), manualProfile = manualProfile, mayRefresh = mayRefresh)))
        sendPendingLocked()
    }
    private fun continuationMatches(continuation: WriteContinuation, state: VaultState): Boolean =
        continuation.origin == state.origin && continuation.generation == state.generation &&
            continuation.sessionId == state.session?.id &&
            continuation.userId == state.profile?.getAsJsonObject("user")?.get("id")?.asString &&
            continuation.path in Protocol.paths && continuation.path != "/auth/refresh"
    private suspend fun sendPendingLocked() {
        val pending = vault.pending ?: throw PendingOperation()
        pending.continuation?.let {
            if (pending.request.path != "/auth/refresh" || !continuationMatches(it, vault)) {
                commit(vault.copy(pending = null, blocked = "SOURCE_ACCOUNT_CHANGED"))
                throw ApiFailure(ApiReply(409, "SOURCE_ACCOUNT_CHANGED"))
            }
        }
        val reply = api.execute(pending.request) // IOException 保留原始请求，不能换 key。
        if (reply.code == "ACCESS_TOKEN_EXPIRED" && pending.mayRefresh && pending.request.path != "/auth/refresh") {
            // This response definitively rejected the write before application. Atomically replace it
            // with refresh + the authorized continuation, rather than retaining it only on the stack.
            val continuation = WriteContinuation(pending.request.path, pending.request.body!!, pending.manualProfile,
                vault.origin, vault.profile?.getAsJsonObject("user")?.get("id")?.asString.orEmpty(), session().id, vault.generation)
            val refresh = Protocol.request(vault.origin, "/auth/refresh", body("refresh_token" to session().refresh), null)
            commit(vault.copy(pending = Pending(refresh, now(), mayRefresh = false, continuation = continuation)))
            sendPendingLocked()
            return
        }
        if (reply.code != "OK" || reply.status !in 200..299) {
            if (pending.request.path == "/me/profile/wechat" && reply.code == "AUTH_RESULT_EXPIRED") {
                // The optional write's receipt is unavailable; it says nothing about current session validity.
                commit(vault.copy(pending = null, wechatProfile = null))
                throw IllegalStateException("本次微信资料更新结果暂时无法确认。请点击“更新账号信息”核对当前资料，勿重复提交。")
            }
            // 限流在服务端幂等查询之前执行：429 不能证明原请求没有成功。
            val retain = (pending.request.path == "/me/profile/wechat" && reply.status >= 500) || reply.code in setOf("RATE_LIMITED", "AUTH_OPERATION_IN_PROGRESS", "VERIFICATION_IN_PROGRESS", "MERGE_RETRYABLE",
                "AUTH_PROVIDER_UNAVAILABLE", "AUTH_STORAGE_UNAVAILABLE", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED")
            if (pending.request.path == "/me/profile/wechat" && reply.code in setOf("WECHAT_PROFILE_EXPIRED", "WECHAT_PROFILE_INVALID", "WECHAT_PROFILE_CHANGED"))
                commit(vault.copy(wechatProfile = null))
            val retryAt = now() + maxOf(reply.retryAfter, if (retain) 2 else 0) * 1000
            commit(vault.copy(pending = if (retain) pending.copy(notBefore = retryAt) else null,
                notBefore = if (reply.retryAfter > 0) retryAt else vault.notBefore))
            ensureSuccess(reply)
        }
        val data = reply.data
        val requestBody = com.google.gson.JsonParser.parseString(pending.request.body).asJsonObject
        var next = vault.copy(pending = null, blocked = null, notBefore = 0)
        when (pending.request.path) {
            "/auth/guest", "/auth/login/phone", "/auth/login/wechat", "/auth/refresh" -> {
                val access = data.get("access_token")?.asString.orEmpty()
                val refresh = data.get("refresh_token")?.asString.orEmpty()
                if (!access.matches(Regex("[A-Za-z0-9_-]{43}")) || !refresh.matches(Regex("[A-Za-z0-9_-]{43}")) ||
                    data.get("token_type")?.asString != "Bearer") throw IOException("认证结果不完整，保留原请求等待恢复")
                val changed = pending.request.path != "/auth/refresh"
                if (changed && !data.has("user")) throw IOException("认证结果缺少账号资料")
                val profile = if (changed) JsonObject().apply { add("user", data.getAsJsonObject("user").deepCopy()) } else vault.profile
                next = next.copy(session = Session(access, refresh, data.get("session_id").asString), profile = profile,
                    generation = vault.generation + if (changed) 1 else 0,
                    tokenVersion = if (changed) 0 else vault.tokenVersion + 1,
                    sms = if (changed) null else vault.sms, wechat = if (changed) null else vault.wechat,
                    wechatProfile = if (changed) null else vault.wechatProfile)
                if (pending.request.path == "/auth/login/wechat")
                    next = next.copy(wechatProfile = parseWechatProfile(data, next, pending.manualProfile))
                pending.continuation?.let { continuation ->
                    if (!continuationMatches(continuation, next)) {
                        commit(next.copy(blocked = "SOURCE_ACCOUNT_CHANGED"))
                        throw ApiFailure(ApiReply(409, "SOURCE_ACCOUNT_CHANGED"))
                    }
                    // Persist tokens and resumed Pending together. A crash here replays this exact
                    // new request, and never repeats refresh or an already-sent OAuth exchange.
                    next = next.copy(pending = Pending(Protocol.request(next.origin, continuation.path,
                        continuation.body, next.session!!.access), now(), manualProfile = continuation.manualProfile,
                        mayRefresh = false))
                }
                commit(next)
                if (changed) onAccountCleared()
                if (next.pending != null) sendPendingLocked()
                return
            }
            "/auth/logout" -> {
                if (data.get("logged_out")?.asBoolean != true) throw IOException("退出结果不完整")
                next = VaultState(vault.origin, vault.installationId, vault.generation + 1)
            }
            "/me/delete/phone", "/me/delete/wechat" -> {
                if (data.get("deleted")?.asBoolean != true) throw IOException("注销结果不完整，保留原请求等待恢复")
                next = VaultState(vault.origin, vault.installationId, vault.generation + 1)
            }
            "/me/profile", "/me/profile/wechat" -> {
                if (!data.has("user")) throw IOException("资料结果不完整")
                val profile = vault.profile?.deepCopy() ?: JsonObject()
                profile.add("user", data.get("user").deepCopy())
                next = next.copy(profile = profile, wechatProfile = if (pending.request.path == "/me/profile/wechat") null else next.wechatProfile)
            }
            "/auth/sms/challenges", "/auth/wechat/challenges" -> {
                val id = data.get("challenge_id")?.asString.orEmpty()
                if (!id.matches(Regex("[A-Za-z0-9_-]{43}"))) throw IOException("挑战结果不完整")
                val expires = data.get("expires_in")?.asLong?.coerceIn(1, 300) ?: throw IOException("缺少挑战期限")
                val purpose = requestBody.get("purpose").asString
                if (pending.request.path == "/auth/sms/challenges") {
                    val resend = data.get("resend_after")?.asLong?.coerceIn(1, 86400) ?: 60
                    next = next.copy(sms = Challenge(id, purpose, pending.createdAt + expires * 1000, pending.createdAt + resend * 1000,
                        phone = requestBody.get("phone").asString))
                } else {
                    val state = data.get("state")?.asString.orEmpty()
                    require(state.matches(Regex("[A-Za-z0-9_-]{43}")))
                    next = next.copy(wechat = Challenge(id, purpose, pending.createdAt + expires * 1000,
                        state = state, appId = data.get("appid").asString, scope = data.get("scope").asString,
                        useWechatProfile = purpose != "delete" && requestBody.get("use_wechat_profile")?.asBoolean == true,
                        manualProfile = pending.manualProfile))
                }
            }
            "/me/identities/phone", "/me/identities/wechat" -> {
                if (data.get("bound")?.asBoolean != true || !data.has("identities")) throw IOException("绑定结果不完整")
                val profile = vault.profile?.deepCopy() ?: JsonObject()
                profile.add("identities", data.get("identities").deepCopy())
                data.getAsJsonObject("user")?.let { profile.add("user", it.deepCopy()) }
                next = next.copy(profile = profile, sms = null, wechat = null)
                if (pending.request.path == "/me/identities/wechat")
                    next = next.copy(wechatProfile = parseWechatProfile(data, next, pending.manualProfile))
            }
        }
        commit(next)
        if (pending.request.path == "/auth/logout" || pending.request.path.startsWith("/me/delete/")) onAccountCleared()
    }
    private fun ensureSuccess(reply: ApiReply) {
        if (reply.code == "OK" && reply.status in 200..299) return
        if (reply.code in setOf("ACCOUNT_DISABLED", "SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED"))
            commit(vault.copy(blocked = reply.code))
        if (reply.retryAfter > 0) commit(vault.copy(notBefore = now() + reply.retryAfter * 1000))
        throw ApiFailure(reply)
    }
}
