# N2SQL 项目代码讲解与 Review

> 按文件逐个讲解功能、代码逻辑，并做 Review 检查。
> 讲解顺序：配置文件 -> 启动类 -> 枚举/实体/DTO -> 配置类 -> 异常处理 -> 工具类 -> Service层 -> 提示词模板 -> Controller

---

## 第一部分：配置文件

### 1. pom.xml

**这个文件是干什么的？**

pom.xml 是 Maven 项目的"购物清单"。就像你装电脑时要列一个配件清单（CPU、内存、显卡），pom.xml 列出了项目需要用到的所有"零件"（依赖库）。Maven 会根据这个清单自动从网上下载对应的 jar 包。

**关键内容讲解：**

- parent: spring-boot-starter-parent 3.2.5 — 继承 Spring Boot 父项目，统一管理版本号
- java.version: 17 — 使用 Java 17
- langchain4j.version: 0.35.0 — LangChain4j 框架版本（Java 版的 LangChain，用来对接大模型）
- jsqlparser.version: 4.9 — SQL 语法解析器版本（用来做静态语法校验）

**依赖清单：**

| 依赖 | 作用 |
|------|------|
| spring-boot-starter-web | Web 框架，提供 HTTP 接口能力 |
| spring-boot-starter-validation | 参数校验（@NotBlank 等注解） |
| spring-boot-starter-jdbc | JDBC 数据库连接支持 |
| spring-boot-configuration-processor | 让 IDE 识别自定义配置属性，有代码提示 |
| mysql-connector-j | MySQL 数据库驱动 |
| postgresql | PostgreSQL 数据库驱动 |
| langchain4j | LangChain4j 核心库 |
| langchain4j-open-ai | OpenAI 兼容接口（DeepSeek 也用这个） |
| langchain4j-spring-boot-starter | LangChain4j 与 Spring Boot 集成 |
| jsqlparser | SQL 语法解析器（第三阶段静态校验用） |
| lombok | 代码简化工具（自动生成 getter/setter 等） |
| spring-boot-starter-test | 测试框架 |

**Review 检查：**
- OK 依赖完整，覆盖了项目所有功能需求
- OK build 插件中排除了 lombok（lombok 只在编译时用，打包时不需要）
- OK 版本号用 properties 统一管理，方便后续升级

---

### 2. application.yml（主配置文件）

**这个文件是干什么的？**

Spring Boot 的"总开关配置"。就像手机的"设置"页面，控制整个应用的基本行为。

**内容讲解：**

- spring.application.name: n2sql — 应用名称
- spring.profiles.active: dev — 激活 dev 开发环境配置（会自动加载 application-dev.yml）
- server.port: 8080 — 服务端口号，启动后访问 localhost:8080
- logging.level.root: INFO — 全局日志级别
- logging.level.com.itheima.n2sql: DEBUG — 我们自己的代码打印 DEBUG 级别日志（更详细）

**举例怎么用：**
Spring Boot 启动时自动读取这个文件。比如设置了 port:8080，那浏览器访问 localhost:8080 就能到达你的服务。

**Review 检查：**
- OK 配置简洁明了
- OK 用 profiles 分环境管理，开发/生产可以用不同配置

---

### 3. application-dev.yml（开发环境配置）

**这个文件是干什么的？**

开发阶段专用的配置。和 application.yml 是"继承"关系——dev.yml 里的配置会覆盖主配置中同名的项。

**内容讲解：**

- n2sql.llm.api-key — DeepSeek 大模型的 API 密钥（需要替换成真实的）
- n2sql.llm.base-url — 大模型 API 地址
- n2sql.llm.model-name: deepseek-chat — 使用的模型名
- n2sql.llm.temperature: 0.0 — 温度参数，0 表示最精确（生成 SQL 不需要创意）
- n2sql.llm.max-tokens: 2048 — 大模型最多回复 2048 个 token
- n2sql.correction.max-retries: 3 — 自纠错最多重试 3 次
- n2sql.sql.max-rows: 1000 — SQL 查询最多返回 1000 行
- n2sql.sql.timeout-seconds: 30 — SQL 查询超时 30 秒

**Review 检查：**
- OK 配置项覆盖了 LLM、纠错、SQL安全三个方面
- 注意 api-key 目前是 YOUR_API_KEY_HERE，实际运行前需要替换

---

## 第二部分：启动类

### 4. N2sqlApplication.java

**路径：** com.itheima.n2sql.N2sqlApplication

**这个文件是干什么的？**

整个项目的"电源开关"。运行 main 方法，Spring Boot 就启动了。就像按电脑的开机键。

**关键代码讲解：**

- @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
  - @SpringBootApplication 是 Spring Boot 的核心注解，标记"这是启动类"
  - exclude 排除了 DataSourceAutoConfiguration — 为什么？因为正常 Spring Boot 会自动从 yml 里找数据库配置来创建连接。但我们的项目是"动态数据源"，用户可以添加多个数据库，所以不需要 Spring 自动创建，我们自己用 DataSourceManager 管理
- SpringApplication.run() — 启动 Spring Boot 应用

**Review 检查：**
- OK exclude DataSourceAutoConfiguration 是正确的，否则启动会报错（因为 yml 里没配默认数据源）
- OK 代码简洁，只有启动逻辑

---

## 第三部分：枚举与实体

### 5. DbType.java（数据库类型枚举）

**路径：** model/enums/DbType.java

**这个文件是干什么的？**

定义系统支持的数据库类型。目前支持 MYSQL 和 POSTGRESQL 两种。每种类型自带三个信息：默认端口、JDBC URL 模板、驱动类名。

**通俗比喻：** 就像一个"数据库品牌目录"。你要连 MySQL，我知道默认端口是 3306、用什么驱动、URL 长什么样。

**举例怎么用：**

  DbType.MYSQL.getDefaultPort()  返回 3306
  DbType.MYSQL.buildJdbcUrl("localhost", 3306, "mydb")
  返回 "jdbc:mysql://localhost:3306/mydb?useUnicode=true&..."

**代码逻辑：**
- 枚举构造方法接收三个参数：defaultPort, urlTemplate, driverClassName
- buildJdbcUrl() 方法用 String.format 把 host/port/dbName 填入 URL 模板

**Review 检查：**
- OK 枚举设计合理，新增数据库类型只需加一个枚举值
- OK MySQL URL 带了常用参数（useUnicode、characterEncoding、serverTimezone）
- 后续如果要支持 ClickHouse，在这里加一个枚举值即可

---

### 6. DataSourceInfo.java（数据源实体）

**路径：** model/entity/DataSourceInfo.java

**这个文件是干什么的？**

描述一个数据库连接的完整信息，相当于"数据库的名片"。用户添加一个数据源，就会创建一个 DataSourceInfo 对象。

**字段说明：**
- id — 唯一标识，如 "ds-abc123"（系统自动生成的 UUID）
- name — 用户起的名字，如 "我的测试数据库"
- dbType — 数据库类型（MYSQL 或 POSTGRESQL）
- host — 主机地址，如 "localhost"
- port — 端口号，如 3306
- dbName — 数据库名，如 "test_db"
- username / password — 账号密码

**关键方法：**
- getJdbcUrl() — 调用 dbType.buildJdbcUrl() 自动拼出完整的 JDBC URL

**Review 检查：**
- OK 字段覆盖了数据库连接所需的所有信息
- OK getJdbcUrl() 通过 DbType 枚举生成，避免了手动拼 URL 出错

---

## 第四部分：DTO 数据传输对象

DTO 是 Data Transfer Object（数据传输对象）的缩写。
通俗理解：DTO 就是"快递包裹的格式"。前端给后端发数据、后端给前端回数据，都要有一个统一的格式。
Entity 是内部用的（像家里的东西），DTO 是对外传输用的（像快递包装）。

### 7. ApiResult.java（统一响应格式）

**路径：** model/dto/ApiResult.java

**这个文件是干什么的？**

所有接口返回给前端的"信封格式"。不管成功还是失败，前端收到的 JSON 都长这样：
  { "code": 200, "message": "操作成功", "data": ... }
  { "code": 500, "message": "出错了", "data": null }

这样前端只需要判断 code 是不是 200 就知道成功没，非常方便。

**泛型 T 是什么意思？**

ApiResult<T> 中的 T 表示 data 字段可以是任何类型：
  - ApiResult<String> — data 是字符串
  - ApiResult<List<DataSourceInfo>> — data 是数据源列表
  - ApiResult<Nl2SqlResponse> — data 是 NL2SQL 的完整结果

