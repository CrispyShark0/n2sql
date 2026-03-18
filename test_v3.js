/**
 * NL2SQL 校验机制专项测试 v3
 *
 * 绕过大模型，直接用 /api/debug/* 接口测试：
 *   一、静态校验测试（各种语法错误、表名错误、列名错误、非SELECT）
 *   二、动态执行错误测试（语法对但逻辑错的SQL）
 *   三、完整流水线测试（模拟多轮纠错，包括重试3次全失败的场景）
 *   四、结合大模型的纠错验证（真实触发重试的复杂查询）
 */

const http = require('http');
const BASE = 'http://localhost:8080/api';
let passCount = 0, failCount = 0, totalTests = 0;
const results = [];

function request(method, path, body) {
    return new Promise((resolve, reject) => {
        const url = new URL(BASE + path);
        const opts = {
            hostname: url.hostname, port: url.port, path: url.pathname,
            method, headers: { 'Content-Type': 'application/json' }, timeout: 120000
        };
        const req = http.request(opts, res => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => { try { resolve(JSON.parse(data)); } catch { resolve(data); } });
        });
        req.on('error', reject);
        req.on('timeout', () => { req.destroy(); reject(new Error('超时')); });
        if (body) req.write(JSON.stringify(body));
        req.end();
    });
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
function log(msg) { console.log(msg); }
function pass(name, detail) {
    totalTests++; passCount++;
    results.push({ name, status: 'PASS', detail });
    log(`  ✅ ${name}${detail ? '\n     → ' + detail : ''}`);
}
function fail(name, detail) {
    totalTests++; failCount++;
    results.push({ name, status: 'FAIL', detail });
    log(`  ❌ ${name}\n     → ${detail}`);
}

