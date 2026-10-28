package cn.yixiu.starter.settings

data class AppSettings(
    val acceptedPrivacy: String = "",
    val declinedPrivacy: String = "",
    val onboardingCompleted: String = "",
    val theme: String = "system",
)
enum class StartupStep { CONSENT, LOCAL_ONLY, ONBOARDING, READY }

object StartupPolicy {
    fun accepted(settings: AppSettings, version: String) =
        version.isNotBlank() && settings.acceptedPrivacy == version

    fun step(settings: AppSettings, privacy: String, onboarding: Boolean, onboardingVersion: String): StartupStep = when {
        !accepted(settings, privacy) -> if (privacy.isNotBlank() && settings.declinedPrivacy == privacy)
            StartupStep.LOCAL_ONLY else StartupStep.CONSENT
        onboarding && settings.onboardingCompleted != onboardingVersion -> StartupStep.ONBOARDING
        else -> StartupStep.READY
    }

    fun canWithdraw(busy: Boolean, pendingPath: String?, callbackBusy: Boolean) =
        !busy && pendingPath == null && !callbackBusy
}
