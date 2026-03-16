# NL2SQL 前端指南 (Vue 3 + Element Plus)

这是基于大模型的自然语言数据库查询系统（NL2SQL）的前端工程，采用 Vue 3 和 Element Plus 开发。

---

## 🛠️ 环境准备

1.  请确保已安装 **Node.js** (建议版本 v16+)。
2.  在当前目录 (`frontend`) 下打开终端。
3.  安装依赖：
    ```bash
    npm install
    ```

---

## ▶️ 开发运行

启动开发服务器：

```bash
npm run dev
```

启动后，前端通常运行在 `http://localhost:5173`。
系统已配置代理，API 请求会自动转发到 `http://localhost:8080` (你的 Spring Boot 后端)。

---

## 📦 生产构建

如需打包发布：

```bash
npm run build
```

---

## 💡 使用教程：如何添加数据源

### 第一步：准备测试数据库 (如果你还没有)
项目根目录下提供了一个初始化脚本 `src/main/resources/init_test_db.sql`。
请使用 Navicat、DBeaver 或命令行连接你的 MySQL，并将该 SQL 文件导入，它会创建一个名为 `n2sql_test` 的数据库和测试表（部门表、员工表等）。

### 第二步：在前端填写信息
1.  启动前后端。
2.  在左侧点击 **`+`** 按钮，弹出 "Add Data Source" 窗口。
3.  按如下填写：

| 字段 | 说明 | 建议填写值 |
| :--- | :--- | :--- |
| **Name** | 给数据源起个名 | `测试数据库` |
| **Type** | 数据库类型 | 选择 `MySQL` |
| **Host** | 主机地址 | `localhost` (本机) |
| **Port** | 端口号 | `3306` (默认) |
| **DB Name** | 数据库名称 | `n2sql_test` (如果你用了上一步的脚本) |
| **Username** | 数据库账号 | `root` (你的MySQL账号) |
| **Password** | 数据库密码 | `040826` (你的MySQL密码) |

4.  点击 **Test Connection** 测试连接。
5.  成功后点击 **Save** 保存。

### 第三步：开始提问
连接成功后，在左侧点击刚添加的数据源，然后在右侧输入框提问，例如：
> "查询薪水最高的5名员工"
> "统计每个部门的平均工资"

