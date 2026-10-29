package cn.yixiu.starter.ui.account

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

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
    val copy = LocalResources.current
    var phone by rememberSaveable(account.origin, account.userId, deletion) { mutableStateOf(account.smsPhone.removePrefix("+86")) }
    var code by remember { mutableStateOf("") }
    val seconds = ((account.smsResendAt - now + 999) / 1000).coerceAtLeast(0)
    InfoCard {
        Text(if (deletion) copy.getString(R.string.delete_phone_title) else if (account.accountType == "member") copy.getString(R.string.bind_phone_title) else copy.getString(R.string.sms_login_title),
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(phone, { phone = normalizeMainlandPhone(it).take(64) },
            label = { Text(copy.getString(R.string.phone_label)) }, prefix = { Text("+86 ") }, singleLine = true,
            isError = phone.isNotBlank() && !validMainlandPhone(phone),
            supportingText = { if (phone.isNotBlank() && !validMainlandPhone(phone)) Text(copy.getString(R.string.phone_format_error)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        if (deletion) Text(copy.getString(R.string.delete_phone_warning, account.phone))
        OutlinedButton(onClick = { action(UiAction(if (deletion) "deleteSms" else "sms", phone)); code = "" },
            enabled = enabled && validMainlandPhone(phone) && seconds == 0L) {
            Text(if (activeAction in setOf("sms", "deleteSms")) copy.getString(R.string.sending) else if (seconds > 0) seconds.toString() + copy.getString(R.string.resend_suffix) else copy.getString(R.string.request_code))
        }
        if (account.smsPhone.isNotBlank() && account.smsExpiresAt > now) Text(
            copy.getString(R.string.verification_prefix) + account.smsPhone.take(5) + "****" + account.smsPhone.takeLast(4),
            style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            label = { Text(copy.getString(R.string.code_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = code.isBlank() && codeError.isNotBlank(),
            supportingText = { if (code.isBlank() && codeError.isNotBlank()) Text(codeError) },
            modifier = Modifier.fillMaxWidth(), enabled = enabled)
        Button(onClick = { action(UiAction("phone", code)); code = "" }, modifier = Modifier.fillMaxWidth(),
            enabled = enabled && code.length == 6 && account.smsExpiresAt > now && "+86" + phone == account.smsPhone &&
                (if (deletion) account.smsPurpose == "delete" else account.smsPurpose != "delete")) {
            Text(if (activeAction == "phone") copy.getString(R.string.verifying) else if (deletion) copy.getString(R.string.verify_and_delete) else if (account.accountType == "member") copy.getString(R.string.confirm_binding) else copy.getString(R.string.verify_and_login))
        }
        if (!deletion) Text(if (account.accountType == "member") copy.getString(R.string.phone_binding_rule) else copy.getString(R.string.guest_merge_help), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = close) { Text(copy.getString(R.string.collapse)) }
    }
}
