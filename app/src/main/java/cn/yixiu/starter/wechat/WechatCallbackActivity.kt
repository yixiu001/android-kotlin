package cn.yixiu.starter.wechat

import cn.yixiu.starter.R

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
        if (!(application as StarterApplication).preferences.hasConsent() ||
            !BuildConfig.WECHAT_ENABLED || BuildConfig.WECHAT_APP_ID.isBlank()) { finish(); return }
        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECHAT_APP_ID, true).apply { setLogImpl(SilentWechatLog) }
        handle(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    private fun handle(intent: Intent) {
        if (!(application as StarterApplication).preferences.hasConsent()) { finish(); return }
        try { if (api?.handleIntent(intent, this) != true) finish() } catch (_: Exception) { finish() }
    }
    override fun onReq(request: BaseReq?) { finish() }
    override fun onResp(response: BaseResp?) {
        val app = application as StarterApplication
        if (response !is SendAuth.Resp || !app.preferences.hasConsent() || app.callbackBusy.value) { finish(); return }
        app.callbackBusy.value = true
        app.scope.launch {
            try {
                if (response.errCode == BaseResp.ErrCode.ERR_OK) {
                    val deleted = app.engine.submitWechat(response.state.orEmpty(), response.code.orEmpty())
                    app.engine.me()
                    app.notice.value = if (deleted) getString(R.string.account_deleted) else getString(R.string.wechat_verified)
                } else {
                    // 只清除匹配的授权；旧取消回调不能影响较新的授权。
                    app.engine.cancelWechat(response.state.orEmpty())
                    app.notice.value = if (response.errCode == BaseResp.ErrCode.ERR_USER_CANCEL) getString(R.string.wechat_cancelled) else getString(R.string.wechat_incomplete)
                }
            } catch (error: Exception) { app.notice.value = friendlyError(error, resources) }
            finally { app.callbackBusy.value = false }
        }
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
    override fun onDestroy() { api?.detach(); super.onDestroy() }
}
