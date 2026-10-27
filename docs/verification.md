# 验收记录 — 2026-10-27

## 已验证

- 完整 JVM 测试 26 项通过：21 项账号状态机、4 项协议、1 项显式启用的真实后端链路。
- 真实测试域名 https://hyperf.tongyiwl.bond：
  - GET / → 200
  - POST /api/v1/auth/guest → 201
  - GET /api/v1/me → 200
  - POST /api/v1/auth/refresh → 200
  - GET /api/v1/me → 200
  - POST /api/v1/auth/logout → 200，已撤销本次会话
- assembleDebug、assembleRelease、assembleDebugAndroidTest 全部构建成功。
- lintDebug：0 errors；9 warnings 均是现有锁定依赖可升级提示，没有屏蔽 lint。
- 审查修复：429 恢复保留原请求、挑战重放不延长期限、登录恢复后拉取真实绑定状态；三项均先复现失败再通过回归测试。
- 合并 Manifest 只有 INTERNET 及 AndroidX 内部动态接收器权限，无短信读取、通讯录或存储权限。

## APK

- debug：app/build/outputs/apk/debug/app-debug.apk（约 13.7 MiB）
- release：app/build/outputs/apk/release/app-release-unsigned.apk（未签名）
- 仪器测试包：app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
- debug SHA-256：1286DA008B37541EF81D4290AAE3A9AD5790CBB38F5DA47646E074DAFCD88F21

## 当前 debug 身份

- 包名 cn.fuyaoshouhu.app（按用户最新登记信息修正）。
- 签名 MD5：22:3F:BA:AB:A4:6B:EA:0E:5E:F0:2E:E4:34:44:DC:90。
- 微信登记格式：223fbaaba46bea0e5ef02ee43444dc90。
- 来自本机 Android debug 签名，仅用于本机测试包；换电脑/密钥后需要重新核对。

## 尚未验收的边界

1. 本机没有可连接 Android 设备；已有 API 37 AVD 因 Android Emulator hypervisor driver 未安装而启动失败。尝试无窗口软件模拟后也没有可用 adb 设备。没有修改 Windows 虚拟化配置或重启系统。
2. 因此 Compose UI、Keystore 加密存储和进程重建的 Android 仪器测试仅编译完成，未在设备上运行，不能视作通过。连接真机后执行 connectedDebugAndroidTest。
3. 微信与短信平台已申请并填入服务器 .env，Jenkins #19 已加载；Android 已同步用户确认的包名和 AppID。未发送真实短信，未完成微信 SDK 真机授权，因此供应商凭据有效性和实际权限仍需真实联调。
4. 微信 SDK 6.8.40 的发布字节码在 R8 中产生 stack map table 警告，release 构建仍成功。没有关闭压缩或忽略该警告；正式启用微信前必须对实际签名的 release 包做真机授权验收。
5. 正式签名和发布商店未配置。本次交付不等于已经具备产品上架条件。

## 后续运行命令

本次配置接入追加验证：

- Jenkins #19 成功，容器 healthy；全部短信/微信配置与服务器文件匹配。
- 受控游客创建 201 → 微信挑战 200（AppID/scope 正确）→ 退出 200。
- APK 实际包名经 aapt 核验为 cn.fuyaoshouhu.app；apksigner 验证通过，签名 MD5 与用户登记一致。
- 新配置 debug 构建、25 项离线单元测试通过；真实 smoke 默认跳过；lint 0 errors。

    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
    .\gradlew.bat :app:connectedDebugAndroidTest

默认 LiveApiSmokeTest 跳过，需要显式 HYPERF_LIVE_TEST=true 才访问测试服务器。其余单元测试均为受控传输替身，不冒充真实供应商验收。
