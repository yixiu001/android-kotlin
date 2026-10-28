# 轻量工具基础工程实施计划

**Goal:** 完成用户批准的六项：启动与隐私、主题与设置、公共页面状态、页面职责拆分、测试/正式配置、发布检查；同步文档并保留数据安装到 P30。

**Architecture:** 仍为单 app 模块。保留 AccountEngine/Vault/接口语义，用应用级启动门禁控制账号初始化与 SDK 回调。普通设置独立持久化；页面、账号表单和通用组件分离。配置按 debug/test 与 release/production 装配，正式打包前执行检查。

**Tech Stack:** Kotlin、Compose Material 3、Android preferences（少量配置）、现有协程/StateFlow；不新增数据库、DI 框架或业务后端。

**Spec:** 本会话已批准的六项改版建议；保持轻量、不完善所有可选业务、现有账号不受影响、安装不得清除数据。

## 约束

- 现有 cn.fuyaoshouhu.app 包名、debug 签名、测试 API/微信配置、Vault schema 均不变。
- 不调用真实账号重置、退出、注销；不清用户数据；不推送/部署。
- 保留 android.injected.androidTest.leaveApksInstalledAfterRun=true。手机先后对比首次安装时间和加密 Vault 摘要，测试限隔离类。
- 首次升级不假定用户已同意政策；保留原凭证，提示用户亲自确认。未同意时不初始化账号、不启动微信 SDK；允许纯本地工具。
- 撤回时不得有账号请求正在进行或待确认；撤回不等于注销，不删除凭证，停止后续账号网络动作。
- 当前正式主体/政策/备案信息未知，不捏造。debug 提供明确测试说明，正式打包由检查阻止缺项。

## 任务与接口

1. 配置/发布检查（独立文件）：app/build.gradle.kts、product.properties、environments/*、scripts/*、相关配置文档。BuildConfig 保持原字段并新增 ENVIRONMENT、PRIVACY_VERSION、ONBOARDING_ENABLED、ONBOARDING_VERSION、SUPPORT_EMAIL、OPERATOR_NAME、APP_RECORD_NUMBER、APP_MARKET_URL。debug 使用原身份；release 正式配置单独提供。verifyReleaseReady 输出明确问题，package/bundle release 自动依赖；compileReleaseKotlin 可做结构检查但不能绕过正式打包校验。
2. 启动门禁/设置基础：AppPreferences、StartupPolicy、SafeCache；拒绝/同意/版本更新/撤回/引导跳过可测试，清理只限 cacheDir/tool-temp。账号存储不迁移。
3. Compose 界面：MainActivity 启动门禁与安全状态栏、主题、系统启动页；轻量本地文本计数示例；隐私/权限/SDK/关于/设置；提取账号 UI 与公共状态，保持现有操作语义。
4. 验证与交付：离线单测、限定 Compose UI 测试、debug/lint/release Kotlin、发布检查负例；只读审查与文档；覆盖安装，校验数据摘要，停留隐私确认由用户操作。

## 验收重点

- 同意前/拒绝后无账号创建或 SDK 回调处理；升级不清凭证；隐私版本改变重新确认。
- 主题持久化/系统跟随、返回/旋转/空态/错误重试，保留账号异常恢复入口。
- 临时缓存清理不触及 Vault、偏好、用户文件；运行中/待确认时不能撤回。
- 正式包不使用测试环境且缺配置不能打包；调试工具 release 仍为空实现。
- 本地工具不联网，不以虚拟账号冒充真机数据；微信资料选择仍仅随登录/绑定。

## 执行记录

- [x] 记录设备安装身份与加密存储摘要；工作区干净。
- [x] 配置与发布检查实现/测试。
- [x] 启动/设置/隐私状态与隔离测试。
- [x] 页面拆分、主题、公共状态与示例。
- [x] 最终检查、文档、覆盖安装和数据保留核验。实际结果及用户自行同意后的会话文件变化见 `docs/verification-2026-10-29-toolkit-foundation.md`。

Ruling: 在当前独立仓库新建 feature/toolkit-foundation 分支执行，保留本机 SDK 与构建缓存；不复制签名或凭证到新工作树。独立配置任务交给子代理，账号与 UI 集成由主代理实现，最终进行只读独立审查。
