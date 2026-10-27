# 真实短信与微信接入

用户已完成平台申请和服务器配置。2026-10-27 经 Jenkins #19 重新部署，已核对 8 个相关字段与运行容器一致；字段值及密钥未输出。客户端代码接入官方微信 SDK 和 Hyperf 短信接口，没有模拟身份或万能验证码。

当前移动应用 AppID：wx57879d4692d97d56；用户确认的包名：cn.fuyaoshouhu.app；本机 debug 签名 MD5：223fbaaba46bea0e5ef02ee43444dc90。AppID 是公开客户端配置，AppSecret 仅在服务器。

已验证游客创建、微信挑战（AppID/scope 匹配）与退出接口。此检查不调用供应商授权交换、不发送短信，因此不能代替真实短信权限/模板或微信真机授权验收。

## 需要的非敏感信息

- 微信移动应用 AppID、开放平台登记 Android 包名、签名 MD5。
- 阿里云短信签名名称、验证码模板 Code，模板变量为 code。

AppSecret 和 AccessKey Secret 不发聊天，不写 Android 工程。

## 服务端填写

测试服务器文件 /opt/hyperf-swow/shared/.env。通过已有 1Panel 文件编辑器/终端填写，保留受限权限，不更改数据库、Redis 和现有账号密钥。

    ACCOUNT_SMS_ENABLED=false
    ACCOUNT_SMS_SIGN_NAME=已审核签名名称
    ACCOUNT_SMS_TEMPLATE_CODE=SMS_实际模板编号
    ACCOUNT_SMS_ACCESS_KEY_ID=短信专用RAM的AccessKeyId
    ACCOUNT_SMS_ACCESS_KEY_SECRET=短信专用RAM的AccessKeySecret
    ACCOUNT_WECHAT_ENABLED=false
    ACCOUNT_WECHAT_APP_ID=移动应用AppID
    ACCOUNT_WECHAT_APP_SECRET=移动应用AppSecret

以上是字段示例，不能把占位文字作为真实值。短信使用独立 RAM 身份和发送权限，不复用 DNS 证书任务的凭据。全部配置核对后再启用对应开关。

修改后需让运行中的应用读取新配置；是否重启或重建容器，取决于部署采用 .env 挂载还是 Docker 环境注入，不能假设保存文件就生效。

## 客户端微信

product.properties 设置对应 WECHAT_APP_ID、APPLICATION_ID 和 WECHAT_ENABLED=true。运行 gradlew.bat :app:signingReport 核对实际签名 MD5，按开放平台格式去掉冒号、转小写。

debug 与正式签名不同，分别核对登记要求。后台返回 challenge 的 AppID 必须与客户端一致；SDK 回调校验 state 和有效期后才交换一次性 code，取消不提交登录。

## 联调

配置好一个供应商再逐项测试登录、绑定、游客合并、身份冲突、取消及超时恢复。

真实短信可能计费，需要受控测试手机号和收到的验证码，自动测试不会发送。微信需要安装微信的真机及已审核移动应用权限。
