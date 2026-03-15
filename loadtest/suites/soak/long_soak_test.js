/**
 * Long Soak Test - 24h / 48h 장기 부하 테스트 (P0)
 *
 * 목적: 장시간 운영 시 메모리 누수, 연결 고갈, 성능 저하 탐지
 *
 * SOAK_MODE 환경변수로 선택:
 *   24h (기본값) - ramp 1h(40 VU) → steady 22h(100 VU) → close 1h
 *   48h          - 0~12h(100 VU) → 12~36h(120 VU 피크) → 36~47h(100 VU) → 47~48h(close)
 *
 * CACHE_MODE 환경변수 (태깅용):
 *   on  (기본값) - 캐시 활성화 상태 측정
 *   off          - 캐시 비활성화 상태 측정 (비교용)
 *
 * 판정 기준 (30분 롤링, Grafana 확인):
 *   - 에러율 ≤ 0.3%
 *   - read p95 < 1s
 *
 * 실행 방법:
 *   SOAK_MODE=24h ./run-soak.sh
 *   SOAK_MODE=48h CACHE_MODE=off ./run-soak.sh
 */

import { sleep } from 'k6';
import {
    BASE_URL,
    createState,
    batchLogin,
    getReviewKeywords,
    executeBrowsingJourney,
    executeSearchingJourney,
    executeGroupJourney,
    executeSubgroupJourney,
    executePersonalJourney,
    executeChatJourney,
    executeWritingJourney,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, createJourneyMetrics } from '../../shared/test-utils.js';

const SOAK_MODE  = __ENV.SOAK_MODE  || '24h';
const CACHE_MODE = __ENV.CACHE_MODE || 'on';
const USER_POOL = Number(__ENV.USER_POOL || '100');

const metrics = createJourneyMetrics();

// ============ Journey 선택 (realistic_test.js와 동일 가중치) ============
const JOURNEYS = [
    { name: 'browsing',  weight: 28, fn: executeBrowsingJourney },
    { name: 'searching', weight: 18, fn: executeSearchingJourney },
    { name: 'group',     weight: 12, fn: executeGroupJourney },
    { name: 'subgroup',  weight: 12, fn: executeSubgroupJourney },
    { name: 'personal',  weight: 12, fn: executePersonalJourney },
    { name: 'chat',      weight: 10, fn: executeChatJourney },
    { name: 'writing',   weight:  8, fn: executeWritingJourney },
];
const TOTAL_WEIGHT = JOURNEYS.reduce((sum, j) => sum + j.weight, 0);

function selectJourney() {
    let rand = Math.random() * TOTAL_WEIGHT;
    for (const journey of JOURNEYS) {
        rand -= journey.weight;
        if (rand <= 0) return journey;
    }
    return JOURNEYS[0];
}

// ============ 시나리오별 stages ============
const SOAK_STAGES = {
    '24h': [
        { target: 40,  duration: '30m' },  // 워밍업 (40% VU)
        { target: 100, duration: '30m' },  // 램프업
        { target: 100, duration: '22h' },  // 스테디 (22시간)
        { target: 0,   duration: '1h'  },  // 종료
    ],
    '48h': [
        { target: 40,  duration: '30m' },  // 워밍업
        { target: 100, duration: '30m' },  // 램프업 → 24h 수준
        { target: 100, duration: '11h' },  // 0~12h 스테디
        { target: 120, duration: '1h'  },  // 12h 피크 진입
        { target: 120, duration: '24h' },  // 12~36h 1.2x 피크
        { target: 100, duration: '1h'  },  // 피크 복귀
        { target: 100, duration: '10h' },  // 36~47h 스테디
        { target: 0,   duration: '1h'  },  // 종료
    ],
};

export const options = withQuickRunOptions({
    setupTimeout: '5m',
    scenarios: {
        soak: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: SOAK_STAGES[SOAK_MODE] || SOAK_STAGES['24h'],
            tags: { cache_mode: CACHE_MODE, soak_mode: SOAK_MODE },
        },
    },
    thresholds: {
        'http_req_duration':             ['p(95)<2000'],
        'http_req_failed':               ['rate<0.003'],  // 0.3%
        'http_req_duration{type:read}':  ['p(95)<1000'],
        'http_req_duration{type:write}': ['p(95)<3000'],
    },
});

// ============ Setup ============
export function setup() {
    logTestStart(`Long Soak Test [${SOAK_MODE}] cache=${CACHE_MODE}`, BASE_URL);
    const mode = SOAK_MODE === '48h'
        ? '48h: 0~12h(100 VU) → 12~36h(120 VU) → 36~47h(100 VU) → 47~48h(close)'
        : '24h: ramp 1h → steady 22h(100 VU) → close 1h';
    console.log(`   모드: ${mode}`);
    console.log(`   판정 기준: 30분 롤링 에러율 ≤ 0.3%, read p95 < 1s`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);

    const groupContext = resolveGroupContext(baseToken);
    const subgroupContext = resolveSubgroupChatContext(baseToken, groupContext.groupId);

    if (!groupContext.groupId) {
        console.warn('⚠️ 그룹 컨텍스트를 확보하지 못해 장기 소크 테스트의 write/group 커버리지가 줄어듭니다.');
    }

    console.log(`✅ Setup 완료: tokens=${tokens.length}개, groupId=${groupContext.groupId}, subgroupId=${subgroupContext.subgroupId}, chatRoomId=${subgroupContext.chatRoomId}`);
    return {
        tokens,
        groupId: groupContext.groupId,
        subgroupId: subgroupContext.subgroupId,
        chatRoomId: subgroupContext.chatRoomId,
        keywordIds,
    };
}

// ============ Main VU Function ============
export default function(data) {
    if (!data || !data.tokens || data.tokens.length === 0) {
        console.error('❌ Setup 데이터 없음');
        return;
    }

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.groupId = data.groupId;
    state.subgroupId = data.subgroupId;
    state.chatRoomId = data.chatRoomId;
    state.keywordIds = data.keywordIds;

    const journey = selectJourney();
    const count = journey.fn(state);
    metrics.add(count, `${journey.name}_count`);

    // Think time: 1~5초 (실제 사용자 행동 간격)
    sleep(1 + Math.random() * 4);
}

// ============ Teardown ============
export function teardown(data) {
    console.log(`🏁 Long Soak Test [${SOAK_MODE}] 완료`);
    console.log('   Grafana 대시보드에서 30분 롤링 에러율 및 p95 추이를 확인하세요.');
    console.log('   판정 기준: 에러율 ≤ 0.3%, read p95 < 1s');
}
