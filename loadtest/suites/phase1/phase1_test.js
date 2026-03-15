import { sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    BASE_URL,
    batchLogin,
    createState,
    getReviewKeywords,
    getHomePage,
    getRestaurantDetail,
    getGroupDetail,
    getSubgroupDetail,
    getSubgroupMembers,
    getSubgroupReviews,
    getChatMessages,
    sendChatMessage,
    updateChatReadCursor,
    search,
    createReview,
    addFavoriteRestaurant,
    removeFavoriteRestaurant,
    executeBrowsingJourney,
    executeSearchingJourney,
    executeGroupJourney,
    executeSubgroupJourney,
    executePersonalJourney,
    executeChatJourney,
    executeWritingJourney,
    prepareHotspotPools,
    pickKeyword,
    pickGroupId,
    pickSubgroupId,
    pickChatRoomId,
    pickRestaurantId,
    extractRestaurantIdsFromSectionsResponse,
    pickRandomRestaurantId,
    resolveGroupContext,
    resolveSubgroupChatContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';

const SUITE = __ENV.TEST_SUITE || 'full';
const CACHE_MODE = __ENV.CACHE_MODE || 'off';
const USER_POOL = Number(__ENV.USER_POOL || '100');

const readSuccess = new Counter('read_success_count');
const writeSuccess = new Counter('write_success_count');
const searchSuccess = new Counter('search_success_count');
const journeySuccess = new Counter('journey_success_count');
const authFailureRate = new Rate('auth_failure_rate');
const connectionFailureRate = new Rate('connection_failure_rate');

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

function randomToken(tokens) {
    return tokens[Math.floor(Math.random() * tokens.length)];
}

function randomLocation() {
    const points = [
        { lat: 37.5665, lon: 126.9780 },
        { lat: 37.4979, lon: 127.0276 },
        { lat: 37.5443, lon: 127.0557 },
        { lat: 37.5600, lon: 127.0369 },
        { lat: 37.5506, lon: 126.9217 },
    ];
    return points[Math.floor(Math.random() * points.length)];
}

function randomKeyword() {
    const keywords = ['파스타', '치킨', '피자', '한식', '강남', '성수', '점심', '회식', '가성비', '카페'];
    return keywords[Math.floor(Math.random() * keywords.length)];
}

function buildOptionsBySuite() {
    const smoke = {
        smoke: {
            executor: 'constant-vus',
            vus: 2,
            duration: '3m',
            exec: 'smokeScenario',
            tags: { phase: 'S1' },
        },
    };

    const spike = {
        spike_search: {
            executor: 'ramping-arrival-rate',
            startRate: 20,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 2000,
            stages: [
                { target: 80, duration: '1m' },
                { target: 300, duration: '2m' },
                { target: 40, duration: '1m' },
            ],
            exec: 'spikeSearch',
            tags: { phase: 'S2', feature: 'search' },
        },
        spike_main: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { target: 300, duration: '2m' },
                { target: 500, duration: '2m' },
                { target: 100, duration: '1m' },
            ],
            exec: 'spikeMain',
            tags: { phase: 'S2', feature: 'main' },
        },
        spike_group: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { target: 240, duration: '2m' },
                { target: 300, duration: '2m' },
                { target: 80, duration: '1m' },
            ],
            exec: 'spikeGroup',
            tags: { phase: 'S2', feature: 'group' },
        },
        spike_chat: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { target: 100, duration: '1m' },
                { target: 180, duration: '2m' },
                { target: 60, duration: '1m' },
            ],
            exec: 'spikeChat',
            tags: { phase: 'S2', feature: 'chat' },
        },
        spike_writing: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { target: 80, duration: '1m' },
                { target: 160, duration: '2m' },
                { target: 40, duration: '1m' },
            ],
            exec: 'spikeWriting',
            tags: { phase: 'S2', feature: 'writing' },
        },
    };

    const separated = {
        search_only: {
            executor: 'ramping-arrival-rate',
            startRate: 20,
            timeUnit: '1s',
            preAllocatedVUs: 300,
            maxVUs: 5000,
            stages: [
                { target: 80, duration: '2m' },
                { target: 180, duration: '3m' },
                { target: 300, duration: '3m' },
                { target: 80, duration: '2m' },
            ],
            exec: 'spikeSearch',
            tags: { phase: 'S3', feature: 'search_only' },
        },
        read_heavy: {
            executor: 'ramping-vus',
            startVUs: 50,
            stages: [
                { target: 300, duration: '3m' },
                { target: 700, duration: '3m' },
                { target: 1000, duration: '4m' },
                { target: 100, duration: '2m' },
            ],
            exec: 'readHeavyScenario',
            tags: { phase: 'S3', feature: 'read_heavy' },
        },
        write_heavy: {
            executor: 'ramping-vus',
            startVUs: 50,
            stages: [
                { target: 120, duration: '3m' },
                { target: 200, duration: '20m' },
                { target: 50, duration: '2m' },
            ],
            exec: 'writeHeavyScenario',
            tags: { phase: 'S3', feature: 'write_heavy' },
        },
    };

    const mixed = {
        mixed_load: {
            executor: 'ramping-vus',
            startVUs: 100,
            stages: [
                { target: 400, duration: '10m' },
                { target: 700, duration: '15m' },
                { target: 1000, duration: '15m' },
                { target: 100, duration: '5m' },
            ],
            exec: 'mixedScenario',
            tags: { phase: 'S4' },
        },
    };

    const breakpoint = {
        breakpoint_read: {
            executor: 'ramping-arrival-rate',
            startRate: 40,
            timeUnit: '1s',
            preAllocatedVUs: 2000,
            maxVUs: 30000,
            stages: [
                { target: 500, duration: '1m' },
                { target: 1500, duration: '1m' },
                { target: 3000, duration: '1m' },
                { target: 4500, duration: '1m' },
                { target: 6000, duration: '1m' },
            ],
            exec: 'breakpointRead',
            tags: { phase: 'S5', rw: 'read' },
        },
        breakpoint_write: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 500,
            maxVUs: 10000,
            stages: [
                { target: 120, duration: '1m' },
                { target: 300, duration: '1m' },
                { target: 600, duration: '1m' },
                { target: 900, duration: '1m' },
                { target: 1200, duration: '1m' },
            ],
            exec: 'breakpointWrite',
            tags: { phase: 'S5', rw: 'write' },
        },
    };

    const recovery = {
        recovery: {
            executor: 'constant-vus',
            vus: 50,
            duration: '10m',
            exec: 'mixedScenario',
            tags: { phase: 'S6' },
        },
    };

    if (SUITE === 'smoke') return smoke;
    if (SUITE === 'spike') return spike;
    if (SUITE === 'separated') return separated;
    if (SUITE === 'mixed') return mixed;
    if (SUITE === 'breakpoint') return breakpoint;

    return {
        ...smoke,
        ...spike,
        ...separated,
        ...mixed,
        ...breakpoint,
        ...recovery,
    };
}