**四个静态工厂方法讲解：**

1. success(T data) — 成功且带数据。用法：return ApiResult.success(myData);
2. success() — 成功但不带数据（比如删除操作，不需要返回什么）
3. success(String message, T data) — 成功+自定义消息+数据
4. error(String message) — 失败，默认状态码 500
5. error(int code, String message) — 失败，自定义状态码（如 404 表示找不到）

**为什么用静态方法而不是 new？**
  写 ApiResult.success(data) 比 new ApiResult(200, "操作成功", data) 更简洁易读。
  这种设计模式叫"静态工厂方法"。

**Review 检查：**
- OK 统一格式，前端处理方便
- OK 泛型设计灵活，data 可以是任何类型
- OK 静态工厂方法简洁

---

### 8. DataSourceCreateRequest.java（创建数据源请求）

**路径：** model/dto/DataSourceCreateRequest.java

**这个文件是干什么的？**

前端调用"新增数据源"接口时，需要传过来的数据格式。
就像你填一个表单：数据源名称、数据库类型、地址、端口、数据库名、用户名、密码。

**参数校验注解讲解（很重要的知识点）：**

- @NotBlank(message = "数据源名称不能为空") — 不能是 null，也不能是空字符串""或纯空格"  "
- @NotNull(message = "数据库类型不能为空") — 不能是 null（用于非字符串类型，如枚举）
- port 字段没有加校验注解 — 因为 port 是选填的，不填就用 DbType 的默认端口

**这些校验什么时候触发？**
当 Controller 方法参数加了 @Valid 注解时，Spring 会自动校验。
如果校验不通过，会抛出 MethodArgumentNotValidException，被 GlobalExceptionHandler 捕获。

**为什么 port 用 Integer 而不是 int？**
  int 是基本类型，默认值是 0，无法区分"用户填了 0"和"用户没填"。
  Integer 是包装类型，默认值是 null，null 就表示"没填"，可以用默认端口。

**Review 检查：**
- OK 必填字段都有校验注解
- OK port 用 Integer 允许为空，设计合理
- OK message 写了中文提示，用户体验好

---

### 9. DataSourceTestResponse.java（测试连接响应）

**路径：** model/dto/DataSourceTestResponse.java

**这个文件是干什么的？**

用户点"测试连接"按钮后，后端返回的结果。告诉用户：连接成功了吗？数据库版本是什么？花了多久？

**字段说明：**
- success — 连接是否成功（true/false）
- message — 提示信息（"连接成功" 或 "连接失败: xxx"）
- dbVersion — 数据库版本（如 "MySQL 8.0.33"），只有成功时才有
- costTimeMs — 连接耗时（毫秒），无论成功失败都有

**两个静态工厂方法：**

1. ok(dbVersion, costTimeMs) — 快速创建"连接成功"的响应
2. fail(reason, costTimeMs) — 快速创建"连接失败"的响应

这样用起来很简洁：
  成功时：return DataSourceTestResponse.ok("MySQL 8.0.33", 156);
  失败时：return DataSourceTestResponse.fail("拒绝连接", 3000);

**Review 检查：**
- OK 静态工厂方法设计简洁
- OK 成功和失败都记录了耗时，方便排查网络问题

---

### 10. ColumnInfo.java（列信息）

**路径：** model/dto/ColumnInfo.java

**这个文件是干什么的？**

描述数据库表中"一个列"的信息。就像 Excel 表格里的一个列头：这列叫什么名字、是什么数据类型、能不能为空。

**字段说明：**
- columnName — 列名，如 "user_id"、"name"、"age"
- dataType — 数据类型，如 "VARCHAR(50)"、"INT"、"DECIMAL(10,2)"
- nullable — 是否允许为空（true 表示可以不填，false 表示必须有值）
- comment — 列的注释（如果在数据库里设置了注释的话，如 "用户姓名"）

**举例：** users 表的 name 列
  columnName = "name"
  dataType = "VARCHAR(50)"
  nullable = true
  comment = "用户姓名"

**Review 检查：**
- OK 四个字段完整描述了一个列的信息
- OK comment 字段很有用，大模型看到注释能更好理解列的含义

---

### 11. TableSchema.java（表结构）

**路径：** model/dto/TableSchema.java

**这个文件是干什么的？**

描述"一张表"的完整结构。就像 Excel 里一张 Sheet 的结构说明：表名是什么、有哪些列、哪列是主键、有没有外键关系。

**字段说明：**
- tableName — 表名，如 "users"、"orders"
- tableComment — 表的注释，如 "用户表"、"订单表"
- columns — 该表所有列的信息（List<ColumnInfo>）
- primaryKeys — 主键列名列表（一般只有一个，如 ["id"]）
- foreignKeys — 外键关系列表，格式为 "本表列名 -> 被引用表.列名"

**外键举例：** orders 表有 user_id 列，引用了 users 表的 id 列
  foreignKeys = ["user_id -> users.id"]
  这告诉大模型：orders.user_id 和 users.id 可以 JOIN 连接

**@Builder.Default 是什么意思？**
  用 @Builder 创建对象时，如果你没设置某个字段，它默认是 null。
  加了 @Builder.Default 后，即使没设置，也会用你给的默认值（这里是 new ArrayList<>()，空列表）。
  这样就不会出现 NullPointerException（空指针异常）。

**Review 检查：**
- OK 外键格式 "user_id -> users.id" 清晰，大模型容易理解
- OK @Builder.Default 避免了空指针问题

---

### 12. DatabaseSchema.java（数据库结构）

**路径：** model/dto/DatabaseSchema.java

**这个文件是干什么的？**

描述"整个数据库"的结构：数据库名 + 包含的所有表。这是提供给大模型的最关键信息。

**字段说明：**
- databaseName — 数据库名，如 "test_db"
- tables — 所有表的结构列表（List<TableSchema>）

**核心方法 formatAsDDL() 详解：**

这是整个文件最重要的方法！它把数据库结构转成 CREATE TABLE 语句格式的文本。

为什么要转成 DDL？因为大模型在训练时见过大量的 CREATE TABLE 语句，这是它最"熟悉"的数据库描述格式。用 DDL 格式告诉大模型数据库长什么样，它理解得最准确。

输出示例：
  -- 数据库: test_db
  CREATE TABLE users (
    id INT NOT NULL,  -- 主键
    name VARCHAR(50),
    age INT,
    PRIMARY KEY (id)
  );
  -- 用户表

**方法内部逻辑（逐步讲解）：**
1. 先写一行注释 "-- 数据库: xxx"
2. 遍历每张表，写 "CREATE TABLE 表名 ("
3. 遍历每个列，写 "列名 类型"，如果不允许为空加 "NOT NULL"
4. 在列后面加注释（主键标记、外键标记、列注释）
5. 如果有主键，加一行 "PRIMARY KEY (列名)"
6. 最后如果表有注释，加一行 "-- 表注释"

**Review 检查：**
- OK DDL 格式是业界公认最适合大模型理解的数据库描述方式
- OK 注释中包含了主键和外键信息，帮助大模型理解表之间的关系
- OK 外键以 "外键: user_id -> users.id" 格式写在列注释里，很直观

---

### 13. Nl2SqlRequest.java（NL2SQL 请求体）

**路径：** model/dto/Nl2SqlRequest.java

**这个文件是干什么的？**

前端调用"自然语言转SQL"接口时需要传的数据。非常简单，只有两个字段。
就像去餐厅点菜，你只需要说：在哪个桌（数据源ID）+ 要什么菜（问题）。

**字段说明：**
- dataSourceId — 数据源ID，指定在哪个数据库上查询（如 "ds-abc123"）
- question — 用户的自然语言问题（如 "查询所有年龄大于25的用户"）

**两个字段都加了 @NotBlank：**
  意思是必填、不能为空字符串。如果前端不传，Spring 自动拦截并返回错误提示。

**Review 检查：**
- OK 设计简洁，只有必要的两个字段
- OK 校验注解完整

---

### 14. Nl2SqlResponse.java（NL2SQL 响应体）

**路径：** model/dto/Nl2SqlResponse.java

**这个文件是干什么的？**

整个系统的"最终答案信封"。用户问了一个问题，系统经过层层处理后，把所有结果都装在这里返回。

