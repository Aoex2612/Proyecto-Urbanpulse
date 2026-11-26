import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MODE = __ENV.MODE || 'filtered_search';
const VUS = __ENV.VUS ? parseInt(__ENV.VUS, 10) : 5;
const DURATION = __ENV.DURATION || '2m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@urbanpulse.es';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'Admin1234';
const CITIZEN_EMAIL = __ENV.CITIZEN_EMAIL || 'citizen@urbanpulse.es';
const CITIZEN_PASSWORD = __ENV.CITIZEN_PASSWORD || 'Citizen1234';

const endpointDuration = new Trend('endpoint_duration');
const endpointFailureRate = new Rate('endpoint_failure_rate');
const supportedModes = ['filtered_search', 'paginated', 'analytics'];

if (!supportedModes.includes(MODE)) {
  throw new Error(`MODE debe ser uno de: ${supportedModes.join(', ')}`);
}

export const options = {
  vus: VUS,
  duration: DURATION,
  discardResponseBodies: true,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(95)', 'p(99)'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    endpoint_failure_rate: ['rate<0.01'],
  },
};

export function setup() {
  const isPagination = MODE === 'paginated';
  const email = isPagination ? CITIZEN_EMAIL : ADMIN_EMAIL;
  const password = isPagination ? CITIZEN_PASSWORD : ADMIN_PASSWORD;
  const response = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    {
      headers: { 'Content-Type': 'application/json' },
      responseType: 'text',
      timeout: REQUEST_TIMEOUT,
    },
  );

  if (response.status !== 200) {
    fail(`[SETUP] Login fallido para MODE=${MODE} (HTTP ${response.status})`);
  }

  let body;
  try {
    body = response.json();
  } catch (error) {
    fail(`[SETUP] La respuesta de login no contiene JSON válido: ${error.message}`);
  }

  const token = body && (body.token || body.data?.token);
  if (!token) {
    fail('[SETUP] La respuesta de login no contiene un token JWT');
  }

  return { token };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };
  const urls = {
    filtered_search: `${BASE_URL}/api/v1/incidents/search?category=ALUMBRADO&district=Centro&startDate=2020-01-01T00%3A00%3A00Z&endDate=2099-12-31T23%3A59%3A59Z`,
    paginated: `${BASE_URL}/api/v1/incidents/my-incidents?page=0&size=50`,
    analytics: `${BASE_URL}/api/analytics/summary?startDate=2020-01-01T00%3A00%3A00Z&endDate=2099-12-31T23%3A59%3A59Z&district=Centro`,
  };
  const response = http.get(urls[MODE], {
    headers,
    timeout: REQUEST_TIMEOUT,
    tags: { experiment: '4', mode: MODE },
  });
  const succeeded = check(response, {
    [`${MODE} responde HTTP 200`]: (result) => result.status === 200,
  });

  endpointDuration.add(response.timings.duration, { mode: MODE });
  endpointFailureRate.add(succeeded ? 0 : 1, { mode: MODE });
  sleep(1);
}
