const urlParams = new URLSearchParams(window.location.search);
let currentProjectId = parseInt(urlParams.get('id')) || 1;

document.addEventListener('DOMContentLoaded', async () => {
    hideProjectInfo();
    hideError();
    hideLoading();
    
    if (urlParams.get('id')) {
        await loadDashboard(currentProjectId);
    } else {
        showProjectInfo();
        const projectTitle = document.getElementById('project-title');
        if (projectTitle) {
            projectTitle.textContent = '프로젝트를 검색하세요';
        }
    }
    
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', handleSearch);
    }
    
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleSearch();
        });
    }
    
    const dismissErrorBtn = document.getElementById('dismiss-error-btn');
    if (dismissErrorBtn) {
        dismissErrorBtn.addEventListener('click', () => {
            hideError();
        });
    }
    
    setupCIIEventListeners();
});

function setupCIIEventListeners() {
    const trySetup = () => {
        const calculateBtn = document.getElementById('calculate-cii-btn');
        const resetBtn = document.getElementById('reset-cii-btn');
        
        if (calculateBtn) {
            if (!calculateBtn.onclick) {
                calculateBtn.onclick = async function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    await calculateCII();
                };
            }
        }
        
        if (resetBtn) {
            if (!resetBtn.onclick) {
                resetBtn.onclick = async function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    await resetCIIToDefault();
                };
            }
        }
        
        // 요소 부재 시 재시도 중단 (무한 루프 방지)
    };
    
    trySetup();
}

function calculateCII() {
    const dwtInput = document.getElementById('cii-dwt-input');
    const nmInput = document.getElementById('cii-nm-input');
    const fuelInput = document.getElementById('cii-fuel-input');
    
    if (!dwtInput || !nmInput || !fuelInput) {
        console.error('CII 입력 필드를 찾을 수 없습니다.');
        alert('CII 입력 필드를 찾을 수 없습니다.');
        return;
    }
    
    const dwt = parseFloat(dwtInput.value) || 0;
    const nm = parseFloat(nmInput.value) || 0;
    const fuelConsumptionTonnes = parseFloat(fuelInput.value) || 0;
    
    if (dwt <= 0 || nm <= 0 || fuelConsumptionTonnes <= 0) {
        alert('dwt, nm, 연료 소비량 값을 올바르게 입력해주세요. (현재 값: dwt=' + dwt + ', nm=' + nm + ', 연료 소비량=' + fuelConsumptionTonnes + '톤)');
        return;
    }
    
    const cii = calculateCIIValue(dwt, nm, fuelConsumptionTonnes);
    updateCIIDisplay(cii);
}

function calculateCIIValue(dwt, nm, fuelConsumptionTonnes) {
    const emissionFactor = 3.114;
    const totalCO2Grams = fuelConsumptionTonnes * emissionFactor * 1000000.0;
    const transportWork = dwt * nm;
    
    if (transportWork === 0) {
        return null;
    }
    
    const cii = totalCO2Grams / transportWork;
    return Math.round(cii * 100000.0) / 100000.0;
}

async function resetCIIToDefault() {
    const dwtInput = document.getElementById('cii-dwt-input');
    const nmInput = document.getElementById('cii-nm-input');
    const fuelInput = document.getElementById('cii-fuel-input');
    
    try {
        const project = await getProject(currentProjectId);
        
        const defaultDWT = getDefaultDWT(project.shipType);
        const defaultNM = getDefaultNM(project.shipType);
        const defaultFuel = getDefaultFuel(project.shipType);
        
        if (dwtInput) dwtInput.value = defaultDWT;
        if (nmInput) nmInput.value = defaultNM;
        if (fuelInput) fuelInput.value = defaultFuel;
        
        const cii = calculateCIIValue(defaultDWT, defaultNM, defaultFuel);
        updateCIIDisplay(cii);
    } catch (error) {
        // 서버 측 에러 로깅 처리
        alert('기본값 재설정 중 오류가 발생했습니다.');
    }
}

