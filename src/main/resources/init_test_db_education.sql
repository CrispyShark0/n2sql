-- ========================================
-- NL2SQL 测试数据库 — 教育管理系统
-- 用于测试系统在不同业务场景下的适配能力
-- ========================================

DROP DATABASE IF EXISTS n2sql_test_edu;
CREATE DATABASE n2sql_test_edu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE n2sql_test_edu;

-- ========================================
-- 1. 院系表 (departments)
-- ========================================
CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '院系ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '院系名称',
    dean_name VARCHAR(50) COMMENT '院长姓名',
    established_year INT COMMENT '成立年份',
    location VARCHAR(100) COMMENT '办公地点'
) COMMENT='院系表';

INSERT INTO departments (dept_name, dean_name, established_year, location) VALUES
('计算机科学与技术学院', '张建国', 1985, '信息楼A座'),
('数学与统计学院', '李明华', 1958, '理科楼B座'),
('外国语学院', '王丽英', 1978, '文科楼C座'),
('经济管理学院', '陈伟明', 1992, '商学楼D座'),
('人工智能学院', '刘思远', 2019, '科创楼E座');

-- ========================================
-- 2. 教师表 (teachers)
-- ========================================
CREATE TABLE teachers (
    teacher_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '教师ID',
    teacher_name VARCHAR(50) NOT NULL COMMENT '教师姓名',
    gender ENUM('男', '女') NOT NULL COMMENT '性别',
    title VARCHAR(50) COMMENT '职称',
    dept_id INT COMMENT '所属院系ID',
    salary DECIMAL(10, 2) COMMENT '月薪',
    hire_date DATE COMMENT '入职日期',
    email VARCHAR(100) COMMENT '邮箱',
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) COMMENT='教师表';

INSERT INTO teachers (teacher_name, gender, title, dept_id, salary, hire_date, email) VALUES
('张建国', '男', '教授', 1, 25000.00, '2000-09-01', 'zhangjg@edu.cn'),
('赵秀芳', '女', '副教授', 1, 18000.00, '2008-03-15', 'zhaoxf@edu.cn'),
('王大力', '男', '讲师', 1, 12000.00, '2015-07-01', 'wangdl@edu.cn'),
('孙小红', '女', '助教', 1, 8000.00, '2022-09-01', 'sunxh@edu.cn'),
('李明华', '男', '教授', 2, 23000.00, '1998-09-01', 'limh@edu.cn'),
('周婷婷', '女', '副教授', 2, 17000.00, '2010-09-01', 'zhoutt@edu.cn'),
('吴强', '男', '讲师', 2, 11500.00, '2016-03-01', 'wuqiang@edu.cn'),
('王丽英', '女', '教授', 3, 22000.00, '2002-09-01', 'wangly@edu.cn'),
('陈杰', '男', '副教授', 3, 16000.00, '2012-09-01', 'chenjie@edu.cn'),
('陈伟明', '男', '教授', 4, 24000.00, '2001-03-01', 'chenwm@edu.cn'),
('林芳', '女', '副教授', 4, 17500.00, '2011-09-01', 'linfang@edu.cn'),
('黄志强', '男', '讲师', 4, 12500.00, '2018-09-01', 'huangzq@edu.cn'),
('刘思远', '男', '教授', 5, 28000.00, '2019-09-01', 'liusy@edu.cn'),
('杨晓梅', '女', '副教授', 5, 20000.00, '2020-03-01', 'yangxm@edu.cn'),
('何磊', '男', '讲师', 5, 15000.00, '2021-09-01', 'helei@edu.cn');

-- ========================================
-- 3. 学生表 (students)
-- ========================================
CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    gender ENUM('男', '女') NOT NULL COMMENT '性别',
    birth_date DATE COMMENT '出生日期',
    dept_id INT COMMENT '所属院系ID',
    grade_year INT COMMENT '入学年份',
    gpa DECIMAL(3, 2) COMMENT '绩点(满分4.0)',
    scholarship ENUM('无', '三等', '二等', '一等', '国奖') DEFAULT '无' COMMENT '奖学金等级',
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) COMMENT='学生表';

