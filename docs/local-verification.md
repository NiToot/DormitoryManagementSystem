# 本地验证记录

日期：2026-10-30，Asia/Shanghai。原始提交：`763d329d92f7aaaaa95e4ee3d0a3010bab3f15a8`，当前分支 `master`。
以下各阶段记录的是当时尚未提交的本地验收快照；后续 GitHub 交付状态以仓库提交历史为准。

## 环境

- Windows，PowerShell 7.6.5。
- Eclipse Temurin JDK 11.0.32.1+1，下载自 Adoptium 返回的官方发布地址并核对 SHA-256，位于被忽略的 `.runtime/tools/`。
- Maven 3.6.2；本轮依赖缓存位于 `.runtime/maven-repository/`。
- Node 22.23.2、npm 10.9.8。
- MySQL 8.0.25，使用已有程序另启独立实例，仅监听 `127.0.0.1:13306`。
- 数据目录 `.runtime/mysql-data/`，专用库 `dormitory_demo`，应用账号仅获该库的 SELECT/INSERT/UPDATE/DELETE 权限。
- 原有 `3306` MySQL 服务与数据库未修改。

## 第一阶段通过项

1. `database/init.sql` 创建 9 张表；重复导入后仍为 3 名学生、5 个房间和 2 个入住床位，不重复插入种子数据。
2. `npm install` 生成锁文件，随后实际执行 `npm ci --ignore-scripts --no-audit --no-fund` 从锁文件干净重装成功。
3. `npm run build` 成功，生成 `vue/dist/`；构建有 2 条体积告警，未视为性能优化完成。
4. Java 11 下 Maven `package` 和最终 `clean package` 均成功；干净构建的 5 项集成测试通过，失败 0、错误 0、跳过 0。
5. 可执行 JAR 实际启动成功，日志显示 Tomcat 在 9090 监听及 `Started SpringbootApplication`。
6. `scripts/smoke-test.ps1` 通过：三角色登录、会话恢复、首页统计、管理分页、学生宿舍、错误密码、退出登录、前端入口与代理 Cookie 会话。
7. 浏览器实际操作：管理员登录及首页、退出、学生登录及“我的宿舍”；1001 房间显示已住 2 人，前两床为 20230001、20230002。检查时浏览器 error 日志为空。

截图和机器运行日志位于被忽略的 `.runtime/`：`admin-home.png`、`student-room.png`、`backend-build.log`、`frontend-build.log`、`frontend-ci.log`、`smoke-test.log`。

## 配置与本机复用

本次数据库随机密码保存在被忽略的 `.runtime/local-env.json`，未写入 README 或版本控制。重新启动后端时可先在仓库根目录加载它：

```powershell
$localConfig = Get-Content .runtime/local-env.json -Raw | ConvertFrom-Json
$env:DB_USERNAME = $localConfig.DB_USERNAME
$env:DB_PASSWORD = $localConfig.DB_PASSWORD
$env:DB_PORT = $localConfig.DB_PORT
```

随后按 README 使用 Java 11 启动 JAR。也可从后端目录以 `--spring.config.additional-location=file:../.runtime/application-local.properties` 显式加载本轮生成的外置私有配置。配置没有放入 src/main/resources，不会进入打包产物。该私有 JSON、配置和下载工具不随 Git 分发，其他电脑按 README 准备独立环境。

## 构建过程中解决的问题

- 原依赖范围解析到新版 Element Plus，而 Webpack 4 无法解析其中的现代语法；固定 Vue/Element Plus 等原年代配套版本并补充锁文件后，构建通过。
- Node 22 与旧 Webpack 的 OpenSSL 兼容问题通过 npm 脚本中的 legacy provider 参数处理。
- 部分 Maven 依赖下载遇到 TLS 握手中断。仅对失败包通过 HTTPS 重取并核对 Central 发布的 SHA-1，再继续正常 Maven 构建；未关闭证书验证。
- HTTP 冒烟访问 SPA 深链接需要 `Accept: text/html`，与浏览器请求一致；否则开发服务器可返回 404。
- Windows 运行中的 JAR 会被占用，干净构建前先停止本次 Java 服务。

