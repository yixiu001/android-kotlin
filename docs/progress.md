# Implementation ledger — 2026-10-27-android-kotlin

- 用户批准完整方案并授权细节；指定工程 android-kotlin 和 GitHub 仓库，远程无分支。
- 采用独立新工程，无现有代码/主分支需要隔离；未修改其他项目。
- 计划：../.. 的 docs/superpowers/plans/2026-10-27-android-kotlin.md。
- Task 1 → Task 2：WireRequest / ApiTransport 为状态机唯一网络接口。
- Task 2 → Task 3：AccountEngine 为 UI 唯一会话操作入口。
- 使用本机缓存可核验的 AGP 9.4.1 / Kotlin 2.2.10 / Compose BOM 2025.08.00；版本固定后以实际构建验证兼容性。
- 测试使用 JVM 可运行的纯 Kotlin 核心，Android 存储/UI 另由仪器测试覆盖。
- Task 1 完成：协议测试先 4 失败，再全部通过；可构建独立工程。
- Task 2 完成：核心行为测试先 10 失败，再通过；增补验证码刷新保留、双身份绑定、恢复等覆盖。
- Task 3 源码和 APK 完成；设备仪器测试编译完成，但 AVD 无硬件加速驱动，软件模拟也未连接，运行验收待真机。
- Final review：429 不丢恢复请求、挑战期限不从重放时间重置、登录恢复更新身份；3 条回归先失败再修复，完整 26 项含真实后端 smoke 通过。
- Task 4：debug/release 构建、lint 0 errors、真实 guest→me→refresh→me→logout 已验证，文档说明未完成的供应商/真机验收。
- 选择：串行化账号请求简化恢复一致性；后续高并发业务需单独实现代际检查。既有编译依赖锁定不整体升级，9 条新版依赖提示保留。
- 用户已完成供应商申请但尚未配置服务端；没有自动启用开关或发送计费短信。
- 后续配置接入：用户完成服务器 .env 并确认最新包名 cn.fuyaoshouhu.app 与签名；经 Jenkins #19 部署，容器加载全部配置。同步公开 AppID wx57879d4692d97d56 并开启客户端微信入口；challenge/退出验证通过，没有发送计费短信或模拟微信身份。
