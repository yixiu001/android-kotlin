package cn.yixiu.starter

import android.app.Application
import android.os.Build
import cn.yixiu.starter.core.*
import cn.yixiu.starter.storage.EncryptedStateStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import cn.yixiu.starter.settings.AppPreferences

class StarterApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val notice = MutableStateFlow("")
    val preferences by lazy { AppPreferences(this) }
    val callbackBusy = MutableStateFlow(false)
    val api by lazy { ApiClient(BuildConfig.DEBUG) }
    val engine by lazy {
        AccountEngine(EncryptedStateStore(this), api, BuildConfig.API_BASE_URL,
            BuildConfig.VERSION_NAME, Build.MODEL, BuildConfig.DEBUG, onAccountCleared = {
                // 未来业务私有缓存统一写入此专用目录；不得删除整个应用 filesDir。
                val cache = File(cacheDir, "account-private")
                if (cache.isDirectory) cache.deleteRecursively()
            })
    }
}
