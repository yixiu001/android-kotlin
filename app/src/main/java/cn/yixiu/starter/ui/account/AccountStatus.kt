package cn.yixiu.starter.ui.account

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

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
    val copy = LocalResources.current
    val needsAttention = account.pendingPath != null || account.privateHidden || account.blocked != null
    if (!needsAttention) {
        if (state.now < account.notBefore) Text(copy.getString(R.string.wait_seconds, ((account.notBefore - state.now + 999) / 1000)))
        if (!busy && message.isNotBlank()) ErrorState(message) { action(UiAction("start")) }
        return
    }
    OutlinedButton(onClick = { state.showRecovery = true }, enabled = !busy) {
        Text(if (busy) copy.getString(R.string.confirming) else copy.getString(R.string.account_attention))
    }
    if (state.showRecovery && !state.confirmChallenge && !state.confirmReset) {
        val logout = account.pendingPath == "/auth/logout"
        val deleting = account.pendingPath?.startsWith("/me/delete/") == true
        AppDialog(onDismissRequest = { state.showRecovery = false },
            title = { Text(when { logout -> copy.getString(R.string.logout_pending); deleting -> copy.getString(R.string.delete_pending); else -> copy.getString(R.string.account_confirm_title) }) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when {
                    logout -> copy.getString(R.string.logout_pending_body)
                    deleting -> copy.getString(R.string.delete_pending_body)
                    account.pendingPath != null -> copy.getString(R.string.operation_pending_body)
                    account.blocked == "ACCOUNT_DISABLED" -> copy.getString(R.string.account_disabled_body)
                    else -> message.ifBlank { copy.getString(R.string.account_unavailable_body) }
                })
                if (account.pendingPath != null) Button(enabled = !busy && state.now >= account.notBefore, onClick = {
                    state.showRecovery = false; action(UiAction("recover"))
                }) { Text(when { logout -> copy.getString(R.string.confirm_logout_result); deleting -> copy.getString(R.string.confirm_delete_result); else -> copy.getString(R.string.continue_operation) }) }
                if (state.now < account.notBefore) Text(copy.getString(R.string.wait_seconds, ((account.notBefore - state.now + 999) / 1000)))
                if (account.pendingPath in setOf("/auth/sms/challenges", "/auth/wechat/challenges"))
                    TextButton(enabled = !busy, onClick = { state.showRecovery = false; state.confirmChallenge = true }) { Text(copy.getString(R.string.abandon_verification)) }
                if (account.blocked in setOf("SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED"))
                    TextButton(enabled = !busy, onClick = { state.showRecovery = false; state.confirmReset = true }) { Text(copy.getString(R.string.relogin_action)) }
            } },
            confirmButton = { TextButton(onClick = { state.showRecovery = false }) { Text(copy.getString(R.string.handle_later)) } })
    }
}
