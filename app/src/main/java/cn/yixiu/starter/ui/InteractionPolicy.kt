package cn.yixiu.starter.ui

import java.net.URI

/** 只去除大陆号码的常见排版；其他国家前缀、字母和超长输入仍交给校验提示。 */
internal fun normalizeMainlandPhone(value: String): String =
    value.filterNot { it.isWhitespace() || it == '-' }.removePrefix("+86")

internal fun validMainlandPhone(value: String) = value.matches(Regex("1[3-9][0-9]{9}"))
internal fun showLoginEntry(enabled: Boolean, debug: Boolean) = enabled || debug

internal sealed interface SupportDestination {
    data class Web(val url: String) : SupportDestination
    data class Email(val address: String) : SupportDestination
}

internal fun supportDestination(url: String, email: String): SupportDestination? {
    val web = url.trim()
    val validWeb = runCatching { URI(web).let { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null } }.getOrDefault(false)
    return when {
        validWeb -> SupportDestination.Web(web)
        email.isNotBlank() -> SupportDestination.Email(email.trim())
        else -> null
    }
}
