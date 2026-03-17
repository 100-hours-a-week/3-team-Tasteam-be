import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { withQuickRunOptions } from '../../shared/quick-run.js';
import { BASE_URL, createReview, getReviewKeywords, login, resolveGroupContext } from '../../shared/scenarios.js';
import { logTestStart } from '../../shared/test-utils.js';

const TEST_TYPE = (__ENV.TEST_TYPE || 'review-create-burst').toLowerCase();
const ADMIN_LOGIN_PATH = __ENV.ADMIN_LOGIN_PATH || '/api/v1/admin/auth/login';
const REVIEW_TARGET_RESTAURANT_ID = Number(__ENV.TEST_RESTAURANT_ID || '0');
const RAW_EXPORT_DT = __ENV.RAW_EXPORT_DT || '';
const RAW_EXPORT_TARGETS = (__ENV.RAW_EXPORT_TARGETS || 'RESTAURANTS,MENUS')
    .split(',')
    .map((value) => value.trim())
    .filter((value) => value.length > 0);
const RECOMMENDATION_MODEL_VERSION = __ENV.RECOMMENDATION_MODEL_VERSION || 'deepfm-local';
const RECOMMENDATION_S3_URI = __ENV.RECOMMENDATION_S3_URI || 's3://tasteam-dev-analytics/result/';

const reviewCreateFailures = new Counter('ai_pipeline_review_create_failures');
const rawExportFailures = new Counter('ai_pipeline_raw_export_failures');
const recommendationImportFailures = new Counter('ai_pipeline_recommendation_import_failures');

