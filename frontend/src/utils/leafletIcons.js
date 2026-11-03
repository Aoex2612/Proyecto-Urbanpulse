/**
 * @fileoverview Configuración y resolución de iconos de marcadores de Leaflet para Vite.
 * Corrige la resolución de rutas de assets de marcadores predeterminados de Leaflet
 * en empaquetadores modernos como Vite y provee iconos estándar y personalizados.
 */

import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

/**
 * Configura los iconos estándar de Leaflet sobreescribiendo las rutas por defecto
 * para que resuelvan adecuadamente con el empaquetado de assets de Vite.
 *
 * @returns {void}
 */
export const setupLeafletIcons = () => {
  // Elimina el método interno de resolución por defecto que asume rutas relativas estáticas
  delete L.Icon.Default.prototype._getIconUrl;

  // Asigna las rutas importadas gestionadas por Vite
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
  });
};

// Se ejecuta la configuración inicial al importar la utilidad
setupLeafletIcons();

/**
 * Icono por defecto de Leaflet configurado con los tamaños y anclajes estándar.
 *
 * @type {L.Icon}
 */
export const defaultIcon = new L.Icon({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

/**
 * Genera un icono de marcador Leaflet con un color personalizado en formato SVG,
 * útil para distinguir estados o categorías de incidencias en el mapa.
 *
 * @param {string} color - Código de color hexadecimal o CSS (ej. '#E53E3E', '#3182CE').
 * @returns {L.DivIcon} Objeto DivIcon de Leaflet listo para su uso en marcadores cartográficos.
 */
export const createColoredMarkerIcon = (color = '#3182CE') => {
  const svgTemplate = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 36" width="30" height="42">
      <path d="M12 0C5.373 0 0 5.373 0 12c0 9 12 24 12 24s12-15 12-24c0-6.627-5.373-12-12-12z" fill="${color}" stroke="#FFFFFF" stroke-width="1.5"/>
      <circle cx="12" cy="12" r="5" fill="#FFFFFF"/>
    </svg>
  `;

  return L.divIcon({
    className: 'custom-colored-marker',
    html: svgTemplate,
    iconSize: [30, 42],
    iconAnchor: [15, 42],
    popupAnchor: [0, -38],
  });
};

export default {
  setupLeafletIcons,
  defaultIcon,
  createColoredMarkerIcon,
};