## 第一阶段当时尚未覆盖

没有实施后端认证授权重构、密码哈希与脱敏、调宿事务/并发、头像共享状态和完整富文本安全整改。未验证天气第三方服务、真实数据迁移、所有写入业务、生产部署、完整依赖漏洞扫描或性能指标。

本次 SQL 是按当前 Java 实体及查询重建的演示模型，不能替代原始数据库 DDL。当前四床位模型及原业务限制继续保留。测试通过不代表生产安全验收通过。

## 第二阶段验证（2026-10-30）

- 保留第一阶段的全部工作区改动，分支仍为 master，HEAD 未变，未暂存、提交或推送。
- 先运行 4 项越权/密码响应反例，旧实现均失败（匿名查询、学生读取宿管、学生读取他人宿舍、密码泄露），随后接入统一安全链。
- 已对专用 13306 的 dormitory_demo 执行 002-security.sql，显式运行旧密码迁移；演示登录密码仍是 Demo123456，库中改为 BCrypt，不保留明文登录回退。迁移再次运行不重复哈希。
- 最终 Java 11 `clean package` 成功，22 项测试全部通过：原 5 项启动/基本功能，加 17 项安全集成测试。覆盖 CSRF、匿名和跨角色拒绝、同名学生隔离、跨楼读写及床位字段白名单、个人资料越权、账号创建哈希、密码变更和旧会话失效、账号删除后失效、Session ID 轮换、POST 退出、头像跨用户隔离、迁移幂等。
- 新建临时库 dormitory_phase2_initcheck，连续导入当前 init.sql 两次，确认 9 张表、3 名学生的 60 字符 BCrypt、报修归属学号与访客楼宇正确；该临时库已清理。事务测试的 9999 开头学生没有残留。
- 前端生产构建通过，仍有 2 条原有体积告警。登录不再缓存密码；刷新通过 /auth/me 恢复，旧版浏览器资料缓存会主动清除。
- 新 JAR 以正常模式启动（未启用迁移开关），本机 HTTP 冒烟通过：三角色、Cookie 会话、CSRF、角色/楼宇隔离、错误密码、退出和前端代理。
- 浏览器实际验证：学生登录、我的宿舍同住者显示“已入住”、个人资料与独立密码表单、刷新恢复会话、退出；宿管只看到 1001/1002 两个本楼房间；管理员可见 3 名学生，普通编辑没有密码字段。密码变更的写入及会话撤销由自动化测试验证，浏览器只检查表单。
- 本轮浏览器检查时 error 日志为空；部分既有 Element Plus 组件仍有警告，未将其视为前端全面整改完成。

第二阶段证据位于忽略目录 .runtime：phase2-red.log、phase2-package.log、phase2-tests.log、phase2-frontend-build.log、phase2-backend.log、phase2-smoke.log、phase2-password-dialog.png、phase2-manager-rooms.png。截图不含密码。服务保持运行，入口为 http://127.0.0.1:8080/Login。

## 第二阶段当时剩余边界

调宿/床位分配的事务和并发保护、富文本清洗、登录限流与审计、依赖漏洞升级、真实数据迁移和生产部署仍未验收。头像归属已修复，旧共享头像需重新上传，文件生命周期与更完整上传防护后续处理。新增安全基线不代表生产安全审计通过。

## 第三阶段验证（2026-10-30）

