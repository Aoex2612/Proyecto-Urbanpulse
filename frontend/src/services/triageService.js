/**
 * @fileoverview Servicio cliente HTTP para el triaje operativo, ciclo de vida, asignaciones
 * multidepartamentales, técnicos y comentarios de incidencias en UrbanPulse.
 * Conecta con los endpoints REST del backend protegidos mediante autenticación JWT y roles RBAC.
 */

import api from './api';

/**
 * Calcula la prioridad sugerida y su justificación para una incidencia según los factores de vía y seguridad.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} factors - Factores de riesgo para el cálculo asistido.
 * @param {boolean} factors.streetCut - Indica si la incidencia provoca corte de calle o calzada.
 * @param {'NONE'|'LOW'|'HIGH'} factors.safetyRisk - Nivel de riesgo para la seguridad ciudadana.
 * @returns {Promise<{suggestedPriority: string, priorityReason: string, streetCut: boolean, safetyRisk: string}>} Respuesta con la prioridad sugerida y justificación calculada.
 * @throws {import('axios').AxiosError} Si ocurre un error en la petición o el usuario carece de permisos.
 */
export const calculatePriority = async (incidentId, { streetCut, safetyRisk }) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/priority/calculate`, {
    streetCut,
    safetyRisk,
  });
  return response.data;
};

/**
 * Fija o actualiza la prioridad definitiva y su justificación textual para una incidencia.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Datos de la prioridad a actualizar.
 * @param {string} data.priority - Nivel de prioridad asignado ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL').
 * @param {string} data.priorityReason - Justificación explicativa de la prioridad establecida.
 * @returns {Promise<Object>} Incidencia actualizada devuelta por el servidor.
 * @throws {import('axios').AxiosError} Si los datos son inválidos o falta autorización.
 */
export const updatePriority = async (incidentId, { priority, priorityReason }) => {
  const response = await api.put(`/api/v2/incidents/${incidentId}/priority`, {
    priority,
    priorityReason,
  });
  return response.data;
};

/**
 * Valida formalmente una incidencia en estado de reporte inicial (REPORTED -> VALIDATED).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} [data={}] - Datos opcionales de la validación.
 * @param {string} [data.reason] - Observaciones o justificación de la validación.
 * @returns {Promise<Object>} Incidencia validada con estado VALIDATED.
 * @throws {import('axios').AxiosError} Si la transición es ilegal o carece de permisos de operador/admin.
 */
export const validateIncident = async (incidentId, data = {}) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/validate`, data);
  return response.data;
};

/**
 * Rechaza y descarta una incidencia del circuito operativo con motivo justificado (mínimo 5 caracteres).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Motivo de descarte.
 * @param {string} data.reason - Justificación obligatoria del rechazo.
 * @returns {Promise<Object>} Incidencia con estado REJECTED y asignaciones inactivadas.
 * @throws {import('axios').AxiosError} Si el motivo no cumple la longitud mínima o la transición no es válida.
 */
export const rejectIncident = async (incidentId, { reason }) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/reject`, { reason });
  return response.data;
};

/**
 * Reevalúa una incidencia rechazada devolviéndola al circuito en estado REPORTED o VALIDATED.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Parámetros de la reevaluación.
 * @param {'REPORTED'|'VALIDATED'} data.targetStatus - Estado destino autorizado.
 * @param {string} data.reason - Motivo obligatorio de la reevaluación (mínimo 5 caracteres).
 * @returns {Promise<Object>} Incidencia reactivada en el estado indicado.
 * @throws {import('axios').AxiosError} Si el estado destino no está permitido o el motivo es insuficiente.
 */
export const reevaluateIncident = async (incidentId, { targetStatus, reason }) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/reevaluate`, {
    targetStatus,
    reason,
  });
  return response.data;
};

/**
 * Inicia la intervención técnica sobre el terreno cambiando el estado a IN_PROGRESS.
 * Requiere que el usuario autenticado sea un técnico asignado activamente a la incidencia.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @returns {Promise<Object>} Incidencia actualizada a estado IN_PROGRESS.
 * @throws {import('axios').AxiosError} Si el técnico no está asignado o el estado previo no es ASSIGNED o REOPENED.
 */
