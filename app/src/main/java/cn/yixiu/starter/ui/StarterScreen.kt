package cn.yixiu.starter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.R
import cn.yixiu.starter.core.AccountView
import kotlinx.coroutines.delay

data class UiAction(val name: String, val value: String = "")
private val Ink = Color(0xFF173B39)
private val Teal = Color(0xFF16756A)
@Composable fun StarterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White,
        background = Color(0xFFF5F7F4), surface = Color.White, onSurface = Ink,
        secondaryContainer = Color(0xFFD9EEE6), onSecondaryContainer = Ink), content = content)
}
@Composable fun StarterScreen(account: AccountView, busy: Boolean, message: String,
                              action: (UiAction) -> Unit, developer: @Composable () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    var phoneForm by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmChallenge by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deletePhoneForm by remember { mutableStateOf(false) }
    var confirmWechat by remember { mutableStateOf(false) }
    var useWechatProfile by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(account.userId, account.phone, account.wechatBound, account.privateHidden) {
        phoneForm = false; deletePhoneForm = false; editProfile = false; confirmDelete = false; confirmWechat = false
    }
    val actionable = !busy && account.pendingPath == null && !account.privateHidden && now >= account.notBefore
    Scaffold(bottomBar = {
        NavigationBar(containerColor = Color.White) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                icon = { Text("⌂", fontSize = 24.sp) }, label = { Text("首页") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                icon = { Text("◎", fontSize = 24.sp) }, label = { Text("我的") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = Teal)
                    Spacer(Modifier.height(6.dp))
                    Text(if (tab == 0) "从这里开始" else "个人中心", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE2EEE8)) {
                    Text(if (account.accountType == "member") "已登录" else "轻松体验", Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEBF1EC)) {
                Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (account.pendingPath != null || account.privateHidden) InfoCard {
                Text(if (account.pendingPath?.startsWith("/me/delete/") == true) "注销待确认" else if (account.pendingPath == "/auth/logout") "退出待确认" else "账号需要处理",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (account.pendingPath != null) "已安全保存原请求。恢复时会核对同一次操作，避免重复提交。"
                    else "当前账号信息暂不展示，请按提示恢复会话。")
                if (account.pendingPath != null)
                    Button(onClick = { action(UiAction("recover")) }, enabled = !busy) { Text("恢复原请求") }
                if (account.pendingPath in setOf("/auth/sms/challenges", "/auth/wechat/challenges"))
                    TextButton(onClick = { confirmChallenge = true }, enabled = !busy) { Text("放弃此次挑战结果") }
                if (account.blocked in setOf("SESSION_INVALID", "REFRESH_TOKEN_INVALID", "AUTH_RESULT_EXPIRED", "SOURCE_ACCOUNT_CHANGED"))
                    OutlinedButton(onClick = { confirmReset = true }, enabled = !busy) { Text("重新开始") }
            }
            if (tab == 0) {
                Surface(shape = RoundedCornerShape(28.dp), color = Ink) {
                    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("欢迎使用", color = Color(0xFFA6D8C8), style = MaterialTheme.typography.labelLarge)
                        Text("让新的想法\n从一个好的基础开始。", fontSize = 28.sp, lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("无需注册即可体验。\n需要保存和找回账号时，在个人中心绑定手机号或微信。",
                            color = Color(0xFFD8E8E1), lineHeight = 24.sp)
                        FilledTonalButton(onClick = { tab = 1 }) { Text("前往个人中心  →") }
                    }
                }
                InfoCard {
                    Text("你的产品，从这里生长", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("这里是首页业务区域。账号与基础连接已经准备好，可以在此扩展你的产品功能。",
                        lineHeight = 24.sp, color = Color(0xFF61716A))
                }
            } else if (!account.privateHidden) {
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        key(account.userId) { UserAvatar(account.avatar, account.avatarUrl) }
                        Text(account.nickname.ifBlank { if (account.accountType == "member") "我的账号" else "你好，体验者" },
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (account.accountType == "member") "手机号和微信都可以进入同一个账号。" else
                        "已为你保留当前体验。绑定身份后，可以在其他设备找回账号。", lineHeight = 24.sp)
                    if (account.userId.isNotBlank()) Text("账号 ID · " + account.userId,
                        style = MaterialTheme.typography.labelMedium, color = Color(0xFF61716A))
                    if (account.hasSession) {
                        TextButton(onClick = { editProfile = true }, enabled = actionable) { Text("编辑昵称和头像") }
                        HorizontalDivider()
                        if (account.accountType == "member") {
                            Text("手机号   " + account.phone.ifBlank { "未绑定" })
                            Text("微信       " + if (account.wechatBound) "已绑定" else "未绑定")
                            Text("身份绑定后不可更换或解绑。", style = MaterialTheme.typography.bodySmall)
                        }
                        if (account.phone.isBlank()) Button(onClick = { phoneForm = true }, modifier = Modifier.fillMaxWidth(),
                            enabled = actionable && BuildConfig.SMS_ENABLED) {
                            Text(if (account.accountType == "member") "绑定手机号" else "手机号登录")
                        }
                        if (!account.wechatBound) OutlinedButton(onClick = {
                            if (account.nickname.isBlank() || (account.avatar.isBlank() && account.avatarUrl.isBlank())) {
                                useWechatProfile = false; confirmWechat = true
                            } else action(UiAction("wechat"))
                        },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = actionable && BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank()) {
                            Text(if (!BuildConfig.WECHAT_ENABLED || BuildConfig.WECHAT_APP_ID.isBlank()) "微信登录（待配置）"
                                else if (account.accountType == "member") "绑定微信" else "微信登录")
                        }
                    } else Button(onClick = { action(UiAction("start")) }, enabled = actionable) { Text("重新连接") }
                }
                if (phoneForm && account.hasSession) PhoneForm(account, actionable, now, action) { phoneForm = false }
                if (deletePhoneForm && account.hasSession) PhoneForm(account, actionable, now, action, deletion = true) { deletePhoneForm = false }
                if (account.hasSession) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { action(UiAction("me")) }, enabled = actionable) { Text("更新账号信息") }
                    TextButton(onClick = { confirmLogout = true }, enabled = actionable) { Text("退出当前账号") }
                }
                if (account.accountType == "member" && account.hasSession)
                    TextButton(onClick = { confirmDelete = true }, enabled = actionable) { Text("注销账号", color = MaterialTheme.colorScheme.error) }
            }
            if (tab == 1) developer()
            Text("每一次开始，都有自己的可能。", modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color(0xFF77857E), style = MaterialTheme.typography.labelSmall)
        }
    }
    if (editProfile && !account.privateHidden) ProfileEditor(account, busy, action) { editProfile = false }
    if (confirmWechat) AlertDialog(onDismissRequest = { confirmWechat = false },
        title = { Text("微信授权") },
        text = { Column {
            Text("是否使用微信昵称和头像补充尚未设置的资料？已有资料不会被覆盖，切换老账号时保留老账号资料。头像使用微信链接，失效时显示默认头像。")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useWechatProfile, onCheckedChange = { useWechatProfile = it })
                Text("使用微信昵称和头像")
            }
        } },
        confirmButton = { TextButton(onClick = { confirmWechat = false; action(UiAction("wechat", useWechatProfile.toString())) }) { Text("继续微信授权") } },
        dismissButton = { TextButton(onClick = { confirmWechat = false }) { Text("取消") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("注销账号？") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("验证成功后立即注销，无法撤销。个人资料将被清除，所有设备退出，当前设备进入新游客账号。")
            Text("已绑定的手机号和微信 30 天内不能注册或绑定其他账号。期满可创建新账号，旧数据不恢复。")
            Text("请验证当前账号已绑定的身份：")
            if (account.phone.isNotBlank()) OutlinedButton(enabled = BuildConfig.SMS_ENABLED, onClick = {
                confirmDelete = false; phoneForm = false; deletePhoneForm = true
            }) { Text("通过手机号验证") }
            if (account.wechatBound) OutlinedButton(enabled = BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank(), onClick = {
                confirmDelete = false; action(UiAction("deleteWechat"))
            }) { Text("验证微信并立即注销") }
        } },
        confirmButton = { TextButton(onClick = { confirmDelete = false }) { Text("暂不注销") } })
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false },
        title = { Text("退出当前账号？") },
        text = { Text("仅退出这台设备。确认后清除本地私有缓存，并进入新的游客账号；未绑定的游客可能无法找回。") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; action(UiAction("logout")) }) { Text("确认退出") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("放弃本地失效会话？") },
        text = { Text("原凭证和恢复材料将被清除。未绑定游客可能无法找回，正式账号可以重新验证手机号或微信登录。") },
        confirmButton = { TextButton(onClick = { confirmReset = false; action(UiAction("restartInvalid")) }) { Text("确认重新开始") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } })
    if (confirmChallenge) AlertDialog(onDismissRequest = { confirmChallenge = false },
        title = { Text("放弃此次挑战结果？") },
        text = { Text("上次短信可能已经发送。放弃后需等待冷却结束，再手动获取新的验证码或微信授权；旧验证码不再用于本次登录。") },
        confirmButton = { TextButton(onClick = { confirmChallenge = false; action(UiAction("abandonChallenge")) }) { Text("确认放弃") } },
        dismissButton = { TextButton(onClick = { confirmChallenge = false }) { Text("取消") } })
}
@Composable private fun PhoneForm(account: AccountView, enabled: Boolean, now: Long,
                                  action: (UiAction) -> Unit, deletion: Boolean = false, close: () -> Unit) {
    var phone by remember { mutableStateOf(account.smsPhone.removePrefix("+86")) }
    var code by remember { mutableStateOf("") }
    val seconds = ((account.smsResendAt - now + 999) / 1000).coerceAtLeast(0)
    InfoCard {
        Text(if (deletion) "验证手机号并注销" else if (account.accountType == "member") "绑定你的手机号" else "手机验证码登录",
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(phone, { if (it.length <= 11 && it.all(Char::isDigit)) phone = it },
            label = { Text("手机号") }, prefix = { Text("+86 ") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        if (deletion) Text("请输入已绑定的手机号：${account.phone}。验证成功后立即注销，原绑定身份 30 天内不可重新注册或绑定。")
        OutlinedButton(onClick = { action(UiAction(if (deletion) "deleteSms" else "sms", phone)); code = "" },
            enabled = enabled && phone.matches(Regex("1[3-9][0-9]{9}")) && seconds == 0L) {
            Text(if (seconds > 0) seconds.toString() + " 秒后可重发" else "获取验证码")
        }
        if (account.smsPhone.isNotBlank() && account.smsExpiresAt > now) Text(
            "本次验证：" + account.smsPhone.take(5) + "****" + account.smsPhone.takeLast(4),
            style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            label = { Text("验证码") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        Button(onClick = { action(UiAction("phone", code)); code = "" }, modifier = Modifier.fillMaxWidth(),
            enabled = enabled && code.length == 6 && account.smsExpiresAt > now && "+86" + phone == account.smsPhone &&
                (if (deletion) account.smsPurpose == "delete" else account.smsPurpose != "delete")) {
            Text(if (deletion) "验证并立即注销" else if (account.accountType == "member") "确认绑定" else "验证并登录")
        }
        if (!deletion) Text(if (account.accountType == "member") "手机号绑定后不可更换或解绑。" else "已绑定老账号时，会切换到老账号并按服务端规则合并游客数据。", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = close) { Text("收起") }
    }
}
@Composable fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
