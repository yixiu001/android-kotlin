package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import cn.yixiu.starter.BuildConfig

internal fun showToast(context: Context, message: String) {
    if (message.isNotBlank()) Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
}

/** 消费结果后即清除，避免 Activity 重建/回前台再次提示；重复操作可产生相同结果。 */
@Composable internal fun ResultToast(message: String, consume: () -> Unit) {
    val copy = LocalResources.current
    val context = LocalContext.current
    var lastShown by rememberSaveable { mutableStateOf("") }
    val onConsume by rememberUpdatedState(consume)
    LaunchedEffect(message) {
        if (message.isBlank()) lastShown = ""
        else {
            if (lastShown != message && message != copy.getString(R.string.connected)) showToast(context, message)
            lastShown = message
            onConsume()
        }
    }
}

/** 帮助页和设置页复用同一反馈优先级；不携带账号令牌或个人资料。 */
@Composable internal fun SupportEntry(url: String = BuildConfig.FEEDBACK_URL,
                                     email: String = BuildConfig.SUPPORT_EMAIL,
                                     debug: Boolean = BuildConfig.DEBUG) {
    val copy = LocalResources.current
    val context = LocalContext.current
    val destination = supportDestination(url, email)
    var fallback by rememberSaveable { mutableStateOf(false) }
    if (destination != null) TextButton(onClick = {
        val intent = when (destination) {
            is SupportDestination.Web -> Intent(Intent.ACTION_VIEW, Uri.parse(destination.url))
            is SupportDestination.Email -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", destination.address, null))
        }
        if (runCatching { context.startActivity(intent) }.isFailure) {
            if (email.isNotBlank()) fallback = true else showToast(context, copy.getString(R.string.feedback_open_error))
        }
    }) { Text(copy.getString(R.string.feedback_action)) }
    else if (debug) Text(copy.getString(R.string.feedback_unconfigured), style = MaterialTheme.typography.bodySmall)
    if (fallback) AppDialog(onDismissRequest = { fallback = false },
        title = { Text(copy.getString(R.string.support_title)) }, text = { Text(copy.getString(R.string.support_fallback, email.trim())) },
        confirmButton = { TextButton(onClick = {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(copy.getString(R.string.support_email_label), email.trim()))
            fallback = false; showToast(context, copy.getString(R.string.support_email_copied))
        }) { Text(copy.getString(R.string.copy_support_email)) } },
        dismissButton = { TextButton(onClick = { fallback = false }) { Text(copy.getString(R.string.cancel)) } })
}
