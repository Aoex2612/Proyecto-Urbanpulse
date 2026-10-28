import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import geolocationUtils, {
  isGeolocationSupported,
  roundCoordinate,
  getGeolocationErrorMessage,
  getCurrentCoordinates,
  DEFAULT_GEOLOCATION_OPTIONS,
  GEOLOCATION_ERROR_MESSAGES,
} from './geolocationUtils';

describe('geolocationUtils', () => {
  const originalGeolocation = navigator.geolocation;
  const originalIsSecureContext = window.isSecureContext;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'geolocation', {
      value: originalGeolocation,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(window, 'isSecureContext', {
      value: originalIsSecureContext,
      configurable: true,
      writable: true,
    });
  });

  describe('Constantes exportadas y exportacion por defecto', () => {
    it('DEFAULT_GEOLOCATION_OPTIONS contiene la configuracion exigida por HU14.4', () => {
      expect(DEFAULT_GEOLOCATION_OPTIONS).toEqual({
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      });
      expect(Object.isFrozen(DEFAULT_GEOLOCATION_OPTIONS)).toBe(true);
    });

    it('GEOLOCATION_ERROR_MESSAGES contiene los mensajes explicativos en espanol', () => {
      expect(GEOLOCATION_ERROR_MESSAGES[1]).toContain('Permiso de geolocalización denegado');
      expect(GEOLOCATION_ERROR_MESSAGES[2]).toContain('No se pudo determinar la posición actual');
      expect(GEOLOCATION_ERROR_MESSAGES[3]).toContain('tiempo de espera');
      expect(GEOLOCATION_ERROR_MESSAGES.UNSUPPORTED).toContain('no disponible');
      expect(GEOLOCATION_ERROR_MESSAGES.DEFAULT).toContain('error inesperado');
    });

    it('exporta todas las funciones y constantes en el objeto default', () => {
      expect(geolocationUtils.isGeolocationSupported).toBe(isGeolocationSupported);
      expect(geolocationUtils.getCurrentCoordinates).toBe(getCurrentCoordinates);
      expect(geolocationUtils.roundCoordinate).toBe(roundCoordinate);
      expect(geolocationUtils.getGeolocationErrorMessage).toBe(getGeolocationErrorMessage);
      expect(geolocationUtils.DEFAULT_GEOLOCATION_OPTIONS).toBe(DEFAULT_GEOLOCATION_OPTIONS);
      expect(geolocationUtils.GEOLOCATION_ERROR_MESSAGES).toBe(GEOLOCATION_ERROR_MESSAGES);
    });
  });

  describe('isGeolocationSupported', () => {
    it('retorna true cuando navigator.geolocation y window.isSecureContext estan presentes y habilitados', () => {
      Object.defineProperty(window, 'isSecureContext', {
        value: true,
        configurable: true,
        writable: true,
      });
      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: vi.fn() },
        configurable: true,
        writable: true,
      });

      expect(isGeolocationSupported()).toBe(true);
    });

    it('retorna false cuando navigator.geolocation no esta disponible en el entorno', () => {
      Object.defineProperty(window, 'isSecureContext', {
        value: true,
        configurable: true,
        writable: true,
      });
      Object.defineProperty(navigator, 'geolocation', {
        value: undefined,
        configurable: true,
        writable: true,
      });

      expect(isGeolocationSupported()).toBe(false);
    });

    it('retorna false cuando el contexto no es seguro (window.isSecureContext es false)', () => {
      Object.defineProperty(window, 'isSecureContext', {
        value: false,
        configurable: true,
        writable: true,
      });
      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: vi.fn() },
        configurable: true,
        writable: true,
      });

      expect(isGeolocationSupported()).toBe(false);
    });

    it('retorna false cuando window.isSecureContext no esta definido (undefined)', () => {
      Object.defineProperty(window, 'isSecureContext', {
        value: undefined,
        configurable: true,
        writable: true,
      });
      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: vi.fn() },
        configurable: true,
        writable: true,
      });

      expect(isGeolocationSupported()).toBe(false);
    });
  });

  describe('roundCoordinate', () => {
    it('redondea coordenadas decimales a 6 cifras por defecto', () => {
      expect(roundCoordinate(36.7213456789)).toBe(36.721346);
      expect(roundCoordinate(-4.4214567891)).toBe(-4.421457);
    });

    it('mantiene valores numericos que ya cuentan con menos de 6 decimales', () => {
      expect(roundCoordinate(36.72)).toBe(36.72);
      expect(roundCoordinate(-4.4214)).toBe(-4.4214);
    });

    it('acepta representaciones numericas en cadena de texto', () => {
      expect(roundCoordinate('36.721345111')).toBe(36.721345);
    });

    it('permite configurar un numero personalizado de decimales', () => {
      expect(roundCoordinate(36.7213456, 2)).toBe(36.72);
      expect(roundCoordinate(36.7213456, 4)).toBe(36.7213);
    });

    it('retorna NaN ante valores no numericos o invalidos', () => {
      expect(Number.isNaN(roundCoordinate('invalido'))).toBe(true);
      expect(Number.isNaN(roundCoordinate(null))).toBe(false); // Number(null) es 0
      expect(Number.isNaN(roundCoordinate(undefined))).toBe(true);
    });
  });

  describe('getGeolocationErrorMessage', () => {
    it('mapea correctamente el codigo 1 (PERMISSION_DENIED)', () => {
      const msgFromNumber = getGeolocationErrorMessage(1);
      const msgFromObj = getGeolocationErrorMessage({ code: 1, message: 'User denied' });

      expect(msgFromNumber).toBe(GEOLOCATION_ERROR_MESSAGES[1]);
      expect(msgFromObj).toBe(GEOLOCATION_ERROR_MESSAGES[1]);
      expect(msgFromNumber).toContain('Permiso de geolocalización denegado');
    });

    it('mapea correctamente el codigo 2 (POSITION_UNAVAILABLE)', () => {
      const msgFromNumber = getGeolocationErrorMessage(2);
      const msgFromObj = getGeolocationErrorMessage({ code: 2, message: 'Position unavailable' });

      expect(msgFromNumber).toBe(GEOLOCATION_ERROR_MESSAGES[2]);
      expect(msgFromObj).toBe(GEOLOCATION_ERROR_MESSAGES[2]);
      expect(msgFromNumber).toContain('No se pudo determinar la posición actual');
    });

    it('mapea correctamente el codigo 3 (TIMEOUT)', () => {
      const msgFromNumber = getGeolocationErrorMessage(3);
      const msgFromObj = getGeolocationErrorMessage({ code: 3, message: 'Timeout' });

      expect(msgFromNumber).toBe(GEOLOCATION_ERROR_MESSAGES[3]);
      expect(msgFromObj).toBe(GEOLOCATION_ERROR_MESSAGES[3]);
      expect(msgFromNumber).toContain('tiempo de espera');
    });

    it('retorna el mensaje de error por defecto ante codigos desconocidos o valores nulos', () => {
      expect(getGeolocationErrorMessage(99)).toBe(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
      expect(getGeolocationErrorMessage({ code: 999 })).toBe(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
      expect(getGeolocationErrorMessage(null)).toBe(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
      expect(getGeolocationErrorMessage(undefined)).toBe(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
      expect(getGeolocationErrorMessage({})).toBe(GEOLOCATION_ERROR_MESSAGES.DEFAULT);
    });
  });

  describe('getCurrentCoordinates', () => {
    beforeEach(() => {
      Object.defineProperty(window, 'isSecureContext', {
        value: true,
        configurable: true,
        writable: true,
      });
    });

    it('resuelve correctamente las coordenadas redondeadas a 6 decimales y su precision', async () => {
      const mockPosition = {
        coords: {
          latitude: 36.721345678,
          longitude: -4.421400012,
          accuracy: 14.5,
        },
      };

      const mockGetCurrentPosition = vi.fn((success) => {
        success(mockPosition);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      const coords = await getCurrentCoordinates();

      expect(coords).toEqual({
        latitude: 36.721346,
        longitude: -4.4214,
        accuracy: 14.5,
      });

      expect(mockGetCurrentPosition).toHaveBeenCalledTimes(1);
      expect(mockGetCurrentPosition).toHaveBeenCalledWith(
        expect.any(Function),
        expect.any(Function),
        DEFAULT_GEOLOCATION_OPTIONS
      );
    });

    it('permite sobrescribir opciones de geolocalizacion (custom options)', async () => {
      const mockPosition = {
        coords: {
          latitude: 36.7213,
          longitude: -4.4214,
          accuracy: 10,
        },
      };

      const mockGetCurrentPosition = vi.fn((success) => {
        success(mockPosition);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await getCurrentCoordinates({ timeout: 5000, enableHighAccuracy: false });

      expect(mockGetCurrentPosition).toHaveBeenCalledWith(
        expect.any(Function),
        expect.any(Function),
        {
          enableHighAccuracy: false,
          timeout: 5000,
          maximumAge: 0,
        }
      );
    });

    it('asigna accuracy en 0 si no viene definido numericamnte en position.coords', async () => {
      const mockPosition = {
        coords: {
          latitude: 36.7213,
          longitude: -4.4214,
          accuracy: null,
        },
      };

      const mockGetCurrentPosition = vi.fn((success) => {
        success(mockPosition);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      const coords = await getCurrentCoordinates();
      expect(coords.accuracy).toBe(0);
    });

    it('rechaza de inmediato si la geolocalizacion no esta soportada o el contexto no es seguro', async () => {
      Object.defineProperty(window, 'isSecureContext', {
        value: false,
        configurable: true,
        writable: true,
      });

      const mockGetCurrentPosition = vi.fn();
      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toThrow(GEOLOCATION_ERROR_MESSAGES.UNSUPPORTED);
      expect(mockGetCurrentPosition).not.toHaveBeenCalled();
    });

    it('rechaza con mensaje y codigo adecuado ante error 1 (PERMISSION_DENIED)', async () => {
      const nativeError = {
        code: 1,
        message: 'User denied Geolocation',
      };

      const mockGetCurrentPosition = vi.fn((_success, errorCallback) => {
        errorCallback(nativeError);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toMatchObject({
        message: GEOLOCATION_ERROR_MESSAGES[1],
        code: 1,
        originalError: nativeError,
      });
    });

    it('rechaza con mensaje y codigo adecuado ante error 2 (POSITION_UNAVAILABLE)', async () => {
      const nativeError = {
        code: 2,
        message: 'Position unavailable',
      };

      const mockGetCurrentPosition = vi.fn((_success, errorCallback) => {
        errorCallback(nativeError);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toMatchObject({
        message: GEOLOCATION_ERROR_MESSAGES[2],
        code: 2,
        originalError: nativeError,
      });
    });

    it('rechaza con mensaje y codigo adecuado ante error 3 (TIMEOUT)', async () => {
      const nativeError = {
        code: 3,
        message: 'Timeout expired',
      };

      const mockGetCurrentPosition = vi.fn((_success, errorCallback) => {
        errorCallback(nativeError);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toMatchObject({
        message: GEOLOCATION_ERROR_MESSAGES[3],
        code: 3,
        originalError: nativeError,
      });
    });

    it('rechaza con mensaje por defecto si ocurre un error con codigo no estandar', async () => {
      const nativeError = {
        code: 99,
        message: 'Unknown error',
      };

      const mockGetCurrentPosition = vi.fn((_success, errorCallback) => {
        errorCallback(nativeError);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toMatchObject({
        message: GEOLOCATION_ERROR_MESSAGES.DEFAULT,
        code: 99,
        originalError: nativeError,
      });
    });

    it('rechaza con mensaje por defecto si position devuelta es nula o carece de coords', async () => {
      const mockGetCurrentPosition = vi.fn((success) => {
        success(null);
      });

      Object.defineProperty(navigator, 'geolocation', {
        value: { getCurrentPosition: mockGetCurrentPosition },
        configurable: true,
        writable: true,
      });

      await expect(getCurrentCoordinates()).rejects.toMatchObject({
        message: GEOLOCATION_ERROR_MESSAGES.DEFAULT,
        code: 0,
      });
    });
  });
});
