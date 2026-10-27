package cn.yixiu.starter.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class ApiReply(val status: Int, val code: String, val data: JsonObject = JsonObject(),
                    val requestId: String = "", val retryAfter: Long = 0)
class ApiFailure(val reply: ApiReply) : Exception(reply.code)
interface ApiTransport { suspend fun execute(request: WireRequest): ApiReply }
data class RequestLog(val path: String, val method: String, val status: Int, val code: String,
                      val requestId: String, val elapsedMs: Long, val summary: String)

class ApiClient(private val collectLogs: Boolean) : ApiTransport {
    private val entries = MutableStateFlow<List<RequestLog>>(emptyList())
    val logs: StateFlow<List<RequestLog>> = entries
    fun clearLogs() { entries.value = emptyList() }
    override suspend fun execute(request: WireRequest): ApiReply = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        val path = if (request.path == "/") "/" else "/api/v1" + request.path
        val connection = URL(Protocol.origin(request.origin) + path).openConnection() as HttpsURLConnection
        val method = if (request.body == null) "GET" else "POST"
        var reply: ApiReply? = null
        var summary = "网络连接失败；敏感内容未记录"
        try {
            connection.instanceFollowRedirects = false // 避免将凭证转发到另一个地址。
            connection.connectTimeout = 15000; connection.readTimeout = 15000
            connection.requestMethod = method
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            request.access?.let { connection.setRequestProperty("Authorization", "Bearer " + it) }
            request.key?.let { connection.setRequestProperty("Idempotency-Key", it) }
            request.body?.let {
                val bytes = it.toByteArray(Charsets.UTF_8)
                require(bytes.size <= 16384)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { out -> out.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val bytes = stream?.use { it.readBounded(262145) } ?: ByteArray(0)
            if (bytes.size > 262144) throw IOException("响应过大")
            val raw = bytes.toString(Charsets.UTF_8)
            if (request.path == "/") {
                reply = ApiReply(status, if (status in 200..299) "OK" else "HEALTH_FAILED")
                summary = "健康检查 HTTP " + status
            } else {
                val envelope = try { JsonParser.parseString(raw).asJsonObject } catch (_: Exception) { throw IOException("无效服务端响应") }
                val code = envelope["code"]?.takeIf { it.isJsonPrimitive }?.asString ?: throw IOException("响应缺少业务码")
                if (!code.matches(Regex("[A-Z_]{2,64}"))) throw IOException("无效业务码")
                val data = envelope["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: throw IOException("无效响应数据")
                val requestId = envelope["request_id"]?.asString?.takeIf { it.matches(Regex("[a-f0-9]{32}")) } ?: ""
                val retry = maxOf(connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 0,
                    data["retry_after"]?.asString?.toLongOrNull() ?: 0).coerceIn(0, 86400)
                reply = ApiReply(status, code, data, requestId, retry)
                // 只保存字段结构与业务结果；不保存认证响应、个人资料、验证码等原文。
                summary = "响应字段：" + data.keySet().filter { it.matches(Regex("[a-z_]{1,40}")) }.joinToString(", ")
            }
            reply
        } finally {
            connection.disconnect()
            if (collectLogs) synchronized(entries) {
                entries.value = (listOf(RequestLog(request.path, method, reply?.status ?: 0,
                    reply?.code ?: "NETWORK_ERROR", reply?.requestId ?: "",
                    (System.nanoTime() - started) / 1_000_000, summary)) + entries.value).take(50)
            }
        }
    }
}
