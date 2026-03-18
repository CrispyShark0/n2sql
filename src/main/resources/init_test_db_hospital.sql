-- ========================================
-- NL2SQL 测试数据库 — 医院管理系统
-- 用于测试日期密集型查询、NULL处理、复杂关联
-- ========================================

DROP DATABASE IF EXISTS n2sql_test_hospital;
CREATE DATABASE n2sql_test_hospital DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE n2sql_test_hospital;

-- ========================================
-- 1. 科室表 (departments)
-- ========================================
CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '科室ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '科室名称',
    floor INT COMMENT '所在楼层',
    phone VARCHAR(20) COMMENT '联系电话'
) COMMENT='科室表';

INSERT INTO departments (dept_name, floor, phone) VALUES
('内科', 2, '0571-88001001'),
('外科', 3, '0571-88001002'),
('儿科', 4, '0571-88001003'),
('妇产科', 5, '0571-88001004'),
('眼科', 2, '0571-88001005'),
('口腔科', 1, '0571-88001006'),
('急诊科', 1, '0571-88001007');

-- ========================================
-- 2. 医生表 (doctors)
-- ========================================
CREATE TABLE doctors (
    doctor_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '医生ID',
    doctor_name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    gender ENUM('男', '女') NOT NULL COMMENT '性别',
    dept_id INT COMMENT '所属科室ID',
    title VARCHAR(50) COMMENT '职称',
    specialty VARCHAR(100) COMMENT '专长方向',
    hire_date DATE COMMENT '入职日期',
    consultation_fee DECIMAL(8, 2) COMMENT '挂号费',
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) COMMENT='医生表';

INSERT INTO doctors (doctor_name, gender, dept_id, title, specialty, hire_date, consultation_fee) VALUES
('张主任', '男', 1, '主任医师', '心血管疾病', '2005-03-01', 300.00),
('李副主任', '女', 1, '副主任医师', '消化系统疾病', '2010-09-01', 200.00),
('王医生', '男', 1, '主治医师', '呼吸系统疾病', '2016-07-01', 100.00),
('赵主任', '男', 2, '主任医师', '骨科手术', '2003-09-01', 350.00),
('钱医生', '女', 2, '主治医师', '普通外科', '2015-03-01', 120.00),
('孙主任', '女', 3, '主任医师', '儿童呼吸道疾病', '2008-09-01', 250.00),
('周医生', '男', 3, '主治医师', '新生儿护理', '2018-03-01', 100.00),
('吴主任', '女', 4, '主任医师', '高危妊娠', '2006-09-01', 300.00),
('郑医生', '女', 4, '主治医师', '妇科常见病', '2017-09-01', 120.00),
('冯医生', '男', 5, '副主任医师', '白内障手术', '2012-03-01', 180.00),
('陈医生', '女', 6, '主治医师', '牙齿矫正', '2019-09-01', 100.00),
('何医生', '男', 7, '副主任医师', '急危重症', '2011-09-01', 150.00);

-- ========================================
-- 3. 患者表 (patients)
-- ========================================
CREATE TABLE patients (
    patient_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '患者ID',
    patient_name VARCHAR(50) NOT NULL COMMENT '患者姓名',
    gender ENUM('男', '女') NOT NULL COMMENT '性别',
    birth_date DATE COMMENT '出生日期',
    phone VARCHAR(20) COMMENT '联系电话',
    blood_type ENUM('A', 'B', 'AB', 'O') COMMENT '血型',
    allergy_info VARCHAR(200) COMMENT '过敏信息',
    insurance_type ENUM('医保', '商保', '自费') DEFAULT '自费' COMMENT '医保类型',
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间'
) COMMENT='患者表';

