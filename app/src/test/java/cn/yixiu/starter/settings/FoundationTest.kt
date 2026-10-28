package cn.yixiu.starter.settings

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class FoundationTest {
    @Test fun boundedInputNeverSplitsAnEmojiPair() {
        val input = "a".repeat(19999) + "😀"
        assertEquals("a".repeat(19999), cn.yixiu.starter.ui.limitToolText(input))
        assertEquals("😀", cn.yixiu.starter.ui.limitToolText("😀"))
    }
    @Test fun consentIsExplicitVersionedAndRefusalDoesNotLoop() {
        val empty = AppSettings()
        assertEquals(StartupStep.CONSENT, StartupPolicy.step(empty, "test-v1", false, "1"))
        assertEquals(StartupStep.LOCAL_ONLY, StartupPolicy.step(empty.copy(declinedPrivacy = "test-v1"), "test-v1", false, "1"))
        val accepted = empty.copy(acceptedPrivacy = "test-v1")
        assertEquals(StartupStep.READY, StartupPolicy.step(accepted, "test-v1", false, "1"))
        assertEquals(StartupStep.CONSENT, StartupPolicy.step(accepted, "production-v1", false, "1"))
        assertFalse(StartupPolicy.accepted(empty, ""))
    }
    @Test fun onboardingIsOptionalAndCompletedVersionDoesNotRepeat() {
        val accepted = AppSettings(acceptedPrivacy = "p1")
        assertEquals(StartupStep.ONBOARDING, StartupPolicy.step(accepted, "p1", true, "v1"))
        assertEquals(StartupStep.READY, StartupPolicy.step(accepted.copy(onboardingCompleted = "v1"), "p1", true, "v1"))
        assertEquals(StartupStep.ONBOARDING, StartupPolicy.step(accepted.copy(onboardingCompleted = "v1"), "p1", true, "v2"))
    }
    @Test fun cacheCleaningOnlyTouchesToolTemporaryDirectory() {
        val root = Files.createTempDirectory("cache-test").toFile()
        try {
            root.resolve("tool-temp/nested").mkdirs()
            root.resolve("tool-temp/nested/data").writeText("12345")
            root.resolve("account-private").mkdirs()
            root.resolve("account-private/data").writeText("account")
            root.resolve("account-vault").writeText("vault")
            assertEquals(5L, ToolCache.size(root))
            ToolCache.clear(root)
            assertEquals(0L, ToolCache.size(root))
            assertEquals("account", root.resolve("account-private/data").readText())
            assertEquals("vault", root.resolve("account-vault").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun withdrawalRequiresAnIdleKnownAccountState() {
        assertTrue(StartupPolicy.canWithdraw(false, null, false))
        assertFalse(StartupPolicy.canWithdraw(true, null, false))
        assertFalse(StartupPolicy.canWithdraw(false, "/auth/logout", false))
        assertFalse(StartupPolicy.canWithdraw(false, null, true))
    }
}
