package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.yixiu.starter.*
import cn.yixiu.starter.core.*
import cn.yixiu.starter.wechat.WechatBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class StarterViewModel(application: Application) : AndroidViewModel(application) {
    private val copy = application.resources
    private val app = application as StarterApplication
    private val result = runCatching {
        check(app.preferences.hasConsent()) { copy.getString(R.string.consent_required) }
        app.engine
    }
    val engine = result.getOrNull()
    val account = engine?.state ?: MutableStateFlow(AccountView(BuildConfig.API_BASE_URL, privateHidden = true))
    val busy = MutableStateFlow(false)
    val activeAction = MutableStateFlow("")
    val message = MutableStateFlow("")
    val accountError = MutableStateFlow(if (engine == null) copy.getString(R.string.vault_unreadable) else "")
    val phoneError = MutableStateFlow("")
    val profileSave = MutableStateFlow(ProfileSaveFeedback())
    val logs = app.api.logs
    init {
        if (engine != null) run("", "start", quiet = true) { engine.start() }
        viewModelScope.launch { app.notice.collect { if (it.isNotEmpty()) { message.value = it; app.notice.value = "" } } }
    }
    fun launchTestTool(block: suspend CoroutineScope.() -> Unit): Job {
        check(BuildConfig.DEBUG && BuildConfig.TEST_TOOLS_ENABLED && app.preferences.hasConsent())
        return viewModelScope.launch(block = block)
    }
    fun dispatch(action: UiAction) {
        if (!app.preferences.hasConsent()) return
        val engine = engine ?: return
        if (busy.value) return
        activeAction.value = action.name
        when (action.name) {
            "start" -> run(copy.getString(R.string.connected)) { engine.start() }
            "me" -> run(copy.getString(R.string.account_refreshed)) { engine.me() }
            "refresh" -> run(copy.getString(R.string.session_refreshed)) { engine.refresh() }
            "recover" -> run(copy.getString(R.string.operation_recovered)) { engine.recover() }
            "recoverProfile" -> {
                if (account.value.pendingPath !in setOf("/me/profile", "/auth/refresh")) return
                val before = account.value
                run(copy.getString(R.string.profile_updated)) {
                    engine.recover()
                    // 保存状态快照可能早于后台失败；普通 refresh 成功不能证明资料已写入。
                    check(profileTargetMatches(action.value, before, account.value)) {
                        copy.getString(R.string.profile_save_not_applied)
                    }
                }
            }
            "abandonChallenge" -> run(copy.getString(R.string.challenge_abandoned)) { engine.abandonChallenge() }
            "logout" -> run(copy.getString(R.string.logged_out)) { engine.logout() }
            "profile" -> run(copy.getString(R.string.profile_updated)) {
                val input = com.google.gson.JsonParser.parseString(action.value).asJsonObject
                engine.updateProfile(input.get("nickname")?.asString, input.get("avatar")?.asString)
            }
            "sms", "deleteSms" -> run(copy.getString(R.string.sms_sent)) {
                check(BuildConfig.SMS_ENABLED) { copy.getString(R.string.sms_disabled) }
                val phone = action.value.trim().let { if (it.startsWith("+86")) it else "+86" + it }
                engine.requestSms(phone, deletion = action.name == "deleteSms")
            }
            "phone" -> run("") {
                val deleted = engine.submitPhone(action.value.trim()); engine.me()
                app.notice.value = if (deleted) copy.getString(R.string.account_deleted) else copy.getString(R.string.identity_verified)
            }
            "skipWechatProfile" -> run(copy.getString(R.string.wechat_profile_skipped)) { engine.skipWechatProfile() }
            "confirmWechatProfile" -> run(copy.getString(R.string.wechat_profile_updated)) {
                val input = com.google.gson.JsonParser.parseString(action.value).asJsonObject
                engine.confirmWechatProfile(input["use_nickname"].asBoolean, input["use_avatar"].asBoolean)
            }
            "wechat", "deleteWechat" -> run("") {
                check(BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) { copy.getString(R.string.wechat_appid_missing) }
                val bridge = WechatBridge(app)
                bridge.checkAvailable()
                val challenge = engine.requestWechat(deletion = action.name == "deleteWechat")
                try { bridge.launch(challenge) } catch (error: Exception) { engine.cancelWechat(); throw error }
            }
            "health" -> run(copy.getString(R.string.health_passed)) { engine.health() }
            "environment" -> run(copy.getString(R.string.environment_changed)) {
                val origin = Protocol.origin(action.value)
                app.api.clearLogs()
                engine.switchOrigin(origin, true)
            }
            "restartInvalid" -> run(copy.getString(R.string.guest_restarted)) { engine.restartInvalidSession() }
        }
    }
    fun consumeMessage(value: String) { if (message.value == value) message.value = "" }

    private fun run(success: String, operation: String = activeAction.value, quiet: Boolean = false, block: suspend () -> Unit) {
        if (busy.value || !app.preferences.hasConsent()) return
        val tracksProfile = operation in setOf("profile", "recoverProfile") ||
            (operation == "recover" && profileSave.value.status == ProfileSaveStatus.Pending &&
                profileSave.value.origin == account.value.origin && profileSave.value.userId == account.value.userId)
        val attempt = if (tracksProfile) ProfileSaveFeedback(profileSave.value.attempt + 1, account.value.origin,
            account.value.userId, ProfileSaveStatus.Saving) else null
        attempt?.let { profileSave.value = it }
        busy.value = true; activeAction.value = operation; message.value = ""; accountError.value = ""; phoneError.value = ""
        viewModelScope.launch {
            try {
                block()
                attempt?.let { profileSave.value = it.copy(status = ProfileSaveStatus.Saved) }
                message.value = success
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val detail = friendlyError(error, copy)
                attempt?.let { profileSave.value = it.copy(status = if (account.value.pendingPath != null)
                    ProfileSaveStatus.Pending else ProfileSaveStatus.Failed, error = detail) }
                // 后台连接失败只留在账号页，不能打扰本地工具；用户操作结果则提示一次。
                if (operation in setOf("start", "me", "recover")) accountError.value = detail
                if (operation == "phone" && error is ApiFailure && error.reply.code == "VERIFICATION_INVALID")
                    phoneError.value = copy.getString(R.string.code_invalid)
                else if (!quiet && !tracksProfile) message.value = detail
            }
            finally { busy.value = false; activeAction.value = "" }
        }
    }
}

