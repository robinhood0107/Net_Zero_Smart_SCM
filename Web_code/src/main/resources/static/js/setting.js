// 설정 페이지 스크립트

let currentLogLevel = '';
let currentSearch = '';
let refreshInterval = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 이벤트 리스너 등록
    setupEventListeners();

    try {
        // 초기 데이터 병렬 로드
        await Promise.allSettled([
            loadSystemStatus(),
            loadLogs()
        ]);
    } catch (error) {
        console.error('초기 로드 실패:', error);
    }

    // 30초 주기 자동 새로고침
    refreshInterval = setInterval(async () => {
        try {
            await Promise.allSettled([
                loadSystemStatus(),
                loadLogs()
            ]);
        } catch (error) {
            console.error('자동 새로고침 실패:', error);
        }
    }, 30000);
});

function setupEventListeners() {
    // 로그 레벨 필터링
    const levelFilter = document.getElementById('log-level-filter');
    if (levelFilter) {
        levelFilter.addEventListener('change', async (e) => {
            currentLogLevel = e.target.value;
            await loadLogs();
        });
    }

    // 로그 검색 기능
    const searchInput = document.getElementById('log-search-input');
    if (searchInput) {
        let searchTimeout = null;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(async () => {
                currentSearch = e.target.value.trim();
                await loadLogs();
            }, 500); // 500ms 디바운스
        });
    }

    // 수동 새로고침
    const refreshBtn = document.getElementById('refresh-logs-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', async () => {
            await loadSystemStatus();
            await loadLogs();
        });
    }
}

// 시스템 상태 정보 조회
async function loadSystemStatus() {
    try {
        const status = await getSystemStatus();
        
        // API 오류 응답 처리
        if (status && status.error) {
            throw new Error(status.error);
        }
        
        // 상태 데이터 부재 처리
        if (!status) {
            throw new Error('상태 데이터를 받을 수 없습니다.');
        }
        
        // 마지막 동기화 시간 기본값 설정
        if (!status.lastSync) {
            const now = new Date();
            status.lastSync = now.getFullYear() + '-' + 
                String(now.getMonth() + 1).padStart(2, '0') + '-' + 
                String(now.getDate()).padStart(2, '0') + ' ' + 
                String(now.getHours()).padStart(2, '0') + ':' + 
                String(now.getMinutes()).padStart(2, '0') + ':' + 
                String(now.getSeconds()).padStart(2, '0');
        }
        
        updateSystemStatus(status);
    } catch (error) {
        console.error('시스템 상태 조회 실패:', error);
        // 시간 포맷팅 변환
        const now = new Date();
        const formattedTime = now.getFullYear() + '-' + 
            String(now.getMonth() + 1).padStart(2, '0') + '-' + 
            String(now.getDate()).padStart(2, '0') + ' ' + 
            String(now.getHours()).padStart(2, '0') + ':' + 
            String(now.getMinutes()).padStart(2, '0') + ':' + 
            String(now.getSeconds()).padStart(2, '0');
        
        updateSystemStatus({
            status: '오류',
            dbConnected: false,
            activeConnections: 0,
            queryLatency: 0,
            lastSync: formattedTime
        });
    }
}

