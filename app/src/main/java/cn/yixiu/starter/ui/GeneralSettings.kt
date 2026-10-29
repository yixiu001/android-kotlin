package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.settings.ToolCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun GeneralSettings(theme: String, onTheme: (String) -> Unit) {
    val copy = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf("") }
    var clearing by remember { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    ResultToast(message) { message = "" }
    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { ToolCache.size(context.cacheDir) } }
            .onSuccess { cacheSize = it }.onFailure { message = copy.getString(R.string.cache_size_error) }
    }
    InfoCard {
        Text(copy.getString(R.string.appearance), style = MaterialTheme.typography.titleMedium)
        listOf("system" to copy.getString(R.string.theme_system), "light" to copy.getString(R.string.theme_light), "dark" to copy.getString(R.string.theme_dark)).forEach { (value, label) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = theme == value, onClick = { onTheme(value) })
                TextButton(onClick = { onTheme(value) }) { Text(label) }
            }
        }
    }
    InfoCard {
        Text(copy.getString(R.string.cache_title), style = MaterialTheme.typography.titleMedium)
        Text(cacheSize?.let { copy.getString(R.string.cache_size, it / 1024) } ?: copy.getString(R.string.calculating))
        Text(copy.getString(R.string.cache_help))
        OutlinedButton(enabled = !clearing && cacheSize != null, onClick = { confirmClear = true }) { Text(copy.getString(R.string.cache_clear)) }
    }
    InfoCard {
        Text(copy.getString(R.string.support_version), style = MaterialTheme.typography.titleMedium)
        Text(copy.getString(R.string.version_label, BuildConfig.VERSION_NAME))
        if (BuildConfig.DEBUG) Text(copy.getString(R.string.debug_version), style = MaterialTheme.typography.bodySmall)
        SupportEntry()
        DocumentButton(copy.getString(R.string.market_version), BuildConfig.APP_MARKET_URL)
        if (BuildConfig.APP_RECORD_NUMBER.isNotBlank()) Text(BuildConfig.APP_RECORD_NUMBER, style = MaterialTheme.typography.bodySmall)
    }
    if (confirmClear) AppDialog(onDismissRequest = { confirmClear = false }, title = { Text(copy.getString(R.string.cache_clear_title)) },
        text = { Text(copy.getString(R.string.cache_clear_warning)) },
        confirmButton = { TextButton(onClick = {
            confirmClear = false; clearing = true
            scope.launch {
                try {
                    cacheSize = withContext(Dispatchers.IO) { ToolCache.clear(context.cacheDir); ToolCache.size(context.cacheDir) }
                    message = copy.getString(R.string.cache_cleared)
                } catch (_: Exception) { message = copy.getString(R.string.cache_clear_error) }
                finally { clearing = false }
            }
        }) { Text(copy.getString(R.string.cache_clear_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(copy.getString(R.string.cancel)) } })
}