INSERT INTO students (student_name, gender, birth_date, dept_id, grade_year, gpa, scholarship) VALUES
('张小明', '男', '2003-05-12', 1, 2021, 3.85, '一等'),
('李婷', '女', '2003-08-23', 1, 2021, 3.92, '国奖'),
('王浩', '男', '2004-01-15', 1, 2022, 3.45, '二等'),
('赵雪', '女', '2004-03-20', 1, 2022, 3.60, '二等'),
('刘杰', '男', '2004-11-08', 2, 2022, 3.78, '一等'),
('陈美丽', '女', '2003-07-30', 2, 2021, 3.50, '三等'),
('孙伟', '男', '2005-02-14', 2, 2023, 3.20, '无'),
('周倩', '女', '2003-12-01', 3, 2021, 3.65, '二等'),
('吴涛', '男', '2004-06-18', 3, 2022, 2.90, '无'),
('郑丽丽', '女', '2005-04-25', 3, 2023, 3.30, '三等'),
('何强', '男', '2003-09-10', 4, 2021, 3.70, '一等'),
('林小芳', '女', '2004-10-05', 4, 2022, 3.55, '三等'),
('黄明', '男', '2005-07-22', 4, 2023, 3.10, '无'),
('杨洋', '男', '2004-08-16', 5, 2022, 3.88, '国奖'),
('马丽', '女', '2005-01-28', 5, 2023, 3.40, '三等');

-- ========================================
-- 4. 课程表 (courses)
-- ========================================
CREATE TABLE courses (
    course_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    dept_id INT COMMENT '开课院系ID',
    teacher_id INT COMMENT '授课教师ID',
    credits DECIMAL(3, 1) NOT NULL COMMENT '学分',
    course_type ENUM('必修', '选修', '公共') NOT NULL COMMENT '课程类型',
    max_students INT DEFAULT 60 COMMENT '最大选课人数',
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)
) COMMENT='课程表';

INSERT INTO courses (course_name, dept_id, teacher_id, credits, course_type, max_students) VALUES
('数据结构与算法', 1, 1, 4.0, '必修', 120),
('操作系统原理', 1, 2, 3.5, '必修', 100),
('数据库系统概论', 1, 3, 3.0, '必修', 100),
('人工智能导论', 1, 4, 2.0, '选修', 80),
('高等数学A', 2, 5, 5.0, '公共', 200),
('线性代数', 2, 6, 3.0, '公共', 200),
('概率论与数理统计', 2, 7, 3.0, '公共', 180),
('大学英语III', 3, 8, 2.0, '公共', 150),
('商务英语', 3, 9, 2.0, '选修', 60),
('微观经济学', 4, 10, 3.0, '必修', 100),
('管理学原理', 4, 11, 3.0, '必修', 100),
('会计学基础', 4, 12, 2.5, '选修', 80),
('深度学习基础', 5, 13, 3.0, '必修', 60),
('自然语言处理', 5, 14, 3.0, '选修', 50),
('计算机视觉', 5, 15, 3.0, '选修', 50);

-- ========================================
-- 5. 选课与成绩表 (enrollments)
-- ========================================
CREATE TABLE enrollments (
    enrollment_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '选课记录ID',
    student_id INT NOT NULL COMMENT '学生ID',
    course_id INT NOT NULL COMMENT '课程ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期(如2024-2025-1)',
    score DECIMAL(5, 2) COMMENT '成绩(百分制)',
    grade_letter VARCHAR(5) COMMENT '等级(A/B/C/D/F)',
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
) COMMENT='选课与成绩表';

