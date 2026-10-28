# 当前状态

更新：2026-10-29（北京时间）。后端协议见[账号 API](https://github.com/yixiu001/hyperf-swow/blob/main/docs/api/android-account.md)。

## 已实现

保留自动游客、短信/微信登录绑定、老账号合并、双身份和独立会话。新增“我的 → 编辑昵称和头像”“注销账号”入口：昵称可改，头像可选 6 个内置项；首次微信绑定可选择以微信资料补空字段，已有资料不覆盖、合并目标不覆盖。

微信头像使用受控 HTTPS 链接，限界读取、无磁盘缓存，失败回退内置头像；没有图片上传或 OSS。注销先说明即时生效/30 天身份限制，再验证当前绑定的手机号或微信；成功后清除本地凭据和私有缓存并进入新游客。未知结果隐藏资料，原请求持久化恢复。

绑定后不可更换或解绑。新后端共 13 个账号接口。Android 功能提交 `132ecee` 已推送 main，debug APK 已构建；后端 `9db609e` 已完成测试库 SQL 002 升级及 Jenkins #20 部署，HTTPS 资料/会话冒烟通过，Redis 配置保持不变。

## 配置（保持不变）

| 项目 | 值 |
| --- | --- |
| 应用名 / applicationId | Android 基础工程 / cn.fuyaoshouhu.app |
| namespace | cn.yixiu.starter |
| API_BASE_URL | https://hyperf.tongyiwl.bond（仅 origin） |
| 微信 AppID | wx57879d4692d97d56，AppSecret 仅服务端 |
| 短信 / 微信入口 | 均已开启 |
| 本机 debug 签名 MD5 | 223fbaaba46bea0e5ef02ee43444dc90 |
| 环境配置 | 单份 product.properties；尚无 test/prod 变体 |

Vault schema=1；新加的微信资料选择缺省 false，旧存储不需要清空。新头像/昵称从服务端 profile 中读取，不另建本地个人资料来源。

## 验证

单元测试 **30 通过 / 1 个真实 smoke 跳过**，debug 构建成功，lint **0 错误 / 9 个原有依赖提示**。详见[本轮验收及 APK 摘要](verification-2026-10-29-profile-deletion.md)。原阶段用户已明确反馈测试通过，旧文档的微信 10005 不再作为当前已知阻塞。

本轮尚未运行连接设备的 UI、微信外部授权、短信及新增注销真机验收；未向线上发送真实注销请求。后端已升级，可安装新版 APK 用专用测试账号联调。生产签名发布、隐私政策、测试/生产配置拆分、设备管理不在本期范围。

产物：`app/build/outputs/apk/debug/app-debug.apk`。操作入口见[文档导航](README.md)和[接口映射](api-map.md)。
