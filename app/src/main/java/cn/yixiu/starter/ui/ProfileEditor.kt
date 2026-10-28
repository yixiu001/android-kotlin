package cn.yixiu.starter.ui

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
private val avatarNames = listOf("青绿", "海蓝", "暖橙", "紫色", "玫红", "灰蓝")

@Composable fun UserAvatar(avatar: String, url: String, modifier: Modifier = Modifier) {
    // 图片仅在内存中加载，账号/链接改变时移除上一张；微信失败显示内置头像。
    key(avatar, url) {
        val bitmap by produceState<Bitmap?>(null) {
            if (avatar.isBlank()) value = withContext(Dispatchers.IO) { loadWechatAvatar(url) }
        }
        val index = Protocol.avatars.indexOf(avatar).coerceAtLeast(0)
        val sized = modifier.size(64.dp).clip(CircleShape)
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), "微信头像", sized, contentScale = ContentScale.Crop)
        else Canvas(sized.semantics { contentDescription = avatarNames[index] + "默认头像" }) {
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

@Composable fun ProfileEditor(account: AccountView, busy: Boolean, action: (UiAction) -> Unit, close: () -> Unit) {
    var nickname by remember(account.userId) { mutableStateOf(account.nickname) }
    var avatar by remember(account.userId) { mutableStateOf<String?>(null) }
    val changedName = nickname.trim() != account.nickname
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("编辑资料") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(nickname, { if (it.codePointCount(0, it.length) <= 64) nickname = it },
                    label = { Text("昵称（最多 64 个字符）") }, singleLine = true, enabled = !busy)
                Text("选择内置头像，不支持上传图片。保留当前选择即可继续使用微信头像。")
                Protocol.avatars.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { preset ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(enabled = !busy) { avatar = preset }.padding(4.dp)) {
                                UserAvatar(preset, "", Modifier.size(48.dp))
                                Text((if ((avatar ?: account.avatar) == preset) "✓ " else "") + avatarNames[Protocol.avatars.indexOf(preset)])
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy && (changedName || avatar != null) && (!changedName || nickname.isNotBlank()), onClick = {
            val input = JsonObject().apply {
                if (changedName) addProperty("nickname", nickname.trim())
                avatar?.let { addProperty("avatar", it) }
            }
            action(UiAction("profile", input.toString())); close()
        }) { Text("保存") } },
        dismissButton = { TextButton(enabled = !busy, onClick = close) { Text("取消") } })
}
