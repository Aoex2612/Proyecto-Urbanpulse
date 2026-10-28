/**
 * @fileoverview Modulo utilitario para la captura y gestion de geolocalizacion
 * mediante la API nativa W3C navigator.geolocation en UrbanPulse.
 * Proporciona validacion de compatibilidad y contextos seguros, obtencion
 * de coordenadas de alta precision redondeadas a 6 decimales y mapeo de errores en espanol.
 */

/**
 * Opciones de configuracion predeterminadas para navigator.geolocation.getCurrentPosition.
 * Configurado con alta precision por GPS/senales de red, tiempo limite estricto de 10 segundos
 * y sin reutilizacion de posiciones en cache obsoletas.
 *
 * @type {Readonly<PositionOptions>}
 */
export const DEFAULT_GEOLOCATION_OPTIONS = Object.freeze({
  enableHighAccuracy: true,
  timeout: 10000,
  maximumAge: 0,
});

/**
 * Catalogo de mensajes de error de geolocalizacion en espanol accesibles y descriptivos.
 * Cubre los codigos nativos W3C (1 = PERMISSION_DENIED, 2 = POSITION_UNAVAILABLE, 3 = TIMEOUT)
 * y escenarios de entorno no compatible o fallos inesperados.
 *
 * @type {Readonly<Record<number|string, string>>}
 */
export const GEOLOCATION_ERROR_MESSAGES = Object.freeze({
  1: 'Permiso de geolocalización denegado. Puedes mantener el punto marcado en el mapa.',
  2: 'No se pudo determinar la posición actual. Comprueba la señal GPS o de red.',
  3: 'Se agotó el tiempo de espera para obtener la ubicación actual.',
  PERMISSION_DENIED: 'Permiso de geolocalización denegado. Puedes mantener el punto marcado en el mapa.',
  POSITION_UNAVAILABLE: 'No se pudo determinar la posición actual. Comprueba la señal GPS o de red.',
  TIMEOUT: 'Se agotó el tiempo de espera para obtener la ubicación actual.',
  UNSUPPORTED: 'Geolocalización no disponible en este navegador o contexto no seguro.',
  DEFAULT: 'Ocurrió un error inesperado al obtener la ubicación actual.',
});

/**
 * Comprueba de forma segura si la API nativa de geolocalizacion y un contexto de ejecucion seguro
 * (HTTPS o localhost) se encuentran disponibles en el entorno de navegacion actual.
 *
 * @returns {boolean} Verdadero si navigator.geolocation y window.isSecureContext estan presentes y habilitados.
 */
export const isGeolocationSupported = () => {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return false;
  }
  return Boolean(navigator?.geolocation && window?.isSecureContext);
};

/**
 * Redondea un valor numerico de coordenada a un numero determinado de decimales.
 *
 * @param {number|string} value - Coordenada geografica a redondear.
 * @param {number} [decimals=6] - Cantidad de cifras decimales requeridas (por defecto 6).
 * @returns {number} Valor numerico redondeado o NaN si no representa un numero valido.
 */
export const roundCoordinate = (value, decimals = 6) => {
  const num = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(num)) {
    return NaN;
  }
  return Number(num.toFixed(decimals));
};

/**
 * Traduce un codigo de error de geolocalizacion o instancia nativa GeolocationPositionError
 * a un mensaje comprensible y contextualizado en espanol para el ciudadano.
 *
 * @param {GeolocationPositionError|{code?: number}|number|null|undefined} error - Error o codigo devuelto por la API.
 * @returns {string} Mensaje explicativo en espanol.
 */
export const getGeolocationErrorMessage = (error) => {
  const code = typeof error === 'number' ? error : error?.code;

  switch (code) {
    case 1:
      return GEOLOCATION_ERROR_MESSAGES[1];
    case 2:
      return GEOLOCATION_ERROR_MESSAGES[2];
    case 3:
      return GEOLOCATION_ERROR_MESSAGES[3];
    default:
      return GEOLOCATION_ERROR_MESSAGES.DEFAULT;
  }
};

/**
 * Solicita las coordenadas geograficas actuales del dispositivo mediante una Promesa
 * que envuelve navigator.geolocation.getCurrentPosition con opciones predeterminadas de alta precision.
 * Redondea latitud y longitud a 6 decimales segun los estandares de precision submetrica de UrbanPulse.
 *
 * @param {PositionOptions} [options={}] - Opciones de geolocalizacion para sobrescribir las predeterminadas.
 * @returns {Promise<{latitude: number, longitude: number, accuracy: number}>} Promesa resuelta con coordenadas y precision.
 * @throws {Error} Error descriptivo en espanol con propiedad 'code' y referencia 'originalError'.
 */
export const getCurrentCoordinates = (options = {}) => {
  const mergedOptions = {
    ...DEFAULT_GEOLOCATION_OPTIONS,
    ...options,
  };

  return new Promise((resolve, reject) => {
    if (!isGeolocationSupported()) {
      const unsupportedError = new Error(GEOLOCATION_ERROR_MESSAGES.UNSUPPORTED);
      unsupportedError.code = 0;
      return reject(unsupportedError);
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (!position || !position.coords) {
          const malformedError = new Error(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
          malformedError.code = 0;
          return reject(malformedError);
        }

        const rawLat = position.coords.latitude;
        const rawLng = position.coords.longitude;
        const rawAccuracy = position.coords.accuracy;

        const latitude = Number(rawLat.toFixed(6));
        const longitude = Number(rawLng.toFixed(6));
        const accuracy = typeof rawAccuracy === 'number' ? rawAccuracy : 0;

        resolve({
          latitude,
          longitude,
          accuracy,
        });
      },
      (geoError) => {
        const message = getGeolocationErrorMessage(geoError);
        const error = new Error(message);
        error.code = geoError?.code ?? 0;
        error.originalError = geoError;
        reject(error);
      },
      mergedOptions
    );
  });
};

export default {
  DEFAULT_GEOLOCATION_OPTIONS,
  GEOLOCATION_ERROR_MESSAGES,
  isGeolocationSupported,
  roundCoordinate,
  getGeolocationErrorMessage,
  getCurrentCoordinates,
};