**通俗比喻：** 就像医院的检查报告单，上面有：
- 你当初来看什么病（question — 用户原始问题）
- 医生开的药方（generatedSql — 生成的 SQL）
- 检查结果数据（queryResult — SQL 执行结果）
- 检查成功了吗（success）
- 如果失败是什么原因（errorMessage）
- 医生反复调整了几次药方（retryCount — 纠错重试次数）
- 每次调整的记录（correctionHistory — 纠错历史）

**字段详解：**

| 字段 | 类型 | 作用 |
|------|------|------|
| question | String | 用户的原始自然语言问题 |
| generatedSql | String | 大模型最终生成的 SQL 语句 |
| queryResult | QueryResult | SQL 执行结果（列名+数据行+行数+耗时） |
| success | boolean | 整个流程是否成功 |
| errorMessage | String | 失败时的错误信息 |
| retryCount | int | 自纠错机制重试了几次（默认0，表示一次就成功） |
| correctionHistory | List<CorrectionRecord> | 每次纠错的详细记录列表 |

**@Builder.Default 的作用：**
  retryCount 默认 0（表示没有重试），correctionHistory 默认空列表。
  这样即使不设置这两个字段，也不会出问题。

**Review 检查：**
- OK 字段设计完整，覆盖了成功和失败两种场景
- OK 纠错相关字段让前端可以展示完整的纠错过程
- OK @Builder.Default 使用正确

---

### 15. QueryResult.java（SQL 查询结果）

**路径：** model/dto/QueryResult.java

**这个文件是干什么的？**

存储 SQL 执行后的结构化结果。就像你在 Navicat 或 DataGrip 里执行一条 SQL 后看到的那个表格。

**举例：** 执行 SELECT name, age FROM users 后：
  columns = ["name", "age"]  -- 列头
  rows = [
    {"name": "张三", "age": 25},  -- 第一行
    {"name": "李四", "age": 30}   -- 第二行
  ]
  rowCount = 2  -- 总共2行
  executeTimeMs = 15  -- 执行了15毫秒

**字段说明：**
- columns — 列名列表（List<String>），如 ["name", "age", "email"]
- rows — 数据行列表（List<Map<String, Object>>），每行是一个 Map，Key=列名，Value=值
- rowCount — 结果总行数
- executeTimeMs — SQL 执行耗时（毫秒）

**为什么 rows 用 List<Map> 而不是 List<Object[]>？**
  Map 有 Key（列名），JSON 序列化后 {"name":"张三"} 比 ["张三"] 更容易理解。
  前端拿到后可以直接用 row.name 访问，非常方便。

**Review 检查：**
- OK List<Map> 结构适合 JSON 序列化
- OK executeTimeMs 记录耗时，便于性能分析

---

### 16. CorrectionRecord.java（纠错记录）

**路径：** model/dto/CorrectionRecord.java

**这个文件是干什么的？**

第三阶段"自纠错机制"的记录。每当大模型生成的 SQL 有问题被纠正一次，就产生一条记录。
就像考试改错本：记录你每次写错了什么、错在哪里。

**举例：**
  第1轮：sql = "SELECT * FROM user"    errorType = "SYNTAX_ERROR"   errorMessage = "表名不存在，应该是 users"
  第2轮：sql = "SELECT * FROM users"   成功！不再产生 CorrectionRecord

**字段说明：**
- sql — 该轮生成的（错误的）SQL
- errorType — 错误类型，如 "SYNTAX_ERROR"（语法错误）、"EXECUTION_ERROR"（执行错误）
- errorMessage — 具体的错误描述
- timestamp — 发生时间（@Builder.Default 默认取当前时间）

**Review 检查：**
- OK errorType 区分了语法错误和执行错误，便于分析哪类错误多
- OK timestamp 自动记录时间，不需要手动设置

---

## 第五部分：配置类

### 17. LlmProperties.java（大模型配置属性）

**路径：** config/LlmProperties.java

**这个文件是干什么的？**

自动从 application-dev.yml 中读取以 "n2sql.llm" 开头的配置项，映射成 Java 对象。
就像一个"翻译官"，把 yml 里的文字配置翻译成 Java 能直接用的对象。

**yml 配置和 Java 字段的对应关系：**

  yml 里写的：n2sql.llm.api-key
  Java 字段：apiKey
  Spring 自动把横杠命名(api-key)转成驼峰命名(apiKey)

**核心注解 @ConfigurationProperties(prefix = "n2sql.llm")：**
  告诉 Spring："去 yml 里找 n2sql.llm 开头的所有配置，自动填充到我的字段里"。
  这样就不用一个一个手动用 @Value 读取了。

**字段及默认值：**
- apiKey = "YOUR_API_KEY_HERE" — API 密钥（必须替换成真实的）
- baseUrl = "https://api.deepseek.com/v1" — DeepSeek 的 API 地址
- modelName = "deepseek-chat" — 使用的模型名称
- temperature = 0.0 — 温度参数（0=最精确，1=最随机）。生成 SQL 要精确，所以设 0
- maxTokens = 2048 — 限制大模型回复的最大长度

**为什么 temperature 设为 0？**
  温度越高，大模型回答越"有创意"但也越不稳定。
  我们要的是精确的 SQL，不需要创意，所以设为 0，让它每次都给最确定的答案。

**Review 检查：**
- OK @ConfigurationProperties 比逐个 @Value 更优雅
- OK 每个字段都有合理的默认值
- OK @Component 注解让 Spring 自动管理这个类

---

### 18. LlmConfig.java（大模型客户端配置）

**路径：** config/LlmConfig.java

**这个文件是干什么的？**

根据 LlmProperties 中的配置参数，创建一个"大模型客户端"对象（ChatLanguageModel）。
其他类要调用大模型时，只需要注入 ChatLanguageModel 就行了。

**通俗比喻：**
  LlmProperties = 手机号码本（存着号码、地址等信息）
  LlmConfig = 拨号器（用号码本里的信息，拨通电话，建立连接）
  ChatLanguageModel = 已接通的电话（直接说话就行）

**关键注解讲解：**

- @Configuration — 告诉 Spring "这个类里面有 @Bean 方法"
  - 就像一个"工厂类"，里面的方法负责"生产"对象
- @Bean — 告诉 Spring "这个方法返回的对象，你帮我管理起来"
  - Spring 会自动调用这个方法，把返回值存到"容器"里
  - 之后任何地方用 @Autowired ChatLanguageModel 就能拿到这个对象
- @RequiredArgsConstructor — Lombok 自动为 final 字段生成构造方法（实现依赖注入）

**chatLanguageModel() 方法详解：**

1. 从 llmProperties 读取所有配置
2. 用 OpenAiChatModel.builder() 创建大模型客户端
   - 为什么用 OpenAi？因为 DeepSeek 的 API 格式兼容 OpenAI，只需要改 baseUrl
3. 设置超时 60 秒（复杂 SQL 生成可能较慢）
4. 开启请求/响应日志（开发阶段方便调试，能看到发给大模型什么、大模型回了什么）
5. 启动时打印初始化日志（模型名、地址、温度）

**Review 检查：**
- OK 60秒超时合理，复杂查询需要更多时间
- OK 开发阶段开启日志方便调试
- OK 使用 OpenAI 兼容格式，切换其他大模型只需改 baseUrl 和 apiKey

---

### 19. DataSourceManager.java（动态数据源管理器）

**路径：** config/DataSourceManager.java

**这个文件是干什么的？**

管理多个数据库连接池的"管家"。用户可以添加多个数据库（如一个 MySQL、一个 PostgreSQL），每个数据库都有自己的连接池，这个类负责创建、获取和销毁这些连接池。

**通俗比喻：** 就像酒店的前台管理多把房间钥匙。
  - register() = 办理入住，给你一把钥匙（创建连接池）
  - getConnection() = 凭钥匙开门进房间（从连接池获取一个数据库连接）
  - remove() = 退房，收回钥匙（关闭并删除连接池）
  - contains() = 查一下这个房号有没有人住

**什么是连接池（HikariCP）？**
  每次用数据库都要"建立连接"，这个过程很慢（像每次打电话都要重新拨号）。
  连接池就是预先建好几个连接放在那里，用的时候直接拿，用完了放回去。
  HikariCP 是目前性能最好的 Java 连接池，Spring Boot 默认推荐。

**ConcurrentHashMap 是什么？**
  就是一个线程安全的 HashMap。多个用户同时操作（并发）也不会出错。
  Key = 数据源ID（如 "ds-abc123"）
  Value = HikariDataSource 连接池对象