const OPTIONS_BY_TYPE = {
    'review-create-burst': {
        scenarios: {
            review_create_burst: {
                executor: 'ramping-arrival-rate',
                startRate: 1,
                timeUnit: '1s',
                preAllocatedVUs: 10,
                maxVUs: 100,
                stages: [
                    { target: 5, duration: '30s' },
                    { target: 20, duration: '1m' },
                    { target: 20, duration: '1m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'runReviewCreateBurst',
            },
        },
        thresholds: {
            http_req_failed: ['rate<0.05'],
            'http_req_duration{name:create_review}': ['p(95)<3000'],
        },
    },
    'raw-export': {
        scenarios: {
            raw_export: {
                executor: 'shared-iterations',
                vus: 1,
                iterations: 1,
                exec: 'runRawExport',
            },
        },
        thresholds: {
            http_req_failed: ['rate<0.05'],
            'http_req_duration{name:admin_raw_export}': ['p(95)<5000'],
        },
    },
    'raw-export-repeat': {
        scenarios: {
            raw_export_repeat: {
                executor: 'constant-vus',
                vus: 1,
                duration: '1m',
                exec: 'runRawExport',
            },
        },
        thresholds: {
            http_req_failed: ['rate<0.05'],
            'http_req_duration{name:admin_raw_export}': ['p(95)<5000'],
        },
    },
    'recommendation-import': {
        scenarios: {
            recommendation_import: {
                executor: 'shared-iterations',
                vus: 1,
                iterations: 1,
                exec: 'runRecommendationImport',
            },
        },
        thresholds: {
            http_req_failed: ['rate<0.05'],
            'http_req_duration{name:admin_recommendation_import}': ['p(95)<10000'],
        },
    },
    'recommendation-import-repeat': {
        scenarios: {
            recommendation_import_repeat: {
                executor: 'constant-vus',
                vus: 1,
                duration: '1m',
                exec: 'runRecommendationImport',
            },
        },
        thresholds: {
            http_req_failed: ['rate<0.05'],
            'http_req_duration{name:admin_recommendation_import}': ['p(95)<10000'],
        },
    },
};

export const options = withQuickRunOptions(OPTIONS_BY_TYPE[TEST_TYPE] || OPTIONS_BY_TYPE['review-create-burst']);

export function setup() {
    logTestStart(`AI Pipeline Test [${TEST_TYPE}]`, BASE_URL);

    const memberToken = login();
    if (!memberToken) {
        throw new Error('테스트 계정 로그인에 실패했습니다. TEST_AUTH_TOKEN_PATH 또는 테스트 계정을 확인하세요.');
    }

    const groupContext = resolveGroupContext(memberToken);
    const keywordIds = getReviewKeywords(memberToken);

    let adminToken = null;
    if (TEST_TYPE !== 'review-create-burst') {
        adminToken = loginAdmin();
        if (!adminToken) {
            throw new Error('어드민 로그인에 실패했습니다. ADMIN_USERNAME / ADMIN_PASSWORD 를 확인하세요.');
        }
    }

    if (TEST_TYPE === 'review-create-burst') {
        if (!groupContext.groupId) {
            throw new Error('리뷰 생성 테스트에 필요한 그룹 컨텍스트를 확보하지 못했습니다.');
        }
        if (!REVIEW_TARGET_RESTAURANT_ID) {
            throw new Error('리뷰 생성 테스트는 TEST_RESTAURANT_ID 환경변수가 필요합니다.');
        }
    }

    if (TEST_TYPE === 'recommendation-import' && !RECOMMENDATION_S3_URI) {
        throw new Error('추천 import 테스트는 RECOMMENDATION_S3_URI 환경변수가 필요합니다.');
    }

    return {
        memberToken,
        adminToken,
        groupId: groupContext.groupId,
        keywordIds,
    };
}

export function runReviewCreateBurst(data) {
    const res = createReview(
        data.memberToken,
        data.groupId,
        data.keywordIds,
        REVIEW_TARGET_RESTAURANT_ID
    );

    if (!res) {
        reviewCreateFailures.add(1);
        return;
    }

    check(res, {
        '리뷰 생성 성공': (r) => r.status === 200 || r.status === 201,
    });

    if (!(res.status === 200 || res.status === 201)) {
        reviewCreateFailures.add(1);
    }
}

export function runRawExport(data) {
    const payload = JSON.stringify({
        dt: RAW_EXPORT_DT || null,
        targets: RAW_EXPORT_TARGETS,
        requestId: buildRequestId('raw-export'),
    });

    const res = http.post(
        `${BASE_URL}/api/v1/admin/analytics/raw-exports`,
        payload,
        {
            headers: authHeaders(data.adminToken),
            tags: { name: 'admin_raw_export', type: 'write' },
        }
    );

    check(res, {
        'raw export accepted': (r) => r.status === 202,
    });

    if (res.status !== 202) {
        rawExportFailures.add(1);
    }

}

export function runRecommendationImport(data) {
    const payload = JSON.stringify({
        modelVersion: RECOMMENDATION_MODEL_VERSION,
        s3PrefixOrUri: RECOMMENDATION_S3_URI,
        requestId: buildRequestId('recommendation-import'),
    });

    const res = http.post(
        `${BASE_URL}/api/v1/admin/recommendations/import`,
        payload,
        {
            headers: authHeaders(data.adminToken),
            tags: { name: 'admin_recommendation_import', type: 'write' },
        }
    );

    check(res, {
        'recommendation import success': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        recommendationImportFailures.add(1);
    }

}

export function teardown() {
    console.log(`🏁 AI Pipeline Test [${TEST_TYPE}] 완료`);
}

function loginAdmin() {
    const username = __ENV.ADMIN_USERNAME;
    const password = __ENV.ADMIN_PASSWORD;

    if (!username || !password) {
        return null;
    }

    const res = http.post(
        `${BASE_URL}${ADMIN_LOGIN_PATH}`,
        JSON.stringify({ username, password }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: { name: 'admin_login', type: 'write' },
        }
    );

    const ok = check(res, {
        '어드민 로그인 성공': (r) => r.status === 200,
    });

    if (!ok) {
        return null;
    }

    return res.json('data.accessToken');
}

function authHeaders(token) {
    return {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
    };
}

function buildRequestId(prefix) {
    return `${prefix}-${Date.now()}-vu${__VU}-iter${__ITER}`;
}
