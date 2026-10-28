# 资料与注销验收记录 · 2026-10-29

已实现昵称编辑、六个默认头像选择、微信昵称/头像链接自愿导入，以及手机号/微信验证注销。无图片上传、OSS 或新的敏感配置。

执行：

```powershell
$env:JAVA_HOME = 'D:/android-studio/jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
```

- JVM：AccountEngineTest 21、AccountLifecycleTest 5、ProtocolTest 4，共 30 通过；LiveApiSmokeTest 1 项默认跳过，0 失败/错误。
- 覆盖注销响应丢失后隐藏资料与原请求恢复、失败保持当前账号、确认后清缓存并建立游客、微信资料选择跨重启保存、注销禁导入资料、旧 challenge 默认不授权导入、资料更新保持会话/身份。
- debug APK 成功：`app/build/outputs/apk/debug/app-debug.apk`。
- SHA-256：`D513663310B4DEDE15CB75AFF853A739506CCBD8B43D3F0BA0378737E7AE8F11`。
- lint：0 errors / 9 warnings，均为已有依赖版本提示。
- Vault schema=1、product.properties、签名、依赖和构建工具版本均保持不变。

独立审阅覆盖两端实现，后端发现的昵称 "0" 及历史合并资料擦除问题已修复。后端本轮 103 测试 / 1485 断言通过，PHPStan 无错误。

本机 adb 未连接设备。新微信授权、头像链接加载/失败回退、短信和注销需后端 SQL 002 升级部署后进行真机验收；此轮未推送、部署或发起真实注销。既有阶段用户已反馈测试通过，与新增功能的未验收项分开记录。

真机检查清单：

1. 游客/正式账号修改昵称与默认头像，重启后仍一致。
2. 首次绑定微信分别选择同意/拒绝资料导入；已有昵称/头像保持不变；断网头像使用默认图。
3. 登录已绑定老账号时，保留老账号资料。
4. 注销弹窗明确立即生效、30 天身份限制；错误验证码/不同微信不能注销。
5. 使用专用账号注销后，当前 App 进入新游客，其他设备旧凭证失效；原身份返回限制期提示。
6. 人为断网造成注销响应未知时，不提前创建游客；恢复原请求后完成切换。
