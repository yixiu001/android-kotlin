package cn.yixiu.starter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
}
