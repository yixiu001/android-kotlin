package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.settings.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** online 是惰性的 composable：同意之前绝不能在调用方创建账号 ViewModel。 */
@Composable fun StartupRouter(settings: AppSettings, onAccept: () -> Unit, onDecline: () -> Unit,
    onOnboardingComplete: () -> Unit, generalSettings: @Composable () -> Unit = {}, online: @Composable () -> Unit) {
    val copy = LocalResources.current
    var reviewConsent by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val localState = rememberSaveableStateHolder()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val step = StartupPolicy.step(settings, BuildConfig.PRIVACY_VERSION, BuildConfig.ONBOARDING_ENABLED, BuildConfig.ONBOARDING_VERSION)
    BackHandler(enabled = reviewConsent || details) {
        if (details) details = false else reviewConsent = false
    }
    if (step == StartupStep.READY) { online(); return }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PageColumn(Modifier.safeDrawingPadding().imePadding()) {
            when {
                step == StartupStep.ONBOARDING -> {
                    InfoCard {
                        Text(copy.getString(R.string.product_onboarding_title), style = MaterialTheme.typography.headlineMedium)
                        Text(copy.getString(R.string.product_onboarding_body))
                        Button(onClick = onOnboardingComplete) { Text(copy.getString(R.string.onboarding_start)) }
                        TextButton(onClick = onOnboardingComplete) { Text(copy.getString(R.string.onboarding_skip)) }
                    }
                }
                step == StartupStep.CONSENT || reviewConsent -> {
                    Text(copy.getString(R.string.product_welcome), style = MaterialTheme.typography.headlineMedium)
                    if (details) PrivacyContent(false) else InfoCard {
                        Text(if (BuildConfig.DEBUG) copy.getString(R.string.privacy_debug_title) else copy.getString(R.string.privacy_title), style = MaterialTheme.typography.titleLarge)
                        Text(copy.getString(R.string.product_consent_account))
                        Text(copy.getString(R.string.product_consent_identity))
                        if (BuildConfig.DEBUG) Text(copy.getString(R.string.product_consent_debug))
                        DocumentButton(copy.getString(R.string.privacy_policy_read), BuildConfig.PRIVACY_POLICY_URL)
                        DocumentButton(copy.getString(R.string.terms_read), BuildConfig.TERMS_URL)
                    }
                    TextButton(onClick = { details = !details }) { Text(if (details) copy.getString(R.string.details_collapse) else copy.getString(R.string.details_show)) }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { onAccept(); reviewConsent = false }) { Text(copy.getString(R.string.consent_accept)) }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onDecline(); reviewConsent = false }) { Text(copy.getString(R.string.consent_decline)) }
                }
                else -> {
                    Text(copy.getString(R.string.local_mode), style = MaterialTheme.typography.headlineMedium)
                    Text(copy.getString(R.string.product_local_mode_help))
                    localState.SaveableStateProvider("local-tool") { ProductHome() }
                    OutlinedButton(onClick = {
                        focus.clearFocus(); keyboard?.hide(); reviewConsent = true
                    }) { Text(copy.getString(R.string.consent_review)) }
                    generalSettings()
                }
            }
        }
    }
}
