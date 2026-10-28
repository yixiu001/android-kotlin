package cn.yixiu.starter

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import cn.yixiu.starter.settings.AppSettings
import cn.yixiu.starter.ui.*
import cn.yixiu.starter.core.AccountView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** 所有状态均为内存假数据：不创建 VM、不读写真实偏好/账号、不请求后端。 */
class FoundationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun refusalAndRestorationNeverConstructOnlineContent() {
        var online = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var declined by rememberSaveable { mutableStateOf("") }
            StarterTheme {
                StartupRouter(AppSettings(declinedPrivacy = declined), {}, { declined = BuildConfig.PRIVACY_VERSION }, {}) {
                    SideEffect { online++ }; Text("在线内容")
                }
            }
        }
        compose.onNodeWithText("暂不同意，使用本地功能").performScrollTo().performClick()
        compose.onNodeWithText("本地模式").assertIsDisplayed()
        compose.onNodeWithText("输入或粘贴文本").performScrollTo().performTextInput("保留本地文本")
        compose.onNodeWithText("查看隐私说明并启用账号").performScrollTo().performClick()
        compose.onNodeWithText("欢迎使用").assertExists()
        compose.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("保留本地文本").performScrollTo().assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("本地模式").assertExists()
        assertEquals(0, online)
    }
    @Test fun onlineSlotOnlyAppearsAfterExplicitAcceptance() {
        compose.setContent {
            var accepted by remember { mutableStateOf("") }
            StarterTheme { StartupRouter(AppSettings(acceptedPrivacy = accepted),
                { accepted = BuildConfig.PRIVACY_VERSION }, {}, {}) { Text("模拟账号页面") } }
        }
        compose.onNodeWithText("模拟账号页面").assertDoesNotExist()
        compose.onNodeWithText("同意并继续").performScrollTo().performClick()
        compose.onNodeWithText("模拟账号页面").assertIsDisplayed()
    }
    @Test fun oldPolicyConsentDoesNotRenderAccount() {
        compose.setContent { StarterTheme {
            StartupRouter(AppSettings(acceptedPrivacy = "old-version"), {}, {}, {}) { Text("模拟账号页面") }
        } }
        compose.onNodeWithText("模拟账号页面").assertDoesNotExist()
        compose.onNodeWithText("同意并继续").assertExists()
    }
    @Test fun homeInputSurvivesNavigationAndThemeIsUsable() {
        compose.setContent { StarterTheme("dark") {
            StarterScreen(AccountView("https://example.com", "42", "member", hasSession = true), false, "", {})
        } }
        compose.onNodeWithText("输入或粘贴文本").performTextInput("Hello 世界")
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("首页").performClick()
        compose.onNodeWithText("Hello 世界").assertIsDisplayed()
        compose.onNodeWithText("字符 8  ·  非空白 7  ·  行数 1").assertExists()
    }
    @Test fun busyAccountCannotWithdrawConsent() {
        var calls = 0
        compose.setContent { StarterTheme { PrivacyContent(true, false) { calls++ } } }
        compose.onNodeWithText("撤回隐私同意").assertIsNotEnabled()
        assertEquals(0, calls)
    }
}
