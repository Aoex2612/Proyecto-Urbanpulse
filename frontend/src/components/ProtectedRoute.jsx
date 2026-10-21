/**
 * @fileoverview Componente de control de acceso para rutas protegidas en UrbanPulse.
 * Verifica la existencia de un token de autenticación activo mediante `authService.isAuthenticated()`.
 * Redirige a la pantalla de inicio de sesión (`/login`) si el usuario no está autenticado.
 * Si se especifica la propiedad opcional `allowedRoles`, compone internamente el componente `RoleGuard`
 * para validar los permisos asociados al rol del usuario autenticado.
 */

import { Navigate } from 'react-router-dom';
import authService from '../services/authService';
import RoleGuard from './RoleGuard';

/**
 * Componente envoltorio de ruta protegida con soporte opcional de autorización RBAC.
 *
 * @param {object} props - Propiedades del componente.
 * @param {import('react').ReactNode} props.children - Elementos hijos que se renderizan si el usuario está autenticado y autorizado.
 * @param {string[]|string} [props.allowedRoles] - Roles autorizados opcionales para acceder al recurso protegido.
 * @returns {import('react').JSX.Element} El contenido hijo envuelto en `RoleGuard`, el contenido directo, o redirección a `/login`.
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles) {
    return <RoleGuard allowedRoles={allowedRoles}>{children}</RoleGuard>;
  }

  return children;
}
