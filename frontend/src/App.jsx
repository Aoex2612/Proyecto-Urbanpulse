/**
 * @fileoverview Componente principal de la aplicación UrbanPulse.
 * Configura el enrutamiento con React Router, estableciendo las rutas públicas de autenticación
 * (/login, /register), la ruta protegida del dashboard (/) y el redireccionamiento para rutas desconocidas.
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import ProtectedRoute from './components/ProtectedRoute';

/**
 * Componente raíz de la aplicación con la jerarquía de rutas.
 *
 * @returns {import('react').JSX.Element} Árbol de navegación y vistas de la aplicación.
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <HomePage />
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
