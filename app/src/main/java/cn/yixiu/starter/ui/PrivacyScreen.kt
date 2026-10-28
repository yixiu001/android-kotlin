package cn.yixiu.starter.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.yixiu.starter.BuildConfig

@Composable fun PrivacyContent(consented: Boolean, canWithdraw: Boolean = false, onWithdraw: () -> Unit = {}) {
    var withdrawal by rememberSaveable { mutableStateOf(false) }
    InfoCard {
        Text(if (BuildConfig.DEBUG) "测试环境隐私说明" else "隐私与个人信息", style = MaterialTheme.typography.titleLarge)
        Text(if (BuildConfig.DEBUG) "当前为开发联调版本。本地文本工具无需账号；以下说明仅覆盖本基础工程，不替代新产品上线时的完整隐私政策。"
            else "请阅读本产品隐私政策和用户协议。你可使用无需账号的本地功能，账号功能将在你同意后启用。")
        Text("同意后，应用会连接账号服务，创建或恢复游客会话。服务端处理随机安装标识、设备型号、应用版本及请求 IP，用于会话管理和安全控制。凭证加密保存在本机。")
        Text("仅当你选择手机号登录时发送手机号及验证码；仅当你点击微信登录或绑定时启动微信 SDK，授权结果交给服务端核验。微信头像和昵称可在登录后选择使用或跳过。")
        Text("拒绝或撤回后暂停账号服务，保留本机凭证，不执行退出、注销或清空。撤回不代表删除服务端数据；注销请在“账号与安全”操作。")
        DocumentButton("阅读隐私政策", BuildConfig.PRIVACY_POLICY_URL)
        DocumentButton("阅读用户协议", BuildConfig.TERMS_URL)
        Text("说明版本：${BuildConfig.PRIVACY_VERSION.ifBlank { "未配置" }}", style = MaterialTheme.typography.bodySmall)
        if (BuildConfig.OPERATOR_NAME.isNotBlank()) Text("运营主体：${BuildConfig.OPERATOR_NAME}")
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) Text("隐私联系：${BuildConfig.SUPPORT_EMAIL}")
    }
    InfoCard {
        Text("第三方 SDK 与信息使用", style = MaterialTheme.typography.titleMedium)
        if (BuildConfig.WECHAT_ENABLED) {
            Text("微信 OpenSDK · 腾讯 · 用于微信授权登录。仅在主动使用微信相关功能后调用；微信客户端及 SDK 按其说明处理应用标识、授权信息和必要设备信息。")
            DocumentButton("微信 SDK 隐私说明", "https://weixin.qq.com/cgi-bin/readtemplate?t=opensdk_agreement&lang=zh_CN")
        } else Text("当前配置未启用微信登录。")
        Text("短信通过自有后端调用阿里云短信服务，客户端不集成阿里云短信 SDK。模板未集成广告、推送或统计 SDK。上线前新增 SDK 必须同步此清单。")
    }
    InfoCard {
        Text("权限与数据", style = MaterialTheme.typography.titleMedium)
        Text("当前模板声明网络权限，不申请通讯录、定位、相机或存储等运行时权限。本地文本计数不会上传输入内容，复制结果仅由你点击触发。")
        val context = LocalContext.current
        var error by remember { mutableStateOf("") }
        OutlinedButton(onClick = {
            error = runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))) }.exceptionOrNull()?.let { "无法打开系统设置" }.orEmpty()
        }) { Text("查看系统权限设置") }
        if (error.isNotBlank()) Text(error)
    }
    if (consented) InfoCard {
        Text("已同意当前隐私说明")
        OutlinedButton(onClick = { withdrawal = true }, enabled = canWithdraw) { Text("撤回隐私同意") }
        if (!canWithdraw) Text("账号操作正在进行或结果待确认，请先完成恢复，再撤回。")
    }
    if (withdrawal) AlertDialog(onDismissRequest = { withdrawal = false }, title = { Text("暂停账号服务？") },
        text = { Text("保留现有账号数据和凭证，停止后续账号请求。你仍可使用本地功能，之后可以重新同意并继续。") },
        confirmButton = { TextButton(enabled = canWithdraw, onClick = { withdrawal = false; onWithdraw() }) { Text("确认撤回") } },
        dismissButton = { TextButton(onClick = { withdrawal = false }) { Text("取消") } })
}

@Composable fun DocumentButton(label: String, url: String) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    if (url.isNotBlank()) TextButton(onClick = {
        error = runCatching {
            val uri = Uri.parse(url)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank())
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.exceptionOrNull()?.let { "暂时无法打开，请稍后重试或联系支持" }.orEmpty()
    }) { Text(label) }
    else if (BuildConfig.DEBUG) Text("$label：正式地址待产品配置", style = MaterialTheme.typography.bodySmall)
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
}
