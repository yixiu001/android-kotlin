# 新产品复制配置清单

适用于当前 android-kotlin 单 app 模块工程。按实际配置文件、Gradle 装配和页面入口核对，更新于 2026-10-29。此清单不修改基础工程当前测试配置。

新产品需要完成：独立代码仓库 → 产品身份 → 测试环境 → 自己的后端和登录供应商 → 品牌与真实业务 → 正式环境和签名 → 验收。**只改应用名或包名，不会隔离后端账号数据。**

## 1. 复制与本机环境

- [ ] 复制受版本控制的源码到新目录，建立新仓库。可下载仓库源码 ZIP；若使用 git clone 保留历史，修改 origin 指向新仓库，推送前用 `git remote -v` 核对。
- [ ] 不带入旧项目的 `.gradle/`、`.kotlin/`、`.idea/`、各级 `build/`、`.verification/`、APK、日志、`local.properties`、`keystore.properties`、签名私钥和实际 `environments/production.properties`。保留 Gradle Wrapper、gradle.properties、proguard-rules.pro、配置 example 和 tests。
- [ ] 修改 `settings.gradle.kts` 的 `rootProject.name`，这是 IDE 工程名，不是手机显示名称。
- [ ] Android Studio 打开新目录，由本机生成 `local.properties`；SDK 路径和 JDK 使用本机实际值。当前工程 compile/target SDK 37、min SDK 26；不要因复制项目顺带降级或随意改整套构建版本。
- [ ] 仓库中的构建、配置、验收文档替换为新产品实际状态；旧产品的 Jenkins 构建号、测试结果和截图不能作为新产品验收证据。

### 1.1 可以清理的本地产物

**以下清理仅针对新产品副本，不要求删除当前基础工程的任何文件。** 优先下载 GitHub 源码 ZIP，通常不会带入已忽略的缓存、日志、APK 和签名文件；直接复制本机目录时，再逐项检查。

| 文件或目录 | 新产品副本中的处理方式 |
| --- | --- |
| `.gradle/`、`.kotlin/`、`.idea/` | 可删除，由构建工具和 IDE 重新生成 |
| 根目录 `build/`、`app/build/` 等构建输出 | 可删除，不复用旧产品的产物 |
| `.verification/`、`*.log`、旧 APK | 可删除，本机验收记录与旧安装包不是新产品交付物 |
| `local.properties` | 可删除，在新工程中生成本机 SDK 路径 |
| `.git/` | 仅在新副本不保留原历史时删除并重新初始化仓库；需要保留历史则保留它，只修改 origin 指向新仓库 |

### 1.2 不照搬的配置与历史文档

| 内容 | 新产品副本中的处理方式 |
| --- | --- |
| `keystore.properties`、签名私钥文件 | 不带入副本；原产品私钥及备份妥善保管，不删除原件。为新产品配置发布签名 |
| 实际的 `environments/production.properties` | 不照搬，按 `.example` 模板重新填写新产品正式环境 |
| `docs/verification*.md`、`docs/superpowers/plans/` | 原工程历史验收与实施记录，可从新副本删除；以后记录新产品自己的验收结果 |
| `docs/progress.md`、`docs/implementation-plan.md`、`design-qa.md` | 旧开发记录，可删除或重写 |
| `docs/images/` 中的旧产品截图 | 可删除或替换；同步更新使用它们的文档 |
| `README.md`、`docs/current-state.md`、`docs/design.md` | 建议保留并改写为新产品说明，去掉旧提交、部署编号和验收状态 |

删除或重命名文档后，同步更新 `docs/README.md` 和其他引用，避免留下失效链接。构建说明、接口映射、供应商接入及本清单可继续复用，具体产品值需要更新。

### 1.3 替换首页后再删除示例代码

文本计数是占位示例，不要求新产品保留。接入真实业务首页后再完成以下操作：

- [ ] 修改 `ui/ProductHome.kt`，接入真实页面并移除对 `LocalToolScreen()` 的调用，然后删除不再使用的 `ui/LocalToolScreen.kt`。
- [ ] 清理 `res/values/product_strings.xml` 中已无引用的文本计数文案，不删除仍供帮助、隐私和引导使用的资源。
- [ ] 调整或删除仅验证文本计数的测试用例；混合测试文件中仍有账号、导航和公共交互验证时，不整份删除。
- [ ] 将对应环境的 `DEMO_ENABLED` 设为 `false`，确认正常模式和本地模式都进入新首页。仅关闭开关仍会显示“尚未配置”，不等于业务替换完成。

