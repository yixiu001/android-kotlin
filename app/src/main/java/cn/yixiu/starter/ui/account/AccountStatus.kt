package cn.yixiu.starter.ui.account

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*

@Composable internal fun AccountStatus(account: AccountView, busy: Boolean, message: String,
                           state: AccountUiState, action: (UiAction) -> Unit) {
    if (busy) LoadingState("正在处理账号请求")
    if (message.isNotBlank() && (account.pendingPath != null || account.privateHidden || account.blocked != null)) {
        ErrorState(message)
    }
    if (account.pendingPath != null || account.privateHidden) InfoCard {
        Text(if (account.pendingPath?.startsWith("/me/delete/") == true) "注销待确认" else if (account.pendingPath == "/auth/logout") "退出待确认" else "账号需要处理",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (account.pendingPath != null) "已安全保存原请求。恢复时会核对同一次操作，避免重复提交。"
            else "当前账号信息暂不展示，请按提示恢复会话。")
        if (account.pendingPath != null)
            Button(onClick = { action(UiAction("recover")) }, enabled = !busy) { Text("恢复原请求") }
        if (account.pendingPath in setOf("/auth/sms/challenges", "/auth/wechat/challenges"))
            TextButton(onClick = { state.confirmChallenge = true }, enabled = !busy) { Text("放弃此次挑战结果") }
        if (account.blocked in setOf("SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED"))
            OutlinedButton(onClick = { state.confirmReset = true }, enabled = !busy) { Text("重新开始") }
    }
}