export const options = withQuickRunOptions({
    setupTimeout: '5m',
    scenarios: buildOptionsBySuite(),
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<2200', 'p(99)<5000'],
        'http_req_duration{type:read}': ['p(95)<1200', 'p(99)<3000'],
        'http_req_duration{type:write}': ['p(95)<3000', 'p(99)<5000'],
        auth_failure_rate: ['rate<0.2'],
        connection_failure_rate: ['rate<0.001'],

        // Smoke
        'http_req_failed{scenario:smoke}': ['rate<0.005'],
        'http_req_duration{scenario:smoke}': ['p(95)<1500'],

        // Spike (완화 기준)
        'http_req_duration{scenario:spike_search}': ['p(95)<1200'],
        'http_req_duration{scenario:spike_main}': ['p(95)<1200'],
        'http_req_duration{scenario:spike_writing}': ['p(95)<3000'],

        // Mixed / separated
        'http_req_duration{scenario:search_only}': ['p(95)<2500'],
        'http_req_duration{scenario:read_heavy}': ['p(95)<1200'],
        'http_req_duration{scenario:write_heavy}': ['p(95)<3000'],
        'http_req_duration{scenario:mixed_load}': ['p(95)<2200'],

        // Breakpoint immediate guard rails
        'http_req_failed{scenario:breakpoint_read}': [
            { threshold: 'rate<0.30', abortOnFail: true, delayAbortEval: '30s' },
        ],
        'http_req_failed{scenario:breakpoint_write}': [
            { threshold: 'rate<0.30', abortOnFail: true, delayAbortEval: '30s' },
        ],
        'http_req_duration{scenario:breakpoint_read}': [
            { threshold: 'p(99)<15000', abortOnFail: true, delayAbortEval: '30s' },
        ],
        'http_req_duration{scenario:breakpoint_write}': [
            { threshold: 'p(99)<15000', abortOnFail: true, delayAbortEval: '30s' },
        ],
    },
});

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
    console.log(`🔥 Phase1 Load Test 시작 | suite=${SUITE} | cache_mode=${CACHE_MODE}`);
    console.log(`   target=${BASE_URL} | user_pool=${USER_POOL}`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        throw new Error('토큰 발급 실패: 테스트 토큰 API 확인 필요');
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);

    const groupContext = resolveGroupContext(baseToken);
    const groupId = groupContext.groupId;

    if (!groupId) {
        throw new Error('필수 그룹 ID 확보 실패: 내 그룹이 비어 있고 GROUP_SEARCH_KEYWORDS 검색 결과 가입도 실패했습니다.');
    }

    const subgroupContext = resolveSubgroupChatContext(baseToken, groupId);
    const subgroupId = subgroupContext.subgroupId;

    if (!subgroupId) {
        throw new Error('필수 서브그룹 ID 확보 실패: 테스트 중단');
    }

    const chatRoomId = subgroupContext.chatRoomId;
    if (!chatRoomId) {
        throw new Error('필수 채팅방 ID 확보 실패: 테스트 중단');
    }

    const hotspot = prepareHotspotPools(baseToken, groupContext.groupIds);

    console.log(`✅ setup 완료 tokens=${tokens.length}, groupId=${groupId}, subgroupId=${subgroupId}, chatRoomId=${chatRoomId}`);
    return { tokens, groupId, subgroupId, chatRoomId, keywordIds, hotspot };
}

