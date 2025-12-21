// 공통 유틸리티 및 상수

// ========== 데이터 포맷팅 함수 ==========

function formatCurrency(amount, currency = '₩') {
    return currency + amount.toLocaleString('ko-KR');
}

function formatCompact(amount, currency = '₩') {
    if (amount >= 1000000) {
        return `${currency}${(amount / 1000000).toFixed(1)}M`;
    } else if (amount >= 1000) {
        return `${currency}${(amount / 1000).toFixed(1)}K`;
    }
    return `${currency}${amount.toLocaleString()}`;
}

function formatPercent(ratio) {
    return `${(ratio * 100).toFixed(1)}%`;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
}

// ========== HTML 특수문자 변환 ==========

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== 국가 코드 매핑 정보 ==========

const countryCodeMap = {
    '한국': 'kr',
    '대한민국': 'kr',
    'Korea': 'kr',
    'South Korea': 'kr',
    '미국': 'us',
    'United States': 'us',
    'USA': 'us',
    'US': 'us',
    '중국': 'cn',
    'China': 'cn',
    '일본': 'jp',
    'Japan': 'jp',
    '독일': 'de',
    'Germany': 'de',
    '영국': 'gb',
    'United Kingdom': 'gb',
    'UK': 'gb',
    '프랑스': 'fr',
    'France': 'fr',
    '이탈리아': 'it',
    'Italy': 'it',
    '스페인': 'es',
    'Spain': 'es',
    '네덜란드': 'nl',
    'Netherlands': 'nl',
    '벨기에': 'be',
    'Belgium': 'be',
    '스웨덴': 'se',
    'Sweden': 'se',
    '노르웨이': 'no',
    'Norway': 'no',
    '덴마크': 'dk',
    'Denmark': 'dk',
    '핀란드': 'fi',
    'Finland': 'fi',
    '스위스': 'ch',
    'Switzerland': 'ch',
    '폴란드': 'pl',
    'Poland': 'pl',
    '그리스': 'gr',
    'Greece': 'gr',
    '포르투갈': 'pt',
    'Portugal': 'pt',
    '아일랜드': 'ie',
    'Ireland': 'ie',
    '오스트리아': 'at',
    'Austria': 'at',
    '체코': 'cz',
    'Czech Republic': 'cz',
    'Czech': 'cz',
    '헝가리': 'hu',
    'Hungary': 'hu',
    '슬로베니아': 'si',
    'Slovenia': 'si',
    '슬로바키아': 'sk',
    'Slovakia': 'sk',
    '크로아티아': 'hr',
    'Croatia': 'hr',
    '루마니아': 'ro',
    'Romania': 'ro',
    '불가리아': 'bg',
    'Bulgaria': 'bg',
    '에스토니아': 'ee',
    'Estonia': 'ee',
    '라트비아': 'lv',
    'Latvia': 'lv',
    '리투아니아': 'lt',
    'Lithuania': 'lt',
    '아이슬란드': 'is',
    'Iceland': 'is',
    '룩셈부르크': 'lu',
    'Luxembourg': 'lu',
    '몰타': 'mt',
    'Malta': 'mt',
    '키프로스': 'cy',
    'Cyprus': 'cy',
    '러시아': 'ru',
    'Russia': 'ru',
    '인도': 'in',
    'India': 'in',
    '베트남': 'vn',
    'Vietnam': 'vn',
    '태국': 'th',
    'Thailand': 'th',
    '싱가포르': 'sg',
    'Singapore': 'sg',
    '말레이시아': 'my',
    'Malaysia': 'my',
    '인도네시아': 'id',
    'Indonesia': 'id',
    '필리핀': 'ph',
    'Philippines': 'ph',
    '호주': 'au',
    'Australia': 'au',
    '뉴질랜드': 'nz',
    'New Zealand': 'nz',
    '브라질': 'br',
    'Brazil': 'br',
    '멕시코': 'mx',
    'Mexico': 'mx',
    '캐나다': 'ca',
    'Canada': 'ca',
    '터키': 'tr',
    'Turkey': 'tr',
    '사우디아라비아': 'sa',
    'Saudi Arabia': 'sa',
    'UAE': 'ae',
    '아랍에미리트': 'ae',
    'United Arab Emirates': 'ae',
    '이스라엘': 'il',
    'Israel': 'il',
    '이집트': 'eg',
    'Egypt': 'eg',
    '남아프리카': 'za',
    'South Africa': 'za',
    '아르헨티나': 'ar',
    'Argentina': 'ar',
    '칠레': 'cl',
    'Chile': 'cl',
    '콜롬비아': 'co',
    'Colombia': 'co',
    '페루': 'pe',
    'Peru': 'pe',
    '우루과이': 'uy',
    'Uruguay': 'uy',
    '베네수엘라': 've',
    'Venezuela': 've',
    '파나마': 'pa',
    'Panama': 'pa',
    '쿠바': 'cu',
    'Cuba': 'cu',
    '도미니카공화국': 'do',
    'Dominican Republic': 'do',
    '자메이카': 'jm',
    'Jamaica': 'jm',
    '바하마': 'bs',
    'Bahamas': 'bs',
    '파푸아뉴기니': 'pg',
    'Papua New Guinea': 'pg',
    '피지': 'fj',
    'Fiji': 'fj',
    '방글라데시': 'bd',
    'Bangladesh': 'bd',
    '파키스탄': 'pk',
    'Pakistan': 'pk',
    '스리랑카': 'lk',
    'Sri Lanka': 'lk',
    '미얀마': 'mm',
    'Myanmar': 'mm',
    '캄보디아': 'kh',
    'Cambodia': 'kh',
    '라오스': 'la',
    'Laos': 'la',
    '브루나이': 'bn',
    'Brunei': 'bn',
    '대만': 'tw',
    'Taiwan': 'tw',
    '홍콩': 'hk',
    'Hong Kong': 'hk',
    '마카오': 'mo',
    'Macau': 'mo',
    '몽골': 'mn',
    'Mongolia': 'mn',
    '카자흐스탄': 'kz',
    'Kazakhstan': 'kz',
    '우즈베키스탄': 'uz',
    'Uzbekistan': 'uz',
    '아제르바이잔': 'az',
    'Azerbaijan': 'az',
    '조지아': 'ge',
    'Georgia': 'ge',
    '아르메니아': 'am',
    'Armenia': 'am',
    '우크라이나': 'ua',
    'Ukraine': 'ua',
    '벨라루스': 'by',
    'Belarus': 'by',
    '리비아': 'ly',
    'Libya': 'ly',
    '튀니지': 'tn',
    'Tunisia': 'tn',
    '알제리': 'dz',
    'Algeria': 'dz',
    '모로코': 'ma',
    'Morocco': 'ma',
    '케냐': 'ke',
    'Kenya': 'ke',
    '나이지리아': 'ng',
    'Nigeria': 'ng',
    '가나': 'gh',
    'Ghana': 'gh',
    '앙골라': 'ao',
    'Angola': 'ao',
    '모잠비크': 'mz',
    'Mozambique': 'mz',
    '탄자니아': 'tz',
    'Tanzania': 'tz',
    '세이셸': 'sc',
    'Seychelles': 'sc',
    '모리셔스': 'mu',
    'Mauritius': 'mu',
    '마다가스카르': 'mg',
    'Madagascar': 'mg'
};

