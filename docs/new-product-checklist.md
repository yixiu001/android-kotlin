# 新产品复制配置清单

适用于当前 android-kotlin 单 app 模块工程。按实际配置文件、Gradle 装配和页面入口核对，更新于 2026-10-29。此清单不修改基础工程当前测试配置。

新产品需要完成：独立代码仓库 → 产品身份 → 测试环境 → 自己的后端和登录供应商 → 品牌与真实业务 → 正式环境和签名 → 验收。**只改应用名或包名，不会隔离后端账号数据。**

## 1. 复制与本机环境

- [ ] 复制受版本控制的源码到新目录，建立新仓库。可下载仓库源码 ZIP；若使用 git clone 保留历史，修改 origin 指向新仓库，推送前用 `git remote -v` 核对。
- [ ] 不带入旧项目的 `.gradle/`、`.kotlin/`、`.idea/`、各级 `build/`、`.verification/`、APK、日志、`local.properties`、`keystore.properties`、签名私钥和实际 `environments/production.properties`。保留 Gradle Wrapper、gradle.properties、proguard-rules.pro、配置 example 和 tests。
- [ ] 修改 `settings.gradle.kts` 的 `rootProject.name`，这是 IDE 工程名，不是手机显示名称。
- [ ] Android Studio 打开新目录，由本机生成 `local.properties`；SDK 路径和 JDK 使用本机实际值。当前工程 compile/target SDK 37、min SDK 26；不要因复制项目顺带降级或随意改整套构建版本。
- [ ] 仓库中的构建、配置、验收文档替换为新产品实际状态；旧产品的 Jenkins 构建号、测试结果和截图不能作为新产品验收证据。

## 2. 产品身份：product.properties

这些值同时用于 debug 和 release。

| 字段 | 新产品如何填写 | 是否必须调整 |
| --- | --- | --- |
| `APP_NAME` | 手机显示的产品名称，支持 UTF-8 中文 | 必须 |
| `APPLICATION_ID` | 新产品独立包名，例如 `cn.yourcompany.yourtool`；示例不能原样使用 | 必须 |
| `VERSION_CODE` | 新产品首版可从 1 开始，同一产品每次发布递增 | 首版确认；升级必增 |
| `VERSION_NAME` | 用户看到的版本，例如 1.0.0；后端要求 UTF-8 不超过 32 字节 | 每次发布确认 |

当前基础工程的 `cn.fuyaoshouhu.app` 属于已有联调产品，复制后不能继续当作新产品安装身份。

**可以保持不动：** Gradle `namespace = "cn.yixiu.starter"`、Kotlin 源码包名和目录。安装隔离由 APPLICATION_ID 决定。Manifest 中 `${applicationId}.wxapi.WXEntryActivity` 会自动跟随新包名，不需要手写第二个微信回调类。若确实要重命名源码包，使用 IDE 重构并同步 imports、Manifest 和测试，不做盲目全文替换。

## 3. 两套环境配置

debug 读取 `environments/test.properties`。release 读取 `environments/production.properties`；后者从 `production.properties.example` 复制并填写，已被 gitignore，不会随仓库传到其他电脑或 CI。正式配置不会回退到测试配置。文件修改后需要重新构建 APK，不是远程配置。

下表的测试值、正式值分别填写；不要因为复制源码就沿用基础工程的后端和微信应用。