// 시스템 상태 UI 업데이트
function updateSystemStatus(status) {
    // 기본 상태값 설정
    if (!status) {
        status = {
            dbConnected: false,
            activeConnections: 0,
            queryLatency: 0,
            lastSync: null
        };
    }
    
    // 상태 UI 표시
    const statusEl = document.getElementById('system-status');
    if (statusEl) {
        // 상태 컨테이너 요소 탐색
        const statusContainer = statusEl.querySelector('div:first-child');
        if (statusContainer) {
            // 상태 요소 가져오기
            const spans = statusContainer.querySelectorAll('span');
            const statusText = spans[1]; // 두 번째 span이 상태 텍스트
            const statusDot = spans[0]; // 첫 번째 span이 상태 점
            
            if (statusText && statusDot) {
                if (status.dbConnected) {
                    statusText.textContent = '시스템 정상';
                    statusText.className = 'text-accent dark:text-emerald-500 text-sm font-bold';
                    statusDot.className = 'size-2.5 rounded-full bg-accent dark:bg-emerald-500 animate-pulse shadow-[0_0_8px_rgba(16,185,129,0.5)]';
                } else {
                    statusText.textContent = '연결 오류';
                    statusText.className = 'text-red-500 dark:text-red-400 text-sm font-bold';
                    statusDot.className = 'size-2.5 rounded-full bg-red-500 animate-pulse shadow-[0_0_8px_rgba(239,68,68,0.5)]';
                }
            }
        }
    }

    // 마지막 동기화 시간 표시
    const lastSyncEl = document.getElementById('last-sync');
    if (lastSyncEl) {
        if (status.lastSync) {
            try {
                // 날짜 문자열 파싱
                let syncTime;
                if (status.lastSync.includes('T')) {
                    // ISO 형식
                    syncTime = new Date(status.lastSync);
                } else {
                    // "yyyy-MM-dd HH:mm:ss" 형식
                    const [datePart, timePart] = status.lastSync.split(' ');
                    if (datePart && timePart) {
                        const [year, month, day] = datePart.split('-');
                        const [hour, minute, second] = timePart.split(':');
                        syncTime = new Date(parseInt(year), parseInt(month) - 1, parseInt(day), 
                                           parseInt(hour), parseInt(minute), parseInt(second || 0));
                    } else {
                        throw new Error('날짜 형식 오류');
                    }
                }
                
                if (isNaN(syncTime.getTime())) {
                    throw new Error('날짜 파싱 실패');
                }
                
                const now = new Date();
                const diffMs = now - syncTime;
                const diffMins = Math.floor(diffMs / 60000);
                
                if (diffMins < 1) {
                    lastSyncEl.textContent = '방금 전';
                } else if (diffMins < 60) {
                    lastSyncEl.textContent = `${diffMins}분 전`;
                } else {
                    const diffHours = Math.floor(diffMins / 60);
                    lastSyncEl.textContent = `${diffHours}시간 전`;
                }
            } catch (e) {
                // 파싱 오류 시 원본 표시
                lastSyncEl.textContent = status.lastSync || '알 수 없음';
            }
        } else {
            lastSyncEl.textContent = '알 수 없음';
        }
    }

}

// 로그 데이터 조회
async function loadLogs() {
    const logEntries = document.getElementById('log-entries');
    if (!logEntries) {
        console.error('log-entries 요소를 찾을 수 없습니다.');
        return;
    }
    
    try {
        const result = await getLogs(currentLogLevel || null, 100, currentSearch || null);
        
        // API가 에러 객체를 반환한 경우 처리
        if (result && result.error) {
            throw new Error(result.error);
        }
        
        updateLogs(result);
    } catch (error) {
        console.error('로그 조회 실패:', error);
        const errorMessage = error.message || '알 수 없는 오류';
        logEntries.innerHTML = `
            <div class="px-5 py-8 text-center">
                <div class="text-red-500 dark:text-red-400 font-semibold mb-2">로그 조회 실패</div>
                <div class="text-text-sub dark:text-text-secondary text-sm">${escapeHtml(errorMessage)}</div>
                <div class="text-text-sub dark:text-text-secondary text-xs mt-2">로그 파일이 없거나 서버에 연결할 수 없습니다.</div>
            </div>
        `;
        const logCount = document.getElementById('log-count');
        if (logCount) {
            logCount.textContent = '오류 발생';
        }
    }
}

