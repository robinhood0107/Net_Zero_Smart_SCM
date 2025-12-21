var updateMainPage = function(summary) {
    const activeProjectsEl = document.getElementById('active-projects-count');
    if (activeProjectsEl) activeProjectsEl.textContent = summary.activeProjects || 0;

    const delayedDeliveriesEl = document.getElementById('delayed-deliveries-count');
    if (delayedDeliveriesEl) delayedDeliveriesEl.textContent = summary.delayedDeliveries || 0;

    const esgGradeEl = document.getElementById('avg-esg-grade');
    if (esgGradeEl) esgGradeEl.textContent = summary.avgEsgGrade || 'N/A';
};

let currentPage = 0;
const PAGE_SIZE = 10;

async function fetchAndRenderProjects() {
    const projectListBody = document.getElementById('project-list-body');
    const prevBtn = document.getElementById('prev-page-btn');
    const nextBtn = document.getElementById('next-page-btn');
    const pageIndicator = document.getElementById('page-indicator');

    if (!projectListBody) return;

    try {
        // 페이지네이션을 적용한 프로젝트 목록 조회
        const projects = await searchProjects('', PAGE_SIZE, currentPage);
        
        if (!projects || projects.length === 0) {
            if (currentPage > 0) {
                // 마지막 페이지 삭제 시 이전 페이지로 이동
                currentPage--;
                await fetchAndRenderProjects(); 
                return;
            }

            projectListBody.innerHTML = `
                <tr>
                    <td colspan="5" class="px-6 py-4 text-center text-text-sub dark:text-text-secondary">
                        표시할 프로젝트가 없습니다.
                    </td>
                </tr>`;
            // 데이터 없을 시 버튼 비활성화
            if (prevBtn) prevBtn.disabled = true;
            if (nextBtn) nextBtn.disabled = true; // 다음 페이지 데이터 없음
            return;
        }
        
        // 프로젝트 목록 렌더링
        projectListBody.innerHTML = projects.map(project => `
            <tr class="bg-white dark:bg-[#1f2b30] border-b border-gray-100 dark:border-border-dark last:border-0 hover:bg-gray-50 dark:hover:bg-[#283539] transition-colors cursor-pointer" onclick="window.location.href='dashboard.html?id=${project.projectId}'">
                <td class="px-6 py-4 font-medium text-text-main dark:text-white">
                    ${project.shipName}
                </td>
                <td class="px-6 py-4 text-text-sub dark:text-text-secondary">
                    ${project.shipType}
                </td>
                <td class="px-6 py-4 text-text-sub dark:text-text-secondary">
                     ${project.contractDate || '-'}
                </td>
                <td class="px-6 py-4">
                    <span class="px-2.5 py-1 rounded-full text-xs font-medium 
                        ${project.status === '정상' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 
                          project.status === '지연' ? 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400' : 
                          'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400'}">
                        ${project.status}
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    <a href="dashboard.html?id=${project.projectId}" class="font-medium text-primary dark:text-primary-cyan hover:underline">View</a>
                </td>
            </tr>
        `).join('');

        // 페이지네이션 UI 업데이트
        if (pageIndicator) pageIndicator.textContent = `${currentPage + 1} 페이지`;
        
        if (prevBtn) {
            prevBtn.disabled = currentPage === 0;
            prevBtn.onclick = () => {
                if (currentPage > 0) {
                    currentPage--;
                    fetchAndRenderProjects();
                }
            };
        }
        
        if (nextBtn) {
            // Simple logic: if we got less items than limit, it's the last page.
            // But if we got exactly 10, there might be more... or not. 
            // Better logic usually requires total count, but "Next" enabled generally works until empty.
            // If projects.length < PAGE_SIZE, definitely last page.
            // 다음 페이지 존재 여부 확인
            nextBtn.onclick = () => {
                currentPage++;
                fetchAndRenderProjects();
            };
        }

    } catch (error) {
        console.error('프로젝트 목록 로드 실패:', error);
        projectListBody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-4 text-center text-text-sub dark:text-text-secondary">
                    데이터를 불러오는 중 오류가 발생했습니다.
                </td>
            </tr>`;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const summary = await getMainSummary();
        updateMainPage(summary);
        await fetchAndRenderProjects();
    } catch (error) {
        console.error('데이터 로드 실패:', error);
        updateMainPage({
            activeProjects: 0,
            carbonReduction: 0,
            delayedDeliveries: 0,
            avgEsgGrade: 'N/A'
        });
        const projectListBody = document.getElementById('project-list-body');
        if (projectListBody) {
             projectListBody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-4 text-center text-text-sub dark:text-text-secondary">
                    데이터를 불러올 수 없습니다.
                </td>
            </tr>`;
        }
    }
});
