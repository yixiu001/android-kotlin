联调工具使用独立 HTTPS POST `/test-tools/v1/account-reset/prepare`（普通 access Bearer，JSON `{}`，顶层预览+grant）、`/execute`（返回的 15 分钟 grant，原 user_id/confirmation）。均经 AccountEngine 调试桥接串行化并校验原账号/origin/generation；不进入普通 API 日志。正常 token 不能执行重置，客户端不接受指定任意目标。成功自动调用 Android 本地完整清理。见[联调工具](test-tools.md)。

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
| POST /me/profile/wechat | confirmWechatProfile | 登录后预览 / 编辑资料 → 使用微信资料 → 选择并确认 |
| POST /me/delete/phone | requestSms(deletion=true) + submitPhone | 注销说明 → 手机号验证 → 验证并立即注销 |
| POST /me/delete/wechat | requestWechat(deletion=true) + submitWechat | 注销说明 → 验证微信并立即注销 |
| GET / | health | debug 健康检查 |

POST 发送前保存原 origin/path/body/access/key；key 是 SecureRandom 32 字节无 padding base64url（43 字符）。guest、refresh 不带 Authorization；退出用 refresh_token body，避免混传不同版本凭证。

成功认证结果和 pending 清空在同一原子持久化提交。未知网络结果保持原请求，不自动换 key。只有 ACCESS_TOKEN_EXPIRED 触发最多一次刷新，其他 401 不递归重试。修改参数意味着新动作，不能用未确认请求的 key。

中文错误根据稳定业务码映射，不展示供应商详情。request_id 用于后台关联。正式账号冲突由服务端裁决，客户端不强行合并。

手动资料编辑允许昵称和 6 个内置头像；不上传图片、不包含 OSS 配置。普通微信 challenge 和 login/bind exchange 均发送 use_wechat_profile=true（注销 challenge 为 false；最终 /me/delete/wechat 按既有严格字段省略该标志，不请求候选），只获取可选候选，不自动写资料。最终会话和用户先落盘，再针对最终用户缺失字段展示预览。正式账号从编辑资料重新发起 bind_wechat（已绑定也重新授权），预览后显式替换独立勾选字段；游客入口引导登录并说明可能切换账号。头像链接仅允许受控微信 HTTPS 域名，限界下载、禁重定向、无磁盘缓存，失败显示默认头像。内置头像客户端本地绘制。

注销用途为 delete，与登录/绑定严格分开。注销未确认时隐藏资料并保留原请求，恢复成功后清理凭证/缓存并创建新游客。会话无效或恢复期已过不假定注销成功，由用户确认重新开始。成功结果含 registration_available_at；被冻结身份错误 IDENTITY_REGISTRATION_BLOCKED 的 available_at 在中文提示中显示为本机时区时间。


微信确认严格 JSON `{preview_id,use_nickname,use_avatar,overwrite}`，Bearer + 原子保存的 Idempotency-Key，至少选择一项。自动预览 overwrite=false；编辑入口 overwrite=true。候选只在加密 Vault 中保存，绑定 origin/user/session/generation，TTL 600 秒；跳过落盘后 /me 不重新引入旧候选，刷新同一会话保留，切换/退出清除。手动意图保存在 Pending/Challenge 的非 wire 字段，重试不会添加 API 未定义字段。确认网络未知、429 或 5xx 保留原请求和候选；成功及 WECHAT_PROFILE_EXPIRED/INVALID/CHANGED 清除候选。过期不影响登录，编辑资料可重新授权；确认不再次交换 OAuth code。日志隐藏 preview_id、候选、昵称和头像 URL。

本流程尚未部署/推送；SQL004 与配套后端发布必须先于新版 APK 联调。原有一键调试重置及完整本地清理保持不变。


ACCESS_TOKEN_EXPIRED 明确在原操作应用前拒绝时，刷新 Pending 携带加密非 wire continuation（原 path/body/manualProfile 与 origin/user/session/generation）。恢复先重放原刷新，成功后原子保存新 token + 已授权操作的新 Pending（新 key、原 body、新 access，禁止再次自动刷新）；若续接网络未知则恢复该新 Pending，不重新刷新或获取/交换 OAuth。恢复后的登录 /me 和注销新游客继续按原操作执行；跨账号/会话/环境/代数停止续接。此元数据不进入 API JSON 或日志。

资料确认回执 AUTH_RESULT_EXPIRED 是特例：清除不可恢复的确认 Pending/候选、保留当前会话，提示通过既有 GET /me 更新资料核对实际状态，不自动重写。真实会话失效及其他认证操作回执过期策略不变。