INSERT INTO enrollments (student_id, course_id, semester, score, grade_letter) VALUES
-- 张小明 (计算机)
(1, 1, '2022-2023-1', 92, 'A'), (1, 2, '2022-2023-2', 88, 'B+'),
(1, 3, '2023-2024-1', 95, 'A'), (1, 5, '2021-2022-1', 85, 'B+'),
(1, 6, '2021-2022-2', 90, 'A'), (1, 13, '2023-2024-2', 91, 'A'),
-- 李婷 (计算机)
(2, 1, '2022-2023-1', 96, 'A'), (2, 2, '2022-2023-2', 93, 'A'),
(2, 3, '2023-2024-1', 97, 'A'), (2, 5, '2021-2022-1', 92, 'A'),
(2, 14, '2023-2024-2', 94, 'A'),
-- 王浩 (计算机)
(3, 1, '2023-2024-1', 78, 'C+'), (3, 5, '2022-2023-1', 82, 'B'),
(3, 6, '2022-2023-2', 75, 'C'), (3, 4, '2023-2024-2', 85, 'B+'),
-- 赵雪 (计算机)
(4, 1, '2023-2024-1', 86, 'B+'), (4, 5, '2022-2023-1', 88, 'B+'),
(4, 8, '2022-2023-1', 91, 'A'), (4, 3, '2023-2024-2', 83, 'B'),
-- 刘杰 (数学)
(5, 5, '2022-2023-1', 95, 'A'), (5, 6, '2022-2023-2', 93, 'A'),
(5, 7, '2023-2024-1', 90, 'A'), (5, 1, '2023-2024-2', 82, 'B'),
-- 陈美丽 (数学)
(6, 5, '2021-2022-1', 80, 'B'), (6, 6, '2021-2022-2', 78, 'C+'),
(6, 7, '2022-2023-1', 85, 'B+'), (6, 10, '2022-2023-2', 76, 'C'),
-- 孙伟 (数学)
(7, 5, '2023-2024-1', 70, 'C'), (7, 6, '2023-2024-2', 68, 'D'),
-- 周倩 (外语)
(8, 8, '2021-2022-1', 88, 'B+'), (8, 9, '2022-2023-1', 92, 'A'),
(8, 10, '2022-2023-2', 79, 'C+'), (8, 5, '2021-2022-1', 72, 'C'),
-- 吴涛 (外语)
(9, 8, '2022-2023-1', 65, 'D'), (9, 5, '2022-2023-1', 58, 'F'),
(9, 9, '2023-2024-1', 70, 'C'),
-- 郑丽丽 (外语)
(10, 8, '2023-2024-1', 82, 'B'), (10, 5, '2023-2024-1', 75, 'C'),
-- 何强 (经管)
(11, 10, '2022-2023-1', 90, 'A'), (11, 11, '2022-2023-2', 88, 'B+'),
(11, 12, '2023-2024-1', 85, 'B+'), (11, 5, '2021-2022-1', 80, 'B'),
-- 林小芳 (经管)
(12, 10, '2023-2024-1', 82, 'B'), (12, 11, '2023-2024-2', 78, 'C+'),
(12, 5, '2022-2023-1', 77, 'C+'),
-- 黄明 (经管)
(13, 10, '2023-2024-1', 73, 'C'), (13, 5, '2023-2024-1', 65, 'D'),
-- 杨洋 (人工智能)
(14, 13, '2023-2024-1', 96, 'A'), (14, 14, '2023-2024-2', 93, 'A'),
(14, 1, '2022-2023-2', 90, 'A'), (14, 5, '2022-2023-1', 94, 'A'),
(14, 15, '2023-2024-2', 91, 'A'),
-- 马丽 (人工智能)
(15, 13, '2023-2024-1', 80, 'B'), (15, 5, '2023-2024-1', 76, 'C'),
(15, 14, '2023-2024-2', 78, 'C+');

-- ========================================
-- 验证数据
-- ========================================
SELECT '教育数据库创建完成！' AS message;
SELECT '院系数量' AS item, COUNT(*) AS count FROM departments
UNION ALL SELECT '教师数量', COUNT(*) FROM teachers
UNION ALL SELECT '学生数量', COUNT(*) FROM students
UNION ALL SELECT '课程数量', COUNT(*) FROM courses
UNION ALL SELECT '选课记录数量', COUNT(*) FROM enrollments;
