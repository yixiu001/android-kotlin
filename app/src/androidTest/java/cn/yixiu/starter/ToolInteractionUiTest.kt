package cn.yixiu.starter

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*
import cn.yixiu.starter.ui.account.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** 内存账号状态，无 VM、网络或真实账号存储操作。 */
class ToolInteractionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pendingOperationDoesNotInterruptHomeButIsRecoverableFromAccount() {
        val actions = mutableListOf<UiAction>()
        compose.setContent { StarterTheme {
            StarterScreen(AccountView("https://example.com", privateHidden = true,
                pendingPath = "/auth/logout"), false, "", actions::add)
        } }
        compose.onNodeWithText("退出待确认").assertDoesNotExist()
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("账号与安全").performClick()
        compose.onNodeWithText("处理账号状态").performClick()
        compose.onNodeWithText("继续确认退出").performClick()
        assertEquals(listOf(UiAction("recover")), actions)
    }

    @Test fun formattedPhoneIsAcceptedAndSurvivesRecreationWithoutSavingOtp() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme {
            StarterScreen(AccountView("https://example.com", "42", "guest", hasSession = true), false, "", {})
        } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("账号与安全").performClick()
        compose.onNodeWithText("手机号登录").performClick()
        compose.onNodeWithText("手机号", substring = false).performScrollTo().performTextInput("+86 138-0013-8000")
        compose.onNodeWithText("获取验证码").assertIsEnabled()
        compose.onNodeWithText("验证码", substring = false).performScrollTo().performTextInput("123456")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("13800138000").performScrollTo().assertExists()
        compose.onNodeWithText("123456").assertDoesNotExist()
    }

    @Test fun releaseHidesDisabledLoginsAndHelpStillOffersEmailFeedback() {
        compose.setContent { StarterTheme {
            androidx.compose.foundation.layout.Column {
                AccountPage(AccountView("https://example.com", "42", "guest", hasSession = true), true,
                    AccountUiState(), {}, smsEnabled = false, wechatEnabled = false, debug = false)
                SupportEntry(url = "", email = "help@example.com", debug = false)
            }
        } }
        compose.onNodeWithText("手机号登录").assertDoesNotExist()
        compose.onNodeWithText("微信登录").assertDoesNotExist()
        compose.onNodeWithText("微信登录（待配置）").assertDoesNotExist()
        compose.onNodeWithText("意见反馈").assertExists()
    }

    @Test fun consumedResultsAreNotReplayedAfterRecreation() {
        var consumed = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var message by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("已保存") }
            ResultToast(message) { consumed++; message = "" }
        }
        compose.waitForIdle()
        assertEquals(1, consumed)
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals(1, consumed)
    }

    @Test fun countdownStillFinishesAfterSwitchingAccountIdentity() {
        var account by mutableStateOf(AccountView("https://example.com", "old", "guest", hasSession = true))
        compose.setContent { StarterTheme {
            val state = rememberAccountUiState(account)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AccountPage(account, state.isActionable(account, false), state, {})
            }
        } }
        compose.runOnIdle { account = AccountView("https://example.com", "new", "guest", hasSession = true,
            smsPhone = "+8613800138000", smsResendAt = System.currentTimeMillis() + 1800) }
        compose.onNodeWithText("手机号登录").performScrollTo().performClick()
        compose.waitUntil(6000) { compose.onAllNodesWithText("获取验证码").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("获取验证码").assertIsEnabled()
    }
}
