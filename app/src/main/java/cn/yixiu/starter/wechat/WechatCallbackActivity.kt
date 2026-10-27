package cn.yixiu.starter.wechat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import cn.yixiu.starter.*
import cn.yixiu.starter.ui.friendlyError
import com.tencent.mm.opensdk.modelbase.*
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.*
import kotlinx.coroutines.launch

class WechatCallbackActivity : Activity(), IWXAPIEventHandler {
    private var api: IWXAPI? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.WECHAT_ENABLED || BuildConfig.WECHAT_APP_ID.isBlank()) { finish(); return }
        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECHAT_APP_ID, true).apply { setLogImpl(SilentWechatLog) }
        handle(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    private fun handle(intent: Intent) {
        try { if (api?.handleIntent(intent, this) != true) finish() } catch (_: Exception) { finish() }
    }
    override fun onReq(request: BaseReq?) { finish() }
    override fun onResp(response: BaseResp?) {
        val app = application as StarterApplication
        if (response !is SendAuth.Resp) { finish(); return }
        app.scope.launch {
            try {
                if (response.errCode == BaseResp.ErrCode.ERR_OK) {
                    app.engine.submitWechat(response.state.orEmpty(), response.code.orEmpty())
                    app.engine.me()
                    app.notice.value = "微信身份验证成功"
                } else {
                    // 取消/错误回调不清除可能属于较新授权的 state，也绝不提交 code。
                    app.notice.value = if (response.errCode == BaseResp.ErrCode.ERR_USER_CANCEL) "已取消微信授权" else "微信授权未完成，请重试"
                }
            } catch (error: Exception) { app.notice.value = friendlyError(error) }
        }
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
    override fun onDestroy() { api?.detach(); super.onDestroy() }
}
