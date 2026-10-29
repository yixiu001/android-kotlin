package cn.yixiu.starter.ui.account

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.ui.*

@Composable internal fun AccountDialogs(account: AccountView, busy: Boolean,
                                        state: AccountUiState, action: (UiAction) -> Unit,
                                        profileSave: ProfileSaveFeedback = ProfileSaveFeedback()) {
    val copy = LocalResources.current
    val now = state.now
    if (state.editProfile && !account.privateHidden) ProfileEditor(account, busy, action,
        feedback = profileSave, now = now) { state.editProfile = false }
    account.wechatProfile?.takeIf { account.pendingPath == null && !account.privateHidden }?.let { preview ->
        WechatProfileDialog(preview, busy || account.pendingPath != null, now, action)
    }
    if (state.confirmDelete) AppDialog(onDismissRequest = { state.confirmDelete = false },
        title = { Text(copy.getString(R.string.delete_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(copy.getString(R.string.delete_warning))
            Text(copy.getString(R.string.delete_identity_warning))
            Text(copy.getString(R.string.delete_verify_prompt))
            if (account.phone.isNotBlank()) OutlinedButton(enabled = BuildConfig.SMS_ENABLED, onClick = {
                state.confirmDelete = false; state.phoneForm = false; state.deletePhoneForm = true
            }) { Text(copy.getString(R.string.delete_by_phone)) }
            if (account.wechatBound) OutlinedButton(enabled = BuildConfig.WECHAT_ENABLED && BuildConfig.WECHAT_APP_ID.isNotBlank(), onClick = {
                state.confirmDelete = false; action(UiAction("deleteWechat"))
            }) { Text(copy.getString(R.string.delete_by_wechat)) }
        } },
        confirmButton = { TextButton(onClick = { state.confirmDelete = false }) { Text(copy.getString(R.string.delete_cancel)) } })
    if (state.confirmLogout) AppDialog(onDismissRequest = { state.confirmLogout = false },
        title = { Text(copy.getString(R.string.logout_title)) },
        text = { Text(copy.getString(R.string.logout_warning)) },
        confirmButton = { TextButton(onClick = { state.confirmLogout = false; action(UiAction("logout")) }) { Text(copy.getString(R.string.logout_confirm)) } },
        dismissButton = { TextButton(onClick = { state.confirmLogout = false }) { Text(copy.getString(R.string.cancel)) } })
    if (state.confirmReset) AppDialog(onDismissRequest = { state.confirmReset = false },
        title = { Text(copy.getString(R.string.relogin_title)) },
        text = { Text(copy.getString(R.string.relogin_warning)) },
        confirmButton = { TextButton(onClick = { state.confirmReset = false; action(UiAction("restartInvalid")) }) { Text(copy.getString(R.string.relogin_confirm)) } },
        dismissButton = { TextButton(onClick = { state.confirmReset = false }) { Text(copy.getString(R.string.cancel)) } })
    if (state.confirmChallenge) AppDialog(onDismissRequest = { state.confirmChallenge = false },
        title = { Text(copy.getString(R.string.abandon_title)) },
        text = { Text(copy.getString(R.string.abandon_warning)) },
        confirmButton = { TextButton(onClick = { state.confirmChallenge = false; action(UiAction("abandonChallenge")) }) { Text(copy.getString(R.string.abandon_confirm)) } },
        dismissButton = { TextButton(onClick = { state.confirmChallenge = false }) { Text(copy.getString(R.string.cancel)) } })
}
