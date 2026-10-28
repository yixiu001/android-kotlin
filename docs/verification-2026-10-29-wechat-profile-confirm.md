# 微信登录后资料确认验收（2026-10-29）

状态：Android `daa37e0` 与后端 `f105663` 已推送 main，测试库 SQL004 已执行，配套后端经 [Jenkins #22](http://192.168.1.10:8080/job/hyperf-swow-test/22/) 后续更新发布成功。旧 Vault 直接升级，不清应用数据；原 debug 重置工具及产品配置保留。服务器 .env、Redis 配置及现有账号数据摘要在发布前后保持一致，未重置账号。

## 自动验证

工作目录 android-kotlin，PowerShell：

```powershell
$env:JAVA_HOME = 'D:/android-studio/jbr'
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileReleaseKotlin --console=plain
```

结果 BUILD SUCCESSFUL（最终完整构建 13 秒）。81 项测试，80 通过，LiveApiSmokeTest 1 项默认跳过，0 失败。lint 0 错误 / 9 个既有依赖版本提示。debug APK 和 release Kotlin 编译成功；未制作签名 release APK。

新增 WechatProfileTest 33 项，涵盖最终合并用户缺失字段与完整账号、异常/缺失/部分候选、独立替换 strict JSON、跳过及 Gson 进程重建、过期、未知确认恢复原请求、429/503 保留、终态候选错误保留登录、手动意图经 challenge/Pending 重建恢复及下一次授权隔离、同会话刷新、用户/session/generation/environment/logout 隔离、旧授权取消/新挑战失败、网络冷却下本地跳过、旧 Gson Vault、日志脱敏、严格注销请求体。原账号/生命周期/调试工具测试通过，更新一处旧微信 challenge 请求体断言为 use_wechat_profile=true。

测试先添加并确认失败：第一轮缺少预览 API 导致测试编译失败；边界测试明确观察 transientConfirmationFailureRetainsExactRequest 的 503 丢 pending 断言失败、localSkipIsAvailableDuringNetworkCooldown 因 429 失败、startingNewAuthInvalidatesOldChallengeEvenWhenNewRequestFails 因旧授权仍可提交失败，以及 deletionDoesNotRequestCandidateOrAddUnknownExchangeFields 的严格注销字段断言失败；逐项实现后全绿。

初次 Gradle 受沙箱限制不能写 ~/.gradle 缓存锁；按要求申请提升权限后成功运行。该问题属于执行环境，不作为测试失败。

APK：`app/build/outputs/apk/debug/app-debug.apk`

SHA256：`be680133503f73a1657f9b9c06417dc6f59bcc9185963b900327e2b9d5538914`

报告：`app/build/reports/tests/testDebugUnitTest/index.html`、`app/build/reports/lint-results-debug.html`。

## 未执行及后续验收

尚未完成微信外部授权、短信或 instrumentation 业务测试。后端已完成连接检查、HTTPS 健康与无效凭证拒绝验证，SQL004 已执行；没有清除设备数据、重置账号或修改 Redis 配置。安装仍使用 cn.fuyaoshouhu.app，受现有签名和微信登记约束。

后端已发布，后续用专用测试账号真机检查：新/老账号最终资料缺失/完整/部分；预览头像和昵称、独立选择与跳过重启；正式账号同微信重新授权替换，未绑定时解释绑定，不同微信拒绝；游客提示可能切换；取消、进程重建、授权状态隔离；候选过期、断网/429/503 后恢复原确认。此业务列表尚未执行，本次发布与安装不调用重置工具。


## 独立审查 round1 修复

已修复 I1：ACCESS_TOKEN_EXPIRED 明确拒绝原写入后，原 path/body/manual intent 与 origin/user/session/generation 作为加密 Pending 的非 wire continuation 和 refresh 请求原子落盘。未知 refresh 重放同一请求；成功刷新和续接的新 Pending 原子落盘，再发送原操作；续接最多一次刷新，不重放已成功的 OAuth 交换。恢复按原操作执行登录后的 /me 或注销后的新游客生命周期。续接前后校验账号/会话/环境/代数，变化即停止。新增 8 项 scoped regression，最初 3 项恢复断言失败和 1 项本地 session 边界断言失败均已修复。

M1：预览的昵称/头像独立勾选改为 rememberSaveable(previewId)，保留旋转选择，仍须明确确认。此项已编译/lint 验证；未运行设备旋转验收。

该轮历史构建 BUILD SUCCESSFUL（13 秒），78 项测试 77 通过 / 1 真实 smoke 跳过；lint 0 错误 / 9 条既有提示，debug APK 与 release Kotlin 均成功。当前发布状态见顶部，最终测试数和 APK hash 已更新为后续最终产物。


## 最终集成审查修复（当前验收）

仅对 `/me/profile/wechat` 的 AUTH_RESULT_EXPIRED 回执过期，清除无法再查询的确认 Pending 和候选，保留当前会话，不认定成功、失败或会话无效，不自动重写。提示点击现有“更新账号信息”（GET /me）查看实际资料；真正的 SESSION_INVALID 继续隐藏资料并走原失效处理。登录/刷新/退出/注销回执过期策略不变。未新增恢复框架或网络动作。

新增 3 项聚焦回归：丢失确认响应后回执过期仍可 /me 核对实际资料且不重复写入；/me 真正会话失效仍受保护；登录回执过期保留原阻断策略。测试先红（2 失败、1 原策略通过）后绿。最终全量构建一次 BUILD SUCCESSFUL（12 秒），81 项测试：80 通过、1 个默认真实 smoke 跳过；lint 0 错误、9 条既有提示，debug APK 与 release Kotlin 编译成功。上方 hash 已更新为本次最终产物。此前 round1 数字为历史修复记录。
