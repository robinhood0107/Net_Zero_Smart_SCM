// 선택된 공급업체 상태 관리
let selectedSupplierId = null;
let currentFilters = { esgGrades: ['A', 'B'], minRatio: null, maxRatio: null };
let currentPage = 0;
let pageSize = 5;
let totalSuppliers = 0;
let detailPage = 0;
let detailPageSize = 3;
let totalOrdersCount = 0;

// 정렬 상태 관리
let sortColumn = null;
let sortDirection = 'asc'; // 'asc' or 'desc'
let allSuppliers = []; // 전체 공급업체 목록 (정렬용)

// 공통 유틸리티 utils.js로 이관됨

// 체크박스와 필터 동기화
function syncCheckboxesWithFilters() {
    const esgCheckboxes = document.querySelectorAll('.checkbox-wrapper input[type="checkbox"]');
    esgCheckboxes.forEach(cb => {
        const grade = cb.parentElement.querySelector('div').textContent.trim();
        cb.checked = currentFilters.esgGrades && currentFilters.esgGrades.includes(grade);
    });
}

// 초기 데이터 로드
document.addEventListener('DOMContentLoaded', async () => {
    // 체크박스 초기화
    syncCheckboxesWithFilters();
    
    try {
        await loadSuppliers();
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        showError('데이터를 불러오는 중 오류가 발생했습니다.');
    }
    
    // 필터링 이벤트 등록
    const filterBtn = Array.from(document.querySelectorAll('button')).find(btn => {
        const text = btn.textContent.trim();
        return text.includes('필터 적용');
    });
    if (filterBtn) {
        filterBtn.addEventListener('click', applyFilter);
    }
    
    // ESG 등급 체크박스 이벤트 - 클릭 시 체크박스 상태 토글 및 자동 필터 적용
    document.querySelectorAll('.checkbox-wrapper').forEach(wrapper => {
        const checkbox = wrapper.querySelector('input[type="checkbox"]');
        const div = wrapper.querySelector('div');
        
        // 라벨 클릭 시 토글
        wrapper.addEventListener('click', (e) => {
            e.preventDefault();
            checkbox.checked = !checkbox.checked;
            // 체크박스 변경 이벤트 트리거
            checkbox.dispatchEvent(new Event('change', { bubbles: true }));
        });
        
        // 체크박스 변경 시 자동 필터링
        checkbox.addEventListener('change', () => {
            // CSS로 체크 상태 표시는 이미 처리됨
            // 필터 적용
            applyFilter();
        });
    });
    
    // 정렬 헤더 초기화
    setupSortHeaders();
    
    // 상세 페이지 네비게이션
    const detailPrevBtn = document.getElementById('detail-prev-btn');
    const detailNextBtn = document.getElementById('detail-next-btn');
    if (detailPrevBtn) {
        detailPrevBtn.addEventListener('click', async () => {
            if (detailPage > 0) {
                detailPage--;
                if (selectedSupplierId) {
                    await loadSupplierOrders(selectedSupplierId);
                }
            }
        });
    }
    if (detailNextBtn) {
        detailNextBtn.addEventListener('click', async () => {
            detailPage++;
            if (selectedSupplierId) {
                await loadSupplierOrders(selectedSupplierId);
            }
        });
    }
});

// 공급업체 목록 조회
async function loadSuppliers() {
    try {
        // 입력값 검증
        const esgGrades = Array.isArray(currentFilters.esgGrades) 
            ? currentFilters.esgGrades.filter(g => ['A', 'B', 'C', 'D'].includes(g))
            : null;
        const minRatio = (currentFilters.minRatio !== null && 
                         typeof currentFilters.minRatio === 'number' && 
                         currentFilters.minRatio >= 0 && 
                         currentFilters.minRatio <= 1) 
            ? currentFilters.minRatio 
            : null;
        const maxRatio = (currentFilters.maxRatio !== null && 
                         typeof currentFilters.maxRatio === 'number' && 
                         currentFilters.maxRatio >= 0 && 
                         currentFilters.maxRatio <= 1) 
            ? currentFilters.maxRatio 
            : null;
        
        const suppliers = await getSuppliers(esgGrades, minRatio, maxRatio);
        
        // 정렬용 전체 목록 저장
        allSuppliers = [...suppliers];
        
        // 정렬 실행
        if (sortColumn) {
            sortSuppliers(allSuppliers);
        }
        
        totalSuppliers = allSuppliers.length;
        updateSupplierTable(allSuppliers);
        updatePagination();
        updateSortIcons();
        
        // 기본 공급업체 선택
        if (allSuppliers.length > 0 && !selectedSupplierId) {
            selectedSupplierId = allSuppliers[0].supplierId;
            await loadSupplierDetail(selectedSupplierId);
        }
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        showError('공급업체 목록을 불러오는 중 오류가 발생했습니다.');
    }
}

