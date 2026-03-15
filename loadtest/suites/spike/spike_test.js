/**
 * Spike Test - 순간 급증 부하 테스트
 *
 * 목적: 갑작스러운 트래픽 급증 시 시스템 응답 검증
 *
 * SPIKE_TARGET 환경변수로 시나리오 선택:
 *   search  (기본값) - POST /search, ramping-arrival-rate 0→300 RPS, 3분
 *   main             - GET /main/home + main 노출 음식점 상세, 2→500 VU, 3분
 *   group            - 그룹/서브그룹 조회, 2→300 VU, 3분
 *   chat             - 채팅 조회+전송, 2→200 VU, 3분
 *   write            - 리뷰+채팅+즐겨찾기, 2→150 VU, 3분
 *
 * SLO:
 *   - 읽기 p95 < 1.2s
 *   - 쓰기 p95 < 2.5s
 *   - 5xx 에러율 < 0.5%
 *
 * 실행 방법:
 *   SPIKE_TARGET=search ./run-spike.sh
 *   SPIKE_TARGET=main BASE_URL=https://stg.tasteam.kr k6 run spike_test.js
 */

import { sleep } from 'k6';
import {
    BASE_URL,
    createState,
    batchLogin,
    getReviewKeywords,
    getHomePage,
    getFoodCategories,
    getRestaurantDetail,
    extractRestaurantIdsFromSectionsResponse,
    pickRandomRestaurantId,
    getGroupDetail,
    getGroupReviews,
    getGroupMembers,
    search,
    randomLocation,
    randomKeyword,
    randomChatMessage,
    getChatMessages,
    sendChatMessage,
    updateChatReadCursor,
    createReview,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

const SPIKE_TARGET = __ENV.SPIKE_TARGET || 'search';

const metrics = new SuccessMetrics(['spike_success_count']);

// ============ 시나리오별 options 정의 ============

const SCENARIO_OPTIONS = {
    search: {
        scenarios: {
            spike: {
                executor: 'ramping-arrival-rate',
                startRate: 10,
                timeUnit: '1s',
                preAllocatedVUs: 500,
                maxVUs: 5000,
                stages: [
                    { target: 10,  duration: '30s' },
                    { target: 300, duration: '1m' },
                    { target: 300, duration: '1m' },
                    { target: 0,   duration: '30s' },
                ],
                exec: 'spikeSearch',
            },
        },
        thresholds: {
            'http_req_duration{type:read}': ['p(95)<1200'],
            'http_req_failed':              ['rate<0.005'],
        },
    },
    main: {
        scenarios: {
            spike: {
                executor: 'ramping-vus',
                startVUs: 2,
                stages: [
                    { target: 2,   duration: '30s' },
                    { target: 500, duration: '1m' },
                    { target: 500, duration: '1m' },
                    { target: 2,   duration: '30s' },
                ],
                exec: 'spikeMain',
            },
        },
        thresholds: {
            'http_req_duration{type:read}': ['p(95)<1200'],
            'http_req_failed':              ['rate<0.005'],
        },
    },
    group: {
        scenarios: {
            spike: {
                executor: 'ramping-vus',
                startVUs: 2,
                stages: [
                    { target: 2,   duration: '30s' },
                    { target: 300, duration: '1m' },
                    { target: 300, duration: '1m' },
                    { target: 2,   duration: '30s' },
                ],
                exec: 'spikeGroup',
            },
        },
        thresholds: {
            'http_req_duration{type:read}': ['p(95)<1200'],
            'http_req_failed':              ['rate<0.005'],
        },
    },
    chat: {
        scenarios: {
            spike: {
                executor: 'ramping-vus',
                startVUs: 2,
                stages: [
                    { target: 2,   duration: '30s' },
                    { target: 200, duration: '1m' },
                    { target: 200, duration: '1m' },
                    { target: 2,   duration: '30s' },
                ],
                exec: 'spikeChat',
            },
        },
        thresholds: {
            'http_req_duration{type:read}':  ['p(95)<1200'],
            'http_req_duration{type:write}': ['p(95)<2500'],
            'http_req_failed':               ['rate<0.005'],
        },
    },
    write: {
        scenarios: {
            spike: {
                executor: 'ramping-vus',
                startVUs: 2,
                stages: [
                    { target: 2,   duration: '30s' },
                    { target: 150, duration: '1m' },
                    { target: 150, duration: '1m' },
                    { target: 2,   duration: '30s' },
                ],
                exec: 'spikeWrite',
            },
        },
        thresholds: {
            'http_req_duration{type:write}': ['p(95)<2500'],
            'http_req_failed':               ['rate<0.005'],
        },
    },
};

export const options = withQuickRunOptions({
    setupTimeout: '5m',
    ...(SCENARIO_OPTIONS[SPIKE_TARGET] || SCENARIO_OPTIONS['search']),
});

// ============ Setup ============
export function setup() {
    logTestStart(`Spike Test [${SPIKE_TARGET}]`, BASE_URL);
    console.log(`   스파이크 패턴: 급격한 부하 상승 후 유지 후 하강`);

    const tokens = batchLogin(50);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);
    const groupContext = resolveGroupContext(baseToken);
    const subgroupContext = resolveSubgroupChatContext(baseToken, groupContext.groupId);

    if (['group', 'chat', 'write'].includes(SPIKE_TARGET) && !groupContext.groupId) {
        throw new Error(`${SPIKE_TARGET} 스파이크 테스트에 필요한 그룹 컨텍스트를 확보하지 못했습니다. 내 그룹 또는 GROUP_SEARCH_KEYWORDS/TEST_GROUP_CODE 설정을 확인하세요.`);
    }

    if (SPIKE_TARGET === 'chat' && !subgroupContext.chatRoomId) {
        throw new Error('chat 스파이크 테스트에 필요한 채팅방 컨텍스트를 확보하지 못했습니다.');
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

export function spikeSearch(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const loc = randomLocation();
    const res = search(token, randomKeyword(), loc);
    metrics.add(res && res.status === 200 ? 1 : 0, 'spike_success_count');
    sleep(0.1);
}

export function spikeMain(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const loc = randomLocation();

    const homeRes = getHomePage(token, loc.lat, loc.lon);
    getFoodCategories();
    const restaurantId = pickRandomRestaurantId(extractRestaurantIdsFromSectionsResponse(homeRes));
    if (restaurantId) {
        getRestaurantDetail(token, restaurantId);
    }
    sleep(0.5);
}

export function spikeGroup(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.groupId = data.groupId;

    if (state.groupId) {
        getGroupDetail(token, state.groupId);
        getGroupReviews(token, state.groupId);
        getGroupMembers(token, state.groupId);
    }
    sleep(0.5);
}

export function spikeChat(data) {
    if (!data || !data.chatRoomId) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];

    const msgRes = getChatMessages(token, data.chatRoomId);
    if (Math.random() < 0.3) {
        sendChatMessage(token, data.chatRoomId, randomChatMessage());
    }
    if (msgRes.nextCursor && typeof updateChatReadCursor === 'function') {
        updateChatReadCursor(token, data.chatRoomId, msgRes.nextCursor);
    }
    sleep(0.3);
}

export function spikeWrite(data) {
    if (!data) return;
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const rand = Math.random();

    if (rand < 0.4) {
        createReview(token, data.groupId, data.keywordIds);
    } else if (rand < 0.8 && data.chatRoomId) {
        sendChatMessage(token, data.chatRoomId, randomChatMessage());
    } else {
        // 검색으로 대체
        search(token, randomKeyword(), randomLocation());
    }
    sleep(0.2);
}

// ============ Teardown ============
export function teardown(data) {
    console.log(`🏁 Spike Test [${SPIKE_TARGET}] 완료`);
    console.log('   결과는 k6 summary 및 Grafana 대시보드에서 확인하세요.');
}
