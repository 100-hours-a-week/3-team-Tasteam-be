import { sleep } from 'k6';
import {
    BASE_URL,
    batchLogin,
    createState,
    getFoodCategories,
    getHomePage,
    getRestaurantDetail,
    getRestaurantMenus,
    getRestaurantReviews,
    getSubgroupReviews,
    extractRestaurantIdsFromSectionsResponse,
    pickRandomRestaurantId,
    pickRestaurantId,
    pickSubgroupId,
    prepareHotspotPools,
    randomLocation,
    resolveGroupContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

const TEST_TYPE = __ENV.TEST_TYPE || 'detail-only';
const USER_POOL = Number(__ENV.USER_POOL || '100');
const metrics = new SuccessMetrics(['restaurant_read_success_count']);

const SCENARIO_OPTIONS = {
    'detail-only': {
        setupTimeout: '5m',
        scenarios: {
            restaurant_read: {
                executor: 'ramping-vus',
                startVUs: 10,
                stages: [
                    { target: 50, duration: '2m' },
                    { target: 200, duration: '4m' },
                    { target: 400, duration: '4m' },
                    { target: 400, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'detailOnly',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:restaurant-detail}': ['p(95)<1500'],
            'http_req_duration{type:read,surface:review-list}': ['p(95)<1500'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'list-then-detail': {
        setupTimeout: '5m',
        scenarios: {
            restaurant_read: {
                executor: 'ramping-vus',
                startVUs: 10,
                stages: [
                    { target: 50, duration: '2m' },
                    { target: 150, duration: '4m' },
                    { target: 300, duration: '4m' },
                    { target: 300, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'listThenDetail',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:restaurant-list}': ['p(95)<1800'],
            'http_req_duration{type:read,surface:restaurant-detail}': ['p(95)<1800'],
            'http_req_duration{type:read,surface:review-list}': ['p(95)<1800'],
            'http_req_failed': ['rate<0.01'],
        },
    },
    'review-list-heavy': {
        setupTimeout: '5m',
        scenarios: {
            restaurant_read: {
                executor: 'ramping-vus',
                startVUs: 10,
                stages: [
                    { target: 50, duration: '2m' },
                    { target: 180, duration: '4m' },
                    { target: 320, duration: '4m' },
                    { target: 320, duration: '4m' },
                    { target: 0, duration: '2m' },
                ],
                exec: 'reviewListHeavy',
            },
        },
        thresholds: {
            'http_req_duration{type:read,surface:review-list}': ['p(95)<1800'],
            'http_req_failed': ['rate<0.01'],
        },
    },
};

export const options = withQuickRunOptions(
    SCENARIO_OPTIONS[TEST_TYPE] || SCENARIO_OPTIONS['detail-only']
);

export function setup() {
    logTestStart(`Restaurant Read Stress [${TEST_TYPE}]`, BASE_URL);
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

export function detailOnly(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.hotspot = data.hotspot || null;

    const restaurantId = pickRestaurantId(state);
    if (!restaurantId) return;

    let successCount = 0;
    const detailRes = getRestaurantDetail(token, restaurantId);
    if (detailRes && detailRes.status === 200) successCount++;

    const menuRes = getRestaurantMenus(token, restaurantId);
    if (menuRes && menuRes.status === 200) successCount++;

    const reviewRes = getRestaurantReviews(token, restaurantId);
    if (reviewRes && reviewRes.response && reviewRes.response.status === 200) successCount++;

    metrics.add(successCount, 'restaurant_read_success_count');
    sleep(0.3 + Math.random() * 0.7);
}

export function listThenDetail(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.hotspot = data.hotspot || null;

    const loc = randomLocation();
    let successCount = 0;

    const homeRes = getHomePage(token, loc.lat, loc.lon);
    if (homeRes && homeRes.status === 200) successCount++;

    const categoriesRes = getFoodCategories();
    if (categoriesRes && categoriesRes.status === 200) successCount++;

    const homeRestaurantId = pickRandomRestaurantId(extractRestaurantIdsFromSectionsResponse(homeRes));
    const restaurantId = pickRestaurantId(state, homeRestaurantId);
    if (restaurantId) {
        const detailRes = getRestaurantDetail(token, restaurantId);
        if (detailRes && detailRes.status === 200) successCount++;

        const reviewRes = getRestaurantReviews(token, restaurantId);
        if (reviewRes && reviewRes.response && reviewRes.response.status === 200) successCount++;
    }

    metrics.add(successCount, 'restaurant_read_success_count');
    sleep(0.5 + Math.random());
}

function pickReviewPageSize() {
    const sizes = [10, 20, 50];
    return sizes[Math.floor(Math.random() * sizes.length)];
}

export function reviewListHeavy(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.hotspot = data.hotspot || null;

    let successCount = 0;
    const restaurantId = pickRestaurantId(state);
    const subgroupId = pickSubgroupId(state);
    const size = pickReviewPageSize();

    if (restaurantId) {
        const reviewRes = getRestaurantReviews(token, restaurantId, { size });
        if (reviewRes && reviewRes.response && reviewRes.response.status === 200) {
            successCount++;
        }
    }

    if (subgroupId && Math.random() < 0.5) {
        const subgroupReviewRes = getSubgroupReviews(token, subgroupId, { size });
        if (subgroupReviewRes && subgroupReviewRes.status === 200) {
            successCount++;
        }
    }

    metrics.add(successCount, 'restaurant_read_success_count');
    sleep(0.3 + Math.random() * 0.7);
}

export function teardown() {
    console.log(`🏁 Restaurant Read Stress [${TEST_TYPE}] 완료`);
}