// 공급업체 데이터 정렬
function sortSuppliers(suppliers) {
    if (!sortColumn) return;
    
    suppliers.sort((a, b) => {
        let aVal, bVal;
        
        switch (sortColumn) {
            case 'id':
                aVal = a.supplierId || 0;
                bVal = b.supplierId || 0;
                break;
            case 'name':
                aVal = (a.name || '').toLowerCase();
                bVal = (b.name || '').toLowerCase();
                break;
            case 'country':
                aVal = (a.country || '').toLowerCase();
                bVal = (b.country || '').toLowerCase();
                break;
            case 'esg':
                // ESG 등급순 정렬
                const esgOrder = { 'A': 1, 'B': 2, 'C': 3, 'D': 4 };
                aVal = esgOrder[a.esgGrade] || 99;
                bVal = esgOrder[b.esgGrade] || 99;
                break;
            case 'amount':
                aVal = a.totalOrderAmount || 0;
                bVal = b.totalOrderAmount || 0;
                break;
            case 'delay':
                aVal = a.delayRatio || 0;
                bVal = b.delayRatio || 0;
                break;
            default:
                return 0;
        }
        
        if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
        if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
        return 0;
    });
}

// 헤더 클릭 정렬 이벤트
function setupSortHeaders() {
    const headers = document.querySelectorAll('thead th');
    headers.forEach((header, index) => {
        header.addEventListener('click', () => {
            let column = null;
            
            // 컬럼 매핑
            switch (index) {
                case 0: column = 'id'; break;
                case 1: column = 'name'; break;
                case 2: column = 'country'; break;
                case 3: column = 'esg'; break;
                case 4: column = 'amount'; break;
                case 5: column = 'delay'; break;
            }
            
            if (column) {
                // 정렬 방향 토글
                if (sortColumn === column) {
                    sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
                } else {
                    sortColumn = column;
                    sortDirection = 'asc';
                }
                
                // 정렬 적용
                sortSuppliers(allSuppliers);
                currentPage = 0; // 정렬 시 첫 페이지로 이동
                updateSupplierTable(allSuppliers);
                updatePagination();
                updateSortIcons();
            }
        });
    });
}

// 정렬 아이콘 UI 갱신
function updateSortIcons() {
    const headers = document.querySelectorAll('thead th');
    headers.forEach((header, index) => {
        const icon = header.querySelector('.material-symbols-outlined');
        if (icon) {
            let column = null;
            switch (index) {
                case 0: column = 'id'; break;
                case 1: column = 'name'; break;
                case 2: column = 'country'; break;
                case 3: column = 'esg'; break;
                case 4: column = 'amount'; break;
                case 5: column = 'delay'; break;
            }
            
            if (column === sortColumn) {
                icon.textContent = sortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward';
                icon.style.opacity = '1';
            } else {
                icon.textContent = 'sort';
                icon.style.opacity = '0';
            }
        }
    });
}

