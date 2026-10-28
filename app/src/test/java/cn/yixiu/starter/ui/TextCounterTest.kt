package cn.yixiu.starter.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCounterTest {
    @Test fun emptyTextHasNoLines() {
        assertEquals(TextCounts(0, 0, 0), countText(""))
    }

    @Test fun supplementaryUnicodeCharactersCountOnce() {
        assertEquals(TextCounts(4, 3, 1), countText("中 A😀"))
    }

    @Test fun lineEndingsAndUnicodeSpacesAreHandled() {
        assertEquals(TextCounts(7, 2, 3), countText("a\r\nb\r\u00a0\u3000"))
    }
}
