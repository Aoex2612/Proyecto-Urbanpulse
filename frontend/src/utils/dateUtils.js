/**
 * @fileoverview Funciones utilitarias para el formateo y manipulación de fechas en la interfaz de UrbanPulse.
 */

/**
 * Formatea una marca temporal ISO 8601 o fecha a un formato localizado en español legible para el usuario.
 *
 * @param {string|Date} dateValue - Marca temporal ISO o instancia Date a formatear.
 * @returns {string} Fecha y hora formateadas (ej. "25/10/2026, 15:30") o texto alternativo si es inválida.
 */
export const formatDateTime = (dateValue) => {
  if (!dateValue) {
    return 'Fecha no disponible';
  }
  try {
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
      return String(dateValue);
    }
    return new Intl.DateTimeFormat('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch {
    return String(dateValue);
  }
};
