import { sleep } from 'k6';
import {
    BASE_URL,
    batchLogin,
    createState,
    getFavoriteTargets,
    getMyFavoriteRestaurants,
    getRestaurantFavoriteTargets,
    getSubgroupFavoriteRestaurants,
    pickRestaurantId,
    pickSubgroupId,
    prepareHotspotPools,
    randomLocation,
    resolveGroupContext,
    toggleFavoriteRestaurant,
    getHomePage,
    extractRestaurantIdsFromSectionsResponse,
    pickRandomRestaurantId,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

const TEST_TYPE = __ENV.TEST_TYPE || 'mixed';
const USER_POOL = Number(__ENV.USER_POOL || '100');
const metrics = new SuccessMetrics(['favorite_success_count']);

const SCENARIO_OPTIONS = {
    'toggle-only': {
        setupTimeout: '5m',
        scenarios: {
            favorite: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 30, duration: '2m' },
                    { target: 80, duration: '4m' },
                    { target: 120, duration: '4m' },
                    { target: 120, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'toggleOnly',
            },
        },
        thresholds: {
            'http_req_duration{type:write}': ['p(95)<2500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'targets-read': {
        setupTimeout: '5m',
        scenarios: {
            favorite: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 30, duration: '2m' },
                    { target: 120, duration: '4m' },
                    { target: 250, duration: '4m' },
                    { target: 250, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'targetsRead',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:favorite-targets}': ['p(95)<1500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'list-read': {
        setupTimeout: '5m',
        scenarios: {
            favorite: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 30, duration: '2m' },
                    { target: 120, duration: '4m' },
                    { target: 220, duration: '4m' },
                    { target: 220, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'listRead',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:favorite-list}': ['p(95)<1500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'mixed': {
        setupTimeout: '5m',
        scenarios: {
            favorite: {
                executor: 'ramping-vus',
                startVUs: 5,
                stages: [
                    { target: 30, duration: '2m' },
                    { target: 100, duration: '4m' },
                    { target: 180, duration: '4m' },
                    { target: 180, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'mixedFavorite',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:favorite-targets}': ['p(95)<1500'],
            'http_req_duration{type:read,surface:favorite-list}': ['p(95)<1500'],
            'http_req_duration{type:write}': ['p(95)<2500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
};

export const options = withQuickRunOptions(
    SCENARIO_OPTIONS[TEST_TYPE] || SCENARIO_OPTIONS['mixed']
);

export function setup() {
    logTestStart(`Favorite Stress [${TEST_TYPE}]`, BASE_URL);
    console.log(`   사용자 풀: ${USER_POOL}`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const baseToken = tokens[0];
    const groupContext = resolveGroupContext(baseToken);
    const hotspot = prepareHotspotPools(baseToken, groupContext.groupIds);

    console.log(`✅ Setup 완료: tokens=${tokens.length}개, groupId=${groupContext.groupId}`);
    return {
        tokens,
        hotspot,
    };
}

export function toggleOnly(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.hotspot = data.hotspot || null;

    const restaurantId = pickRestaurantId(state);
    if (!restaurantId) return;

    const result = toggleFavoriteRestaurant(token, restaurantId);
    const success = result.response && (
        result.response.status === 200 ||
        result.response.status === 201 ||
        result.response.status === 204
    );
    metrics.add(success ? 1 : 0, 'favorite_success_count');
    sleep(0.4 + Math.random() * 0.8);
}

export function targetsRead(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const loc = randomLocation();
    let successCount = 0;

    const homeRes = getHomePage(token, loc.lat, loc.lon);
    if (homeRes && homeRes.status === 200) successCount++;

    const restaurantId = pickRandomRestaurantId(extractRestaurantIdsFromSectionsResponse(homeRes));

    const pageTargetsRes = getFavoriteTargets(token);
    if (pageTargetsRes && pageTargetsRes.status === 200) successCount++;

    const restaurantTargetsRes = getRestaurantFavoriteTargets(token, restaurantId);
    if (restaurantTargetsRes && restaurantTargetsRes.status === 200) successCount++;

    metrics.add(successCount, 'favorite_success_count');
    sleep(0.3 + Math.random() * 0.7);
}

export function mixedFavorite(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const roll = Math.random();
    if (roll < 0.45) {
        targetsRead(data);
    } else if (roll < 0.7) {
        listRead(data);
    } else {
        toggleOnly(data);
    }
}

function nextCursorFrom(res) {
    if (!res || res.status !== 200) return null;
    try {
        return res.json('data.pagination.nextCursor') || null;
    } catch (e) {
        return null;
    }
}

export function listRead(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.hotspot = data.hotspot || null;

    let successCount = 0;
    const myFavoritesRes = getMyFavoriteRestaurants(token);
    if (myFavoritesRes && myFavoritesRes.status === 200) {
        successCount++;

        const nextCursor = nextCursorFrom(myFavoritesRes);
        if (nextCursor && Math.random() < 0.3) {
            const nextPageRes = getMyFavoriteRestaurants(token, { cursor: nextCursor });
            if (nextPageRes && nextPageRes.status === 200) {
                successCount++;
            }
        }
    }

    const subgroupId = pickSubgroupId(state);
    if (subgroupId) {
        const subgroupFavoritesRes = getSubgroupFavoriteRestaurants(token, subgroupId);
        if (subgroupFavoritesRes && subgroupFavoritesRes.status === 200) {
            successCount++;

            const nextCursor = nextCursorFrom(subgroupFavoritesRes);
            if (nextCursor && Math.random() < 0.3) {
                const nextPageRes = getSubgroupFavoriteRestaurants(token, subgroupId, { cursor: nextCursor });
                if (nextPageRes && nextPageRes.status === 200) {
                    successCount++;
                }
            }
        }
    }

    metrics.add(successCount, 'favorite_success_count');
    sleep(0.3 + Math.random() * 0.7);
}

export function teardown() {
    console.log(`🏁 Favorite Stress [${TEST_TYPE}] 완료`);
}