**register() 方法详解：**
1. 如果这个 ID 已经注册过，先关闭旧的再重新注册
2. 配置 HikariCP 参数：
   - 连接池名称（方便日志识别）
   - JDBC URL、用户名、密码、驱动类
   - 最大连接数 5（毕设够用了）
   - 最小空闲连接 1
   - 连接超时 10秒、空闲超时 5分钟、最大存活 10分钟
3. 创建连接池并存入 Map

**Review 检查：**
- OK 连接池参数设置合理（5个连接对毕设足够）
- OK register() 先检查已存在再注册，避免重复创建
- OK remove() 会关闭连接池释放资源，不会内存泄漏
- OK ConcurrentHashMap 保证线程安全

---

## 第六部分：异常处理

### 20. BizException.java（自定义业务异常）

**路径：** exception/BizException.java

**这个文件是干什么的？**

自定义的业务异常类。当业务逻辑出问题时（比如数据源不存在、SQL 执行失败），就抛出这个异常。

**通俗比喻：** Java 自带的异常像"通用报警器"，只会说"出错了"。
BizException 是我们自定义的"智能报警器"，能说清楚"什么错了"和"错误代码是多少"。

**三个构造方法（三种不同的报警方式）：**

1. BizException("数据源不存在")
   - 只传错误消息，状态码默认 500

2. BizException(404, "数据源不存在")
   - 传状态码 + 消息，404 表示"找不到"

3. BizException("SQL执行失败", originalException)
   - 传消息 + 原始异常。这样日志里能看到完整的错误链
   - 比如原始异常是 SQLException，包装后还能追溯到根本原因

**@Getter 注解：**
  Lombok 自动生成 getCode() 方法，让 GlobalExceptionHandler 能读取错误码。

**为什么继承 RuntimeException 而不是 Exception？**
  RuntimeException 是"运行时异常"，不需要在方法签名上声明 throws。
  代码更简洁，throw new BizException("xxx") 就行了，不用到处写 throws。

**Review 检查：**
- OK 三种构造方法覆盖了常见使用场景
- OK 继承 RuntimeException 使用方便
- OK code 字段配合 GlobalExceptionHandler 返回正确的 HTTP 状态码

---

### 21. GlobalExceptionHandler.java（全局异常处理器）

**路径：** exception/GlobalExceptionHandler.java

**这个文件是干什么的？**

全局的"异常拦截网"。所有 Controller 中抛出的异常都会被这里自动捕获和处理，不需要在每个方法里写 try-catch。

**通俗比喻：** 就像公司的客服部门。
  - 员工（Controller）遇到问题，不需要自己对客户解释
  - 把问题扔给客服部（GlobalExceptionHandler），客服用统一的话术回复客户（前端）

**工作原理：**
1. Controller 方法执行时抛出异常
2. Spring 发现这个类有 @RestControllerAdvice 注解
3. 根据异常类型匹配 @ExceptionHandler 方法
4. 执行对应方法，把返回值作为 HTTP 响应

**三个处理方法：**

1. handleBizException(BizException) — 处理我们自定义的业务异常
   - 日志级别 warn（预期内的错误）
   - 返回对应的 code 和 message

2. handleValidationException(MethodArgumentNotValidException) — 处理参数校验异常
   - 当 @NotBlank 等校验不通过时触发
   - 提取第一条错误信息返回
   - 状态码 400（客户端参数错误）

3. handleException(Exception) — 兜底处理所有未知异常
   - 日志级别 error（意外错误），打印完整堆栈
   - 返回 500 + "系统内部错误"（不暴露敏感信息给前端）

**Review 检查：**
- OK 三级处理：业务异常/校验异常/未知异常，覆盖完整
- OK 未知异常不暴露堆栈给前端（安全）
- OK 统一返回 ApiResult 格式

---

## 第七部分：工具类

### 22. SqlCleanUtil.java（SQL 清洗工具）

**路径：** util/SqlCleanUtil.java

**这个文件是干什么的？**

从大模型返回的"杂乱文本"中提取出干净的 SQL 语句。

**为什么需要这个？**
大模型有时候不只返回 SQL，还会附带解释文字或 Markdown 格式。比如：

  大模型返回的原始文本：
  "以下是查询语句：
  （三个反引号sql）
  SELECT * FROM users;
  （三个反引号）
  希望对你有帮助"

  我们需要提取出的干净SQL：
  "SELECT * FROM users"

**cleanSql() 方法处理逻辑（按顺序）：**

1. 检查是否为空
2. 如果包含（三个反引号sql），提取代码块内的内容
3. 如果包含（三个反引号），提取代码块内的内容
4. 去除前后空白
5. 去除末尾的分号（JDBC 执行不需要分号）

**isSelectStatement() 方法：**
  简单判断 SQL 是否以 SELECT 开头（忽略大小写）。
  用于安全检查——禁止执行 DELETE、UPDATE、DROP 等危险操作。

**为什么是 static 方法？**
  工具类的方法通常是 static 的，因为它不需要保存任何状态。
  直接 SqlCleanUtil.cleanSql(text) 就能调用，不需要 new 对象。

**Review 检查：**
- OK 处理了两种常见的 Markdown 代码块格式
- OK 去末尾分号是正确的（JDBC Statement.executeQuery 不需要分号）
- OK isSelectStatement 做了安全防护

---

## 第八部分：Service 业务服务层

Service 层是整个项目的"大脑"，所有核心业务逻辑都在这里。
通俗理解：Controller 是"前台接待"，Service 是"后台干活的人"。

### 23. DataSourceService.java（数据源业务服务）

**路径：** service/DataSourceService.java

**这个文件是干什么的？**

封装数据源的增删查 + 测试连接。就像一个"数据库管理员"，负责：
添加新数据库、查看已有数据库、删除数据库、测试数据库能不能连上。

**数据存储方式：**
  目前数据源信息存在内存中（ConcurrentHashMap），重启后会丢失。
  这是毕设阶段的简化方案，后续可以改为存到数据库中持久化。

**依赖注入讲解（@RequiredArgsConstructor）：**
  private final DataSourceManager dataSourceManager;
  这个字段前面有 final，Lombok 会自动生成构造方法。
  Spring 启动时会自动创建 DataSourceManager 对象并传进来。
  这就是"依赖注入"——你不需要自己 new，Spring 帮你准备好。

**五个核心方法：**

1. create(DataSourceCreateRequest request)
   - 生成唯一ID：ds- + 8位UUID（如 "ds-a1b2c3d4"）
   - 如果用户没填端口，用 DbType 的默认端口
   - 用 Builder 模式创建 DataSourceInfo
   - **先注册连接池**（try-catch 包裹），成功后才存入内存 Map
   - 如果连接池初始化失败（数据库名错误、密码错误等），抛出 BizException 友好提示
   - 返回创建好的数据源信息

2. listAll()
   - 返回所有已注册的数据源列表
   - 从 ConcurrentHashMap 的 values() 转成 ArrayList

3. getById(String id)
   - 根据ID查找数据源，找不到就抛 BizException(404)

4. delete(String id)
   - 从内存中移除 + 关闭连接池
   - 不存在就抛 BizException(404)

5. testConnection(DataSourceCreateRequest request)
   - 创建临时数据源（ID固定为 "temp-test"）
   - 尝试获取连接并读取数据库版本
   - 无论成功失败，最后都用 finally 清理临时数据源
   - 返回 DataSourceTestResponse（成功/失败+版本+耗时）

**Review 检查：**
- OK 增删查功能完整
- OK testConnection 用 finally 确保临时资源被清理
- OK 找不到时抛 404，前端能区分错误类型

---

### 24. SchemaExtractService.java（数据库结构提取服务）

**路径：** service/SchemaExtractService.java

**这个文件是干什么的？**

自动扫描一个数据库的完整结构（有哪些表、每张表有哪些列、主键外键是什么）。
这些信息会被转成 DDL 文本塞进提示词，让大模型"看懂"你的数据库。

**通俗比喻：** 就像一个"数据库X光机"，扫描数据库的"骨架"。

**核心技术：JDBC DatabaseMetaData**
  Java 标准库自带的功能，不需要写 SQL 就能获取数据库的结构信息：
  - getTables() — 获取所有表名
  - getColumns() — 获取某张表的所有列
  - getPrimaryKeys() — 获取主键
  - getImportedKeys() — 获取外键关系

