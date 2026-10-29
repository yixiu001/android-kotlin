package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig

@Composable fun PrivacyContent(consented: Boolean, canWithdraw: Boolean = false, onWithdraw: () -> Unit = {}) {
    val copy = LocalResources.current
    var withdrawal by rememberSaveable { mutableStateOf(false) }
    InfoCard {
        Text(if (BuildConfig.DEBUG) copy.getString(R.string.product_privacy_debug_title) else copy.getString(R.string.product_privacy_title), style = MaterialTheme.typography.titleLarge)
        Text(if (BuildConfig.DEBUG) copy.getString(R.string.product_privacy_debug_intro)
            else copy.getString(R.string.product_privacy_intro))
        Text(copy.getString(R.string.product_privacy_account_data))
        Text(copy.getString(R.string.product_privacy_identity_data))
        Text(copy.getString(R.string.product_privacy_withdraw_help))
        DocumentButton(copy.getString(R.string.privacy_policy_read), BuildConfig.PRIVACY_POLICY_URL)
        DocumentButton(copy.getString(R.string.terms_read), BuildConfig.TERMS_URL)
        Text(copy.getString(R.string.product_privacy_version, BuildConfig.PRIVACY_VERSION.ifBlank { copy.getString(R.string.not_configured) }), style = MaterialTheme.typography.bodySmall)
        if (BuildConfig.OPERATOR_NAME.isNotBlank()) Text(copy.getString(R.string.product_operator, BuildConfig.OPERATOR_NAME))
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) Text(copy.getString(R.string.product_privacy_contact, BuildConfig.SUPPORT_EMAIL))
    }
    InfoCard {
        Text(copy.getString(R.string.product_sdk_title), style = MaterialTheme.typography.titleMedium)
        if (BuildConfig.WECHAT_ENABLED) {
            Text(copy.getString(R.string.product_wechat_sdk))
            DocumentButton(copy.getString(R.string.product_wechat_policy), "https://weixin.qq.com/cgi-bin/readtemplate?t=opensdk_agreement&lang=zh_CN")
        } else Text(copy.getString(R.string.product_wechat_disabled))
        Text(copy.getString(R.string.product_sms_sdk))
    }
    InfoCard {
        Text(copy.getString(R.string.product_permissions_title), style = MaterialTheme.typography.titleMedium)
        Text(copy.getString(R.string.product_permissions_body))
        val context = LocalContext.current
        var error by remember { mutableStateOf("") }
        OutlinedButton(onClick = {
            error = runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))) }.exceptionOrNull()?.let { copy.getString(R.string.settings_open_error) }.orEmpty()
        }) { Text(copy.getString(R.string.permissions_settings)) }
        if (error.isNotBlank()) Text(error)
    }
    if (consented) InfoCard {
        Text(copy.getString(R.string.product_privacy_accepted))
        OutlinedButton(onClick = { withdrawal = true }, enabled = canWithdraw) { Text(copy.getString(R.string.privacy_withdraw)) }
        if (!canWithdraw) Text(copy.getString(R.string.product_privacy_busy))
    }
    if (withdrawal) AppDialog(onDismissRequest = { withdrawal = false }, title = { Text(copy.getString(R.string.privacy_withdraw_title)) },
        text = { Text(copy.getString(R.string.product_privacy_withdraw_warning)) },
        confirmButton = { TextButton(enabled = canWithdraw, onClick = { withdrawal = false; onWithdraw() }) { Text(copy.getString(R.string.privacy_withdraw_confirm)) } },
        dismissButton = { TextButton(onClick = { withdrawal = false }) { Text(copy.getString(R.string.cancel)) } })
}

@Composable fun DocumentButton(label: String, url: String) {
    val copy = LocalResources.current
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    ResultToast(error) { error = "" }
    if (url.isNotBlank()) TextButton(onClick = {
        error = runCatching {
            val uri = Uri.parse(url)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank())
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.exceptionOrNull()?.let { copy.getString(R.string.document_open_error) }.orEmpty()
    }) { Text(label) }
    else if (BuildConfig.DEBUG) Text(copy.getString(R.string.document_unconfigured, label), style = MaterialTheme.typography.bodySmall)
}
