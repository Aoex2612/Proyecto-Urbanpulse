/**
 * @fileoverview Servicio de autenticación para UrbanPulse.
 * Proporciona métodos para registrar ciudadanos, iniciar sesión con JWT,
 * cerrar sesión y consultar el estado de autenticación en el cliente.
 */

import api from './api';

/**
 * Registra un nuevo ciudadano en la plataforma UrbanPulse.
 *
 * @param {string} fullName - Nombre completo del ciudadano (máximo 150 caracteres).
 * @param {string} email - Correo electrónico del ciudadano (máximo 150 caracteres).
 * @param {string} password - Contraseña en texto plano (mínimo 8 caracteres, al menos 1 letra y 1 dígito).
 * @returns {Promise<{message: string, email: string}>} Promesa con la respuesta del servidor tras registrarse (HTTP 201).
 * @throws {import('axios').AxiosError} Error si la validación falla (400) o el correo ya existe (409).
 */
export const register = async (fullName, email, password) => {
  const response = await api.post('/api/auth/register', {
    fullName,
    email,
    password,
  });
  return response.data;
};

/**
 * Autentica un usuario contra la API y persiste sus credenciales en localStorage.
 *
 * @param {string} email - Correo electrónico del usuario.
 * @param {string} password - Contraseña del usuario.
 * @returns {Promise<{token: string, tokenType: string, expiresIn: number, role: string, fullName: string}>} Datos de autenticación devueltos por el servidor.
 * @throws {import('axios').AxiosError} Error si las credenciales son inválidas (401) o los datos erróneos (400).
 */
export const login = async (email, password) => {
  const response = await api.post('/api/auth/login', {
    email,
    password,
  });

  const { token, fullName, role } = response.data;

  if (token) {
    localStorage.setItem('token', token);
  }

  if (fullName || role) {
    const user = {
      fullName: fullName || '',
      role: role || '',
      email,
    };
    localStorage.setItem('user', JSON.stringify(user));
  }

  return response.data;
};

/**
 * Cierra la sesión activa del usuario, purgando el token JWT y los datos del perfil de localStorage.
 *
 * @returns {void}
 */
export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

/**
 * Obtiene el token JWT actual almacenado en localStorage.
 *
 * @returns {string|null} El token JWT en cadena o null si no se encuentra registrado.
 */
export const getToken = () => {
  return localStorage.getItem('token');
};

/**
 * Comprueba si existe un token de autenticación activo en el cliente.
 *
 * @returns {boolean} Verdadero si hay un token persistido, falso en caso contrario.
 */
export const isAuthenticated = () => {
  return Boolean(getToken());
};

/**
 * Obtiene los datos del usuario autenticado guardados en localStorage.
 *
 * @returns {{fullName: string, role: string, email: string}|null} Objeto con la información del usuario o null si no existe.
 */
export const getUser = () => {
  const rawUser = localStorage.getItem('user');
  if (!rawUser) {
    return null;
  }
  try {
    return JSON.parse(rawUser);
  } catch {
    return null;
  }
};

/**
 * Obtiene el rol del usuario autenticado normalizado a mayúsculas.
 *
 * @returns {string|null} Rol del usuario en mayúsculas (ej. 'CITIZEN', 'OPERATOR', 'ADMIN') o null si no existe.
 */
export const getUserRole = () => {
  const user = getUser();
  if (!user || !user.role || typeof user.role !== 'string') {
    return null;
  }
  return user.role.trim().toUpperCase();
};

/**
 * Comprueba si el rol del usuario autenticado coincide con alguno de los roles permitidos.
 *
 * @param {string[]|string} allowedRoles - Lista de roles permitidos o rol individual requerido.
 * @returns {boolean} Verdadero si el usuario posee al menos uno de los roles permitidos, falso en caso contrario.
 */
export const hasAnyRole = (allowedRoles) => {
  const currentRole = getUserRole();
  if (!currentRole) {
    return false;
  }
  if (!allowedRoles) {
    return false;
  }
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
  if (roles.length === 0) {
    return false;
  }
  const normalizedAllowedRoles = roles.map((role) =>
    typeof role === 'string' ? role.trim().toUpperCase() : String(role).trim().toUpperCase()
  );
  return normalizedAllowedRoles.includes(currentRole);
};

const authService = {
  register,
  login,
  logout,
  getToken,
  isAuthenticated,
  getUser,
  getUserRole,
  hasAnyRole,
};

export default authService;
