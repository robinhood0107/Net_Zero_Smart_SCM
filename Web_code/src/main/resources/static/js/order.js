// 발주 정보 객체 초기화
let orderData = {
    projectId: null,
    supplierId: null,
    engineerName: 'Admin',
    status: '요청', // 기본값: '요청' 또는 '발주완료'
    lines: [],
    warehouseId: null,
    transportMode: '트럭',
    distanceKm: 100
};

// 전체 부품 목록 저장
let allParts = [];

// 선택된 창고의 현재 재고 정보
let currentWarehouseInventory = [];

// 페이지 로드 시 초기화
// 페이지 로드 시 초기화 및 이벤트 등록
document.addEventListener('DOMContentLoaded', async () => {
    // 1. UI 이벤트 리스너 등록
    initializeEventListeners();
    
    // 2. 초기 데이터 비동기 로드
    try {
        await loadInitialData();
        
        // 3. UI 초기 상태 설정
        initializeUI();
    } catch (error) {
        console.error('초기 데이터 로드 중 오류 일부 발생:', error);
        showToast('데이터 로드 알림', '일부 데이터를 불러오는 데 실패했습니다.', 'warning');
    }
});

function initializeEventListeners() {
    // 공급업체 변경 시 상태 업데이트
    const supplierSelect = document.getElementById('supplier-select');
    if (supplierSelect) {
        supplierSelect.addEventListener('change', (e) => {
            const selectedValue = e.target.value;
            if (selectedValue) {
                orderData.supplierId = parseInt(selectedValue);
            } else {
                orderData.supplierId = null;
            }
        });
    }

    // 창고 변경 시 재고 조회 및 미리보기 업데이트
    const warehouseSelect = document.getElementById('warehouse-select');
    if (warehouseSelect) {
        warehouseSelect.addEventListener('change', async (e) => {
            const selectedValue = e.target.value;
            if (selectedValue) {
                orderData.warehouseId = parseInt(selectedValue);
                await loadWarehouseInventory(parseInt(selectedValue));
            } else {
                orderData.warehouseId = null;
                hideWarehouseInventory();
            }
            updateDeliveryPreview();
        });
    }

    // 주문 상태 변경 처리
    const orderStatusSelect = document.getElementById('order-status');
    if (orderStatusSelect) {
        orderStatusSelect.addEventListener('change', (e) => {
            orderData.status = e.target.value;
            updateDeliveryPreview();
        });
    }

    // 프로젝트 변경 처리
    const projectSelect = document.getElementById('project-select');
    if (projectSelect) {
        projectSelect.addEventListener('change', (e) => {
            const selectedValue = e.target.value;
            if (selectedValue) {
                orderData.projectId = parseInt(selectedValue);
            } else {
                orderData.projectId = null;
            }
        });
    }

    // 주문 항목 추가
    const addRowBtn = document.getElementById('add-row-btn');
    if (addRowBtn) {
        addRowBtn.addEventListener('click', () => {
            addOrderLine();
            updateDeliveryPreview();
        });
    }

    // 발주 등록 실행
    const submitOrderBtn = document.getElementById('submit-order-btn');
    if (submitOrderBtn) {
        submitOrderBtn.addEventListener('click', submitOrder);
    }

    // 알림 메시지 닫기
    const toastCloseBtn = document.getElementById('toast-close-btn');
    if (toastCloseBtn) {
        toastCloseBtn.addEventListener('click', hideToast);
    }

    // 수량/단가 변경에 따른 합계 자동 계산
    const tbody = document.querySelector('tbody');
    if (tbody) {
        tbody.addEventListener('input', (e) => {
            if (e.target.type === 'number') {
                updateRowTotal(e.target.closest('tr'));
                updateGrandTotal();
                updateDeliveryPreview();
            }
        });
        
        // 항목 삭제 처리
        tbody.addEventListener('click', (e) => {
            const deleteBtn = e.target.closest('.delete-row');
            if (deleteBtn) {
                const row = deleteBtn.closest('tr');
                if (row) {
                    row.remove();
                    updateRowNumbers();
                    updateGrandTotal();
                    updateDeliveryPreview();
                }
            }
        });
    }
}

