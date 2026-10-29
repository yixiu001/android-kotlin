package cn.yixiu.starter.ui.account

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showRecovery by mutableStateOf(false)
    var now by mutableLongStateOf(System.currentTimeMillis())

    fun closeForms() { phoneForm = false; deletePhoneForm = false }
    fun isActionable(account: AccountView, busy: Boolean) =
        !busy && account.pendingPath == null && !account.privateHidden && now >= account.notBefore
}

@Composable internal fun rememberAccountUiState(account: AccountView): AccountUiState {
    val state = rememberSaveable(account.origin, account.userId, saver = listSaver(
        save = { listOf(it.phoneForm, it.deletePhoneForm, it.editProfile) },
        restore = { AccountUiState().apply { phoneForm = it[0]; deletePhoneForm = it[1]; editProfile = it.getOrElse(2) { false } } }
    )) { AccountUiState() }
    val identity = listOf(account.origin, account.userId, account.phone, account.wechatBound, account.privateHidden).joinToString("|")
    var previousIdentity by rememberSaveable { mutableStateOf(identity) }
    LaunchedEffect(state) {
        while (true) { delay(1000); state.now = System.currentTimeMillis() }
    }
    LaunchedEffect(identity) {
        if (previousIdentity != identity) {
            state.closeForms(); state.editProfile = false; state.confirmDelete = false; state.confirmLogout = false
            state.showRecovery = false
            previousIdentity = identity
        }
    }
    return state
}
