import { check, sleep } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import {
    BASE_URL,
    search,
    prepareKeywordHotspot,
    pickKeyword,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

// ============ Test Options ============
const metrics = new SuccessMetrics(['search_success_count']);

export const options = withQuickRunOptions({
    scenarios: {
        search_stress: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 1000,
            maxVUs: 10000,
            stages: [
                { target: 100, duration: '30s' },   // Warm up
                { target: 500, duration: '1m' },    // Load
                { target: 1000, duration: '1m' },   // High Load
                { target: 2000, duration: '1m' },   // Very High Load
                { target: 4000, duration: '1m' },   // Stress
                { target: 6000, duration: '1m' },   // Extreme Stress
            ],
            exec: 'searchScenario',
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<2000'], // 2s timeout for stress
        'http_req_failed': ['rate<0.05'],    // Allow 5% failure under stress
    },
});

// ============ Test Data ============
const SEARCH_KEYWORDS = [
    '파스타', '피자', '삼겹살', '치킨', '카페', '커피',
    '강남', '역삼', '홍대', '이태원', '성수',
    '점심', '저녁', '회식', '데이트', '가성비',
    '맛집', '비건', '디저트', '한식', '일식', '중식',
    'Burger', 'Sushi', 'Pasta', 'BBQ',
    '아', '가', 'A', 'B', // Short queries
    '맛있는 파스타 집 추천해주세요', // Long query
];

// ============ Setup ============
export function setup() {
    logTestStart('Search Stress Test', BASE_URL);

    // 검색 API는 인증 불필요 - 로그인 없이 바로 테스트 진행
    console.log('✅ Setup 완료: 비로그인 검색 테스트 모드');

    return {
        tokens: [], // 토큰 없이 진행
        hotspot: prepareKeywordHotspot(SEARCH_KEYWORDS),
    };
}

// ============ Scenarios ============

export function searchScenario(data) {
    let token = null;

    // 토큰이 있으면 랜덤하게 선택하여 사용
    if (data.tokens && data.tokens.length > 0) {
        token = randomItem(data.tokens);
    }

    // 랜덤 키워드 선택
    const keyword = pickKeyword({ hotspot: data.hotspot });

    // 검색 요청
    const res = search(token, keyword);

    // 응답 상태코드 상세 분석
    check(res, {
        'Status 200 (OK)': (r) => r.status === 200,
        'Status 429 (Too Many Requests)': (r) => r.status === 429,
        'Status 500 (Internal Server Error)': (r) => r.status === 500,
        'Status 502 (Bad Gateway - Server Down?)': (r) => r.status === 502,
        'Status 503 (Service Unavailable)': (r) => r.status === 503,
        'Status 504 (Gateway Timeout - Time out)': (r) => r.status === 504,
        'Status 0 (Connection Error)': (r) => r.status === 0,
    });

    if (res.status === 200) {
        metrics.add(1, 'search_success_count');
    }
}

export function teardown(data) {
    console.log('🏁 Search Stress Test 완료');
}
