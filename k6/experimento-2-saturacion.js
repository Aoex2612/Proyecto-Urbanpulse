import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Métricas personalizadas
const reqDuration = new Trend('custom_req_duration');
const failureRate = new Rate('custom_failure_rate');
const droppedCounter = new Counter('custom_dropped_iterations');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'admin@urbanpulse.es';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'Admin1234';

// Modo de ejecución: 'ramping_vus' (por defecto) o 'arrival_rate'
const MODE = __ENV.MODE || 'ramping_vus';

// Endpoint a estresar: por defecto 'get_incident' para medir capacidad pura de la API
// Opciones: 'get_incident' | 'list_incidents' | 'search_incidents' | 'analytics_summary' | 'create_incident'
const TARGET_ENDPOINT = __ENV.ENDPOINT || 'get_incident';

export const options = {
  scenarios: {
    // -------------------------------------------------------------------------
    // Escenario A: Ramping VUs (10 -> 25 -> 50 -> 100 VUs)
    // Evalúa la degradación progresiva de latencia según aumenta la concurrencia
    // -------------------------------------------------------------------------
    ...(MODE === 'ramping_vus'
      ? {
          ramping_concurrency: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
              { duration: '30s', target: 10 },  // Rampa a 10 VUs
              { duration: '1m',  target: 10 },  // Carga estable 10 VUs
              { duration: '30s', target: 25 },  // Rampa a 25 VUs
              { duration: '1m',  target: 25 },  // Carga estable 25 VUs
              { duration: '30s', target: 50 },  // Rampa a 50 VUs
              { duration: '1m',  target: 50 },  // Carga estable 50 VUs
              { duration: '30s', target: 100 }, // Rampa a 100 VUs
              { duration: '1m',  target: 100 }, // Carga estable 100 VUs
              { duration: '30s', target: 0 },   // Rampa de bajada (enfriamiento)
            ],
            gracefulRampDown: '10s',
          },
        }
      : {}),

    // -------------------------------------------------------------------------
    // Escenario B: Constant Arrival Rate (Tasa de peticiones fija)
    // Evalúa capacidad independiente del tiempo de respuesta
    // y monitoriza la saturación mediante dropped_iterations
    // -------------------------------------------------------------------------
    ...(MODE === 'arrival_rate'
      ? {
          arrival_rate_test: {
            executor: 'constant-arrival-rate',
            rate: __ENV.RATE ? parseInt(__ENV.RATE) : 100, // Tasa deseada (ej. 50, 100, 200 req/s)
            timeUnit: '1s',
            duration: __ENV.DURATION || '2m',
            preAllocatedVUs: 20,
            maxVUs: __ENV.MAX_VUS ? parseInt(__ENV.MAX_VUS) : 100,
          },
        }
      : {}),
  },

  thresholds: {
    // Criterios de calidad generales
    http_req_failed: ['rate<0.05'], // Permitir hasta 5% de fallos bajo estrés extremo
    custom_failure_rate: ['rate<0.05'],
    // En constant-arrival-rate vigilamos dropped_iterations
    ...(MODE === 'arrival_rate'
      ? {
          dropped_iterations: ['count==0'],
        }
      : {}),
  },
};

/**
 * Fase setup (1 vez antes del inicio de los VUs).
 * Obtiene el token JWT Bearer.
 */
export function setup() {
  const loginPayload = JSON.stringify({
    email: USER_EMAIL,
    password: USER_PASSWORD,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  let res = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, params);
  if (res.status === 404) {
    res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, params);
  }

  if (res.status !== 200) {
    console.warn(`[SETUP] Login fallido (Status ${res.status}): ${res.body}`);
    return { token: null };
  }

  const jsonBody = res.json();
  const token = jsonBody.token || (jsonBody.data && jsonBody.data.token);
  return { token: token };
}

/**
 * Ejecución por iteración
 */
export default function (data) {
  const authHeaders = {
    'Content-Type': 'application/json',
  };

  if (data && data.token) {
    authHeaders['Authorization'] = `Bearer ${data.token}`;
  }

  let res;

  switch (TARGET_ENDPOINT) {
    case 'list_incidents':
      res = http.get(`${BASE_URL}/api/v1/incidents`, { headers: authHeaders });
      check(res, { 'status es 200': (r) => r.status === 200 });
      break;

    case 'search_incidents':
      res = http.get(`${BASE_URL}/api/v1/incidents/search?category=ALUMBRADO`, { headers: authHeaders });
      check(res, { 'status es 200': (r) => r.status === 200 });
      break;

    case 'analytics_summary':
      res = http.get(`${BASE_URL}/api/v1/analytics/summary`, { headers: authHeaders });
      check(res, { 'status es 200': (r) => r.status === 200 });
      break;

    case 'create_incident':
      const createPayload = JSON.stringify({
        title: `Incidencia stress VU-${__VU}-${__ITER}`,
        description: `Incidencia generada durante prueba de saturacion`,
        category: 'ALUMBRADO',
        latitude: 36.7213 + (__VU * 0.0001),
        longitude: -4.4214 + (__ITER * 0.0001),
        address: 'Calle Marqués de Larios',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      });
      res = http.post(`${BASE_URL}/api/v1/incidents`, createPayload, { headers: authHeaders });
      check(res, { 'status es 201': (r) => r.status === 201 });
      break;

    case 'get_incident':
    default:
      res = http.get(`${BASE_URL}/api/v1/incidents/1`, { headers: authHeaders });
      check(res, { 'status es 200': (r) => r.status === 200 });
      break;
  }

  if (res) {
    reqDuration.add(res.timings.duration);
    const isError = res.status >= 400 || res.status === 0;
    failureRate.add(isError ? 1 : 0);
  }

  // En modo ramping_vus aplicamos think-time de 1s para simular concurrencia humana
  // En arrival_rate no se requiere sleep porque la tasa la impone el ejecutor
  if (MODE === 'ramping_vus') {
    sleep(1);
  }
}