// 테이블 데이터 렌더링
function updateSupplierTable(suppliers) {
    const tbody = document.getElementById('supplier-table-body');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (suppliers.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="6" class="p-8 text-center text-text-sub dark:text-text-secondary">공급업체가 없습니다.</td>';
        tbody.appendChild(row);
        return;
    }
    
    // 페이지네이션 처리
    const startIndex = currentPage * pageSize;
    const endIndex = Math.min(startIndex + pageSize, suppliers.length);
    const pageSuppliers = suppliers.slice(startIndex, endIndex);
    
    pageSuppliers.forEach((supplier, index) => {
        const delayRatio = supplier.delayRatio || 0;
        const delayPercent = (delayRatio * 100).toFixed(1);
        const isDelayed = delayRatio > 0.1; // 10% 이상이면 지연으로 표시
        const isSelected = supplier.supplierId === selectedSupplierId;
        
        // 등급별 색상 적용
        const esgColor = esgGradeColors[supplier.esgGrade] || esgGradeColors['D'];
        
        const row = document.createElement('tr');
        row.className = isSelected 
            ? 'bg-primary-subtle dark:bg-primary-cyan/5 border-l-4 border-primary dark:border-primary-cyan hover:bg-primary-light/50 dark:hover:bg-primary-cyan/10 transition-colors cursor-pointer group'
            : 'hover:bg-slate-50 dark:hover:bg-[#283539] border-l-4 border-transparent transition-colors cursor-pointer group';
        
        row.innerHTML = `
            <td class="p-4 font-mono ${isSelected ? 'text-primary-dark dark:text-primary-cyan font-bold dark:font-medium' : 'text-text-main dark:text-text-secondary'} group-hover:text-primary-dark dark:group-hover:text-white">SUP-${String(supplier.supplierId).padStart(3, '0')}</td>
            <td class="p-4 text-text-main dark:text-white ${isSelected ? 'font-bold text-base' : 'font-medium'}">${escapeHtml(supplier.name || 'N/A')}</td>
            <td class="p-4">
                <div class="flex items-center gap-2">
                    ${getCountryFlag(supplier.country)}
                    <span class="text-text-sub dark:text-white font-medium">${escapeHtml(supplier.country || 'N/A')}</span>
                </div>
            </td>
            <td class="p-4">
                <span class="px-2.5 py-1 rounded-full dark:rounded ${esgColor.bg} ${esgColor.text} text-xs font-bold border ${esgColor.border}">${supplier.esgGrade || 'N/A'}</span>
            </td>
            <td class="p-4 text-right text-text-main dark:text-white font-mono ${isSelected ? 'font-medium' : ''}">${formatCompact(supplier.totalOrderAmount || 0, '₩')}</td>
            <td class="p-4 text-right">
                ${isDelayed ? 
                    `<div class="flex items-center justify-end gap-1 text-danger font-bold bg-danger/5 dark:bg-danger/10 px-2.5 py-1 rounded-lg dark:rounded w-fit ml-auto border border-danger/20">
                        <span class="material-symbols-outlined text-[16px]">warning</span>
                        ${delayPercent}%
                    </div>` :
                    `<span class="text-text-sub dark:text-text-secondary font-medium">${delayPercent}%</span>`
                }
            </td>
        `;
        
        // 상세 정보 조회 이벤트
        row.addEventListener('click', () => {
            // 공급업체 ID 검증
            if (supplier && supplier.supplierId) {
                selectedSupplierId = supplier.supplierId;
                detailPage = 0; // 상세 화면 페이지 초기화
                loadSupplierDetail(selectedSupplierId);
                loadSuppliers(); // 테이블 재로드하여 선택 상태 업데이트
            }
        });
        
        tbody.appendChild(row);
    });
}

// 페이지네이션 UI 갱신
function updatePagination() {
    const pagination = document.getElementById('pagination');
    const tableCount = document.getElementById('table-count');
    
    if (!pagination || !tableCount) return;
    
    const totalPages = Math.ceil(totalSuppliers / pageSize);
    const startIndex = currentPage * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalSuppliers);
    
    tableCount.textContent = `${totalSuppliers}개 공급업체 중 ${startIndex + 1}-${endIndex}개 표시`;
    
    pagination.innerHTML = '';
    
    // 이전 페이지 버튼
    const prevBtn = document.createElement('button');
    prevBtn.className = 'w-8 h-8 rounded-lg dark:rounded border border-border-color dark:border-border-dark flex items-center justify-center hover:bg-slate-50 dark:hover:bg-card-dark text-text-sub dark:text-text-secondary hover:text-primary dark:hover:text-white transition-colors';
    prevBtn.textContent = '<';
    prevBtn.disabled = currentPage === 0;
    prevBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            loadSuppliers();
        }
    });
    pagination.appendChild(prevBtn);
    
    // 페이지 번호 생성
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = i === currentPage
            ? 'w-8 h-8 rounded-lg dark:rounded bg-primary dark:bg-primary-cyan text-white border border-primary dark:border-primary-cyan flex items-center justify-center shadow-sm font-bold'
            : 'w-8 h-8 rounded-lg dark:rounded border border-border-color dark:border-border-dark flex items-center justify-center hover:bg-slate-50 dark:hover:bg-card-dark text-text-sub dark:text-text-secondary hover:text-primary dark:hover:text-white transition-colors';
        pageBtn.textContent = i + 1;
        pageBtn.addEventListener('click', () => {
            currentPage = i;
            loadSuppliers();
        });
        pagination.appendChild(pageBtn);
    }
    
    // 다음 페이지 버튼
    const nextBtn = document.createElement('button');
    nextBtn.className = 'w-8 h-8 rounded-lg dark:rounded border border-border-color dark:border-border-dark flex items-center justify-center hover:bg-slate-50 dark:hover:bg-card-dark text-text-sub dark:text-text-secondary hover:text-primary dark:hover:text-white transition-colors';
    nextBtn.textContent = '>';
    nextBtn.disabled = currentPage >= totalPages - 1;
    nextBtn.addEventListener('click', () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadSuppliers();
        }
    });
    pagination.appendChild(nextBtn);
}

