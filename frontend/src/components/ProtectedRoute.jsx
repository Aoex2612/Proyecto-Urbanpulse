/**
 * @fileoverview Componente de control de acceso para rutas protegidas en UrbanPulse.
 * Verifica la existencia de un token de autenticación activo mediante `authService.isAuthenticated()`.
 * Redirige a la pantalla de inicio de sesión (`/login`) si el usuario no está autenticado.
 */

import { Navigate } from 'react-router-dom';
import authService from '../services/authService';

/**
 * Componente envoltorio de ruta protegida.
 *
 * @param {object} props - Propiedades del componente.
 * @param {import('react').ReactNode} props.children - Elementos hijos que se renderizan si el usuario está autenticado.
 * @returns {import('react').JSX.Element} El contenido hijo o el componente de redirección a `/login`.
 */
export default function ProtectedRoute({ children }) {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
