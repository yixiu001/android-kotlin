package cn.yixiu.starter.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.InfoCard
import cn.yixiu.starter.ui.UiAction
import cn.yixiu.starter.ui.normalizeMainlandPhone
import cn.yixiu.starter.ui.validMainlandPhone

@Composable internal fun PhoneForm(account: AccountView, enabled: Boolean, now: Long,
                                  action: (UiAction) -> Unit, deletion: Boolean = false,
                                  activeAction: String = "", codeError: String = "", close: () -> Unit) {
    var phone by rememberSaveable(account.origin, account.userId, deletion) { mutableStateOf(account.smsPhone.removePrefix("+86")) }
    var code by remember { mutableStateOf("") }
    val seconds = ((account.smsResendAt - now + 999) / 1000).coerceAtLeast(0)
    InfoCard {
        Text(if (deletion) "验证手机号并注销" else if (account.accountType == "member") "绑定你的手机号" else "手机验证码登录",
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(phone, { phone = normalizeMainlandPhone(it).take(64) },
            label = { Text("手机号") }, prefix = { Text("+86 ") }, singleLine = true,
            isError = phone.isNotBlank() && !validMainlandPhone(phone),
            supportingText = { if (phone.isNotBlank() && !validMainlandPhone(phone)) Text("请输入 11 位中国大陆手机号") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        if (deletion) Text("请输入已绑定的手机号：${account.phone}。验证成功后立即注销，原绑定身份 30 天内不可重新注册或绑定。")
        OutlinedButton(onClick = { action(UiAction(if (deletion) "deleteSms" else "sms", phone)); code = "" },
            enabled = enabled && validMainlandPhone(phone) && seconds == 0L) {
            Text(if (activeAction in setOf("sms", "deleteSms")) "正在发送…" else if (seconds > 0) seconds.toString() + " 秒后可重发" else "获取验证码")
        }
        if (account.smsPhone.isNotBlank() && account.smsExpiresAt > now) Text(
            "本次验证：" + account.smsPhone.take(5) + "****" + account.smsPhone.takeLast(4),
            style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            label = { Text("验证码") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = code.isBlank() && codeError.isNotBlank(),
            supportingText = { if (code.isBlank() && codeError.isNotBlank()) Text(codeError) },
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        Button(onClick = { action(UiAction("phone", code)); code = "" }, modifier = Modifier.fillMaxWidth(),
            enabled = enabled && code.length == 6 && account.smsExpiresAt > now && "+86" + phone == account.smsPhone &&
                (if (deletion) account.smsPurpose == "delete" else account.smsPurpose != "delete")) {
            Text(if (activeAction == "phone") "正在验证…" else if (deletion) "验证并立即注销" else if (account.accountType == "member") "确认绑定" else "验证并登录")
        }
        if (!deletion) Text(if (account.accountType == "member") "手机号绑定后不可更换或解绑。" else "已绑定老账号时，会切换到老账号并按服务端规则合并游客数据。", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = close) { Text("收起") }
    }
}
