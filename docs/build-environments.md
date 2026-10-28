# 构建环境与正式发布检查

项目仍只有 `app` 模块，不增加 flavor。任务名保持 `testDebugUnitTest`、`assembleDebug`、`compileReleaseKotlin`。

## 配置来源

| 文件 | 内容 | 是否提交 |
| --- | --- | --- |
| `product.properties` | 包名、名称、版本；当前包名保持 `cn.fuyaoshouhu.app` | 是 |
| `environments/test.properties` | debug 测试 API、微信及界面配置；保留原 API/微信值 | 是 |
| `environments/production.properties.example` | 正式配置模板；空值是有意的，不代表可发布产品 | 是 |
| `environments/production.properties` | 独立正式 API、真实主体、联系邮箱、政策、业务确认 | 否 |
| `keystore.properties` 与 keystore 文件 | 正式签名配置及私钥；沿用原有配置格式 | 否 |

debug 使用系统原有 debug 签名，不增加 applicationIdSuffix，不迁移账号数据。release **不回退读取测试配置**；正式配置缺失时仅允许结构编译，生成的空值/禁用功能不能用于发布。

BuildConfig 保留 `API_BASE_URL`、`WECHAT_APP_ID`、`SMS_ENABLED`、`WECHAT_ENABLED`、`TEST_TOOLS_ENABLED`、`FEEDBACK_URL`、`PRIVACY_POLICY_URL`、`TERMS_URL`，增加：

- `ENVIRONMENT`：debug 为 `test`，release 为 `production`，不能通过属性覆盖。
- `PRIVACY_VERSION`：测试以 `test-` 区分，正式必须以 `production-` 开头。政策变更后递增版本，避免复用测试同意。
- `ONBOARDING_ENABLED`、`ONBOARDING_VERSION`：引导开关及独立版本。
- `SUPPORT_EMAIL`、`OPERATOR_NAME`、`APP_RECORD_NUMBER`、`APP_MARKET_URL`：公开产品信息；未配置时界面应明确显示未提供。
- `DEMO_ENABLED`：缺省为 true；正式发布要求显式 false，真实产品必须替换/关闭示例入口。

`TEST_TOOLS_ENABLED` 在 release 中始终硬编码 false，且校验要求正式文件也写 false。SMS/微信由正式配置独立控制；微信启用时需明确填写 App ID，允许经过确认的测试/正式包共用同一微信应用。不要把任何后端密钥写入 BuildConfig：APK 内容可被提取。

## 正式准备

1. 复制 `environments/production.properties.example` 为 `environments/production.properties`，填写实际经过确认的信息。
2. 填写真实 `OPERATOR_NAME` 和 `SUPPORT_EMAIL`。`APP_RECORD_REQUIRED` 必须明确 true 或 false；true 需要真实备案号，false 需要 `APP_RECORD_EXEMPTION_REASON` 说明适用性判断。程序不判断司法管辖、资质是否适用或备案是否真实。
3. 配置公开 HTTPS 隐私政策与用户协议。构建检查 URL 格式及域名，拒绝测试域名、localhost、IP、示例地址和带账号密码的 URL；不发送网络请求。页面可公开访问、TLS、正文、权限/SDK 清单、主体和实际行为一致性须由发布人员人工审核。
4. 安装真正的正式签名文件并填写现有 `keystore.properties` 格式。校验检查文件存在、可读取私钥/证书、证书有效期，拒绝 Android debug 签名；不打印密码。debug 的原签名不变。
5. 实现真实首页/业务，检查所有示例、隐私文案、SDK/权限披露后，设置 `DEMO_ENABLED=false`、`RELEASE_CONTENT_READY=true`。后者是明确的人工确认，**不是程序已自动验证业务完整性的声明**。

没有真实正式信息时应保留模板空值，不要为了让检查通过填写虚构主体、资质或可用但无关的域名。

## 检查命令

在项目根目录，使用 JDK 17 或更新版本：

```powershell
.\gradlew.bat :app:verifyReleaseReady
.\gradlew.bat :app:compileReleaseKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
java scripts/VerifyReleaseReady.java --self-test
```

也可不启动 Gradle 独立检查：`java scripts/VerifyReleaseReady.java .`。脚本与 Gradle 使用同一份校验逻辑，不需要额外依赖。纯逻辑自测只用内存 fixture，不联网、不使用真实签名、不生成 APK。fixture 域名和签名字符串仅用于测试，不应复制为生产配置。

正式产物任务 `packageRelease`、`packageReleaseBundle`、`packageReleaseUniversalApk`、`packageReleaseApk`、`bundleRelease`、`assembleRelease`、`signReleaseBundle` 自动依赖 `verifyReleaseReady`，包括直接调用底层打包任务。资源/类的中间任务不挂此检查，确保 `compileReleaseKotlin`、release 单元测试和静态分析可在没有生产凭证时执行。检查任务每次执行，不缓存成功结果，且不依赖外网。

Gradle 的 `-x`、修改构建脚本或直接使用底层打包工具能够绕过任务，因此发布 CI 必须从可信代码运行检查和构建，不允许排除 `verifyReleaseReady`。这不是防恶意修改的安全边界。

## 本次脚本验证（2026-10-29）

- `& 'D:/android-studio/jbr/bin/java.exe' scripts/VerifyReleaseReady.java --self-test`：34 个正反 fixture 全部通过，退出码 0。
- `& 'D:/android-studio/jbr/bin/java.exe' scripts/VerifyReleaseReady.java .`：因当前缺少正式配置与签名正确阻止，退出码 1；未访问网络、未产生正式 APK。
- `git check-ignore environments/production.properties environments/production.properties.example`：仅实际正式配置被忽略，示例可提交。

Gradle 编译、单测、任务依赖检查及真机覆盖安装已由主实施流程完成，见[本轮验收](verification-2026-10-29-toolkit-foundation.md)。正式配置缺失时 `verifyReleaseReady` 按预期失败，结构编译成功。
