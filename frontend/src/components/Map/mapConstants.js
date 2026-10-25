/**
 * @fileoverview Constantes y funciones de utilidad para los componentes cartográficos de UrbanPulse.
 * Define las categorías predefinidas de incidencias, valores por defecto de Leaflet
 * y formateadores de estado e iconos de marcadores.
 */

import { defaultIcon, createColoredMarkerIcon } from '../../utils/leafletIcons';

/**
 * Coordenadas de referencia geoespacial por defecto centradas en el núcleo urbano de Málaga.
 *
 * @type {[number, number]}
 */
export const DEFAULT_MAP_CENTER = [36.7213, -4.4214];

/**
 * Nivel de aumento (zoom) urbano por defecto para la visualización del callejero.
 *
 * @type {number}
 */
export const DEFAULT_MAP_ZOOM = 13;

/**
 * Catálogo predefinido y cerrado de categorías de incidencias urbanas.
 *
 * @type {string[]}
 */
export const INCIDENT_CATEGORIES = [
  'Alumbrado',
  'Vías Públicas',
  'Limpieza',
  'Parques y Jardines',
  'Mobiliario Urbano',
];

/**
 * Mapea el identificador técnico de estado de una incidencia a su etiqueta legible en español.
 *
 * @param {string} status - Código de estado técnico de la incidencia (REPORTED, IN_PROGRESS, etc.).
 * @returns {string} Etiqueta descriptiva para mostrar al usuario en la interfaz.
 */
export const getStatusLabel = (status) => {
  switch (status) {
    case 'REPORTED':
      return 'Reportada';
    case 'IN_PROGRESS':
      return 'En progreso';
    case 'RESOLVED':
      return 'Resuelta';
    case 'CLOSED':
      return 'Cerrada';
    case 'REJECTED':
      return 'Rechazada';
    default:
      return status || 'Desconocido';
  }
};

/**
 * Obtiene el icono de marcador Leaflet apropiado en función del estado operativo de la incidencia.
 *
 * @param {string} status - Estado operativo de la incidencia.
 * @returns {import('leaflet').Icon|import('leaflet').DivIcon} Icono de marcador Leaflet configurado.
 */
export const getIncidentMarkerIcon = (status) => {
  switch (status) {
    case 'REPORTED':
      return createColoredMarkerIcon('#f59e0b'); // Ámbar / Pendiente de revisión
    case 'IN_PROGRESS':
      return createColoredMarkerIcon('#3b82f6'); // Azul / En proceso de resolución
    case 'RESOLVED':
      return createColoredMarkerIcon('#10b981'); // Verde / Resuelta
    case 'CLOSED':
      return createColoredMarkerIcon('#6b7280'); // Gris / Concluida
    case 'REJECTED':
      return createColoredMarkerIcon('#ef4444'); // Rojo / Descartada
    default:
      return defaultIcon;
  }
};