**主要方法：**

1. extractSchema(dataSourceId) — 入口方法
   - 获取数据库连接
   - 读取数据库名称（MySQL 用 getCatalog()，PostgreSQL 用 getSchema()）
   - 调用 extractAllTables() 提取所有表
   - 返回 DatabaseSchema 对象

2. extractAllTables(metaData, dbName) — 提取所有表
   - getTables() 参数：catalog=dbName, schema="public", pattern="%", types=["TABLE"]
   - 同时传 catalog 和 schema 是为了兼容 MySQL 和 PostgreSQL
   - 对每张表调用 extractColumns/extractPrimaryKeys/extractForeignKeys

3. extractColumns() — 提取列信息
   - 读取列名、类型名、长度、小数位、是否可空、注释
   - 调用 buildFullTypeName() 拼接完整类型（如 VARCHAR(50)、DECIMAL(10,2)）

4. extractPrimaryKeys() — 提取主键列名

5. extractForeignKeys() — 提取外键关系
   - getImportedKeys() 返回"本表引用了谁"
   - 格式化为 "user_id -> users.id"

6. buildFullTypeName() — 智能拼接类型名
   - INT/BOOL/TEXT/DATE 等类型不显示长度
   - 有小数位的显示为 DECIMAL(10,2)
   - 有长度的显示为 VARCHAR(50)

**Review 检查：**
- OK 用标准 JDBC API，不依赖特定数据库
- OK 同时兼容 MySQL(catalog) 和 PostgreSQL(schema)
- OK buildFullTypeName 处理了常见数据类型
- OK try-with-resources 自动关闭连接

---

### 25. PromptTemplateService.java（提示词模板服务）

**路径：** service/prompt/PromptTemplateService.java

**这个文件是干什么的？**

负责加载和管理提示词模板文件，把模板中的占位符替换为真实值，生成完整的提示词发给大模型。

**通俗比喻：** 就像一个"填空题批改老师"。
  模板文件 = 一张有空格的试卷
  这个服务 = 帮你把空格填上正确答案，交给大模型

**这不是 RAG！**
  提示词模板是固定的"填空格"，不是根据用户问题去检索知识库。
  变量来源：schema 来自数据库自动扫描，question 来自用户输入原文。
  用户输入什么问题，就原封不动地填进去，不做任何"提取"或"理解"。

**核心机制（2026-03-18 改进版）：**

1. @PostConstruct init() — 项目启动时自动调用
   - 只加载2个模板：base_nl2sql.txt + correction_nl2sql.txt
   - 存入 HashMap 缓存，后续直接从内存读取（不用每次读文件）
   - 旧的4个专用模板(simple/aggregate/multijoin/nested)不再加载

2. loadTemplate(name, filePath) — 从 resources 目录加载模板文件

3. 【核心方法】buildSoftPrompt(schemaDDL, question, dbType, hints) — 软策略提示词
   - 始终用 base_nl2sql 基础模板
   - 把 hints 列表用 String.join("\n", hints) 拼成文本
   - 替换3个占位符：{{schema}}、{{hints}}、{{question}}
   - 如果 dbType 不为空，末尾追加方言提示
   - hints 为空时 {{hints}} 被替换为空字符串（等于简单查询）

4. 【核心方法】buildCorrectionPromptWithHistory(schemaDDL, question, correctionHistory) — 完整历史纠错
   - 遍历 List<CorrectionRecord>，格式化为：
     "### Attempt 1\nSQL:\n```sql\n...\n```\nError Type: SYNTAX_ERROR\nError Message: ...\n"
   - 替换3个占位符：{{schema}}、{{question}}、{{error_history}}
   - 大模型能看到所有之前的失败尝试，避免来回振荡犯同样的错

**已删除的旧方法（2026-03-18）：**
- buildNl2SqlPrompt() — 被 buildSoftPrompt() 替代
- buildPromptByTemplateName() 两个重载 — 不再需要按模板名选模板
- buildCorrectionPrompt() — 被 buildCorrectionPromptWithHistory() 替代

**Review 检查：**
- OK @PostConstruct 只在启动时加载一次，后续从内存读取
- OK 模板和代码分离，修改提示词不需要改 Java 代码
- OK buildSoftPrompt 的 hints 可为空，优雅降级
- OK buildCorrectionPromptWithHistory 传入完整历史，按导师要求避免振荡

---

### 26. SqlExecuteService.java（SQL 执行引擎）

**路径：** service/SqlExecuteService.java

**这个文件是干什么的？**

在指定的数据源上安全地执行 SQL 查询，并把结果转成结构化的 QueryResult。
就像一个"安全执行官"，带着三把锁去执行任务。

**三重安全防护：**

1. 只允许 SELECT 语句
   - 调用 SqlCleanUtil.isSelectStatement() 检查
   - 如果是 DELETE/UPDATE/DROP 等，直接拒绝

2. 查询超时限制（默认30秒）
   - stmt.setQueryTimeout(timeoutSeconds)
   - 超时自动中断，防止慢查询拖垮系统

3. 返回行数限制（默认1000行）
   - stmt.setMaxRows(maxRows)
   - 防止 SELECT * FROM 大表 返回百万行导致内存爆炸

**@Value 注解讲解：**
  @Value("${n2sql.sql.timeout-seconds:30}")
  从 yml 读取单个配置值。冒号后面的 30 是默认值（yml 里找不到就用 30）。
  比 @ConfigurationProperties 更轻量，适合只需要一两个配置的场景。

**execute() 方法详解：**
1. 安全检查（isSelectStatement）
2. 获取数据库连接（从连接池）
3. 创建 Statement 并设置超时和行数限制
4. 执行 SQL（executeQuery）
5. 调用 convertResultSet() 把结果转成 QueryResult
6. 记录执行耗时

**convertResultSet() 方法详解：**
  把 JDBC 的 ResultSet（游标式，一行一行读）转成 List<Map>（一次性全读出来）：
  1. 从 ResultSetMetaData 获取列数和列名
  2. 用 getColumnLabel() 优先取别名（如 SELECT name AS 姓名）
  3. 逐行读取，每行用 LinkedHashMap 存储（保持列的顺序不乱）
  4. 组装成 QueryResult 返回

**Review 检查：**
- OK 三重安全防护完善
- OK try-with-resources 自动关闭连接和 Statement
- OK LinkedHashMap 保持列顺序（HashMap 顺序是随机的）
- OK getColumnLabel 支持别名，用户体验好

---

### 27. SqlValidateService.java（SQL 静态验证服务）

**路径：** service/SqlValidateService.java

**这个文件是干什么的？**

在 SQL 真正去数据库执行之前，先在本地做两级"预检"。
就像考试交卷前先自己检查一遍——能发现的错误先改掉，不要等老师批改（数据库执行）再发现。

**两级验证：**

第一级：语法验证（JSQLParser 解析）
  - 用 CCJSqlParserUtil.parse(sql) 尝试解析 SQL
  - 如果语法有错（括号不匹配、关键字拼错），解析会抛异常
  - 同时检查是否是 SELECT 语句（instanceof Select）
  - 不需要连数据库，纯本地检查，速度极快

第二级：Schema 校验（表名 + 列名比对）
  - 检查 SQL 中用到的表名是否存在于 Schema 中
  - 检查 SQL 中明确写出的列名是否存在于对应的表中
  - 用已扫描的 DatabaseSchema 做本地比对，也不需要连数据库

**validate() 方法流程：**
1. 检查 SQL 是否为空
2. 用 JSQLParser 解析（第一级）
3. 检查是否是 SELECT（安全防护）
4. 如果有 Schema 信息，做表名+列名校验（第二级）
5. 全部通过返回 ValidationResult.ok()

**validateSchema() 方法详解：**

1. 构建查询字典：
   - allTableNames — 数据库所有表名的集合（全小写，忽略大小写）
   - tableColumnMap — 每张表有哪些列（Map<表名, Set<列名>>）

2. 校验表名：
   - 用 TablesNamesFinder（JSQLParser 自带工具）从 SQL 中提取所有表名
   - 逐个检查是否存在于 allTableNames 中
   - 如果不存在，调用 findSimilar() 找最相似的表名给出建议

3. 校验列名：
   - 先构建别名映射（如 "SELECT u.name FROM users u" 中 u 对应 users）
   - 从 SQL 中提取所有列引用
   - 如果列有表前缀（如 u.name），先通过别名映射找到真实表名，再检查该表是否有这列
   - 如果没有表前缀（如 SELECT name），在所有表的所有列中模糊查找