async function loadInitialData() {
    // 모든 선택 옵션 병렬 로드
    const results = await Promise.allSettled([
        getSupplierOptions(),
        getWarehouseOptions(),
        getProjectOptions(''),
        searchParts('')
    ]);

    const [suppliersResult, warehousesResult, projectsResult, partsResult] = results;

    // 1. 공급업체 목록 렌더링
    if (suppliersResult.status === 'fulfilled') {
        renderSupplierOptions(suppliersResult.value);
    } else {
        console.error('공급업체 목록 로드 실패:', suppliersResult.reason);
    }

    // 2. 창고 목록 렌더링
    if (warehousesResult.status === 'fulfilled') {
        renderWarehouseOptions(warehousesResult.value);
    } else {
        console.error('창고 목록 로드 실패:', warehousesResult.reason);
    }

    // 3. 프로젝트 목록 렌더링
    if (projectsResult.status === 'fulfilled') {
        renderProjectOptions(projectsResult.value);
    } else {
        console.error('프로젝트 목록 로드 실패:', projectsResult.reason);
    }

    // 4. 부품 목록 저장
    if (partsResult.status === 'fulfilled') {
        allParts = partsResult.value || [];
    } else {
        console.error('부품 목록 로드 실패:', partsResult.reason);
        allParts = [];
    }
}

function initializeUI() {
    // 오늘 날짜로 주문 일자 설정
    const orderDateEl = document.getElementById('order-date');
    if (orderDateEl) {
        const today = new Date();
        orderDateEl.textContent = today.toISOString().split('T')[0];
        orderDateEl.classList.remove('text-text-sub/50', 'dark:text-text-secondary/50');
        orderDateEl.classList.add('text-text-main', 'dark:text-white');
    }

    // 초기 합계 및 미리보기 갱신
    updateGrandTotal();
    updateDeliveryPreview();
}

// 공급업체 옵션 렌더링 (Datalist)
function renderSupplierOptions(suppliers) {
    const datalist = document.getElementById('supplier-datalist');
    if (datalist && suppliers && suppliers.length > 0) {
        datalist.innerHTML = ''; // 기존 옵션 초기화
        const sortedSuppliers = [...suppliers].sort((a, b) => (parseInt(a.supplierId) || 0) - (parseInt(b.supplierId) || 0));
        
        sortedSuppliers.forEach(supplier => {
            const option = document.createElement('option');
            // value에 ID를 넣어서 입력 시 ID가 들어가도록 유도 (혹은 "ID - 이름" 포맷 사용 가능)
            // 여기서는 사용자가 ID 입력이 주 목적이므로 ID를 value로 설정
            option.value = supplier.supplierId;
            option.label = supplier.name; // 브라우저에 따라 다르게 보일 수 있음
            datalist.appendChild(option);
        });
    }
}

// 창고 옵션 렌더링 (Datalist)
function renderWarehouseOptions(warehouses) {
    const datalist = document.getElementById('warehouse-datalist');
    if (datalist && warehouses && warehouses.length > 0) {
        datalist.innerHTML = '';
        
        warehouses.forEach(warehouse => {
            const option = document.createElement('option');
            option.value = warehouse.warehouseId;
            option.label = `${warehouse.name} (${warehouse.location})`;
            datalist.appendChild(option);
        });
    }
}

// 프로젝트 옵션 렌더링 (Datalist)
function renderProjectOptions(projects) {
    const datalist = document.getElementById('project-datalist');
    if (datalist && projects && projects.length > 0) {
        datalist.innerHTML = ''; // 기존 옵션 초기화
        const sortedProjects = [...projects].sort((a, b) => (parseInt(a.projectId) || 0) - (parseInt(b.projectId) || 0));
        
        sortedProjects.forEach(project => {
            const option = document.createElement('option');
            option.value = project.projectId;
            option.label = project.shipName;
            datalist.appendChild(option);
        });
    }
}