function postProcess(res) {
    if (!res) return;
    if (res.status === 401 || res.status === 403) authFailureRate.add(1);
    else authFailureRate.add(0);

    if (res.status === 0) connectionFailureRate.add(1);
    else connectionFailureRate.add(0);
}

export function smokeScenario(data) {
    const state = buildState(data);
    const j = executeBrowsingJourney(state) + executePersonalJourney(state);
    journeySuccess.add(j);
    sleep(1);
}

export function spikeSearch(data) {
    const state = buildState(data);
    const loc = randomLocation();
    const res = search(state.token, pickKeyword(state), loc);
    if (res && res.status === 200) searchSuccess.add(1);
    postProcess(res);
    sleep(0.2);
}

export function spikeMain(data) {
    const state = buildState(data);
    const loc = randomLocation();
    const h = getHomePage(state.token, loc.lat, loc.lon);
    const restaurantId = pickRandomRestaurantId(extractRestaurantIdsFromSectionsResponse(h));
    const detail = restaurantId ? getRestaurantDetail(state.token, restaurantId) : null;
    if (h && h.status === 200) readSuccess.add(1);
    if (detail && detail.status === 200) readSuccess.add(1);
    postProcess(h);
    postProcess(detail);
    sleep(0.3);
}

export function spikeGroup(data) {
    const state = buildState(data);
    const groupId = pickGroupId(state) || state.groupId;
    const subgroupId = pickSubgroupId(state) || state.subgroupId;
    const g = getGroupDetail(state.token, groupId);
    const s = getSubgroupDetail(state.token, subgroupId);
    const m = getSubgroupMembers(state.token, subgroupId);
    const r = getSubgroupReviews(state.token, subgroupId);
    [g, s, m, r].forEach((res) => {
        if (res && res.status === 200) readSuccess.add(1);
        postProcess(res);
    });
    sleep(0.3);
}

