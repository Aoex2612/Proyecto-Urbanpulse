/**
 * @fileoverview Configuración del cliente HTTP Axios e interceptores para UrbanPulse.
 * Gestiona la URL base, la inyección automática del token JWT en cabeceras de autorización
 * y la captura de errores 401 para expiración o revocación de sesión.
 */

import axios from 'axios';

/**
 * Instancia configurada de Axios para realizar peticiones HTTP a la API REST de UrbanPulse.
 * Utiliza la variable de entorno `VITE_API_URL` o el valor por defecto `http://localhost:8080`.
 *
 * @type {import('axios').AxiosInstance}
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Interceptor de peticiones HTTP salientes.
 * Inserta la cabecera `Authorization: Bearer <token>` si existe un token JWT en localStorage.
 */
api.interceptors.request.use(
  /**
   * Modifica la configuración de la petición antes de ser enviada.
   *
   * @param {import('axios').InternalAxiosRequestConfig} config - Configuración de la petición.
   * @returns {import('axios').InternalAxiosRequestConfig} Configuración actualizada con la cabecera de autorización si aplica.
   */
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  /**
   * Captura y propaga cualquier error producido durante la preparación de la petición.
   *
   * @param {unknown} error - Error de la petición.
   * @returns {Promise<never>} Promesa rechazada con el error.
   */
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Interceptor de respuestas HTTP entrantes.
 * Captura códigos de respuesta HTTP 401 (Unauthorized), purgando las credenciales de
 * localStorage y redirigiendo al usuario a la pantalla de inicio de sesión `/login`.
 * Garantiza que las respuestas HTTP 403 (Forbidden) u otros códigos de error no purguen
 * la sesión ni ejecuten logout, preservando las credenciales del usuario en localStorage.
 */
api.interceptors.response.use(
  /**
   * Retorna la respuesta HTTP si es exitosa.
   *
   * @param {import('axios').AxiosResponse} response - Respuesta exitosa del servidor.
   * @returns {import('axios').AxiosResponse} La respuesta sin modificaciones.
   */
  (response) => {
    return response;
  },
  /**
   * Maneja errores devueltos por el servidor o fallos de red.
   * Únicamente si el error es estrictamente HTTP 401 (no autorizado / token inválido o expirado)
   * limpia las credenciales de localStorage y redirige a la ruta `/login`.
   * En caso de HTTP 403 (Forbidden por permisos insuficientes), se preserva la sesión activa.
   *
   * @param {import('axios').AxiosError} error - Error de la respuesta HTTP.
   * @returns {Promise<never>} Promesa rechazada con el error de respuesta.
   */
  (error) => {
    // Solo purga la sesión y redirige si el estado HTTP es 401 (Unauthorized).
    // Si status === 403 (Forbidden), se mantiene intacta la sesión para permitir gestión de alertas visuales en UI.
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
