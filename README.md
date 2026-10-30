# DormitoryManagementSystem · 宿舍管理系统

基于 Spring Boot、MyBatis-Plus、Vue 3 和 Element Plus 的前后端分离宿舍管理演示项目，覆盖后勤管理员、宿管和学生三类使用场景。

**当前阶段：本地运行、认证隔离与住宿一致性基线。** 已补充数据库、环境、Session 认证、角色及数据权限、BCrypt 密码，以及住宿写入事务、并发协调和重复入住校验；仅用于本地学习和开发，后续继续完善富文本安全与依赖升级。

![管理员主页](doc/img/管理员主页.png)

> 截图来自原仓库，反映原有功能，不是本轮新增功能的验收截图。

## 功能与角色

- **后勤管理员**：学生与宿管、楼宇与房间、公告、报修、调宿和访客管理。
- **宿管**：本楼学生、房间、报修、调宿与访客管理；楼宇信息只读，账号创建/删除及楼宇结构调整由管理员处理。
- **学生**：我的宿舍、调宿申请、报修和个人信息。
- **首页**：学生/入住/报修/未满房间统计、入住分布图、公告、日历和天气组件。

后端统一检查登录、角色和记录归属。学生只访问本人资料、报修、调宿与宿舍；同住者学号显示为“已入住”。宿管按账号所属楼宇过滤查询并校验写入对象，管理员具有全局管理权限。未分配宿舍的学生与归属不明的历史记录由管理员处理。

## 技术与环境

- Java 11；Spring Boot 2.6.3；MyBatis-Plus 3.5.1；MySQL 8.0。
- Vue 3.2.31；Element Plus 2.0.1；Vue Router 4.0.13；Vuex 4.0.2；ECharts 5.3.0。
- Vue CLI 4.5.19；Node.js 22.23.2；npm 10.9.8；Maven 3.6.2。
- 完整前端依赖树由 `vue/package-lock.json` 固定，日常安装使用 `npm ci`。

Vue CLI 4 使用旧 Webpack，Node 22 下的启动和构建脚本显式启用 `--openssl-legacy-provider`。部分旧传递依赖会产生 engine/deprecated 警告；该兼容基线不等于依赖安全升级完成。不要随意放开 Element Plus/Vue 的版本范围。

## 目录

```text
database/init.sql           9 张业务表、1 张协调锁表及虚构演示数据
database/002-security.sql   第一阶段演示库的增量迁移
database/003-accommodation.sql 第二阶段演示库的住宿事务增量迁移
Dormitory_business/         Spring Boot 后端
  .mvn/settings.xml         可选的 Maven Central HTTPS 配置
  src/main/resources/       环境变量配置与私有配置示例
  src/test/                 演示库启动与接口集成测试
vue/                        Vue 前端、依赖锁文件、代理配置
scripts/smoke-test.ps1       本机 HTTP 冒烟检查
docs/local-verification.md  本轮本地验证结果与边界
docs/security-baseline.md   认证接口、权限及旧库升级步骤
docs/accommodation-consistency.md 住宿事务、并发、版本和迁移说明
doc/img/                    原有页面截图
```

## 快速启动（Windows PowerShell）

### 1. 准备环境

安装 Java 11、Maven、Node 22 和 MySQL 8.0，并确保相关命令可使用。`.java-version` 和 `vue/.nvmrc` 记录版本，不会自动安装工具。

```powershell
java -version
mvn -version
node -v
npm.cmd -v
mysql --version
```

以下命令从仓库根目录开始。端口约定：MySQL `13306`、后端 `9090`、前端 `8080`。MySQL 应使用单独的本机演示实例；如果你的专用实例使用其他端口，相应修改连接参数和 `DB_PORT`。

### 2. 初始化专用演示库

```powershell
mysql --host=127.0.0.1 --port=13306 --user=root --password --default-character-set=utf8mb4
```

进入 MySQL 客户端后执行：

