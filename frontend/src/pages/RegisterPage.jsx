/**
 * @fileoverview Vista de Registro de Ciudadanos (/register) para UrbanPulse.
 * Permite a nuevos usuarios registrarse en la plataforma con validación reactiva
 * en cliente, envío de petición a la API y redirección a login o dashboard.
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
 * Validador de complejidad de contraseña (mínimo 8 caracteres, al menos una letra y un dígito).
 *
 * @param {string} password - Cadena de contraseña a evaluar.
 * @returns {boolean} Verdadero si cumple los requisitos mínimos de seguridad.
 */
const isValidPassword = (password) => {
  const passwordRegex = /^(?=.*[a-zA-Z])(?=.*\d).{8,}$/;
  return passwordRegex.test(password);
};

/**
 * Componente principal de la página de Registro de Ciudadanos.
 *
 * @returns {import('react').JSX.Element} Vista del formulario de registro.
 */
export default function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
  });

  const [fieldErrors, setFieldErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
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
    if (apiError) {
      setApiError('');
    }
  };

  /**
   * Valida en cliente todos los campos del formulario conforme a RF-3 y RF-4.
   *
   * @returns {boolean} Verdadero si todos los campos son válidos; falso en caso contrario.
   */
  const validateForm = () => {
    const errors = {};
    const trimmedFullName = formData.fullName.trim();
    const trimmedEmail = formData.email.trim();
    const password = formData.password;

    if (!trimmedFullName) {
      errors.fullName = 'El nombre completo es obligatorio.';
    } else if (trimmedFullName.length > 150) {
      errors.fullName = 'El nombre no puede superar los 150 caracteres.';
    }

    if (!trimmedEmail) {
      errors.email = 'El correo electrónico es obligatorio.';
    } else if (trimmedEmail.length > 150) {
      errors.email = 'El correo electrónico no puede superar los 150 caracteres.';
    } else if (!isValidEmail(trimmedEmail)) {
      errors.email = 'Introduce un formato de correo electrónico válido.';
    }

    if (!password) {
      errors.password = 'La contraseña es obligatoria.';
    } else if (password.length < 8) {
      errors.password = 'La contraseña debe tener al menos 8 caracteres.';
    } else if (!isValidPassword(password)) {
      errors.password = 'La contraseña debe contener al menos una letra y un dígito.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Manejador del envío del formulario de registro.
   *
   * @param {import('react').FormEvent<HTMLFormElement>} event - Evento de envío del formulario.
   */
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setApiError('');
    setSuccessMessage('');

    try {
      await authService.register(
        formData.fullName.trim(),
        formData.email.trim().toLowerCase(),
        formData.password
      );

      setSuccessMessage('¡Registro completado con éxito! Redirigiendo a iniciar sesión...');

      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: {
            registeredEmail: formData.email.trim().toLowerCase(),
            message: 'Registro exitoso. Inicia sesión con tus credenciales.',
          },
        });
      }, 1500);
    } catch (error) {
      const message =
        error.response?.data?.message ||
        'Ocurrió un error al procesar el registro. Inténtalo de nuevo.';
      setApiError(message);
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <header className="auth-header">
          <h1 className="auth-title">Crear Cuenta</h1>
          <p className="auth-subtitle">Regístrate como ciudadano en UrbanPulse</p>
        </header>

        {apiError && (
          <div className="alert-message alert-error" role="alert">
            {apiError}
          </div>
        )}

        {successMessage && (
          <div className="alert-message alert-success" role="status">
            {successMessage}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="fullName">
              Nombre completo
            </label>
            <input
              id="fullName"
              name="fullName"
              type="text"
              autoComplete="name"
              maxLength={150}
              placeholder="Ej. Ana García"
              className={`form-input ${fieldErrors.fullName ? 'input-error' : ''}`}
              value={formData.fullName}
              onChange={handleChange}
              disabled={loading}
            />
            {fieldErrors.fullName && (
              <span className="field-error">{fieldErrors.fullName}</span>
            )}
          </div>

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
              placeholder="ana.garcia@ejemplo.com"
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
              autoComplete="new-password"
              placeholder="Mínimo 8 caracteres, letra y número"
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
            {loading ? 'Creando cuenta...' : 'Registrarse'}
          </button>
        </form>

        <footer className="auth-footer">
          <span>¿Ya tienes una cuenta?</span>
          <Link to="/login" className="auth-link">
            Inicia sesión aquí
          </Link>
        </footer>
      </div>
    </div>
  );
}
