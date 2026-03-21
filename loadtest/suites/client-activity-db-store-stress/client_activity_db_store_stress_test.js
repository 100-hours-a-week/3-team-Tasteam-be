import { Counter } from 'k6/metrics';
import { sleep } from 'k6';
import {
    BASE_URL,
    batchLogin,
    buildClientActivityBatch,
    createClientActivityAnonymousPool,
    getClientActivityEventNames,
    ingestClientActivityEvents,
    pickClientActivityIdentity,
    prepareHotspotPools,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart } from '../../shared/test-utils.js';

const TEST_TYPE = __ENV.TEST_TYPE || 'store-mixed-realistic';
const USER_POOL = Number(__ENV.USER_POOL || '120');
const ANONYMOUS_POOL = Number(__ENV.ANONYMOUS_POOL || '600');
const MEMBER_SHARE = Number(__ENV.MEMBER_SHARE || '0.5');

const successfulRequests = new Counter('client_activity_store_request_success_count');
const acceptedEvents = new Counter('client_activity_store_event_success_count');

function parseBatchSizeList(raw, fallback) {
    if (!raw) {
        return fallback;
    }

    const parsed = raw
        .split(',')
        .map((value) => Number(value.trim()))
        .filter((value) => Number.isInteger(value) && value > 0);

    return parsed.length > 0 ? parsed : fallback;
}

function pickBatchSize(config) {
    if (Array.isArray(config.batchSizes) && config.batchSizes.length > 0) {
        return config.batchSizes[Math.floor(Math.random() * config.batchSizes.length)];
    }
    return config.batchSize || 1;
}

const DEFAULT_SWEEP_BATCHES = parseBatchSizeList(__ENV.BATCH_SWEEP_SIZES, [1, 5, 10, 20]);

const SCENARIO_OPTIONS = {
    'store-only-cold': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityStore: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 20, duration: '2m' },
                    { target: 60, duration: '3m' },
                    { target: 120, duration: '5m' },
                    { target: 120, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'storeOnlyCold',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<1500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'store-hot-identity': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityStore: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 20, duration: '2m' },
                    { target: 50, duration: '3m' },
                    { target: 90, duration: '5m' },
                    { target: 90, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'storeHotIdentity',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<1800'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'store-mixed-realistic': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityStore: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 20, duration: '2m' },
                    { target: 80, duration: '4m' },
                    { target: 140, duration: '5m' },
                    { target: 140, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'storeMixedRealistic',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<1800'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'batch-size-sweep': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityStore: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 20, duration: '2m' },
                    { target: 60, duration: '3m' },
                    { target: 100, duration: '5m' },
                    { target: 100, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'batchSizeSweep',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<2200'],
            'http_req_failed': ['rate<0.02'],
        },
    },
};

export const options = withQuickRunOptions(
    SCENARIO_OPTIONS[TEST_TYPE] || SCENARIO_OPTIONS['store-mixed-realistic']
);

function collectRestaurantPools(hotspot) {
    return {
        hotRestaurants: hotspot && hotspot.restaurants ? hotspot.restaurants.hot || [] : [],
        coldRestaurants: hotspot && hotspot.restaurants ? hotspot.restaurants.cold || [] : [],
    };
}

export function setup() {
    logTestStart(`Client Activity DB Store Stress [${TEST_TYPE}]`, BASE_URL);
    console.log(`   사용자 풀: ${USER_POOL}`);
    console.log(`   anonymous 풀: ${ANONYMOUS_POOL}`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const anonymousIds = createClientActivityAnonymousPool(ANONYMOUS_POOL);
    const hotspot = prepareHotspotPools(tokens[0], []);
    const eventNames = getClientActivityEventNames();

    console.log(`✅ Setup 완료: tokens=${tokens.length}, anonymousIds=${anonymousIds.length}, events=${eventNames.length}`);
    return {
        tokens,
        anonymousIds,
        hotspot,
        eventNames,
    };
}

function runStoreIteration(data, config) {
    if (!data) {
        return;
    }

    const identity = pickClientActivityIdentity(
        data.tokens,
        data.anonymousIds,
        config.identityMode,
        config.memberShare ?? MEMBER_SHARE
    );
    const restaurantPools = collectRestaurantPools(data.hotspot);
    const batchSize = pickBatchSize(config);
    const events = buildClientActivityBatch({
        batchSize,
        eventNames: data.eventNames,
        hotRestaurants: restaurantPools.hotRestaurants,
        coldRestaurants: restaurantPools.coldRestaurants,
        restaurantHotShare: config.restaurantHotShare,
    });

    const res = ingestClientActivityEvents({
        token: identity.token,
        anonymousId: identity.anonymousId,
        events,
        tags: {
            scenario: config.scenario,
            identity_mode: identity.identityMode,
            batch_profile: config.batchProfile || 'fixed',
        },
    });

    if (res && res.status === 200) {
        successfulRequests.add(1);
        acceptedEvents.add(events.length);
    }

    sleep(config.sleepBase + Math.random() * config.sleepJitter);
}

export function storeOnlyCold(data) {
    runStoreIteration(data, {
        scenario: 'store_only_cold',
        identityMode: 'anonymous',
        batchSize: Number(__ENV.BATCH_SIZE || '2'),
        restaurantHotShare: 0,
        batchProfile: 'cold',
        sleepBase: 0.2,
        sleepJitter: 0.5,
    });
}

export function storeHotIdentity(data) {
    runStoreIteration(data, {
        scenario: 'store_hot_identity',
        identityMode: 'mixed',
        memberShare: Number(__ENV.MEMBER_SHARE || '0.7'),
        batchSize: Number(__ENV.BATCH_SIZE || '2'),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.85'),
        batchProfile: 'hot-identity',
        sleepBase: 0.2,
        sleepJitter: 0.5,
    });
}

export function storeMixedRealistic(data) {
    runStoreIteration(data, {
        scenario: 'store_mixed_realistic',
        identityMode: 'mixed',
        memberShare: Number(__ENV.MEMBER_SHARE || '0.5'),
        batchSizes: parseBatchSizeList(__ENV.BATCH_SIZES, [2, 3, 5]),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.3'),
        batchProfile: 'mixed',
        sleepBase: 0.15,
        sleepJitter: 0.35,
    });
}

export function batchSizeSweep(data) {
    runStoreIteration(data, {
        scenario: 'batch_size_sweep',
        identityMode: 'mixed',
        memberShare: Number(__ENV.MEMBER_SHARE || '0.5'),
        batchSizes: DEFAULT_SWEEP_BATCHES,
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.3'),
        batchProfile: 'sweep',
        sleepBase: 0.15,
        sleepJitter: 0.25,
    });
}

export function teardown() {
    console.log(`🏁 Client Activity DB Store Stress [${TEST_TYPE}] 완료`);
}
