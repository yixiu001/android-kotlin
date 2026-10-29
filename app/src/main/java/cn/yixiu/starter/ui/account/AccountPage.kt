package cn.yixiu.starter.ui.account

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*

@Composable internal fun AccountPage(account: AccountView, actionable: Boolean,
                     state: AccountUiState, action: (UiAction) -> Unit, activeAction: String = "",
                     smsEnabled: Boolean = BuildConfig.SMS_ENABLED,
                     wechatEnabled: Boolean = BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank(),
                     debug: Boolean = BuildConfig.DEBUG, phoneError: String = "") {
    val copy = LocalResources.current
    val now = state.now
    if (!account.privateHidden) {
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                key(account.userId) { UserAvatar(account.avatar, account.avatarUrl) }
                Text(account.nickname.ifBlank { if (account.accountType == "member") copy.getString(R.string.account_mine) else copy.getString(R.string.account_guest) },
                    modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Text(if (account.accountType == "member") copy.getString(R.string.account_two_methods) else
                copy.getString(R.string.account_guest_help), lineHeight = 24.sp)
            if (account.userId.isNotBlank()) Text(copy.getString(R.string.account_id_prefix) + account.userId,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (account.hasSession) {
                TextButton(onClick = { state.editProfile = true }, enabled = actionable) { Text(copy.getString(R.string.edit_profile_action)) }
                HorizontalDivider()
                if (account.accountType == "member") {
                    Text(copy.getString(R.string.phone_prefix) + account.phone.ifBlank { copy.getString(R.string.identity_unbound) })
                    Text(copy.getString(R.string.wechat_prefix) + if (account.wechatBound) copy.getString(R.string.identity_bound) else copy.getString(R.string.identity_unbound))
                    Text(copy.getString(R.string.identity_binding_rule), style = MaterialTheme.typography.bodySmall)
                }
                if (account.phone.isBlank() && showLoginEntry(smsEnabled, debug)) Button(onClick = { state.phoneForm = true }, modifier = Modifier.fillMaxWidth(),
                    enabled = actionable && smsEnabled) {
                    Text(if (!smsEnabled) copy.getString(R.string.sms_unconfigured) else if (account.accountType == "member") copy.getString(R.string.bind_phone) else copy.getString(R.string.login_phone))
                }
                if (!account.wechatBound && showLoginEntry(wechatEnabled, debug)) OutlinedButton(onClick = { action(UiAction("wechat")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = actionable && wechatEnabled) {
                    Text(if (activeAction == "wechat") copy.getString(R.string.opening_wechat) else if (!wechatEnabled) copy.getString(R.string.wechat_unconfigured)
                        else if (account.accountType == "member") copy.getString(R.string.bind_wechat) else copy.getString(R.string.login_wechat))
                }
            } else Button(onClick = { action(UiAction("start")) }, enabled = actionable) { Text(if (activeAction == "start") copy.getString(R.string.connecting) else copy.getString(R.string.reconnect)) }
        }
        if (state.phoneForm && account.hasSession && smsEnabled) PhoneForm(account, actionable, now, action, activeAction = activeAction, codeError = phoneError) { state.phoneForm = false }
        if (state.deletePhoneForm && account.hasSession && smsEnabled) PhoneForm(account, actionable, now, action, deletion = true, activeAction = activeAction, codeError = phoneError) { state.deletePhoneForm = false }
        if (account.hasSession) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { action(UiAction("me")) }, enabled = actionable) { Text(if (activeAction == "me") copy.getString(R.string.updating) else copy.getString(R.string.refresh_account)) }
            TextButton(onClick = { state.confirmLogout = true }, enabled = actionable) { Text(copy.getString(R.string.logout_action)) }
        }
        if (account.accountType == "member" && account.hasSession)
            TextButton(onClick = { state.confirmDelete = true }, enabled = actionable) { Text(copy.getString(R.string.delete_action), color = MaterialTheme.colorScheme.error) }
    }
}