**findSimilar() 方法 — 智能建议：**
  当表名或列名不存在时，用"编辑距离"算法找最相似的名字。
  比如用户写了 "user"，数据库里是 "users"，编辑距离=1（只差一个s）。
  系统会提示："你是不是想用: users"
  只有编辑距离小于等于3才推荐，太远了就不推荐。

**editDistance() 方法 — 编辑距离算法：**
  计算把字符串A变成字符串B最少需要几步（插入/删除/替换）。
  这是经典的动态规划算法（Levenshtein Distance）。

**ValidationResult 内部类：**
  验证结果的封装，包含两个字段：
  - valid — 是否通过验证
  - errorMessage — 错误信息（通过时为 null）
  静态工厂方法 ok() 和 fail(msg) 方便创建。

**Review 检查：**
- OK 两级验证设计合理：语法先检查，再检查表名列名
- OK 大小写不敏感（全部转小写比较），避免误报
- OK findSimilar 给出修正建议，帮助大模型纠错
- OK 跳过了函数参数中的列（如 COUNT(id)），避免误报
- OK 支持表别名（u.name 能正确解析为 users.name）
- 已修复 JSQLParser 4.9 API 兼容问题：移除了 SelectBody 和 SelectExpressionItem 的使用

**2026-03-16 Bug修复：复杂嵌套查询静态校验误判**

问题：当大模型为"查询销售额最高的产品类别，以及该类别下卖得最好的产品"等复杂问题
生成含子查询的SQL（如 `SELECT * FROM (SELECT ... ROW_NUMBER() ...) sub`）时，
原有的 `buildAliasMap()` 和 `extractColumns()` 只处理 `PlainSelect`，
导致子查询/UNION/窗口函数等复杂SQL被误判为"列不存在"或抛异常。

修复内容：
1. **表名校验容错** — `TablesNamesFinder.getTableList()` 包裹 try-catch
   - 复杂SQL导致表名提取异常时，跳过表名校验，交给数据库执行阶段处理
2. **列名校验智能跳过** — 三层判断：
   - `Select` 不是 `PlainSelect`（如 UNION、SetOperationList）→ 跳过列名校验
   - FROM 子句不是真实表（是子查询 SubSelect）→ 跳过列名校验
   - 只有简单 `PlainSelect` 且 FROM 是真实 `Table` → 正常进行列名校验
3. **新增辅助方法** `getOutermostPlainSelect(Select)` — 安全地从 Select 中提取 PlainSelect
4. **设计理念** — 静态校验只处理"有把握的简单情况"，复杂查询完全交给数据库执行阶段 + 自纠错反馈闭环

---

### 28. Nl2SqlService.java（核心 NL2SQL 转换服务 - 项目的心脏）

**路径：** service/Nl2SqlService.java

**这个文件是干什么的？**

整个项目最核心的文件！实现了从"用户说中文"到"返回数据库查询结果"的完整流程，
并且带有：软策略提示词、完整历史自纠错、NO_MATCH实体检测。

**通俗比喻：** 就像一个"翻译+执行+检查"的全能助手：
  1. 用户说中文 -> 检测关键词生成补充指令(hints)
  2. 用基础模板+hints翻译成SQL
  3. 检查大模型是否说"这个东西数据库里不存在"(NO_MATCH)
  4. 检查 SQL 有没有语法错误
  5. 去数据库执行
  6. 如果出错了，把所有历史错误信息告诉翻译助手让他重新翻译
  7. 最多重试3次

**注入了6个依赖（2026-03-18 更新）：**
- SchemaExtractService — 提取数据库结构
- PromptTemplateService — 构建提示词（buildSoftPrompt + buildCorrectionPromptWithHistory）
- ChatLanguageModel — 大模型客户端（调用 DeepSeek）
- SqlExecuteService — 执行 SQL
- SqlValidateService — 静态验证 SQL
- QueryClassifier — 查询意图检测器（软策略版本，detectHints）

**generateSql() 方法完整流程（2026-03-18 改进版）：**

第一步：提取数据库 Schema
  调用 schemaExtractService.extractSchema()
  把结果格式化为 DDL 文本（formatAsDDL()）
  这一步只做一次，后续纠错时复用同一份 Schema

第二步：【改进-软策略】检测查询关键词，生成补充指令
  调用 queryClassifier.detectHints(question, schema)
  返回 List<String> hints（可同时包含聚合+多表+嵌套等多种提示）

第三步：【改进-软策略】构建带动态hints的提示词
  调用 promptTemplateService.buildSoftPrompt(schemaDDL, question, dbType, hints)
  始终用 base_nl2sql.txt 基础模板 + hints 动态注入

第四步：调用大模型 + 清洗响应

第五步：【新增-NO_MATCH检测】
  调用 isNoMatch(rawResponse) 检查大模型是否返回 "NO_MATCH: 原因"
  如果是 → extractNoMatchReason() 提取原因 → 直接返回友好错误提示
  不进入验证/执行流程

第六步：进入验证-执行-纠错循环（最多 maxRetries+1 轮）

  循环的每一轮做两件事：

  第一关：静态验证
    调用 sqlValidateService.validate(sql, schema)
    如果不通过（表名不存在、语法错误等）：
      - 记录 CorrectionRecord 到 correctionHistory
      - 【改进】调用 requestCorrectionWithHistory() 传入完整历史
      - 纠错后也检查 NO_MATCH

  第二关：数据库执行
    调用 sqlExecuteService.execute(dataSourceId, sql)
    成功 → 返回结果
    失败：
      - 记录 CorrectionRecord 到 correctionHistory
      - 【改进】调用 requestCorrectionWithHistory() 传入完整历史
      - 纠错后也检查 NO_MATCH

**requestCorrectionWithHistory() 私有方法（2026-03-18 改进）：**
  按导师要求：每次重试时，把之前【所有】失败的 SQL 和对应的错误信息都传给大模型。
  调用 promptTemplateService.buildCorrectionPromptWithHistory(schemaDDL, question, correctionHistory)
  大模型能看到："第1次试了这个SQL，错在这里；第2次试了那个，也错了"
  避免来回振荡犯同样的错误。

**isNoMatch() / extractNoMatchReason() 私有方法（2026-03-18 新增）：**
  按导师要求：如果用户问了不存在的实体（如"查询学生"，但数据库只有员工表），
  大模型应返回 "NO_MATCH: <原因>"，系统不应猜测。
  isNoMatch() 检查文本是否以 "NO_MATCH" 开头。
  extractNoMatchReason() 提取冒号后面的原因说明。

**buildFailResponse() 私有方法：**
  构建统一的失败响应，避免重复代码。

**Review 检查：**
- OK 完整实现了软策略+完整历史纠错+NO_MATCH三项导师改进要求
- OK 静态验证在前、动态执行在后，减少无效的数据库查询
- OK 纠错历史完整记录，前端可以展示每一轮的情况
- OK NO_MATCH 在首次生成和每次纠错后都做检测，覆盖全面
- OK 最外层有 try-catch 兜底，任何意外错误都不会导致系统崩溃

---

## 第九部分：提示词模板文件

### 29. base_nl2sql.txt（基础 NL2SQL 提示词模板 — 软策略核心模板）

**路径：** resources/prompts/base_nl2sql.txt

**这个文件是干什么的？**

首次生成 SQL 时使用的提示词模板。用英文写（大模型处理英文更准确）。
这是软策略的核心模板——所有类型的查询都用这一个模板，通过 {{hints}} 动态注入补充指令。

**包含三个占位符变量（2026-03-18 更新）：**
- {{schema}} — 被替换为数据库结构的 DDL 文本（来自 SchemaExtractService 扫描）
- {{hints}} — 被替换为 QueryClassifier.detectHints() 返回的补充指令（可为空字符串）
- {{question}} — 被替换为用户输入的原始自然语言问题

**模板结构：**
1. 角色设定："You are an expert SQL query generator"
2. 数据库结构：{{schema}} 占位符
3. 11条规则约束（比旧版多了第11条）：
   - 只生成 SELECT
   - 只用 Schema 中存在的表和列
   - 正确使用 JOIN、聚合函数、ORDER BY
   - 不要加解释、不要 Markdown 格式
   - 【新增】第11条：NO_MATCH 约束 — 如果用户问的实体在 Schema 中不存在，
     不准猜测，必须返回 "NO_MATCH: <原因>"
