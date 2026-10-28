package cn.yixiu.starter.debug

import android.app.ActivityManager
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cn.yixiu.starter.ui.StarterViewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.*

private class ResetMemory {
    val preview = mutableStateOf<JsonObject?>(null)
    val generation = mutableStateOf<Long?>(null)
    val binding = mutableStateOf<Pair<String, String>?>(null)
    val running = mutableStateOf(false)
    val unknown = mutableStateOf(false)
    val done = mutableStateOf(false)
    val message = mutableStateOf("")
    val confirm = mutableStateOf(false)
    val localConfirm = mutableStateOf(false)
}
private val memories = java.util.WeakHashMap<StarterViewModel, ResetMemory>()

@Composable fun ResetTools(vm: StarterViewModel, origin: String, currentUser: String) {
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current.applicationContext
    // ViewModel-lifetime memory survives panel navigation/recomposition, never persisted.
    val memory = remember(vm) { memories.getOrPut(vm) { ResetMemory() } }
    var preview by memory.preview
    var generation by memory.generation
    var binding by memory.binding
    var running by memory.running
    var unknown by memory.unknown
    var done by memory.done
    var message by memory.message
    var confirm by memory.confirm
    var localConfirm by memory.localConfirm
    val client = remember(origin) { TestToolsClient(origin) }
    val engine = vm.engine
    fun clearLocal() {
        val accepted = runCatching { (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData() }.getOrDefault(false)
        if (!accepted) message = "系统未接受清除请求，请重试清除。"
    }
    fun execute() {
        val original = preview ?: return
        if (running) return
        val bound = binding ?: return
        running = true; vm.busy.value = true
        vm.launchTestTool {
            try {
                requireNotNull(engine).withTestAccount(bound.first, bound.second, generation) { _, revision ->
                    TestToolsClient(bound.first).execute(original["grant"].asString, original)
                    check(ResetPolicy.mayAutoClear(bound.second, original["user_id"].asString, requireNotNull(engine).state.value.origin, bound.first, revision, generation!!, true, false))
                    done = true; unknown = false
                    withContext(Dispatchers.Main) { clearLocal() }
                }
            } catch (error: Exception) {
                val recovery = recoverResetFailure(original, error, priorUnknown = unknown)
                preview = recovery.preview; unknown = recovery.unknown
                message = if (unknown && error is TestToolsFailure && error.code in setOf("TEST_GRANT_INVALID", "TEST_TOOLS_DISABLED")) "原请求结果仍未知；授权过期或服务端关闭不能证明此前未成功。请先在服务端核实，禁止重新准备或清除本地。" else if (error is TestToolsFailure) error.message.orEmpty() else "结果未确认；仅可重试原请求，不会清除本地数据。"
            } finally { running = false; vm.busy.value = unknown || done }
        }
    }
    Text("当前账号重置 · 环境：${client.origin} · 账号：$currentUser")
    TextButton(enabled = !busy && !unknown && !done && currentUser.isNotBlank(), onClick = {
        vm.busy.value = true
        vm.launchTestTool {
            try {
                requireNotNull(engine).withTestAccount(client.origin, currentUser, prepare = true) { access, revision ->
                    val prepared = client.prepare(access)
                    check(prepared["user_id"].asString == currentUser) { "目标与当前账号不一致" }
                    preview = prepared; generation = revision; binding = client.origin to currentUser
                }
                confirm = true
            } catch (_: Exception) { message = "无法准备当前账号重置，请检查会话与测试环境。"; vm.busy.value = false }
        }
    }) { Text("重置当前测试账号及全部本地数据") }
    if (unknown) {
        Text("结果未知：保留原授权与确认值。返回面板可继续；进程结束会丢失恢复材料。", color = MaterialTheme.colorScheme.error)
        TextButton(enabled = !running, onClick = { execute() }) { Text("重试同一重置请求") }
    }
    if (done) TextButton(onClick = {
        vm.launchTestTool {
            try { requireNotNull(engine).withTestAccount(binding!!.first, binding!!.second, generation) { _, _ -> withContext(Dispatchers.Main) { clearLocal() } } }
            catch (_: Exception) { message = "账号或环境已改变，禁止清除。" }
        }
    }) { Text("服务端已成功，重试清除本地数据") }
    Text(message)
    TextButton(enabled = !busy && !unknown, onClick = { localConfirm = true }) { Text("仅清除此应用全部本地数据") }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false; vm.busy.value = false; preview = null },
        title = { Text("重置当前账号并清除全部本地数据？") },
        text = { Text("环境：${client.origin}\n目标：${preview?.get("user_id")}\n类型：${preview?.get("account_type")}；状态：${preview?.get("status")}\n手机号：${preview?.get("phone_masked")}；微信：${preview?.get("wechat_bound")}；关联游客：${preview?.get("related_guest_count")}\n授权到期：${preview?.get("expires_at")}\n将解除当前账号手机号/微信身份、会话及关联游客认证数据。成功后自动清除此应用全部私有文件、数据库、偏好、缓存和引导标记，并结束应用，请重新打开。其他账号与微信授权不变。") },
        confirmButton = { TextButton(onClick = { confirm = false; execute() }) { Text("确认重置并自动清除") } },
        dismissButton = { TextButton(onClick = { confirm = false; preview = null; vm.busy.value = false }) { Text("取消") } })
    if (localConfirm) AlertDialog(onDismissRequest = { localConfirm = false }, title = { Text("仅清除此应用全部本地数据？") },
        text = { Text("清除全部私有文件、数据库、会话、偏好、缓存和引导标记，重置权限并结束应用。请重新打开。不重置服务端账号、不删除 APK、其他应用或共享媒体、不撤销微信授权。") },
        confirmButton = { TextButton(onClick = { localConfirm = false; clearLocal() }) { Text("确认清除并关闭应用") } },
        dismissButton = { TextButton(onClick = { localConfirm = false }) { Text("取消") } })
}
