import { sleep } from 'k6';
import { Counter } from 'k6/metrics';
import {
    BASE_URL,
    batchLogin,
    createState,
    getReviewKeywords,
    executeBrowsingJourney,
    executeSearchingJourney,
    executeGroupJourney,
    executeSubgroupJourney,
    executePersonalJourney,
    executeChatJourney,
    executeWritingJourney,
    prepareHotspotPools,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';

const SOAK_MODE = __ENV.SOAK_MODE || '24h';
const CACHE_MODE = __ENV.CACHE_MODE || 'off';
const USER_POOL = Number(__ENV.USER_POOL || '100');
const BASE_VUS = Number(__ENV.BASE_VUS || '120');
const PEAK_VUS = Math.floor(BASE_VUS * 1.2);
const WARMUP_VUS = Math.max(10, Math.floor(BASE_VUS * 0.4));

const journeySuccess = new Counter('journey_success_count');

const JOURNEYS = [
    { name: 'browsing', weight: 28, fn: executeBrowsingJourney },
    { name: 'searching', weight: 18, fn: executeSearchingJourney },
    { name: 'group', weight: 12, fn: executeGroupJourney },
    { name: 'subgroup', weight: 12, fn: executeSubgroupJourney },
    { name: 'personal', weight: 12, fn: executePersonalJourney },
    { name: 'chat', weight: 10, fn: executeChatJourney },
    { name: 'writing', weight: 8, fn: executeWritingJourney },
];

function pickJourney() {
    const total = JOURNEYS.reduce((sum, x) => sum + x.weight, 0);
    let pivot = Math.random() * total;
    for (const j of JOURNEYS) {
        pivot -= j.weight;
        if (pivot <= 0) return j;
    }
    return JOURNEYS[0];
}

function buildStages() {
    if (SOAK_MODE === '48h') {
        return [
            { target: WARMUP_VUS, duration: '1h' },
            { target: BASE_VUS, duration: '11h' },
            { target: PEAK_VUS, duration: '24h' },
            { target: BASE_VUS, duration: '11h' },
            { target: 0, duration: '1h' },
        ];
    }

    return [
        { target: WARMUP_VUS, duration: '1h' },
        { target: BASE_VUS, duration: '22h' },
        { target: 0, duration: '1h' },
    ];
}

export const options = withQuickRunOptions({
    setupTimeout: '5m',
    scenarios: {
        soak: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: buildStages(),
            exec: 'soakScenario',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.003'],
        'http_req_failed{status:0}': ['rate<0.001'],
        'http_req_duration{type:read}': ['p(95)<1000', 'p(99)<5000'],
        'http_req_duration{type:write}': ['p(95)<3000'],
        http_req_duration: ['p(99)<5000'],
    },
});

function randomToken(tokens) {
    return tokens[Math.floor(Math.random() * tokens.length)];
}

function buildState(data) {
    const state = createState();
    state.token = randomToken(data.tokens);
    state.groupId = data.groupId;
    state.subgroupId = data.subgroupId;
    state.chatRoomId = data.chatRoomId;
    state.keywordIds = data.keywordIds;
    state.hotspot = data.hotspot || null;
    return state;
}

export function setup() {
    console.log(`🔥 Soak Test 시작 | mode=${SOAK_MODE} | cache_mode=${CACHE_MODE}`);
    console.log(`   target=${BASE_URL} | user_pool=${USER_POOL} | base_vus=${BASE_VUS}`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        throw new Error('토큰 발급 실패: 테스트 토큰 API 확인 필요');
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);

    const groupContext = resolveGroupContext(baseToken);
    const groupId = groupContext.groupId;
    if (!groupId) throw new Error('필수 그룹 ID 확보 실패: 내 그룹이 비어 있고 GROUP_SEARCH_KEYWORDS 검색 결과 가입도 실패했습니다.');

    const subgroupContext = resolveSubgroupChatContext(baseToken, groupId);
    const subgroupId = subgroupContext.subgroupId;
    if (!subgroupId) throw new Error('필수 서브그룹 ID 확보 실패: 테스트 중단');

    const chatRoomId = subgroupContext.chatRoomId;
    if (!chatRoomId) throw new Error('필수 채팅방 ID 확보 실패: 테스트 중단');

    const hotspot = prepareHotspotPools(baseToken, groupContext.groupIds);

    console.log(`✅ setup 완료 tokens=${tokens.length}, groupId=${groupId}, subgroupId=${subgroupId}, chatRoomId=${chatRoomId}`);
    return { tokens, groupId, subgroupId, chatRoomId, keywordIds, hotspot };
}

export function soakScenario(data) {
    const state = buildState(data);
    const selected = pickJourney();
    const success = selected.fn(state);
    journeySuccess.add(success);

    sleep(1 + Math.random() * 4);
}

export function teardown() {
    console.log(`🏁 Soak Test 종료 | mode=${SOAK_MODE} | cache_mode=${CACHE_MODE}`);
    console.log('📌 30분 롤링 실패율(0.3%) 판단은 Grafana에서 testid 기준으로 확인하세요.');
}
