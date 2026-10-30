# 第二阶段：认证、密码与个人数据隔离

## 权限约定

- 管理员管理全部记录，账号创建/删除、宿管、楼宇结构和公告写入仅管理员开放。
- 宿管只能管理本楼已入住学生、房间、报修、调宿及访客；不能通过请求体改变楼宇归属。跨楼调宿必须由管理员处理。
- 学生只能修改本人的允许资料字段、密码和头像，查看本人宿舍/报修/调宿，提交本人申请及编辑本人未处理调宿。不能提交审批结果或修改他人记录。
- 报修按 `student_username` 归属，不按姓名；归属不明的历史报修、无楼宇的历史访客只允许管理员访问。
- 学生首页可见非实名汇总与公告；床位可用性查询仅返回是否可用，不返回住户。个人宿舍中其他同住者显示“已入住”。
- 安全链默认拒绝未列出的接口。前端菜单、路由限制只辅助交互，后端承担权限校验。

## 会话与接口

登录路径沿用 `POST /admin/login`、`/dormManager/login`、`/stu/login`，请求含 username/password。身份由服务端登录路径选择，前端缓存不参与授权。

1. `GET /auth/csrf` 取得 `data.token`、`data.headerName`，保存响应 Cookie。
2. 所有 POST/PUT/DELETE 请求，包括登录与退出，携带 Cookie 和对应 CSRF Header。
3. 登录成功创建新会话，旧 Session ID 失效；`GET /auth/me` 返回不含密码的当前用户。
4. `PUT /auth/profile` 仅接受 name、age、gender、phoneNum、email，用户名、身份、楼宇、头像和密码不能由此修改。
5. `POST /auth/password` 接收 currentPassword/newPassword。原密码正确后写 BCrypt 哈希并递增 auth_version，退出当前会话；其他旧会话的下一次请求返回 401。
6. `POST /auth/logout` 清理服务端会话与 Cookie。旧 `GET /main/signOut` 返回 405，不再执行退出。
7. `POST /files/avatar` 接收 multipart file，校验实际图片且不超过 2MB、边长 4096，重新编码为随机 PNG 并绑定当前用户；读取头像也检查归属。旧两步上传/绑定路径不再开放。

密码长度 8–64 字符，UTF-8 不超过 72 字节，BCrypt cost=12。管理接口创建学生/宿管时必须提供初始密码；普通编辑不能重设密码。账号删除或密码认证版本变化后旧会话失效。会话默认 30 分钟，JSESSIONID 为 HttpOnly/SameSite=Lax；本机 HTTP 环境不设置 Secure，正式部署需要 HTTPS 和 Secure Cookie。

允许的前端来源默认是 http://127.0.0.1:8080 与 http://localhost:8080，通过逗号分隔的 `app.allowed-origins` 配置。不得配合凭据使用任意来源。

## 第一阶段演示库升级

以下步骤只针对本机专用 `dormitory_demo`；其他数据库需要单独审查、备份和迁移设计。先停止旧后端，避免旧实现继续写入明文。使用数据库管理员导入 SQL，应用账号仍只需业务 DML 权限。

在仓库根目录执行（MySQL 客户端会提示输入管理员密码）：

```powershell
mysql --host=127.0.0.1 --port=13306 --user=root --password --execute="SOURCE database/002-security.sql"
```

该 SQL 幂等增加三个账号表的 auth_version、报修的 student_username 和访客的 dormbuild_id，不删除记录。报修仅对能唯一匹配学生姓名的旧记录回填学号；同名情况保留 NULL，不能猜测归属。无楼宇的历史访客需管理员核实后补填有效楼宇。

完成 SQL、构建新 JAR 后，从后端目录使用自己的外置配置启动一次：

```powershell
java -jar target/Dormitory_bussiness-1.0.0.jar --spring.config.additional-location=file:../.runtime/application-local.properties --app.security.migrate-legacy-passwords=true
```

等待 `Started SpringbootApplication` 之后的启动 runner 完成且应用就绪，验证登录及数据库中密码已为 `$2...` 格式。迁移对未哈希密码编码并增加认证版本，已有合法 BCrypt 不再编码；空密码或超过 72 字节会使启动失败，需离线核实和重置后重试。迁移在一个事务内执行，不打印密码。旧版本数据库备份含明文，应限制访问。

随后停止该进程，去掉迁移参数，按 README 正常启动。不要把开关长期保存在共享配置。登录路径没有明文兼容回退。

全新数据库直接导入 init.sql 即可，不需要启用迁移开关。init.sql 保留已有数据，不负责 ALTER 旧表。

## 验证边界

后端测试包含真实专用 MySQL 的事务回滚与 MockMvc 完整过滤链；HTTP 冒烟另外验证实际 JAR、Cookie、CSRF 和前端代理。不能据此宣称调宿并发、富文本安全、限流、所有浏览器或生产部署已经验收；这些仍属于后续阶段。
