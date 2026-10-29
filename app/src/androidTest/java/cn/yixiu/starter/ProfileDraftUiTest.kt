package cn.yixiu.starter

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** 仅使用内存模拟状态，不创建 VM、不访问真实账号存储或后端。 */
class ProfileDraftUiTest {
    @get:Rule val compose = createComposeRule()
    private val member = AccountView("https://example.com", "42", "member", hasSession = true, nickname = "原昵称", avatar = "default_1")

    @Test fun savingDoesNotDiscardDraftBeforeServerConfirms() {
        val actions = mutableListOf<UiAction>()
        var closed = false
        compose.setContent { StarterTheme { if (!closed) ProfileEditor(member, false, actions::add) { closed = true } } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("新昵称")
        compose.onNodeWithText("保存").performClick()
        assertEquals(1, actions.size)
        assertFalse("Request dispatch must not close editor before confirmation", closed)
        compose.onNodeWithText("新昵称").assertExists()
    }

    @Test fun editorAndDraftSurviveActivityRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme { StarterScreen(member, false, "", {}) } }
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("编辑个人资料").performClick()
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("旋转保留的昵称")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("编辑资料").assertExists()
        compose.onNodeWithText("旋转保留的昵称").assertExists()
    }

    @Test fun definiteFailureKeepsDraftAndConfirmedRetryClosesEditor() {
        var feedback by mutableStateOf(ProfileSaveFeedback())
        var closed by mutableStateOf(false)
        val actions = mutableListOf<UiAction>()
        compose.setContent { StarterTheme { if (!closed) ProfileEditor(member, false, {
            actions.add(it)
            feedback = ProfileSaveFeedback(actions.size.toLong(), member.origin, member.userId,
                if (actions.size == 1) ProfileSaveStatus.Failed else ProfileSaveStatus.Saved, "保存失败，请稍后重试")
        }, feedback = feedback) { closed = true } } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("保留草稿")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("保留草稿").assertExists()
        compose.onNodeWithText("保存失败，请稍后重试").assertExists()
        compose.onNodeWithText("保存").performClick()
        compose.waitForIdle()
        assertTrue(closed)
        assertEquals(actions[0], actions[1])
    }

    @Test fun unknownResultOnlyAllowsRecoveryOfOriginalSave() {
        var account by mutableStateOf(member)
        var feedback by mutableStateOf(ProfileSaveFeedback())
        var closed by mutableStateOf(false)
        val actions = mutableListOf<UiAction>()
        compose.setContent { StarterTheme { if (!closed) ProfileEditor(account, false, {
            actions.add(it)
            if (it.name == "profile") {
                account = member.copy(pendingPath = "/me/profile")
                feedback = ProfileSaveFeedback(1, member.origin, member.userId, ProfileSaveStatus.Pending)
            } else if (it.name == "recoverProfile") {
                account = member.copy(nickname = "等待确认")
                feedback = ProfileSaveFeedback(2, member.origin, member.userId, ProfileSaveStatus.Saved)
            }
        }, feedback = feedback) { closed = true } } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("等待确认")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("保存").assertIsNotEnabled()
        compose.onNodeWithText("昵称（最多 64 个字符）").assertIsNotEnabled()
        compose.onNodeWithText("继续确认保存").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf("profile", "recoverProfile"), actions.map { it.name })
        assertTrue(closed)
    }

    @Test fun draftDoesNotLeakIntoAnotherAccountOrEnvironment() {
        var account by mutableStateOf(member)
        compose.setContent { StarterTheme { ProfileEditor(account, false, {}) {} } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("不应泄漏")
        compose.runOnIdle { account = member.copy(origin = "https://second.example.com", nickname = "新环境昵称") }
        compose.onNodeWithText("不应泄漏").assertDoesNotExist()
        compose.onNodeWithText("新环境昵称").assertExists()
    }

    @Test fun unrelatedRefreshRecoveryDoesNotCloseRestoredDraft() {
        var account by mutableStateOf(member)
        var closed by mutableStateOf(false)
        val actions = mutableListOf<UiAction>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme { if (!closed) ProfileEditor(account, false, {
            actions.add(it); account = member
        }) { closed = true } } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("尚未提交的草稿")
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { account = member.copy(pendingPath = "/auth/refresh") }
        compose.onNodeWithText("继续确认保存").assertDoesNotExist()
        compose.onNodeWithText("处理账号状态").performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(closed)
        assertEquals(listOf("recover"), actions.map { it.name })
        compose.onNodeWithText("尚未提交的草稿").assertExists()
        compose.onNodeWithText("保存").assertIsEnabled()
    }

    @Test fun submittedDraftRefreshContinuationSurvivesRestoration() {
        var account by mutableStateOf(member)
        var feedback by mutableStateOf(ProfileSaveFeedback())
        var closed by mutableStateOf(false)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme { if (!closed) ProfileEditor(account, false, {
            if (it.name == "profile") {
                account = member.copy(pendingPath = "/auth/refresh")
                feedback = ProfileSaveFeedback(1, member.origin, member.userId, ProfileSaveStatus.Pending)
            } else {
                assertEquals("recoverProfile", it.name)
                account = member.copy(nickname = "提交后的草稿")
                feedback = ProfileSaveFeedback(1, member.origin, member.userId, ProfileSaveStatus.Saved)
            }
        }, feedback = feedback) { closed = true } } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("提交后的草稿")
        compose.onNodeWithText("保存").performClick()
        // 新进程的 VM 不保留瞬时反馈，但原恢复请求与界面保存状态仍可恢复。
        compose.runOnIdle { feedback = ProfileSaveFeedback() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("继续确认保存").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(closed)
    }

    @Test fun restoredDraftCannotLoadUnderDifferentIdentity() {
        var current = member
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StarterTheme { ProfileEditor(current, false, {}) {} } }
        compose.onNodeWithText("昵称（最多 64 个字符）").performTextReplacement("旧账号草稿")
        current = member.copy(userId = "99", nickname = "另一账号")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("旧账号草稿").assertDoesNotExist()
        compose.onNodeWithText("另一账号").assertExists()
    }
}
