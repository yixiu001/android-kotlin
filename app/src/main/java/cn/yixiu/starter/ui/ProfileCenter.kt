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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.R
import cn.yixiu.starter.core.AccountView

internal enum class ProfilePage(val title: String) {
    Overview("我的"), Account("账号与安全"), Settings("通用设置"),
    Help("帮助与反馈"), Privacy("隐私与协议"), About("关于应用")
}

private val Muted = Color(0xFF7B8791)
private val Accent = Color(0xFF16756A)

/** 只负责页面导航和公开内容；账号变更仍由 StarterScreen/AccountEngine 处理。 */
@Composable internal fun ProfileOverview(account: AccountView, enabled: Boolean,
                                        open: (ProfilePage) -> Unit, edit: () -> Unit) {
    if (!account.privateHidden) {
        val member = account.accountType == "member"
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled && account.hasSession, role = Role.Button,
            onClickLabel = if (member) "编辑个人资料" else "登录账号") {
            if (member) edit() else open(ProfilePage.Account)
        }.padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            key(account.userId) { UserAvatar(account.avatar, account.avatarUrl, Modifier.size(68.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.nickname.ifBlank { if (member) "我的账号" else "登录 / 注册" },
                        Modifier.weight(1f, fill = false), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (member) Surface(color = Color(0xFFE4F4ED), shape = RoundedCornerShape(12.dp)) {
                        Text(if (account.wechatBound) "微信已绑定" else "手机已绑定",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Accent, fontSize = 11.sp,
                            maxLines = 1)
                    }
                }
                Text(if (member) "编辑个人资料" else "绑定身份，随时找回账号", color = Muted, fontSize = 14.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(22.dp))
        }
    }
    ProfileGroup("账号") {
        ProfileRow(Icons.Outlined.Shield, "账号与安全", status = if (account.privateHidden) "待处理"
            else if (account.wechatBound) "微信已绑定" else if (account.phone.isNotBlank()) "手机已绑定" else "未绑定",
            statusColor = Accent) { open(ProfilePage.Account) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Tune, "通用设置") { open(ProfilePage.Settings) }
    }
    ProfileGroup("支持与服务") {
        ProfileRow(Icons.Outlined.ChatBubbleOutline, "帮助与反馈") { open(ProfilePage.Help) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Description, "隐私与协议") { open(ProfilePage.Privacy) }
        ProfileDivider()
        ProfileRow(Icons.Outlined.Info, "关于应用", "v" + BuildConfig.VERSION_NAME) { open(ProfilePage.About) }
    }
}

@Composable private fun ProfileGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
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
        Text(title, Modifier.weight(1f), fontSize = 16.sp)
        if (status.isNotBlank()) Text(status, color = statusColor, fontSize = 12.sp, maxLines = 1)
        if (enabled) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable private fun ProfileDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Color(0xFFEDF0F2))
}

@Composable internal fun ProfileHelp() {
    InfoCard {
        Text("常见问题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HelpItem("如何找回账号？", "在“账号与安全”中使用已绑定的手机号或微信登录，即可进入同一个账号。未绑定身份的游客无法跨设备找回。")
        HelpItem("可以更换手机号或微信吗？", "绑定后不支持更换或解绑。一个账号可以同时绑定手机号和微信。")
        HelpItem("如何修改头像和昵称？", "点击个人中心顶部资料区。昵称可以修改，头像可从内置项选择；微信资料只在微信登录或绑定成功后提供一次可跳过的选择。")
        HelpItem("退出和注销有什么区别？", "退出只影响当前设备；注销需要验证已绑定身份，立即生效，原手机号和微信 30 天内不能再次注册或绑定。")
    }
    PublicLink("意见反馈", BuildConfig.FEEDBACK_URL, "暂未开放在线反馈。")
}

@Composable private fun HelpItem(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    Text(body, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
}

@Composable internal fun ProfilePrivacy() {
    PublicLink("隐私政策", BuildConfig.PRIVACY_POLICY_URL, "暂未提供隐私政策，请在正式使用前确认产品说明。")
    PublicLink("用户协议", BuildConfig.TERMS_URL, "暂未提供用户协议。")
}

@Composable private fun PublicLink(title: String, url: String, unavailable: String) {
    val handler = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    InfoCard {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (url.isBlank()) Text(unavailable, color = Muted, fontSize = 14.sp)
        else {
            OutlinedButton(onClick = { failed = runCatching { handler.openUri(url) }.isFailure }) { Text("查看$title") }
            if (failed) Text("暂时无法打开，请检查是否已安装浏览器。", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable internal fun ProfileAbout() {
    InfoCard {
        Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("版本 " + BuildConfig.VERSION_NAME, color = Muted)
    }
}