```sql
SOURCE database/init.sql;
CREATE USER 'dormitory'@'127.0.0.1' IDENTIFIED BY '替换为你自己的本机数据库密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON dormitory_demo.* TO 'dormitory'@'127.0.0.1';
```

账号已存在时，跳过 `CREATE USER` 并确认密码和授权。不要使用示例占位密码。

`init.sql` 根据当前实体及查询重建，不是原作者数据库备份，也不是存量库迁移脚本。它使用 `CREATE TABLE IF NOT EXISTS` 和存在性判断的种子插入，不执行 `DROP`、`TRUNCATE` 或覆盖已有记录。它不会自动修复已有表结构或被修改过的演示数据。仅在专用演示实例执行。

首次导入创建 3 名学生、1 名宿管、1 名管理员、4 栋楼、5 个房间、2 个入住床位，以及各一条公告、报修、调宿和访客记录。

### 3. 配置并启动后端

新开一个 PowerShell 窗口，从仓库根目录执行：

```powershell
$env:JAVA_HOME = '你的 JDK 11 安装目录'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_PORT = '13306'
$env:DB_USERNAME = 'dormitory'
$env:DB_PASSWORD = '你设置的本机数据库密码'
Set-Location Dormitory_business
mvn -s .mvn/settings.xml clean package
java -jar target/Dormitory_bussiness-1.0.0.jar
```

`clean package` 执行集成测试，需要先导入演示库。打包成功后等待日志出现 `Started SpringbootApplication`。后端默认只监听 `127.0.0.1:9090`。

配置默认值：`DB_HOST=127.0.0.1`、`DB_PORT=13306`、`DB_NAME=dormitory_demo`、`SERVER_ADDRESS=127.0.0.1`、`SERVER_PORT=9090`。数据库用户名和密码必须显式提供；启动不会自动导入 SQL。

也可在仓库根目录创建 `.runtime`，把 `application-example.properties` 复制为 `.runtime/application-local.properties` 并填入数据库账号密码。从后端目录启动时增加 `--spring.config.additional-location=file:../.runtime/application-local.properties`。私有配置留在源码外，不会打入 JAR，整个 `.runtime` 已加入 Git 忽略。

`.mvn/settings.xml` 显式选择 HTTPS Central，不修改系统 Maven 设置；公司网络需认证镜像时使用自己的 settings 文件。

### 4. 启动前端

在另一个窗口，从仓库根目录执行：

```powershell
Set-Location vue
npm.cmd ci
npm.cmd run serve
```