// 공급업체 상세 정보 조회
async function loadSupplierDetail(supplierId) {
    try {
        // 공급업체 ID 유효성 확인
        if (!supplierId || (typeof supplierId !== 'number' && typeof supplierId !== 'string')) {
            showError('유효하지 않은 공급업체 ID입니다.');
            return;
        }
        
        const numericId = typeof supplierId === 'string' ? parseInt(supplierId, 10) : supplierId;
        if (isNaN(numericId) || numericId <= 0) {
            showError('유효하지 않은 공급업체 ID입니다.');
            return;
        }
        
        const detail = await getSupplierDetail(numericId);
        updateSupplierDetail(detail);
        
        // 발주서 목록 페이징 조회
        await loadSupplierOrders(supplierId);
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        showError('공급업체 상세 정보를 불러오는 중 오류가 발생했습니다.');
    }
}

// 발주서 목록 조회
async function loadSupplierOrders(supplierId) {
    try {
        // 입력 검증
        if (!supplierId || (typeof supplierId !== 'number' && typeof supplierId !== 'string')) {
            return;
        }
        
        const numericId = typeof supplierId === 'string' ? parseInt(supplierId, 10) : supplierId;
        if (isNaN(numericId) || numericId <= 0) {
            return;
        }
        
        // 페이지 번호 유효성 검증
        const validPage = Math.max(0, Math.floor(detailPage || 0));
        const validPageSize = Math.max(1, Math.min(100, Math.floor(detailPageSize || 3)));
        
        const orders = await getSupplierOrders(numericId, validPage, validPageSize);
        updateRecentOrders(orders);
        // 다음 페이지 데이터 존재 확인
        const nextPageOrders = await getSupplierOrders(numericId, validPage + 1, 1);
        const hasMore = nextPageOrders.length > 0;
        // 다음 버튼 활성화 여부 확인
        const showNext = orders.length === detailPageSize && hasMore;
        updateDetailPagination(orders.length, showNext);
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        // 사용자에게는 조용히 실패 처리
    }
}

// 상세 정보 패널 업데이트
function updateSupplierDetail(detail) {
    const supplier = detail.supplier;
    const delayRatio = supplier.delayRatio || 0;
    const delayPercent = (delayRatio * 100).toFixed(1);
    
    // 헤더 정보 갱신
    const headerId = document.querySelector('.bg-gradient-to-r span.text-xs');
    if (headerId) {
        headerId.textContent = `ID: SUP-${String(supplier.supplierId).padStart(3, '0')}`;
    }
    
    const headerTitle = document.querySelector('.bg-gradient-to-r h3');
    if (headerTitle) {
        headerTitle.textContent = supplier.name || 'N/A';
    }
    
    const headerCountry = document.querySelector('.bg-gradient-to-r .text-sm.text-teal-100 span.font-medium');
    if (headerCountry) {
        const countryFlag = getCountryFlag(supplier.country);
        headerCountry.innerHTML = countryFlag 
            ? `${countryFlag}<span class="ml-1.5">${escapeHtml(supplier.country || 'N/A')}</span>`
            : escapeHtml(supplier.country || 'N/A');
    }
    
    // 총 주문액 표시
    const totalAmount = document.querySelector('.bg-white\\/10 p.font-mono.text-lg');
    if (totalAmount) {
        totalAmount.textContent = formatCompact(supplier.totalOrderAmount || 0, '₩');
    }
    
    // 평균 지연률 표시
    const delayRate = document.querySelector('.bg-white\\/10 p.font-mono.text-lg.flex.items-center.gap-1');
    if (delayRate) {
        // 지연률 등급별 색상 적용
        let textColor, icon;
        const delayPercentNum = parseFloat(delayPercent);
        
        if (delayPercentNum < 10) {
            // 0~10%: 정상
            textColor = 'text-green-300 dark:text-success';
            icon = 'check_circle';
        } else if (delayPercentNum < 20) {
            // 10~20%: 양호
            textColor = 'text-teal-300 dark:text-teal-400';
            icon = 'info';
        } else if (delayPercentNum < 40) {
            // 20~40%: 주의
            textColor = 'text-yellow-300 dark:text-warning';
            icon = 'warning';
        } else if (delayPercentNum < 60) {
            // 40~60%: 경고
            textColor = 'text-orange-300 dark:text-orange-400';
            icon = 'trending_up';
        } else {
            // 60~100%: 위험
            textColor = 'text-red-300 dark:text-danger';
            icon = 'error';
        }
        
        delayRate.className = `text-lg font-mono ${textColor} font-bold flex items-center gap-1`;
        delayRate.innerHTML = `${delayPercent}%<span class="material-symbols-outlined text-sm">${icon}</span>`;
    }
}

