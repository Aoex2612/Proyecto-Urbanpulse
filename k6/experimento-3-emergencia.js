import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Métricas personalizadas segmentadas por tipo de operación
const readDuration = new Trend('read_map_duration');
const writeDuration = new Trend('write_incident_duration');
const readFailRate = new Rate('read_map_failures');
const writeFailRate = new Rate('write_incident_failures');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'admin@urbanpulse.es';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'Admin1234';

/**
 * EXPERIMENTO 3: Simulación de un pico de incidencias urbanas (Emergencia Urbana / Spike Test)
 * 
 * Pregunta arquitectónica clave:
 * ¿Qué ocurre con la creación de incidencias si al mismo tiempo hay muchos
 * usuarios consultando el mapa de incidencias?
 * 
 * Escenario:
 * 1. map_readers: Tráfico continuo de fondo de ciudadanos consultando el mapa / listado de incidencias.
 * 2. emergency_writers: Flujo con pico brusco (spike) que simula ciudadanos reportando incidencias
 *    masivamente durante un temporal de lluvia (alcantarillas obstruidas, inundaciones, etc.).
 */
export const options = {
  scenarios: {
    // Escenario 1: Lectores del mapa (Carga continua de fondo)
    map_readers: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 }, // Calentamiento y carga base
        { duration: '1m', target: 20 },  // Tráfico constante consultando el mapa
        { duration: '30s', target: 40 }, // Aumento de consultas durante la alarma meteorológica
        { duration: '1m', target: 40 },  // Mantener alto volumen de consultas
        { duration: '30s', target: 10 }, // Regreso paulatino a la normalidad
        { duration: '30s', target: 0 },  // Enfriamiento
      ],
      gracefulRampDown: '10s',
      exec: 'readMapTask',
    },

    // Escenario 2: Pico extraordinario de creación de incidencias (Spike de escritura)
    emergency_writers: {
      executor: 'ramping-vus',
      startVUs: 1,
      startTime: '30s', // Comienza tras la estabilización inicial
      stages: [
        { duration: '15s', target: 5 },  // Empieza la lluvia: primeros reportes
        { duration: '15s', target: 50 }, // Pico brusco (Spike): tromba de agua, saturación de reportes
        { duration: '1m',  target: 50 }, // Sostener el pico de emergencias
        { duration: '15s', target: 5 },  // Cesa el temporal: caída rápida del flujo de reportes
        { duration: '45s', target: 2 },  // Fase de recuperación / resiliencia del sistema
        { duration: '15s', target: 0 },  // Fin
      ],
      gracefulRampDown: '10s',
      exec: 'reportEmergencyTask',
    },
  },

  thresholds: {
    // Umbrales de calidad para evaluar la resiliencia
    http_req_failed: ['rate<0.05'], // Permitir hasta 5% de fallos globales durante el spike
    read_map_failures: ['rate<0.05'],
    write_incident_failures: ['rate<0.05'],
    'read_map_duration': ['p(95)<1500'],    // El mapa no debería demorar más de 1.5s en p95
    'write_incident_duration': ['p(95)<2500'], // La creación no debería exceder 2.5s en p95
  },
};

/**
 * Fase setup: Obtención del token JWT
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
    fail(`[SETUP] Login fallido (Status ${res.status}): ${res.body}`);
  }

  const jsonBody = res.json();
  const token = jsonBody.token || (jsonBody.data && jsonBody.data.token);
  if (!token) {
    fail(`[SETUP] No se pudo obtener el token JWT de autenticación de la respuesta: ${res.body}`);
  }
  return { token: token };
}

/**
 * Tarea 1: Ciudadano / Operador consultando incidencias para el mapa (GET /api/v1/incidents)
 */
export function readMapTask(data) {
  const headers = { 'Content-Type': 'application/json' };
  if (data && data.token) headers['Authorization'] = `Bearer ${data.token}`;

  const res = http.get(`${BASE_URL}/api/v1/incidents`, { headers });
  
  const success = check(res, {
    'map: status 200': (r) => r.status === 200,
  });

  readDuration.add(res.timings.duration);
  readFailRate.add(!success);

  // Think time moderado simulando interacción con mapa interactivo
  sleep(1);
}

/**
 * Tarea 2: Ciudadano reportando emergencia urbana (POST /api/v1/incidents)
 */
export function reportEmergencyTask(data) {
  const headers = { 'Content-Type': 'application/json' };
  if (data && data.token) headers['Authorization'] = `Bearer ${data.token}`;

  // Tipos de problemas típicos de emergencias por lluvia en Málaga
  const emergencyDescriptions = [
    'Alcantarilla desbordada en calzada principal',
    'Bolsa de agua impidiendo el tráfico vehicular',
    'Rotura de rama de árbol de gran porte por viento y lluvia',
    'Inundación de paso subterráneo',
    'Fallo de semáforos por cortocircuito de pluviales',
  ];

  const desc = emergencyDescriptions[Math.floor(Math.random() * emergencyDescriptions.length)];
  
  // Coordenadas con variación aleatoria en torno al centro / zonas inundables
  const payload = JSON.stringify({
    title: `EMERGENCIA PLUVIAL: VU-${__VU}-${__ITER}`,
    description: `${desc} (Reporte automático k6 iteración ${__ITER})`,
    category: 'ALCANTARILLADO',
    latitude: 36.7196 + (Math.random() - 0.5) * 0.02,
    longitude: -4.4200 + (Math.random() - 0.5) * 0.02,
    address: 'Zona Centro / Carretera de Cádiz, Málaga',
    district: 'Centro',
    neighborhood: 'Zona Afectada',
  });

  const res = http.post(`${BASE_URL}/api/v1/incidents`, payload, { headers });

  const success = check(res, {
    'emergency: status 201': (r) => r.status === 201,
  });

  writeDuration.add(res.timings.duration);
  writeFailRate.add(!success);

  // Menor think time durante el pico de emergencia
  sleep(0.5);
}
