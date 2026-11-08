/**
 * @fileoverview Barra de navegación principal y transversal para UrbanPulse (RF-19).
 * Proporciona acceso a las secciones clave de la plataforma según el rol del usuario autenticado (RBAC).
 * Incluye enlace a "Analítica" (/analytics) exclusivamente para ANALYST, ADMIN y OPERATOR.
 */

import { Link, useNavigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';
import './Navbar.css';

/**
 * Componente Navbar reutilizable con control de acceso RBAC.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Function} [props.onNavClick] - Callback opcional al hacer clic en un enlace de navegación.
 * @returns {import('react').JSX.Element} Elemento JSX de la barra de navegación.
 */
export default function Navbar({ onNavClick }) {
  const navigate = useNavigate();
  const location = useLocation();
  const user = authService.getUser();

  const hasOperatorAccess = authService.hasAnyRole(['OPERATOR', 'ADMIN', 'TECHNICIAN']);
  const hasAnalyticsAccess = authService.hasAnyRole(['ANALYST', 'ADMIN', 'OPERATOR']);
  const isTechnician = user?.role === 'TECHNICIAN';
  const operatorPanelLabel = isTechnician ? 'Mis Asignaciones' : 'Panel de Operador';

  /**
   * Manejador para el cierre de sesión del usuario.
   */
  const handleLogout = () => {
    authService.logout();
    navigate('/login', { replace: true });
  };

  /**
   * Ejecuta el callback opcional al navegar.
   */
  const handleLinkClick = () => {
    if (onNavClick) {
      onNavClick();
    }
  };

  return (
    <nav className="navbar" aria-label="Navegación principal">
      <div className="navbar-container">
        <div className="navbar-brand">
          <Link to="/" className="navbar-logo" onClick={handleLinkClick}>
            <span className="navbar-logo-text">UrbanPulse</span>
          </Link>
          {user?.role && (
            <span className="navbar-role-badge" title={`Rol de usuario: ${user.role}`}>
              {user.role}
            </span>
          )}
        </div>

        <div className="navbar-links">
          <Link
            to="/"
            className={`navbar-link ${location.pathname === '/' ? 'active' : ''}`}
            onClick={handleLinkClick}
          >
            Inicio
          </Link>
          <Link
            to="/map"
            className={`navbar-link ${location.pathname === '/map' ? 'active' : ''}`}
            onClick={handleLinkClick}
          >
            Mapa
          </Link>
          <Link
            to="/my-incidents"
            className={`navbar-link ${location.pathname === '/my-incidents' ? 'active' : ''}`}
            onClick={handleLinkClick}
          >
            Mis Incidencias
          </Link>

          {hasOperatorAccess && (
            <Link
              to="/operator-panel"
              className={`navbar-link operator-link ${
                location.pathname === '/operator-panel' ? 'active' : ''
              }`}
              onClick={handleLinkClick}
            >
              {operatorPanelLabel}
            </Link>
          )}

          {hasAnalyticsAccess && (
            <Link
              to="/analytics"
              className={`navbar-link analytics-link ${
                location.pathname === '/analytics' ? 'active' : ''
              }`}
              onClick={handleLinkClick}
            >
              Analítica
            </Link>
          )}

          <button
            type="button"
            className="navbar-logout-btn"
            onClick={handleLogout}
            aria-label="Cerrar sesión"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </nav>
  );
}