| 字段 | 测试环境 | 正式环境 / 注意事项 |
| --- | --- | --- |
| `API_BASE_URL` | 新产品的测试 API 域名 | 新产品正式 API；当前发布检查不允许与测试 API 同域名 |
| `WECHAT_APP_ID` | 对应测试包/签名的微信移动应用 AppID | 与正式后端、微信平台登记一致；不是 AppSecret |
| `SMS_ENABLED` | 准备好短信服务时 true，否则 false | 按实际开放能力填写；只控制客户端入口 |
| `WECHAT_ENABLED` | 未准备好微信登录时 false | 启用时 AppID、包名、签名、平台权限和后端配置都需对应 |
| `TEST_TOOLS_ENABLED` | 新产品建议先 false；确定是可重置测试环境后才 true | 必须 false，release 代码也强制关闭 |
| `DEMO_ENABLED` | 开发初期可 true，显示文本计数示例 | 必须 false，且先完成第 5 节真实业务替换 |
| `PRIVACY_VERSION` | 使用独立测试版本，例如 test-2026-10-29 | 必须 production- 开头，例如 production-2026-10-29；政策变化递增 |
| `ONBOARDING_ENABLED` | 默认 false，需要引导再打开 | 按产品需要，不要求为了发布强行开启 |
| `ONBOARDING_VERSION` | 开启引导时填写，例如 1 | 完成和跳过都会记录；仅需重新展示引导时变更 |
| `OPERATOR_NAME` | 测试阶段可明确标为测试说明 | 真实运营主体，不能使用模板占位文字 |
| `SUPPORT_EMAIL` | 接收用户问题的邮箱 | 当前发布检查要求有效格式的真实联系邮箱 |
| `PRIVACY_POLICY_URL` | 产品测试政策地址；空值会显示测试提示 | 真实、公开、可访问的 HTTPS 隐私政策页面 |
| `TERMS_URL` | 产品测试用户协议地址 | 真实、公开、可访问的 HTTPS 用户协议页面 |
| `FEEDBACK_URL` | 可选反馈网页，空值不接入在线反馈 | 可选公开 HTTPS 页面；无需为基础工程另建完整工单系统 |
| `APP_MARKET_URL` | 可留空 | 可选应用市场 HTTPS 详情页，当前是打开网页，不是自动更新服务 |
| `APP_RECORD_NUMBER` | 可留空 | 按产品适用情况填写真实备案信息，不能填示例号 |

正式文件另有以下**发布校验字段**，不属于运行时业务参数：

| 字段 | 用法 |
| --- | --- |
| `APP_RECORD_REQUIRED` | 明确 true/false；由发布人员确认产品和发行地区的适用要求，程序不代替判断 |
| `APP_RECORD_EXEMPTION_REASON` | 上项为 false 时写明实际不适用理由；为 true 时应提供 APP_RECORD_NUMBER |
| `RELEASE_CONTENT_READY` | 真实业务、政策、权限/SDK 披露等人工核对完成后才设 true；不能拿它绕过未完成业务 |

`API_BASE_URL` 只填 HTTPS origin，不带 `/api/v1`、其他路径、查询参数或片段；客户端自行拼接接口路径。正式检查还限制正式域名、HTTPS 默认端口，拒绝 localhost、IP、明显测试或示例地址。政策/协议地址也不能使用测试 API 域名。

布尔值写小写 `true` / `false`。`ENVIRONMENT` 自动由构建类型设为 test/production，不需要另加属性。所有这些 BuildConfig 值都能从 APK 提取，**不能放入 AppSecret、AccessKey Secret、数据库或 Redis 密码**。

## 4. 配套后端、短信和微信

| 系统 | 新产品需要完成 |
| --- | --- |
| Hyperf | 新产品独立部署目录、容器/服务身份、测试和正式域名；Android 指向对应环境 |
| MySQL | 新产品独立数据库及权限受限的应用用户；初始化 schema，不复制旧产品用户/会话数据 |
| Redis | 独立键前缀及匹配权限；同 Redis 实例可按现有隔离方案使用，但不能沿用旧产品前缀 |
| 账号密钥 | 按后端复制清单生成新产品自己的加密/HMAC 密钥；只放服务器配置，不放 APK |
| 阿里云短信 | 在后端配置已准备好的签名、验证码模板及变量名、RAM 凭据；客户端不填写 AccessKey，也不直接调用短信供应商 |
| 微信开放平台 | 核对移动应用 AppID、Android 包名、实际安装包签名 MD5；Android 与后端 AppID 一致，AppSecret 仅在后端 |
| 测试重置工具 | 客户端 debug 开关只是第一道入口；还需新后端测试开关与数据库白名单。确认指向新测试库后再启用，不把授权 grant 固定写入配置 |

