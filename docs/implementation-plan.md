# Android Kotlin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 可复制的 Kotlin Android 产品基础工程，对接 Hyperf 全部 10 个账号接口。

**Architecture:** 单 app 模块，独立纯 Kotlin 账号协调器、API 传输、加密存储、Compose 页面。所有写操作通过同一 Mutex 串行化，网络不确定结果持久化恢复，UI 不直接管理令牌。

**Tech Stack:** Kotlin、Compose、Android Keystore、HttpsURLConnection、Gson、Coroutines、微信 OpenSDK，Gradle Wrapper。

**Spec:** design.md（用户已批准，项目名改 android-kotlin）。执行结果见 progress.md 与 verification.md，设备/供应商尚待真实联调。

## Global Constraints
- 独立目录 android-kotlin；不修改后端和原 android 工程。
- minSdk 26，默认 HTTPS https://hyperf.tongyiwl.bond；所有 POST 使用 43 字符随机幂等键。
- 自动游客、双身份、独立设备会话；正式账号不能合并；无密码。
- 供应商未配置不模拟真实成功；debug 工具不编入 release。
- 复制产品集中配置；密钥、令牌不进入源码、日志或备份。

## Review Focus
- 请求完成但响应丢失：重启后使用原 key/body/credential 恢复。
- 并发过期请求：仅一次刷新，迟到响应不覆盖新账号。
- 退出结果不明：隐藏私有信息，保持恢复材料，不宣称成功。
- 微信回调进程重建：state/操作匹配且未过期才交换 code。
- 环境切换：新域名绝不接收旧凭证，未确认操作明确处理。

### Task 1: 协议与可构建工程
**Files:** android-kotlin/{settings.gradle.kts,build.gradle.kts,product.properties,app/build.gradle.kts}; app/src/main/java/cn/yixiu/starter/core/{Protocol,ApiClient}.kt; app/src/test/java/cn/yixiu/starter/core/ProtocolTest.kt
**Interfaces:** ApiTransport.execute(WireRequest): ApiReply；WireRequest 包含 origin/path/body/access/key；Protocol.origin、newKey、request、redact。
- [ ] 建立 Gradle 工程和集中产品配置，复用本机锁定依赖版本。
- [ ] 写 ProtocolTest：拒绝 HTTP/query/userinfo；guest/refresh 无 Authorization；随机 key 解码 32 字节；嵌套秘密脱敏；先运行见失败。
- [ ] 实现协议边界、HTTPS 无重定向无自动重试传输，运行 testDebugUnitTest 通过。

### Task 2: 账号状态与持久化
**Files:** core/{AccountEngine,AccountState,StateStore}.kt；storage/EncryptedStateStore.kt；test/core/AccountEngineTest.kt
**Interfaces:** StateStore.load/save(VaultState)；AccountEngine.start/me/refresh/logout/requestSms/submitPhone/requestWechat/submitWechat/recover/switchOrigin，state StateFlow。
- [ ] 写恢复、并发刷新、失效不建号、退出不确定、环境隔离测试，运行失败。
- [ ] 实现串行账号状态机、幂等恢复、业务码/冷却处理、加密原子存储及按账号清理钩子。
- [ ] 运行完整单元测试通过。

### Task 3: 产品页面、微信和调试工具
**Files:** StarterApplication.kt, MainActivity.kt, ui/{StarterViewModel,StarterScreen}.kt, wechat/{WechatBridge,WechatCallbackActivity}.kt, debug/.../DeveloperPanel.kt, release/.../DeveloperPanel.kt, AndroidManifest.xml；androidTest/.../StarterUiTest.kt
**Interfaces:** ViewModel StateFlow<ScreenState>，操作调用 AccountEngine；微信通过持久化 challenge 校验回调。
- [ ] 添加真实用户流程 UI 测试及微信回调匹配/过期测试；先运行验证缺失。
- [ ] 实现中文首页/个人中心/手机号界面和错误恢复入口；微信 SDK 与动态 applicationId 回调别名。
- [ ] 添加 debug 专属环境与接口日志，release 空实现；执行编译、测试、lint。

### Task 4: 验收与复制说明
**Files:** README.md, docs/{new-product-checklist,api-map,verification}.md, scripts/smoke-test.ps1
- [ ] 模拟器安装并验证基础界面和游客→me→refresh→logout，真实短信/微信明确列为待配置联调。
- [ ] 提供复制配置清单、架构扩展点、签名配置和构建命令。
- [ ] 新上下文 reviewer 做全工程审查，修复重要问题并完整验证。
- [ ] 记录 APK 路径和验证结果，初始化/关联用户指定的空仓库，交付源代码。
