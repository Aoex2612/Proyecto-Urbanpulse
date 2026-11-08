import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationBell from './NotificationBell';
import notificationService from '../../services/notificationService';

vi.mock('../../services/notificationService', () => ({
  default: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
  },
}));

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    notificationService.getUnreadCount.mockResolvedValue(0);
    notificationService.getNotifications.mockResolvedValue([]);
  });

  it('renderiza el botón de campana accesible', () => {
    render(<NotificationBell />);
    expect(screen.getByRole('button', { name: /Notificaciones/i })).toBeInTheDocument();
  });

  it('no muestra badge si unreadCount es 0', async () => {
    notificationService.getUnreadCount.mockResolvedValue(0);
    render(<NotificationBell />);
    await waitFor(() => {
      expect(screen.queryByText(/^\d+$/)).not.toBeInTheDocument();
    });
  });

  it('muestra badge con el número de no leídas', async () => {
    notificationService.getUnreadCount.mockResolvedValue(3);
    render(<NotificationBell />);
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('abre el dropdown al hacer clic en la campana', async () => {
    render(<NotificationBell />);
    const btn = screen.getByRole('button', { name: /Notificaciones/i });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(screen.getByText('Notificaciones')).toBeInTheDocument();
    });
  });

  it('muestra mensaje cuando no hay notificaciones', async () => {
    notificationService.getNotifications.mockResolvedValue([]);
    render(<NotificationBell />);
    fireEvent.click(screen.getByRole('button', { name: /Notificaciones/i }));
    await waitFor(() => {
      expect(screen.getByText(/No hay notificaciones pendientes/i)).toBeInTheDocument();
    });
  });

  it('muestra listado de notificaciones recibidas', async () => {
    notificationService.getNotifications.mockResolvedValue([
      {
        id: 1,
        title: 'Incidencia #10 actualizada a VALIDATED',
        message: 'La farola ha sido validada',
        isRead: false,
        createdAt: new Date().toISOString(),
        incidentId: 10,
      },
    ]);
    render(<NotificationBell />);
    fireEvent.click(screen.getByRole('button', { name: /Notificaciones/i }));
    await waitFor(() => {
      expect(screen.getByText('Incidencia #10 actualizada a VALIDATED')).toBeInTheDocument();
      expect(screen.getByText('La farola ha sido validada')).toBeInTheDocument();
    });
  });

  it('invoca markAsRead al hacer clic en Marcar como leída', async () => {
    notificationService.getNotifications.mockResolvedValue([
      {
        id: 5,
        title: 'Incidencia #5',
        message: 'Mensaje de prueba',
        isRead: false,
        createdAt: new Date().toISOString(),
        incidentId: 5,
      },
    ]);
    notificationService.markAsRead.mockResolvedValue({ id: 5, isRead: true });

    render(<NotificationBell />);
    fireEvent.click(screen.getByRole('button', { name: /Notificaciones/i }));

    await waitFor(() => {
      expect(screen.getByText('Marcar como leída')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Marcar como leída'));

    await waitFor(() => {
      expect(notificationService.markAsRead).toHaveBeenCalledWith(5);
    });
  });

  it('invoca onIncidentClick al hacer clic en el título de la notificación', async () => {
    const handleIncidentClick = vi.fn();
    notificationService.getNotifications.mockResolvedValue([
      {
        id: 7,
        title: 'Incidencia #7 actualizada',
        message: 'Detalle',
        isRead: true,
        createdAt: new Date().toISOString(),
        incidentId: 77,
      },
    ]);

    render(<NotificationBell onIncidentClick={handleIncidentClick} />);
    fireEvent.click(screen.getByRole('button', { name: /Notificaciones/i }));

    await waitFor(() => {
      expect(screen.getByText('Incidencia #7 actualizada')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Incidencia #7 actualizada'));

    expect(handleIncidentClick).toHaveBeenCalledWith(77);
  });
});
