package cn.yixiu.starter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** 公共页面保持单列；宽屏限制阅读宽度，窄屏减小边距，不改变系统字体缩放。 */
@Composable internal fun PageColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val inset = if (maxWidth < 360.dp) 12.dp else 20.dp
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight()
            .verticalScroll(rememberScrollState()).padding(horizontal = inset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** 将可滚动正文与始终可达的确认/取消按钮分开，适配横屏、键盘与大字体。 */
@Composable internal fun AppDialog(onDismissRequest: () -> Unit, title: @Composable () -> Unit,
                                  text: @Composable () -> Unit, confirmButton: @Composable () -> Unit,
                                  dismissButton: @Composable (() -> Unit)? = null) {
    val window = LocalWindowInfo.current.containerSize
    val height = with(LocalDensity.current) { window.height.toDp() - 32.dp }.coerceAtLeast(200.dp)
    val width = with(LocalDensity.current) { window.width.toDp() - 32.dp }.coerceIn(240.dp, 560.dp)
    AlertDialog(onDismissRequest = onDismissRequest, title = title,
        modifier = Modifier.widthIn(max = width).heightIn(max = height),
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { text() } },
        confirmButton = confirmButton, dismissButton = dismissButton)
}
