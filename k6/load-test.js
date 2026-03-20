import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

const originalDuration = new Trend('original_duration', true);
const v1Duration = new Trend('v1_duration', true);
const v2Duration = new Trend('v2_duration', true);
const v3Duration = new Trend('v3_duration', true);

const originalErrors = new Counter('original_errors');
const v1Errors = new Counter('v1_errors');
const v2Errors = new Counter('v2_errors');
const v3Errors = new Counter('v3_errors');

const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${TOKEN}`,
};

export const options = {
    scenarios: {
        original: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 30,
            exec: 'testOriginal',
        },
        v1: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 30,
            exec: 'testV1',
            startTime: '120s',
        },
        v2: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 30,
            exec: 'testV2',
            startTime: '240s',
        },
        v3: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 30,
            exec: 'testV3',
            startTime: '360s',
        },
    },
};

export function testOriginal() {
    const res = http.get(`${BASE_URL}/core/dev/members`, { headers, timeout: '60s' });
    originalDuration.add(res.timings.duration);
    if (!check(res, { 'original status 200': (r) => r.status === 200 })) {
        originalErrors.add(1);
    }
    sleep(0.3);
}

export function testV1() {
    const res = http.get(`${BASE_URL}/core/dev/members/v1`, { headers, timeout: '60s' });
    v1Duration.add(res.timings.duration);
    if (!check(res, { 'v1 status 200': (r) => r.status === 200 })) {
        v1Errors.add(1);
    }
    sleep(0.3);
}

export function testV2() {
    const res = http.get(`${BASE_URL}/core/dev/members/v2`, { headers, timeout: '60s' });
    v2Duration.add(res.timings.duration);
    if (!check(res, { 'v2 status 200': (r) => r.status === 200 })) {
        v2Errors.add(1);
    }
    sleep(0.3);
}

export function testV3() {
    const res = http.get(`${BASE_URL}/core/dev/members/v3`, { headers, timeout: '60s' });
    v3Duration.add(res.timings.duration);
    if (!check(res, { 'v3 status 200': (r) => r.status === 200 })) {
        v3Errors.add(1);
    }
    sleep(0.3);
}

export function handleSummary(data) {
    const getMetric = (name) => {
        const m = data.metrics[name];
        if (!m) return { avg: 'N/A', med: 'N/A', p95: 'N/A', p99: 'N/A', min: 'N/A', max: 'N/A', count: 0 };
        return {
            avg: m.values.avg?.toFixed(2) + 'ms',
            med: m.values.med?.toFixed(2) + 'ms',
            p95: (m.values['p(95)'] || 0).toFixed(2) + 'ms',
            p99: (m.values['p(99)'] || 0).toFixed(2) + 'ms',
            min: m.values.min?.toFixed(2) + 'ms',
            max: m.values.max?.toFixed(2) + 'ms',
            count: m.values.count || 0,
        };
    };

    const getCount = (name) => {
        const m = data.metrics[name];
        return m ? m.values.count : 0;
    };

    const original = getMetric('original_duration');
    const v1 = getMetric('v1_duration');
    const v2 = getMetric('v2_duration');
    const v3 = getMetric('v3_duration');

    const summary = `
============================================================
       부하 테스트 결과 비교 (1 VU, 30 iterations each)
============================================================

| 버전     | Avg         | Med         | P95         | Min         | Max         | Reqs | Errors |
|----------|-------------|-------------|-------------|-------------|-------------|------|--------|
| Original | ${original.avg.padStart(11)} | ${original.med.padStart(11)} | ${original.p95.padStart(11)} | ${original.min.padStart(11)} | ${original.max.padStart(11)} | ${String(original.count).padStart(4)} | ${String(getCount('original_errors')).padStart(6)} |
| V1       | ${v1.avg.padStart(11)} | ${v1.med.padStart(11)} | ${v1.p95.padStart(11)} | ${v1.min.padStart(11)} | ${v1.max.padStart(11)} | ${String(v1.count).padStart(4)} | ${String(getCount('v1_errors')).padStart(6)} |
| V2       | ${v2.avg.padStart(11)} | ${v2.med.padStart(11)} | ${v2.p95.padStart(11)} | ${v2.min.padStart(11)} | ${v2.max.padStart(11)} | ${String(v2.count).padStart(4)} | ${String(getCount('v2_errors')).padStart(6)} |
| V3       | ${v3.avg.padStart(11)} | ${v3.med.padStart(11)} | ${v3.p95.padStart(11)} | ${v3.min.padStart(11)} | ${v3.max.padStart(11)} | ${String(v3.count).padStart(4)} | ${String(getCount('v3_errors')).padStart(6)} |

============================================================
  Original: 1+3N 쿼리 (cohort/authority/team 모두 루프 안)
  V1:       2+2N 쿼리 (cohortMap 루프 밖)
  V2:       3+N  쿼리 (cohortMap + authority batch)
  V3:       4    쿼리 (전부 batch)
============================================================
`;

    console.log(summary);
    return {
        'k6/results.txt': summary,
    };
}
