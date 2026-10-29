# android-kotlin

可复制的 Android 产品基础工程，Kotlin + Jetpack Compose，对接 [hyperf-swow](https://github.com/yixiu001/hyperf-swow) 单产品账号体系。

当前配置、配套后端、验证结果和待处理问题统一见[当前状态](docs/current-state.md)；按[文档导航](docs/README.md)查阅接入、复制和历史记录。新产品模板默认值与已部署测试环境的实际状态分开记录。

内置中文本地工具首页、个人中心、明确同意隐私说明后的游客会话、手机验证码登录/绑定、微信授权登录/绑定、会话刷新、当前设备退出、昵称/默认头像编辑和验证注销。微信登录/绑定成功后先建立最终账号，再提供一次可跳过的资料选择，仅补充尚未设置的字段；编辑资料不再提供微信重新授权入口。头像使用微信链接，无上传或 OSS。新增启动隐私门禁、可关闭引导、浅色/深色/跟随系统主题、普通设置和受限临时缓存清理。首页文本计数为本地示例，新产品应替换为实际业务。仍是单 app 模块，开发工具只存在于 debug 版本。

[联调工具](docs/test-tools.md)支持当前已登录测试账号一键 prepare/确认/重置，服务端确认成功后自动清除全部本地数据并结束应用；另有独立本地清除按钮。本项目 debug 已开启，配置缺项回退 false，release 强制 false 且不包含实现。

## 快速开始

个人中心采用资料头部与分组列表；登录/绑定、退出、注销位于“我的 → 账号与安全”，开发工具位于“我的 → 通用设置”。完整页面与公开链接配置见[个人中心说明](docs/profile-center.md)。

普通操作结果使用一次性 Toast，确认/选择使用弹窗，输入错误就地提示。账号恢复集中在账号页，不打扰工具首页；正式版隐藏未开放登录方式。反馈入口统一使用网页优先、邮箱兜底。复制新产品只需在 `ui/ProductHome.kt` 接入业务首页，正常模式和本地模式共用；文本工具仅为占位。

资料保存只有收到成功结果才关闭，明确失败保留昵称和头像草稿，未知结果继续确认原请求；草稿支持界面重建并按账号/环境隔离。页面与弹窗补充窄屏、横屏和大字体适配。通用界面文案在 `app/src/main/res/values/strings.xml`，产品帮助、隐私说明、引导及示例文案在同目录 `product_strings.xml`；不增加语言切换。验证见[资料保存与适配验收](docs/verification-2026-10-29-profile-adaptive-copy.md)。

1. Android Studio 打开本目录并 Gradle Sync。
2. SDK Manager 安装 Android API 37（本机为 37.0）和 Build Tools 36.0.0。Gradle JDK 使用 Android Studio 自带 JBR，本机 Java 25 已验证构建。
3. 检查 [product.properties](product.properties) 的应用身份和 [environments/test.properties](environments/test.properties) 的 debug 配置；测试 API 保持 https://hyperf.tongyiwl.bond。release 使用独立正式配置，见[构建环境](docs/build-environments.md)。
4. 连接 Android 8.0/API 26 或以上设备，选择 app → Run。首次启动先展示隐私说明；明确同意后才创建或恢复会话。旧版覆盖升级保留原 Vault，但缺少同意记录时同样先询问。暂不同意可使用本地文本工具。

Windows 构建命令（按本机实际路径设置 JAVA_HOME）：

    $env:JAVA_HOME = 'D:\android-studio\jbr'
    .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

测试 APK：app/build/outputs/apk/debug/app-debug.apk。

无需另装 Kotlin 编译器。工程固定 Gradle 9.6.0、AGP 9.4.1、Kotlin/Compose Compiler 2.2.10、Compose BOM 2025.08.00；升级编译插件时应成套验证。

## 产品配置

`product.properties` 仅维护 APP_NAME、APPLICATION_ID、VERSION_CODE、VERSION_NAME。以下 API、开关及公开信息由 `environments/test.properties`（debug）或不入库的 `environments/production.properties`（release）提供，正式配置不会回退到测试环境。

| 配置 | 用途 |
| --- | --- |
| APP_NAME | 应用显示名称，UTF-8 中文 |
| APPLICATION_ID | 应用安装身份，复制产品必改，微信开放平台须一致 |
| API_BASE_URL | HTTPS origin，不带 /api/v1、路径或查询参数 |
| WECHAT_APP_ID | 微信移动应用 AppID，不是 AppSecret |
| SMS_ENABLED | 客户端短信入口开关，不代表服务端已启用 |
| WECHAT_ENABLED | 客户端微信开关，需平台和服务端配置匹配 |
| TEST_TOOLS_ENABLED | debug 联调工具开关，默认 false；release 强制 false |
| VERSION_CODE / VERSION_NAME | 安装升级版本号与显示版本，版本字符串最多 32 字节 |
| FEEDBACK_URL | 可选公开 HTTPS 反馈地址；空值显示暂未开放 |
| PRIVACY_POLICY_URL / TERMS_URL | 公开 HTTPS 政策与协议；正式发布检查必填 |
| PRIVACY_VERSION | 隐私说明版本；变化后重新征得同意，正式以 production- 开头 |
| ONBOARDING_ENABLED / ONBOARDING_VERSION | 可跳过的引导及版本；默认关闭，独立记录完成状态 |
| DEMO_ENABLED | 本地文本工具示例开关；正式产品须替换真实首页并关闭示例 |
| OPERATOR_NAME / SUPPORT_EMAIL / APP_RECORD_NUMBER | 产品运营主体、支持邮箱及适用时的备案号 |
| APP_MARKET_URL | 可选应用市场 HTTPS 链接；不内置下载或自动安装 |

第一次复制请完成 [新产品配置清单](docs/new-product-checklist.md)。当前 debug 保留 cn.fuyaoshouhu.app、测试后端及公开微信 AppID；复制新产品必须配置自己的身份与环境。本次更新完成资料保存、基础适配和文案集中，不涉及后端修改或部署。配套测试后端继续沿用 `f105663` / Jenkins #22 / SQL004；当前检查与安装结果见[资料保存与适配验收](docs/verification-2026-10-29-profile-adaptive-copy.md)，供应商说明见[供应商接入](docs/providers.md)。

## 账号行为

- 仅在明确同意当前隐私说明、完成已启用的引导后初始化账号；无凭证才创建游客，安装 ID 不能恢复账号。
- 拒绝或撤回同意只暂停账号服务，保留本机凭证，不执行退出、注销或清空；重新同意后继续原会话流程。有请求进行中或结果待确认时先完成恢复，才能撤回。
- 未绑定身份升级游客；已绑定身份切换到老账号，由服务端执行游客合并。
- 正式账号可绑定手机和微信，绑定后不可更换或解绑，正式账号不合并。
- 昵称可改、头像可选内置项；微信登录/绑定后仅预览最终账号缺少的字段，可独立选择或跳过。确认、跳过或过期后本次选择结束，编辑资料不提供微信授权；/me 和重新打开 App 不重新创建已结束的选择。
- 注销需用当前绑定身份验证，立即生效、全部设备会话失效；原手机号和微信 30 天内不可注册或绑定，期满新建账号且旧数据不恢复。
- 服务端确认退出后清理本地凭证和私有缓存，再建立新游客。
- 超时保留原 key/body/credential，加密保存后可在重启恢复。401 不循环建号，429 等待服务端冷却。
- 退出或注销未确认时隐藏私有内容。明确失效后需用户确认才重新开始；停用账号不能自动建号规避。
- 验证码挑战长期无法确认，可显式放弃结果、等待冷却后重新获取。不能用此入口放弃登录合并、刷新或退出。

## 目录与扩展

    product.properties                   产品名称、包名、版本
    environments/                        独立测试/正式配置
    app/src/main/java/cn/yixiu/starter/
      core/                              协议、状态机、HTTP 传输
      storage/                           Keystore + AES-GCM 原子存储
      settings/                          普通设置、启动策略、tool-temp 缓存边界
      ui/                                启动路由、主题、页面、公共状态和 ViewModel
      ui/account/                        账号页面、表单、恢复与确认弹窗
      wechat/                            官方 SDK 和回调
      StarterApplication.kt              装配、应用协程、缓存清理钩子
    app/src/debug/                       开发工具和环境切换
    app/src/release/                     空调试入口
    app/src/test/                        JVM 协议和状态机测试
    app/src/androidTest/                 Android UI/存储仪器测试

AccountEngine 是唯一账号操作入口。账号请求用 Mutex 串行化，优先保证操作顺序；旧响应完成后才切换账号/环境。generation/tokenVersion 随状态保存。未来并发业务 Repository 必须捕获并检查账号代际，避免旧用户响应写入新账号缓存。

StarterScreen.kt 只负责应用导航和插槽；LocalToolScreen.kt 是不联网的文本计数示例，Theme.kt 维护浅色/深色配色，UiStates.kt 提供共用加载/空态/错误展示。业务首页应替换示例；仅关闭 DEMO_ENABLED 会显示“尚未配置”，不能视为已完成产品。普通“清理临时缓存”仅触及 cacheDir/tool-temp，不清凭证、偏好或用户保存文件。未来私有缓存按账号分目录，统一放 cacheDir/account-private/ 并接入 onAccountCleared。当前没有业务缓存，服务端账号合并也不等于未来任意业务自动合并。

令牌和恢复材料保存在 noBackupFilesDir 的加密文件，密钥在 Android Keystore，禁用备份与迁移。解密失败不清空账号。卸载/清除应用数据会丢失未绑定游客。

HTTP 不跟随重定向，不跳过证书验证，不自动重试写请求。debug 日志最多 50 条内存记录，只保存路径、状态、业务码、追踪 ID、字段名和耗时。

## 验证

    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileReleaseKotlin
    .\gradlew.bat :app:connectedDebugAndroidTest

第二条完整仪器测试仅用于可丢弃数据的测试设备/模拟器。保留日常真机账号时，只运行隔离界面测试：` .\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cn.yixiu.starter.StarterUiTest`。仓库已设置 `android.injected.androidTest.leaveApksInstalledAfterRun=true` 防止测试结束自动卸载 App；不要移除此项。默认单元测试不访问线上。受控真实测试明确启用：

    $env:HYPERF_LIVE_TEST = 'true'
    .\gradlew.bat :app:testDebugUnitTest --tests '*LiveApiSmokeTest' --rerun-tasks
    Remove-Item Env:HYPERF_LIVE_TEST

LiveApiSmokeTest 读取对应构建的 BuildConfig.API_BASE_URL（上述 debug 测试来自 environments/test.properties），启用前核对其为可创建测试账号的环境。只创建一个游客、查询、刷新、退出；不发送短信、不触发微信、不删除服务器审计记录。

参见 [接口映射](docs/api-map.md)、[验收记录](docs/verification.md)。

## 正式发布

正式打包自动执行 `:app:verifyReleaseReady`。当前缺少正式配置、真实主体/政策与正式签名，预期阻止正式 APK/AAB；`compileReleaseKotlin` 只验证结构，不代表可发布。复制 `environments/production.properties.example`、配置不入库的 `keystore.properties` 与真实签名，并完成实际产品内容后再打包。

检查要求正式 API、政策/协议、主体、支持邮箱、备案适用性说明、有效非 debug 签名、`DEMO_ENABLED=false` 及 `RELEASE_CONTENT_READY=true`。发布人员仍须审核真实首页、政策正文、权限/SDK 清单及市场要求；开关不能证明业务完成。完整规则见[构建环境与正式发布检查](docs/build-environments.md)。

微信需登记实际 APK 的包名和签名 MD5；本机 debug 密钥不能充当正式签名。上线前完成真机与供应商验收。
