# 复制为新产品

## 必改项

1. 复制源码到独立目录，不复制 .git、.gradle、.idea、build、local.properties、签名文件或日志。创建自己的 Git 仓库。
2. 修改 product.properties 中名称、APPLICATION_ID 和版本号；在 environments/test.properties 配置测试 API、微信 AppID、短信/微信开关。复制 production.properties.example 为不入库的 production.properties，单独填写正式值。
   联调需要时仅在 debug 开启 `TEST_TOOLS_ENABLED`；本项目已开 true，缺项回退 false；新产品默认关闭，release 强制 false，不把重置授权码加入配置文件。
3. 修改 settings.gradle.kts 的 rootProject.name，调整工程展示名称。
4. 替换 app/src/main/res/drawable/ic_launcher.xml 图标、ui/Theme.kt 品牌色及 ui/LocalToolScreen.kt / StarterScreen.kt 首页业务区域；正式产品建议配 adaptive icon。
5. 配置本机 SDK/JDK 后重新 Sync。
6. 在对应环境配置中按[个人中心说明](profile-center.md)填写 `FEEDBACK_URL`、`PRIVACY_POLICY_URL`、`TERMS_URL` 的产品公开 HTTPS 地址；测试空值显示未配置提示；正式发布还需真实主体、支持邮箱、备案适用性、非 debug 签名，并通过[发布检查](build-environments.md)。

namespace 可以保持 cn.yixiu.starter，安装身份由 APPLICATION_ID 区分，无需全局替换 Kotlin 包名。微信回调的 Manifest 别名随 applicationId 自动变化。

API_BASE_URL 只填 HTTPS origin，例如 `https://fysh-api.tongyiwl.bond`，不带 `/api/v1`。debug/release 自动读取独立测试/正式环境文件，不额外增加 flavor；正式配置绝不回退读取测试值。模板当前保留现有联调产品的配置，不能直接作为新产品发布。

## 启动、隐私与设置

- 编写与实际数据处理、权限、SDK 一致的政策及本地说明；隐私版本变化后重新确认，正式 PRIVACY_VERSION 使用 production- 前缀。不要把测试说明作为正式政策。
- 首装和旧版缺同意记录时，确认前不初始化账号或微信 SDK；拒绝仅使用本地工具。验证覆盖升级保留原 Vault，而不是假定旧用户已同意。
- 撤回暂停账号服务并保留凭证；有进行中或待确认操作先恢复。退出、注销仍是独立账号操作。
- 引导默认关闭，需要时开启 ONBOARDING_ENABLED 并维护 ONBOARDING_VERSION；完成和跳过均记录版本，不用引导开关绕过隐私。
- 验证主题系统跟随/浅色/深色、设置持久化与系统栏。普通缓存仅写入并清理 cacheDir/tool-temp，不能将用户文件或凭证放在这里。

## 后端与身份

- 后端也复制独立工程：独立 MySQL 库、账号加密/HMAC 密钥、Redis 前缀和最小 ACL。Android 不保存这些值。
- 新产品不能继续用模板测试后端，否则会共用账号数据。只改 Android 包名不会隔离后端。
- 微信移动应用需登记新包名和实际 APK 的 MD5，AppID 与后端一致。
- 短信模板/签名/RAM 密钥在服务器配置，Android 只调用 Hyperf。
- 不支持解绑或换绑；已提供资料编辑和手机号/微信验证注销。新后端执行 sql/001→002→003→004，已有 001+002+003 的库仅升级 004，再发布后端和新版 App。当前已登录账号重置只属于测试工具，需服务端开关及测试数据库白名单；普通会话 prepare 得到短期 grant，确认成功自动清本地，无日常 CLI/授权码输入。prepare 已发布基线为 Jenkins #21；当前 SQL004/微信资料确认后端已通过 Jenkins #22 发布。新产品的配套后端仍应先于新版 APK 发布。
- 头像从内置项选择，或微信登录/绑定后一次性选择补空字段；编辑资料不提供微信重新授权入口。完成、跳过或过期结束本次选择；不上传图片，不需要 OSS。

## 签名与升级

- 发布密钥保存在工程外并备份，用签名向导或不入库的 keystore.properties。
- 运行 gradlew.bat :app:signingReport 查看 debug 公开指纹；不分享密钥文件和密码。
- debug 未加 applicationIdSuffix。和 release 同包名不同签名时不能直接覆盖安装，卸载会丢失未绑定游客。
- Vault schema=1 保持兼容；新增候选及 auth intent 为可选成员；旧 Vault 无候选、旧手动意图默认 false，不需清应用数据。解密失败不能自动清空账号。
- 保留 `gradle.properties` 的 `android.injected.androidTest.leaveApksInstalledAfterRun=true`，避免设备测试收尾自动卸载 App。全量存储/清理仪器测试只能在可丢弃数据的设备运行；日常手机仅运行隔离 `StarterUiTest`。

## 业务扩展

- 本地文本计数仅是可运行示例；替换为真实首页和本地模式功能，再设置 DEMO_ENABLED=false、RELEASE_CONTENT_READY=true。只关闭示例会显示未配置提示，不能通过人工确认冒充业务完成。
- 保留启动门禁，纯本地功能不读取账号、不联网；网络业务通过原账号层协调，页面不管理令牌。
- 新增并发请求捕获 generation/tokenVersion，账号切换后丢弃旧响应。
- 业务私有缓存按账号分目录，接入 onAccountCleared，退出/升级/合并/注销时清理；新增业务表同时实现后端注销清理。
- 后端业务数据合并需要注册业务合并器，基础账号模块不会自动合并任意业务表。
- 规模增长后再拆分模块，当前单 app 工程便于复制维护。

## 验收

- 首次/升级隐私确认、拒绝与重新同意、政策版本变化、可选引导跳过/完成、撤回边界；同意后游客、重启恢复、短信及微信登录/绑定、游客合并、正式账号冲突、退出后新游客。
- 多设备独立会话，退出一台不影响其他设备。
- 断网/响应丢失/原请求恢复/并发刷新/验证码过期/429/503。
- 微信真机验证包名签名、取消、未安装微信、AppID 不匹配、进程重建回调；登录最终账号后预览缺失字段/完整账号不弹窗、独立选择/跳过重启、编辑页无微信授权入口、过期、未知确认原请求恢复。
- release 无开发工具；缺正式环境、政策、主体、签名、真实业务确认时自动阻止打包。正式后端属于新产品，APK 不包含服务端密钥。
- 单元测试、lint、release 结构编译与限定设备仪器测试通过；正式产物另需 verifyReleaseReady 和人工内容审查。覆盖安装前后核对首次安装时间及加密 Vault 摘要，不执行清数据。