// 주문 라인 추가
function addOrderLine() {
    const tbody = document.querySelector('tbody');
    if (!tbody) return;
    
    const rowCount = tbody.querySelectorAll('tr').length;
    const newRow = document.createElement('tr');
    newRow.className = 'group hover:bg-surface-alt dark:hover:bg-white/5 transition-colors';
    
    // 부품 선택 select 생성
    const partSelectWrapper = document.createElement('div');
    partSelectWrapper.className = 'relative';
    
    const partSelect = document.createElement('select');
    partSelect.className = 'w-full h-10 rounded-lg bg-gray-50 dark:bg-surface-dark border border-border dark:border-border-dark text-text-main dark:text-white pl-3 pr-10 focus:ring-2 focus:ring-primary dark:focus:ring-primary-cyan focus:border-transparent cursor-pointer';
    partSelect.style.cssText = 'appearance: none; -webkit-appearance: none; -moz-appearance: none;';
    partSelect.innerHTML = '<option disabled selected value="">부품 선택...</option>';
    
    // 화살표 아이콘 추가
    const arrowIcon = document.createElement('span');
    arrowIcon.className = 'absolute right-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-text-sub dark:text-text-secondary pointer-events-none z-10';
    arrowIcon.textContent = 'expand_more';
    partSelectWrapper.appendChild(partSelect);
    partSelectWrapper.appendChild(arrowIcon);
    
    // 부품 목록을 select에 추가 (PartID로 정렬)
    const sortedParts = [...allParts].sort((a, b) => a.partId - b.partId);
    sortedParts.forEach(part => {
        const option = document.createElement('option');
        option.value = part.partId;
        option.textContent = `${part.partId} - ${part.name} (${part.unit})`;
        option.dataset.partName = part.name;
        option.dataset.unit = part.unit || '';
        option.dataset.unitPrice = part.unitPrice || 0;
        partSelect.appendChild(option);
    });
    
    // 부품 선택 시 정보 채우기
    partSelect.addEventListener('change', (e) => {
        const selectedOption = e.target.options[e.target.selectedIndex];
        if (selectedOption.value) {
            const row = e.target.closest('tr');
            const descInput = row.querySelector('input[readonly]');
            const unitPriceInput = row.querySelectorAll('input[type="number"]')[1];
            
            // 부품 설명 채우기
            if (descInput) {
                descInput.value = `${selectedOption.dataset.partName} (${selectedOption.dataset.unit})`;
            }
            
            // 단가 자동 채우기
            if (unitPriceInput) {
                unitPriceInput.value = selectedOption.dataset.unitPrice || 0;
                unitPriceInput.dispatchEvent(new Event('input', { bubbles: true }));
            }
            
            // 소계 및 총합계 업데이트
            updateRowTotal(row);
            updateGrandTotal();
            updateDeliveryPreview();
        }
    });
    
    newRow.innerHTML = `
        <td class="p-4 text-center text-text-sub dark:text-text-secondary">${rowCount + 1}</td>
        <td class="p-3">
        </td>
        <td class="p-3">
            <input class="w-full bg-transparent border border-transparent hover:border-border dark:hover:border-border-dark focus:border-primary dark:focus:border-primary-cyan focus:ring-0 rounded px-2 py-1 text-sm transition-colors text-text-sub dark:text-text-secondary" readonly type="text" placeholder="부품 설명"/>
        </td>
        <td class="p-3">
            <input class="w-full bg-gray-50 dark:bg-surface-dark border border-border dark:border-border-dark focus:border-primary dark:focus:border-primary-cyan focus:ring-0 rounded px-3 py-1.5 text-sm text-right font-mono" type="number" value="0" min="0"/>
        </td>
        <td class="p-3">
            <input class="w-full bg-gray-50 dark:bg-surface-dark border border-border dark:border-border-dark focus:border-primary dark:focus:border-primary-cyan focus:ring-0 rounded px-3 py-1.5 text-sm text-right font-mono" type="number" value="0" min="0"/>
        </td>
        <td class="p-4 text-right font-mono font-medium text-text-main dark:text-white">
            0
        </td>
        <td class="p-4 text-center">
            <button class="text-text-sub dark:text-text-secondary hover:text-red-500 dark:hover:text-red-400 transition-colors delete-row">
                <span class="material-symbols-outlined text-[20px]">delete</span>
            </button>
        </td>
    `;
    
    // select를 두 번째 td에 추가
    const partTd = newRow.querySelector('td:nth-child(2)');
    if (partTd) {
        partTd.appendChild(partSelectWrapper);
    }
    
    tbody.appendChild(newRow);
    
    // 삭제 버튼 이벤트
    const deleteBtn = newRow.querySelector('.delete-row');
    deleteBtn.addEventListener('click', () => {
        newRow.remove();
        updateRowNumbers();
        updateGrandTotal();
        updateDeliveryPreview();
    });
}

// 행 번호 업데이트
function updateRowNumbers() {
    const rows = document.querySelectorAll('tbody tr');
    rows.forEach((row, index) => {
        const numCell = row.querySelector('td:first-child');
        if (numCell) {
            numCell.textContent = index + 1;
        }
    });
}

// 행 소계 업데이트
function updateRowTotal(row) {
    const quantityInput = row.querySelectorAll('input[type="number"]')[0];
    const unitPriceInput = row.querySelectorAll('input[type="number"]')[1];
    const totalCell = row.querySelector('td:nth-last-child(2)');
    
    if (quantityInput && unitPriceInput && totalCell) {
        const quantity = parseInt(quantityInput.value) || 0;
        const unitPrice = parseFloat(unitPriceInput.value) || 0;
        const total = quantity * unitPrice;
        totalCell.textContent = total.toLocaleString('ko-KR');
    }
}