- 延续原 master 工作区，未暂存、提交、推送或切换分支。旧后端停止后，仅对本机 13306 的 dormitory_demo 执行 003-accommodation.sql；未修改原有 3306 实例。
- 先运行 6 项反例，旧实现全部失败，分别对应跨房重复入住、同房重复床位、伪造人数、已住房间删除、过期版本覆盖、重复待处理申请。
- 当前完整测试 44 项，失败 0、错误 0、跳过 0：22 项原启动/安全回归，11 项住宿规则，11 项独立事务及并发/故障测试。干净 clean package 成功；最后新增的锁等待测试在完整 Maven test 中验证。
- 真实 MySQL 并发覆盖：同学生多房、不同学生抢同床、同申请重复审批、两申请抢同床、学生编辑与审批竞争、重复申请。每组断言只有一个成功，失败返回 409，再独立查询床位、人数、版本和状态。另用独立事务持有数据库锁，确认 API 写入等待释放后才完成。
- 故障注入在实际写入原房间和目标房间之后抛异常，确认两房床位/人数/版本、申请状态全部回滚。同房换床、目标容量、原床位变化、历史冲突启动检查、账号/楼宇删除旁路也有测试。
- 新建 dormitory_phase3_initcheck 连续导入当前 init.sql 两次，确认 10 张表与协调锁种子行；003 SQL 再执行成功。临时库已删除，事务/并发测试学生没有残留。
- 最新前端生产构建成功，仍有 2 条原有体积告警。房间/申请更新携带版本，删除通过 query 携带版本；冲突会提示、关闭过期表单并刷新。人数表单为只读，新增默认 0，服务端按床位重算。
- 新 JAR 正常启动并完成数据预检查，HTTP 冒烟通过三角色登录、CSRF、Cookie 会话、权限隔离、统计、主要分页和前端代理。
- 浏览器用新建的虚构空房间 9699 打开两个版本相同的编辑表单。第一个把楼层保存为 2，第二个尝试保存为 3 时提示“记录已变化或缺少版本，请刷新后重试”并关闭表单刷新。数据库确认楼层为 2、version=1；该临时房间已条件校验后清理，仍为 5 个原演示房间、2 个入住床位。测试账号已退出。

第三阶段证据位于忽略目录 .runtime：phase3-red.log、phase3-tests.log、phase3-package.log、phase3-frontend-build.log、phase3-backend.log、phase3-smoke.log、phase3-stale-room.png。服务保持运行，入口 http://127.0.0.1:8080/Login。

## 当前剩余边界

当前住宿写入以数据库单行锁串行执行，未做生产吞吐/压力测试；直接 SQL、旧版本应用等外部写入不遵守应用锁协议，不具备跨四床位字段的数据库唯一约束。后续若扩大并发规模，应规范化入住关系并改为细粒度锁和唯一约束。上传资源生命周期、登录限流、完整审计、依赖安全升级、真实数据迁移及生产部署仍未验收。公告和报修的富文本处理见下面的验证记录。

## 富文本处理与文档调整（2026-11-10）

- 在原实现运行 8 项接口用例，7 项失败：公告及报修的新增、修改和旧数据查询会保留危险 HTML，超长内容也未被拒绝。未传内容的报修状态更新正常。
- 增加共享 HTML 白名单，新增与修改时清洗，读取旧记录时再清洗。允许常用文本排版、表格和 HTTP(S) 链接，移除图片、嵌入内容、脚本、事件属性和内联样式；写入长度限制为 20,000 个 UTF-16 字符。
- JDK 11、Maven 3.6.2、MySQL 8.0.25 下执行 `clean package`：63 项测试全部通过，失败、错误、跳过均为 0。包含原有 44 项回归、8 项富文本接口测试及 11 项清洗规则测试。
- 数据库连接限定本机 13306 的 `dormitory_demo`。新增接口测试使用事务回滚，旧记录读取不会修改原内容，未增加或执行数据库迁移。
- `npm.cmd run build` 成功；仍有资源体积和入口体积两类原有告警。本次未进行页面性能优化或浏览器交互验收。
- README 按功能、截图、启动、账号、测试和常见问题重新整理，详细设计和测试记录保留在 `docs`。文档链接、代码块和 `git diff --check` 检查通过。

日志保存在忽略目录 `.runtime`：`phase4-richtext-red.log`、`phase4-richtext-green.log`、`phase4-package.log`、`phase4-frontend-build.log`。未将本机账号配置或日志加入版本控制。
