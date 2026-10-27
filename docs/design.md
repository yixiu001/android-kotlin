# Android 通用产品基础工程设计（已确认）

2026-10-27：用户批准直接开发，细节由实现者决定；工程改名 android-kotlin，仓库 https://github.com/yixiu001/android-kotlin.git。

## 目标与范围

用户明确要求：使用 Kotlin，对接 hyperf-swow 已开放接口，并且像后端模板一样，后续复制即可开发新的 Android 产品。本工程应交付可复用的产品基础能力和正常用户界面，开发调试仅是辅助功能。

范围以 `hyperf/docs/api/android-account.md` 和 `app/Account/Http/AccountRequest.php` 为准：10 个账号接口与首页健康检查。不改后端协议、不新增后端业务功能，不绑定服药或其他特定业务。

## 方案与取舍

此前提出了“真实流程＋调试面板”和“纯接口工具”，用户进一步明确选择可复制的产品基础工程这一方向。

拟采用单 app 模块、按职责分包的 Kotlin + Jetpack Compose 工程，UI / ViewModel / Repository / API 与会话存储分离。优点是复制和理解成本低，足以容纳当前账号体系；后续业务变大再按模块拆分。现在不引入多模块插件框架、多产品 flavor 矩阵或跨平台层。以上为实现建议，已获用户授权。

用户已指定独立目录：`C:/Users/Admin/Desktop/work/kaifa/android-kotlin`。不覆盖现有 `android/` 项目，不修改 `hyperf/`。

## 产品界面

- 中文界面，底部“首页 / 我的”。首页提供业务扩展占位内容，后续产品替换。
- 启动时恢复凭证和待完成请求，无凭证才创建游客，不弹登录框；离线、超时或账户禁用不能导致循环创建游客。
- “我的”展示游客或正式账号状态，点击登录后选择短信验证码或微信授权。
- 正式账号可查看手机、微信绑定状态，补充绑定另一身份。两个正式账号之间的冲突按后端错误提示，不自行合并。
- 提供正常的加载、空态、失败重试、短信倒计时、退出待确认及会话失效提示。
- 退出在服务端确认后清除旧凭证及私有缓存，再创建新游客。确认前遮蔽旧账号私有内容，不宣称已成功退出。
- 不展示后端没有的改昵称、换绑、解绑、注销、密码登录或其他设备管理入口。

## 接口覆盖

| 接口 | 产品用途 |
| --- | --- |
| POST /api/v1/auth/guest | 首次启动和退出后的游客创建 |
| GET /api/v1/me | 用户状态、身份绑定状态 |
| POST /api/v1/auth/sms/challenges | 登录或绑定手机号前请求短信 |
| POST /api/v1/auth/login/phone | 游客通过短信升级或合并登录 |
| POST /api/v1/me/identities/phone | 正式账号绑定手机号 |
| POST /api/v1/auth/wechat/challenges | 微信授权前获取 challenge、state、appid |
| POST /api/v1/auth/login/wechat | 游客通过微信升级或合并登录 |
| POST /api/v1/me/identities/wechat | 正式账号绑定微信 |
| POST /api/v1/auth/refresh | 自动更新会话令牌 |
| POST /api/v1/auth/logout | 当前设备退出 |
| GET / | 开发环境健康检查 |

## 网络与会话约束

- 默认后端 `https://hyperf.tongyiwl.bond`，使用 `/api/v1` 前缀；只接受 HTTPS，正常验证证书，不添加跳过校验开关。
- 所有 POST 使用 JSON 对象和随机 32 字节 base64url（43 字符）的 Idempotency-Key；guest、refresh 不带 Authorization，其他接口遵循现有协议。
- 在发送前加密保存待完成写操作的原始 key、请求体、凭证与目标地址。网络结果不明时保留原请求供恢复；不自动换 key 重发短信、刷新或微信 code。
- 登录、刷新和退出等会话写操作串行协调；状态包含账号 generation 与 tokenVersion。迟到的响应不得覆盖新会话或新环境状态。
- 仅 ACCESS_TOKEN_EXPIRED 触发合并并发刷新，最多重试原请求一次；刷新结果原子持久化。SESSION_INVALID、ACCOUNT_DISABLED 和 429 分别处理，不通过自动建游客绕过错误。
- 遵守 Retry-After，识别操作进行中、结果过期、身份冲突等后端业务码。幂等领取窗口到期时明确提示会话恢复失败，不静默执行新的登录或创建操作。
- Android Keystore 保存 AES-GCM 密钥，令牌与待完成材料保存于应用私有存储，原子写入；排除云备份及设备迁移。日志默认脱敏，不输出手机号全文、验证码、code、state、令牌及 Idempotency-Key。