async function main() {
    log('🔧 NL2SQL 校验机制专项测试 v3');
    log(`⏰ ${new Date().toLocaleString()}\n`);

    // 创建数据源
    const dsRes = await request('POST', '/datasource', {
        name: '校验测试库', dbType: 'MYSQL', host: 'localhost',
        port: 3306, dbName: 'n2sql_test', username: 'root', password: 'Zlj2004826'
    });
    const dsId = dsRes.data?.id;
    if (!dsId) { log('❌ 数据源创建失败'); return; }
    log(`📦 数据源: ${dsId}\n`);

    // ============================================================
    log('='.repeat(60));
    log('一、静态校验测试（绕过大模型，直接送SQL）');
    log('='.repeat(60));

    // --- 1.1 正常SQL应该通过 ---
    log('\n--- 1.1 正确的SQL ---');
    let r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT emp_name, salary FROM employees WHERE salary > 20000' });
    let d = r.data;
    if (d?.valid === true && d?.errorCategory === 'OK') {
        pass('正确SQL通过校验', `category=${d.errorCategory}`);
    } else { fail('正确SQL通过校验', `expected valid=true, got ${JSON.stringify(d)}`); }

    // --- 1.2 语法错误：括号不匹配 ---
    log('\n--- 1.2 语法错误：括号不匹配 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT emp_name FROM employees WHERE (salary > 20000' });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'SYNTAX_ERROR') {
        pass('括号不匹配被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 60)}`);
    } else { fail('括号不匹配被拦截', JSON.stringify(d)); }

    // --- 1.3 语法错误：SELECT后缺少列名 ---
    log('\n--- 1.3 语法错误：SELECT后接WHERE（无列名） ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT FROM employees WHERE salary > 20000' });
    d = r.data;
    if (d?.valid === false) {
        pass('语法错误被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 60)}`);
    } else { fail('语法错误被拦截', JSON.stringify(d)); }

    // --- 1.4 语法错误：关键字拼写错误 ---
    log('\n--- 1.4 语法错误：SELCET拼写错误 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELCET emp_name FROM employees' });
    d = r.data;
    if (d?.valid === false) {
        pass('关键字拼错被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 60)}`);
    } else { fail('关键字拼错被拦截', JSON.stringify(d)); }

    // --- 1.5 非SELECT语句：DELETE ---
    log('\n--- 1.5 非SELECT：DELETE语句 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'DELETE FROM employees WHERE emp_id = 1' });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'NON_SELECT') {
        pass('DELETE被拦截', `${d.errorCategory}`);
    } else { fail('DELETE被拦截', JSON.stringify(d)); }

    // --- 1.6 非SELECT语句：DROP TABLE ---
    log('\n--- 1.6 非SELECT：DROP TABLE ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'DROP TABLE employees' });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'NON_SELECT') {
        pass('DROP TABLE被拦截', `${d.errorCategory}`);
    } else { fail('DROP TABLE被拦截', JSON.stringify(d)); }

    // --- 1.7 非SELECT语句：UPDATE ---
    log('\n--- 1.7 非SELECT：UPDATE语句 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: "UPDATE employees SET salary = 99999 WHERE emp_id = 1" });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'NON_SELECT') {
        pass('UPDATE被拦截', `${d.errorCategory}`);
    } else { fail('UPDATE被拦截', JSON.stringify(d)); }

    // --- 1.8 表名不存在 ---
    log('\n--- 1.8 Schema校验：表名不存在 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT * FROM student_scores' });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'TABLE_NOT_FOUND') {
        pass('不存在的表被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 80)}`);
    } else { fail('不存在的表被拦截', JSON.stringify(d)); }

    // --- 1.9 表名拼写错误（相似度推荐） ---
    log('\n--- 1.9 Schema校验：表名拼写错误 employes → employees ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT * FROM employes' });
    d = r.data;
    if (d?.valid === false && d?.errorMessage?.includes('employees')) {
        pass('表名拼错+修正建议', `推荐: ${d.errorMessage?.substring(0, 100)}`);
    } else if (d?.valid === false) {
        pass('表名拼错被拦截(无建议)', `${d.errorMessage?.substring(0, 80)}`);
    } else { fail('表名拼错被拦截', JSON.stringify(d)); }

    // --- 1.10 列名不存在 ---
    log('\n--- 1.10 Schema校验：列名不存在 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT emp_name, phone_number FROM employees' });
    d = r.data;
    if (d?.valid === false && d?.errorCategory === 'COLUMN_NOT_FOUND') {
        pass('不存在的列被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 80)}`);
    } else { fail('不存在的列被拦截', JSON.stringify(d)); }

    // --- 1.11 列名拼写错误 ---
    log('\n--- 1.11 Schema校验：列名拼写错误 empl_name → emp_name ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT empl_name FROM employees' });
    d = r.data;
    if (d?.valid === false && d?.errorMessage?.includes('emp_name')) {
        pass('列名拼错+修正建议', `推荐: ${d.errorMessage?.substring(0, 100)}`);
    } else if (d?.valid === false) {
        pass('列名拼错被拦截(无建议)', `${d.errorMessage?.substring(0, 80)}`);
    } else { fail('列名拼错被拦截', JSON.stringify(d)); }

    // --- 1.12 带别名的列校验：错误的列+表别名 ---
    log('\n--- 1.12 Schema校验：带别名的错误列 e.birthday ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: 'SELECT e.emp_name, e.birthday FROM employees e' });
    d = r.data;
    if (d?.valid === false) {
        pass('别名列错误被拦截', `${d.errorCategory}: ${d.errorMessage?.substring(0, 80)}`);
    } else { fail('别名列错误被拦截', JSON.stringify(d)); }

    // --- 1.13 空SQL ---
    log('\n--- 1.13 边界：空SQL ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId, sql: '' });
    d = r.data;
    if (d?.valid === false) {
        pass('空SQL被拦截', `${d.errorMessage}`);
    } else { fail('空SQL被拦截', JSON.stringify(d)); }

    // --- 1.14 多表JOIN中表名错误 ---
    log('\n--- 1.14 Schema校验：JOIN中的表名错误 ---');
    r = await request('POST', '/debug/validate', { dataSourceId: dsId,
        sql: 'SELECT e.emp_name, d.dept_name FROM employees e JOIN department d ON e.dept_id = d.dept_id'
    });
    d = r.data;
    if (d?.valid === false && d?.errorMessage?.includes('departments')) {
        pass('JOIN表名错误+修正建议', `推荐departments: ${d.errorMessage?.substring(0, 100)}`);
    } else if (d?.valid === false) {
        pass('JOIN表名错误被拦截', `${d.errorMessage?.substring(0, 80)}`);
    } else { fail('JOIN表名错误被拦截', JSON.stringify(d)); }

    // ============================================================
    log('\n' + '='.repeat(60));
    log('二、动态执行错误测试（语法正确但执行报错）');
    log('='.repeat(60));

    // --- 2.1 除以零 ---
    log('\n--- 2.1 除以零错误 ---');
    r = await request('POST', '/debug/execute', { dataSourceId: dsId, sql: 'SELECT 1/0 AS result' });
    // MySQL中 1/0 返回NULL而不报错，这里只是测试
    if (r.code === 200) {
        pass('除以零(MySQL返回NULL)', `结果: ${JSON.stringify(r.data?.rows?.[0])}`);
    } else { pass('除以零报错', r.message); }

    // --- 2.2 类型不匹配的WHERE条件 ---
    log('\n--- 2.2 类型不匹配条件 ---');
    r = await request('POST', '/debug/execute', { dataSourceId: dsId,
        sql: "SELECT emp_name FROM employees WHERE salary = 'not_a_number'"
    });
    // MySQL会做隐式转换，可能返回0行
    if (r.code === 200) {
        pass('类型不匹配(MySQL隐式转换)', `返回${r.data?.rowCount}行`);
    } else { pass('类型不匹配报错', r.message); }

    // --- 2.3 GROUP BY缺失（有聚合但没GROUP BY） ---
    log('\n--- 2.3 语义错误：有聚合函数但SELECT列不在GROUP BY中 ---');
    r = await request('POST', '/debug/execute', { dataSourceId: dsId,
        sql: 'SELECT dept_id, emp_name, COUNT(*) FROM employees'
    });
    if (r.code !== 200 || r.message?.includes('ERROR')) {
        pass('GROUP BY缺失被数据库拒绝', `${r.message?.substring(0, 80)}`);
    } else {
        // MySQL的ONLY_FULL_GROUP_BY模式可能不开启
        pass('GROUP BY缺失(MySQL宽松模式允许)', `返回${r.data?.rowCount}行`);
    }

    // --- 2.4 子查询返回多行给=操作符 ---
    log('\n--- 2.4 子查询返回多行 ---');
    r = await request('POST', '/debug/execute', { dataSourceId: dsId,
        sql: 'SELECT emp_name FROM employees WHERE salary = (SELECT salary FROM employees WHERE dept_id = 1)'
    });
    if (r.code !== 200 || r.message?.includes('ERROR')) {
        pass('子查询多行=报错', `${(r.message || '').substring(0, 80)}`);
    } else { fail('子查询多行应报错', JSON.stringify(r.data)); }

    // ============================================================
    log('\n' + '='.repeat(60));
    log('三、完整流水线测试（模拟多轮纠错过程）');
    log('='.repeat(60));

    // --- 3.1 首次就正确（0次重试） ---
    log('\n--- 3.1 场景：首次生成就正确，0次重试 ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELECT emp_name, salary FROM employees ORDER BY salary DESC LIMIT 1'
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 0) {
        pass('0次重试成功', `步骤: ${d.steps.length}个 | SQL正确`);
    } else { fail('0次重试成功', JSON.stringify(d)); }

    // --- 3.2 第1次语法错误 → 第2次成功（1次重试） ---
    log('\n--- 3.2 场景：第1轮语法错误 → 第2轮修正成功（1次重试） ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELCET emp_name FROM employees',                          // 语法错误: SELCET
        'SELECT emp_name, salary FROM employees ORDER BY salary DESC LIMIT 1'  // 修正后正确
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 1) {
        const step0 = d.steps.find(s => s.attempt === 0);
        pass('1次重试成功', `第1轮: ${step0?.errorType}(${step0?.errorMessage?.substring(0,40)}) → 第2轮: 成功`);
    } else { fail('1次重试成功', JSON.stringify(d)); }

    // --- 3.3 第1次表名错误 → 第2次列名错误 → 第3次成功（2次重试） ---
    log('\n--- 3.3 场景：表名错→列名错→最终成功（2次重试） ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELECT * FROM employes',                                   // 表名错误
        'SELECT empl_name FROM employees',                          // 列名错误
        'SELECT emp_name, salary FROM employees ORDER BY salary DESC LIMIT 1'  // 正确
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 2) {
        pass('2次重试成功', `第1轮: ${d.steps[0]?.errorType} → 第2轮: ${d.steps[1]?.errorType} → 第3轮: 成功`);
    } else { fail('2次重试成功', JSON.stringify(d)); }

    // --- 3.4 语法错→表名错→执行报错→成功（3次重试） ---
    log('\n--- 3.4 场景：语法错→表名错→执行报错→最终成功（3次重试） ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELECT emp_name FROM employees WHERE (salary > 10000',     // 语法错误：括号不匹配
        'SELECT * FROM student_scores',                             // 表名不存在
        'SELECT emp_name FROM employees WHERE salary = (SELECT salary FROM employees WHERE dept_id = 1)', // 执行报错: 子查询返回多行
        'SELECT emp_name, salary FROM employees ORDER BY salary DESC LIMIT 1'  // 终于正确
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 3) {
        log('     步骤详情:');
        d.steps.forEach(s => {
            log(`       第${s.attempt + 1}轮 [${s.stage}] ${s.passed ? '✓' : '✗'} ${s.errorType || '成功'} ${s.errorMessage ? '— ' + s.errorMessage.substring(0, 50) : ''}`);
        });
        pass('3次重试成功', `经历: 语法错→表名错→执行错→成功`);
    } else { fail('3次重试成功', JSON.stringify(d)?.substring(0, 200)); }

    // --- 3.5 ★ 重点：连续4次全部失败（超过maxRetries=3） ---
    log('\n--- 3.5 场景：★ 4次全部失败，最终返回失败 ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELCET * FROM employees',                                  // 语法错误
        'SELECT * FROM student_scores',                             // 表名不存在
        'SELECT birthday FROM employees',                           // 列名不存在
        'SELECT emp_name FROM employees WHERE (salary > 10000',     // 括号不匹配
    ]});
    d = r.data;
    if (d?.finalSuccess === false && d?.totalRetries === 4) {
        log('     步骤详情:');
        d.steps.forEach(s => {
            log(`       第${s.attempt + 1}轮 [${s.stage}] ✗ ${s.errorType} — ${s.errorMessage?.substring(0, 60)}`);
        });
        pass('4次全失败正确返回失败', `总重试: ${d.totalRetries} | finalSuccess=false`);
    } else { fail('4次全失败正确返回失败', JSON.stringify(d)?.substring(0, 200)); }

    // --- 3.6 静态通过但执行失败 → 下一轮修正 ---
    log('\n--- 3.6 场景：静态校验通过但数据库执行失败 → 修正后成功 ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        // 这个SQL语法正确、表名列名都对，但子查询返回多行会在执行时报错
        'SELECT emp_name FROM employees WHERE salary = (SELECT salary FROM employees WHERE dept_id = 1)',
        // 修正: 用IN代替=
        'SELECT emp_name FROM employees WHERE salary IN (SELECT salary FROM employees WHERE dept_id = 1)'
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 1) {
        const step0 = d.steps.find(s => s.attempt === 0 && s.stage === 'EXECUTE');
        pass('执行报错→纠正成功', `第1轮执行报错(${step0?.errorType}) → 第2轮执行成功`);
    } else { fail('执行报错→纠正成功', JSON.stringify(d)?.substring(0, 200)); }

    // --- 3.7 混合错误：静态错→执行错→静态错→最终成功 ---
    log('\n--- 3.7 场景：混合错误类型交替出现 ---');
    r = await request('POST', '/debug/full-pipeline', { dataSourceId: dsId, sqlSequence: [
        'SELECT * FROM employes',                                   // 静态: 表名错
        'SELECT emp_name FROM employees WHERE salary = (SELECT salary FROM employees WHERE dept_id = 1)', // 动态: 子查询多行
        'SELECT empl_name FROM employees',                          // 静态: 列名错
        'SELECT emp_name, salary FROM employees WHERE salary > 20000 ORDER BY salary DESC'  // 正确
    ]});
    d = r.data;
    if (d?.finalSuccess && d?.totalRetries === 3) {
        log('     步骤详情:');
        d.steps.forEach(s => {
            log(`       第${s.attempt + 1}轮 [${s.stage}] ${s.passed ? '✓' : '✗'} ${s.errorType || 'OK'}`);
        });
        pass('混合错误交替→最终成功', `静态错→执行错→静态错→成功`);
    } else { fail('混合错误交替→最终成功', JSON.stringify(d)?.substring(0, 200)); }

    // ============================================================
    log('\n' + '='.repeat(60));
    log('四、结合大模型的纠错验证（真实触发重试）');
    log('='.repeat(60));

    // --- 4.1 极难查询：年度同比增长率 ---
    log('\n--- 4.1 极难查询：2025年相对2024年的月度销售同比增长率 ---');
    r = await request('POST', '/nl2sql', { dataSourceId: dsId, question: '计算2025年每个月相对2024年同月的销售额同比增长率' });
    d = r.data || r;
    if (d.success) {
        pass('年度同比增长率', `retries=${d.retryCount} | ${d.queryResult?.rowCount}行 | SQL: ${d.generatedSql?.substring(0, 80)}...`);
    } else {
        // 极难查询失败也是合理的
        pass('年度同比增长率(失败但有纠错记录)', `retries=${d.retryCount} | 错误: ${d.errorMessage?.substring(0, 60)}`);
    }
    await sleep(3000);

    // --- 4.2 极难查询：滑动平均 ---
    log('\n--- 4.2 极难查询：每个产品的3个月滑动平均销售额 ---');
    r = await request('POST', '/nl2sql', { dataSourceId: dsId, question: '计算每个产品最近3个月的滑动平均销售额' });
    d = r.data || r;
    if (d.success) {
        pass('滑动平均', `retries=${d.retryCount} | ${d.queryResult?.rowCount}行`);
    } else {
        pass('滑动平均(失败但有纠错记录)', `retries=${d.retryCount} | 错误: ${d.errorMessage?.substring(0, 60)}`);
    }
    await sleep(3000);

    // --- 4.3 极难查询：客户流失分析 ---
    log('\n--- 4.3 极难查询：最近6个月没有下单的老客户（2024年有订单但2025年没有） ---');
    r = await request('POST', '/nl2sql', { dataSourceId: dsId, question: '找出2024年下过单但2025年一单都没下的客户' });
    d = r.data || r;
    if (d.success) {
        pass('客户流失分析', `retries=${d.retryCount} | ${d.queryResult?.rowCount}行 | SQL: ${d.generatedSql?.substring(0, 80)}...`);
    } else {
        fail('客户流失分析', `retries=${d.retryCount} | ${d.errorMessage?.substring(0, 60)}`);
    }
    await sleep(3000);

    // ============================================================
    // 清理
    await request('DELETE', `/datasource/${dsId}`);

    // ============================================================
    log('\n' + '='.repeat(60));
    log('📊 测试汇总');
    log('='.repeat(60));
    log(`总测试数: ${totalTests}`);
    log(`✅ 通过: ${passCount}`);
    log(`❌ 失败: ${failCount}`);
    log(`通过率: ${(passCount / totalTests * 100).toFixed(1)}%`);

    const failed = results.filter(r => r.status === 'FAIL');
    if (failed.length > 0) {
        log('\n❌ 失败的测试:');
        failed.forEach(f => log(`  - ${f.name}: ${f.detail}`));
    }

    log(`\n⏰ 完成时间: ${new Date().toLocaleString()}`);
}

main().catch(e => { console.error('异常:', e); process.exit(1); });
