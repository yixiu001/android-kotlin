# 当前状态

更新：2026-10-29（北京时间）。协议见[后端账号 API](https://github.com/yixiu001/hyperf-swow/blob/main/docs/api/android-account.md)。

## 当前工作区：轻量工具基础工程

本次提交包含在 `feature/toolkit-foundation` 开发完成的六项基础能力及文档，保留现有测试配置；未修改或部署后端。保持单 app 模块，账号协议与 Vault schema 不变。检查和覆盖安装的实际结果统一见[本轮基础工程验收](verification-2026-10-29-toolkit-foundation.md)，本文不将历史数字当作本轮测试结果。

- 启动：使用系统 SplashScreen 兼容层，不新建开屏 Activity、不人为等待。先读普通设置；首次安装、旧版缺同意记录或政策版本变化时显示隐私说明。明确同意后才初始化账号及原会话恢复；无凭证才建游客。
- 隐私：暂不同意进入本地模式；撤回会停止后续账号请求、保留原凭证，不退出、不注销、不清空。有账号请求、微信回调或待确认操作时禁止撤回。SDK、权限说明和系统权限设置入口集中展示，正式政策由产品配置。
- 主题与设置：支持跟随系统/浅色/深色并持久保存，系统栏同步。普通缓存清理只处理 `cacheDir/tool-temp`，保留 Vault、偏好、用户文件；与 debug 全量清除工具分开。
- 页面职责：`StarterScreen` 负责导航，`ui/account` 承接原表单/弹窗/恢复；共享加载、空态和错误组件。原账号功能、确认文案、幂等恢复和微信登录后资料选择语义保留。
- 本地示例与引导：文本计数无需账号和网络，包含清空和主动复制统计结果。可选引导默认关闭；开启后可跳过，按独立版本记录完成。新产品必须替换真实业务，关掉示例仍只有未配置提示。
- 环境与发布：身份保留在 `product.properties`；debug 读 `environments/test.properties`，release 读独立、不入库的正式配置。正式打包强制执行配置/签名/政策/主体/示例检查，缺项不能生成正式产物；release Kotlin 编译不等同发布验收。

当前尚未提供真实正式运营主体、政策、签名和业务首页，不宣称可上架。测试环境原包名、API 和微信 AppID 保持；配套后端沿用 `f105663` / Jenkins #22 / SQL004。完整配置见[构建环境](build-environments.md)，接入与页面见[个人中心](profile-center.md)。

历史设备事件：此前个人中心阶段首批仪器测试曾默认卸载 P30 应用，重新安装后为游客 13。已经固定测试后保留 APK；此事件及当时数据核对见[历史个人中心验收](verification-2026-10-29-profile-layout.md)。不能将那一阶段描述为原会话完整保留，也不能把历史事件当作本轮已发生的数据变化。

## 历史发布基线：微信登录后资料确认（配套后端已发布）

以下历史章节保留原阶段的测试、提交及修复记录；其中“本次”“本轮”指微信资料确认阶段，不指当前基础工程分支。

Android `daa37e0`、后端 `f105663` 已提交并推送 main。测试库 SQL004 已执行，[Jenkins #22](http://192.168.1.10:8080/job/hyperf-swow-test/22/) 后续更新 SUCCESS，release `b22-f10566362f8b-774bcac5`，已具备新版 APK 联调条件。容器健康、HTTPS 根页面及默认头像正常；新接口拒绝无效凭证。发布核验时原有 8 个用户、2 条身份及相关账号数据摘要不变，服务器 .env 与 Redis 配置不变，没有重置账号。

普通微信登录/绑定只请求可选候选，不自动写资料。先建立最终用户和最终会话，再展示实际微信昵称和头像；自动模式仅允许补充最终账号缺失字段，完整账号不弹窗。每项独立勾选，也可跳过；候选获取失败、格式异常或过期均不影响登录。

2026-10-29 后续精简：移除“编辑资料 → 使用微信资料/前往微信登录”及独立重新授权操作。资料选择仅随正常微信登录/绑定成功出现；完成、跳过或过期即结束本次选择。编辑资料只改昵称、选择内置头像，可保留已有微信头像。完整账号仍不弹窗，不增加“账号终生只能选择一次”的数据库规则。本次客户端修改与两端文档一并提交；后端沿用 #22，不重新部署，无新增迁移。

候选保存在原 schema=1 加密 Vault，无需清空应用数据。旧手动授权元数据和请求恢复只为升级兼容保留，新 UI 不再发起该操作。候选绑定 origin/user/session/generation；/me 与同会话刷新保留尚未结束的选择，跳过持久化，退出和账号/环境切换清除。未知确认保留原请求、凭证与幂等 key；网络/429/5xx 不消耗候选，成功或明确候选终态错误才清除。确认不重用 OAuth code、不再次调用微信。日志隐藏候选凭证及相关个人资料。

自动游客、短信登录绑定、老账号合并、双身份与独立会话、昵称/内置头像、验证注销及 debug 一键重置保持原行为。头像仍受控 HTTPS、限界读取、禁重定向、无磁盘缓存；无上传或 OSS。注销立即生效，原身份 30 天内受限，未知结果隐藏资料并恢复原请求。

## 历史微信资料确认验证（非本轮基础工程验证）

81 项测试：80 通过、1 个真实 LiveApiSmokeTest 按默认配置跳过、0 失败。新增 33 项 WechatProfileTest 覆盖最终用户、缺失/完整/部分/异常候选、独立替换、跳过/Gson 重建、过期、未知确认/原请求回放、429/503、同会话刷新、账号环境隔离、授权意图重建与旧回调隔离、旧 Vault、日志脱敏。

执行成功：`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileReleaseKotlin --console=plain`，JDK 为 `D:/android-studio/jbr`。lint 0 错误、9 个既有依赖版本提示。APK：`app/build/outputs/apk/debug/app-debug.apk`；SHA256 `be680133503f73a1657f9b9c06417dc6f59bcc9185963b900327e2b9d5538914`。

真实微信/短信业务流程与真实接口冒烟尚未验收；SQL004 和服务部署已完成，没有执行设备清理或账号重置。发布前四项 Android 检查再次成功。完整报告见 [微信资料确认验收](verification-2026-10-29-wechat-profile-confirm.md)。

## 已发布基线（历史，与本轮分开）

Android `9130753` / 后端 `6052fe8` / Jenkins #21 / SQL003 是一键联调工具基线。当时 47 项通过、1 个真实 smoke 跳过，见[历史联调验收](verification-2026-10-29-test-tools.md)。普通 access 经 AccountEngine 调试桥 prepare，短期 grant execute；成功清本地，未知只重试原授权，release 不包含调试实现。

更早的 Android `132ecee` / 后端 `9db609e` / Jenkins #20 / SQL002 是资料编辑和注销基线，当时 30 项通过、1 个真实 smoke 跳过，见[历史验收](verification-2026-10-29-profile-deletion.md)。历史微信授权前选择资料的描述已被本轮登录后预览取代，不能作为本轮验收步骤。原阶段用户反馈测试通过，历史微信 10005 不是当前已知阻塞。

## 当前 debug 配置与正式隔离

| 项目 | 值 |
| --- | --- |
| 应用名 / applicationId | Android 基础工程 / cn.fuyaoshouhu.app |
| namespace | cn.yixiu.starter |
| API_BASE_URL | https://hyperf.tongyiwl.bond（仅 origin） |
| 微信 AppID | wx57879d4692d97d56，AppSecret 仅服务端 |
| 短信 / 微信入口 | 均已开启 |
| 本机 debug 签名 MD5 | 223fbaaba46bea0e5ef02ee43444dc90 |
| 环境配置 | product.properties 为身份；environments/test.properties 为 debug 环境；release 独立 production.properties |

本轮未修改 Redis 和后端环境；客户端已拆分环境配置并加入发布检查。真实生产签名、主体、政策及业务内容仍由具体产品准备。未增加 flavor 矩阵或多模块架构。操作入口见[文档导航](README.md)及[接口映射](api-map.md)。


## 历史独立审查 round1 修复

已修复 I1：ACCESS_TOKEN_EXPIRED 明确拒绝原写入后，原 path/body/manual intent 与 origin/user/session/generation 作为加密 Pending 的非 wire continuation 和 refresh 请求原子落盘。未知 refresh 重放同一请求；成功刷新和续接的新 Pending 原子落盘，再发送原操作；续接最多一次刷新，不重放已成功的 OAuth 交换。恢复按原操作执行登录后的 /me 或注销后的新游客生命周期。续接前后校验账号/会话/环境/代数，变化即停止。新增 8 项 scoped regression，最初 3 项恢复断言失败和 1 项本地 session 边界断言失败均已修复。

M1：预览的昵称/头像独立勾选改为 rememberSaveable(previewId)，保留旋转选择，仍须明确确认。此项已编译/lint 验证；未运行设备旋转验收。

该轮历史构建 BUILD SUCCESSFUL（13 秒），78 项测试 77 通过 / 1 真实 smoke 跳过；lint 0 错误 / 9 条既有提示，debug APK 与 release Kotlin 均成功。后续最终结果与当前发布状态见本文上方，不能把该轮数字当作当前总数。


## 历史微信资料确认最终集成审查修复

仅对 `/me/profile/wechat` 的 AUTH_RESULT_EXPIRED 回执过期，清除无法再查询的确认 Pending 和候选，保留当前会话，不认定成功、失败或会话无效，不自动重写。提示点击现有“更新账号信息”（GET /me）查看实际资料；真正的 SESSION_INVALID 继续隐藏资料并走原失效处理。登录/刷新/退出/注销回执过期策略不变。未新增恢复框架或网络动作。

新增 3 项聚焦回归：丢失确认响应后回执过期仍可 /me 核对实际资料且不重复写入；/me 真正会话失效仍受保护；登录回执过期保留原阻断策略。测试先红（2 失败、1 原策略通过）后绿。最终全量构建一次 BUILD SUCCESSFUL（12 秒），81 项测试：80 通过、1 个默认真实 smoke 跳过；lint 0 错误、9 条既有提示，debug APK 与 release Kotlin 编译成功。上方 hash 已更新为本次最终产物。此前 round1 数字为历史修复记录。
