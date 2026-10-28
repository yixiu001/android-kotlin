package cn.yixiu.starter.debug

import cn.yixiu.starter.core.readBounded
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Isolated client: never routes grants through normal API logging or persistence. */
class TestToolsFailure(val code: String, val status: Int) : Exception(when (code) {
    "TEST_TOOLS_DISABLED" -> "测试服务器未启用联调工具，请先配置开关和测试库"
    "TEST_GRANT_INVALID" -> "授权过期或无效，请重新签发"
    "TEST_PREVIEW_CHANGED" -> "账号数据已变化，请重新预览"
    "TEST_TARGET_INVALID" -> "账号不可重置，已合并游客不能重置其目标账号"
    else -> "服务器未确认成功；可重试原请求或重新申请授权"
})

class TestToolsClient(origin: String, private val connect: (URL) -> HttpsURLConnection = { it.openConnection() as HttpsURLConnection }) {
    val origin = ResetPolicy.boundOrigin(origin)
    suspend fun prepare(access: String): JsonObject = post("prepare", access, JsonObject())
    suspend fun preview(grant: String): JsonObject = post("preview", grant, JsonObject())
    suspend fun execute(grant: String, preview: JsonObject): JsonObject = post("execute", grant, JsonObject().apply {
        addProperty("user_id", preview["user_id"].asString)
        addProperty("confirmation", preview["confirmation"].asString)
    }).also {
        check(it["reset"]?.asBoolean == true && it["local_reset_required"]?.asBoolean == true && it["user_id"]?.asString == preview["user_id"].asString) { "重置结果未确认，请重试原请求" }
    }
    private suspend fun post(action: String, grant: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        require(grant.matches(Regex("[A-Za-z0-9_-]{43}"))) { "请输入服务器签发的短期授权码" }
        val connection = connect(URL("$origin/test-tools/v1/account-reset/$action"))
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000; connection.readTimeout = 15000
            connection.requestMethod = "POST"; connection.useCaches = false; connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $grant")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            if (status in 300..399) throw IOException("重定向已拒绝")
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val raw = stream?.use { it.readBounded(65537) } ?: throw IOException("响应未确认")
            if (raw.size > 65536) throw IOException("响应未确认")
            val envelope = try { JsonParser.parseString(raw.toString(Charsets.UTF_8)).asJsonObject } catch (_: Exception) { throw IOException("响应未确认") }
            if (status != 200 || envelope["code"]?.asString != "OK") throw TestToolsFailure(envelope["code"]?.asString ?: "", status)
            val data = envelope["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: throw IOException("响应未确认")
            if (action == "preview" || action == "prepare") {
                check(data["user_id"]?.asString?.isNotBlank() == true && data["confirmation"]?.asString?.matches(Regex("[A-Za-z0-9_-]{43}")) == true) { "预览响应无效" }
            }
            if (action == "prepare") check(data["grant"]?.asString?.matches(Regex("[A-Za-z0-9_-]{43}")) == true) { "Invalid prepare response" }
            data
        } finally { connection.disconnect() }
    }
}
