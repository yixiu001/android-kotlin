package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import cn.yixiu.starter.BuildConfig

/** 新产品只在这里接入业务首页。正常模式与本地模式共用，禁止在此初始化账号/微信 SDK。 */
@Composable fun ProductHome() {
    val copy = LocalResources.current
    if (BuildConfig.DEMO_ENABLED) LocalToolScreen()
    else EmptyState(copy.getString(R.string.product_home_unconfigured), copy.getString(R.string.product_home_unconfigured_help))
}
