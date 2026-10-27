package cn.yixiu.starter.storage
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import cn.yixiu.starter.core.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedStateStore(context: Context, name: String = "account-vault") : StateStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, name))
    private val alias = context.packageName + "." + name
    private val gson = Gson()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }
    @Synchronized override fun load(): VaultState? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        try {
            val bytes = file.openRead().use { it.readBounded(1_048_577) }
            check(bytes.size in 29..1_048_576)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
            val envelope = JsonParser.parseString(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)).asJsonObject
            check(envelope["schema"].asInt == 1)
            val state = gson.fromJson(envelope["state"], VaultState::class.java)
            check(Protocol.origin(state.origin) == state.origin)
            java.util.UUID.fromString(state.installationId)
            state.pending?.let { check(it.request.origin == state.origin && it.request.path in Protocol.paths) }
            return state
        } catch (error: Exception) {
            // 损坏不能当作首次安装，否则可能丢弃唯一的游客恢复凭证。
            throw IOException("本地账号存储无法读取，请保留应用数据并检查设备密钥状态", error)
        }
    }
    @Synchronized override fun save(state: VaultState) {
        val envelope = JsonObject().apply { addProperty("schema", 1); add("state", gson.toJsonTree(state)) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(gson.toJson(envelope).toByteArray(Charsets.UTF_8))
        val bytes = ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array()
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
    }
}
