package cn.yixiu.starter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*
import org.junit.Rule
import org.junit.Test

class StarterUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun guestCanOpenPhoneLoginFromPersonalCenter() {
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", "42", "guest", hasSession = true), false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("手机号登录").performClick()
        compose.onNodeWithText("获取验证码").assertIsDisplayed()
        compose.onNodeWithText("验证码").assertIsDisplayed()
    }
    @Test fun pendingLogoutNeverShowsPrivateIdentity() {
        compose.setContent { StarterTheme { StarterScreen(AccountView("https://example.com", privateHidden = true, pendingPath = "/auth/logout"), false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("退出待确认").assertIsDisplayed()
        compose.onNodeWithText("恢复原请求").assertIsDisplayed()
        compose.onNodeWithText("手机号登录").assertDoesNotExist()
    }
}
