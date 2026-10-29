package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.core.AccountView
import cn.yixiu.starter.core.Protocol
import cn.yixiu.starter.core.readBounded
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import javax.net.ssl.HttpsURLConnection

private val avatarColors = listOf(0xFF16756A, 0xFF2879B9, 0xFFC87832, 0xFF7954B3, 0xFFBD587B, 0xFF566675)
private val avatarNames = listOf(R.string.avatar_green, R.string.avatar_blue, R.string.avatar_orange, R.string.avatar_purple, R.string.avatar_rose, R.string.avatar_gray)

@Composable fun UserAvatar(avatar: String, url: String, modifier: Modifier = Modifier) {
    val copy = LocalResources.current
    // 图片仅在内存中加载，账号/链接改变时移除上一张；微信失败显示内置头像。
    key(avatar, url) {
        val bitmap by produceState<Bitmap?>(null) {
            if (avatar.isBlank()) value = withContext(Dispatchers.IO) { loadWechatAvatar(url) }
        }
        val index = Protocol.avatars.indexOf(avatar).coerceAtLeast(0)
        val sized = modifier.size(64.dp).clip(CircleShape)
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), copy.getString(R.string.avatar_wechat), sized, contentScale = ContentScale.Crop)
        else Canvas(sized.semantics { contentDescription = copy.getString(avatarNames[index]) + copy.getString(R.string.avatar_default_suffix) }) {
            drawCircle(Color(avatarColors[index]))
            drawCircle(Color.White, size.width * 21 / 128, Offset(size.width / 2, size.height * 47 / 128))
            drawArc(Color.White, 180f, 180f, useCenter = true,
                topLeft = Offset(size.width * 24 / 128, size.height * 72 / 128), size = Size(size.width * 80 / 128, size.height * 80 / 128))
        }
    }
}

private fun loadWechatAvatar(url: String): Bitmap? {
    return try {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host !in listOf("thirdwx.qlogo.cn", "wx.qlogo.cn") ||
            uri.userInfo != null || uri.port != -1 || uri.fragment != null || !uri.path.startsWith("/mmopen/")) return null
        val connection = uri.toURL().openConnection() as HttpsURLConnection
        val bytes = try {
            connection.instanceFollowRedirects = false; connection.useCaches = false
            connection.connectTimeout = 5000; connection.readTimeout = 5000
            if (connection.responseCode != 200) return null
            connection.inputStream.use { it.readBounded(1_048_577) }
        } finally { connection.disconnect() }
        if (bytes.size > 1_048_576) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..2048 || bounds.outHeight !in 1..2048) return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
            inSampleSize = maxOf(bounds.outWidth / 256, bounds.outHeight / 256, 1)
        })
    } catch (_: Exception) { null }
}

@Composable fun ProfileEditor(account: AccountView, busy: Boolean, action: (UiAction) -> Unit,
                             feedback: ProfileSaveFeedback = ProfileSaveFeedback(), now: Long = System.currentTimeMillis(),
                             close: () -> Unit) {
    // 身份进入保存状态的组合 key，恢复另一账号/环境时不能载入旧草稿。
    key(account.origin, account.userId) { ProfileEditorContent(account, busy, action, feedback, now, close) }
}

