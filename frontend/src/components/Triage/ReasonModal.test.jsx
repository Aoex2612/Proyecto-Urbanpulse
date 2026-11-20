import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ReasonModal from './ReasonModal';

describe('ReasonModal', () => {
  const dummyIncident = {
    id: 101,
    title: 'Fuga de agua en calzada',
    status: 'REPORTED',
  };

  it('no renderiza nada si isOpen es false', () => {
    const { container } = render(
      <ReasonModal isOpen={false} onClose={vi.fn()} onSubmit={vi.fn()} incident={dummyIncident} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renderiza correctamente en modo REJECT con título y validación mínima de 5 caracteres', () => {
    render(
      <ReasonModal
        isOpen={true}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        actionType="REJECT"
        incident={dummyIncident}
      />
    );

    expect(screen.getByText('Rechazar Incidencia #101')).toBeInTheDocument();
    const submitBtn = screen.getByRole('button', { name: 'Confirmar Rechazo' });
    expect(submitBtn).toBeDisabled();

    const textarea = screen.getByPlaceholderText(/Detalla el motivo del rechazo/i);
    fireEvent.change(textarea, { target: { value: 'Hola' } });
    expect(submitBtn).toBeDisabled();
    expect(screen.getByText('4 / 5 mín.')).toBeInTheDocument();

    fireEvent.change(textarea, { target: { value: 'Motivo suficiente para descartar' } });
    expect(submitBtn).not.toBeDisabled();
    expect(screen.getByText('32 / 5 mín.')).toBeInTheDocument();
  });

  it('envía la justificación al hacer submit en modo REJECT', async () => {
    const handleSubmit = vi.fn().mockResolvedValue({});
    const handleClose = vi.fn();

    render(
      <ReasonModal
        isOpen={true}
        onClose={handleClose}
        onSubmit={handleSubmit}
        actionType="REJECT"
        incident={dummyIncident}
      />
    );

    const textarea = screen.getByPlaceholderText(/Detalla el motivo del rechazo/i);
    fireEvent.change(textarea, { target: { value: 'No procede por ser vía privada' } });

    const submitBtn = screen.getByRole('button', { name: 'Confirmar Rechazo' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        reason: 'No procede por ser vía privada',
        targetStatus: undefined,
      });
      expect(handleClose).toHaveBeenCalled();
    });
  });

  it('renderiza selector de estado destino en modo REEVALUATE y envía targetStatus', async () => {
    const handleSubmit = vi.fn().mockResolvedValue({});
    const handleClose = vi.fn();

    render(
      <ReasonModal
        isOpen={true}
        onClose={handleClose}
        onSubmit={handleSubmit}
        actionType="REEVALUATE"
        incident={{ ...dummyIncident, status: 'REJECTED' }}
      />
    );

    expect(screen.getByText('Reevaluar Incidencia Rechazada #101')).toBeInTheDocument();
    const select = screen.getByLabelText(/Estado Operativo de Destino/i);
    expect(select.value).toBe('VALIDATED');

    fireEvent.change(select, { target: { value: 'REPORTED' } });
    expect(select.value).toBe('REPORTED');

    const textarea = screen.getByPlaceholderText(/Justifica la rectificación/i);
    fireEvent.change(textarea, { target: { value: 'Nuevos informes técnicos confirman competencia' } });

    const submitBtn = screen.getByRole('button', { name: 'Confirmar Reevaluación' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        reason: 'Nuevos informes técnicos confirman competencia',
        targetStatus: 'REPORTED',
      });
      expect(handleClose).toHaveBeenCalled();
    });
  });

  it('cierra el modal al pulsar la tecla Escape', () => {
    const handleClose = vi.fn();

    render(
      <ReasonModal
        isOpen={true}
        onClose={handleClose}
        onSubmit={vi.fn()}
        actionType="REOPEN"
        incident={dummyIncident}
      />
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalled();
  });
});
