package cn.yixiu.starter.core

import com.google.gson.*
import java.net.URI
import java.security.SecureRandom
import java.util.Base64

data class WireRequest(val origin: String, val path: String, val body: String?, val access: String?, val key: String?)
object Protocol {
    private val random = SecureRandom()
    val paths = listOf("/auth/guest", "/me", "/auth/sms/challenges", "/auth/login/phone",
        "/me/identities/phone", "/auth/wechat/challenges", "/auth/login/wechat",
        "/me/identities/wechat", "/auth/refresh", "/auth/logout")
    fun origin(input: String): String {
        val uri = try { URI(input.trim()) } catch (_: Exception) { throw IllegalArgumentException("请输入合法的 HTTPS 地址") }
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path in listOf("", "/") &&
            (uri.port == -1 || uri.port in 1..65535)) { "仅支持 HTTPS 域名及可选端口，不含路径、账号、查询参数" }
        return URI("https", null, uri.host.lowercase(), uri.port, null, null, null).toASCIIString()
    }
    fun newKey(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
    fun request(origin: String, path: String, body: String?, access: String?): WireRequest {
        require(path in paths || path == "/")
        return WireRequest(origin(origin), path, body,
            if (path in listOf("/auth/guest", "/auth/refresh", "/")) null else access,
            if (body == null) null else newKey())
    }
    // 非 JSON 响应（网关错误等）不展示原文，以免意外携带凭证。
    fun redact(text: String): String = try {
        val root = JsonParser.parseString(text)
        fun clean(value: JsonElement): JsonElement = when {
            value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(clean(it)) } }
            value.isJsonObject -> JsonObject().apply {
                value.asJsonObject.entrySet().forEach { (key, child) ->
                    if (key.lowercase() in setOf("access_token","refresh_token","authorization","idempotency-key",
                            "phone","code","state","challenge_id","installation_id","openid","unionid"))
                        addProperty(key, "[已隐藏]")
                    else add(key, clean(child))
                }
            }
            else -> value
        }
        clean(root).toString()
    } catch (_: Exception) { "[非 JSON 内容已隐藏]" }
}
