/**
 * Recovery Test - 스파이크 후 복구 관찰 테스트
 *
 * 목적: 급격한 부하 이후 시스템이 정상 응답 수준으로 복구되는지 검증
 *
 * 단계:
 *   Phase 1 (3분): 50→500 VU (스파이크)
 *   Phase 2 (1분): 500 VU 유지
 *   Phase 3 (2분): 500→50 VU (램프다운)
 *   Phase 4 (10분): 50 VU 저부하 (복구 관찰)
 *
 * 복구 완료 기준 (Phase 4):
 *   - read p95 < 1.5s
 *   - 에러율 < 0.1%
 *
 * 실행 방법:
 *   ./run-recovery.sh
 *   BASE_URL=https://stg.tasteam.kr k6 run recovery_test.js
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
    executePersonalJourney,
    executeWritingJourney,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, createJourneyMetrics } from '../../shared/test-utils.js';

const metrics = createJourneyMetrics();
const USER_POOL = Number(__ENV.USER_POOL || '100');

// ============ Journey 선택 ============
const JOURNEYS = [
    { name: 'browsing',  weight: 28, fn: executeBrowsingJourney },
    { name: 'searching', weight: 18, fn: executeSearchingJourney },
    { name: 'group',     weight: 12, fn: executeGroupJourney },
    { name: 'personal',  weight: 12, fn: executePersonalJourney },
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

// ============ Test Options ============
export const options = withQuickRunOptions({
    setupTimeout: '5m',
    scenarios: {
        recovery: {
            executor: 'ramping-vus',
            startVUs: 50,
            stages: [
                { target: 500, duration: '3m' },  // Phase 1: 스파이크
                { target: 500, duration: '1m' },  // Phase 2: 유지
                { target: 50,  duration: '2m' },  // Phase 3: 램프다운
                { target: 50,  duration: '10m' }, // Phase 4: 복구 관찰
            ],
        },
    },
    thresholds: {
        // 전체 구간 기준
        'http_req_duration':             ['p(95)<3000'],
        'http_req_failed':               ['rate<0.01'],
        // Phase 4 복구 기준 (전체 임계치로 근사 - 실제 Phase 4 측정은 Grafana에서 확인)
        'http_req_duration{type:read}':  ['p(95)<1500'],
        'http_req_duration{type:write}': ['p(95)<3000'],
    },
});

// ============ Setup ============
export function setup() {
    logTestStart('Recovery Test', BASE_URL);
    console.log('   Phase 1 (3m): 50→500 VU 스파이크');
    console.log('   Phase 2 (1m): 500 VU 유지');
    console.log('   Phase 3 (2m): 500→50 VU 램프다운');
    console.log('   Phase 4 (10m): 50 VU 저부하 복구 관찰');

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
        console.warn('⚠️ 그룹 컨텍스트를 확보하지 못해 복구 테스트의 write/group 커버리지가 줄어듭니다.');
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

    sleep(1 + Math.random() * 3);
}

// ============ Teardown ============
export function teardown(data) {
    console.log('🏁 Recovery Test 완료');
    console.log('   Phase 4 (저부하 10분) 구간의 p95 추이를 Grafana에서 확인하세요.');
    console.log('   복구 완료 기준: read p95 < 1.5s, 에러율 < 0.1%');
}