export const startWork = async (incidentId) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/start-work`);
  return response.data;
};

/**
 * Declara resuelta una incidencia tras finalizar las labores de campo (estado RESOLVED).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} [data={}] - Memoria de actuaciones.
 * @param {string} [data.resolutionNotes] - Resumen descriptivo de la intervención efectuada.
 * @returns {Promise<Object>} Incidencia con estado RESOLVED.
 * @throws {import('axios').AxiosError} Si la incidencia requiere justificación y no se aporta.
 */
export const resolveIncident = async (incidentId, data = {}) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/resolve`, data);
  return response.data;
};

/**
 * Cierra administrativa y definitivamente el expediente de una incidencia resuelta (estado CLOSED).
 * Inactiva automáticamente todas las asignaciones técnicas y departamentales activas.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @returns {Promise<Object>} Incidencia con estado CLOSED.
 * @throws {import('axios').AxiosError} Si la incidencia no se encuentra en estado RESOLVED o falta autorización.
 */
export const closeIncident = async (incidentId) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/close`);
  return response.data;
};

/**
 * Reabre una incidencia resuelta para actuaciones complementarias con motivo justificado (estado REOPENED).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Motivo de reapertura.
 * @param {string} data.reason - Justificación obligatoria de reapertura (mínimo 5 caracteres).
 * @returns {Promise<Object>} Incidencia reactivada en estado REOPENED.
 * @throws {import('axios').AxiosError} Si la incidencia no está en RESOLVED o el motivo es inválido.
 */
export const reopenIncident = async (incidentId, { reason }) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/reopen`, { reason });
  return response.data;
};

/**
 * Asigna uno o varios departamentos y técnicos específicos a una incidencia urbana.
 * Si la incidencia estaba en VALIDATED, transiciona automáticamente a ASSIGNED.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Identificadores de departamentos y técnicos a asignar.
 * @param {number[]} data.departmentIds - Lista de IDs de departamentos a vincular.
 * @param {number[]} [data.technicianIds=[]] - Lista opcional de IDs de técnicos a vincular.
 * @returns {Promise<Array<Object>>} Lista de asignaciones activas de la incidencia.
 * @throws {import('axios').AxiosError} Si los técnicos no pertenecen a los departamentos o la incidencia no permite asignación.
 */
export const assignDepartmentsAndTechnicians = async (
  incidentId,
  { departmentIds, technicianIds = [] }
) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/assignments`, {
    departmentIds,
    technicianIds,
  });
  return response.data;
};

/**
 * Recupera el listado de asignaciones activas de una incidencia.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @returns {Promise<Array<Object>>} Lista de asignaciones activas de la incidencia.
 * @throws {import('axios').AxiosError} Si la incidencia no existe o falla la consulta.
 */
export const getIncidentAssignments = async (incidentId) => {
  const response = await api.get(`/api/v2/incidents/${incidentId}/assignments`);
  return response.data;
};

/**
 * Desactiva lógicamente asignaciones departamentales o técnicas específicas de una incidencia.
 * Si no restan departamentos activos, revierte automáticamente el estado de la incidencia a VALIDATED.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Identificadores de departamentos y técnicos a desactivar.
 * @param {number[]} [data.departmentIds=[]] - IDs de departamentos a desactivar.
 * @param {number[]} [data.technicianIds=[]] - IDs de técnicos a desactivar.
 * @returns {Promise<Array<Object>>} Lista actualizada de asignaciones activas de la incidencia.
 * @throws {import('axios').AxiosError} Si la incidencia no existe o falla la actualización.
 */
export const deactivateAssignments = async (
  incidentId,
  { departmentIds = [], technicianIds = [] }
) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/assignments/deactivate`, {
    departmentIds,
    technicianIds,
  });
  return response.data;
};