// 총 합계 업데이트
function updateGrandTotal() {
    const rows = document.querySelectorAll('tbody tr');
    let grandTotal = 0;
    
    rows.forEach(row => {
        const totalCell = row.querySelector('td:nth-last-child(2)');
        if (totalCell) {
            const total = parseFloat(totalCell.textContent.replace(/,/g, '')) || 0;
            grandTotal += total;
        }
    });
    
    const grandTotalCell = document.getElementById('grand-total');
    if (grandTotalCell) {
        if (rows.length > 0 && grandTotal > 0) {
            grandTotalCell.textContent = `₩ ${grandTotal.toLocaleString('ko-KR')}`;
        } else {
            grandTotalCell.textContent = '₩ 0';
        }
    }
}


// 배송 미리보기 업데이트
function updateDeliveryPreview() {
    const rows = document.querySelectorAll('tbody tr');
    let totalQty = 0;
    let totalDeliveryQty = 0;
    const deliveryItems = [];
    
    rows.forEach(row => {
        const partSelect = row.querySelector('select');
        const quantityInput = row.querySelectorAll('input[type="number"]')[0];
        const descInput = row.querySelector('input[readonly]');
        
        if (partSelect && quantityInput) {
            const selectedOption = partSelect.options[partSelect.selectedIndex];
            const partId = selectedOption ? parseInt(selectedOption.value) : null;
            const qty = parseInt(quantityInput.value) || 0;
            
            if (partId && qty > 0) {
                totalQty += qty;
                
                // 초기 납품 수량 계산 (50%)
                const deliveryQty = Math.round(qty * 0.5);
                if (deliveryQty > 0 || qty > 0) {
                    const finalDeliveryQty = deliveryQty === 0 && qty > 0 ? 1 : deliveryQty;
                    totalDeliveryQty += finalDeliveryQty;
                    
                    const partName = selectedOption.dataset.partName || '부품명 없음';
                    const partDesc = descInput ? descInput.value : '';
                    deliveryItems.push({
                        partId: partId,
                        partName: partName,
                        partDesc: partDesc,
                        orderQty: qty,
                        deliveryQty: finalDeliveryQty
                    });
                }
            }
        }
    });
    
    // 재고 미리보기도 업데이트
    updateInventoryPreview();
    
    // 예상 배송일 (오늘 날짜)
    const previewDeliveryDate = document.getElementById('preview-delivery-date');
    if (previewDeliveryDate) {
        if (rows.length > 0) {
            const today = new Date();
            previewDeliveryDate.textContent = today.toISOString().split('T')[0];
            previewDeliveryDate.classList.remove('text-text-sub/50', 'dark:text-text-secondary/50');
            previewDeliveryDate.classList.add('text-text-main', 'dark:text-white');
        } else {
            previewDeliveryDate.textContent = '-';
            previewDeliveryDate.classList.remove('text-text-main', 'dark:text-white');
            previewDeliveryDate.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
        }
    }
    
    // 발주 상태 (실제 DB 값: '요청' 또는 '발주완료')
    const previewStatus = document.getElementById('preview-status');
    if (previewStatus) {
        if (orderData.status) {
            previewStatus.textContent = orderData.status;
            previewStatus.classList.remove('text-text-sub/50', 'dark:text-text-secondary/50');
            previewStatus.classList.add('text-accent');
        } else {
            previewStatus.textContent = '-';
            previewStatus.classList.remove('text-accent');
            previewStatus.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
        }
    }
    
    // 총 초기 납품 수량
    const previewTotalQty = document.getElementById('preview-total-qty');
    if (previewTotalQty) {
        if (rows.length > 0 && totalDeliveryQty > 0) {
            previewTotalQty.textContent = `${totalDeliveryQty} 개`;
            previewTotalQty.classList.remove('text-text-sub/50', 'dark:text-text-secondary/50');
            previewTotalQty.classList.add('text-text-main', 'dark:text-white');
        } else {
            previewTotalQty.textContent = '-';
            previewTotalQty.classList.remove('text-text-main', 'dark:text-white');
            previewTotalQty.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
        }
    }
    
    // 초기 납품 항목 상세 업데이트
    const deliveryItemsPreview = document.getElementById('delivery-items-preview');
    if (deliveryItemsPreview) {
        if (deliveryItems.length > 0) {
            deliveryItemsPreview.innerHTML = deliveryItems.map(item => `
                <div class="flex items-center justify-between py-2 px-3 bg-surface dark:bg-background-dark rounded border border-border dark:border-border-dark">
                    <div class="flex-1 min-w-0">
                        <div class="text-text-main dark:text-white text-sm font-medium truncate">${item.partName}</div>
                        ${item.partDesc ? `<div class="text-text-sub dark:text-text-secondary text-xs truncate">${item.partDesc}</div>` : ''}
                    </div>
                    <div class="flex items-center gap-4 ml-4">
                        <div class="text-right">
                            <div class="text-text-sub dark:text-text-secondary text-xs">발주 수량</div>
                            <div class="text-text-main dark:text-white text-sm font-mono">${item.orderQty}</div>
                        </div>
                        <span class="material-symbols-outlined text-text-sub dark:text-text-secondary text-[16px]">arrow_forward</span>
                        <div class="text-right">
                            <div class="text-text-sub dark:text-text-secondary text-xs">초기 납품</div>
                            <div class="text-primary dark:text-primary-cyan text-sm font-mono font-semibold">${item.deliveryQty}</div>
                        </div>
                    </div>
                </div>
            `).join('');
        } else {
            deliveryItemsPreview.innerHTML = '<p class="text-text-sub/50 dark:text-text-secondary/50 text-xs text-center py-4 italic">주문 항목을 추가하면 초기 납품 수량이 표시됩니다</p>';
        }
    }
}

