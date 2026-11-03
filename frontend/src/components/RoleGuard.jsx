/**
 * @fileoverview Componente de control de acceso por roles (Role-Based Access Control) en UrbanPulse.
 * Evalúa si el usuario autenticado posee alguno de los roles requeridos especificados en `allowedRoles`.
 * Si el rol del usuario no coincide, redirige automáticamente a la vista principal (`/`) transmitiendo
 * el motivo de la denegación en el estado de navegación (`location.state`), garantizando que no se invoque
 * `logout()` ni se purgue la sesión activa del usuario.
 */

import { Navigate } from 'react-router-dom';
import authService from '../services/authService';

/**
 * Mensaje estándar transmitido al denegar el acceso a una ruta protegida por rol.
 * @type {string}
 */
const DEFAULT_ACCESS_DENIED_MESSAGE =
  'Acceso denegado: no dispones de permisos de operador o administrador';

/**
 * Componente guardián de autorización basado en roles.
 *
 * @param {object} props - Propiedades del componente.
 * @param {string[]|string} props.allowedRoles - Colección de roles permitidos o rol individual requerido.
 * @param {import('react').ReactNode} props.children - Elementos hijos que se renderizan si el rol coincide.
 * @param {string} [props.message] - Mensaje explicativo personalizado opcional ante acceso denegado.
 * @returns {import('react').JSX.Element} El contenido hijo autorizado o redirección a `/` con estado de acceso denegado.
 */
export default function RoleGuard({
  allowedRoles,
  children,
  message = DEFAULT_ACCESS_DENIED_MESSAGE,
}) {
  const isAuthorized = authService.hasAnyRole(allowedRoles);

  if (!isAuthorized) {
    return (
      <Navigate
        to="/"
        replace
        state={{
          accessDenied: true,
          message,
        }}
      />
    );
  }

  return children;
}
