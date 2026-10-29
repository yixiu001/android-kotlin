package cn.yixiu.starter

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.yixiu.starter.ui.*
import cn.yixiu.starter.debug.DeveloperPanel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cn.yixiu.starter.settings.StartupPolicy
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/** 旋转保留账号 VM；撤回同意时清理 VM，但绝不删除账号保险箱。 */
class AccountScope : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    override fun onCleared() { viewModelStore.clear() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as StarterApplication
            val settings by app.preferences.state.collectAsStateWithLifecycle()
            val owner: AccountScope = viewModel()
            var error by remember { mutableStateOf("") }
            fun save(block: () -> Unit) { runCatching(block).onFailure { error = it.message ?: "设置未保存，请稍后重试" } }
            val dark = settings.theme == "dark" || (settings.theme == "system" && isSystemInDarkTheme())
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            StarterTheme(settings.theme) {
                val general: @Composable () -> Unit = { GeneralSettings(settings.theme) { save { app.preferences.theme(it) } } }
                StartupRouter(settings, onAccept = { save { app.preferences.accept() } },
                    onDecline = { save { app.preferences.decline() } },
                    onOnboardingComplete = { save { app.preferences.completeOnboarding() } }, generalSettings = general) {
                    // 同意且完成可选引导后才创建 VM，启动原有账号流程。
                    val vm: StarterViewModel = viewModel(viewModelStoreOwner = owner,
                        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app))
                    val account by vm.account.collectAsStateWithLifecycle()
                    val busy by vm.busy.collectAsStateWithLifecycle()
                    val callbackBusy by app.callbackBusy.collectAsStateWithLifecycle()
                    val message by vm.message.collectAsStateWithLifecycle()
                    val accountError by vm.accountError.collectAsStateWithLifecycle()
                    val activeAction by vm.activeAction.collectAsStateWithLifecycle()
                    val phoneError by vm.phoneError.collectAsStateWithLifecycle()
                    StarterScreen(account, busy || callbackBusy, message, vm::dispatch, settings = general,
                        accountError = accountError, activeAction = activeAction,
                        phoneError = phoneError,
                        consumeMessage = { vm.consumeMessage(message) },
                        privacy = { PrivacyContent(true,
                            StartupPolicy.canWithdraw(busy, account.pendingPath, callbackBusy)) {
                            save {
                                check(StartupPolicy.canWithdraw(vm.busy.value, vm.account.value.pendingPath, app.callbackBusy.value)) {
                                    "请先完成账号操作或恢复待确认请求"
                                }
                                app.preferences.decline()
                                owner.viewModelStore.clear()
                            }
                        } }) { DeveloperPanel(vm) }
                }
                ResultToast(error) { error = "" }
            }
        }
    }
}
