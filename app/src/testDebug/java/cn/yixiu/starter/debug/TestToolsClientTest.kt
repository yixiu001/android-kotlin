package cn.yixiu.starter.debug
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
class TestToolsClientTest {
    private class Connection(url: URL, private val status: Int, private val response: String): HttpsURLConnection(url) {
        val sent = ByteArrayOutputStream()
        override fun getOutputStream() = sent
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
        override fun getResponseCode() = status
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun connect() {}
        override fun getCipherSuite() = "TLS"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
    @Test fun previewBindsHttpsOriginAndNeverFollowsRedirects() = runBlocking {
        val c = Connection(URL("https://example.com/test-tools/v1/account-reset/preview"), 302, "")
        var requested = ""
        val client = TestToolsClient("https://EXAMPLE.com") { url -> requested = url.toString(); c }
        try { client.preview("A".repeat(43)); fail("redirect accepted") } catch (_: java.io.IOException) {}
        assertEquals("https://example.com/test-tools/v1/account-reset/preview", requested)
        assertFalse(c.instanceFollowRedirects)
        assertEquals("POST", c.requestMethod)
        assertEquals("Bearer " + "A".repeat(43), c.getRequestProperty("Authorization"))
        assertEquals("{}", c.sent.toString("UTF-8"))
    }
    @Test fun prepareUsesAccessBearerAndEmptyBody() = runBlocking {
        val token = "A".repeat(43)
        val c = Connection(URL("https://example.com"), 200, """{"code":"OK","data":{"user_id":"42","confirmation":"${"B".repeat(43)}","grant":"${"C".repeat(43)}"}}""")
        var requested = ""
        val p = TestToolsClient("https://example.com") { requested = it.toString(); c }.prepare(token)
        assertEquals("https://example.com/test-tools/v1/account-reset/prepare", requested)
        assertEquals("Bearer $token", c.getRequestProperty("Authorization"))
        assertEquals("{}", c.sent.toString("UTF-8"))
        assertEquals("C".repeat(43), p["grant"].asString)
    }
    @Test fun prepareRejectsMissingGrant() = runBlocking {
        val c = Connection(URL("https://example.com"), 200, """{"code":"OK","data":{"user_id":"42","confirmation":"${"B".repeat(43)}"}}""")
        try { TestToolsClient("https://example.com") { c }.prepare("A".repeat(43)); fail("missing grant accepted") } catch (_: IllegalStateException) {}
    }
    @Test fun executePostsSameConfirmationAndRejectsWrongTarget() = runBlocking {
        val c = Connection(URL("https://example.com"), 200, """{"code":"OK","data":{"reset":true,"user_id":"other","local_reset_required":true}}""")
        val client = TestToolsClient("https://example.com") { c }
        val p = com.google.gson.JsonParser.parseString("""{"user_id":"42","confirmation":"${"B".repeat(43)}"}""").asJsonObject
        try { client.execute("A".repeat(43), p); fail("wrong target accepted") } catch (_: IllegalStateException) {}
        val body = com.google.gson.JsonParser.parseString(c.sent.toString("UTF-8")).asJsonObject
        assertEquals("42", body["user_id"].asString)
        assertEquals("B".repeat(43), body["confirmation"].asString)
        assertEquals(setOf("user_id", "confirmation"), body.keySet())
    }
}
