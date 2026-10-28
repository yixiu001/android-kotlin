package cn.yixiu.starter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.settings.ToolCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun GeneralSettings(theme: String, onTheme: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf("") }
    var clearing by remember { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { ToolCache.size(context.cacheDir) } }
            .onSuccess { cacheSize = it }.onFailure { message = "临时缓存大小暂不可用" }
    }
    InfoCard {
        Text("外观", style = MaterialTheme.typography.titleMedium)
        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = theme == value, onClick = { onTheme(value) })
                TextButton(onClick = { onTheme(value) }) { Text(label) }
            }
        }
    }
    InfoCard {
        Text("临时缓存", style = MaterialTheme.typography.titleMedium)
        Text(cacheSize?.let { "工具临时文件：${it / 1024} KB" } ?: "正在计算…")
        Text("仅清理工具临时文件，不影响账号凭证、个人资料、主题设置和用户保存的文件。")
        OutlinedButton(enabled = !clearing && cacheSize != null, onClick = { confirmClear = true }) { Text("清理临时缓存") }
        if (message.isNotBlank()) Text(message)
    }
    InfoCard {
        Text("支持与版本", style = MaterialTheme.typography.titleMedium)
        Text("版本 ${BuildConfig.VERSION_NAME}")
        if (BuildConfig.DEBUG) Text("测试版本 · 不用于正式发布", style = MaterialTheme.typography.bodySmall)
        if (BuildConfig.FEEDBACK_URL.isNotBlank()) DocumentButton("意见反馈", BuildConfig.FEEDBACK_URL)
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) TextButton(onClick = {
            message = runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}"))) }.exceptionOrNull()?.let { "没有可用邮件应用，请联系 ${BuildConfig.SUPPORT_EMAIL}" }.orEmpty()
        }) { Text("联系支持") }
        DocumentButton("查看应用市场版本", BuildConfig.APP_MARKET_URL)
        if (BuildConfig.APP_RECORD_NUMBER.isNotBlank()) Text(BuildConfig.APP_RECORD_NUMBER, style = MaterialTheme.typography.bodySmall)
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("清理工具临时文件？") },
        text = { Text("不会退出账号，也不会清除现有账号数据。") },
        confirmButton = { TextButton(onClick = {
            confirmClear = false; clearing = true
            scope.launch {
                try {
                    cacheSize = withContext(Dispatchers.IO) { ToolCache.clear(context.cacheDir); ToolCache.size(context.cacheDir) }
                    message = "临时缓存已清理"
                } catch (_: Exception) { message = "清理未完成，请稍后重试" }
                finally { clearing = false }
            }
        }) { Text("确认清理") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } })
}
