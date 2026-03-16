import { check, sleep } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import {
    BASE_URL,
    search,
    prepareKeywordHotspot,
    pickKeyword,
    randomSearchLocation,
    getSearchVariationSummary,
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
                { target: 1500, duration: '1m' },   // Very High Load
                { target: 2200, duration: '1m' },   // Stress
                { target: 3000, duration: '1m' },   // Peak
            ],
            exec: 'searchScenario',
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<2000'], // 2s timeout for stress
        'http_req_failed': ['rate<0.05'],    // Allow 5% failure under stress
    },
});

// ============ Setup ============
export function setup() {
    logTestStart('Search Stress Test', BASE_URL);
    const searchVariation = getSearchVariationSummary();

    console.log(`✅ Setup 완료: 비로그인 검색 테스트 모드, search combinations=${searchVariation.combinationCount} (${searchVariation.keywordCount} keywords = ${searchVariation.primaryKeywordCount} primary + ${searchVariation.typingKeywordCount} typing @ ${Math.round(searchVariation.typingKeywordShare * 100)}% x ${searchVariation.locationCount} locations x ${searchVariation.radiusCount} radii)`);

    return {
        tokens: [], // 토큰 없이 진행
        hotspot: prepareKeywordHotspot(),
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
    const loc = randomSearchLocation();

    // 검색 요청
    const res = search(token, keyword, loc);

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
