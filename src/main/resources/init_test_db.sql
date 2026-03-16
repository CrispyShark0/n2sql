-- ========================================
-- NL2SQL 测试数据库初始化脚本
-- 场景：电商销售系统（便于测试各种复杂查询）
-- ========================================

-- 创建数据库
DROP DATABASE IF EXISTS n2sql_test;
CREATE DATABASE n2sql_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE n2sql_test;

-- ========================================
-- 1. 部门表 (departments)
-- ========================================
CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    manager_name VARCHAR(50) COMMENT '部门经理',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='部门表';

INSERT INTO departments (dept_name, manager_name) VALUES
('销售部', '王强'),
('市场部', '李娜'),
('技术部', '张伟'),
('财务部', '赵敏'),
('人事部', '陈静');

-- ========================================
-- 2. 员工表 (employees)
-- ========================================
CREATE TABLE employees (
    emp_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    gender ENUM('男', '女') NOT NULL COMMENT '性别',
    age INT COMMENT '年龄',
    dept_id INT COMMENT '所属部门ID',
    salary DECIMAL(10, 2) COMMENT '月薪',
    hire_date DATE COMMENT '入职日期',
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) COMMENT='员工表';

INSERT INTO employees (emp_name, gender, age, dept_id, salary, hire_date) VALUES
('王强', '男', 42, 1, 25000.00, '2018-03-15'),
('刘洋', '男', 30, 1, 12000.00, '2020-06-01'),
('张丽', '女', 28, 1, 11000.00, '2021-01-10'),
('周明', '男', 35, 1, 15000.00, '2019-08-20'),
('李娜', '女', 38, 2, 22000.00, '2017-05-22'),
('孙悦', '女', 26, 2, 9500.00, '2022-03-15'),
('吴磊', '男', 32, 2, 13000.00, '2020-11-01'),
('张伟', '男', 40, 3, 30000.00, '2016-01-05'),
('陈晨', '男', 29, 3, 18000.00, '2021-07-15'),
('林小红', '女', 27, 3, 16000.00, '2022-01-20'),
('黄杰', '男', 31, 3, 17000.00, '2020-09-10'),
('赵敏', '女', 45, 4, 20000.00, '2015-06-01'),
('钱多多', '男', 33, 4, 14000.00, '2019-12-15'),
('陈静', '女', 36, 5, 18000.00, '2018-10-01'),
('何芳', '女', 25, 5, 8500.00, '2023-02-14');

-- ========================================
-- 3. 客户表 (customers)
-- ========================================
CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    city VARCHAR(50) COMMENT '所在城市',
    customer_level ENUM('普通', 'VIP', 'SVIP') DEFAULT '普通' COMMENT '客户等级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) COMMENT='客户表';

INSERT INTO customers (customer_name, contact_phone, city, customer_level, created_at) VALUES
('北京科技有限公司', '13800001111', '北京', 'SVIP', '2022-01-15 10:00:00'),
('上海贸易公司', '13800002222', '上海', 'VIP', '2022-03-20 14:30:00'),
('广州制造集团', '13800003333', '广州', 'VIP', '2022-05-10 09:15:00'),
('深圳创新科技', '13800004444', '深圳', 'SVIP', '2022-02-28 16:45:00'),
('杭州电商公司', '13800005555', '杭州', '普通', '2023-01-05 11:20:00'),
('成都软件公司', '13800006666', '成都', '普通', '2023-03-12 08:00:00'),
('武汉教育机构', '13800007777', '武汉', 'VIP', '2022-08-18 13:10:00'),
('南京医药集团', '13800008888', '南京', '普通', '2023-06-22 15:30:00'),
('重庆物流公司', '13800009999', '重庆', '普通', '2023-09-01 10:45:00'),
('天津食品公司', '13800000000', '天津', 'VIP', '2022-11-30 09:00:00');

-- ========================================
-- 4. 产品表 (products)
-- ========================================
CREATE TABLE products (
    product_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    category VARCHAR(50) COMMENT '产品类别',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    stock_quantity INT DEFAULT 0 COMMENT '库存数量',
    status ENUM('在售', '下架', '缺货') DEFAULT '在售' COMMENT '产品状态'
) COMMENT='产品表';

INSERT INTO products (product_name, category, unit_price, stock_quantity, status) VALUES
('企业管理系统', '软件', 50000.00, 999, '在售'),
('数据分析平台', '软件', 80000.00, 999, '在售'),
('云服务器-基础版', '云服务', 3000.00, 500, '在售'),
('云服务器-高级版', '云服务', 8000.00, 200, '在售'),
('网络安全套件', '安全', 15000.00, 100, '在售'),
('智能办公终端', '硬件', 6000.00, 50, '在售'),
('视频会议系统', '软件', 25000.00, 999, '在售'),
('数据备份服务', '云服务', 2000.00, 999, '在售'),
('AI客服机器人', '软件', 35000.00, 999, '在售'),
('物联网传感器套装', '硬件', 12000.00, 30, '缺货');

-- ========================================
-- 5. 订单表 (orders)
-- ========================================
CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    customer_id INT NOT NULL COMMENT '客户ID',
    emp_id INT COMMENT '负责销售员工ID',
    order_date DATE NOT NULL COMMENT '订单日期',
    total_amount DECIMAL(12, 2) COMMENT '订单总金额',
    status ENUM('待付款', '已付款', '已发货', '已完成', '已取消') DEFAULT '待付款' COMMENT '订单状态',
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
) COMMENT='订单表';