// 최근 발주서 목록 렌더링
function updateRecentOrders(orders) {
    const ordersContainer = document.getElementById('recent-orders-container');
    if (!ordersContainer) return;
    
    ordersContainer.innerHTML = '';
    
    if (orders.length === 0) {
        const emptyDiv = document.createElement('div');
        emptyDiv.className = 'text-center py-8 text-text-sub dark:text-text-secondary text-sm';
        emptyDiv.textContent = '발주서가 없습니다.';
        ordersContainer.appendChild(emptyDiv);
        return;
    }
    
    orders.forEach(order => {
        // 날짜 형식 변환
        const orderDateStr = order.orderDate instanceof Date 
            ? order.orderDate.toISOString().split('T')[0]
            : order.orderDate;
        const formattedDate = formatDate(orderDateStr);
        
        // 지연 상태 스타일 적용
        let borderColorClass = 'bg-success';
        let badgeBgClass = 'bg-success/10';
        let badgeTextClass = 'text-success';
        let badgeBorderClass = 'border-success/20';
        let icon = 'check_circle';
        let statusText = '정상';
        let statusLabel = '정시 도착';
        
        if (order.delayed) {
            borderColorClass = 'bg-danger';
            badgeBgClass = 'bg-danger/10';
            badgeTextClass = 'text-danger';
            badgeBorderClass = 'border-danger/20';
            icon = 'schedule';
            statusText = '지연됨';
            statusLabel = '지연 중';
        } else {
            // 상태별 스타일 적용
            const statusInfo = orderStatusInfo[order.status] || orderStatusInfo['요청'];
            badgeBgClass = statusInfo.bg;
            badgeTextClass = statusInfo.textColor;
            badgeBorderClass = statusInfo.border;
            icon = statusInfo.icon;
            statusText = statusInfo.text;
            statusLabel = statusInfo.label;
            
            if (order.status === '요청' || order.status === '검수중') {
                borderColorClass = 'bg-warning';
            } else if (order.status === '취소') {
                borderColorClass = 'bg-gray-400';
            } else {
                borderColorClass = 'bg-success';
            }
        }
        
        const orderDiv = document.createElement('div');
        orderDiv.className = 'bg-white dark:bg-[#111618] rounded-lg border border-border-color dark:border-transparent p-4 hover:bg-slate-50 dark:hover:bg-[#1a2023] transition-all group shadow-sm hover:shadow-md dark:shadow-none relative overflow-hidden';
        
        orderDiv.innerHTML = 
            '<div class="absolute left-0 top-0 bottom-0 w-1 ' + borderColorClass + ' dark:rounded-none"></div>' +
            '<div class="flex justify-between items-start mb-2 pl-2 dark:pl-0">' +
                '<div>' +
                    '<p class="text-xs font-mono text-text-sub dark:text-text-secondary">PO-' + order.poid + '</p>' +
                    '<p class="text-sm font-bold text-text-main dark:text-white">발주서 #' + order.poid + '</p>' +
                '</div>' +
                '<span class="flex items-center gap-1.5 ' + badgeBgClass + ' ' + badgeTextClass + ' border ' + badgeBorderClass + ' px-2 py-0.5 dark:px-2.5 dark:py-1 rounded dark:rounded-full text-[10px] font-bold uppercase tracking-wide">' +
                    '<span class="w-1.5 h-1.5 rounded-full ' + borderColorClass + (order.delayed ? ' animate-pulse' : '') + '"></span>' +
                    escapeHtml(statusText) +
                '</span>' +
            '</div>' +
            '<div class="flex justify-between items-end text-xs pl-2 dark:pl-0">' +
                '<div class="text-text-sub dark:text-text-secondary">' +
                    '<span class="block mb-0.5">주문일: ' + formattedDate + '</span>' +
                    '<span class="' + badgeTextClass + ' font-bold flex items-center gap-1">' +
                        '<span class="material-symbols-outlined text-[14px]">' + icon + '</span>' +
                        escapeHtml(statusLabel) +
                    '</span>' +
                '</div>' +
            '</div>';
        ordersContainer.appendChild(orderDiv);
    });
}