/**
 * Obtiene el listado cronológico de comentarios y notas de una incidencia autorizados para el usuario actual.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @returns {Promise<Array<{id: number, incidentId: number, authorId: number, authorName: string, authorRole: string, comment: string, isInternal: boolean, createdAt: string, updatedAt: string|null}>>} Lista de comentarios.
 * @throws {import('axios').AxiosError} Si el usuario no tiene permisos para consultar la incidencia.
 */
export const getIncidentComments = async (incidentId) => {
  const response = await api.get(`/api/v2/incidents/${incidentId}/comments`);
  return response.data;
};

/**
 * Publica un nuevo comentario o nota interna en el triaje de la incidencia.
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {Object} data - Datos del comentario.
 * @param {string} data.comment - Texto del comentario (mínimo 1 carácter).
 * @param {boolean} [data.isInternal=false] - Indica si es una nota interna restringida a personal municipal.
 * @returns {Promise<Object>} Comentario recién creado.
 * @throws {import('axios').AxiosError} Si la incidencia está en estado terminal o los datos son inválidos.
 */
export const addIncidentComment = async (incidentId, { comment, isInternal = false }) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/comments`, {
    comment,
    isInternal,
  });
  return response.data;
};

/**
 * Modifica el contenido de un comentario existente (autor original o administrador).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia.
 * @param {number|string} commentId - Identificador único del comentario a editar.
 * @param {Object} data - Datos de actualización.
 * @param {string} data.comment - Nuevo texto del comentario.
 * @returns {Promise<Object>} Comentario actualizado con sellado de fecha updatedAt.
 * @throws {import('axios').AxiosError} Si el usuario no es el autor ni administrador o el comentario no existe.
 */
export const updateIncidentComment = async (incidentId, commentId, { comment }) => {
  const response = await api.put(`/api/v2/incidents/${incidentId}/comments/${commentId}`, {
    comment,
  });
  return response.data;
};

/**
 * Recupera el catálogo oficial de departamentos municipales ordenados alfabéticamente.
 *
 * @returns {Promise<Array<{id: number, name: string, description: string}>>} Lista de departamentos municipales.
 * @throws {import('axios').AxiosError} Si ocurre un error de red o falta autenticación.
 */
export const getDepartments = async () => {
  const response = await api.get('/api/v2/departments');
  return response.data;
};

/**
 * Recupera la lista de técnicos asignados a un departamento municipal específico.
 *
 * @param {number|string} departmentId - Identificador único del departamento.
 * @returns {Promise<Array<{id: number, fullName: string, email: string, role: string, departmentId: number, departmentName: string}>>} Lista de técnicos del departamento.
 * @throws {import('axios').AxiosError} Si el departamento no existe (404) o el usuario no tiene rol OPERATOR/ADMIN.
 */
export const getTechniciansByDepartment = async (departmentId) => {
  const response = await api.get(`/api/v2/departments/${departmentId}/technicians`);
  return response.data;
};

/**
 * Confirma y marca una incidencia como duplicada de otra rechazándola automáticamente (HU20).
 *
 * @param {number|string} incidentId - Identificador único de la incidencia duplicada.
 * @returns {Promise<Object>} Incidencia con estado actualizado a REJECTED.
 * @throws {import('axios').AxiosError} Si la petición falla o el usuario carece de permisos de operador/admin.
 */
export const markAsDuplicate = async (incidentId) => {
  const response = await api.post(`/api/v2/incidents/${incidentId}/mark-duplicate`);
  return response.data;
};

const triageService = {
  markAsDuplicate,
  calculatePriority,
  updatePriority,
  validateIncident,
  rejectIncident,
  reevaluateIncident,
  startWork,
  resolveIncident,
  closeIncident,
  reopenIncident,
  getIncidentAssignments,
  assignDepartmentsAndTechnicians,
  deactivateAssignments,
  getIncidentComments,
  addIncidentComment,
  updateIncidentComment,
  getDepartments,
  getTechniciansByDepartment,
};

export default triageService;
