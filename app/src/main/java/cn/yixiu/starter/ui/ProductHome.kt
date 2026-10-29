package cn.yixiu.starter.ui

import androidx.compose.runtime.Composable
import cn.yixiu.starter.BuildConfig

/** 新产品只在这里接入业务首页。正常模式与本地模式共用，禁止在此初始化账号/微信 SDK。 */
@Composable fun ProductHome() {
    if (BuildConfig.DEMO_ENABLED) LocalToolScreen()
    else EmptyState("首页功能尚未配置", "请接入产品业务首页后再发布正式版本。")
}
