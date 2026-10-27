package cn.yixiu.starter.wechat

import android.content.Context
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.core.Challenge
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.mm.opensdk.utils.ILog

class WechatBridge(private val context: Context) {
    private fun api() = WXAPIFactory.createWXAPI(context, BuildConfig.WECHAT_APP_ID, true).apply {
        // SDK 调试输出可能包含授权参数；统一关闭应用侧输出。
        setLogImpl(SilentWechatLog)
        registerApp(BuildConfig.WECHAT_APP_ID)
    }
    fun checkAvailable() {
        check(BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) { "微信登录尚未配置" }
        val api = api()
        try { check(api.isWXAppInstalled) { "请先安装微信客户端" } } finally { api.detach() }
    }
    fun launch(challenge: Challenge) {
        require(challenge.appId == BuildConfig.WECHAT_APP_ID && challenge.scope == "snsapi_userinfo") {
            "服务端微信 AppID 与客户端配置不一致"
        }
        val api = api()
        try {
            val request = SendAuth.Req().apply { scope = challenge.scope; state = challenge.state }
            check(api.sendReq(request)) { "无法启动微信授权，请检查应用包名和签名配置" }
        } finally { api.detach() }
    }
}
object SilentWechatLog : ILog {
    override fun v(tag: String?, message: String?) {}
    override fun d(tag: String?, message: String?) {}
    override fun i(tag: String?, message: String?) {}
    override fun w(tag: String?, message: String?) {}
    override fun e(tag: String?, message: String?) {}
}
