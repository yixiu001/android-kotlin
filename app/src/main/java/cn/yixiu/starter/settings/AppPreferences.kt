package cn.yixiu.starter.settings

import android.content.Context
import cn.yixiu.starter.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 普通显示设置与账号保险箱分开存放，升级缺项时不推断用户已同意。 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app-settings", Context.MODE_PRIVATE)
    private val current = MutableStateFlow(AppSettings(
        prefs.getString("privacy.accepted", "").orEmpty(),
        prefs.getString("privacy.declined", "").orEmpty(),
        prefs.getString("onboarding.completed", "").orEmpty(),
        prefs.getString("theme", "system").orEmpty().takeIf { it in setOf("system", "light", "dark") } ?: "system",
    ))
    val state = current.asStateFlow()
    fun hasConsent() = StartupPolicy.accepted(current.value, BuildConfig.PRIVACY_VERSION)
    private fun save(next: AppSettings) {
        // 必须先落盘，防止同意写入失败后仍继续账号初始化。
        check(prefs.edit().putString("privacy.accepted", next.acceptedPrivacy)
            .putString("privacy.declined", next.declinedPrivacy)
            .putString("onboarding.completed", next.onboardingCompleted)
            .putString("theme", next.theme).commit()) { "设置保存失败，请检查可用空间后重试" }
        current.value = next
    }
    fun accept() {
        check(BuildConfig.PRIVACY_VERSION.isNotBlank()) { "隐私版本尚未配置" }
        save(current.value.copy(acceptedPrivacy = BuildConfig.PRIVACY_VERSION, declinedPrivacy = ""))
    }
    fun decline() = save(current.value.copy(acceptedPrivacy = "", declinedPrivacy = BuildConfig.PRIVACY_VERSION))
    fun completeOnboarding() = save(current.value.copy(onboardingCompleted = BuildConfig.ONBOARDING_VERSION))
    fun theme(value: String) {
        require(value in setOf("system", "light", "dark"))
        save(current.value.copy(theme = value))
    }
}