export function spikeChat(data) {
    const state = buildState(data);
    const chatRoomId = pickChatRoomId(state) || state.chatRoomId;
    const messages = getChatMessages(state.token, chatRoomId);
    if (messages && messages.response && messages.response.status === 200) readSuccess.add(1);
    const send = sendChatMessage(state.token, chatRoomId, `spike-${Date.now()}`);
    if (send && (send.status === 200 || send.status === 201)) writeSuccess.add(1);

    let lastMessageId = null;
    if (messages && messages.messages && messages.messages.length > 0) {
        lastMessageId = messages.messages[messages.messages.length - 1].id;
    }
    const cursor = updateChatReadCursor(state.token, chatRoomId, lastMessageId);
    if (cursor && (cursor.status === 200 || cursor.status === 204)) writeSuccess.add(1);

    postProcess(messages && messages.response);
    postProcess(send);
    postProcess(cursor);
    sleep(0.2);
}

export function spikeWriting(data) {
    const state = buildState(data);
    const restaurantId = pickRestaurantId(state);
    const review = createReview(state.token, state.groupId, state.keywordIds, restaurantId);
    if (review && (review.status === 200 || review.status === 201)) writeSuccess.add(1);

    const addFav = addFavoriteRestaurant(state.token, restaurantId);
    if (addFav && (addFav.status === 200 || addFav.status === 201 || addFav.status === 409)) writeSuccess.add(1);

    if (Math.random() < 0.5) {
        const delFav = removeFavoriteRestaurant(state.token, restaurantId);
        if (delFav && (delFav.status === 200 || delFav.status === 204)) writeSuccess.add(1);
        postProcess(delFav);
    }

    postProcess(review);
    postProcess(addFav);
    sleep(0.3);
}

export function readHeavyScenario(data) {
    const state = buildState(data);
    const count = executeBrowsingJourney(state) + executeSearchingJourney(state);
    readSuccess.add(count);
    sleep(0.5);
}

export function writeHeavyScenario(data) {
    const state = buildState(data);
    const pivot = Math.random();
    let res = null;
    if (pivot < 0.4) {
        const restaurantId = pickRestaurantId(state);
        res = createReview(state.token, state.groupId, state.keywordIds, restaurantId);
    } else if (pivot < 0.8) {
        const chatRoomId = pickChatRoomId(state) || state.chatRoomId;
        res = sendChatMessage(state.token, chatRoomId, `write-heavy-${Date.now()}`);
    } else {
        const restaurantId = pickRestaurantId(state);
        const add = addFavoriteRestaurant(state.token, restaurantId);
        res = Math.random() < 0.5 ? removeFavoriteRestaurant(state.token, restaurantId) : add;
    }

    if (res && [200, 201, 204, 409].includes(res.status)) writeSuccess.add(1);
    postProcess(res);
    sleep(0.5);
}

export function mixedScenario(data) {
    const state = buildState(data);
    const selected = pickJourney();
    const count = selected.fn(state);
    journeySuccess.add(count);
    sleep(1 + Math.random() * 3);
}

export function breakpointRead(data) {
    const state = buildState(data);
    const count = executeBrowsingJourney(state) + executeSearchingJourney(state) + executeGroupJourney(state);
    readSuccess.add(count);
}

export function breakpointWrite(data) {
    const state = buildState(data);
    const count = executeWritingJourney(state) + executeChatJourney(state);
    writeSuccess.add(count);
}

export function teardown() {
    console.log(`🏁 Phase1 Load Test 종료 | suite=${SUITE} | cache_mode=${CACHE_MODE}`);
    if (CACHE_MODE === 'on') {
        console.log('📌 Cache ON 목표: OFF 대비 p95 latency 30%+ 개선 여부를 비교 리포트로 확인하세요.');
    }
}