INSERT INTO patients (patient_name, gender, birth_date, phone, blood_type, allergy_info, insurance_type) VALUES
('刘大爷', '男', '1955-03-12', '13900001111', 'A', '青霉素过敏', '医保'),
('陈阿姨', '女', '1960-07-25', '13900002222', 'B', NULL, '医保'),
('张先生', '男', '1985-11-08', '13900003333', 'O', NULL, '商保'),
('李女士', '女', '1990-04-18', '13900004444', 'AB', '海鲜过敏', '商保'),
('王宝宝', '男', '2022-06-01', '13900005555', 'A', NULL, '医保'),
('赵宝宝', '女', '2023-01-15', '13900006666', 'O', '牛奶过敏', '医保'),
('孙先生', '男', '1978-09-20', '13900007777', 'B', '磺胺类药物过敏', '自费'),
('周女士', '女', '1995-02-28', '13900008888', 'A', NULL, '医保'),
('吴老先生', '男', '1948-12-05', '13900009999', 'AB', '阿司匹林过敏', '医保'),
('郑女士', '女', '1988-08-14', '13900000000', 'O', NULL, '商保'),
('钱先生', '男', '1975-05-30', '13900001100', 'A', NULL, '自费'),
('冯女士', '女', '2000-10-10', '13900002200', 'B', '花粉过敏', '医保');

-- ========================================
-- 4. 挂号与就诊表 (appointments)
-- ========================================
CREATE TABLE appointments (
    appointment_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '挂号ID',
    patient_id INT NOT NULL COMMENT '患者ID',
    doctor_id INT NOT NULL COMMENT '医生ID',
    appointment_date DATE NOT NULL COMMENT '就诊日期',
    time_slot ENUM('上午', '下午') NOT NULL COMMENT '时段',
    status ENUM('已预约', '已就诊', '已取消', '爽约') DEFAULT '已预约' COMMENT '状态',
    diagnosis VARCHAR(200) COMMENT '诊断结果',
    total_cost DECIMAL(10, 2) COMMENT '总费用',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
) COMMENT='挂号与就诊表';

INSERT INTO appointments (patient_id, doctor_id, appointment_date, time_slot, status, diagnosis, total_cost) VALUES
-- 2024年就诊记录
(1, 1, '2024-01-10', '上午', '已就诊', '高血压二级', 850.00),
(1, 1, '2024-03-15', '上午', '已就诊', '高血压复查，控制良好', 500.00),
(2, 2, '2024-01-20', '下午', '已就诊', '慢性胃炎', 620.00),
(3, 4, '2024-02-05', '上午', '已就诊', '腰椎间盘突出', 3500.00),
(4, 8, '2024-02-18', '上午', '已就诊', '孕检正常', 1200.00),
(5, 6, '2024-03-01', '上午', '已就诊', '上呼吸道感染', 350.00),
(6, 6, '2024-03-10', '下午', '已就诊', '轮状病毒腹泻', 480.00),
(7, 4, '2024-04-01', '上午', '已就诊', '右臂骨折', 8500.00),
(8, 9, '2024-04-15', '下午', '已就诊', '月经不调', 380.00),
(9, 1, '2024-05-01', '上午', '已就诊', '冠心病', 2200.00),
(9, 1, '2024-06-01', '上午', '已就诊', '冠心病复查', 800.00),
(10, 5, '2024-06-15', '下午', '已就诊', '阑尾炎手术', 12000.00),
(11, 10, '2024-07-01', '上午', '已就诊', '白内障初期', 500.00),
(12, 11, '2024-07-20', '下午', '已就诊', '智齿发炎', 300.00),
(3, 12, '2024-08-05', '上午', '已就诊', '急性肠胃炎', 650.00),
(1, 1, '2024-09-10', '上午', '已就诊', '高血压复查', 500.00),
(4, 8, '2024-09-25', '上午', '已就诊', '孕晚期检查', 1500.00),
-- 2025年就诊记录
(1, 1, '2025-01-08', '上午', '已就诊', '高血压复查', 500.00),
(2, 2, '2025-01-15', '下午', '已就诊', '胃镜检查', 1200.00),
(5, 7, '2025-02-01', '上午', '已就诊', '儿童体检', 200.00),
(6, 6, '2025-02-10', '下午', '已就诊', '感冒发烧', 280.00),
(9, 1, '2025-02-20', '上午', '已就诊', '冠心病复查', 900.00),
(3, 3, '2025-03-01', '上午', '已就诊', '支气管炎', 450.00),
(7, 4, '2025-03-10', '上午', '已就诊', '骨折复查', 600.00),
(8, 9, '2025-03-15', '下午', '已预约', NULL, NULL),
(11, 10, '2025-03-20', '上午', '已预约', NULL, NULL),
(12, 11, '2025-03-25', '下午', '已预约', NULL, NULL),
(10, 2, '2025-02-28', '下午', '已取消', NULL, NULL),
(4, 8, '2025-01-20', '上午', '爽约', NULL, NULL);

