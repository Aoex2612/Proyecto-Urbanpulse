import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import notificationService from './notificationService';

vi.mock('axios');

describe('notificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('token', 'fake-jwt-token');
  });

  it('getNotifications: realiza petición GET y retorna lista de notificaciones', async () => {
    const mockNotifications = [
      { id: 1, title: 'Incidencia actualizada', isRead: false },
      { id: 2, title: 'Duplicado detectado', isRead: true },
    ];
    axios.get.mockResolvedValueOnce({ data: mockNotifications });

    const result = await notificationService.getNotifications();

    expect(result).toEqual(mockNotifications);
    expect(axios.get).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications'),
      expect.objectContaining({
        headers: { Authorization: 'Bearer fake-jwt-token' },
      })
    );
  });

  it('getUnreadCount: retorna el número de notificaciones no leídas', async () => {
    axios.get.mockResolvedValueOnce({ data: 4 });

    const count = await notificationService.getUnreadCount();

    expect(count).toBe(4);
    expect(axios.get).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications/unread-count'),
      expect.objectContaining({
        headers: { Authorization: 'Bearer fake-jwt-token' },
      })
    );
  });

  it('markAsRead: realiza petición PATCH y retorna notificación actualizada', async () => {
    const updated = { id: 10, title: 'Incidencia #10', isRead: true };
    axios.patch.mockResolvedValueOnce({ data: updated });

    const result = await notificationService.markAsRead(10);

    expect(result).toEqual(updated);
    expect(axios.patch).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications/10/read'),
      {},
      expect.objectContaining({
        headers: { Authorization: 'Bearer fake-jwt-token' },
      })
    );
  });
});