4. 动态补充指令：{{hints}} 占位符（软策略的关键！）
5. 用户问题：{{question}} 占位符
6. 以 "SQL Query" 结尾，引导大模型直接输出 SQL

**{{hints}} 的动态注入机制：**
  - 简单查询：hints 为空 → {{hints}} 被替换为空字符串 → 等于没有额外指令
  - 聚合查询：hints = ["## Additional Hint: Aggregation Detected\n..."]
  - 多表+聚合：hints = [聚合提示, 多表提示] → 两条同时生效
  - 全部叠加：hints = [聚合提示, 多表提示, 嵌套提示] → 三条同时生效

**Review 检查：**
- OK 规则详细，约束了大模型的输出格式
- OK {{hints}} 实现了软策略，一个模板适应所有查询类型
- OK NO_MATCH 规则防止大模型幻觉（如把"学生"猜成"员工"）
- OK 英文提示词效果比中文好

---

### 30. correction_nl2sql.txt（纠错提示词模板 — 完整历史版）

**路径：** resources/prompts/correction_nl2sql.txt

**这个文件是干什么的？**

当大模型生成的 SQL 有错时，用这个模板构建纠错提示词。
2026-03-18 按导师要求改为支持完整历史错误上下文。

**包含三个占位符变量（2026-03-18 更新）：**
- {{schema}} — 数据库结构 DDL（和首次一样）
- {{error_history}} — 【新】完整的错误历史（所有之前失败的SQL和错误信息）
- {{question}} — 用户的原始问题

**和旧版的区别：**
  旧版：{{previous_sql}} + {{error_message}} — 只传最近一次的错误
  新版：{{error_history}} — 传入所有历史错误，格式化为：
    ### Attempt 1
    SQL: SELECT * FROM student ...
    Error Type: SYNTAX_ERROR
    Error Message: 表 'student' 不存在
    
    ### Attempt 2
    SQL: SELECT * FROM employees WHERE type='student' ...
    Error Type: EXECUTION_ERROR
    Error Message: 列 'type' 不存在

**模板规则包含 NO_MATCH 约束：**
  第9条规则和 base_nl2sql.txt 的第11条一样，纠错过程中也不允许猜测。

**Review 检查：**
- OK 完整历史让大模型看到所有之前的尝试，避免来回振荡
- OK "Do NOT repeat any of the previously failed SQL statements" 明确禁止重复
- OK 包含 NO_MATCH 约束，纠错过程中也能"醒悟"实体不存在

---

## Review 总结（阶段性）

**第1~30个文件/模块检查完毕，覆盖了后端核心逻辑。**

**整体评价：**
- 项目结构清晰，分层合理（config/model/service/exception/util/controller）
- 核心链路完整：基础架构 + LLM对接 + 自纠错 + 软策略 + 多方言 + REST API
- 代码注释详细，可读性好
- 编译零错误

**当前系统已具备的完整链路：**
  用户说中文 -> 扫描数据库结构 -> 检测查询关键词(hints) -> 构建提示词
  -> 调用大模型生成SQL -> NO_MATCH检测 -> 静态验证（语法+Schema+Levenshtein建议）
  -> 数据库执行 -> 完整历史自纠错（最多3次）-> 返回结果 + 图表可视化

---

## 第十部分：Controller 接口层（第六阶段新增）

### 31. DataSourceController.java（数据源管理接口）

**路径：** controller/DataSourceController.java

**这个文件是干什么的？**

数据源管理的"前台服务员"。接收前端/Postman 的 HTTP 请求，调用 DataSourceService 处理，返回统一格式的结果。

**通俗比喻：** Service 是厨师做菜，Controller 是服务员接单、传菜。
服务员不做菜，只负责：接收点单 -> 告诉厨师 -> 把做好的菜端给客人。

**提供6个接口（2026-03-23 新增Schema接口）：**

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/datasource | 创建数据源 |
| GET | /api/datasource | 获取所有数据源 |
| GET | /api/datasource/{id} | 根据ID获取 |
| DELETE | /api/datasource/{id} | 删除数据源 |
| GET | /api/datasource/{id}/schema | 获取数据库Schema结构（新增） |
| POST | /api/datasource/test | 测试连接 |

**关键注解讲解：**
- @RestController = @Controller + @ResponseBody，返回值自动转 JSON
- @RequestMapping("/api/datasource") — 所有接口的公共路径前缀
- @Valid — 触发 @NotBlank 等参数校验
- @RequestBody — 把请求体中的 JSON 自动转成 Java 对象
- @PathVariable — 从 URL 中提取参数（如 /api/datasource/ds-abc 中的 ds-abc）

**Review 检查：**
- OK RESTful 风格接口设计规范
- OK 所有需要校验的接口都加了 @Valid
- OK 返回统一 ApiResult 格式

---

### 32. Nl2SqlController.java（NL2SQL 核心接口）

**路径：** controller/Nl2SqlController.java

**这个文件是干什么的？**

整个系统最核心的接口！前端传一个中文问题，后端返回 SQL 和查询结果。

**只有一个接口：**

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/nl2sql | 自然语言转SQL并执行 |

**请求体格式：**
  {"dataSourceId": "ds-a1b2c3d4", "question": "查询销售额最高的前5个产品"}

**返回值包含：**
  - generatedSql — 大模型生成的 SQL
  - queryResult — SQL 执行结果（列名+数据行）
  - success — 是否成功
  - retryCount — 纠错了几次
  - correctionHistory — 纠错历史

**为什么失败也返回 HTTP 200？**
  NL2SQL 失败（如大模型生成了错误的SQL）不是"系统错误"，而是"业务结果"。
  前端应该根据 response.success 判断成功失败，而不是 HTTP 状态码。
  HTTP 500 只用于真正的系统级故障。

**Review 检查：**
- OK 接口设计简洁，只需要 dataSourceId + question
- OK 失败时也返回完整信息（包括纠错历史），方便调试

---

## 第十一部分：第四阶段新增 — 查询意图分类与动态路由

### 33. QueryType.java（查询意图类型枚举 — ⚠️ 已弃用）

**路径：** model/enums/QueryType.java

**状态：** 2026-03-18 改为软策略后，此枚举不再被核心流程使用。
QueryClassifier 现在通过 `detectHints()` 返回 `List<String>` 补充指令列表，不再依赖 QueryType 做互斥分类。
文件保留在代码中供历史参考。

---

### 34. QueryClassifier.java（查询意图检测器 — 软策略版）

**路径：** service/QueryClassifier.java

**这个文件是干什么的？**

分析用户的自然语言问题，检测其中包含的查询特征，返回补充指令列表。
使用关键词匹配（不调用大模型），速度快、免费、可控。

**2026-03-18 重大改造：从硬分类改为软策略**

旧方案：`classify(question, schema)` → 返回 QueryType 枚举（互斥四选一）
新方案：`detectHints(question, schema)` → 返回 `List<String>`（可同时叠加多种提示）

**三组关键词检测（可同时命中）：**
- 聚合关键词：多少、总数、平均、最大、最小、分组、每个、统计、最高、最低...
- 联查关键词：的订单、的产品、购买了、及其、对应的... + mentionsMultipleTables()
- 嵌套关键词：排名、前N、环比、同比、高于平均、每个.*最... (正则支持)

**举例场景：**
- "查询所有年龄大于30的员工" → hints=[] (空，基础模板够用)
- "统计每个部门的平均工资" → hints=[AGGREGATE_HINT]
- "查找订单金额超过10000的客户" → hints=[MULTI_JOIN_HINT]
- "查询销售额最高的产品类别及最好的产品" → hints=[AGGREGATE_HINT, MULTI_JOIN_HINT]
- "查询每个部门薪资最高的员工" → hints=[AGGREGATE_HINT, MULTI_JOIN_HINT, NESTED_HINT]

**核心方法：**
- `detectHints(question, schema)` — 主入口，返回补充指令列表
- `matchesAny(question, keywords)` — 正则+包含双模式匹配
- `mentionsMultipleTables(question, schema)` — 从Schema表名/注释判断是否提到2+张表

**Review 检查：**
- OK 不依赖大模型，零额外开销
- OK 软策略避免了硬分类误判的问题
- OK 多种提示可同时叠加，处理复杂查询更准确
- OK Schema辅助判断提高了多表检测的准确性

---