当前配套后端 sql 目录有 `000_create_database.example.sql`、001 账号结构、002 资料与注销、003 测试工具、004 微信资料候选。新空库按该副本的 `sql/README.md` 创建和顺序初始化；已有库只执行尚未应用的增量。不要对已有数据库重复执行整套初始化，更不要复制旧用户数据作为新产品初始数据。

当前 AppID `wx57879d4692d97d56` 和后端 `https://hyperf.tongyiwl.bond` 是既有联调配置，新产品不能未经核对直接继承。微信资料选择、游客合并和注销行为由现有两端协议共同决定，只改客户端按钮不能改变服务端规则。

## 5. 品牌、首页和文案：需要改代码的部分

以下资源和 Manifest 路径相对 `app/src/main/`；`ui/` 开头的 Kotlin 路径相对 `app/src/main/java/cn/yixiu/starter/`。

| 文件 / 入口 | 应修改内容 |
| --- | --- |
| `res/drawable/ic_launcher.xml` | 产品启动图标；当前系统启动页也引用此图标。若换资源位置，同时改 Manifest 和启动主题引用 |
| `res/values/themes.xml`、`res/values/colors.xml`、`res/values-night/colors.xml` | 系统启动页样式、浅深色背景；保留无额外开屏 Activity、无强制倒计时的轻量启动 |
| `ui/Theme.kt` | 产品品牌色、深浅主题，兼顾文字对比度 |
| `ui/StarterScreen.kt` | 已同意隐私后的首页业务入口及需要调整的导航；保留账号安全/恢复入口 |
| `ui/StartupRouter.kt` | 未同意隐私时的本地功能入口、启动提示和可选引导文案；本地功能不能提前调用账号或第三方登录 |
| `ui/LocalToolScreen.kt` | 文本计数只是示例，替换/移除为产品自己的真实工具页面 |
| `ui/PrivacyScreen.kt` | 实际数据处理、SDK、权限和撤回说明，必须随新增 SDK/权限/业务更新 |
| `ui/ProfileCenter.kt` | 帮助与反馈、常见问题、关于应用等产品文案；应用名和版本已经从配置读取 |
| `AndroidManifest.xml` | 仅在实际业务需要时增加权限、组件、深链或 provider；同步隐私披露和运行时申请，不预先申请一堆权限 |

**关闭 DEMO_ENABLED 不是完成业务替换。** 当前 StarterScreen 和 StartupRouter 的 false 分支分别显示“首页功能尚未配置”和“当前产品尚未配置本地功能”。新产品必须把这两个分支接到自己的页面，再关闭示例并确认 RELEASE_CONTENT_READY。

纯离线、完全不需要账号的产品仍需进一步裁剪账号入口和服务启动，当前没有一个现成的 `ACCOUNT_ENABLED` 总开关，不能只关闭短信/微信就认为账号服务被关闭。

## 6. 签名、包名和升级

复制 `keystore.properties.example` 为本地 `keystore.properties`：

| 字段 | 填写内容 |
| --- | --- |
| `storeFile` | 新产品发布 keystore 的实际路径；Windows 推荐使用正斜杠，例如 D:/signing/your-product.jks |
| `storePassword` | 密钥库密码，仅本地或受控 CI 保存 |
| `keyAlias` | 实际私钥别名 |
| `keyPassword` | 对应私钥密码 |