fun friendlyError(error: Exception, copy: android.content.res.Resources): String = when (error) {
    is PendingOperation -> copy.getString(R.string.pending_operation_error)
    is ApiFailure -> when (error.reply.code) {
        "AUTH_CONFIGURATION_ERROR" -> copy.getString(R.string.provider_config_error)
        "AUTH_PROVIDER_UNAVAILABLE" -> copy.getString(R.string.provider_pending_error)
        "AUTH_STORAGE_UNAVAILABLE" -> copy.getString(R.string.storage_error)
        "RATE_LIMITED" -> copy.getString(R.string.rate_limit_prefix) + error.reply.retryAfter.coerceAtLeast(1) + copy.getString(R.string.rate_limit_suffix)
        "IDENTITY_ALREADY_BOUND" -> copy.getString(R.string.identity_conflict)
        "IDENTITY_TYPE_ALREADY_BOUND" -> copy.getString(R.string.identity_type_conflict)
        "IDENTITY_REGISTRATION_BLOCKED" -> {
            val until = error.reply.data.get("available_at")?.takeUnless { it.isJsonNull }?.asString
            val display = runCatching { java.time.Instant.parse(until).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
            copy.getString(R.string.identity_blocked) + (display?.let { copy.getString(R.string.identity_available_at, it) } ?: "")
        }
        "WECHAT_PROFILE_EXPIRED" -> copy.getString(R.string.wechat_profile_expired_error)
        "WECHAT_PROFILE_INVALID" -> copy.getString(R.string.wechat_profile_invalid_error)
        "WECHAT_PROFILE_CHANGED" -> copy.getString(R.string.wechat_profile_changed_error)
        "VERIFICATION_INVALID" -> copy.getString(R.string.verification_invalid)
        "ACCOUNT_DISABLED" -> copy.getString(R.string.account_disabled)
        "SESSION_INVALID", "REFRESH_TOKEN_INVALID" -> copy.getString(R.string.session_invalid)
        "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED" -> copy.getString(R.string.auth_result_expired)
        "AUTH_OPERATION_IN_PROGRESS", "VERIFICATION_IN_PROGRESS", "MERGE_RETRYABLE" -> copy.getString(R.string.operation_pending_error)
        else -> copy.getString(R.string.operation_unavailable)
    }
    is java.io.IOException -> copy.getString(R.string.network_error)
    is IllegalArgumentException, is IllegalStateException -> error.message ?: copy.getString(R.string.operation_invalid)
    else -> copy.getString(R.string.operation_failed)
}
