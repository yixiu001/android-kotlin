# 接口映射

前缀 /api/v1，健康检查为根 /。权威协议是[后端账号 API](https://github.com/yixiu001/hyperf-swow/blob/main/docs/api/android-account.md)。`product.properties` 的 `API_BASE_URL` 只填 HTTPS origin（如 `https://hyperf.tongyiwl.bond`），前缀由 `ApiClient` 添加；配置里带 `/api/v1` 会被 `Protocol.origin` 拒绝。当前验收边界见[当前状态](current-state.md)。

| 方法和路径 | AccountEngine | 用户入口 |
| --- | --- | --- |
| POST /auth/guest | start，退出后内部创建 | 首次自动游客 |
| GET /me | me | 我的、更新账号信息 |
| POST /auth/sms/challenges | requestSms | 获取验证码 |
| POST /auth/login/phone | submitPhone，游客 | 验证并登录 |
| POST /me/identities/phone | submitPhone，正式账号 | 确认绑定 |
| POST /auth/wechat/challenges | requestWechat | 微信授权前 |
| POST /auth/login/wechat | submitWechat，游客 | SDK 回调 |
| POST /me/identities/wechat | submitWechat，正式账号 | SDK 回调 |
| POST /auth/refresh | refresh，过期内部触发 | 自动刷新/debug 手动刷新 |
| POST /auth/logout | logout | 确认退出当前账号 |
| POST /me/profile | updateProfile | 我的 → 编辑昵称和头像 |
| POST /me/delete/phone | requestSms(deletion=true) + submitPhone | 注销说明 → 手机号验证 → 验证并立即注销 |
| POST /me/delete/wechat | requestWechat(deletion=true) + submitWechat | 注销说明 → 验证微信并立即注销 |
| GET / | health | debug 健康检查 |

POST 发送前保存原 origin/path/body/access/key；key 是 SecureRandom 32 字节无 padding base64url（43 字符）。guest、refresh 不带 Authorization；退出用 refresh_token body，避免混传不同版本凭证。

成功认证结果和 pending 清空在同一原子持久化提交。未知网络结果保持原请求，不自动换 key。只有 ACCESS_TOKEN_EXPIRED 触发最多一次刷新，其他 401 不递归重试。修改参数意味着新动作，不能用未确认请求的 key。

中文错误根据稳定业务码映射，不展示供应商详情。request_id 用于后台关联。正式账号冲突由服务端裁决，客户端不强行合并。

资料编辑只允许昵称和 6 个内置头像；不上传图片、不包含 OSS 配置。微信授权前可选择补充空昵称/头像；选择随 challenge 持久化，提交 use_wechat_profile 布尔值。头像链接仅允许受控微信 HTTPS 域名，限界下载、禁重定向、无磁盘缓存，失败显示默认头像。内置头像客户端本地绘制。

注销用途为 delete，与登录/绑定严格分开。注销未确认时隐藏资料并保留原请求，恢复成功后清理凭证/缓存并创建新游客。会话无效或恢复期已过不假定注销成功，由用户确认重新开始。成功结果含 registration_available_at；被冻结身份错误 IDENTITY_REGISTRATION_BLOCKED 的 available_at 在中文提示中显示为本机时区时间。
