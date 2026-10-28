# 微信资料仅在登录后选择（2026-10-29）

## 当前行为

- 微信登录/绑定成功后，最终账号尚有空资料时提供一次可跳过的昵称/头像选择；完整账号不打扰。
- 确认、跳过或过期结束本次选择。重新读取账号或重启不会重新创建已结束的选择；未确认请求继续沿用原恢复机制。
- 编辑资料只修改昵称与选择内置头像，保留现有微信头像时不用重新授权。删除编辑页的“使用微信资料/前往微信登录”和 ViewModel 中独立 wechatProfile 操作。
- 过期/失效提示不再引导重新授权。旧 Vault 手动意图、overwrite 协议及未知请求恢复仅为覆盖升级兼容保留，新界面不能发起该流程。

后端接口无需改动，继续使用 Jenkins #22 / f105663 与 SQL004。本次仅同步后端协议、状态与复制清单文档，不修改数据库、Redis 或服务器环境，不重置账号。本次提交包含安卓修改与两端文档；后端无代码变更，不触发 Jenkins 部署。

## 验证

新增资料页测试在旧代码上先失败：发现不应存在的“使用微信资料”按钮。修改后两项新增测试通过：编辑页无微信入口；登录后资料选择可跳过且只发出 skipWechatProfile 动作。

P30（ELE-AL00 / Android 10）执行 StarterUiTest 共 4 项、全部通过。已有短信页测试在真机小屏首次因按钮位于滚动区域下方失败，改为滚动到控件后验证，未修改业务页面。测试使用构造的 AccountView 和隔离 Compose Activity，没有调用微信、短信、后端账号或测试重置接口。

```powershell
$env:JAVA_HOME = 'D:/android-studio/jbr'
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileReleaseKotlin :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=cn.yixiu.starter.StarterUiTest' --console=plain
```

最终 BUILD SUCCESSFUL。单元测试共 81 项，80 通过、1 项真实 LiveApiSmoke 默认跳过、0 失败；Debug APK、lint、Release Kotlin 编译通过。真实微信登录及头像选择仍由用户联调，不把合成 UI 测试当作供应商验收。

APK SHA256：`8c7b51cff79ff4e7e46e9bb88cc82f52463252b33fa2563feb21b8e8617d3ea5`。

已通过 `adb install -r` 覆盖安装到 P30，安装 Success；启动 `cn.fuyaoshouhu.app/cn.yixiu.starter.MainActivity` 返回 Status: ok，进程正常。没有卸载主应用或清除其数据。