访问 [http://127.0.0.1:8080/Login](http://127.0.0.1:8080/Login)。`/api` 由开发代理转发至 `http://127.0.0.1:9090`。

后端使用其他端口时，将 `vue/.env.example` 复制为 `.env.local` 并修改 `VUE_APP_API_TARGET`，然后重启前端。代理只在开发服务器生效；部署 `dist` 时需另配 `/api` 反向代理和 history 路由回退。

### 5. 演示账号

以下均为公开、虚构的本机演示账号，密码统一为 `Demo123456`：

- 后勤管理中心：`admin01`
- 宿舍管理员：`dorm01`
- 学生：`20230001`、`20230002`、`20230003`

学生 `20230001`、`20230002` 已入住 `1001` 房间，`20230003` 未分配宿舍。登录时选择对应身份，不要录入真实个人信息。

## 构建与验证

前端：在 `vue` 执行 `npm.cmd run build`，产物为 `vue/dist`。

后端：保持数据库环境变量有效，在 `Dormitory_business` 执行 `mvn -s .mvn/settings.xml test`。测试限定 `dormitory_demo`，包括启动、三角色登录、CSRF、匿名及跨角色拒绝、同名学生隔离、跨楼读写、个人资料白名单、BCrypt、修改密码后旧会话失效、头像归属和迁移幂等性。安全测试使用事务回滚虚构记录，头像测试文件自行清理；请勿对真实业务库运行测试。

住宿测试另覆盖重复入住、版本冲突、退宿计数与删除保护；并发测试使用两个独立会话和真实 MySQL 事务竞争，验证抢床、重复审批及故障回滚，并在结束后清理虚构数据。运行前先完成当前数据库增量迁移。

前后端启动后，从仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

该执行策略参数只对本次进程生效；也可在已允许脚本执行的 PowerShell 7 中直接运行 `./scripts/smoke-test.ps1`。

冒烟脚本限制目标为本机 HTTP 服务，检查三角色登录、CSRF、Session、角色与楼宇隔离、首页统计、主要列表、错误密码、前端入口和 API 代理。它不修改业务记录，但会创建及清理测试会话。

## 认证与数据库升级

全新演示库直接导入 `database/init.sql`；种子密码已经是 BCrypt 哈希，公开演示密码仍为 `Demo123456`。第一阶段已有数据库先停止后端、备份专用演示库，再按 [安全基线说明](docs/security-baseline.md) 导入 `database/002-security.sql`，并显式运行一次旧密码迁移。仅重新导入 `init.sql` 不会升级既有表。

前端通过 `/auth/me` 恢复服务端会话。登录、退出及其他写请求先获取 `/auth/csrf` 并携带 Cookie 和 `X-XSRF-TOKEN`。无会话返回 HTTP 401，无权限或 CSRF 不通过返回 403。个人资料修改和密码修改分开；修改密码需验证原密码，并使所有旧会话失效。响应和浏览器缓存均不保存密码或哈希。

第二阶段旧库继续升级时，停止旧后端并备份后导入 `database/003-accommodation.sql`，详见 [住宿一致性说明](docs/accommodation-consistency.md)。房间和调宿申请更新必须携带当前 version，过期页面返回 409 并重新加载；同一学生只允许一个床位和一个待处理调宿申请。审批中原床位、目标床位及申请状态在一个事务提交，失败全部回滚。历史冲突会阻止新后端启动，需人工核实。

## 常见问题

- **数据库连接失败**：核对 MySQL 端口、库名、授权和环境变量；进程启动不等于数据库可用。
- **Maven 下载失败**：检查网络和 settings；离线模式要求依赖已全部缓存。
- **Windows `clean` 无法删除 JAR**：先停止使用该 JAR 的后端进程，再执行干净构建；构建后重新启动服务。
- **`ERR_OSSL_EVP_UNSUPPORTED`**：使用仓库内的 npm 脚本，不要直接运行未带兼容参数的旧 Webpack。
- **依赖语法错误**：使用锁文件与 `npm ci`，不要直接升级界面库。
- **8080 被占用**：使用 `npm.cmd run serve -- --port 18080`，并在后端私有配置的 `app.allowed-origins` 中加入 `http://127.0.0.1:18080` 后重启；冒烟脚本支持 `-FrontendUrl http://127.0.0.1:18080`。
- **升级后登录失败或出现缺少字段**：先执行增量 SQL 和显式密码迁移；不再接受数据库中的明文密码直接登录。旧缓存可退出后重新登录，权限以服务端为准。
- **天气组件不显示**：它依赖第三方网络、服务和 key，核心业务启动不依赖天气组件。

## 已知限制与后续计划

住宿写入采用数据库单行锁串行事务，适合当前规模；四床位模型尚未规范化为入住关系表，直接执行外部 SQL 不受应用校验保护。富文本清洗、登录限流、完整操作审计、依赖安全升级及生产 HTTPS 部署尚未完成。头像默认保存到后端工作目录的 `data/uploads`，可通过 `app.upload-dir` 配置；旧共享头像需要重新上传。历史访客无楼宇、历史报修无法唯一对应学号时，仅管理员可见，需人工核实归属。

下一阶段完善富文本与上传资源生命周期、前端重复逻辑及依赖升级。演示验证不等于生产可用或完整安全审计通过。

## 来源与贡献

原项目：[NiToot/DormitoryManagementSystem](https://github.com/NiToot/DormitoryManagementSystem)。反馈问题时提供复现步骤、错误摘要和环境版本，移除密码、数据库凭据及个人信息。

仓库未附明确许可证文件，本次不代替作者选择开源许可证。