// Toast 알림 표시
let toastTimeout = null;

function showToast(title, message, type = 'success', autoClose = true) {
    const toast = document.getElementById('toast-notification');
    const toastContent = document.getElementById('toast-content');
    const toastIcon = document.getElementById('toast-icon');
    const toastIconSymbol = document.getElementById('toast-icon-symbol');
    const toastTitle = document.getElementById('toast-title');
    const toastMessage = document.getElementById('toast-message');
    
    if (!toast || !toastContent || !toastIcon || !toastIconSymbol || !toastTitle || !toastMessage) {
        console.error('Toast 요소를 찾을 수 없습니다.');
        return;
    }
    
    // 기존 타이머 취소
    if (toastTimeout) {
        clearTimeout(toastTimeout);
        toastTimeout = null;
    }
    
    // 제목과 메시지 설정
    toastTitle.textContent = title;
    toastMessage.textContent = message;
    
    // 타입에 따라 아이콘과 색상 설정
    if (type === 'success') {
        toastContent.className = 'bg-surface dark:bg-surface-dark border border-success/30 shadow-lg dark:shadow-[0_4px_20px_rgba(0,0,0,0.5)] rounded-lg p-4 flex items-center gap-4 min-w-[320px]';
        toastIcon.className = 'bg-success/20 text-success rounded-full p-2 flex items-center justify-center';
        toastIconSymbol.textContent = 'check_circle';
    } else if (type === 'error') {
        toastContent.className = 'bg-surface dark:bg-surface-dark border border-red-500/30 shadow-lg dark:shadow-[0_4px_20px_rgba(0,0,0,0.5)] rounded-lg p-4 flex items-center gap-4 min-w-[320px]';
        toastIcon.className = 'bg-red-500/20 text-red-500 rounded-full p-2 flex items-center justify-center';
        toastIconSymbol.textContent = 'error';
    } else if (type === 'warning') {
        toastContent.className = 'bg-surface dark:bg-surface-dark border border-yellow-500/30 shadow-lg dark:shadow-[0_4px_20px_rgba(0,0,0,0.5)] rounded-lg p-4 flex items-center gap-4 min-w-[320px]';
        toastIcon.className = 'bg-yellow-500/20 text-yellow-500 rounded-full p-2 flex items-center justify-center';
        toastIconSymbol.textContent = 'warning';
    } else {
        toastContent.className = 'bg-surface dark:bg-surface-dark border border-primary/30 shadow-lg dark:shadow-[0_4px_20px_rgba(0,0,0,0.5)] rounded-lg p-4 flex items-center gap-4 min-w-[320px]';
        toastIcon.className = 'bg-primary/20 text-primary dark:text-primary-cyan rounded-full p-2 flex items-center justify-center';
        toastIconSymbol.textContent = 'info';
    }
    
    // Toast 표시
    toast.classList.remove('hidden');
    
    // autoClose가 true일 때만 자동 닫기
    if (autoClose) {
        toastTimeout = setTimeout(() => {
            hideToast();
        }, 5000);
    }
}

// Toast 숨기기
function hideToast() {
    const toast = document.getElementById('toast-notification');
    if (toast) {
        toast.classList.add('hidden');
    }
    if (toastTimeout) {
        clearTimeout(toastTimeout);
        toastTimeout = null;
    }
}

