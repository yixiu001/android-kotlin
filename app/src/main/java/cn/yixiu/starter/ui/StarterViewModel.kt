package cn.yixiu.starter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.yixiu.starter.*
import cn.yixiu.starter.core.*
import cn.yixiu.starter.wechat.WechatBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class StarterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StarterApplication
    private val result = runCatching {
        check(app.preferences.hasConsent()) { "请先阅读并同意隐私说明" }
        app.engine
    }
    val engine = result.getOrNull()
    val account = engine?.state ?: MutableStateFlow(AccountView(BuildConfig.API_BASE_URL, privateHidden = true))
    val busy = MutableStateFlow(false)
    val activeAction = MutableStateFlow("")
    val message = MutableStateFlow("")
    val accountError = MutableStateFlow(if (engine == null) "本地账号数据暂时无法读取，请保留应用数据并联系支持。" else "")
    val phoneError = MutableStateFlow("")
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
            "start" -> run("已连接") { engine.start() }
            "me" -> run("账号信息已更新") { engine.me() }
            "refresh" -> run("登录状态已更新") { engine.refresh() }
            "recover" -> run("上次操作已确认") { engine.recover() }
            "abandonChallenge" -> run("已放弃上次验证，请在倒计时结束后重试") { engine.abandonChallenge() }
            "logout" -> run("已退出，正在使用新游客账号") { engine.logout() }
            "profile" -> run("资料已更新") {
                val input = com.google.gson.JsonParser.parseString(action.value).asJsonObject
                engine.updateProfile(input.get("nickname")?.asString, input.get("avatar")?.asString)
            }
            "sms", "deleteSms" -> run("验证码已发送，请查看手机短信") {
                check(BuildConfig.SMS_ENABLED) { "本产品暂未开放短信登录" }
                val phone = action.value.trim().let { if (it.startsWith("+86")) it else "+86" + it }
                engine.requestSms(phone, deletion = action.name == "deleteSms")
            }
            "phone" -> run("") {
                val deleted = engine.submitPhone(action.value.trim()); engine.me()
                app.notice.value = if (deleted) "账号已注销，当前为新游客；原手机号和微信 30 天内不能注册或绑定。" else "身份验证成功"
            }
            "skipWechatProfile" -> run("已跳过，登录成功") { engine.skipWechatProfile() }
            "confirmWechatProfile" -> run("微信资料已更新") {
                val input = com.google.gson.JsonParser.parseString(action.value).asJsonObject
                engine.confirmWechatProfile(input["use_nickname"].asBoolean, input["use_avatar"].asBoolean)
            }
            "wechat", "deleteWechat" -> run("") {
                check(BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) { "微信登录待配置移动应用 AppID" }
                val bridge = WechatBridge(app)
                bridge.checkAvailable()
                val challenge = engine.requestWechat(deletion = action.name == "deleteWechat")
                try { bridge.launch(challenge) } catch (error: Exception) { engine.cancelWechat(); throw error }
            }
            "health" -> run("服务器健康检查通过") { engine.health() }
            "environment" -> run("已连接新的测试环境") {
                val origin = Protocol.origin(action.value)
                app.api.clearLogs()
                engine.switchOrigin(origin, true)
            }
            "restartInvalid" -> run("已建立新游客账号，可重新验证身份") { engine.restartInvalidSession() }
        }
    }
    fun consumeMessage(value: String) { if (message.value == value) message.value = "" }

    private fun run(success: String, operation: String = activeAction.value, quiet: Boolean = false, block: suspend () -> Unit) {
        if (busy.value || !app.preferences.hasConsent()) return
        busy.value = true; activeAction.value = operation; message.value = ""; accountError.value = ""; phoneError.value = ""
        viewModelScope.launch {
            try { block(); message.value = success }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val detail = friendlyError(error)
                // 后台连接失败只留在账号页，不能打扰本地工具；用户操作结果则提示一次。
                if (operation in setOf("start", "me", "recover")) accountError.value = detail
                if (operation == "phone" && error is ApiFailure && error.reply.code == "VERIFICATION_INVALID")
                    phoneError.value = "验证码无效或已过期，请重新输入或获取"
                else if (!quiet) message.value = detail
            }
            finally { busy.value = false; activeAction.value = "" }
        }
    }
}

fun friendlyError(error: Exception): String = when (error) {
    is PendingOperation -> "上次操作尚未确认，请到账号与安全继续处理"
    is ApiFailure -> when (error.reply.code) {
        "AUTH_CONFIGURATION_ERROR" -> "此登录方式暂不可用，请稍后重试或联系支持"
        "AUTH_PROVIDER_UNAVAILABLE" -> "验证结果暂未确认，请到账号与安全继续处理"
        "AUTH_STORAGE_UNAVAILABLE" -> "账号服务暂不可用，请稍后重试"
        "RATE_LIMITED" -> "请求过于频繁，请在 " + error.reply.retryAfter.coerceAtLeast(1) + " 秒后重试。"
        "IDENTITY_ALREADY_BOUND" -> "此手机号或微信已绑定其他正式账号，无法合并。"
        "IDENTITY_TYPE_ALREADY_BOUND" -> "当前账号已绑定另一手机号或微信，暂不支持换绑。"
        "IDENTITY_REGISTRATION_BLOCKED" -> {
            val until = error.reply.data.get("available_at")?.takeUnless { it.isJsonNull }?.asString
            val display = runCatching { java.time.Instant.parse(until).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
            "此身份处于注销后的 30 天限制期，暂不能注册或绑定。" + (display?.let { "可用时间：$it。" } ?: "")
        }
        "WECHAT_PROFILE_EXPIRED" -> "本次微信资料选择已过期，登录仍有效。可在编辑资料中修改昵称或选择内置头像。"
        "WECHAT_PROFILE_INVALID" -> "本次微信资料选择已失效，登录仍有效。"
        "WECHAT_PROFILE_CHANGED" -> "资料已在其他操作中改变，本次微信资料选择已结束。请更新账号信息查看。"
        "VERIFICATION_INVALID" -> "验证码或授权无效、过期，请检查或重新获取。"
        "ACCOUNT_DISABLED" -> "账号已被停用，请联系管理员。"
        "SESSION_INVALID", "REFRESH_TOKEN_INVALID" -> "登录已失效，请到账号与安全重新登录"
        "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED" -> "原操作结果已无法恢复，请确认账号状态后重新验证身份。"
        "AUTH_OPERATION_IN_PROGRESS", "VERIFICATION_IN_PROGRESS", "MERGE_RETRYABLE" -> "上次操作仍在处理，请稍后到账号与安全继续确认"
        else -> "操作暂未完成，请稍后重试或联系支持"
    }
    is java.io.IOException -> "连接未完成，请稍后重试；结果待确认时请到账号与安全处理"
    is IllegalArgumentException, is IllegalStateException -> error.message ?: "当前操作不可用"
    else -> "操作未完成，请稍后重试。"
}
