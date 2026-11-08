import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import EmptyAnalyticsState from './EmptyAnalyticsState';

describe('EmptyAnalyticsState', () => {
  it('renderiza el título por defecto, mensaje explicativo y botón de restablecimiento (RF-11)', () => {
    const handleReset = vi.fn();
    render(<EmptyAnalyticsState onResetFilters={handleReset} />);

    expect(
      screen.getByRole('heading', {
        name: /no se encontraron incidencias en el período seleccionado/i,
      })
    ).toBeInTheDocument();

    expect(
      screen.getByText(
        /no constan incidencias registradas que coincidan con los criterios temporales o territoriales vigentes/i
      )
    ).toBeInTheDocument();

    const resetBtn = screen.getByRole('button', { name: /restablecer filtros/i });
    expect(resetBtn).toBeInTheDocument();

    fireEvent.click(resetBtn);
    expect(handleReset).toHaveBeenCalledTimes(1);
  });

  it('permite personalizar el título y el mensaje descriptivo', () => {
    render(
      <EmptyAnalyticsState
        title="Sin registros en Churriana"
        message="No constan incidencias reportadas en Churriana en los últimos 7 días."
      />
    );

    expect(screen.getByText('Sin registros en Churriana')).toBeInTheDocument();
    expect(
      screen.getByText('No constan incidencias reportadas en Churriana en los últimos 7 días.')
    ).toBeInTheDocument();
    // No renderiza botón si no se pasa onResetFilters
    expect(screen.queryByRole('button', { name: /restablecer filtros/i })).not.toBeInTheDocument();
  });
});
