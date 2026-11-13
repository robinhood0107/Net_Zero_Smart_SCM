const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

async function search(input, results = [{ projectId: 7 }]) {
    const calls = [];
    const context = vm.createContext({
        URLSearchParams,
        window: { location: { search: '' } },
        document: {
            addEventListener() {},
            getElementById() { return { value: input }; }
        },
        alert(message) { calls.push(['alert', message]); },
        async searchProjects(keyword, limit) {
            calls.push(['search', keyword, limit]);
            return results;
        },
        console
    });
    vm.runInContext(fs.readFileSync('src/main/resources/static/js/dashboard.js', 'utf8'), context);
    context.recordLoad = id => calls.push(['load', id]);
    vm.runInContext('loadDashboard = async id => recordLoad(id)', context);
    await context.handleSearch();
    return calls;
}

test('선박명에 포함된 하이픈과 숫자 접두사를 유지한다', async () => {
    assert.deepEqual(await search(' HYUNDAI GREEN-1 '), [['search', 'HYUNDAI GREEN-1', 10], ['load', 7]]);
    assert.deepEqual(await search('123-선박'), [['search', '123-선박', 10], ['load', 7]]);
});

test('정수 ID는 바로 조회하고 빈 검색어는 거부한다', async () => {
    assert.deepEqual(await search('1'), [['load', 1]]);
    assert.deepEqual(await search('  '), [['alert', '프로젝트 ID 또는 선박명을 입력하세요.']]);
    assert.deepEqual(await search('없는 선박', []), [['search', '없는 선박', 10], ['alert', '검색 결과가 없습니다.']]);
});
