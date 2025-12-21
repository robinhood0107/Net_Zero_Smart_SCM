const API_BASE = '/api';
const API_TIMEOUT = 10000; // 10초 타임아웃 설정

async function apiCall(endpoint, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            signal: controller.signal,
            ...options
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = `API 오류: ${response.status}`;
            try {
                const errorJson = JSON.parse(errorText);
                if (errorJson.error) {
                    errorMessage = errorJson.error;
                } else if (errorJson.message) {
                    errorMessage = errorJson.message;
                }
            } catch (e) {
                // JSON 파싱 실패 시 원본 텍스트 반환
                if (errorText) {
                    errorMessage = errorText;
                }
            }
            throw new Error(errorMessage);
        }

        return await response.json();
    } catch (error) {
        clearTimeout(timeoutId);
        
        if (error.name === 'AbortError') {
            throw new Error('요청 시간이 초과되었습니다. 서버가 응답하지 않습니다.');
        }
        
        if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
            throw new Error('서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.');
        }
        
        console.error('API 호출 실패:', error);
        throw error;
    }
}

async function getMainSummary() {
    return await apiCall('/main/summary');
}

async function getProject(projectId) {
    return await apiCall(`/projects/${projectId}`);
}

async function searchProjects(keyword, limit = 10, page = 0) {
    return await apiCall(`/projects/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}&page=${page}`);
}

async function getDashboardStats(projectId) {
    return await apiCall(`/projects/${projectId}/stats`);
}

async function getSuppliers(esgGrades = null, minRatio = null, maxRatio = null) {
    let params = new URLSearchParams();
    if (esgGrades && esgGrades.length > 0) {
        esgGrades.forEach(g => params.append('esgGrades', g));
    }
    if (minRatio !== null) params.append('minRatio', minRatio);
    if (maxRatio !== null) params.append('maxRatio', maxRatio);

    const queryString = params.toString();
    return await apiCall(`/suppliers${queryString ? '?' + queryString : ''}`);
}

async function getSupplierDetail(supplierId) {
    return await apiCall(`/suppliers/${supplierId}`);
}

async function getSupplierOrders(supplierId, page = 0, size = 5) {
    return await apiCall(`/suppliers/${supplierId}/orders?page=${page}&size=${size}`);
}

async function getProjectOptions(keyword = '') {
    return await apiCall(`/orders/projects?keyword=${encodeURIComponent(keyword)}`);
}

async function getSupplierOptions() {
    return await apiCall('/orders/suppliers');
}

async function searchParts(keyword = '') {
    return await apiCall(`/orders/parts?keyword=${encodeURIComponent(keyword)}`);
}

async function getWarehouseOptions() {
    return await apiCall('/orders/warehouses');
}

async function getWarehouseInventory(warehouseId) {
    return await apiCall(`/orders/warehouses/${warehouseId}/inventory`);
}

async function createOrder(orderData) {
    return await apiCall('/orders', {
        method: 'POST',
        body: JSON.stringify(orderData)
    });
}

// ========== 설정 및 로그 관련 API ==========

async function getLogs(level = null, limit = 100, search = null) {
    let params = new URLSearchParams();
    if (level) params.append('level', level);
    params.append('limit', limit);
    if (search) params.append('search', search);
    return await apiCall(`/settings/logs?${params.toString()}`);
}

async function getSystemStatus() {
    return await apiCall('/settings/status');
}
