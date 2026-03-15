/**
 * Stress Test - Read-heavy / Write-heavy / Search-only 스트레스 테스트
 *
 * 목적: 지속적인 고부하 상황에서 시스템 한계 탐색
 *
 * TEST_TYPE 환경변수로 선택:
 *   read-heavy   (기본값) - 100→1000 VU, 20분, browsingJourney + searchingJourney
 *   write-heavy            - 50→200 VU, 20분, 리뷰40%+채팅40%+즐겨찾기20%
 *   search-only            - ramping-arrival-rate 20→300 RPS, 20분
 *
 * 실행 방법:
 *   TEST_TYPE=read-heavy ./run-stress.sh
 *   TEST_TYPE=write-heavy BASE_URL=https://stg.tasteam.kr k6 run stress_test.js
 */

import { sleep } from 'k6';
import {
    BASE_URL,
    createState,
    batchLogin,
    getReviewKeywords,
    executeBrowsingJourney,
    executeSearchingJourney,
    search,
    createReview,
    sendChatMessage,
    randomLocation,
    randomKeyword,
    randomChatMessage,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

const TEST_TYPE = __ENV.TEST_TYPE || 'read-heavy';
const USER_POOL = Number(__ENV.USER_POOL || '100');

const metrics = new SuccessMetrics(['stress_success_count']);

// ============ 시나리오별 options ============

const SCENARIO_OPTIONS = {
    'read-heavy': {
        setupTimeout: '5m',
        scenarios: {
            stress: {
                executor: 'ramping-vus',
                startVUs: 10,
                stages: [
                    { target: 100,  duration: '2m' },
                    { target: 500,  duration: '5m' },
                    { target: 1000, duration: '5m' },
                    { target: 1000, duration: '5m' },
                    { target: 0,    duration: '3m' },
                ],
                exec: 'readHeavy',
            },
        },
        thresholds: {
            'http_req_duration{type:read}': ['p(95)<1000'],
            'http_req_failed':              ['rate<0.01'],
        },
    },
    'write-heavy': {
        setupTimeout: '5m',
        scenarios: {
            stress: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 50,  duration: '2m' },
                    { target: 150, duration: '5m' },
                    { target: 200, duration: '5m' },
                    { target: 200, duration: '5m' },
                    { target: 0,   duration: '3m' },
                ],
                exec: 'writeHeavy',
            },
        },
        thresholds: {
            'http_req_duration{type:write}': ['p(95)<3000'],
            'http_req_failed':               ['rate<0.01'],
        },
    },
    'search-only': {
        setupTimeout: '5m',
        scenarios: {
            stress: {
                executor: 'ramping-arrival-rate',
                startRate: 20,
                timeUnit: '1s',
                preAllocatedVUs: 500,
                maxVUs: 5000,
                stages: [
                    { target: 20,  duration: '2m' },
                    { target: 150, duration: '5m' },
                    { target: 300, duration: '5m' },
                    { target: 300, duration: '5m' },
                    { target: 0,   duration: '3m' },
                ],
                exec: 'searchOnly',
            },
        },
        thresholds: {
            'http_req_duration{type:read}': ['p(95)<1000'],
            'http_req_failed':              ['rate<0.01'],
        },
    },
};

export const options = withQuickRunOptions(
    SCENARIO_OPTIONS[TEST_TYPE] || SCENARIO_OPTIONS['read-heavy']
);

// ============ Setup ============
export function setup() {
    logTestStart(`Stress Test [${TEST_TYPE}]`, BASE_URL);
    console.log(`   부하 패턴: 램프업 → 지속 고부하 → 램프다운`);
    console.log(`   사용자 풀: ${USER_POOL}`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);

    const groupContext = resolveGroupContext(baseToken);
    const subgroupContext = resolveSubgroupChatContext(baseToken, groupContext.groupId);

    if (TEST_TYPE === 'write-heavy' && !groupContext.groupId) {
        throw new Error('write-heavy 스트레스 테스트에 필요한 그룹 컨텍스트를 확보하지 못했습니다. 내 그룹 또는 GROUP_SEARCH_KEYWORDS/TEST_GROUP_CODE 설정을 확인하세요.');
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

// ============ 시나리오 함수들 ============

export function readHeavy(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.groupId = data.groupId;
    state.subgroupId = data.subgroupId;
    state.chatRoomId = data.chatRoomId;
    state.keywordIds = data.keywordIds;

    // browsing(60%) / searching(40%) 혼합
    if (Math.random() < 0.6) {
        const count = executeBrowsingJourney(state);
        metrics.add(count, 'stress_success_count');
    } else {
        const count = executeSearchingJourney(state);
        metrics.add(count, 'stress_success_count');
    }

    sleep(0.5 + Math.random() * 1.5);
}

export function writeHeavy(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const rand = Math.random();

    if (rand < 0.4) {
        // 리뷰 작성 (40%)
        createReview(token, data.groupId, data.keywordIds);
    } else if (rand < 0.8 && data.chatRoomId) {
        // 채팅 전송 (40%)
        sendChatMessage(token, data.chatRoomId, randomChatMessage());
    } else {
        // 검색으로 대체 (20% - 즐겨찾기 API 없을 경우)
        search(token, randomKeyword(), randomLocation());
    }

    sleep(1 + Math.random() * 2);
}

export function searchOnly(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const loc = Math.random() < 0.7 ? randomLocation() : null;
    search(token, randomKeyword(), loc);
    sleep(0.1);
}

// ============ Teardown ============
export function teardown(data) {
    console.log(`🏁 Stress Test [${TEST_TYPE}] 완료`);
    console.log('   결과는 k6 summary 및 Grafana 대시보드에서 확인하세요.');
}
