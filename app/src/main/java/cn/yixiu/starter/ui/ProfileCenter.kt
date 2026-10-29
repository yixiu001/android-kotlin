package cn.yixiu.starter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.R
import cn.yixiu.starter.core.AccountView

internal enum class ProfilePage(val title: Int) {
    Overview(R.string.nav_mine), Account(R.string.nav_account), Settings(R.string.nav_settings),
    Help(R.string.nav_help), Privacy(R.string.nav_privacy), About(R.string.nav_about)
}

private val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary

/** 只负责页面导航和公开内容；账号变更仍由 ui/account 与 AccountEngine 处理。 */
@Composable internal fun ProfileOverview(account: AccountView, enabled: Boolean,
                                        open: (ProfilePage) -> Unit, edit: () -> Unit) {
    val copy = LocalResources.current
    if (!account.privateHidden) {
        val member = account.accountType == "member"
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled && account.hasSession, role = Role.Button,
            onClickLabel = if (member) copy.getString(R.string.profile_edit) else copy.getString(R.string.login_account)) {
            if (member) edit() else open(ProfilePage.Account)
        }.padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            key(account.userId) { UserAvatar(account.avatar, account.avatarUrl, Modifier.size(68.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.nickname.ifBlank { if (member) copy.getString(R.string.account_mine) else copy.getString(R.string.login_register) },
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (member) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Text(if (account.wechatBound) copy.getString(R.string.wechat_bound) else copy.getString(R.string.phone_bound),
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Accent, fontSize = 11.sp,
                            maxLines = 1)
                    }
                }
                Text(if (member) copy.getString(R.string.profile_edit) else copy.getString(R.string.guest_recover_hint), color = Muted, fontSize = 14.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(22.dp))
        }
    }
    ProfileGroup(copy.getString(R.string.account_group)) {
        ProfileRow(Icons.Outlined.Shield, copy.getString(R.string.nav_account), status = if (account.privateHidden) copy.getString(R.string.attention_required)
            else if (account.wechatBound) copy.getString(R.string.wechat_bound) else if (account.phone.isNotBlank()) copy.getString(R.string.phone_bound) else copy.getString(R.string.identity_unbound),
            statusColor = Accent) { open(ProfilePage.Account) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Tune, copy.getString(R.string.nav_settings)) { open(ProfilePage.Settings) }
    }
    ProfileGroup(copy.getString(R.string.support_group)) {
        ProfileRow(Icons.Outlined.ChatBubbleOutline, copy.getString(R.string.nav_help)) { open(ProfilePage.Help) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Description, copy.getString(R.string.nav_privacy)) { open(ProfilePage.Privacy) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Info, copy.getString(R.string.nav_about), "v" + BuildConfig.VERSION_NAME) { open(ProfilePage.About) }
    }
}

@Composable private fun ProfileGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable internal fun ProfileRow(icon: ImageVector, title: String, status: String = "",
                                   statusColor: Color = Muted, enabled: Boolean = true, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = click)
        .heightIn(min = 52.dp).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, modifier = Modifier.size(23.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp)
            if (status.isNotBlank()) Text(status, color = statusColor, fontSize = 12.sp)
        }
        if (enabled) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable private fun ProfileDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable internal fun ProfileHelp() {
    val copy = LocalResources.current
    InfoCard {
        Text(copy.getString(R.string.product_help_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HelpItem(copy.getString(R.string.product_help_recover_question), copy.getString(R.string.product_help_recover_answer))
        HelpItem(copy.getString(R.string.product_help_binding_question), copy.getString(R.string.product_help_binding_answer))
        HelpItem(copy.getString(R.string.product_help_profile_question), copy.getString(R.string.product_help_profile_answer))
        HelpItem(copy.getString(R.string.product_help_logout_question), copy.getString(R.string.product_help_logout_answer))
    }
    SupportEntry()
}

@Composable private fun HelpItem(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    Text(body, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
}

@Composable internal fun ProfilePrivacy() {
    val copy = LocalResources.current
    PublicLink(copy.getString(R.string.privacy_policy), BuildConfig.PRIVACY_POLICY_URL, copy.getString(R.string.product_policy_missing))
    PublicLink(copy.getString(R.string.terms), BuildConfig.TERMS_URL, copy.getString(R.string.product_terms_missing))
}

@Composable private fun PublicLink(title: String, url: String, unavailable: String) {
    val copy = LocalResources.current
    val handler = LocalUriHandler.current
    val context = LocalContext.current
    InfoCard {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (url.isBlank()) Text(unavailable, color = Muted, fontSize = 14.sp)
        else {
            OutlinedButton(onClick = {
                if (runCatching { handler.openUri(url) }.isFailure) showToast(context, copy.getString(R.string.browser_open_error))
            }) { Text(copy.getString(R.string.document_view, title)) }
        }
    }
}

@Composable internal fun ProfileAbout() {
    val copy = LocalResources.current
    InfoCard {
        Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(copy.getString(R.string.version_prefix) + BuildConfig.VERSION_NAME, color = Muted)
    }
}