async function handleSearch() {
    const searchInput = document.getElementById('search-input');
    if (!searchInput) return;
    
    let keyword = searchInput.value.trim();
    
    // 검색어 유효성 검증 및 정제
    if (!keyword) {
        alert('프로젝트 ID 또는 선박명을 입력하세요.');
        return;
    }
    
    // XSS 방지를 위한 최대 길이 제한
    if (keyword.length > 100) {
        keyword = keyword.substring(0, 100);
        searchInput.value = keyword;
    }
    
    // 특수 문자 제거 및 입력 값 정제
    const sanitizedKeyword = keyword.replace(/[<>\"']/g, '');
    
    try {
        const projectId = parseInt(sanitizedKeyword, 10);
        if (!isNaN(projectId) && projectId > 0 && projectId <= 2147483647) {
            // 프로젝트 ID 숫자 범위 검증
            currentProjectId = projectId;
            await loadDashboard(projectId);
        } else {
            // 선박명 검색 (허용 문자: 알파벳, 한글, 숫자, 공백)
            const validKeyword = sanitizedKeyword.replace(/[^a-zA-Z0-9가-힣\s]/g, '');
            if (!validKeyword || validKeyword.length === 0) {
                alert('유효하지 않은 검색어입니다.');
                return;
            }
            
            const results = await searchProjects(validKeyword, 10);
            if (results.length === 0) {
                alert('검색 결과가 없습니다.');
                return;
            }
            
            // 검색 결과의 첫 번째 프로젝트 ID 검증
            const firstResultId = results[0]?.projectId;
            if (firstResultId && typeof firstResultId === 'number' && firstResultId > 0) {
                currentProjectId = firstResultId;
                await loadDashboard(currentProjectId);
            } else {
                alert('검색 결과가 유효하지 않습니다.');
            }
        }
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        alert('검색 중 오류가 발생했습니다.');
    }
}

async function loadDashboard(projectId) {
    // 입력 값 유효성 검증
    if (!projectId || (typeof projectId !== 'number' && typeof projectId !== 'string')) {
        showError('오류 발생', '유효하지 않은 프로젝트 ID입니다.', 'general', null);
        return;
    }
    
    const numericId = typeof projectId === 'string' ? parseInt(projectId, 10) : projectId;
    if (isNaN(numericId) || numericId <= 0 || numericId > 2147483647) {
        showError('오류 발생', '유효하지 않은 프로젝트 ID입니다.', 'general', null);
        return;
    }
    
    hideError();
    showLoading();
    hideProjectInfo();
    
    try {
        const stats = await getDashboardStats(numericId);
        hideLoading();
        showProjectInfo();
        updateDashboard(stats);
    } catch (error) {
        // 에러 로깅은 서버 측에서 처리
        hideLoading();
        
        let errorTitle = '오류 발생';
        let errorText = '데이터를 불러오는 중 오류가 발생했습니다.';
        let errorType = 'general';
        
        if (error.message.includes('404') || error.message.includes('not found') || error.message.includes('찾을 수 없습니다')) {
            errorTitle = '프로젝트를 찾을 수 없습니다';
            errorText = `프로젝트 ID "${projectId}"에 해당하는 프로젝트가 존재하지 않습니다. 프로젝트 ID나 선박명을 확인해주세요.`;
            errorType = 'not_found';
        } else if (error.message.includes('network') || error.message.includes('fetch') || error.message.includes('Failed to fetch')) {
            errorTitle = '네트워크 오류';
            errorText = '서버에 연결할 수 없습니다. 인터넷 연결을 확인하거나 잠시 후 다시 시도해주세요.';
            errorType = 'network';
        } else if (error.message.includes('500') || error.message.includes('Internal Server Error')) {
            errorTitle = '서버 오류';
            errorText = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            errorType = 'server';
        }
        
        showError(errorTitle, errorText, errorType, projectId);
    }
}

function showError(title, text, type, projectId) {
    const errorDiv = document.getElementById('error-message');
    const errorTitle = document.getElementById('error-title');
    const errorText = document.getElementById('error-text');
    const retryBtn = document.getElementById('retry-btn');
    
    if (errorDiv && errorTitle && errorText) {
        errorTitle.textContent = title;
        errorText.textContent = text;
        
        const iconSpan = errorDiv.querySelector('.material-symbols-outlined');
        if (iconSpan) {
            if (type === 'not_found') {
                iconSpan.textContent = 'search_off';
            } else if (type === 'network') {
                iconSpan.textContent = 'wifi_off';
            } else if (type === 'server') {
                iconSpan.textContent = 'dns';
            } else {
                iconSpan.textContent = 'error';
            }
        }
        
        if (retryBtn) {
            retryBtn.onclick = () => {
                loadDashboard(projectId);
            };
        }
        
        errorDiv.classList.remove('hidden');
    }
}

function hideError() {
    const errorDiv = document.getElementById('error-message');
    if (errorDiv) {
        errorDiv.classList.add('hidden');
    }
}

function showLoading() {
    const loadingDiv = document.getElementById('loading-state');
    if (loadingDiv) {
        loadingDiv.classList.remove('hidden');
    }
}

function hideLoading() {
    const loadingDiv = document.getElementById('loading-state');
    if (loadingDiv) {
        loadingDiv.classList.add('hidden');
    }
}

function showProjectInfo() {
    const projectCard = document.getElementById('project-info-card');
    const statsCards = document.getElementById('stats-cards');
    
    if (projectCard) {
        projectCard.classList.remove('hidden');
    }
    if (statsCards) {
        statsCards.classList.remove('hidden');
    }
}

function hideProjectInfo() {
    const projectCard = document.getElementById('project-info-card');
    const statsCards = document.getElementById('stats-cards');
    
    if (projectCard) {
        projectCard.classList.add('hidden');
    }
    if (statsCards) {
        statsCards.classList.add('hidden');
    }
}

function updateDashboard(stats) {
    const project = stats.project;
    
    const projectTitle = document.getElementById('project-title');
    if (projectTitle) {
        const shipName = project.shipName ? escapeHtml(project.shipName) : '프로젝트 정보 없음';
        projectTitle.textContent = shipName;
    }
    
    const projectIdBadge = document.getElementById('project-id-badge');
    if (projectIdBadge && project.projectId) {
        // 프로젝트 ID 유효성 검증
        const projectId = typeof project.projectId === 'number' ? project.projectId : parseInt(project.projectId, 10);
        if (!isNaN(projectId) && projectId > 0) {
            projectIdBadge.textContent = `ID: ${projectId}`;
        } else {
            projectIdBadge.textContent = 'ID: -';
        }
    }
    
    const projectInfo = document.getElementById('project-info');
    if (projectInfo) {
        const shipType = escapeHtml(project.shipType || 'N/A');
        const status = escapeHtml(project.status || 'N/A');
        projectInfo.innerHTML = `선종: ${shipType} <span class="mx-2 text-border dark:hidden">|</span><span class="hidden dark:inline"> | </span> 상태: ${status}`;
    }
    
    const projectStatusEl = document.getElementById('project-status');
    if (projectStatusEl) {
        const statusText = project.status === '진행중' ? '진행중' : project.status || '대기중';
        const escapedStatus = escapeHtml(statusText);
        projectStatusEl.innerHTML = `<span class="w-1.5 h-1.5 rounded-full bg-success animate-pulse"></span>${escapedStatus}`;
    }
    
    const contractDateEl = document.getElementById('contract-date');
    if (contractDateEl && project.contractDate) {
        contractDateEl.textContent = formatDate(project.contractDate) || 'N/A';
    }
    
    const deliveryDateEl = document.getElementById('delivery-date');
    if (deliveryDateEl && project.deliveryDueDate) {
        deliveryDateEl.textContent = formatDate(project.deliveryDueDate) || 'N/A';
    }
    
    const supplierCountEl = document.getElementById('supplier-count');
    if (supplierCountEl && stats.topSuppliers) {
        supplierCountEl.textContent = `${stats.topSuppliers.length} 개사`;
    }
    
    const projectStatusTextEl = document.getElementById('project-status-text');
    if (projectStatusTextEl) {
        projectStatusTextEl.textContent = project.status || 'N/A';
    }
    
    const totalAmount = stats.totalOrderAmount || 0;
    const totalAmountEl = document.getElementById('total-amount');
    if (totalAmountEl) {
        totalAmountEl.textContent = formatCompact(totalAmount, '₩');
    }
    
    const supplierList = document.getElementById('supplier-list');
    if (supplierList && stats.topSuppliers && stats.topSuppliers.length > 0) {
        supplierList.innerHTML = '';
        stats.topSuppliers.forEach((supplier) => {
            // 공급업체 ID 유효성 검증
            const supplierId = supplier.supplierId;
            if (!supplierId || (typeof supplierId !== 'number' && typeof supplierId !== 'string')) {
                return; // 유효하지 않은 supplierId는 건너뛰기
            }
            
            const numericId = typeof supplierId === 'string' ? parseInt(supplierId, 10) : supplierId;
            if (isNaN(numericId) || numericId <= 0) {
                return; // 유효하지 않은 ID는 건너뛰기
            }
            
            const percentage = totalAmount > 0 ? (supplier.amount / totalAmount * 100).toFixed(0) : 0;
            const supplierName = escapeHtml(supplier.name || 'N/A');
            const formattedAmount = formatCompact(supplier.amount || 0, '₩');
            
            // 안전한 URL 생성 (특수문자 인코딩)
            const safeUrl = `supplier.html?id=${encodeURIComponent(numericId)}`;
            
            const supplierDiv = document.createElement('div');
            supplierDiv.className = 'group block';
            supplierDiv.innerHTML = `
                <a href="${safeUrl}" class="block">
                    <div class="flex justify-between text-sm mb-2">
                        <span class="text-text-main dark:text-white font-semibold group-hover:text-primary dark:group-hover:text-primary-dark transition-colors">${supplierName}</span>
                        <span class="text-text-main dark:text-white font-mono font-bold">${formattedAmount}</span>
                    </div>
                    <div class="h-2.5 w-full bg-gray-100 dark:bg-[#111618] rounded-full overflow-hidden border border-gray-100 dark:border-0">
                        <div class="h-full bg-primary dark:bg-primary-dark rounded-full group-hover:bg-primary-hover dark:group-hover:bg-sky-400 transition-colors shadow-sm dark:shadow-none" style="width: ${Math.min(100, Math.max(0, parseFloat(percentage)))}%"></div>
                    </div>
                </a>
            `;
            supplierList.appendChild(supplierDiv);
        });
    }
    
    const totalEmissionKg = stats.totalEmission || 0;
    const transportEmission = stats.transportEmission || 0;
    const storageEmission = stats.storageEmission || 0;
    const processingEmission = stats.processingEmission || 0;
    
    const totalEmissionEl = document.getElementById('total-emission');
    if (totalEmissionEl) {
        totalEmissionEl.textContent = totalEmissionKg.toLocaleString('ko-KR', { 
            minimumFractionDigits: 1, 
            maximumFractionDigits: 1 
        });
    }
    
    const transportPercent = totalEmissionKg > 0 ? (transportEmission / totalEmissionKg * 100).toFixed(0) : 0;
    const storagePercent = totalEmissionKg > 0 ? (storageEmission / totalEmissionKg * 100).toFixed(0) : 0;
    const processingPercent = totalEmissionKg > 0 ? (processingEmission / totalEmissionKg * 100).toFixed(0) : 0;
    
    const transportPercentEl = document.getElementById('transport-percent');
    if (transportPercentEl) transportPercentEl.textContent = transportPercent + '%';
    
    const storagePercentEl = document.getElementById('storage-percent');
    if (storagePercentEl) storagePercentEl.textContent = storagePercent + '%';
    
    const processingPercentEl = document.getElementById('processing-percent');
    if (processingPercentEl) processingPercentEl.textContent = processingPercent + '%';
    
    if (stats.carbonIntensity !== null && stats.carbonIntensity !== undefined) {
        const intensity = stats.carbonIntensity;
        const intensityEl = document.getElementById('carbon-intensity');
        if (intensityEl) {
            intensityEl.textContent = intensity.toFixed(1);
        }
        
        let grade = 'C';
        if (intensity < 800) grade = 'A';
        else if (intensity < 1000) grade = 'B';
        else if (intensity < 1200) grade = 'C';
        else if (intensity < 1500) grade = 'D';
        else grade = 'E';
        
        const gradeEl = document.getElementById('intensity-grade');
        if (gradeEl) {
            gradeEl.textContent = `등급 ${grade}`;
            gradeEl.className = 'text-xs px-2 py-1 rounded font-bold border transition-colors';
            if (grade === 'A') {
                gradeEl.className += ' bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400 border-green-200 dark:border-green-500/30';
            } else if (grade === 'B') {
                gradeEl.className += ' bg-sky-100 dark:bg-blue-500/20 text-sky-700 dark:text-blue-400 border-sky-200 dark:border-blue-500/30';
            } else if (grade === 'C') {
                gradeEl.className += ' bg-yellow-100 dark:bg-yellow-500/20 text-yellow-700 dark:text-yellow-400 border-yellow-200 dark:border-yellow-500/30';
            } else if (grade === 'D') {
                gradeEl.className += ' bg-orange-100 dark:bg-orange-500/20 text-orange-700 dark:text-orange-400 border-orange-200 dark:border-orange-500/30';
            } else if (grade === 'E') {
                gradeEl.className += ' bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border-red-200 dark:border-red-500/30';
            }
        }
        
        const currentGradeDisplay = document.getElementById('current-grade-display');
        if (currentGradeDisplay) {
            currentGradeDisplay.textContent = grade;
            currentGradeDisplay.className = 'text-8xl font-black mb-8';
            if (grade === 'A') {
                currentGradeDisplay.className += ' text-green-600 dark:text-green-400';
            } else if (grade === 'B') {
                currentGradeDisplay.className += ' text-sky-600 dark:text-blue-400';
            } else if (grade === 'C') {
                currentGradeDisplay.className += ' text-yellow-600 dark:text-yellow-400';
            } else if (grade === 'D') {
                currentGradeDisplay.className += ' text-orange-600 dark:text-orange-400';
            } else if (grade === 'E') {
                currentGradeDisplay.className += ' text-red-600 dark:text-red-400';
            }
        }
        
        updateIntensityGradeColors(grade);
    } else {
        const intensityEl = document.getElementById('carbon-intensity');
        if (intensityEl) {
            intensityEl.textContent = '-';
        }
        const gradeEl = document.getElementById('intensity-grade');
        if (gradeEl) {
            gradeEl.textContent = '-';
            gradeEl.className = 'text-xs bg-sky-100 dark:bg-blue-500/20 text-sky-700 dark:text-blue-400 border border-sky-200 dark:border-blue-500/30 px-2 py-1 rounded font-bold dark:text-white';
        }
        const currentGradeDisplay = document.getElementById('current-grade-display');
        if (currentGradeDisplay) {
            currentGradeDisplay.textContent = '-';
            currentGradeDisplay.className = 'text-8xl font-black text-text-main dark:text-white mb-8';
        }
        updateIntensityGradeColors(null);
    }
    
    const defaultDWT = getDefaultDWT(project.shipType);
    const defaultNM = getDefaultNM(project.shipType);
    const defaultFuel = getDefaultFuel(project.shipType);
    
    const dwtInput = document.getElementById('cii-dwt-input');
    const nmInput = document.getElementById('cii-nm-input');
    const fuelInput = document.getElementById('cii-fuel-input');
    if (dwtInput && !dwtInput.value) {
        dwtInput.value = defaultDWT;
    }
    if (nmInput && !nmInput.value) {
        nmInput.value = defaultNM;
    }
    if (fuelInput && !fuelInput.value) {
        fuelInput.value = defaultFuel;
    }
    
    const initialCII = calculateCIIValue(defaultDWT, defaultNM, defaultFuel);
    updateCIIDisplay(initialCII);
    
    setupCIIEventListeners();
    
    const newUrl = new URL(window.location);
    newUrl.searchParams.set('id', project.projectId);
    window.history.pushState({}, '', newUrl);
}

function updateIntensityGradeColors(grade) {
    const gradeBars = document.querySelectorAll('#intensity-card .grid.grid-cols-5 > div');
    if (gradeBars && gradeBars.length === 5) {
        gradeBars.forEach((bar, index) => {
            const barDiv = bar.querySelector('div');
            const labelSpan = bar.querySelector('span');
            if (barDiv && labelSpan) {
                if (index === 0) {
                    barDiv.className = 'h-2 bg-gray-200 dark:bg-[#283539] rounded mb-2 group-hover:bg-green-500/50 transition-colors';
                    labelSpan.className = 'text-2xl text-text-sub dark:text-text-secondary font-bold';
                } else if (index === 1) {
                    barDiv.className = 'h-2 bg-gray-200 dark:bg-[#283539] rounded mb-2 group-hover:bg-primary/50 transition-colors';
                    labelSpan.className = 'text-2xl text-text-sub dark:text-text-secondary font-bold';
                } else if (index === 2) {
                    barDiv.className = 'h-2 bg-gray-200 dark:bg-[#283539] rounded mb-2 group-hover:bg-yellow-500/50 transition-colors';
                    labelSpan.className = 'text-2xl text-text-sub dark:text-text-secondary font-bold';
                } else if (index === 3) {
                    barDiv.className = 'h-2 bg-gray-200 dark:bg-[#283539] rounded mb-2 group-hover:bg-orange-500/50 transition-colors';
                    labelSpan.className = 'text-2xl text-text-sub dark:text-text-secondary font-bold';
                } else if (index === 4) {
                    barDiv.className = 'h-2 bg-gray-200 dark:bg-[#283539] rounded mb-2 group-hover:bg-red-500/50 transition-colors';
                    labelSpan.className = 'text-2xl text-text-sub dark:text-text-secondary font-bold';
                }
            }
        });
        
        if (!grade) return;
        
        const gradeIndex = { 'A': 0, 'B': 1, 'C': 2, 'D': 3, 'E': 4 }[grade];
        if (gradeIndex !== undefined && gradeBars[gradeIndex]) {
            const barDiv = gradeBars[gradeIndex].querySelector('div');
            const labelSpan = gradeBars[gradeIndex].querySelector('span');
            if (barDiv && labelSpan) {
                if (grade === 'A') {
                    barDiv.className = 'h-2 bg-green-500 dark:bg-green-500 rounded mb-2 group-hover:bg-green-500/50 transition-colors shadow-sm dark:shadow-[0_0_8px_rgba(34,197,94,0.6)]';
                    labelSpan.className = 'text-2xl text-green-600 dark:text-green-400 font-black';
                } else if (grade === 'B') {
                    barDiv.className = 'h-2 bg-primary dark:bg-primary-dark rounded mb-2 shadow-md dark:shadow-[0_0_10px_rgba(19,182,236,0.6)] shadow-primary/30 dark:shadow-none group-hover:bg-primary dark:group-hover:bg-primary-dark transition-colors';
                    labelSpan.className = 'text-2xl text-primary dark:text-white font-black';
                } else if (grade === 'C') {
                    barDiv.className = 'h-2 bg-yellow-500 dark:bg-yellow-500 rounded mb-2 group-hover:bg-yellow-500/50 transition-colors shadow-sm dark:shadow-[0_0_8px_rgba(234,179,8,0.6)]';
                    labelSpan.className = 'text-2xl text-yellow-600 dark:text-yellow-400 font-black';
                } else if (grade === 'D') {
                    barDiv.className = 'h-2 bg-orange-500 dark:bg-orange-500 rounded mb-2 group-hover:bg-orange-500/50 transition-colors shadow-sm dark:shadow-[0_0_8px_rgba(249,115,22,0.6)]';
                    labelSpan.className = 'text-2xl text-orange-600 dark:text-orange-400 font-black';
                } else if (grade === 'E') {
                    barDiv.className = 'h-2 bg-red-500 dark:bg-red-500 rounded mb-2 group-hover:bg-red-500/50 transition-colors shadow-sm dark:shadow-[0_0_8px_rgba(239,68,68,0.6)]';
                    labelSpan.className = 'text-2xl text-red-600 dark:text-red-400 font-black';
                }
            }
        }
    }
}

function updateCIIGradeDisplay(ciiGrade) {
    if (!ciiGrade) {
        ciiGrade = 'C';
    }
    
    const ciiGradeEl = document.getElementById('cii-grade');
    if (ciiGradeEl) {
        ciiGradeEl.textContent = `등급 ${ciiGrade}`;
        ciiGradeEl.className = 'text-xs px-2 py-1 rounded font-bold border transition-colors';
        if (ciiGrade === 'A') {
            ciiGradeEl.className += ' bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400 border-green-200 dark:border-green-500/30';
        } else if (ciiGrade === 'B') {
            ciiGradeEl.className += ' bg-sky-100 dark:bg-blue-500/20 text-sky-700 dark:text-blue-400 border-sky-200 dark:border-blue-500/30';
        } else if (ciiGrade === 'C') {
            ciiGradeEl.className += ' bg-yellow-100 dark:bg-yellow-500/20 text-yellow-700 dark:text-yellow-400 border-yellow-200 dark:border-yellow-500/30';
        } else if (ciiGrade === 'D') {
            ciiGradeEl.className += ' bg-orange-100 dark:bg-orange-500/20 text-orange-700 dark:text-orange-400 border-orange-200 dark:border-orange-500/30';
        } else if (ciiGrade === 'E') {
            ciiGradeEl.className += ' bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400 border-red-200 dark:border-red-500/30';
        }
    }
    // cii-grade 요소가 없으면 조용히 무시
}

function updateCIIDisplay(cii) {
    // CII 관련 요소가 없을 수 있으므로 조용히 처리
    if (cii !== null && cii !== undefined) {
        const ciiEl = document.getElementById('ship-cii');
        if (ciiEl) {
            const formattedValue = cii.toFixed(5);
            ciiEl.textContent = formattedValue;
            ciiEl.style.display = 'none';
            ciiEl.offsetHeight;
            ciiEl.style.display = '';
        }
        // 요소가 없으면 조용히 무시
    } else {
        const ciiEl = document.getElementById('ship-cii');
        if (ciiEl) {
            ciiEl.textContent = '-';
        }
        const ciiGradeEl = document.getElementById('cii-grade');
        if (ciiGradeEl) {
            ciiGradeEl.textContent = '-';
            ciiGradeEl.className = 'text-xs bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-500/30 px-2 py-1 rounded font-bold dark:text-white';
        }
    }
}

function getDefaultDWT(shipType) {
    if (!shipType) return 50000;
    const type = shipType.toLowerCase();
    if (type.includes('벌크') || type.includes('bulk')) return 80000;
    if (type.includes('컨테이너') || type.includes('container')) return 100000;
    if (type.includes('일반화물') || type.includes('cargo')) return 30000;
    if (type.includes('유조선') || type.includes('tanker') || type.includes('vlcc')) return 150000;
    if (type.includes('lng') || type.includes('lpg') || type.includes('운반선')) return 150000;
    if (type.includes('여객') || type.includes('passenger')) return 50000;
    if (type.includes('냉동') || type.includes('reefer')) return 15000;
    if (type === 'ro-ro' || type.includes('ro-ro')) return 25000;
    if (type.includes('해양공급') || type.includes('osv')) return 8000;
    return 50000;
}

function getDefaultNM(shipType) {
    if (!shipType) return 40000;
    const type = shipType.toLowerCase();
    if (type.includes('벌크') || type.includes('bulk')) return 45000;
    if (type.includes('컨테이너') || type.includes('container')) return 60000;
    if (type.includes('일반화물') || type.includes('cargo')) return 35000;
    if (type.includes('유조선') || type.includes('tanker') || type.includes('vlcc')) return 50000;
    if (type.includes('lng') || type.includes('lpg') || type.includes('운반선')) return 50000;
    if (type.includes('여객') || type.includes('passenger')) return 30000;
    if (type.includes('냉동') || type.includes('reefer')) return 40000;
    if (type === 'ro-ro' || type.includes('ro-ro')) return 35000;
    if (type.includes('해양공급') || type.includes('osv')) return 25000;
    return 40000;
}

function getDefaultFuel(shipType) {
    if (!shipType) return 10000;
    const type = shipType.toLowerCase();
    const defaultDWT = getDefaultDWT(shipType);
    const defaultNM = getDefaultNM(shipType);
    const baseFuel = 10000;
    const baseDWT = 50000;
    const baseNM = 20000;
    const estimatedFuel = baseFuel * (defaultDWT / baseDWT) * (defaultNM / baseNM);
    return Math.round(estimatedFuel);
}

