# Personal center visual QA · 2026-10-29

final result: passed

此结论仅适用于布局与导航验收；安装期间本地会话丢失的事件及修复见 [验收记录](docs/verification-2026-10-29-profile-layout.md)，不可据此认定原会话已保留。

## Evidence and comparison

- Approved source: `C:/Users/Admin/.codex/generated_images/01a15302-376e-7d41-86ea-41538d066bd0/exec-d24d4ece-5c09-4c1e-981d-12c8bf8301a2.png`，852 × 1846。
- Native implementation: `docs/images/profile-center.png`，1080 × 2238，P30 Compose 内容截图，设备 density 3；不含系统状态栏和系统导航栏。
- Combined comparison: `docs/images/profile-comparison.png`，两图按同一 430px 宽度等比缩放；较短的原生内容区因系统栏预留而高度不同，不拉伸截图。
- 比较状态：浅色、我的、正式账号、虚拟昵称“一休”、默认青绿头像、微信已绑定。真实手机目前为游客，不能把虚拟截图当成真实用户资料。

## Iteration

第一版 P2：资料头部下方留白和列表行高偏大，支持服务分组被明显下推。已将头部底部 padding 从 20dp 收紧至 4dp，分组间距 24dp→20dp，行 minHeight 56dp→52dp/vertical padding 16dp→12dp；帮助与协议使用更贴近参考的气泡/文档图标。重新编译、重新捕获并比较，已消除主要密度偏差。全图中文、图标和状态可读，无需额外放大区域才能判断。

## Fidelity surfaces

- 字体：系统中文字体，标题 24sp、昵称 22sp、菜单 16sp、说明 14sp，层级清晰；长昵称省略，完整内容可在编辑框查看。系统字体度量差异保留。
- 间距：20dp 页面边距、18dp 分组圆角、52dp 以上触控行高，一级菜单完整可见，二级可滚动且有返回。原生安全区与静态图高度不同属预期。
- 配色：浅灰 `#F6F8FA`、白色分组、青绿 `#16756A`，文字 `#202B32`；无额外营销卡片或统计区。
- 资产：沿用真实账号已有头像组件，未更改用户数据；标准 Material Icons 代替参考图通用线形符号，没有创建自定义假品牌图标。原生 Material 选中导航胶囊位于图标背景，与参考图标签背景不同，作为 P3 平台组件差异保留。
- 文案：功能名称与已批准图一致，版本来自 BuildConfig，绑定状态来自 AccountView。不存在重新微信授权入口；公开链接为空时说明不可用，没有伪造隐私文件或在线客服。

## Interaction verification

7 项隔离 Compose UI 测试通过。账号操作不直接放在一级页；登录/绑定、退出、注销与调试入口均保留原保护；二级返回及 Activity 状态恢复通过。无未解决 P0/P1/P2 布局问题。
