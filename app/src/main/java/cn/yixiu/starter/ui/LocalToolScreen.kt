package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalResources
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
    val copy = LocalResources.current
    var text by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val counts = remember(text) { countText(text) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(copy.getString(R.string.product_demo_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(copy.getString(R.string.product_demo_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoCard {
            OutlinedTextField(value = text, onValueChange = { text = limitToolText(it); copied = false },
                label = { Text(copy.getString(R.string.product_demo_input)) }, modifier = Modifier.fillMaxWidth(),
                minLines = 5, maxLines = 10)
            Text(copy.getString(R.string.product_demo_counts, counts.characters, counts.nonWhitespace, counts.lines),
                style = MaterialTheme.typography.titleMedium)
            Text(copy.getString(R.string.product_demo_limit), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { text = "" }, enabled = text.isNotEmpty()) { Text(copy.getString(R.string.product_demo_clear)) }
            OutlinedButton(onClick = {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(copy.getString(R.string.product_demo_clip_label),
                    copy.getString(R.string.product_demo_clip_counts, counts.characters, counts.nonWhitespace, counts.lines)))
                copied = true
            }, enabled = text.isNotEmpty()) { Text(if (copied) copy.getString(R.string.product_demo_copied) else copy.getString(R.string.product_demo_copy)) }
        }
        if (text.isEmpty()) EmptyState(copy.getString(R.string.product_demo_empty), copy.getString(R.string.product_demo_empty_help))
    }
}