### 35-38. 四套专用提示词模板（已删除）

2026-03-18 按导师反馈改为软策略后，这四套模板（simple/aggregate/multijoin/nested）已从项目中删除。
所有查询统一使用 base_nl2sql.txt + {{hints}} 动态注入。

---

## 第十二部分：第五阶段新增 — 多数据库方言适配

### 核心改动说明

不是新增文件，而是对已有文件的增强：

**DatabaseSchema.java 改动：**
- 新增 dbType 字段（如 "MySQL"、"PostgreSQL"）
- formatAsDDL() 输出头部增加类型标识（如 "-- 数据库: test_db (MySQL)"）

**SchemaExtractService.java 改动：**
- 用 metaData.getDatabaseProductName() 自动检测数据库类型
- 构建 Schema 时自动填入 dbType

**PromptTemplateService.java 改动：**
- buildPromptByTemplateName 新增带 dbType 的重载方法
- 如果 dbType 不为空，自动在提示词末尾追加方言提示

**Nl2SqlService.java 改动：**
- 调用提示词构建时传入 schema.getDbType()

**效果：** 大模型现在会知道目标数据库是 MySQL 还是 PostgreSQL，生成对应方言的 SQL。

**Review 检查：**
- OK 自动检测，不需要用户手动指定
- OK 方言提示追加在末尾，不破坏原有模板结构
- OK 无 dbType 时优雅降级（不追加任何内容）

---

## 最终 Review 总结（2026-03-24 更新）

**项目状态：全部开发完成，进入论文撰写阶段。编译零错误，测试全通过。**

**后端已完成功能（Java 17 + Spring Boot 3.2.5）：**
1. 基础架构 — 动态数据源管理(HikariCP)、Schema自动扫描(含字段类型+注释+外键)
2. LLM对接 — DeepSeek大模型调用(LangChain4j)、提示词模板管理
3. 自纠错 — 静态验证(JSQLParser语法+Schema表名列名+Levenshtein建议) + 动态验证(数据库执行) + 完整历史反馈闭环(最多3次)
4. 软策略 — 基础模板兜底 + QueryClassifier.detectHints() 动态追加补充指令
5. 多方言 — 自动检测数据库类型 + 方言提示注入
6. NO_MATCH — 不存在的实体不猜测，返回明确拒绝
7. REST API — DataSourceController(6个) + Nl2SqlController(1个) + DebugController(4个)
8. Schema API — GET /api/datasource/{id}/schema 返回结构化Schema JSON
9. 数据源连接保护 — 创建数据源时先注册连接池再存内存，连接失败返回友好提示
10. 安全限制 — 只允许SELECT、超时30s、最大1000行

**前端已完成功能（Vue 3.4 + Vite 5）：**
1. Gemini风格界面 — 纯白+Google蓝配色
2. 聊天历史 — 多会话管理，侧边栏切换/删除
3. Schema预览 — 右侧抽屉面板，结构化展示表/列/主键/外键
4. 图表可视化 — ECharts智能分析数据 → 柱状图/折线图/饼图自动切换
5. API拦截器 — 正确处理后端统一响应格式(ApiResult)

**测试结果：**
- test_v2.js 精确验证 27/27 通过（结果正确性对比标准答案）
- test_v3.js 校验机制专项 28/28 通过（静态校验 + 动态执行 + 多轮纠错流水线）

---

## 第十三部分：DebugController（校验调试接口）

### 36. DebugController.java

**路径：** controller/DebugController.java

**这个文件是干什么的？**

专门为测试和调试设计的接口，可以绕过大模型直接测试校验和执行功能。
test_v3.js 的静态校验测试和流水线测试就是通过这些接口实现的。

**提供4个接口：**

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/debug/validate | 直接测试静态校验（手动送SQL进来） |
| POST | /api/debug/execute | 直接测试SQL执行（跳过静态校验） |
| POST | /api/debug/full-pipeline | 测试完整流水线（模拟多轮纠错） |
| GET | /api/debug/schema/{dsId} | 查看提取的Schema DDL文本 |

**full-pipeline 接口详解：**
接收一个 `sqlSequence`（SQL列表），模拟大模型每轮生成的SQL。
系统按顺序对每个SQL执行"静态校验→数据库执行"，失败就取下一个SQL，直到成功或用完。
返回每轮的步骤详情（attempt/stage/passed/errorType/errorMessage），完整记录纠错过程。

**内部DTO（用Lombok @Data定义的静态内部类）：**
- ValidateRequest — dataSourceId + sql
- ExecuteRequest — dataSourceId + sql
- PipelineRequest — dataSourceId + sqlSequence(List<String>)
- ValidateResponse — sql + valid + errorMessage + errorCategory
- PipelineResponse — steps(List<StepResult>) + finalSuccess + finalSql + queryResult + totalRetries
- StepResult — attempt + sql + stage + passed + errorType + errorMessage

**Review 检查：**
- OK 绕过大模型直接测试，非常适合论文中展示自验证机制效果
- OK full-pipeline 完整模拟了纠错循环的每一步，数据可直接用于论文
- OK 安全检查仍保留（execute接口仍然只允许SELECT）

---

## 第十四部分：近期完善与修复（2026-03-23~24）

### DataSourceController 新增 Schema API

**改动文件：** controller/DataSourceController.java

新增 `GET /api/datasource/{id}/schema` 接口，注入 SchemaExtractService，
返回结构化的 DatabaseSchema JSON（包含表名、列名、数据类型、主键、外键等完整信息）。
前端 SchemaDrawer 抽屉面板调用此接口展示数据库结构。

接口总数从5个增加到6个：
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/datasource | 创建数据源 |
| GET | /api/datasource | 获取所有数据源 |
| GET | /api/datasource/{id} | 根据ID获取 |
| DELETE | /api/datasource/{id} | 删除数据源 |
| GET | /api/datasource/{id}/schema | **新增**：获取Schema结构 |
| POST | /api/datasource/test | 测试连接 |

---

### DataSourceService 连接保护修复

**改动文件：** service/DataSourceService.java

**旧逻辑（有bug）：**
```
create() {
    dataSourceStore.put(id, info);    // 先存内存
    dataSourceManager.register(info);  // 再注册连接池（可能失败）
}
```
问题：如果数据库名写错（如 `n2sql_tset`），register() 抛异常，但数据源已存入内存。
后续对这个"幽灵数据源"的任何操作都会报错。

**新逻辑（修复后）：**
```
create() {
    try {
        dataSourceManager.register(info);  // 先注册连接池
    } catch (Exception e) {
        throw new BizException("数据源连接失败: " + rootMsg);  // 失败不存内存
    }
    dataSourceStore.put(id, info);  // 成功后才存内存
}
```
现在用户填错数据库名/密码时，收到友好的错误提示（如"数据源连接失败: Unknown database 'n2sql_tset'"），
而不是 500 内部错误。且错误的数据源不会污染内存。

---

## 第十五部分：前端文件结构（Vue 3 + Vite，不做详细讲解）

```
frontend/src/
├── main.js                 ← Vue应用入口
├── App.vue                 ← 主页面（聊天历史管理+Schema按钮+消息区+输入区）
├── api/
│   └── index.js            ← Axios封装（7个API：数据源5个+NL2SQL+Schema）
└── components/
    ├── Sidebar.vue          ← 侧边栏（品牌+New Chat+聊天历史列表+数据源列表）
    ├── ChatMessage.vue      ← 消息组件（SQL高亮+结果表格+图表+纠错提示）
    ├── ChartDisplay.vue     ← ECharts图表可视化（柱状图/折线图/饼图自动切换）
    ├── DataSourceModal.vue  ← 添加数据源弹窗（表单+测试连接+保存）
    └── SchemaDrawer.vue     ← Schema预览抽屉面板（表/列/主键/外键可视化）
```

**前端技术栈：**
| 技术 | 版本 | 作用 |
|------|------|------|
| Vue 3 | 3.4 | 前端框架（Composition API + setup语法糖） |
| Vite | 5.1 | 构建工具（开发热更新+生产打包） |
| Axios | 1.6 | HTTP 请求库（统一拦截器处理ApiResult格式） |
| highlight.js | 11.9 | SQL 语法高亮 |
| ECharts (vue-echarts) | 6.0 | 图表可视化 |

**配色方案：** Gemini 风格（纯白 #ffffff + Google蓝 #1a73e8 + 浅灰侧栏 #E8EEF4）