// 창고 재고 정보 로드 및 표시
async function loadWarehouseInventory(warehouseId) {
    try {
        const inventory = await getWarehouseInventory(warehouseId);
        currentWarehouseInventory = inventory || [];
        updateInventoryPreview();
    } catch (error) {
        console.error('창고 재고 정보 조회 실패:', error);
        currentWarehouseInventory = [];
        const inventoryContainer = document.getElementById('warehouse-inventory');
        if (inventoryContainer) {
            inventoryContainer.classList.add('hidden');
        }
    }
}

// 재고 미리보기 업데이트 (현재 재고 + 예상 재고)
function updateInventoryPreview() {
    const inventoryContainer = document.getElementById('warehouse-inventory');
    const inventoryList = document.getElementById('inventory-list');
    
    if (!inventoryContainer || !inventoryList) return;
    
    // 발주 항목에서 부품별 초기 납품 수량 계산
    const rows = document.querySelectorAll('tbody tr');
    const orderItems = new Map(); // partId -> 초기 납품 수량
    
    rows.forEach(row => {
        const partSelect = row.querySelector('select');
        const quantityInput = row.querySelectorAll('input[type="number"]')[0];
        
        if (partSelect && quantityInput) {
            const selectedOption = partSelect.options[partSelect.selectedIndex];
            const partId = selectedOption ? parseInt(selectedOption.value) : null;
            const qty = parseInt(quantityInput.value) || 0;
            
            if (partId && qty > 0) {
                // 초기 납품 수량 계산 (50%)
                const deliveryQty = Math.round(qty * 0.5);
                const finalDeliveryQty = deliveryQty === 0 && qty > 0 ? 1 : deliveryQty;
                
                // 같은 부품이 여러 행에 있으면 합산
                const existingQty = orderItems.get(partId) || 0;
                orderItems.set(partId, existingQty + finalDeliveryQty);
            }
        }
    });
    
    if (currentWarehouseInventory.length === 0) {
        if (orderItems.size > 0) {
            // 재고는 없지만 발주 항목이 있는 경우
            inventoryList.innerHTML = '<p class="text-text-sub/50 dark:text-text-secondary/50 text-xs text-center py-4 italic">현재 재고가 없습니다. 발주 등록 후 재고가 생성됩니다.</p>';
            inventoryContainer.classList.remove('hidden');
        } else {
            inventoryList.innerHTML = '<p class="text-text-sub/50 dark:text-text-secondary/50 text-xs text-center py-4 italic">재고가 없습니다</p>';
            inventoryContainer.classList.remove('hidden');
        }
        return;
    }
    
    // 재고 목록 표시 (현재 재고 + 예상 재고)
    const inventoryMap = new Map();
    currentWarehouseInventory.forEach(item => {
        inventoryMap.set(item.partId, item);
    });
    
    // 재고에 있는 부품 + 발주 항목에 있는 부품 모두 표시
    const allPartIds = new Set([...inventoryMap.keys(), ...orderItems.keys()]);
    
    inventoryList.innerHTML = Array.from(allPartIds).map(partId => {
        const inventoryItem = inventoryMap.get(partId);
        const deliveryQty = orderItems.get(partId) || 0;
        
        const currentQty = inventoryItem ? inventoryItem.quantity : 0;
        const expectedQty = currentQty + deliveryQty;
        const unit = inventoryItem ? inventoryItem.unit : (allParts.find(p => p.partId === partId)?.unit || '개');
        const partName = inventoryItem ? inventoryItem.partName : (allParts.find(p => p.partId === partId)?.name || '부품명 없음');
        
        // 중복 제거 로직
        let displayContent = '';
        
        if (currentQty > 0 && deliveryQty > 0) {
            // 현재 재고 + 발주 항목 모두 있는 경우: 현재 + 추가 = 예상
            displayContent = `
                <div class="ml-4 flex items-baseline gap-2">
                    <div class="text-right">
                        <div class="text-text-sub dark:text-text-secondary text-xs mb-0.5 opacity-70">현재</div>
                        <div class="text-text-main dark:text-white text-sm font-mono font-medium">${currentQty.toLocaleString('ko-KR')}</div>
                    </div>
                    <span class="text-text-sub/50 dark:text-text-secondary/50 text-lg font-light">+</span>
                    <div class="text-right">
                        <div class="text-primary/80 dark:text-primary-cyan/80 text-xs mb-0.5">추가</div>
                        <div class="text-primary dark:text-primary-cyan text-sm font-mono font-semibold">${deliveryQty.toLocaleString('ko-KR')}</div>
                    </div>
                    <div class="text-right border-l border-border dark:border-border-dark pl-3 ml-1">
                        <div class="text-primary dark:text-primary-cyan text-xs mb-0.5 font-semibold">예상</div>
                        <div class="text-primary dark:text-primary-cyan text-lg font-mono font-bold">${expectedQty.toLocaleString('ko-KR')}</div>
                        <div class="text-primary/60 dark:text-primary-cyan/60 text-[10px] font-mono mt-0.5">${unit}</div>
                    </div>
                </div>
            `;
        } else if (currentQty > 0 && deliveryQty === 0) {
            // 현재 재고만 있는 경우: 현재 재고만 표시
            displayContent = `
                <div class="ml-4 text-right">
                    <div class="text-text-main dark:text-white text-lg font-mono font-semibold">${currentQty.toLocaleString('ko-KR')}</div>
                    <div class="text-text-sub/60 dark:text-text-secondary/60 text-[10px] font-mono mt-0.5">${unit}</div>
                </div>
            `;
        } else if (currentQty === 0 && deliveryQty > 0) {
            // 발주 항목만 있는 경우: 예상 재고만 표시 (추가 정보 생략)
            displayContent = `
                <div class="ml-4 text-right">
                    <div class="text-primary dark:text-primary-cyan text-xs mb-0.5 font-semibold">예상</div>
                    <div class="text-primary dark:text-primary-cyan text-lg font-mono font-bold">${expectedQty.toLocaleString('ko-KR')}</div>
                    <div class="text-primary/60 dark:text-primary-cyan/60 text-[10px] font-mono mt-0.5">${unit}</div>
                </div>
            `;
        } else {
            // 재고가 없는 경우
            displayContent = `
                <div class="ml-4 text-right">
                    <div class="text-text-sub/50 dark:text-text-secondary/50 text-sm font-mono">0</div>
                    <div class="text-text-sub/40 dark:text-text-secondary/40 text-[10px] font-mono mt-0.5">${unit}</div>
                </div>
            `;
        }
        
        return `
            <div class="flex items-center justify-between py-3 px-4 bg-surface dark:bg-background-dark rounded-lg border border-border dark:border-border-dark hover:border-primary/30 dark:hover:border-primary-cyan/30 transition-colors">
                <div class="flex-1 min-w-0">
                    <div class="text-text-main dark:text-white text-sm font-semibold truncate mb-1">${partName}</div>
                    <div class="text-text-sub/70 dark:text-text-secondary/70 text-xs">
                        <span class="font-mono">ID: ${partId}</span>
                    </div>
                </div>
                ${displayContent}
            </div>
        `;
    }).join('');
    
    inventoryContainer.classList.remove('hidden');
}