// 국가 이름으로 국기 아이콘 HTML 가져오기
function getCountryFlag(countryName) {
    if (!countryName) return '';
    const trimmed = countryName.trim();
    const countryCode = countryCodeMap[trimmed] || countryCodeMap[trimmed.toUpperCase()];
    if (countryCode) {
        return `<span class="fi fi-${countryCode} rounded-sm" style="width: 20px; height: 15px; display: inline-block; vertical-align: middle;"></span>`;
    }
    return '';
}

// ========== ESG 등급별 색상 매핑 ==========

const esgGradeColors = {
    'A': {
        bg: 'bg-green-50 dark:bg-green-500/10',
        text: 'text-green-700 dark:text-green-400',
        border: 'border-green-200 dark:border-green-500/20'
    },
    'B': {
        bg: 'bg-blue-50 dark:bg-primary-cyan/10',
        text: 'text-blue-700 dark:text-primary-cyan',
        border: 'border-blue-200 dark:border-primary-cyan/20'
    },
    'C': {
        bg: 'bg-yellow-50 dark:bg-yellow-500/10',
        text: 'text-yellow-700 dark:text-yellow-400',
        border: 'border-yellow-200 dark:border-yellow-500/20'
    },
    'D': {
        bg: 'bg-orange-50 dark:bg-orange-500/10',
        text: 'text-orange-700 dark:text-orange-400',
        border: 'border-orange-200 dark:border-orange-500/20'
    }
};

// ========== 발주서 상태별 표시 정보 ==========

const orderStatusInfo = {
    '요청': {
        text: '요청',
        bg: 'bg-gray-50 dark:bg-gray-500/10',
        textColor: 'text-gray-700 dark:text-gray-400',
        border: 'border-gray-200 dark:border-gray-500/20',
        icon: 'pending',
        label: '요청됨'
    },
    '발주완료': {
        text: '발주완료',
        bg: 'bg-blue-50 dark:bg-blue-500/10',
        textColor: 'text-blue-700 dark:text-blue-400',
        border: 'border-blue-200 dark:border-blue-500/20',
        icon: 'check_circle',
        label: '발주완료'
    },
    '취소': {
        text: '취소',
        bg: 'bg-gray-50 dark:bg-gray-500/10',
        textColor: 'text-gray-700 dark:text-gray-400',
        border: 'border-gray-200 dark:border-gray-500/20',
        icon: 'cancel',
        label: '취소됨'
    },
    '검수중': {
        text: '검수중',
        bg: 'bg-warning/10',
        textColor: 'text-warning',
        border: 'border-warning/20',
        icon: 'pending',
        label: '검수중'
    }
};