## 短信与微信接入

短信直接调用后端已有接口，由用户填写真实收到的验证码；不加入万能验证码或客户端短信供应商密钥。

微信接入真实 Android OpenSDK，通过后端 challenge 返回的 AppID/scope/state 发起授权，校验回调 state 和当前操作；回调取消、错误或状态过期不调用登录。AppSecret 只在后端。

复制产品后必须配置它自己的微信移动应用 AppID、Android applicationId、签名信息，SDK AppID 与后端一致。采用可配置 manifest 回调入口绑定应用包名的方式，避免复制时遗漏硬编码包名。具体 SDK 版本和官方接入约束在实现时核验。

后端短信和微信当前尚未完成真实供应商联调，默认安全关闭。工程仍完成所有接口和 SDK 适配，未配置时提供明确不可用状态。自动测试使用受控替身；不得将替身编入正式登录路径。实际手机微信授权与真实短信验收需有效供应商配置，报告中与编译/模拟测试区分。

## 复制新产品时的集中配置

计划使用根目录产品配置文件集中维护 APP_NAME、APPLICATION_ID、API_BASE_URL、WECHAT_APP_ID 及功能开关；密钥和签名配置走不提交 Git 的本机配置/环境变量。

- 显示名称、applicationId、API 地址、微信 AppID、版本号集中配置，不要求全局替换源码字符串。
- 代码 namespace 可以保持通用值；Android 安装身份由 applicationId 区分。图标、主题和业务首页有明确替换位置。
- SDK/Gradle/Compose 等版本锁定，Gradle Wrapper 纳入工程；local.properties、构建输出及签名文件不提交。
- default debug 连接现有测试后端；release 不得不经核验就作为新产品发布。新产品首次运行前必须更换 applicationId、后端及业务配置。
- 不把后端 RAM Secret、AppSecret、数据库、Redis 凭据打包进 APK。
- 不擅自创建 GitHub 仓库、推送或配置 Jenkins；完成本地工程、文档和 debug APK。

## 开发工具与扩展点

仅 debug 构建包含工具入口：查看 10 个接口覆盖情况、请求/响应摘要、HTTP 状态、业务码、request_id 和耗时；支持健康检查和手动读取账号/刷新。写请求恢复复用同一会话协调器，不在工具里再实现第二套登录逻辑。

仅 debug 提供环境切换，严格校验 HTTPS origin，切换时递增 generation、取消旧请求并清除本地旧环境凭证/待完成项，绝不把原 token 发给新域名。切换前说明本地清除不等于服务端退出；存在未确认操作时先要求恢复或明确放弃本地材料。release 不包含可编辑后端地址的工具。

账号资料与各业务缓存按账号隔离，留出账号切换/退出清理钩子。新产品业务 Repository、首页内容与主题按目录扩展，无须修改基础协议代码。无后端能力的功能不提供假入口。

## 验证和交付

- 独立 Android Studio 工程、构建说明、接口映射说明、复制新产品清单和 debug APK。
- 单元测试覆盖协议字段与请求头、幂等 key、恢复材料、状态机、并发刷新、迟到响应、错误处理、日志脱敏和环境隔离。
- 执行 debug 构建、单元测试与 Android lint。在可用模拟器/设备上验证首页、个人中心、游客、刷新和退出；若环境有阻碍需定位并如实记录。
- 对真实测试后端进行受限的游客→查询→刷新→退出链路验证，不批量制造账户，不触发真实短信费用，供应商真实授权待准备好后单独验收。
- 保留后端及其他现有项目运行配置；不改 Redis ACL、数据库结构或服务器凭据。

## 由实现者确定的细节

依赖注入方式、具体 HTTP 客户端、Compose 导航实现、颜色/布局、内部类名和兼容版本，由实现者按简洁、可维护、与本机 SDK 兼容的原则选择并记录。建议 minSdk 26，中文 Material 风格，不增加无关权限。包名暂拟 `cn.yixiu.androidstarter`。