// 로그 UI 업데이트
function updateLogs(result) {
    const logEntries = document.getElementById('log-entries');
    const logCount = document.getElementById('log-count');

    if (!logEntries) {
        console.error('log-entries 요소를 찾을 수 없습니다.');
        return;
    }

    // 결과값 부재 처리
    if (!result) {
        logEntries.innerHTML = `
            <div class="px-5 py-8 text-center text-red-500 dark:text-red-400">
                로그 데이터를 받을 수 없습니다.
            </div>
        `;
        if (logCount) {
            logCount.textContent = '오류';
        }
        return;
    }

    if (!result.logs || result.logs.length === 0) {
        logEntries.innerHTML = `
            <div class="px-5 py-8 text-center text-text-sub dark:text-text-secondary">
                표시할 로그가 없습니다.<br>
                <span class="text-xs mt-2 block">로그 파일이 없거나 필터 조건에 맞는 로그가 없습니다.</span>
            </div>
        `;
        if (logCount) {
            logCount.textContent = `${result.total || 0}개 중 0개 표시`;
        }
        return;
    }

    // 로그 필터링 적용
    const filteredLogs = result.logs.filter(log => {
        const level = log.level || '';
        const message = (log.message || '').toLowerCase();
        const fullMessage = (log.fullMessage || '').toLowerCase();
        
        // 중요 로그(경고, 오류) 강제 표시
        if (level === 'WARNING' || level === 'SEVERE') {
            return true;
        }
        
        // 주요 키워드 포함 INFO 로그 표시
        const importantKeywords = [
            'error', 'exception', '예외', '오류', '에러',
            'transaction', '트랜잭션',
            'failed', '실패',
            'timeout', '타임아웃',
            'connection', '연결',
            'database', '데이터베이스', 'db',
            'critical', '심각',
            'warning', '경고'
        ];
        
        return importantKeywords.some(keyword => 
            message.includes(keyword) || fullMessage.includes(keyword)
        );
    });

    if (filteredLogs.length === 0) {
        logEntries.innerHTML = `
            <div class="px-5 py-8 text-center text-text-sub dark:text-text-secondary">
                표시할 로그가 없습니다.<br>
                <span class="text-xs mt-2 block">필터 조건에 맞는 로그가 없습니다.</span>
            </div>
        `;
        if (logCount) {
            logCount.textContent = `${result.total || 0}개 중 0개 표시`;
        }
        return;
    }

    // 로그 개수 표시형식 업데이트
    if (logCount) {
        logCount.innerHTML = `
            <span class="text-text-main dark:text-white font-bold font-mono">${result.total || 0}</span>개 중 
            <span class="text-text-main dark:text-white font-bold font-mono">${filteredLogs.length}</span>개 표시
        `;
    }

    // 로그 항목 렌더링
    try {
        logEntries.innerHTML = filteredLogs.map(log => createLogEntryHTML(log)).join('');
    } catch (error) {
        console.error('로그 엔트리 생성 실패:', error);
        logEntries.innerHTML = `
            <div class="px-5 py-8 text-center text-red-500 dark:text-red-400">
                로그 표시 중 오류가 발생했습니다: ${error.message}
            </div>
        `;
    }
}

// 개별 로그 항목 HTML 생성
function createLogEntryHTML(log) {
    const level = log.level || 'INFO';
    const levelColors = getLogLevelColors(level);
    const bgColor = level === 'SEVERE' ? 'bg-red-50/30 dark:bg-red-900/10' : 
                    level === 'WARNING' ? 'bg-amber-50/50 dark:bg-transparent' : 
                    '';

    return `
        <div class="grid grid-cols-12 px-5 dark:px-4 py-3.5 dark:py-3 hover:bg-slate-50 dark:hover:bg-white/5 transition-colors ${bgColor} ${level === 'SEVERE' ? 'border-l-4 border-red-500' : ''}">
            <div class="col-span-3 lg:col-span-2 text-text-sub dark:text-text-secondary text-xs flex items-center font-medium">${log.timestamp || ''}</div>
            <div class="col-span-2 lg:col-span-1 text-center flex items-center justify-center">
                <span class="px-2 py-1 dark:py-0.5 rounded text-[10px] font-bold ${levelColors.bg} ${levelColors.text} ${levelColors.border} ${level === 'SEVERE' ? 'animate-pulse' : ''}">${level}</span>
            </div>
            <div class="col-span-2 lg:col-span-2 text-text-main dark:text-text-secondary font-semibold dark:font-normal truncate pr-2 flex items-center" title="${log.source || ''}">${log.source || 'System'}</div>
            <div class="col-span-5 lg:col-span-7 ${level === 'SEVERE' ? 'text-red-900 dark:text-red-100' : level === 'WARNING' ? 'text-amber-800 dark:text-yellow-100' : 'text-text-sub dark:text-gray-300'} break-all flex items-center font-medium dark:font-normal">
                ${escapeHtml(log.message || '')}
            </div>
        </div>
    `;
}

// 로그 레벨별 스타일 반환
function getLogLevelColors(level) {
    switch (level) {
        case 'SEVERE':
            return {
                bg: 'bg-red-100 dark:bg-red-500/20',
                text: 'text-red-700 dark:text-red-400',
                border: 'border border-red-200 dark:border-red-500/30'
            };
        case 'WARNING':
            return {
                bg: 'bg-amber-100 dark:bg-yellow-500/20',
                text: 'text-amber-700 dark:text-yellow-400',
                border: 'border border-amber-200 dark:border-yellow-500/30'
            };
        case 'INFO':
        default:
            return {
                bg: 'bg-blue-100 dark:bg-blue-500/20',
                text: 'text-blue-700 dark:text-blue-400',
                border: 'border border-blue-200 dark:border-blue-500/30'
            };
    }
}

// 종료 시 타이머 정리
window.addEventListener('beforeunload', () => {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});