-- ========================================
-- 5. 处方表 (prescriptions)
-- ========================================
CREATE TABLE prescriptions (
    prescription_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '处方ID',
    appointment_id INT NOT NULL COMMENT '就诊记录ID',
    medicine_name VARCHAR(100) NOT NULL COMMENT '药品名称',
    dosage VARCHAR(50) COMMENT '用法用量',
    quantity INT COMMENT '数量',
    unit_price DECIMAL(8, 2) COMMENT '单价',
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
) COMMENT='处方表';

INSERT INTO prescriptions (appointment_id, medicine_name, dosage, quantity, unit_price) VALUES
-- 刘大爷-高血压
(1, '氨氯地平片', '每日一次，每次5mg', 30, 3.50),
(1, '阿托伐他汀钙片', '每晚一次，每次10mg', 30, 5.00),
(2, '氨氯地平片', '每日一次，每次5mg', 30, 3.50),
-- 陈阿姨-胃炎
(3, '奥美拉唑肠溶胶囊', '每日两次，每次20mg', 28, 2.80),
(3, '铝碳酸镁咀嚼片', '饭后嚼服，每次1-2片', 42, 1.50),
-- 张先生-腰椎
(4, '塞来昔布胶囊', '每日一次，每次200mg', 14, 8.00),
(4, '甲钴胺片', '每日三次，每次0.5mg', 42, 2.00),
-- 王宝宝-感冒
(6, '小儿氨酚黄那敏颗粒', '每次半包，每日三次', 9, 3.00),
-- 赵宝宝-腹泻
(7, '蒙脱石散', '每次半包，每日三次', 9, 4.50),
(7, '口服补液盐III', '少量多次口服', 6, 2.00),
-- 孙先生-骨折
(8, '骨肽注射液', '静脉滴注，每日一次', 7, 35.00),
-- 周女士-月经不调
(9, '乌鸡白凤丸', '每日两次，每次6g', 20, 8.50),
-- 吴老先生-冠心病
(10, '硝酸甘油片', '舌下含服，必要时使用', 30, 1.50),
(10, '阿托伐他汀钙片', '每晚一次，每次20mg', 30, 5.00),
(11, '硝酸甘油片', '舌下含服，必要时使用', 30, 1.50),
-- 2025年处方
(18, '氨氯地平片', '每日一次，每次5mg', 30, 3.50),
(19, '奥美拉唑肠溶胶囊', '每日一次，每次20mg', 14, 2.80),
(21, '小儿布洛芬混悬液', '发热时口服', 1, 25.00),
(22, '硝酸甘油片', '舌下含服', 30, 1.50),
(23, '头孢克洛缓释胶囊', '每日两次，每次0.375g', 14, 6.00);

-- ========================================
-- 验证数据
-- ========================================
SELECT '医院数据库创建完成！' AS message;
SELECT '科室数量' AS item, COUNT(*) AS count FROM departments
UNION ALL SELECT '医生数量', COUNT(*) FROM doctors
UNION ALL SELECT '患者数量', COUNT(*) FROM patients
UNION ALL SELECT '就诊记录数量', COUNT(*) FROM appointments
UNION ALL SELECT '处方记录数量', COUNT(*) FROM prescriptions;
