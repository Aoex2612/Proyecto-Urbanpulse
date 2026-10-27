/**
 * @fileoverview Funciones utilitarias para el formateo y manipulacion de fechas en la interfaz de UrbanPulse.
 */

/**
 * Constantes de configuracion para los filtros temporales de incidencias.
 */
export const DATE_FILTER_PRESETS = Object.freeze({
  ALL: 'all',
  TODAY: 'today',
  WEEK: 'week',
  MONTH: 'month',
  CUSTOM: 'custom',
});

/**
 * Formatea una marca temporal ISO 8601 o fecha a un formato localizado en espanol legible para el usuario.
 *
 * @param {string|Date} dateValue - Marca temporal ISO o instancia Date a formatear.
 * @returns {string} Fecha y hora formateadas (ej. "25/10/2026, 15:30") o texto alternativo si es invalida.
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

/**
 * Evalua si una fecha se encuentra dentro de un rango temporal delimitado por fechas de inicio y fin.
 *
 * @param {string|Date} targetDate - Fecha objetivo a evaluar.
 * @param {string|Date|null} [startDate=null] - Fecha inicial inclusiva (a partir de las 00:00:00.000).
 * @param {string|Date|null} [endDate=null] - Fecha final inclusiva (hasta las 23:59:59.999).
 * @returns {boolean} True si la fecha se encuentra dentro de los limites establecidos.
 */
export const isWithinDateRange = (targetDate, startDate = null, endDate = null) => {
  if (!targetDate) return false;

  const target = new Date(targetDate);
  if (Number.isNaN(target.getTime())) return false;

  if (startDate) {
    const start = new Date(startDate);
    if (!Number.isNaN(start.getTime())) {
      start.setHours(0, 0, 0, 0);
      if (target.getTime() < start.getTime()) return false;
    }
  }

  if (endDate) {
    const end = new Date(endDate);
    if (!Number.isNaN(end.getTime())) {
      end.setHours(23, 59, 59, 999);
      if (target.getTime() > end.getTime()) return false;
    }
  }

  return true;
};

/**
 * Filtra una coleccion de incidencias segun el criterio temporal configurado.
 *
 * @param {Array<Object>} incidents - Lista de incidencias a filtrar.
 * @param {Object} filterConfig - Parametros del filtro.
 * @param {string} [filterConfig.preset='week'] - Clave del preset ('all', 'today', 'week', 'month', 'custom').
 * @param {string} [filterConfig.startDate=''] - Cadena de fecha 'YYYY-MM-DD' inicial (para preset 'custom').
 * @param {string} [filterConfig.endDate=''] - Cadena de fecha 'YYYY-MM-DD' final (para preset 'custom').
 * @param {Date} [referenceDate=new Date()] - Fecha base de referencia para el calculo relativo de dias.
 * @returns {Array<Object>} Lista filtrada de incidencias que cumplen con la condicion temporal.
 */
export const filterIncidentsByDate = (
  incidents = [],
  filterConfig = {},
  referenceDate = new Date()
) => {
  if (!Array.isArray(incidents)) return [];

  const { preset = DATE_FILTER_PRESETS.WEEK, startDate = '', endDate = '' } = filterConfig;

  if (preset === DATE_FILTER_PRESETS.ALL) {
    return incidents;
  }

  const now = referenceDate instanceof Date ? referenceDate : new Date(referenceDate);

  if (preset === DATE_FILTER_PRESETS.TODAY) {
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);

    return incidents.filter((incident) => {
      if (!incident?.createdAt) return false;
      const created = new Date(incident.createdAt);
      return !Number.isNaN(created.getTime()) && created.getTime() >= startOfToday.getTime();
    });
  }

  if (preset === DATE_FILTER_PRESETS.WEEK) {
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    return incidents.filter((incident) => {
      if (!incident?.createdAt) return false;
      const created = new Date(incident.createdAt);
      return !Number.isNaN(created.getTime()) && created.getTime() >= sevenDaysAgo.getTime();
    });
  }

  if (preset === DATE_FILTER_PRESETS.MONTH) {
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    return incidents.filter((incident) => {
      if (!incident?.createdAt) return false;
      const created = new Date(incident.createdAt);
      return !Number.isNaN(created.getTime()) && created.getTime() >= thirtyDaysAgo.getTime();
    });
  }

  if (preset === DATE_FILTER_PRESETS.CUSTOM) {
    if (!startDate && !endDate) {
      return incidents;
    }
    return incidents.filter((incident) => {
      return isWithinDateRange(incident?.createdAt, startDate, endDate);
    });
  }

  return incidents;
};
