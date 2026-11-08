/**
 * @fileoverview Servicio cliente HTTP para el módulo de Analítica Urbana y Reportes de UrbanPulse.
 * Conecta con los endpoints REST del backend protegidos mediante autenticación JWT y roles RBAC
 * (ANALYST, ADMIN, OPERATOR) para la obtención de métricas operacionales, agregaciones territoriales,
 * desgloses de rendimiento y sugerencias de asistencia inteligente (RF-1 al RF-18).
 */

import api from './api';

/**
 * Parámetros de consulta opcionales para filtrar el resumen analítico consolidado.
 *
 * @typedef {Object} AnalyticsFilterParams
 * @property {'TODAY'|'LAST_7_DAYS'|'LAST_30_DAYS'|'LAST_3_MONTHS'|'LAST_6_MONTHS'|'LAST_YEAR'|'SPECIFIC_YEAR'} [preset] - Preset temporal predefinido.
 * @property {string} [startDate] - Fecha inicial en formato ISO o YYYY-MM-DD.
 * @property {string} [endDate] - Fecha final en formato ISO o YYYY-MM-DD.
 * @property {number} [year] - Año específico para análisis histórico.
 * @property {string} [district] - Nombre del distrito municipal.
 * @property {string} [neighborhood] - Nombre del barrio o vecindario.
 */

/**
 * Obtiene el resumen integral consolidado de analítica urbana conforme a los filtros aplicados.
 * Incluye KPIs operacionales (MTTT, MTTR, cumplimiento SLA), agregaciones territoriales,
 * métricas departamentales y por categoría, desempeño del recomendador y la lista de incidencias filtradas.
 *
 * @param {AnalyticsFilterParams} [params={}] - Parámetros opcionales de filtrado temporal y territorial.
 * @returns {Promise<Object>} Promesa que resuelve al objeto consolidado AnalyticsSummaryResponse devuelto por el backend.
 * @throws {import('axios').AxiosError} Si la petición falla o el usuario carece de permisos autorizados (403).
 */
export const getAnalyticsSummary = async (params = {}) => {
  const response = await api.get('/api/analytics/summary', { params });
  return response.data;
};

/**
 * Parámetros opcionales para solicitar la recomendación asistida de departamento responsable.
 *
 * @typedef {Object} DepartmentRecommendationParams
 * @property {number|string} [incidentId] - Identificador único de una incidencia ya existente.
 * @property {string} [title] - Título descriptivo de la incidencia.
 * @property {string} [description] - Descripción detallada del suceso.
 * @property {string} [category] - Categoría temática clasificada.
 */

/**
 * Obtiene la sugerencia asistida del departamento municipal responsable con su grado de certeza porcentual.
 * Admite consulta por identificador de incidencia (`incidentId`) o por los atributos textuales (`title`, `description`, `category`).
 *
 * @param {DepartmentRecommendationParams} [params={}] - Parámetros de la incidencia a evaluar.
 * @returns {Promise<{suggestedDepartmentId: number|null, suggestedDepartmentName: string, confidencePercentage: number, isLowConfidenceWarning: boolean, reasoning: string}>} Objeto con la sugerencia y grado de confianza.
 * @throws {import('axios').AxiosError} Si la petición falla o el usuario no cuenta con rol autorizado.
 */
export const getDepartmentRecommendation = async (params = {}) => {
  const response = await api.get('/api/analytics/recommend-department', { params });
  return response.data;
};

const analyticsService = {
  getAnalyticsSummary,
  getDepartmentRecommendation,
};

export default analyticsService;
