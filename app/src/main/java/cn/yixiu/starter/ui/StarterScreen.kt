package cn.yixiu.starter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.account.*

data class UiAction(val name: String, val value: String = "")

/** Application navigation; account forms and recovery are isolated in ui/account. */
@Composable fun StarterScreen(account: AccountView, busy: Boolean, message: String,
                              action: (UiAction) -> Unit,
                              settings: @Composable () -> Unit = {},
                              privacy: @Composable () -> Unit = { ProfilePrivacy() },
                              accountError: String = "",
                              activeAction: String = "",
                              phoneError: String = "",
                              consumeMessage: () -> Unit = {},
                              developer: @Composable () -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val pageState = rememberSaveableStateHolder()
    var profilePage by rememberSaveable { mutableStateOf(ProfilePage.Overview) }
    val accountUi = rememberAccountUiState(account)
    val actionable = accountUi.isActionable(account, busy)
    ResultToast(message, consumeMessage)
    fun backToProfile() {
        profilePage = ProfilePage.Overview
        accountUi.closeForms()
    }
    BackHandler(enabled = tab == 1 && profilePage != ProfilePage.Overview) { backToProfile() }
    Scaffold(topBar = {
        if (tab == 1) Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (profilePage != ProfilePage.Overview) {
                    IconButton(onClick = { backToProfile() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
                    Spacer(Modifier.width(4.dp))
                }
                Text(profilePage.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }, bottomBar = {
        if (tab == 0 || profilePage == ProfilePage.Overview) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("首页") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.PersonOutline, null) }, label = { Text("我的") })
            }
        }
    }) { padding ->
        key(tab, profilePage) {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = if (tab == 0) 24.dp else 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (tab == 0) {
                    pageState.SaveableStateProvider("home-tool") { ProductHome() }
                } else when (profilePage) {
                    ProfilePage.Overview -> ProfileOverview(account, actionable, { profilePage = it }, { accountUi.editProfile = true })
                    ProfilePage.Help -> ProfileHelp()
                    ProfilePage.Privacy -> privacy()
                    ProfilePage.About -> ProfileAbout()
                    ProfilePage.Settings -> {
                        settings()
                        if (account.hasSession && !account.privateHidden) InfoCard {
                            Text("账号信息", fontWeight = FontWeight.SemiBold)
                            Text("重新获取当前账号的最新资料。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            OutlinedButton(onClick = { action(UiAction("me")) }, enabled = actionable) { Text(if (activeAction == "me") "正在更新…" else "更新账号信息") }
                        }
                        developer()
                    }
                    ProfilePage.Account -> {
                        AccountStatus(account, busy, accountError, accountUi, action)
                        AccountPage(account, actionable, accountUi, action, activeAction, phoneError = phoneError)
                    }
                }
            }
        }
    }
    AccountDialogs(account, busy, accountUi, action)
}

@Composable fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
