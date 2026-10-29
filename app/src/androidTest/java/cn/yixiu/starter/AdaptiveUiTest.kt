package cn.yixiu.starter

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*
import org.junit.Rule
import org.junit.Test

/** 仅改变隔离测试 Activity 的方向和 composition 字体，不修改手机系统设置。 */
class AdaptiveUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val member = AccountView("https://example.com", "42", "member", hasSession = true,
        nickname = "用于检查长昵称的虚拟账号", avatar = "default_1")

    @Test fun largeFontShortDialogKeepsFieldsAndActionsReachable() {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply { screenHeightDp = 360; screenWidthDp = 320 }
            val density = LocalDensity.current.density
            val window = object : WindowInfo by LocalWindowInfo.current {
                override val containerSize = IntSize((320 * density).toInt(), (360 * density).toInt())
            }
            CompositionLocalProvider(LocalConfiguration provides config,
                LocalWindowInfo provides window,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                StarterTheme { ProfileEditor(member, false, {}) {} }
            }
        }
        compose.onNodeWithText("保存").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.onNodeWithText("昵称（最多 64 个字符）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("灰蓝").performScrollTo().performClick()
        compose.onNodeWithText("保存").assertIsEnabled()
    }

    @Test fun landscapeProfileAndEditorRemainUsable() {
        val original = compose.activity.requestedOrientation
        try {
            compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(7000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            compose.setContent { StarterTheme("dark") { StarterScreen(member, false, "", {}) } }
            compose.onNodeWithText("我的").performClick()
            compose.onNodeWithText("编辑个人资料").performScrollTo().performClick()
            compose.onNodeWithText("保存").assertIsDisplayed()
            compose.onNodeWithText("灰蓝").performScrollTo().performClick()
            compose.onNodeWithText("取消").performClick()
            compose.onNodeWithText("账号与安全").performScrollTo().performClick()
            compose.onNodeWithText("注销账号").performScrollTo().assertIsDisplayed()
        } finally {
            compose.runOnUiThread { compose.activity.requestedOrientation = original }
        }
    }
}