// 창고 재고 정보 숨기기
function hideWarehouseInventory() {
    const inventoryContainer = document.getElementById('warehouse-inventory');
    if (inventoryContainer) {
        inventoryContainer.classList.add('hidden');
    }
}

// 발주 폼 초기화 (등록 완료 후)
function resetOrderForm() {
    // 1. orderData 초기화
    orderData = {
        projectId: null,
        supplierId: null,
        engineerName: 'Admin',
        status: '요청',
        lines: [],
        warehouseId: null,
        transportMode: '트럭',
        distanceKm: 100
    };
    
    // 2. 프로젝트 입력 초기화
    const projectInput = document.getElementById('project-select');
    if (projectInput) {
        projectInput.value = '';
    }
    
    // 3. 공급업체 입력 초기화
    const supplierInput = document.getElementById('supplier-select');
    if (supplierInput) {
        supplierInput.value = '';
    }
    
    // 4. 주문 상태 초기화 (기본값 '요청')
    const orderStatusSelect = document.getElementById('order-status');
    if (orderStatusSelect) {
        orderStatusSelect.value = '요청';
    }
    
    // 5. 발주 항목 테이블 초기화 (모든 행 삭제)
    const tbody = document.querySelector('tbody');
    if (tbody) {
        tbody.innerHTML = '';
    }
    
    // 6. 창고 입력 초기화
    const warehouseInput = document.getElementById('warehouse-select');
    if (warehouseInput) {
        warehouseInput.value = '';
    }
    
    // 7. 재고 정보 숨기기
    hideWarehouseInventory();
    currentWarehouseInventory = [];
    
    // 8. 총 합계 초기화
    const grandTotalCell = document.getElementById('grand-total');
    if (grandTotalCell) {
        grandTotalCell.textContent = '₩ 0';
    }
    
    // 9. 발주 정보 미리보기 초기화
    const previewDeliveryDate = document.getElementById('preview-delivery-date');
    if (previewDeliveryDate) {
        previewDeliveryDate.textContent = '-';
        previewDeliveryDate.classList.remove('text-text-main', 'dark:text-white');
        previewDeliveryDate.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
    }
    
    const previewStatus = document.getElementById('preview-status');
    if (previewStatus) {
        previewStatus.textContent = '-';
        previewStatus.classList.remove('text-accent');
        previewStatus.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
    }
    
    const previewTotalQty = document.getElementById('preview-total-qty');
    if (previewTotalQty) {
        previewTotalQty.textContent = '-';
        previewTotalQty.classList.remove('text-text-main', 'dark:text-white');
        previewTotalQty.classList.add('text-text-sub/50', 'dark:text-text-secondary/50');
    }
    
    const deliveryItemsPreview = document.getElementById('delivery-items-preview');
    if (deliveryItemsPreview) {
        deliveryItemsPreview.innerHTML = '<p class="text-text-sub/50 dark:text-text-secondary/50 text-xs text-center py-4 italic">주문 항목을 추가하면 초기 납품 수량이 표시됩니다</p>';
    }
    
    // 주문 일자 초기화
    const orderDateEl = document.getElementById('order-date');
    if (orderDateEl) {
        const today = new Date();
        orderDateEl.textContent = today.toISOString().split('T')[0];
        orderDateEl.classList.remove('text-text-sub/50', 'dark:text-text-secondary/50');
        orderDateEl.classList.add('text-text-main', 'dark:text-white');
    }
}