上述 `ui/` 路径相对 `app/src/main/java/cn/yixiu/starter/`，`res/` 相对 `app/src/main/`。

### 1.4 应保留的基础能力与文件

| 内容 | 保留原因与调整范围 |
| --- | --- |
| `core/`、`storage/`、`wechat/` | 复用账号状态机、会话安全存储、请求恢复和微信接入，不因复制产品重新实现 |
| `settings/`、隐私/账号页面、公共布局与通用文案 | 保留设置、隐私流程、资料保存及适配能力，按产品修改文案和品牌 |
| `app/src/debug/`、`app/src/release/` | 保留调试与正式隔离；不需要联调工具时设置 `TEST_TOOLS_ENABLED=false`，不用直接删除实现 |
| `app/src/test/`、`app/src/androidTest/` | 保留协议、账号、安全和通用交互回归；替换示例后同步调整相关用例 |
| `gradle/`、`gradlew`、`gradlew.bat`、Gradle 构建文件 | 保留可重复构建能力；注意 `gradle/` 是 Wrapper 文件，和可清理的 `.gradle/` 缓存不同 |
| `gradle.properties`、`app/proguard-rules.pro`、`scripts/` | 保留构建、混淆和发布检查；保留测试后不自动卸载 App 的设置 |
| `.gitignore`、`.gitattributes`、配置 `.example` 文件 | 保留忽略规则、文件规范及新环境/签名模板 |
| `product.properties`、`environments/test.properties` | 保留文件并改写产品身份、后端域名、微信 AppID 与开关，不继续指向原产品 |

代码目录名和 namespace 可以保留，独立安装身份由 `APPLICATION_ID` 决定；后端数据隔离仍须完成第 4 节，不能靠删除客户端缓存实现。

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
| `ui/ProductHome.kt` | 唯一业务首页接入点，正常模式与本地模式共用；替换占位工具，不提前初始化账号或微信 SDK |
| `ui/StarterScreen.kt` | 需要调整的导航；保留账号安全及账号页内的恢复弹窗入口 |
| `res/values/product_strings.xml` | 产品帮助、隐私说明、引导、占位示例文案；按新产品实际行为修改 |
| `res/values/strings.xml` | 通用按钮、表单、状态及错误提示；保留格式参数的顺序和类型 |
| `ui/StartupRouter.kt` | 仅在改变启动/引导流程时修改；纯文案修改使用上述 XML，本地功能由 ProductHome 统一接入 |
| `ui/LocalToolScreen.kt` | 文本计数只是示例，替换/移除为产品自己的真实工具页面 |
| `ui/PrivacyScreen.kt` | 实际数据处理、SDK、权限和撤回说明，必须随新增 SDK/权限/业务更新 |
| `ui/ProfileCenter.kt` | 仅在改变个人中心布局/页面结构时修改；帮助等文字使用 XML，应用名和版本从配置读取 |
| `AndroidManifest.xml` | 仅在实际业务需要时增加权限、组件、深链或 provider；同步隐私披露和运行时申请，不预先申请一堆权限 |

**关闭 DEMO_ENABLED 不是完成业务替换。** 新产品在 `ProductHome` 的 false 分支接入自己的真实页面，然后关闭示例并确认 RELEASE_CONTENT_READY。正常/本地模式会使用同一个入口，不再分别修改两个分支。联网业务须在用户已同意且主动使用时接入；纯本地页面不能要求账号初始化。

反馈入口统一由 `SupportEntry` 提供：优先 `FEEDBACK_URL`，为空或不是有效 HTTPS 地址时使用 `SUPPORT_EMAIL`。外部应用不可用时可复制支持邮箱；正式版两项皆空不显示入口，正式发布校验仍要求支持邮箱。登录能力关闭时正式版隐藏相应按钮，debug 保留配置诊断。短结果用 Toast，确认/选择用弹窗，输入错误留在字段下，不在业务首页增加账号提示卡片。

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
- [ ] 窄屏、横屏与系统大字体检查按钮和输入是否可达；复用 `PageColumn` 与 `AppDialog`，不要固定字号规避溢出。表单明确失败保留输入，只有确认成功才关闭，未知结果恢复原请求。
- [ ] 日常覆盖安装用新包名对应的 `adb install -r`，不卸载、不清应用数据。登录和数据迁移另行设计，不能靠复制旧 Vault 文件完成。

清单重点是把每个新产品的身份、环境、品牌、供应商和发布信息配置正确；账号基础逻辑可复用。配置细节见[构建环境与发布检查](build-environments.md)、[供应商接入](providers.md)、[个人中心](profile-center.md)。
