package cn.yixiu.starter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.ClipboardManager

internal data class TextCounts(val characters: Int, val nonWhitespace: Int, val lines: Int)

internal fun limitToolText(value: String): String {
    var end = minOf(value.length, 20000)
    if (end < value.length && end > 0 && value[end - 1].isHighSurrogate() && value[end].isLowSurrogate()) end--
    return value.substring(0, end)
}

/** Count Unicode code points, treating CRLF as one line break. No account or network dependency. */
internal fun countText(text: String): TextCounts {
    var nonWhitespace = 0
    var offset = 0
    while (offset < text.length) {
        val point = text.codePointAt(offset)
        if (!Character.isWhitespace(point) && !Character.isSpaceChar(point)) nonWhitespace++
        offset += Character.charCount(point)
    }
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    return TextCounts(text.codePointCount(0, text.length), nonWhitespace,
        if (text.isEmpty()) 0 else normalized.count { it == '\n' } + 1)
}

/** Pure local demo, also usable before privacy consent. The host owns scrolling and insets. */
@Composable fun LocalToolScreen() {
    var text by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val counts = remember(text) { countText(text) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("文本计数", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("本地工具示例 · 无需登录，也不会上传内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoCard {
            OutlinedTextField(value = text, onValueChange = { text = limitToolText(it); copied = false },
                label = { Text("输入或粘贴文本") }, modifier = Modifier.fillMaxWidth(),
                minLines = 5, maxLines = 10)
            Text("字符 ${counts.characters}  ·  非空白 ${counts.nonWhitespace}  ·  行数 ${counts.lines}",
                style = MaterialTheme.typography.titleMedium)
            Text("最多输入约 2 万字。字符数包含空格和换行；组合表情可能计为多个字符。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { text = "" }, enabled = text.isNotEmpty()) { Text("清空文本") }
            OutlinedButton(onClick = {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("文本统计",
                    "字符 ${counts.characters} · 非空白 ${counts.nonWhitespace} · 行数 ${counts.lines}"))
                copied = true
            }, enabled = text.isNotEmpty()) { Text(if (copied) "已复制统计" else "复制统计结果") }
        }
        if (text.isEmpty()) EmptyState("还没有文本", "输入一段文字，即可查看字符数和行数。")
    }
}
