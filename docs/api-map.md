# 接口映射

前缀 /api/v1，健康检查为根 /。权威协议是 hyperf-swow/docs/api/android-account.md。

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
| GET / | health | debug 健康检查 |

POST 发送前保存原 origin/path/body/access/key；key 是 SecureRandom 32 字节无 padding base64url（43 字符）。guest、refresh 不带 Authorization；退出用 refresh_token body，避免混传不同版本凭证。

成功认证结果和 pending 清空在同一原子持久化提交。未知网络结果保持原请求，不自动换 key。只有 ACCESS_TOKEN_EXPIRED 触发最多一次刷新，其他 401 不递归重试。修改参数意味着新动作，不能用未确认请求的 key。

中文错误根据稳定业务码映射，不展示供应商详情。request_id 用于后台关联。正式账号冲突由服务端裁决，客户端不强行合并。