@Composable private fun ProfileEditorContent(account: AccountView, busy: Boolean, action: (UiAction) -> Unit,
                             feedback: ProfileSaveFeedback, now: Long, close: () -> Unit) {
    val copy = LocalResources.current
    var nickname by rememberSaveable(account.origin, account.userId) { mutableStateOf(account.nickname) }
    var avatar by rememberSaveable(account.origin, account.userId) { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable(account.origin, account.userId) { mutableStateOf(false) }
    var awaitingConfirmation by rememberSaveable { mutableStateOf(false) }
    var initialAttempt by rememberSaveable(account.origin, account.userId) { mutableLongStateOf(feedback.attempt) }
    val ownsResult = submitted && feedback.attempt > initialAttempt && feedback.origin == account.origin && feedback.userId == account.userId
    val pending = account.pendingPath != null
    // refresh 可能是资料写入的续接，也可能是启动刷新；仅已提交草稿才能归入前者。
    val profilePending = pending && awaitingConfirmation && account.pendingPath in setOf("/me/profile", "/auth/refresh")
    val cooldown = now < account.notBefore
    val locked = busy || pending
    LaunchedEffect(feedback, ownsResult) {
        if (ownsResult && feedback.status == ProfileSaveStatus.Saved) close()
        if (ownsResult && feedback.status == ProfileSaveStatus.Failed) awaitingConfirmation = false
    }
    val changedName = nickname.trim() != account.nickname
    fun profileInput() = JsonObject().apply {
        if (changedName) addProperty("nickname", nickname.trim())
        avatar?.let { addProperty("avatar", it) }
    }.toString()
    AppDialog(onDismissRequest = { if (!busy) close() }, title = { Text(copy.getString(R.string.profile_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(nickname, { if (it.codePointCount(0, it.length) <= 64) nickname = it },
                    label = { Text(copy.getString(R.string.nickname_label)) }, singleLine = true, enabled = !locked)
                Text(copy.getString(R.string.avatar_selection_help))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                        Protocol.avatars.forEach { preset ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(72.dp).clickable(enabled = !locked) { avatar = preset }.padding(4.dp)) {
                                UserAvatar(preset, "", Modifier.size(48.dp))
                                Text((if ((avatar ?: account.avatar) == preset) "✓ " else "") + copy.getString(avatarNames[Protocol.avatars.indexOf(preset)]))
                            }
                        }
                }
                if (pending) {
                    Text(copy.getString(if (profilePending) R.string.profile_save_pending else R.string.profile_other_pending))
                    OutlinedButton(enabled = !busy && !cooldown, onClick = {
                        if (profilePending) {
                            submitted = true; initialAttempt = feedback.attempt; action(UiAction("recoverProfile", profileInput()))
                        } else action(UiAction("recover"))
                    }) { Text(copy.getString(if (busy) R.string.confirming else if (profilePending) R.string.profile_save_recover else R.string.profile_other_recover)) }
                } else if (ownsResult && feedback.status == ProfileSaveStatus.Failed) {
                    Text(feedback.error, color = MaterialTheme.colorScheme.error)
                }
                if (cooldown) Text(copy.getString(R.string.wait_seconds, ((account.notBefore - now + 999) / 1000)))
            }
        },
        confirmButton = { TextButton(enabled = !locked && !cooldown && (changedName || avatar != null) && (!changedName || nickname.isNotBlank()), onClick = {
            submitted = true; awaitingConfirmation = true; initialAttempt = feedback.attempt
            action(UiAction("profile", profileInput()))
        }) { Text(if (busy) copy.getString(R.string.profile_saving) else copy.getString(R.string.save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = close) { Text(if (pending) copy.getString(R.string.handle_later) else copy.getString(R.string.cancel)) } })
}

@Composable fun WechatProfileDialog(preview: cn.yixiu.starter.core.WechatProfile, busy: Boolean, now: Long, action: (UiAction) -> Unit) {
    val copy = LocalResources.current
    var nickname by rememberSaveable(preview.previewId) { mutableStateOf(preview.canNickname) }
    var avatar by rememberSaveable(preview.previewId) { mutableStateOf(preview.canAvatar) }
    val expired = now >= preview.expiresAt
    AppDialog(onDismissRequest = { if (!busy) action(UiAction("skipWechatProfile")) },
        title = { Text(if (expired) copy.getString(R.string.wechat_preview_expired) else copy.getString(R.string.wechat_profile_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (expired) copy.getString(R.string.wechat_preview_expired_help) else if (preview.overwrite)
                copy.getString(R.string.wechat_profile_overwrite_help) else copy.getString(R.string.wechat_profile_fill_help))
            UserAvatar("", preview.avatarUrl)
            Text(preview.nickname.ifBlank { copy.getString(R.string.wechat_no_nickname) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(nickname, { nickname = it }, enabled = !busy && !expired && preview.canNickname)
                Text(if (preview.canNickname) copy.getString(R.string.wechat_use_nickname) else copy.getString(R.string.wechat_nickname_unavailable))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(avatar, { avatar = it }, enabled = !busy && !expired && preview.canAvatar)
                Text(if (preview.canAvatar) copy.getString(R.string.wechat_use_avatar) else copy.getString(R.string.wechat_avatar_unavailable))
            }
        } },
        confirmButton = { TextButton(enabled = !busy && !expired && (nickname || avatar), onClick = {
            action(UiAction("confirmWechatProfile", JsonObject().apply {
                addProperty("use_nickname", nickname); addProperty("use_avatar", avatar)
            }.toString()))
        }) { Text(copy.getString(R.string.wechat_profile_apply)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { action(UiAction("skipWechatProfile")) }) { Text(if (expired) copy.getString(R.string.close) else copy.getString(R.string.skip)) } })
}
