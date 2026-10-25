/**
 * @fileoverview Servicio de gestión de incidencias urbanas y auditoría para UrbanPulse.
 * Proporciona métodos para consultar incidencias activas en el mapa, obtener detalles,
 * crear nuevas incidencias, actualizar el estado de una incidencia y consultar su historial de cambios.
 */

import api from './api';

/**
 * Obtiene la lista de todas las incidencias urbanas activas para representar en el mapa.
 * Excluye aquellas en estado CLOSED o REJECTED según las reglas del backend.
 *
 * @returns {Promise<Array<{id: number, title: string, description: string, category: string, status: string, priority: string, latitude: number, longitude: number, citizenId: number, citizenName: string, citizenEmail: string, createdAt: string, updatedAt: string}>>} Lista de incidencias activas.
 * @throws {import('axios').AxiosError} Si ocurre un error durante la petición HTTP.
 */
export const getAllIncidents = async () => {
  const response = await api.get('/api/incidents');
  return response.data;
};

/**
 * Obtiene los detalles de una incidencia específica a partir de su identificador único.
 *
 * @param {number|string} id - Identificador numérico de la incidencia a consultar.
 * @returns {Promise<{id: number, title: string, description: string, category: string, status: string, priority: string, latitude: number, longitude: number, citizenId: number, citizenName: string, citizenEmail: string, createdAt: string, updatedAt: string}>} Datos detallados de la incidencia.
 * @throws {import('axios').AxiosError} Si la incidencia no existe (HTTP 404) o falla la conexión.
 */
export const getIncidentById = async (id) => {
  const response = await api.get(`/api/incidents/${id}`);
  return response.data;
};

/**
 * Registra una nueva incidencia urbana en la plataforma asociada al ciudadano autenticado.
 *
 * @param {{title: string, description: string, category: string, latitude: number, longitude: number}} incidentData - Datos del formulario de creación de incidencia.
 * @returns {Promise<{id: number, title: string, description: string, category: string, status: string, priority: string, latitude: number, longitude: number, citizenId: number, citizenName: string, citizenEmail: string, createdAt: string, updatedAt: string}>} Incidencia creada devuelta por el servidor con estado REPORTED y prioridad MEDIUM (HTTP 201).
 * @throws {import('axios').AxiosError} Si la validación falla (HTTP 400) o el usuario no está autenticado (HTTP 401).
 */
export const createIncident = async (incidentData) => {
  const response = await api.post('/api/incidents', incidentData);
  return response.data;
};

/**
 * Actualiza el estado operativo de una incidencia y registra la transición atómica en la tabla de auditoría.
 * Operación restringida a usuarios con roles operativos (OPERATOR o ADMIN).
 *
 * @param {number|string} id - Identificador numérico de la incidencia cuyo estado se modificará.
 * @param {{status: string, reason?: string}} statusData - Objeto con el nuevo estado y el motivo justificado de la transición.
 * @returns {Promise<{id: number, title: string, description: string, category: string, status: string, priority: string, latitude: number, longitude: number, citizenId: number, citizenName: string, citizenEmail: string, createdAt: string, updatedAt: string}>} Incidencia actualizada con el nuevo estado (HTTP 200).
 * @throws {import('axios').AxiosError} Si el usuario no tiene permisos (HTTP 403), la incidencia no existe (HTTP 404) o los datos son inválidos (HTTP 400).
 */
export const updateIncidentStatus = async (id, statusData) => {
  const response = await api.patch(`/api/incidents/${id}/status`, statusData);
  return response.data;
};

/**
 * Alias de updateIncidentStatus para compatibilidad de nomenclatura en componentes.
 *
 * @param {number|string} id - Identificador de la incidencia.
 * @param {{status: string, reason?: string}} statusData - Datos de la actualización.
 * @returns {Promise<Object>} Promesa con la respuesta del servidor.
 */
export const updateStatus = updateIncidentStatus;

/**
 * Recupera el registro cronológico inmutable de cambios de estado de una incidencia para el timeline de auditoría.
 * Operación restringida a usuarios con rol administrativo (ADMIN).
 *
 * @param {number|string} id - Identificador numérico de la incidencia a auditar.
 * @returns {Promise<Array<{id: number, incidentId: number, previousStatus: string, newStatus: string, changeReason: string, changedByUserId: number, changedByUserName: string, changedByUserEmail: string, createdAt: string}>>} Historial de cambios de estado ordenado cronológicamente (HTTP 200).
 * @throws {import('axios').AxiosError} Si el usuario carece de rol ADMIN (HTTP 403) o la incidencia no existe (HTTP 404).
 */
export const getIncidentHistory = async (id) => {
  const response = await api.get(`/api/incidents/${id}/history`);
  return response.data;
};

const incidentService = {
  getAllIncidents,
  getIncidentById,
  createIncident,
  updateIncidentStatus,
  updateStatus,
  getIncidentHistory,
};

export default incidentService;
