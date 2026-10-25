/**
 * @fileoverview Vista principal / Dashboard (/) para ciudadanos autenticados en UrbanPulse.
 * Muestra el perfil del ciudadano, información de la plataforma, avisos de acceso denegado
 * provenientes de redirecciones de autorización RBAC y permite el acceso directo al mapa
 * de incidencias (/map) y a vistas operativas para roles autorizados.
 */

import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import authService from '../services/authService';
import './HomePage.css';

/**
 * Componente principal del Dashboard del ciudadano.
 *
 * @returns {import('react').JSX.Element} Vista del dashboard para usuarios autenticados.
 */
export default function HomePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = authService.getUser();
  const hasOperatorAccess = authService.hasAnyRole(['OPERATOR', 'ADMIN']);

  // Control de descarte de la alerta asociado a la clave de navegación actual
  const [dismissedLocationKey, setDismissedLocationKey] = useState(null);

  const isAlertDismissed = dismissedLocationKey === location.key;
  const hasAccessDenied = Boolean(location.state?.accessDenied) && !isAlertDismissed;
  const accessDeniedMessage = hasAccessDenied
    ? location.state?.message || 'Acceso denegado: no dispones de permisos de operador o administrador'
    : null;

  /**
   * Descarta el banner de advertencia por acceso denegado y limpia el estado
   * en el historial de navegación para prevenir persistencia ante recargas.
   *
   * @returns {void}
   */
  const handleDismissAccessDenied = () => {
    setDismissedLocationKey(location.key);
    navigate(location.pathname, { replace: true, state: {} });
  };

  /**
   * Manejador para el cierre de sesión del usuario.
   * Elimina las credenciales de localStorage y redirige a la vista de login.
   *
   * @returns {void}
   */
  const handleLogout = () => {
    authService.logout();
    navigate('/login', { replace: true });
  };

  /**
   * Limpia el estado de navegación al hacer clic en un enlace de navegación interno.
   *
   * @returns {void}
   */
  const handleNavClick = () => {
    if (location.state?.accessDenied) {
      window.history.replaceState({}, document.title);
    }
  };

  return (
    <div className="home-container">
      <header className="home-header">
        <div className="home-brand">
          <h1 className="brand-title">UrbanPulse</h1>
          <span className="brand-badge">Portal Ciudadano</span>
        </div>
        <div className="home-header-actions">
          <Link
            to="/map"
            className="home-map-nav-link"
            onClick={handleNavClick}
            title="Ir al mapa cartográfico interactivo"
          >
            Ver Mapa de Incidencias
          </Link>
          {hasOperatorAccess && (
            <Link
              to="/operator-panel"
              className="operator-nav-link"
              onClick={handleNavClick}
            >
              Panel de Operador
            </Link>
          )}
          <button
            type="button"
            onClick={handleLogout}
            className="logout-button"
          >
            Cerrar sesión
          </button>
        </div>
      </header>

      {accessDeniedMessage && (
        <div className="access-denied-alert" role="alert" aria-live="assertive">
          <div className="alert-body">
            <span className="alert-icon" aria-hidden="true">⚠️</span>
            <div className="alert-text">
              <strong className="alert-title">Acceso Restringido por Control RBAC</strong>
              <p className="alert-description">{accessDeniedMessage}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleDismissAccessDenied}
            className="alert-dismiss-btn"
            aria-label="Cerrar aviso de advertencia"
          >
            ×
          </button>
        </div>
      )}

      <main className="home-main">
        <section className="welcome-card">
          <h2>
            ¡Bienvenido/a{user?.fullName ? `, ${user.fullName}` : ''}!
          </h2>
          <p className="welcome-subtitle">
            Has iniciado sesión correctamente en la plataforma de gestión de incidencias urbanas.
          </p>

          <div className="user-details">
            <div className="detail-item">
              <span className="detail-label">Correo electrónico:</span>
              <span className="detail-value">{user?.email || 'No disponible'}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Rol:</span>
              <span className="detail-value role-tag">{user?.role || 'CITIZEN'}</span>
            </div>
          </div>
        </section>

        <section className="features-preview">
          {/* Tarjeta destacada hacia el Mapa Cartográfico */}
          <div className="feature-card map-feature-card">
            <div className="map-feature-tag">Mapa Interactivo</div>
            <h3>Mapa de Incidencias Urbanas</h3>
            <p>
              Explora las incidencias en el territorio de Málaga en tiempo real y reporta avisos asistidos
              haciendo clic directamente sobre el mapa.
            </p>
            <Link
              to="/map"
              className="map-feature-btn"
              onClick={handleNavClick}
            >
              Ver Mapa de Incidencias &rarr;
            </Link>
          </div>

          {hasOperatorAccess && (
            <div className="feature-card operator-feature-card">
              <div className="operator-feature-tag">Rol Operativo Activo</div>
              <h3>Centro de Triaje de Incidencias</h3>
              <p>
                Dispones de credenciales de operador o administrador. Accede al panel para revisar,
                priorizar y tramitar los estados de incidencias reportadas.
              </p>
              <Link
                to="/operator-panel"
                className="operator-feature-btn"
                onClick={handleNavClick}
              >
                Acceder al Panel de Operador &rarr;
              </Link>
            </div>
          )}

          <div className="feature-card">
            <h3>Mis Reportes</h3>
            <p>
              Seguimiento en tiempo real del estado de tus solicitudes y actuaciones técnicas.
            </p>
          </div>
        </section>
      </main>
    </div>
  );
}
