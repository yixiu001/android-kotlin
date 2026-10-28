# 联调工具验收记录 · 2026-10-29

Android 功能提交 9130753 已推送 main；最终两项恢复问题已修复并复审通过。后端 Jenkins #21 发布成功，未执行真实账号重置或设备清除。

执行命令：

```powershell
$env:JAVA_HOME = 'D:/android-studio/jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileReleaseKotlin --console=plain
```

- JVM：48 项总计，47 通过、1 个既有 LiveApiSmokeTest 跳过，0 失败/错误。
- 联调相关测试：ResetAccountBridgeTest 2 项、ResetPolicyTest 7 项、TestToolsClientTest 4 项、ResetRecoveryTest 3 项、ResetSourceBoundaryTest 1 项。
- debug APK 构建成功；lint 0 errors / 9 warnings（依赖版本等既有提示）；release Kotlin 编译通过。
- release 生成的 `TEST_TOOLS_ENABLED=false`，工具实现位于 `src/debug`，release 仅保留空入口。
- 当前项目 debug 开关已启用；缺少配置时回退 false。复制新产品应按[联调说明](test-tools.md)默认关闭，release 强制 false。
- APK：`app/build/outputs/apk/debug/app-debug.apk`。
- SHA-256：`D129D7396DF022C54BAE8D11844A2BE992EEC3F5F9B6D4B01C2C3A59D19C2EB2`。

独立审阅指出的明确拒绝后无法重预览问题已复现、修复并复审通过：首次明确拒绝且没有此前未知结果时清除预览、允许重新 prepare；未知结果以及未知结果之后的拒绝继续保留同一请求并禁止新重置和本地清除。已补充旋转/导航后面板仍可展开的回归覆盖。错误码不直接显示供应商或服务器敏感响应。

这不是手机仪器验收：本轮未真正清除 App 数据或执行真实账号重置，未安装 APK。配套后端 6052fe8 已经 Jenkins #21 成功发布，测试库 SQL003 和联调开关已就绪；账号数据与 Redis 配置保持不变。首次安装/引导状态、权限重置及供应商真实流程的设备清单见[联调工具](test-tools.md)。
