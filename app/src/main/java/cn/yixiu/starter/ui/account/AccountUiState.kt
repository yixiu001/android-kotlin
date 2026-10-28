package cn.yixiu.starter.ui.account

import androidx.compose.runtime.*
import cn.yixiu.starter.core.AccountView
import kotlinx.coroutines.delay

internal class AccountUiState {
    var phoneForm by mutableStateOf(false)
    var confirmLogout by mutableStateOf(false)
    var confirmReset by mutableStateOf(false)
    var confirmChallenge by mutableStateOf(false)
    var editProfile by mutableStateOf(false)
    var confirmDelete by mutableStateOf(false)
    var deletePhoneForm by mutableStateOf(false)
    var now by mutableLongStateOf(System.currentTimeMillis())

    fun closeForms() { phoneForm = false; deletePhoneForm = false }
    fun isActionable(account: AccountView, busy: Boolean) =
        !busy && account.pendingPath == null && !account.privateHidden && now >= account.notBefore
}

@Composable internal fun rememberAccountUiState(account: AccountView): AccountUiState {
    val state = remember { AccountUiState() }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); state.now = System.currentTimeMillis() }
    }
    LaunchedEffect(account.userId, account.phone, account.wechatBound, account.privateHidden) {
        state.closeForms(); state.editProfile = false; state.confirmDelete = false; state.confirmLogout = false
    }
    return state
}