// 상세 페이지 네비게이션 갱신
function updateDetailPagination(currentOrderCount, showNext) {
    const detailPageInfo = document.getElementById('detail-page-info');
    const detailPrevBtn = document.getElementById('detail-prev-btn');
    const detailNextBtn = document.getElementById('detail-next-btn');
    
    if (detailPageInfo) {
        // 페이징 정보 숨김
        detailPageInfo.textContent = '';
    }
    
    if (detailPrevBtn) {
        detailPrevBtn.disabled = detailPage === 0;
        // 이전 버튼 표시 여부
        detailPrevBtn.style.visibility = detailPage === 0 ? 'hidden' : 'visible';
    }
    
    if (detailNextBtn) {
        detailNextBtn.disabled = !showNext;
        // 다음 버튼 표시 여부
        detailNextBtn.style.visibility = showNext ? 'visible' : 'hidden';
    }
}

// 필터링 실행
async function applyFilter() {
    try {
        // ESG 등급 필터 확인
        const esgCheckboxes = document.querySelectorAll('.checkbox-wrapper input[type="checkbox"]');
        const selectedGrades = [];
        esgCheckboxes.forEach(cb => {
            if (cb.checked && cb.type === 'checkbox') {
                const gradeDiv = cb.parentElement?.querySelector('div');
                if (gradeDiv) {
                    const grade = gradeDiv.textContent?.trim();
                    // 등급 유효성 검증
                    if (grade && ['A', 'B', 'C', 'D'].includes(grade)) {
                        selectedGrades.push(grade);
                    }
                }
            }
        });
        
        // 지연 납품률 필터 확인
        const minRatioInput = document.querySelector('input[placeholder="최소"]');
        const maxRatioInput = document.querySelector('input[placeholder="최대"]');
        
        let minRatio = null;
        let maxRatio = null;
        
        if (minRatioInput && minRatioInput.value) {
            const parsed = parseFloat(minRatioInput.value);
            // 0~100 범위 검증
            if (!isNaN(parsed) && parsed >= 0 && parsed <= 100) {
                minRatio = parsed / 100;
            }
        }
        
        if (maxRatioInput && maxRatioInput.value) {
            const parsed = parseFloat(maxRatioInput.value);
            // 범위 검증: 0-100 사이의 숫자만 허용
            if (!isNaN(parsed) && parsed >= 0 && parsed <= 100) {
                maxRatio = parsed / 100;
            }
        }
        
        // 최소/최대값 범위 교정
        if (minRatio !== null && maxRatio !== null && minRatio > maxRatio) {
            [minRatio, maxRatio] = [maxRatio, minRatio];
        }
        
        currentFilters = {
            esgGrades: selectedGrades.length > 0 ? selectedGrades : null,
            minRatio: minRatio,
            maxRatio: maxRatio
        };
        
        currentPage = 0; // 필터 적용 시 첫 페이지로 이동
        await loadSuppliers();
        
        // 체크박스 상태 동기화
        syncCheckboxesWithFilters();
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        showError('필터 적용 중 오류가 발생했습니다.');
    }
}

// escapeHtml 유틸리티 사용

function showError(message) {
    const tbody = document.getElementById('supplier-table-body');
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="6" class="p-8 text-center text-danger">${escapeHtml(message)}</td></tr>`;
    }
}
