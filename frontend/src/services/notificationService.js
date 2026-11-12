/**
 * @fileoverview Servicio de acceso a la API REST de notificaciones in-app de UrbanPulse.
 * Proporciona métodos para recuperar notificaciones del usuario, consultar el contador
 * de no leídas y marcar notificaciones individuales como leídas.
 */

import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Recupera la lista completa de notificaciones del usuario autenticado,
 * ordenadas cronológicamente de más reciente a más antigua.
 *
 * @returns {Promise<Array<{id: number, title: string, message: string, type: string, isRead: boolean, createdAt: string, incidentId: number}>>} Lista de notificaciones.
 * @throws {Error} Si la petición falla o el usuario no está autenticado.
 */
async function getNotifications() {
  const token = localStorage.getItem('token');
  const response = await axios.get(`${API_BASE}/api/v2/notifications`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

/**
 * Recupera el número de notificaciones no leídas del usuario autenticado.
 *
 * @returns {Promise<number>} Contador de notificaciones pendientes de lectura.
 * @throws {Error} Si la petición falla o el usuario no está autenticado.
 */
async function getUnreadCount() {
  const token = localStorage.getItem('token');
  const response = await axios.get(`${API_BASE}/api/v2/notifications/unread-count`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return typeof response.data === 'number' ? response.data : response.data?.count ?? 0;
}

/**
 * Marca una notificación individual como leída.
 *
 * @param {number|string} id Identificador único de la notificación a marcar como leída.
 * @returns {Promise<Object>} DTO actualizado de la notificación con isRead en true.
 * @throws {Error} Si la petición falla, el usuario no está autenticado o la notificación no le pertenece.
 */
async function markAsRead(id) {
  const token = localStorage.getItem('token');
  const response = await axios.patch(
    `${API_BASE}/api/v2/notifications/${id}/read`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
}

const notificationService = { getNotifications, getUnreadCount, markAsRead };
export default notificationService;
