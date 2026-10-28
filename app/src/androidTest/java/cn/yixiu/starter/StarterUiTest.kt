package cn.yixiu.starter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.graphics.asAndroidBitmap
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*
import org.junit.Rule
import org.junit.Test

class StarterUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun profileEditorOnlyOffersNicknameAndPresetAvatars() {
        compose.setContent { StarterTheme {
            ProfileEditor(AccountView("https://example.com", "42", "member", hasSession = true, wechatBound = true), false, {}) {}
        } }
        compose.onNodeWithText("昵称（最多 64 个字符）").assertIsDisplayed()
        compose.onNodeWithText("使用微信资料").assertDoesNotExist()
        compose.onNodeWithText("前往微信登录").assertDoesNotExist()
        compose.onNodeWithText("重新授权当前绑定微信，预览后选择要替换的资料。").assertDoesNotExist()
    }
    @Test fun loginProfileChoiceCanBeSkippedWithoutRequestingAnotherAuthorization() {
        val actions = mutableListOf<UiAction>()
        val preview = cn.yixiu.starter.core.WechatProfile("preview", "42", "session", "https://example.com", 0, 1000, "微信昵称", "", false, true, false)
        compose.setContent { StarterTheme { WechatProfileDialog(preview, false, 0, actions::add) } }
        compose.onNodeWithText("选择微信资料").assertIsDisplayed()
        compose.onNodeWithText("跳过").performClick()
        org.junit.Assert.assertEquals(listOf(UiAction("skipWechatProfile")), actions)
    }
    @Test fun guestCanOpenPhoneLoginFromPersonalCenter() {
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", "42", "guest", hasSession = true), false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("账号与安全").performClick()
        compose.onNodeWithText("手机号登录").performClick()
        compose.onNodeWithText("获取验证码").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("验证码").performScrollTo().assertIsDisplayed()
    }
    @Test fun pendingLogoutNeverShowsPrivateIdentity() {
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", privateHidden = true, pendingPath = "/auth/logout"), false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("退出待确认").assertIsDisplayed()
        compose.onNodeWithText("恢复原请求").assertIsDisplayed()
        compose.onNodeWithText("手机号登录").assertDoesNotExist()
    }

    @Test fun profileGroupsMoveSensitiveActionsToAccountPage() {
        val actions = mutableListOf<UiAction>()
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", "42", "member",
            hasSession = true, wechatBound = true, nickname = "很长的测试昵称用来检查资料头部布局"), false, "", actions::add) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("编辑个人资料").assertIsDisplayed()
        compose.onNodeWithText("支持与服务").assertIsDisplayed()
        compose.onNodeWithText("退出当前账号").assertDoesNotExist()
        compose.onNodeWithText("注销账号").assertDoesNotExist()
        compose.onNodeWithText("使用微信资料").assertDoesNotExist()
        compose.onNodeWithText("账号与安全").performClick()
        compose.onNodeWithText("账号 ID · 42").assertIsDisplayed()
        compose.onNodeWithText("注销账号").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("编辑个人资料").assertIsDisplayed()
        org.junit.Assert.assertTrue(actions.isEmpty())
    }

    @Test fun developerToolsAreOnlyInsideSettingsAndAboutUsesBuildVersion() {
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", "42", "member", hasSession = true), false, "", {},
            developer = { androidx.compose.material3.Text("隔离调试面板") }) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("隔离调试面板").assertDoesNotExist()
        compose.onNodeWithText("通用设置").performClick()
        compose.onNodeWithText("隔离调试面板").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("关于应用").performClick()
        compose.onNodeWithText("版本 " + BuildConfig.VERSION_NAME).assertIsDisplayed()
    }

    @Test fun profilePreviewAndSecondaryPageSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", "42", "member",
            hasSession = true, wechatBound = true, nickname = "一休"), false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        // 仅保存虚拟账号页面截图，绝不读取或写入真实账号存储。
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.cacheDir, "profile-design-member.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("隐私与协议").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("用户协议").assertIsDisplayed()
        if (BuildConfig.TERMS_URL.isBlank()) compose.onNodeWithText("暂未提供用户协议。").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("编辑个人资料").assertIsDisplayed()
    }
}
