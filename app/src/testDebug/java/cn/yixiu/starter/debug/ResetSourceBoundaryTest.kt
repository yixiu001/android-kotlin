package cn.yixiu.starter.debug

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Architectural boundaries that compilation alone cannot enforce. */
class ResetSourceBoundaryTest {
    private fun source(name: String): String {
        val file = File("src/debug/java/cn/yixiu/starter/debug/$name.kt")
        return (if (file.exists()) file else File("app", file.path)).readText()
    }
    @Test fun noManualGrantInputOrCredentialLogging() {
        val ui = source("ResetTools")
        assertFalse(ui.contains("OutlinedTextField"))
        assertFalse(ui.contains("TextField("))
        val client = source("TestToolsClient")
        for (forbidden in listOf("android.util.Log", "println(", "logs.", "SharedPreferences", "StateStore", "ApiClient")) {
            assertFalse("tool credential boundary: $forbidden", client.contains(forbidden))
        }
        assertFalse(ui.contains("rememberSaveable"))
    }
}