INSERT INTO orders (customer_id, emp_id, order_date, total_amount, status) VALUES
-- 2024年订单
(1, 1, '2024-01-15', 130000.00, '已完成'),
(2, 2, '2024-01-20', 80000.00, '已完成'),
(3, 3, '2024-02-10', 50000.00, '已完成'),
(4, 1, '2024-02-28', 96000.00, '已完成'),
(5, 4, '2024-03-05', 3000.00, '已完成'),
(1, 2, '2024-03-15', 35000.00, '已完成'),
(7, 3, '2024-04-01', 25000.00, '已完成'),
(2, 1, '2024-04-20', 50000.00, '已完成'),
(6, 4, '2024-05-10', 15000.00, '已完成'),
(8, 2, '2024-05-25', 6000.00, '已完成'),
(4, 1, '2024-06-01', 80000.00, '已完成'),
(3, 3, '2024-06-15', 25000.00, '已完成'),
(10, 4, '2024-07-01', 8000.00, '已付款'),
(1, 1, '2024-07-10', 50000.00, '已付款'),
(9, 2, '2024-07-20', 3000.00, '已发货'),
-- 2025年订单
(2, 1, '2025-01-10', 160000.00, '已完成'),
(4, 2, '2025-01-18', 50000.00, '已完成'),
(1, 3, '2025-02-05', 80000.00, '已完成'),
(3, 1, '2025-02-20', 35000.00, '已完成'),
(7, 4, '2025-03-01', 12000.00, '已完成'),
(5, 2, '2025-03-15', 8000.00, '已付款'),
(6, 3, '2025-04-01', 50000.00, '已付款'),
(8, 1, '2025-04-10', 25000.00, '已发货'),
(10, 4, '2025-05-01', 6000.00, '待付款'),
(9, 2, '2025-05-15', 15000.00, '待付款');

-- ========================================
-- 6. 订单明细表 (order_items)
-- ========================================
CREATE TABLE order_items (
    item_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    order_id INT NOT NULL COMMENT '订单ID',
    product_id INT NOT NULL COMMENT '产品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '成交单价',
    subtotal DECIMAL(12, 2) COMMENT '小计金额',
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
) COMMENT='订单明细表';

INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES
-- 订单1: 企业管理系统 + 数据分析平台
(1, 1, 1, 50000.00, 50000.00),
(1, 2, 1, 80000.00, 80000.00),
-- 订单2: 数据分析平台
(2, 2, 1, 80000.00, 80000.00),
-- 订单3: 企业管理系统
(3, 1, 1, 50000.00, 50000.00),
-- 订单4: 云服务器高级版 x12
(4, 4, 12, 8000.00, 96000.00),
-- 订单5: 云服务器基础版
(5, 3, 1, 3000.00, 3000.00),
-- 订单6: AI客服机器人
(6, 9, 1, 35000.00, 35000.00),
-- 订单7: 视频会议系统
(7, 7, 1, 25000.00, 25000.00),
-- 订单8: 企业管理系统
(8, 1, 1, 50000.00, 50000.00),
-- 订单9: 网络安全套件
(9, 5, 1, 15000.00, 15000.00),
-- 订单10: 智能办公终端
(10, 6, 1, 6000.00, 6000.00),
-- 订单11: 数据分析平台
(11, 2, 1, 80000.00, 80000.00),
-- 订单12: 视频会议系统
(12, 7, 1, 25000.00, 25000.00),
-- 订单13: 云服务器高级版
(13, 4, 1, 8000.00, 8000.00),
-- 订单14: 企业管理系统
(14, 1, 1, 50000.00, 50000.00),
-- 订单15: 云服务器基础版
(15, 3, 1, 3000.00, 3000.00),
-- 2025年订单明细
-- 订单16: 企业管理系统 + 数据分析平台 + 数据备份
(16, 1, 1, 50000.00, 50000.00),
(16, 2, 1, 80000.00, 80000.00),
(16, 8, 15, 2000.00, 30000.00),
-- 订单17: 企业管理系统
(17, 1, 1, 50000.00, 50000.00),
-- 订单18: 数据分析平台
(18, 2, 1, 80000.00, 80000.00),
-- 订单19: AI客服机器人
(19, 9, 1, 35000.00, 35000.00),
-- 订单20: 物联网传感器
(20, 10, 1, 12000.00, 12000.00),
-- 订单21: 云服务器高级版
(21, 4, 1, 8000.00, 8000.00),
-- 订单22: 企业管理系统
(22, 1, 1, 50000.00, 50000.00),
-- 订单23: 视频会议系统
(23, 7, 1, 25000.00, 25000.00),
-- 订单24: 智能办公终端
(24, 6, 1, 6000.00, 6000.00),
-- 订单25: 网络安全套件
(25, 5, 1, 15000.00, 15000.00);

-- ========================================
-- 验证数据
-- ========================================
SELECT '数据库创建完成！' AS message;
SELECT '部门数量' AS item, COUNT(*) AS count FROM departments
UNION ALL
SELECT '员工数量', COUNT(*) FROM employees
UNION ALL
SELECT '客户数量', COUNT(*) FROM customers
UNION ALL
SELECT '产品数量', COUNT(*) FROM products
UNION ALL
SELECT '订单数量', COUNT(*) FROM orders
UNION ALL
SELECT '订单明细数量', COUNT(*) FROM order_items;
