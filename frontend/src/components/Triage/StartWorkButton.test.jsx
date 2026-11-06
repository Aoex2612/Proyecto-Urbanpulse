import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import StartWorkButton from './StartWorkButton';
import triageService from '../../services/triageService';

vi.mock('../../services/triageService', () => ({
  default: {
    startWork: vi.fn(),
  },
}));

describe('StartWorkButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('se habilita cuando el estado es ASSIGNED o REOPENED', () => {
    const { rerender } = render(
      <StartWorkButton incidentId={20} incidentStatus="ASSIGNED" />
    );
    expect(screen.getByRole('button', { name: /Iniciar trabajo/i })).toBeEnabled();

    rerender(<StartWorkButton incidentId={20} incidentStatus="REOPENED" />);
    expect(screen.getByRole('button', { name: /Iniciar trabajo/i })).toBeEnabled();
  });

  it('se deshabilita cuando el estado no es ASSIGNED ni REOPENED (ej. REPORTED, IN_PROGRESS)', () => {
    const { rerender } = render(
      <StartWorkButton incidentId={20} incidentStatus="REPORTED" />
    );
    expect(screen.getByRole('button', { name: /Iniciar trabajo/i })).toBeDisabled();

    rerender(<StartWorkButton incidentId={20} incidentStatus="IN_PROGRESS" />);
    expect(screen.getByRole('button', { name: /Iniciar trabajo/i })).toBeDisabled();
  });

  it('invoca triageService.startWork y emite onWorkStarted', async () => {
    const handleWorkStarted = vi.fn();
    const updatedIncident = { id: 20, status: 'IN_PROGRESS' };

    triageService.startWork.mockResolvedValueOnce(updatedIncident);

    render(
      <StartWorkButton
        incidentId={20}
        incidentStatus="ASSIGNED"
        onWorkStarted={handleWorkStarted}
      />
    );

    const btn = screen.getByRole('button', { name: /Iniciar trabajo/i });
    fireEvent.click(btn);

    await waitFor(() => {
      expect(triageService.startWork).toHaveBeenCalledWith(20);
    });

    expect(handleWorkStarted).toHaveBeenCalledWith(updatedIncident);
  });

  it('muestra mensaje de error si falla la llamada de inicio de trabajo', async () => {
    triageService.startWork.mockRejectedValueOnce({
      response: {
        data: {
          message: 'El técnico autenticado no está asignado activamente a esta incidencia',
        },
      },
    });

    render(
      <StartWorkButton
        incidentId={20}
        incidentStatus="ASSIGNED"
      />
    );

    const btn = screen.getByRole('button', { name: /Iniciar trabajo/i });
    fireEvent.click(btn);

    await waitFor(() => {
      expect(
        screen.getByText(/El técnico autenticado no está asignado activamente a esta incidencia/i)
      ).toBeInTheDocument();
    });
  });
});
