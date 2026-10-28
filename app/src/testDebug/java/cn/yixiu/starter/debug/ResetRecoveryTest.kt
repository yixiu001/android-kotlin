package cn.yixiu.starter.debug

import com.google.gson.JsonObject
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class ResetRecoveryTest {
    private val original = JsonObject().apply {
        addProperty("user_id", "42")
        addProperty("confirmation", "A".repeat(43))
    }

    @Test fun definitiveRejectionsDiscardStalePreviewAndUnlockGrant() {
        listOf(401 to "TEST_GRANT_INVALID", 403 to "TEST_TOOLS_DISABLED",
            409 to "TEST_PREVIEW_CHANGED", 409 to "TEST_TARGET_INVALID").forEach { (status, code) ->
            val recovery = recoverResetFailure(original, TestToolsFailure(code, status))
            assertNull(code, recovery.preview)
            assertFalse(code, recovery.unknown)
            assertFalse(code, recovery.done)
        }
    }

    @Test fun priorUnknownCannotBeClearedByLaterExpiryDisabledGateOrRefusal() {
        listOf(401 to "TEST_GRANT_INVALID", 403 to "TEST_TOOLS_DISABLED",
            409 to "TEST_PREVIEW_CHANGED", 409 to "TEST_TARGET_INVALID").forEach { (status, code) ->
            val first = recoverResetFailure(original, IOException("response lost"))
            val retry = recoverResetFailure(first.preview!!, TestToolsFailure(code, status), priorUnknown = first.unknown)
            assertSame(original, retry.preview)
            assertTrue(code, retry.unknown)
            assertFalse(retry.done)
            assertFalse(ResetPolicy.mayClearLocal("42", "42", retry.done))
        }
    }
    @Test fun ambiguousFailuresKeepExactRequestAndPreventClear() {
        listOf(IOException("timeout"), TestToolsFailure("AUTH_STORAGE_UNAVAILABLE", 503),
            TestToolsFailure("TEST_PREVIEW_CHANGED", 500)).forEach { error ->
            val recovery = recoverResetFailure(original, error)
            assertSame(original, recovery.preview)
            assertTrue(recovery.unknown)
            assertFalse(recovery.done)
            assertFalse(ResetPolicy.mayClearLocal("42", "42", recovery.done))
        }
    }
}
