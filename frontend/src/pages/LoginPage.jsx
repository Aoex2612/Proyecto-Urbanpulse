/**
 * @fileoverview Vista de Inicio de Sesión (/login) para UrbanPulse.
 * Permite a los ciudadanos autenticarse mediante correo electrónico y contraseña,
 * gestionando el almacenamiento del token JWT, control de errores genéricos (401)
 * y redirección condicional.
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';
import './Auth.css';

/**
 * Validador del formato de correo electrónico.
 *
 * @param {string} email - Cadena con el correo a evaluar.
 * @returns {boolean} Verdadero si cumple con el patrón de email.
 */
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Componente principal de la página de Inicio de Sesión.
 *
 * @returns {import('react').JSX.Element} Vista del formulario de login.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: location.state?.registeredEmail || '',
    password: '',
  });

  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [infoMessage, setInfoMessage] = useState(location.state?.message || '');
  const [loading, setLoading] = useState(false);

  // RF-11: Si el usuario ya está autenticado, redirigir inmediatamente a la raíz
  useEffect(() => {
    if (authService.isAuthenticated()) {
      navigate('/', { replace: true });
    }
  }, [navigate]);

  /**
   * Manejador de cambio para los inputs del formulario.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} event - Evento del input.
   */
  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({
        ...prev,
        [name]: undefined,
      }));
    }
    if (errorMessage) {
      setErrorMessage('');
    }
  };

  /**
   * Valida en cliente los campos de correo y contraseña antes de llamar a la API.
   *
   * @returns {boolean} Verdadero si los campos son válidos; falso en caso contrario.
   */
  const validateForm = () => {
    const errors = {};
    const trimmedEmail = formData.email.trim();
    const password = formData.password;

    if (!trimmedEmail) {
      errors.email = 'El correo electrónico es obligatorio.';
    } else if (!isValidEmail(trimmedEmail)) {
      errors.email = 'Introduce un formato de correo electrónico válido.';
    }

    if (!password) {
      errors.password = 'La contraseña es obligatoria.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Manejador del envío del formulario de inicio de sesión.
   *
   * @param {import('react').FormEvent<HTMLFormElement>} event - Evento de envío del formulario.
   */
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setErrorMessage('');
    setInfoMessage('');

    try {
      await authService.login(formData.email.trim().toLowerCase(), formData.password);
      navigate('/', { replace: true });
    } catch (error) {
      if (error.response && error.response.status === 401) {
        // RF-7: Error genérico para credenciales inválidas sin revelar qué falló
        setErrorMessage('Correo electrónico o contraseña incorrectos.');
      } else {
        const msg =
          error.response?.data?.message ||
          'Ha ocurrido un error al iniciar sesión. Inténtalo de nuevo.';
        setErrorMessage(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <header className="auth-header">
          <h1 className="auth-title">Iniciar Sesión</h1>
          <p className="auth-subtitle">Accede a tu cuenta de ciudadano en UrbanPulse</p>
        </header>

        {infoMessage && (
          <div className="alert-message alert-success" role="status">
            {infoMessage}
          </div>
        )}

        {errorMessage && (
          <div className="alert-message alert-error" role="alert">
            {errorMessage}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              Correo electrónico
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              maxLength={150}
              placeholder="tu.correo@ejemplo.com"
              className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
              value={formData.email}
              onChange={handleChange}
              disabled={loading}
            />
            {fieldErrors.email && (
              <span className="field-error">{fieldErrors.email}</span>
            )}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">
              Contraseña
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
              value={formData.password}
              onChange={handleChange}
              disabled={loading}
            />
            {fieldErrors.password && (
              <span className="field-error">{fieldErrors.password}</span>
            )}
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
          </button>
        </form>

        <footer className="auth-footer">
          <span>¿No tienes una cuenta?</span>
          <Link to="/register" className="auth-link">
            Regístrate aquí
          </Link>
        </footer>
      </div>
    </div>
  );
}
