package cn.yixiu.starter.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*

@Composable internal fun AccountDialogs(account: AccountView, busy: Boolean,
                                        state: AccountUiState, action: (UiAction) -> Unit) {
    val now = state.now
    if (state.editProfile && !account.privateHidden) ProfileEditor(account, busy, action) { state.editProfile = false }
    account.wechatProfile?.let { preview ->
        WechatProfileDialog(preview, busy || account.pendingPath != null, now, action)
    }
    if (state.confirmDelete) AlertDialog(onDismissRequest = { state.confirmDelete = false },
        title = { Text("注销账号？") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("验证成功后立即注销，无法撤销。个人资料将被清除，所有设备退出，当前设备进入新游客账号。")
            Text("已绑定的手机号和微信 30 天内不能注册或绑定其他账号。期满可创建新账号，旧数据不恢复。")
            Text("请验证当前账号已绑定的身份：")
            if (account.phone.isNotBlank()) OutlinedButton(enabled = BuildConfig.SMS_ENABLED, onClick = {
                state.confirmDelete = false; state.phoneForm = false; state.deletePhoneForm = true
            }) { Text("通过手机号验证") }
            if (account.wechatBound) OutlinedButton(enabled = BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank(), onClick = {
                state.confirmDelete = false; action(UiAction("deleteWechat"))
            }) { Text("验证微信并立即注销") }
        } },
        confirmButton = { TextButton(onClick = { state.confirmDelete = false }) { Text("暂不注销") } })
    if (state.confirmLogout) AlertDialog(onDismissRequest = { state.confirmLogout = false },
        title = { Text("退出当前账号？") },
        text = { Text("仅退出这台设备。确认后清除本地私有缓存，并进入新的游客账号；未绑定的游客可能无法找回。") },
        confirmButton = { TextButton(onClick = { state.confirmLogout = false; action(UiAction("logout")) }) { Text("确认退出") } },
        dismissButton = { TextButton(onClick = { state.confirmLogout = false }) { Text("取消") } })
    if (state.confirmReset) AlertDialog(onDismissRequest = { state.confirmReset = false },
        title = { Text("放弃本地失效会话？") },
        text = { Text("原凭证和恢复材料将被清除。未绑定游客可能无法找回，正式账号可以重新验证手机号或微信登录。") },
        confirmButton = { TextButton(onClick = { state.confirmReset = false; action(UiAction("restartInvalid")) }) { Text("确认重新开始") } },
        dismissButton = { TextButton(onClick = { state.confirmReset = false }) { Text("取消") } })
    if (state.confirmChallenge) AlertDialog(onDismissRequest = { state.confirmChallenge = false },
        title = { Text("放弃此次挑战结果？") },
        text = { Text("上次短信可能已经发送。放弃后需等待冷却结束，再手动获取新的验证码或微信授权；旧验证码不再用于本次登录。") },
        confirmButton = { TextButton(onClick = { state.confirmChallenge = false; action(UiAction("abandonChallenge")) }) { Text("确认放弃") } },
        dismissButton = { TextButton(onClick = { state.confirmChallenge = false }) { Text("取消") } })
}
