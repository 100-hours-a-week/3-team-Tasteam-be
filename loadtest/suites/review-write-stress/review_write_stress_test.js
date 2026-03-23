import { sleep } from 'k6';
import {
    BASE_URL,
    batchLogin,
    createState,
    createReview,
    getRestaurantDetail,
    getReviewKeywords,
    pickRestaurantId,
    prepareHotspotPools,
    resolveGroupContext,
} from '../../shared/scenarios.js';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { logTestStart, SuccessMetrics } from '../../shared/test-utils.js';

const USER_POOL = Number(__ENV.USER_POOL || '100');
const FOLLOWUP_READ_RATIO = Number(__ENV.FOLLOWUP_READ_RATIO || '0.2');
const metrics = new SuccessMetrics(['review_write_success_count']);

export const options = withQuickRunOptions({
    setupTimeout: '5m',
    scenarios: {
        review_write: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { target: 30, duration: '2m' },
                { target: 80, duration: '4m' },
                { target: 150, duration: '4m' },
                { target: 150, duration: '4m' },
                { target: 0, duration: '2m' },
            ],
            exec: 'reviewWrite',
        },
    },
    thresholds: {
        'http_req_duration{type:write,surface:review-create}': ['p(95)<3000'],
        'http_req_failed': ['rate<0.01'],
    },
});

export function setup() {
    logTestStart('Review Write Stress', BASE_URL);
    console.log(`   사용자 풀: ${USER_POOL}`);
    console.log(`   후속 상세 조회 비율: ${Math.round(FOLLOWUP_READ_RATIO * 100)}%`);

    const tokens = batchLogin(USER_POOL);
    if (!tokens || tokens.length === 0) {
        console.error('❌ 로그인 실패 - 테스트 중단');
        return null;
    }

    const baseToken = tokens[0];
    const keywordIds = getReviewKeywords(baseToken);
    const groupContext = resolveGroupContext(baseToken);

    if (!groupContext.groupId) {
        throw new Error('review-write-stress에 필요한 그룹 컨텍스트를 확보하지 못했습니다.');
    }

    const hotspot = prepareHotspotPools(baseToken, groupContext.groupIds);

    console.log(`✅ Setup 완료: tokens=${tokens.length}개, groupId=${groupContext.groupId}, keywords=${keywordIds.length}개`);
    return {
        tokens,
        groupId: groupContext.groupId,
        keywordIds,
        hotspot,
    };
}

export function reviewWrite(data) {
    if (!data || !data.tokens || data.tokens.length === 0) return;

    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const state = createState();
    state.token = token;
    state.groupId = data.groupId;
    state.keywordIds = data.keywordIds;
    state.hotspot = data.hotspot || null;

    const restaurantId = pickRestaurantId(state);
    if (!restaurantId) return;

    let successCount = 0;
    const res = createReview(token, data.groupId, data.keywordIds, restaurantId);
    if (res && (res.status === 200 || res.status === 201)) {
        successCount++;
    }

    if (Math.random() < FOLLOWUP_READ_RATIO) {
        const detailRes = getRestaurantDetail(token, restaurantId);
        if (detailRes && detailRes.status === 200) {
            successCount++;
        }
    }

    metrics.add(successCount, 'review_write_success_count');
    sleep(0.5 + Math.random());
}

export function teardown() {
    console.log('🏁 Review Write Stress 완료');
}
