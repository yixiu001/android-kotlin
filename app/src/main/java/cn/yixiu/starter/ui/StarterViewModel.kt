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
    private val result = runCatching { app.engine }
    val engine = result.getOrNull()
    val account = engine?.state ?: MutableStateFlow(AccountView(BuildConfig.API_BASE_URL, privateHidden = true))
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow(if (engine == null) "本地账号数据无法读取。请保留应用数据，检查设备密钥或产品接口配置。" else "")
    val logs = app.api.logs
    init {
        if (engine != null) run("已连接") { engine.start() }
        viewModelScope.launch { app.notice.collect { if (it.isNotEmpty()) { message.value = it; app.notice.value = "" } } }
    }
    fun launchTestTool(block: suspend CoroutineScope.() -> Unit): Job {
        check(BuildConfig.DEBUG && BuildConfig.TEST_TOOLS_ENABLED)
        return viewModelScope.launch(block = block)
    }
    fun dispatch(action: UiAction) {
        val engine = engine ?: return
        when (action.name) {
            "start" -> run("已连接") { engine.start() }
            "me" -> run("账号信息已更新") { engine.me() }
            "refresh" -> run("会话已安全更新") { engine.refresh() }
            "recover" -> run("原请求已恢复") { engine.recover() }
            "abandonChallenge" -> run("已放弃上次挑战结果；请等待冷却结束后重新获取验证码或微信授权。") { engine.abandonChallenge() }
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
            "wechat", "deleteWechat" -> run("") {
                check(BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) { "微信登录待配置移动应用 AppID" }
                val bridge = WechatBridge(app)
                bridge.checkAvailable()
                val challenge = engine.requestWechat(useProfile = action.value == "true", deletion = action.name == "deleteWechat")
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
    private fun run(success: String, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; message.value = ""
        viewModelScope.launch {
            try { block(); message.value = success }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message.value = friendlyError(error) }
            finally { busy.value = false }
        }
    }
}

fun friendlyError(error: Exception): String = when (error) {
    is PendingOperation -> "上次操作尚未确认，请点击“恢复原请求”，不要重复提交。"
    is ApiFailure -> when (error.reply.code) {
        "AUTH_CONFIGURATION_ERROR" -> "服务端尚未开通此登录方式，请完成供应商配置后再试。"
        "AUTH_PROVIDER_UNAVAILABLE" -> "验证服务暂不可用，发送结果可能未确认，请先恢复原请求。"
        "AUTH_STORAGE_UNAVAILABLE" -> "账号服务暂不可用，已保留恢复材料。"
        "RATE_LIMITED" -> "请求过于频繁，请在 " + error.reply.retryAfter.coerceAtLeast(1) + " 秒后重试。"
        "IDENTITY_ALREADY_BOUND" -> "此手机号或微信已绑定其他正式账号，无法合并。"
        "IDENTITY_TYPE_ALREADY_BOUND" -> "当前账号已绑定另一手机号或微信，暂不支持换绑。"
        "IDENTITY_REGISTRATION_BLOCKED" -> {
            val until = error.reply.data.get("available_at")?.takeUnless { it.isJsonNull }?.asString
            val display = runCatching { java.time.Instant.parse(until).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
            "此身份处于注销后的 30 天限制期，暂不能注册或绑定。" + (display?.let { "可用时间：$it。" } ?: "")
        }
        "VERIFICATION_INVALID" -> "验证码或授权无效、过期，请检查或重新获取。"
        "ACCOUNT_DISABLED" -> "账号已被停用，请联系管理员。"
        "SESSION_INVALID", "REFRESH_TOKEN_INVALID" -> "会话已失效，请确认后重新开始并验证原有身份。"
        "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED" -> "原操作结果已无法恢复，请确认账号状态后重新验证身份。"
        "AUTH_OPERATION_IN_PROGRESS", "VERIFICATION_IN_PROGRESS", "MERGE_RETRYABLE" -> "服务器仍在处理原操作，请稍后恢复原请求。"
        else -> "请求未完成：" + error.reply.code
    }
    is java.io.IOException -> "网络或存储操作未完成。若有待确认操作，请恢复原请求；不要重复获取验证码。"
    is IllegalArgumentException, is IllegalStateException -> error.message ?: "当前操作不可用"
    else -> "操作未完成，请稍后重试。"
}