- [ ] 为新产品准备并备份发布签名，后续版本持续使用一致的发布身份；不能丢失后临时换成另一把密钥。
- [ ] 确认实际 debug/release APK 的包名及签名，重新核对微信平台。现有 `223fbaaba46bea0e5ef02ee43444dc90` 只是当前本机 debug 指纹，不是所有电脑、新产品或 release 的固定值。
- [ ] 当前 debug/release 共用 APPLICATION_ID，没有 applicationIdSuffix。不同签名不能直接覆盖，同包名也无法同时安装两份。需要并装时须另外配置包名后缀/变体，并同步微信平台和后端；仅修改 API 地址无法实现并装。
- [ ] 新产品新包名自然拥有独立本地沙盒；同一产品后续覆盖升级保持包名、签名和 Vault 兼容，不清数据。已有会话绑定 API origin，换后端不是透明迁移方式。
- [ ] `keystore.properties`、私钥及真实 production.properties 不提交 Git；在构建机器上单独配置。APK 内只放公开配置。

## 7. 业务数据与可保留的基础能力

- 账号状态机、随机令牌、幂等恢复、无效会话处理、debug/release 调试入口分离可继续复用，无需复制时重新设计。
- 绑定后不可换绑/解绑，正式账号不合并；微信资料只随登录/绑定给出一次可跳过选择；这些业务规则若改变，必须同时改后端与文档。
- 普通工具临时文件放 `cacheDir/tool-temp`，用户保存的内容不能放这里。账号私有缓存使用 `cacheDir/account-private` 下的账号分区并接入现有清理钩子。
- 新业务请求遵守账号 generation/tokenVersion 边界；账号切换后不能把旧响应写给新账号。
- 新业务表需要接入后端游客合并、注销清理和测试重置规则。基础账号模块不会自动处理任意新增业务数据。
- 维持当前单 app 模块即可，新增功能有真实复杂度后再拆模块；不必为了复用提前加入支付、广告、推送或云同步。

## 8. 构建与交付检查

先确认当前目录是新产品，再执行：

```powershell
# JDK 路径按本机修改
$env:JAVA_HOME = 'D:\android-studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileReleaseKotlin
.\gradlew.bat :app:signingReport
# 配齐正式资料与签名后执行；结构编译成功不等于可以正式打包
.\gradlew.bat :app:verifyReleaseReady
.\gradlew.bat :app:assembleRelease
# 发行渠道需要 AAB 时
.\gradlew.bat :app:bundleRelease
```

- [ ] 核对新包名、图标、名称、域名、微信 AppID、正式签名；新旧产品在手机上不互相覆盖。
- [ ] 首次隐私提示、拒绝后本地功能、重新同意、撤回、政策版本升级和按需引导行为正确。
- [ ] 主题、系统栏、键盘、返回、旋转、输入保留、空态/断网/错误恢复符合本产品页面。
- [ ] 自己的测试环境验证游客恢复、短信和微信登录/绑定、老账号合并、多设备、资料选择、退出与注销；使用专用测试账号。
- [ ] 权限/SDK 清单、隐私政策、用户协议、主体、支持联系方式、适用资质和渠道材料与实际产品一致；发布检查仅核对部分配置，不能保证过审。
- [ ] 缺正式配置时校验失败；正式包没有联调重置/切环境入口，也没有示例首页和“待配置”占位。
- [ ] 保留 `android.injected.androidTest.leaveApksInstalledAfterRun=true`。日常手机只运行已核对不碰真实存储的 StarterUiTest / FoundationUiTest；全量存储/清理测试用可丢弃数据的设备。
- [ ] 新产品若调整首页、文案或引导开关，同步修改对应 UI 测试的预期和模拟状态；测试不能永远依赖模板页面。
- [ ] 日常覆盖安装用新包名对应的 `adb install -r`，不卸载、不清应用数据。登录和数据迁移另行设计，不能靠复制旧 Vault 文件完成。

清单重点是把每个新产品的身份、环境、品牌、供应商和发布信息配置正确；账号基础逻辑可复用。配置细节见[构建环境与发布检查](build-environments.md)、[供应商接入](providers.md)、[个人中心](profile-center.md)。
