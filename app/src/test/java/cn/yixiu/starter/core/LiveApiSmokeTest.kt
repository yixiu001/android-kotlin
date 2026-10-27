package cn.yixiu.starter.core
import cn.yixiu.starter.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/**
 * 显式 HYPERF_LIVE_TEST=true 才运行；创建一个测试游客，不发送短信、不调用微信。
 * 不输出令牌、完整响应或私有资料，结束撤销此会话。
 */
class LiveApiSmokeTest {
    @Test fun guestMeRefreshLogoutAgainstConfiguredTestServer(): Unit = runBlocking {
        assumeTrue(System.getenv("HYPERF_LIVE_TEST") == "true")
        val api = ApiClient(false)
        val base = Protocol.origin(BuildConfig.API_BASE_URL)
        suspend fun call(path: String, body: String? = null, access: String? = null): ApiReply {
            val response = api.execute(Protocol.request(base, path, body, access))
            check(response.code == "OK" && response.status in 200..299) { "Live API failed: " + path + " HTTP " + response.status + " " + response.code }
            println("Live API: " + path + " HTTP " + response.status + " OK")
            return response
        }
        call("/")
        var session = call("/auth/guest", json("installation_id" to UUID.randomUUID().toString(),
            "platform" to "android", "app_version" to "1.0-smoke", "device_name" to "Kotlin integration test").toString()).data
        try {
            call("/me", access = session["access_token"].asString)
            session = call("/auth/refresh", json("refresh_token" to session["refresh_token"].asString).toString()).data
            call("/me", access = session["access_token"].asString)
        } finally {
            val logout = call("/auth/logout", json("refresh_token" to session["refresh_token"].asString).toString())
            assertTrue(logout.data["logged_out"].asBoolean)
        }
    }
}
