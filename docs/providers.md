# 真实短信与微信接入

已填写供应商配置。2026-10-27 经 Jenkins #19 重新部署；2026-10-29 再次只读核对，两项开关均为 true，8 个相关字段与运行容器一致，秘密仅检查是否存在及是否匹配，未输出原值。平台权限是否可用需真实登录验证，不能由配置存在推断。最新结果见[当前状态](current-state.md)。客户端代码接入官方微信 SDK 和 Hyperf 短信接口，没有模拟身份或万能验证码。

当前移动应用 AppID：wx57879d4692d97d56；用户确认的包名：cn.fuyaoshouhu.app；本机 debug 签名 MD5：223fbaaba46bea0e5ef02ee43444dc90。AppID 是公开客户端配置，AppSecret 仅在服务器。

已验证游客创建、微信挑战（AppID/scope 匹配）与退出接口。此检查不调用供应商授权交换、不发送短信，因此不能代替真实短信权限/模板或微信真机授权验收。

用户真机反馈微信提示“此公众号并没有这些 scope 的权限，错误码 10005”。当前请求 scope 为 `snsapi_userinfo`；尚未核实该 AppID 的移动应用类型、审核及登录权限，不能仅凭弹窗“公众号”字样判定 AppID 类型，也不能把修改 scope 当作解决方案。真实短信发送、微信成功授权和账号绑定/合并仍待完成验收。

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

以上是新环境的安全配置示例，不代表现有测试环境的实际开关状态，不能把占位文字作为真实值。短信使用独立 RAM 身份和发送权限，不复用 DNS 证书任务的凭据。全部配置核对后再启用对应开关并受控联调。

本项目测试部署使用 Docker `--env-file` 注入环境变量。修改服务器文件后，应通过 Jenkins 重新部署以重建应用容器；仅保存文件或重启原容器不会重新加载环境文件。供应商配置无需写入 APK。

## 客户端微信

product.properties 设置对应 WECHAT_APP_ID、APPLICATION_ID 和 WECHAT_ENABLED=true。运行 gradlew.bat :app:signingReport 核对实际签名 MD5，按开放平台格式去掉冒号、转小写。

当前未实现 test/prod 产品变体，debug 与 release 共用 product.properties 的包名、AppID 和接口地址，签名可不同。测试/生产分离仅是讨论过的后续方案，不是现有能力；实际 APK 身份必须与对应平台登记匹配。后台返回 challenge 的 AppID 必须与客户端一致；SDK 回调校验 state 和有效期后才交换一次性 code，取消不提交登录。

## 联调

配置好一个供应商再逐项测试登录、绑定、游客合并、身份冲突、取消及超时恢复。

真实短信可能计费，需要受控测试手机号和收到的验证码，自动测试不会发送。微信需要安装微信的真机及已审核移动应用权限。
