# 复制为新产品

## 必改项

1. 复制源码到独立目录，不复制 .git、.gradle、.idea、build、local.properties、签名文件或日志。创建自己的 Git 仓库。
2. 修改 product.properties 中名称、APPLICATION_ID、API_BASE_URL、微信 AppID/开关、版本号、短信入口开关。
3. 修改 settings.gradle.kts 的 rootProject.name，调整工程展示名称。
4. 替换 app/src/main/res/drawable/ic_launcher.xml 图标、ui/StarterScreen.kt 品牌色及首页业务区域；正式产品建议配 adaptive icon。
5. 配置本机 SDK/JDK 后重新 Sync。

namespace 可以保持 cn.yixiu.starter，安装身份由 APPLICATION_ID 区分，无需全局替换 Kotlin 包名。微信回调的 Manifest 别名随 applicationId 自动变化。

## 后端与身份

- 后端也复制独立工程：独立 MySQL 库、账号加密/HMAC 密钥、Redis 前缀和最小 ACL。Android 不保存这些值。
- 新产品不能继续用模板测试后端，否则会共用账号数据。只改 Android 包名不会隔离后端。
- 微信移动应用需登记新包名和实际 APK 的 MD5，AppID 与后端一致。
- 短信模板/签名/RAM 密钥在服务器配置，Android 只调用 Hyperf。
- 后端未提供解绑、换绑、注销等功能，不要添加假入口。

## 签名与升级

- 发布密钥保存在工程外并备份，用签名向导或不入库的 keystore.properties。
- 运行 gradlew.bat :app:signingReport 查看 debug 公开指纹；不分享密钥文件和密码。
- debug 未加 applicationIdSuffix。和 release 同包名不同签名时不能直接覆盖安装，卸载会丢失未绑定游客。
- Vault schema=1，修改结构要写升级迁移测试，解密失败不能自动清空账号。

## 业务扩展

- 从首页扩展业务页面；通过 Repository 协调网络与缓存，页面不管理令牌。
- 新增并发请求捕获 generation/tokenVersion，账号切换后丢弃旧响应。
- 业务私有缓存按账号分目录，接入 onAccountCleared，退出/升级/合并时清理。
- 后端业务数据合并需要注册业务合并器，基础账号模块不会自动合并任意业务表。
- 规模增长后再拆分模块，当前单 app 工程便于复制维护。

## 验收

- 首次游客、重启恢复、短信及微信登录/绑定、游客合并、正式账号冲突、退出后新游客。
- 多设备独立会话，退出一台不影响其他设备。
- 断网/响应丢失/原请求恢复/并发刷新/验证码过期/429/503。
- 微信真机验证包名签名、取消、未安装微信、AppID 不匹配、进程重建回调。
- release 无开发工具，后端为新产品域名，签名属于新产品，不包含服务端密钥。
- 单元测试、lint、release 编译和设备仪器测试通过。
