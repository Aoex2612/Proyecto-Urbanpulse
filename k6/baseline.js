import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Métricas personalizadas para segmentar por endpoint
const endpointDuration = new Trend('endpoint_duration');
const endpointFailRate = new Rate('endpoint_failure_rate');

// Configuración general de la baseline
// Por defecto 10 usuarios virtuales durante 2 minutos (como especifica la práctica)
export const options = {
  vus: __ENV.VUS ? parseInt(__ENV.VUS) : 10,
  duration: __ENV.DURATION || '2m',
  thresholds: {
    // Umbral de fiabilidad: menos del 1% de errores en la baseline
    http_req_failed: ['rate<0.01'],
    endpoint_failure_rate: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'admin@urbanpulse.es';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'Admin1234';

// Endpoint objetivo: 'list_incidents' | 'get_incident' | 'search_incidents' | 'analytics_summary' | 'create_incident'
const TARGET_ENDPOINT = __ENV.ENDPOINT || 'list_incidents';

/**
 * Fase setup (se ejecuta 1 sola vez al inicio antes de que arranquen los VUs).
 * Realiza el login en la API de UrbanPulse para obtener un token JWT válido.
 */
export function setup() {
  const loginPayload = JSON.stringify({
    email: USER_EMAIL,
    password: USER_PASSWORD,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Intentamos login en v1 o fallback a ruta base
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
 * Función principal ejecutada por cada Virtual User (VU) en cada iteración.
 */
export default function (data) {
  const authHeaders = {
    'Content-Type': 'application/json',
  };

  if (data && data.token) {
    authHeaders['Authorization'] = `Bearer ${data.token}`;
  }

  let res;
  let testName = TARGET_ENDPOINT;

  switch (TARGET_ENDPOINT) {
    case 'get_incident':
      // 1. Lectura puntual por ID (ej. ID = 1)
      res = http.get(`${BASE_URL}/api/v1/incidents/1`, { headers: authHeaders });
      check(res, {
        'status es 200': (r) => r.status === 200,
      });
      break;

    case 'search_incidents':
      // 2. Búsqueda y filtrado (ej. por categoría ALUMBRADO)
      res = http.get(`${BASE_URL}/api/v1/incidents/search?category=ALUMBRADO`, { headers: authHeaders });
      check(res, {
        'status es 200': (r) => r.status === 200,
      });
      break;

    case 'analytics_summary':
      // 3. Consulta de cálculo analítico agregado
      res = http.get(`${BASE_URL}/api/v1/analytics/summary`, { headers: authHeaders });
      check(res, {
        'status es 200': (r) => r.status === 200,
      });
      break;

    case 'create_incident':
      // 4. Escritura de una nueva incidencia urbana con datos dinámicos
      const createPayload = JSON.stringify({
        title: `Incidencia baseline VU-${__VU}-${__ITER}`,
        description: `Reporte de prueba automático generado por k6 en iteración ${__ITER}`,
        category: 'ALUMBRADO',
        latitude: 36.7213 + (__VU * 0.0001),
        longitude: -4.4214 + (__ITER * 0.0001),
        address: 'Calle Larios, Málaga',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      });

      res = http.post(`${BASE_URL}/api/v1/incidents`, createPayload, { headers: authHeaders });
      check(res, {
        'status es 201': (r) => r.status === 201,
      });
      break;

    case 'list_incidents':
    default:
      // 5. Listado de incidencias activas para el mapa
      res = http.get(`${BASE_URL}/api/v1/incidents`, { headers: authHeaders });
      check(res, {
        'status es 200': (r) => r.status === 200,
      });
      break;
  }

  // Registrar métricas específicas
  if (res) {
    endpointDuration.add(res.timings.duration, { endpoint: testName });
    const isError = res.status >= 400 || res.status === 0;
    endpointFailRate.add(isError ? 1 : 0, { endpoint: testName });
  }

  // Pausa de 1 segundo (Think Time) para simular comportamiento humano estable
  sleep(1);
}
