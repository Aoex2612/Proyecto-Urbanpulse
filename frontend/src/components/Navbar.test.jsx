/**
 * @fileoverview Pruebas unitarias para el componente Navbar de UrbanPulse (RF-19).
 * Verifica la visualización del enlace "Analítica" (/analytics) exclusivamente para roles
 * ANALYST, ADMIN y OPERATOR, ocultándose para CITIZEN y TECHNICIAN.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Navbar from './Navbar';
import authService from '../services/authService';

vi.mock('../services/authService', () => ({
  default: {
    getUser: vi.fn(),
    hasAnyRole: vi.fn(),
    logout: vi.fn(),
  },
}));

describe('Navbar — Navegación Global y Enlace de Analítica (RF-19)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderNavbar = (initialEntries = ['/']) =>
    render(
      <MemoryRouter initialEntries={initialEntries}>
        <Navbar />
      </MemoryRouter>
    );

  it('renderiza enlaces comunes para todos los usuarios autenticados', () => {
    authService.getUser.mockReturnValue({ id: 1, role: 'CITIZEN' });
    authService.hasAnyRole.mockReturnValue(false);

    renderNavbar();

    expect(screen.getByText('UrbanPulse')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inicio' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mapa' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mis Incidencias' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cerrar sesión/i })).toBeInTheDocument();
  });

  it('muestra el enlace "Analítica" hacia /analytics para rol ANALYST (RF-19)', () => {
    authService.getUser.mockReturnValue({ id: 2, role: 'ANALYST' });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('ANALYST'));

    renderNavbar();

    const analyticsLink = screen.getByRole('link', { name: 'Analítica' });
    expect(analyticsLink).toBeInTheDocument();
    expect(analyticsLink).toHaveAttribute('href', '/analytics');
  });

  it('muestra el enlace "Analítica" hacia /analytics para rol ADMIN (RF-19)', () => {
    authService.getUser.mockReturnValue({ id: 3, role: 'ADMIN' });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('ADMIN'));

    renderNavbar();

    const analyticsLink = screen.getByRole('link', { name: 'Analítica' });
    expect(analyticsLink).toBeInTheDocument();
    expect(analyticsLink).toHaveAttribute('href', '/analytics');
    expect(screen.getByRole('link', { name: 'Panel de Operador' })).toBeInTheDocument();
  });

  it('muestra el enlace "Analítica" hacia /analytics para rol OPERATOR (RF-19)', () => {
    authService.getUser.mockReturnValue({ id: 4, role: 'OPERATOR' });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('OPERATOR'));

    renderNavbar();

    const analyticsLink = screen.getByRole('link', { name: 'Analítica' });
    expect(analyticsLink).toBeInTheDocument();
    expect(analyticsLink).toHaveAttribute('href', '/analytics');
    expect(screen.getByRole('link', { name: 'Panel de Operador' })).toBeInTheDocument();
  });

  it('oculta el enlace "Analítica" para rol CITIZEN (RF-19)', () => {
    authService.getUser.mockReturnValue({ id: 5, role: 'CITIZEN' });
    authService.hasAnyRole.mockReturnValue(false);

    renderNavbar();

    expect(screen.queryByRole('link', { name: 'Analítica' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Panel de Operador' })).not.toBeInTheDocument();
  });

  it('oculta el enlace "Analítica" para rol TECHNICIAN y muestra "Mis Asignaciones" (RF-19)', () => {
    authService.getUser.mockReturnValue({ id: 6, role: 'TECHNICIAN' });
    authService.hasAnyRole.mockImplementation((roles) => roles.includes('TECHNICIAN'));

    renderNavbar();

    expect(screen.queryByRole('link', { name: 'Analítica' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mis Asignaciones' })).toBeInTheDocument();
  });

  it('invoca authService.logout al hacer clic en Cerrar sesión', () => {
    authService.getUser.mockReturnValue({ id: 1, role: 'CITIZEN' });
    authService.hasAnyRole.mockReturnValue(false);

    renderNavbar();

    const logoutBtn = screen.getByRole('button', { name: /Cerrar sesión/i });
    fireEvent.click(logoutBtn);

    expect(authService.logout).toHaveBeenCalledTimes(1);
  });
});