// 발주 등록 처리
async function submitOrder() {
    // 기본 필수 입력값 검증
    // 필수 입력값 누락 시 등록 불가 확인
    
    // 프로젝트 ID 선택 확인
    if (!orderData.projectId) {
        showToast('입력 오류', '프로젝트를 선택하세요.', 'error');
        return;
    }
    
    // 공급업체 ID 선택 확인
    if (!orderData.supplierId) {
        showToast('입력 오류', '공급업체를 선택하세요.', 'error');
        return;
    }
    
    // 발주 항목 목록 유효성 검증
    const rows = document.querySelectorAll('tbody tr');
    orderData.lines = [];
    let hasInvalidLine = false;
    let invalidLineMessage = '';

    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        const partSelect = row.querySelector('select');
        const quantityInput = row.querySelectorAll('input[type="number"]')[0];
        const unitPriceInput = row.querySelectorAll('input[type="number"]')[1];
        
        // 부품 선택 여부 확인
        if (!partSelect || !partSelect.value) {
            hasInvalidLine = true;
            invalidLineMessage = `${i + 1}번째 행: 부품을 선택하세요.`;
            break;
        }
        
        const partId = parseInt(partSelect.value);
        const quantity = parseInt(quantityInput.value) || 0;
        const unitPrice = parseFloat(unitPriceInput.value) || 0;
        
        // 수량 양수 확인
        if (quantity <= 0) {
            hasInvalidLine = true;
            invalidLineMessage = `${i + 1}번째 행: 수량은 1 이상이어야 합니다.`;
            break;
        }
        
        // 단가 양수 확인
        if (unitPrice <= 0) {
            hasInvalidLine = true;
            invalidLineMessage = `${i + 1}번째 행: 단가는 0보다 커야 합니다.`;
            break;
        }
        
        // 유효 항목 목록에 추가
        orderData.lines.push({
            partId: partId,
            quantity: quantity,
            unitPrice: unitPrice
        });
    }
    
    // 항목 없음 확인
    if (orderData.lines.length === 0) {
        showToast('입력 오류', '최소 하나 이상의 발주 항목을 추가하세요.', 'error');
        return;
    }
    
    // 유효하지 않은 항목 존재 시 중단
    if (hasInvalidLine) {
        showToast('입력 오류', invalidLineMessage, 'error');
        return;
    }
    
    // 창고 ID 선택 확인 (재고 반영용)
    if (!orderData.warehouseId) {
        showToast('입력 오류', '입고할 창고를 선택하세요.', 'error');
        return;
    }
    
    const submitBtn = document.getElementById('submit-order-btn');
    
    try {
        // 로딩 상태 표시
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="material-symbols-outlined">hourglass_empty</span>처리 중...';
        }
        
        const result = await createOrder(orderData);
        
        // 결과 알림 표시 (납품서 ID 포함)
        showToast('발주 등록 완료', `발주서 ID: ${result.poid}, 납품서 ID: DEL-${result.deliveryId}`, 'success', false);
        
        // 입력 폼 초기화
        resetOrderForm();
        
    } catch (error) {
        console.error('발주 등록 실패:', error);
        const errorMessage = error.message || '알 수 없는 오류가 발생했습니다.';
        showToast('발주 등록 실패', errorMessage, 'error');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span class="material-symbols-outlined">send</span>주문 등록';
        }
    }
}

