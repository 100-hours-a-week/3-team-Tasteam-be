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

const TEST_TYPE = __ENV.TEST_TYPE || 'mixed-identity-spike';
const USER_POOL = Number(__ENV.USER_POOL || '160');
const ANONYMOUS_POOL = Number(__ENV.ANONYMOUS_POOL || '2400');
const MEMBER_SHARE = Number(__ENV.MEMBER_SHARE || '0.5');

const successfulRequests = new Counter('client_activity_kafka_request_success_count');
const acceptedEvents = new Counter('client_activity_kafka_event_success_count');

const SCENARIO_OPTIONS = {
    'single-burst-small-batch': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityKafka: {
                executor: 'ramping-arrival-rate',
                timeUnit: '1s',
                startRate: 10,
                preAllocatedVUs: 80,
                maxVUs: 240,
                stages: [
                    { target: 20, duration: '1m' },
                    { target: 90, duration: '90s' },
                    { target: 20, duration: '2m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'singleBurstSmallBatch',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<1800'],
            'http_req_failed': ['rate<0.02'],
        },
    },
    'single-burst-large-batch': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityKafka: {
                executor: 'ramping-arrival-rate',
                timeUnit: '1s',
                startRate: 10,
                preAllocatedVUs: 120,
                maxVUs: 320,
                stages: [
                    { target: 20, duration: '1m' },
                    { target: 70, duration: '90s' },
                    { target: 20, duration: '2m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'singleBurstLargeBatch',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<2200'],
            'http_req_failed': ['rate<0.02'],
        },
    },
    'double-spike-recovery': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityKafka: {
                executor: 'ramping-arrival-rate',
                timeUnit: '1s',
                startRate: 10,
                preAllocatedVUs: 120,
                maxVUs: 360,
                stages: [
                    { target: 20, duration: '1m' },
                    { target: 90, duration: '1m' },
                    { target: 20, duration: '90s' },
                    { target: 120, duration: '1m' },
                    { target: 20, duration: '2m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'doubleSpikeRecovery',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<2200'],
            'http_req_failed': ['rate<0.02'],
        },
    },
    'mixed-identity-spike': {
        setupTimeout: '5m',
        scenarios: {
            clientActivityKafka: {
                executor: 'ramping-arrival-rate',
                timeUnit: '1s',
                startRate: 10,
                preAllocatedVUs: 120,
                maxVUs: 320,
                stages: [
                    { target: 20, duration: '1m' },
                    { target: 60, duration: '1m' },
                    { target: 110, duration: '90s' },
                    { target: 20, duration: '2m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'mixedIdentitySpike',
            },
        },
        thresholds: {
            'http_req_duration{name:client_activity_ingest}': ['p(95)<2000'],
            'http_req_failed': ['rate<0.02'],
        },
    },
};

export const options = withQuickRunOptions(
    SCENARIO_OPTIONS[TEST_TYPE] || SCENARIO_OPTIONS['mixed-identity-spike']
);

function collectRestaurantPools(hotspot) {
    return {
        hotRestaurants: hotspot && hotspot.restaurants ? hotspot.restaurants.hot || [] : [],
        coldRestaurants: hotspot && hotspot.restaurants ? hotspot.restaurants.cold || [] : [],
    };
}

export function setup() {
    logTestStart(`Client Activity Kafka Publish Spike [${TEST_TYPE}]`, BASE_URL);
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

function runKafkaSpikeIteration(data, config) {
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
    const events = buildClientActivityBatch({
        batchSize: config.batchSize,
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
            batch_profile: config.batchProfile,
        },
    });

    if (res && res.status === 200) {
        successfulRequests.add(1);
        acceptedEvents.add(events.length);
    }

    sleep(config.sleepSeconds || 0);
}

export function singleBurstSmallBatch(data) {
    runKafkaSpikeIteration(data, {
        scenario: 'single_burst_small_batch',
        identityMode: 'anonymous',
        batchSize: Number(__ENV.BATCH_SIZE || '2'),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.2'),
        batchProfile: 'small',
    });
}

export function singleBurstLargeBatch(data) {
    runKafkaSpikeIteration(data, {
        scenario: 'single_burst_large_batch',
        identityMode: 'anonymous',
        batchSize: Number(__ENV.BATCH_SIZE || '12'),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.2'),
        batchProfile: 'large',
    });
}

export function doubleSpikeRecovery(data) {
    runKafkaSpikeIteration(data, {
        scenario: 'double_spike_recovery',
        identityMode: 'mixed',
        memberShare: Number(__ENV.MEMBER_SHARE || '0.4'),
        batchSize: Number(__ENV.BATCH_SIZE || '8'),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.4'),
        batchProfile: 'double-spike',
    });
}

export function mixedIdentitySpike(data) {
    runKafkaSpikeIteration(data, {
        scenario: 'mixed_identity_spike',
        identityMode: 'mixed',
        memberShare: Number(__ENV.MEMBER_SHARE || '0.5'),
        batchSize: Number(__ENV.BATCH_SIZE || '6'),
        restaurantHotShare: Number(__ENV.RESTAURANT_HOT_SHARE || '0.4'),
        batchProfile: 'mixed',
    });
}

export function teardown() {
    console.log(`🏁 Client Activity Kafka Publish Spike [${TEST_TYPE}] 완료`);
}
