/**
 * @fileoverview Pruebas unitarias para el componente HomePage del portal ciudadano.
 * Verifica la correcta visualización de enlaces y accesos directos al panel operativo
 * según los diferentes roles RBAC (CITIZEN, TECHNICIAN, OPERATOR, ADMIN).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HomePage from './HomePage';
import authService from '../services/authService';

vi.mock('../services/authService', () => ({
  default: {
    getUser: vi.fn(),
    hasAnyRole: vi.fn(),
    logout: vi.fn(),
  },
}));

describe('HomePage — Dashboard Ciudadano y Accesos Operativos', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

  it('para rol TECHNICIAN muestra enlace a Mis Asignaciones y tarjeta de asignaciones técnicas', () => {
    authService.getUser.mockReturnValue({
      id: 10,
      fullName: 'Técnico Campo',
      email: 'tech@urbanpulse.es',
      role: 'TECHNICIAN',
    });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('TECHNICIAN'));

    renderComponent();

    expect(screen.getByText('Portal Ciudadano')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mis Asignaciones' })).toBeInTheDocument();
    expect(screen.getByText('Mis Asignaciones Técnicas')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ver Mis Asignaciones/i })).toBeInTheDocument();
  });

  it('para rol OPERATOR o ADMIN muestra enlace a Panel de Operador y tarjeta de centro de triaje', () => {
    authService.getUser.mockReturnValue({
      id: 20,
      fullName: 'Operador Triaje',
      email: 'operator@urbanpulse.es',
      role: 'OPERATOR',
    });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('OPERATOR'));

    renderComponent();

    expect(screen.getByRole('link', { name: 'Panel de Operador' })).toBeInTheDocument();
    expect(screen.getByText('Centro de Triaje de Incidencias')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Acceder al Panel de Operador/i })).toBeInTheDocument();
  });

  it('para rol CITIZEN oculta cualquier enlace o tarjeta hacia el panel de operador y analítica', () => {
    authService.getUser.mockReturnValue({
      id: 30,
      fullName: 'Vecino Ejemplo',
      email: 'vecino@urbanpulse.es',
      role: 'CITIZEN',
    });
    authService.hasAnyRole.mockReturnValue(false);

    renderComponent();

    expect(screen.queryByRole('link', { name: 'Panel de Operador' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Mis Asignaciones' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Analítica' })).not.toBeInTheDocument();
    expect(screen.queryByText('Centro de Triaje de Incidencias')).not.toBeInTheDocument();
    expect(screen.queryByText('Mis Asignaciones Técnicas')).not.toBeInTheDocument();
    expect(screen.queryByText('Analítica Urbana y Reportes')).not.toBeInTheDocument();
  });

  it('para rol ANALYST muestra enlace a Analítica y tarjeta de analítica urbana (RF-19)', () => {
    authService.getUser.mockReturnValue({
      id: 50,
      fullName: 'Analista Urbano',
      email: 'analyst@urbanpulse.es',
      role: 'ANALYST',
    });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('ANALYST'));

    renderComponent();

    const analyticsLink = screen.getByRole('link', { name: 'Analítica' });
    expect(analyticsLink).toBeInTheDocument();
    expect(analyticsLink).toHaveAttribute('href', '/analytics');
    expect(screen.getByText('Analítica Urbana y Reportes')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Acceder a Analítica/i })).toBeInTheDocument();
  });

  it('para rol ADMIN muestra tanto panel de operador como analítica (RF-19)', () => {
    authService.getUser.mockReturnValue({
      id: 60,
      fullName: 'Administrador General',
      email: 'admin@urbanpulse.es',
      role: 'ADMIN',
    });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('ADMIN'));

    renderComponent();

    expect(screen.getByRole('link', { name: 'Panel de Operador' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Analítica' })).toBeInTheDocument();
  });

  it('para rol TECHNICIAN oculta enlace a Analítica y tarjeta asociada (RF-19)', () => {
    authService.getUser.mockReturnValue({
      id: 70,
      fullName: 'Técnico Operativo',
      email: 'tech2@urbanpulse.es',
      role: 'TECHNICIAN',
    });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('TECHNICIAN'));

    renderComponent();

    expect(screen.queryByRole('link', { name: 'Analítica' })).not.toBeInTheDocument();
    expect(screen.queryByText('Analítica Urbana y Reportes')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mis Asignaciones' })).toBeInTheDocument();
  });

  it('permite cerrar sesión invocando authService.logout', () => {
    authService.getUser.mockReturnValue({
      id: 30,
      fullName: 'Vecino Ejemplo',
      email: 'vecino@urbanpulse.es',
      role: 'CITIZEN',
    });
    authService.hasAnyRole.mockReturnValue(false);

    renderComponent();

    const logoutBtn = screen.getByRole('button', { name: 'Cerrar sesión' });
    fireEvent.click(logoutBtn);

    expect(authService.logout).toHaveBeenCalled();
  });
});
