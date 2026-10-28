# 轻量工具基础工程验收

日期：2026-10-29。开发分支 `feature/toolkit-foundation`，基于已推送的 `13b420f`。本次提交包含安卓代码和文档，保留现有测试配置；不包含后端修改或部署。提交前重新执行单元测试、debug 构建、lint 和 release Kotlin 结构编译检查成功。

## 已完成的六项

| 项目 | 实际落地 |
| --- | --- |
| 启动与隐私 | 系统 SplashScreen 兼容层；明确同意后才创建账号 VM；拒绝使用本地模式；撤回暂停服务并保留凭证；可选、可跳过、独立版本引导，默认关闭 |
| 主题与设置 | 跟随系统/浅色/深色；单独偏好保存；只清理 tool-temp 临时目录；隐私、权限、SDK、支持和市场版本链接 |
| 公共 UI 状态 | 加载、空态、错误展示；保留待确认请求的原恢复入口；本地文本计数示例验证无需账号的业务接入 |
| 页面职责 | 单 app 模块，导航/主题/普通设置/账号表单/账号弹窗分别维护；未新增 DI、数据库、后台任务框架 |
| 环境配置 | product.properties 保留产品身份；debug 读取 test.properties，release 独立正式配置；包名、debug 签名、测试 API、微信 AppID 和调试工具开关保持 |
| 发布检查 | 正式产物任务依赖 verifyReleaseReady；验证政策/协议 URL 格式、主体/联系信息、适用备案声明、签名、关闭示例及人工业务完成确认 |

账号协议、AccountEngine、Vault schema 和后端均未修改。未执行 SQL、服务器部署、Redis 配置修改或账号重置。微信资料仍只随登录/绑定提供一次可跳过选择，不增加独立授权入口。

## 构建与测试

- `:app:testDebugUnitTest`：89 项，88 通过、1 项真实 API 冒烟按默认关闭跳过，0 失败。
- `:app:assembleDebug`、`:app:assembleDebugAndroidTest`、`:app:compileReleaseKotlin`：成功。
- `:app:lintDebug`：0 errors、13 warnings（9 项依赖版本提示、4 项 KTX 风格建议；SharedPreferences 保留 commit 返回值以检查持久化失败）。没有在本轮额外升级整套依赖。
- 发布脚本 `--self-test`：34 个正反 fixture 通过。Gradle `:app:verifyReleaseReady` 因当前缺正式配置和签名按预期失败，没有生成正式产物。
- 华为 P30 隔离 UI 测试：7 项原账号页面测试通过；5 项新增启动/隐私/输入保留测试通过。测试使用内存模拟状态，不创建真实账号 VM，不读取或写入账号存储。
- 审查后修复了切页丢输入、输入长度限制以及截断表情代理对的边界。新增返回键测试发现键盘可能优先消费返回，已在打开隐私详情前清焦点并收起键盘；最后重新运行 5 项基础 UI 测试全部通过。
- `git diff --check`：通过。已完成独立只读审查，发现的具体问题均已处理。

本轮真机采用手动 `adb install -r` 安装主 APK 和测试 APK，再用 `am instrument -w -e class ...` 限定测试类；没有执行卸载或清除应用数据命令，也没有运行会操作真实存储的 EncryptedStateStoreTest。仓库保留测试结束不卸载 APK 的 Gradle 选项。

## 真机交付和数据保留

- 设备：Huawei P30 / ELE-AL00 / Android 10。
- 包名：`cn.fuyaoshouhu.app`；版本 `1.0.0` / versionCode 1。
- 签名 MD5：`223fbaaba46bea0e5ef02ee43444dc90`，与原安装一致。
- 首次安装时间始终为 `2026-10-29 04:55:35`，未发生重装清数据。
- 首次覆盖安装后及首轮 12 项 UI 测试后，加密 `no_backup/account-vault` 摘要与实施前记录完全一致。
- 05:29 手机写入当前隐私同意记录；用户明确确认是自己点击“同意并继续”并查看了系统权限。账号服务恢复后保险箱文件发生更新，因此**不把最终文件摘要描述为全程不变**。未执行退出、注销、重置或会话文件恢复覆盖。
- 最终 APK 已覆盖安装并启动，App 前台显示个人中心；没有替用户进行真实短信或微信登录。
- APK：`app/build/outputs/apk/debug/app-debug.apk`。
- APK SHA256：`b92161e3f453d6160d26e332b48338acf2fc09ee8b4ba85d3ebba34e36cc78c5`。

## 验证范围

本轮没有重复真实短信发送、微信登录、注销和重置等会改变账号的流程；以原账号状态机单元测试、隔离 UI 回归、原存储格式不变和覆盖安装核对保证修改边界。正式设备/渠道验收、真实政策内容、备案及产品业务完成情况仍须在各产品发布时检查，构建校验不承诺应用市场审核通过。

参考 Android 官方 [SplashScreen 迁移说明](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate) 和 [Core SplashScreen 发布记录](https://developer.android.com/jetpack/androidx/releases/core#core_splashscreen_version_12_2)：本轮使用 1.2.0 兼容层，无第二个开屏 Activity，无人为延时。
