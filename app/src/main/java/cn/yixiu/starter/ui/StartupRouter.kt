package cn.yixiu.starter.ui

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
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when {
                step == StartupStep.ONBOARDING -> {
                    InfoCard {
                        Text("准备好了，从一个小工具开始", style = MaterialTheme.typography.headlineMedium)
                        Text("本地工具可以直接使用。需要跨设备找回账号时，再到“我的”绑定手机号或微信。主题与隐私选项都在设置中。")
                        Button(onClick = onOnboardingComplete) { Text("开始使用") }
                        TextButton(onClick = onOnboardingComplete) { Text("跳过引导") }
                    }
                }
                step == StartupStep.CONSENT || reviewConsent -> {
                    Text("欢迎使用", style = MaterialTheme.typography.headlineMedium)
                    if (details) PrivacyContent(false) else InfoCard {
                        Text(if (BuildConfig.DEBUG) "测试环境隐私提示" else "隐私提示", style = MaterialTheme.typography.titleLarge)
                        Text("本地工具无需账号。你同意后，我们才会连接服务端，创建或恢复游客会话，处理随机安装标识、设备型号、应用版本及请求 IP。")
                        Text("手机号、微信授权仅在你主动使用相关登录功能时处理。拒绝不会删除现有数据，你仍可使用本地功能。")
                        if (BuildConfig.DEBUG) Text("这是开发联调版本，完整产品政策须在正式发布前配置。")
                        DocumentButton("阅读隐私政策", BuildConfig.PRIVACY_POLICY_URL)
                        DocumentButton("阅读用户协议", BuildConfig.TERMS_URL)
                    }
                    TextButton(onClick = { details = !details }) { Text(if (details) "收起详细说明" else "查看详细说明与 SDK 清单") }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { onAccept(); reviewConsent = false }) { Text("同意并继续") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onDecline(); reviewConsent = false }) { Text("暂不同意，使用本地功能") }
                }
                else -> {
                    Text("本地模式", style = MaterialTheme.typography.headlineMedium)
                    Text("账号服务已暂停；已有账号数据仍保留在本机。")
                    localState.SaveableStateProvider("local-tool") { ProductHome() }
                    OutlinedButton(onClick = {
                        focus.clearFocus(); keyboard?.hide(); reviewConsent = true
                    }) { Text("查看隐私说明并启用账号") }
                    generalSettings()
                }
            }
        }
    }
}
