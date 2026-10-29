package cn.yixiu.starter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import cn.yixiu.starter.BuildConfig

internal fun showToast(context: Context, message: String) {
    if (message.isNotBlank()) Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
}

/** 消费结果后即清除，避免 Activity 重建/回前台再次提示；重复操作可产生相同结果。 */
@Composable internal fun ResultToast(message: String, consume: () -> Unit) {
    val context = LocalContext.current
    var lastShown by rememberSaveable { mutableStateOf("") }
    val onConsume by rememberUpdatedState(consume)
    LaunchedEffect(message) {
        if (message.isBlank()) lastShown = ""
        else {
            if (lastShown != message && message != "已连接") showToast(context, message)
            lastShown = message
            onConsume()
        }
    }
}

/** 帮助页和设置页复用同一反馈优先级；不携带账号令牌或个人资料。 */
@Composable internal fun SupportEntry(url: String = BuildConfig.FEEDBACK_URL,
                                     email: String = BuildConfig.SUPPORT_EMAIL,
                                     debug: Boolean = BuildConfig.DEBUG) {
    val context = LocalContext.current
    val destination = supportDestination(url, email)
    var fallback by rememberSaveable { mutableStateOf(false) }
    if (destination != null) TextButton(onClick = {
        val intent = when (destination) {
            is SupportDestination.Web -> Intent(Intent.ACTION_VIEW, Uri.parse(destination.url))
            is SupportDestination.Email -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", destination.address, null))
        }
        if (runCatching { context.startActivity(intent) }.isFailure) {
            if (email.isNotBlank()) fallback = true else showToast(context, "暂时无法打开反馈页面，请检查浏览器后重试")
        }
    }) { Text("意见反馈") }
    else if (debug) Text("意见反馈：请配置反馈网址或支持邮箱", style = MaterialTheme.typography.bodySmall)
    if (fallback) AlertDialog(onDismissRequest = { fallback = false },
        title = { Text("联系支持") }, text = { Text("暂时无法打开反馈应用，可复制邮箱后联系：\n${email.trim()}") },
        confirmButton = { TextButton(onClick = {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("支持邮箱", email.trim()))
            fallback = false; showToast(context, "支持邮箱已复制")
        }) { Text("复制邮箱") } },
        dismissButton = { TextButton(onClick = { fallback = false }) { Text("取消") } })
}
