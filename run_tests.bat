@echo off
chcp 65001 >nul
echo ========================================
echo   NL2SQL 系统端到端测试脚本
echo   请确保项目已启动在 localhost:8080
echo ========================================
echo.

REM 设置数据源ID（首次需要先注册数据源）
set DS_ID=NONE

echo [步骤1] 检查项目是否启动...
curl -s http://localhost:8080/api/datasource >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 项目未启动！请先在IDEA中运行 N2sqlApplication
    pause
    exit /b
)
echo [OK] 项目已启动

echo.
echo [步骤2] 注册测试数据源...
curl -s -X POST http://localhost:8080/api/datasource ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"name\":\"test_db\",\"dbType\":\"MYSQL\",\"host\":\"localhost\",\"port\":3306,\"dbName\":\"n2sql_test\",\"username\":\"root\",\"password\":\"Zlj2004826\"}" ^
  -o test_ds_response.json
echo 数据源注册响应已写入 test_ds_response.json
type test_ds_response.json
echo.

REM 从响应中提取数据源ID（简单方式，需要手动填写）
echo.
echo [提示] 请从上面的响应中找到 "id":"ds-xxxxxxxx" 的值
echo        然后修改下面的 DS_ID 变量，或者直接按回车使用默认值
set /p DS_ID="请输入数据源ID (直接回车使用响应中的ID): "

if "%DS_ID%"=="NONE" (
    echo [提示] 将尝试从响应文件中自动获取...
    for /f "tokens=2 delims=:," %%a in ('findstr /C:"\"id\"" test_ds_response.json') do set DS_ID=%%~a
)
set DS_ID=%DS_ID: =%
echo 使用数据源ID: %DS_ID%

echo.
echo ========================================
echo   开始 NL2SQL 功能测试
echo ========================================

echo.
echo ------ 测试1: 简单查询 ------
echo 问题: 查询所有员工的姓名和工资
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询所有员工的姓名和工资\"}" ^
  -o test_result_1.json
echo 结果:
type test_result_1.json
echo.
echo.

echo ------ 测试2: 聚合查询 + 多表联查 ------
echo 问题: 查询每个部门的平均工资，按平均工资从高到低排列
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询每个部门的平均工资，按平均工资从高到低排列\"}" ^
  -o test_result_2.json
echo 结果:
type test_result_2.json
echo.
echo.

echo ------ 测试3: 多表联查 + TOP N ------
echo 问题: 查询购买金额最多的前3个客户的名称和总消费金额
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询购买金额最多的前3个客户的名称和总消费金额\"}" ^
  -o test_result_3.json
echo 结果:
type test_result_3.json
echo.
echo.

echo ------ 测试4: 嵌套子查询 ------
echo 问题: 查询工资高于公司平均工资的员工姓名和工资
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询工资高于公司平均工资的员工姓名和工资\"}" ^
  -o test_result_4.json
echo 结果:
type test_result_4.json
echo.
echo.

echo ------ 测试5: 时间条件 + 聚合 ------
echo 问题: 查询2025年每个月的销售总额和订单数量
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询2025年每个月的销售总额和订单数量\"}" ^
  -o test_result_5.json
echo 结果:
type test_result_5.json
echo.
echo.

echo ------ 测试6: 复杂多表联查 ------
echo 问题: 查询每个销售员工的姓名、所属部门、负责的订单数量和总销售额
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询每个销售员工的姓名、所属部门、负责的订单数量和总销售额\"}" ^
  -o test_result_6.json
echo 结果:
type test_result_6.json
echo.
echo.

echo ------ 测试7: 条件过滤 ------
echo 问题: 查询VIP和SVIP客户在北京和上海的订单信息
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询VIP和SVIP客户在北京和上海的订单信息，包括客户名称、订单日期和金额\"}" ^
  -o test_result_7.json
echo 结果:
type test_result_7.json
echo.
echo.

echo ------ 测试8: 嵌套查询 + 排名 ------
echo 问题: 查询销售额最高的产品类别，以及该类别下卖得最好的产品
curl -s -X POST http://localhost:8080/api/nl2sql ^
  -H "Content-Type: application/json; charset=utf-8" ^
  -d "{\"dataSourceId\":\"%DS_ID%\",\"question\":\"查询每个产品类别的总销售额，并找出销售额最高的类别\"}" ^
  -o test_result_8.json
echo 结果:
type test_result_8.json
echo.
echo.

echo ========================================
echo   测试完成！结果文件保存在项目根目录
echo   test_result_1.json ~ test_result_8.json
echo ========================================
pause
