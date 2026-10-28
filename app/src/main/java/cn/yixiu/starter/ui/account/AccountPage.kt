package cn.yixiu.starter.ui.account

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
                     state: AccountUiState, action: (UiAction) -> Unit) {
    val now = state.now
    if (!account.privateHidden) {
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                key(account.userId) { UserAvatar(account.avatar, account.avatarUrl) }
                Text(account.nickname.ifBlank { if (account.accountType == "member") "我的账号" else "游客账号" },
                    modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Text(if (account.accountType == "member") "手机号和微信都可以进入同一个账号。" else
                "已为你保留当前体验。绑定身份后，可以在其他设备找回账号。", lineHeight = 24.sp)
            if (account.userId.isNotBlank()) Text("账号 ID · " + account.userId,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (account.hasSession) {
                TextButton(onClick = { state.editProfile = true }, enabled = actionable) { Text("编辑昵称和头像") }
                HorizontalDivider()
                if (account.accountType == "member") {
                    Text("手机号   " + account.phone.ifBlank { "未绑定" })
                    Text("微信       " + if (account.wechatBound) "已绑定" else "未绑定")
                    Text("身份绑定后不可更换或解绑。", style = MaterialTheme.typography.bodySmall)
                }
                if (account.phone.isBlank()) Button(onClick = { state.phoneForm = true }, modifier = Modifier.fillMaxWidth(),
                    enabled = actionable && BuildConfig.SMS_ENABLED) {
                    Text(if (account.accountType == "member") "绑定手机号" else "手机号登录")
                }
                if (!account.wechatBound) OutlinedButton(onClick = { action(UiAction("wechat")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = actionable && BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) {
                    Text(if (!BuildConfig.WECHAT_ENABLED || BuildConfig.WECHAT_APP_ID.isBlank()) "微信登录（待配置）"
                        else if (account.accountType == "member") "绑定微信" else "微信登录")
                }
            } else Button(onClick = { action(UiAction("start")) }, enabled = actionable) { Text("重新连接") }
        }
        if (state.phoneForm && account.hasSession) PhoneForm(account, actionable, now, action) { state.phoneForm = false }
        if (state.deletePhoneForm && account.hasSession) PhoneForm(account, actionable, now, action, deletion = true) { state.deletePhoneForm = false }
        if (account.hasSession) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { action(UiAction("me")) }, enabled = actionable) { Text("更新账号信息") }
            TextButton(onClick = { state.confirmLogout = true }, enabled = actionable) { Text("退出当前账号") }
        }
        if (account.accountType == "member" && account.hasSession)
            TextButton(onClick = { state.confirmDelete = true }, enabled = actionable) { Text("注销账号", color = MaterialTheme.colorScheme.error) }
    }
}
