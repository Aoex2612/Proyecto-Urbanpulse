/**
 * @fileoverview Vista principal / Dashboard (/ ) para ciudadanos autenticados en UrbanPulse.
 * Muestra el perfil del ciudadano, información de la plataforma y permite el cierre de sesión seguro.
 */

import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import './HomePage.css';

/**
 * Componente principal del Dashboard del ciudadano.
 *
 * @returns {import('react').JSX.Element} Vista del dashboard para usuarios autenticados.
 */
export default function HomePage() {
  const navigate = useNavigate();
  const user = authService.getUser();

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

  return (
    <div className="home-container">
      <header className="home-header">
        <div className="home-brand">
          <h1 className="brand-title">UrbanPulse</h1>
          <span className="brand-badge">Portal Ciudadano</span>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="logout-button"
        >
          Cerrar sesión
        </button>
      </header>

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
          <div className="feature-card">
            <h3>Reportar Incidencia</h3>
            <p>
              Próximamente podrás registrar averías de alumbrado, desperfectos en vías públicas y más.
            </p>
          </div>
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
