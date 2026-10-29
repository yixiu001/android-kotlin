# 工具基础工程交互整理验收

日期：2026-10-29。本次提交收录在 `feature/tool-ui-refinement` 开发完成的交互调整，基于 `a064968`。仅更新安卓、测试与文档，保留现有测试配置；无后端修改、SQL 或部署。

## 范围与结果

1. 账号加载和恢复卡片不再显示在工具首页；后台连接结果不打扰本地工具。账号异常仍能从“我的 → 账号与安全”访问，加载失败保留重试，未知操作用弹窗继续原请求。
2. release 隐藏未开放的登录方式，debug 保留禁用诊断。既有测试开关、包名、API、微信 AppID 和签名保持。
3. 普通结果改为消费后清除的 Toast；确认/选择保留弹窗；输入格式和验证码错误在字段附近显示，执行中按钮禁用并显示状态。恢复文案不再要求普通用户理解请求/挑战等术语。
4. 手机号支持 +86、空格、连字符，其他国家前缀/字母/超长号码不被误当有效大陆号码。表单步骤与手机号随 Activity 重建恢复，验证码不持久化；账号/环境变化隔离草稿。
5. 帮助页和设置页共用反馈入口：HTTPS 网页优先、邮箱兜底；外部应用无法打开时可复制邮箱。两项为空时 release 隐藏，debug 显示配置提示；不自动附带账号信息。
6. ProductHome 是正常与本地模式唯一业务接入点；不提前初始化账号/微信。文本工具仍仅为占位，没有扩展完整性。

AccountEngine、账号协议、Vault schema、昵称头像规则、退出/注销规则及微信登录后一次性资料选择不变。没有执行真实短信发送、微信授权、退出、注销、账号重置或应用数据清理。

## 验证证据

- 旧实现的两项 UI 回归先失败：首页出现待确认状态；格式化手机号无法获取验证码。修复后通过。
- 独立只读审查发现状态实例随账号变化但计时协程仍更新旧实例。新增账号切换后倒计时回归先超时失败，改为 `LaunchedEffect(state)` 后通过；无 pending 的冷却状态也显示等待时间。
- `:app:testDebugUnitTest`：92 项，91 通过、1 项真实 API 冒烟按默认设置跳过，0 失败/错误。
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:compileReleaseKotlin :app:lintDebug` 成功；lint 0 错误、13 项既有警告，未升级依赖或生成正式产物。
- P30 手动限定运行 `ToolInteractionUiTest`（5）、`StarterUiTest`（7）、`FoundationUiTest`（5）：17 项全部通过，包含隐私前不创建在线内容、账号/微信资料入口、页面恢复、手机号粘贴/重建、Toast 消费、正式版隐藏能力的组合测试。
- 测试均使用内存模拟账号，不创建真实账号 ViewModel，不操作真实账号存储；没有运行 `EncryptedStateStoreTest` 或 Gradle 全量 connected 测试。
- `product.properties`、`environments/test.properties`、`gradle.properties`、`AccountEngine.kt` 的 SHA256 与开始前一致；`git diff --check` 通过。

## 真机交付与数据保留

- Huawei P30（ELE-AL00），包名 `cn.fuyaoshouhu.app`。
- 主 APK 与测试 APK 均手动 `adb install -r` 覆盖安装，未卸载或清除数据。
- 最终通过 `am start -W` 启动成功，`mResumedActivity` 为 MainActivity；已查看实际首页截图，未显示账号加载/恢复卡片，没有触发登录或清理操作。
- 首次安装时间始终为 `2026-10-29 04:55:35`；最终覆盖更新时间 `2026-10-29 19:45:26`。
- 实施前、覆盖安装后与最终 17 项 UI 测试后，`no_backup/account-vault` SHA256 均为 `b5513cc6c2d03a553b64cef35b1951981cb62065b39487d7a5c33e03a7aac8a0`。此比较发生在启动真实主页面之前，正常恢复会话后文件可能更新，不将摘要描述为永远不变。
- APK：`app/build/outputs/apk/debug/app-debug.apk`。
- APK SHA256：`9a0016742ac49dae0a918d268fa135fc19dc286d17221c525a6c70f06645eed8`。

验证边界：本轮没有重复真实手机号/微信登录或破坏性账号操作，账号行为依据原引擎回归、界面隔离测试与未变更存储格式核对；没有宣称完成渠道审核或所有品牌设备验收。
