package cn.yixiu.starter

import androidx.test.platform.app.InstrumentationRegistry
import cn.yixiu.starter.core.*
import cn.yixiu.starter.storage.EncryptedStateStore
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EncryptedStateStoreTest {
    @Test fun encryptedAtomicStateSurvivesStoreRecreationAndRejectsCorruption() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "vault-test-" + System.nanoTime()
        val store = EncryptedStateStore(context, name)
        val request = Protocol.request("https://example.com", "/auth/refresh", """{"refresh_token":"super-secret-refresh"}""", null)
        val state = VaultState("https://example.com", session = Session("super-secret-access", "super-secret-refresh", "session"), pending = Pending(request, 1))
        store.save(state)
        assertEquals(state, EncryptedStateStore(context, name).load())
        val file = File(context.noBackupFilesDir, name)
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("super-secret"))
        file.writeBytes(byteArrayOf(1, 2, 3))
        assertThrows(Exception::class.java) { EncryptedStateStore(context, name).load() }
        file.delete()
    }
}
