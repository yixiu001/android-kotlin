package cn.yixiu.starter.debug
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.yixiu.starter.core.Protocol
import cn.yixiu.starter.ui.*

@Composable fun DeveloperPanel(vm: StarterViewModel) {
    if (!cn.yixiu.starter.BuildConfig.TEST_TOOLS_ENABLED) return
    var expanded by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val account by vm.account.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    var origin by remember(account.origin) { mutableStateOf(account.origin) }
    InfoCard {
        TextButton(onClick = { expanded = ResetPolicy.panelAfterToggle(expanded, busy) }) { Text(if (expanded) "收起开发工具" else "开发工具 · 仅调试版本") }
        if (expanded) {
            ResetTools(vm, account.origin, account.userId)
            Text("当前环境：" + account.origin, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(origin, { origin = it }, label = { Text("HTTPS 环境地址") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { confirm = true }, enabled = !busy) { Text("切换环境") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.dispatch(UiAction("health")) }, enabled = !busy) { Text("健康检查") }
                TextButton(onClick = { vm.dispatch(UiAction("refresh")) }, enabled = !busy && account.hasSession && account.pendingPath == null) { Text("刷新会话") }
            }
            TextButton(onClick = { vm.dispatch(UiAction("me")) }, enabled = !busy && account.hasSession) { Text("刷新资料") }
            Text("接口覆盖", style = MaterialTheme.typography.titleMedium)
            Protocol.paths.forEach { path ->
                val latest = logs.firstOrNull { it.path == path }
                Text((if (latest == null) "○ " else if (latest.code == "OK") "✓ " else "△ ") + path, style = MaterialTheme.typography.bodySmall)
            }
            Text("最近请求（最多 50 条）", style = MaterialTheme.typography.titleMedium)
            logs.take(12).forEach {
                HorizontalDivider()
                Text(it.method + " " + it.path, style = MaterialTheme.typography.labelLarge)
                Text(it.status.toString() + " · " + it.code + " · " + it.elapsedMs + " ms", style = MaterialTheme.typography.bodySmall)
                Text(it.summary, style = MaterialTheme.typography.bodySmall)
                if (it.requestId.isNotBlank()) Text("request_id: " + it.requestId, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("切换测试环境？") },
        text = { Text("将清除当前本地凭证与恢复材料，这不等于服务端退出。有未确认操作时建议先恢复；继续代表明确放弃本地恢复材料。旧令牌不会发送到新环境。") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.dispatch(UiAction("environment", origin)) }) { Text("确认清除并切换") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("取消") } })
}
