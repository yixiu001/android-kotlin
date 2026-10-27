# android-kotlin

可复制的 Android 产品基础工程，Kotlin + Jetpack Compose，对接 [hyperf-swow](https://github.com/yixiu001/hyperf-swow) 单产品账号体系。

内置中文首页、个人中心、自动游客、手机验证码登录/绑定、微信授权登录/绑定、会话刷新和当前设备退出。新产品在此基础上扩展业务页面。开发工具只存在于 debug 版本。

## 快速开始

1. Android Studio 打开本目录并 Gradle Sync。
2. SDK Manager 安装 Android API 37（本机为 37.0）和 Build Tools 36.0.0。Gradle JDK 使用 Android Studio 自带 JBR，本机 Java 25 已验证构建。
3. 检查根目录 [product.properties](product.properties)，默认访问 https://hyperf.tongyiwl.bond。
4. 连接 Android 8.0/API 26 或以上设备，选择 app → Run。首次启动自动建立游客。

Windows 构建命令（按本机实际路径设置 JAVA_HOME）：

    $env:JAVA_HOME = 'D:\android-studio\jbr'
    .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

测试 APK：app/build/outputs/apk/debug/app-debug.apk。

无需另装 Kotlin 编译器。工程固定 Gradle 9.6.0、AGP 9.4.1、Kotlin/Compose Compiler 2.2.10、Compose BOM 2025.08.00；升级编译插件时应成套验证。

## 产品配置

| 配置 | 用途 |
| --- | --- |
| APP_NAME | 应用显示名称，UTF-8 中文 |
| APPLICATION_ID | 应用安装身份，复制产品必改，微信开放平台须一致 |
| API_BASE_URL | HTTPS origin，不带 /api/v1、路径或查询参数 |
| WECHAT_APP_ID | 微信移动应用 AppID，不是 AppSecret |
| SMS_ENABLED | 客户端短信入口开关，不代表服务端已启用 |
| WECHAT_ENABLED | 客户端微信开关，需平台和服务端配置匹配 |
| VERSION_CODE / VERSION_NAME | 安装升级版本号与显示版本，版本字符串最多 32 字节 |

第一次复制请完成 [新产品配置清单](docs/new-product-checklist.md)。测试服务端已通过 Jenkins #19 加载短信和微信配置，客户端已同步 AppID 与用户登记的包名 cn.fuyaoshouhu.app。微信挑战接口已通过验证，真实短信发送及微信真机授权仍待验收。参见 [供应商接入](docs/providers.md)。

## 账号行为

- 无凭证自动创建游客，安装 ID 不能恢复账号。
- 未绑定身份升级游客；已绑定身份切换到老账号，由服务端执行游客合并。
- 正式账号可绑定手机和微信，正式账号之间不合并。不提供后端未开放的密码、解绑、注销等入口。
- 服务端确认退出后清理本地凭证和私有缓存，再建立新游客。
- 超时保留原 key/body/credential，加密保存后可在重启恢复。401 不循环建号，429 等待服务端冷却。
- 退出未确认时隐藏私有内容。明确失效后需用户确认才重新开始；停用账号不能自动建号规避。
- 验证码挑战长期无法确认，可显式放弃结果、等待冷却后重新获取。不能用此入口放弃登录合并、刷新或退出。

## 目录与扩展

    product.properties                   产品集中配置
    app/src/main/java/cn/yixiu/starter/
      core/                              协议、状态机、HTTP 传输
      storage/                           Keystore + AES-GCM 原子存储
      ui/                                Compose 页面和 ViewModel
      wechat/                            官方 SDK 和回调
      StarterApplication.kt              装配、应用协程、缓存清理钩子
    app/src/debug/                       开发工具和环境切换
    app/src/release/                     空调试入口
    app/src/test/                        JVM 协议和状态机测试
    app/src/androidTest/                 Android UI/存储仪器测试

AccountEngine 是唯一账号操作入口。账号请求用 Mutex 串行化，优先保证操作顺序；旧响应完成后才切换账号/环境。generation/tokenVersion 随状态保存。未来并发业务 Repository 必须捕获并检查账号代际，避免旧用户响应写入新账号缓存。

StarterScreen.kt 的首页区供业务扩展。未来私有缓存按账号分目录，统一放 cacheDir/account-private/ 并接入 onAccountCleared。当前没有业务缓存，服务端账号合并也不等于未来任意业务自动合并。

令牌和恢复材料保存在 noBackupFilesDir 的加密文件，密钥在 Android Keystore，禁用备份与迁移。解密失败不清空账号。卸载/清除应用数据会丢失未绑定游客。

HTTP 不跟随重定向，不跳过证书验证，不自动重试写请求。debug 日志最多 50 条内存记录，只保存路径、状态、业务码、追踪 ID、字段名和耗时。

## 验证

    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
    .\gradlew.bat :app:connectedDebugAndroidTest

第二条需可连接的真机/模拟器。默认单元测试不访问线上。受控真实测试明确启用：

    $env:HYPERF_LIVE_TEST = 'true'
    .\gradlew.bat :app:testDebugUnitTest --tests '*LiveApiSmokeTest' --rerun-tasks
    Remove-Item Env:HYPERF_LIVE_TEST

LiveApiSmokeTest 读取 product.properties 中的 API_BASE_URL，启用前核对其为可创建测试账号的环境。只创建一个游客、查询、刷新、退出；不发送短信、不触发微信、不删除服务器审计记录。

参见 [接口映射](docs/api-map.md)、[验收记录](docs/verification.md)。

## 正式发布

release 默认产出未签名 APK。使用 Android Studio → Generate Signed App Bundle / APK，或复制 keystore.properties.example 为不提交的 keystore.properties，配置自己的发布密钥。

微信需登记实际安装 APK 的包名和签名 MD5；本机 debug 密钥不能充当产品正式签名。上线前补齐产品隐私政策、业务内容和平台资质，并完成真机和供应商验收。
