# 当前状态

最后核对：2026-10-29（北京时间）。本页负责 Android 配置与验证结果；后端运行状态以[后端当前状态](https://github.com/yixiu001/hyperf-swow/blob/main/docs/current-state.md)为准。

## 代码与配置

| 项目 | 本次核对结果 |
| --- | --- |
| Android 代码基线 | `a9abed39db7075499b3aac9758c54d50daa3a97f`；整理前本地 HEAD 与 GitHub main 一致 |
| 配套后端基线 | `ff7028aa550a35c59d38a1ba55903717017e279a`，测试部署 Jenkins #19 |
| 应用名 | Android 基础工程 |
| applicationId | `cn.fuyaoshouhu.app`；代码 namespace 仍为 `cn.yixiu.starter` |
| API_BASE_URL | `https://hyperf.tongyiwl.bond`，不含 `/api/v1`，客户端自动添加 |
| 微信 AppID | `wx57879d4692d97d56`；AppSecret 仅在后端 |
| 客户端入口开关 | SMS_ENABLED / WECHAT_ENABLED 均为 true；不代表供应商验收通过 |
| 包名及环境选择 | 只有一份 product.properties，尚无 test/prod 产品变体；debug/release 共用产品配置 |
| 本机 debug 签名 MD5 | `223fbaaba46bea0e5ef02ee43444dc90`，此前已核验；不是产品正式签名 |
| 发布状态 | 正式签名与发布流程尚未完成验收 |

本次整理不改变配置值或应用逻辑，不升级 SDK/依赖。表中的 SHA 是被验证的代码基线，不是文档整理提交。当前包名及 AppID 为已有联调配置，不应直接带到复制出的新产品。

## 本次验证

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 成功；首次运行多数任务命中缓存，随后使用 `:app:testDebugUnitTest --rerun-tasks` 重新执行测试。
- JVM 结果：**25 项通过，1 项真实后端 smoke 默认跳过，0 失败/错误**。未把跳过的测试算成通过。
- lint：当前报告 0 errors / 9 warnings（依赖升级提示）；本次复用未变化代码的 lint 缓存，不代表重新扫描依赖漏洞。
- debug APK 对应现有源码，SHA-256：`1286DA008B37541EF81D4290AAE3A9AD5790CBB38F5DA47646E074DAFCD88F21`。位置 `app/build/outputs/apk/debug/app-debug.apk`。
- `adb devices -l` 未列出设备，本次未运行 Android 仪器测试；此前测试包编译成功不等于设备测试通过。
- release 构建及真实 guest→me→refresh→logout 的此前结果见[2026-10-27 验收记录](verification.md)，本次未重跑，不宣称本次全部真机验收通过。

## 待验收

1. 微信真机曾返回 scope 权限错误 10005，尚无修复成功记录；配置匹配或挑战接口成功不能代替微信平台权限验证。
2. 真实短信收取、手机登录/绑定、微信授权后的登录/绑定与老账号合并尚无完整验收记录。
3. 在连接设备上运行 UI、Keystore、进程重建测试，并验证实际签名的 release 包。此前 R8 的微信 SDK stack map table 警告仍需 release 真机验证。
4. 测试/生产配置拆分、隐私流程、资料修改、换绑/解绑、注销、设备管理均不属于当前已交付功能，不展示已完成状态。

## 维护方式

- 接口约束以[后端 API](https://github.com/yixiu001/hyperf-swow/blob/main/docs/api/android-account.md)为准，同步维护[接口映射](api-map.md)和测试。
- 配置来源为 product.properties；[供应商说明](providers.md)记录配置步骤，[复制清单](new-product-checklist.md)记录新产品必改项。
- 真实环境变更或验证后更新本页；保留日期验收和设计计划作为历史，避免把历史“未配置”或“已通过”当成现在的状态。
- 全部说明入口见[文档导航](README.md)。
