package cn.yixiu.starter.ui

import cn.yixiu.starter.core.AccountView
import org.junit.Assert.*
import org.junit.Test

class ProfileSaveFeedbackTest {
    private val account = AccountView("https://example.com", "42", nickname = "原昵称", avatar = "a")

    @Test fun unrelatedRefreshCannotConfirmStaleSubmittedDraft() {
        assertFalse(profileTargetMatches("""{"nickname":"新昵称"}""", account, account))
    }

    @Test fun appliedTargetConfirmsOnlySpecifiedFields() {
        assertTrue(profileTargetMatches("""{"nickname":"新昵称"}""", account, account.copy(nickname = "新昵称", avatar = "b")))
        assertFalse(profileTargetMatches("""{"nickname":"新昵称","avatar":"b"}""", account, account.copy(nickname = "新昵称")))
        assertTrue(profileTargetMatches("""{"nickname":"新昵称","avatar":"b"}""", account, account.copy(nickname = "新昵称", avatar = "b")))
    }

    @Test fun matchingFieldsCannotConfirmAnotherIdentityOrHiddenAccount() {
        val target = """{"nickname":"原昵称"}"""
        assertFalse(profileTargetMatches(target, account, account.copy(userId = "99")))
        assertFalse(profileTargetMatches(target, account, account.copy(origin = "https://second.example.com")))
        assertFalse(profileTargetMatches(target, account, account.copy(privateHidden = true)))
        assertFalse(profileTargetMatches("invalid", account, account))
    }
}
