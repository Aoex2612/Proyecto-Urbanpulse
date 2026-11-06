/**
 * @fileoverview Pruebas de integración del enrutamiento principal en App.jsx.
 * Verifica la protección de rutas RBAC para /operator-panel permitiendo acceso
 * a OPERATOR, ADMIN y TECHNICIAN, y bloqueando el acceso a perfiles no autorizados.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';

// Mocks de vistas hijas para aislar la validación de enrutamiento y guardas de seguridad
vi.mock('./pages/HomePage', () => ({
  default: () => <div data-testid="page-home">Vista HomePage</div>,
}));

vi.mock('./pages/OperatorPanelPage', () => ({
  default: () => <div data-testid="page-operator-panel">Vista OperatorPanelPage</div>,
}));

vi.mock('./pages/LoginPage', () => ({
  default: () => <div data-testid="page-login">Vista LoginPage</div>,
}));

vi.mock('./pages/RegisterPage', () => ({
  default: () => <div data-testid="page-register">Vista RegisterPage</div>,
}));

vi.mock('./pages/MapPage', () => ({
  default: () => <div data-testid="page-map">Vista MapPage</div>,
}));

vi.mock('./pages/MyIncidentsPage', () => ({
  default: () => <div data-testid="page-my-incidents">Vista MyIncidentsPage</div>,
}));

describe('App — Enrutamiento y Protección RBAC', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState({}, '', '/');
  });

  it('permite acceder a /operator-panel cuando el usuario tiene rol TECHNICIAN', () => {
    localStorage.setItem('token', 'valid-tech-token');
    localStorage.setItem('user', JSON.stringify({ id: 10, role: 'TECHNICIAN' }));
    window.history.pushState({}, '', '/operator-panel');

    render(<App />);

    expect(screen.getByTestId('page-operator-panel')).toBeInTheDocument();
  });

  it('permite acceder a /operator-panel cuando el usuario tiene rol OPERATOR', () => {
    localStorage.setItem('token', 'valid-operator-token');
    localStorage.setItem('user', JSON.stringify({ id: 20, role: 'OPERATOR' }));
    window.history.pushState({}, '', '/operator-panel');

    render(<App />);

    expect(screen.getByTestId('page-operator-panel')).toBeInTheDocument();
  });

  it('permite acceder a /operator-panel cuando el usuario tiene rol ADMIN', () => {
    localStorage.setItem('token', 'valid-admin-token');
    localStorage.setItem('user', JSON.stringify({ id: 30, role: 'ADMIN' }));
    window.history.pushState({}, '', '/operator-panel');

    render(<App />);

    expect(screen.getByTestId('page-operator-panel')).toBeInTheDocument();
  });

  it('redirige a / cuando un usuario con rol CITIZEN intenta acceder a /operator-panel', () => {
    localStorage.setItem('token', 'valid-citizen-token');
    localStorage.setItem('user', JSON.stringify({ id: 40, role: 'CITIZEN' }));
    window.history.pushState({}, '', '/operator-panel');

    render(<App />);

    expect(screen.queryByTestId('page-operator-panel')).not.toBeInTheDocument();
    expect(screen.getByTestId('page-home')).toBeInTheDocument();
  });

  it('redirige a /login cuando un usuario no autenticado intenta acceder a /operator-panel', () => {
    window.history.pushState({}, '', '/operator-panel');

    render(<App />);

    expect(screen.queryByTestId('page-operator-panel')).not.toBeInTheDocument();
    expect(screen.getByTestId('page-login')).toBeInTheDocument();
  });
});
