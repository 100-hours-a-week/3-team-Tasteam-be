import { Counter } from 'k6/metrics';

/**
 * Standard test start logger
 * @param {string} testName - Name of the test
 * @param {string} baseUrl - Target Base URL
 */
export function logTestStart(testName, baseUrl) {
    console.log(`🔥 ${testName} 시작`);
    console.log(`   Test ID: ${__ENV.TEST_ID}`);
    console.log(`   Target: ${baseUrl}`);
}

/**
 * Helper class to manage success counting metrics
 */
export class SuccessMetrics {
    constructor(metricNames = []) {
        this.counters = new Map();

        // Always create a total success counter
        this.totalCounter = new Counter('request_success_count');
        this.counters.set('total', this.totalCounter);

        // Create specific counters
        metricNames.forEach(name => {
            if (name) {
                this.counters.set(name, new Counter(name));
            }
        });
    }

    /**
     * Add success count
     * @param {number} count - Number of successes to add
     * @param {string} [specificMetric] - Optional specific metric key to increment
     */
    add(count, specificMetric = null) {
        if (count > 0) {
            this.totalCounter.add(count);

            if (specificMetric && this.counters.has(specificMetric)) {
                this.counters.get(specificMetric).add(count);
            }
        }
    }
}

/**
 * Journey별 성공 카운터 메트릭을 생성합니다.
 * @returns {SuccessMetrics}
 */
export function createJourneyMetrics() {
    return new SuccessMetrics([
        'browsing_count',
        'searching_count',
        'group_count',
        'subgroup_count',
        'personal_count',
        'chat_count',
        'writing_count',
    ]);
}
