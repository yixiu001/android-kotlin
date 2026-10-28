package cn.yixiu.starter.debug
import org.junit.Assert.*
import org.junit.Test
class ResetPolicyTest {
    @Test fun recreatedOrReenteredPanelCanOpenDuringAnyBusyRecoveryState() {
        // Confirmation, unknown response and OS-clear refusal all keep busy=true.
        repeat(3) {
            assertTrue(ResetPolicy.panelAfterToggle(expanded = false, busy = true))
            assertTrue(ResetPolicy.panelAfterToggle(expanded = true, busy = true))
        }
        assertTrue(ResetPolicy.panelAfterToggle(expanded = false, busy = false))
        assertFalse(ResetPolicy.panelAfterToggle(expanded = true, busy = false))
    }

    @Test fun autoClearRejectsUnknownChangedOriginOrGeneration() {
        assertTrue(ResetPolicy.mayAutoClear("42", "42", "https://a.com", "https://a.com", 2, 2, true, false))
        assertFalse(ResetPolicy.mayAutoClear("42", "42", "https://b.com", "https://a.com", 2, 2, true, false))
        assertFalse(ResetPolicy.mayAutoClear("42", "42", "https://a.com", "https://a.com", 3, 2, true, false))
        assertFalse(ResetPolicy.mayAutoClear("42", "42", "https://a.com", "https://a.com", 2, 2, true, true))
        assertFalse(ResetPolicy.mayAutoClear("other", "42", "https://a.com", "https://a.com", 2, 2, true, false))
    }
    @Test fun rejectsOtherLocalAccount() { assertFalse(ResetPolicy.mayClearLocal("42", "43", true)) }
    @Test fun requiresServerSuccess() { assertFalse(ResetPolicy.mayClearLocal("42", "42", false)) }
    @Test fun requiresKnownLocalAccount() { assertFalse(ResetPolicy.mayClearLocal("", "", true)) }
    @Test fun permitsConfirmedSameAccount() { assertTrue(ResetPolicy.mayClearLocal("42", "42", true)) }
    @Test fun rejectsNonHttpsAndPaths() { listOf("http://example.com", "https://example.com/path", "https://example.com/?token=x").forEach { assertThrows(IllegalArgumentException::class.java) { ResetPolicy.boundOrigin(it) } } }
}
