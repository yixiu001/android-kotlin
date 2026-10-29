package cn.yixiu.starter.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*

/** 只在账号页呈现；恢复必须由用户发起，不影响本地工具，也不改写原请求。 */
@Composable internal fun AccountStatus(account: AccountView, busy: Boolean, message: String,
                                      state: AccountUiState, action: (UiAction) -> Unit) {
    val needsAttention = account.pendingPath != null || account.privateHidden || account.blocked != null
    if (!needsAttention) {
        if (state.now < account.notBefore) Text("请等待 ${((account.notBefore - state.now + 999) / 1000)} 秒后重试")
        if (!busy && message.isNotBlank()) ErrorState(message) { action(UiAction("start")) }
        return
    }
    OutlinedButton(onClick = { state.showRecovery = true }, enabled = !busy) {
        Text(if (busy) "正在确认…" else "处理账号状态")
    }
    if (state.showRecovery && !state.confirmChallenge && !state.confirmReset) {
        val logout = account.pendingPath == "/auth/logout"
        val deleting = account.pendingPath?.startsWith("/me/delete/") == true
        AlertDialog(onDismissRequest = { state.showRecovery = false },
            title = { Text(when { logout -> "退出待确认"; deleting -> "注销待确认"; else -> "账号操作需要确认" }) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when {
                    logout -> "上次退出的结果还未确认。继续确认会查询或完成同一次退出，不会重复提交新的退出操作。"
                    deleting -> "上次注销的结果还未确认。请继续确认结果；确认前暂不展示原账号资料。"
                    account.pendingPath != null -> "上次操作的结果还未确认。请继续完成这次操作，避免重复发送验证码或提交登录。"
                    account.blocked == "ACCOUNT_DISABLED" -> "当前账号已停用，请联系支持。"
                    else -> message.ifBlank { "登录状态暂不可用。已保留本地账号数据，请按提示处理或联系支持。" }
                })
                if (account.pendingPath != null) Button(enabled = !busy && state.now >= account.notBefore, onClick = {
                    state.showRecovery = false; action(UiAction("recover"))
                }) { Text(when { logout -> "继续确认退出"; deleting -> "继续确认注销"; else -> "继续上次操作" }) }
                if (state.now < account.notBefore) Text("请等待 ${((account.notBefore - state.now + 999) / 1000)} 秒后重试")
                if (account.pendingPath in setOf("/auth/sms/challenges", "/auth/wechat/challenges"))
                    TextButton(enabled = !busy, onClick = { state.showRecovery = false; state.confirmChallenge = true }) { Text("放弃上次验证") }
                if (account.blocked in setOf("SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED"))
                    TextButton(enabled = !busy, onClick = { state.showRecovery = false; state.confirmReset = true }) { Text("重新登录") }
            } },
            confirmButton = { TextButton(onClick = { state.showRecovery = false }) { Text("稍后处理") } })
    }
}
