package cn.yixiu.starter.ui

import org.junit.Assert.*
import org.junit.Test

class InteractionPolicyTest {
    @Test fun mainlandPhoneAcceptsFormattingButDoesNotConvertOtherIdentities() {
        assertEquals("13800138000", normalizeMainlandPhone(" +86 138-0013-8000 "))
        assertEquals("13800138000", normalizeMainlandPhone("138 0013 8000"))
        assertTrue(validMainlandPhone(normalizeMainlandPhone("+86 13800138000")))
        assertFalse(validMainlandPhone(normalizeMainlandPhone("+1 13800138000")))
        assertFalse(validMainlandPhone(normalizeMainlandPhone("abc13800138000")))
        assertFalse(validMainlandPhone(normalizeMainlandPhone("138001380001")))
    }

    @Test fun feedbackPrefersWebThenEmailAndCanBeAbsent() {
        assertEquals(SupportDestination.Web("https://example.com/help"), supportDestination("https://example.com/help", "help@example.com"))
        assertEquals(SupportDestination.Email("help@example.com"), supportDestination("", " help@example.com "))
        assertEquals(SupportDestination.Email("help@example.com"), supportDestination("http://example.com", "help@example.com"))
        assertNull(supportDestination("", ""))
    }

    @Test fun disabledLoginsAreHiddenInReleaseButDiagnosableInDebug() {
        assertFalse(showLoginEntry(false, false))
        assertTrue(showLoginEntry(false, true))
        assertTrue(showLoginEntry(true, false))
    }
}
