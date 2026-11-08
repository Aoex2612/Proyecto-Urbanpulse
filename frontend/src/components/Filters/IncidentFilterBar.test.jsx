import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import IncidentFilterBar from './IncidentFilterBar';

describe('IncidentFilterBar', () => {
  it('renderiza filtros comunes (texto, categoría, fechas) para CITIZEN', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="CITIZEN" />);

    expect(screen.getByPlaceholderText(/Título o descripción/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Categoría:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Desde:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Hasta:/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Estado:/i)).not.toBeInTheDocument();
  });

  it('renderiza filtro de estado para roles internos (OPERATOR, ADMIN)', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="OPERATOR" />);

    expect(screen.getByLabelText(/Estado:/i)).toBeInTheDocument();
  });

  it('notifica onFilterChange al cambiar el texto de búsqueda', () => {
    const handleFilterChange = vi.fn();
    render(<IncidentFilterBar onFilterChange={handleFilterChange} userRole="CITIZEN" />);

    const input = screen.getByPlaceholderText(/Título o descripción/i);
    fireEvent.change(input, { target: { value: 'farola' } });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({ text: 'farola' })
    );
  });

  it('limpia todos los filtros al pulsar Limpiar filtros', () => {
    const handleFilterChange = vi.fn();
    render(<IncidentFilterBar onFilterChange={handleFilterChange} userRole="OPERATOR" />);

    const input = screen.getByPlaceholderText(/Título o descripción/i);
    fireEvent.change(input, { target: { value: 'bache' } });

    const clearBtn = screen.getByRole('button', { name: /Limpiar filtros/i });
    fireEvent.click(clearBtn);

    expect(handleFilterChange).toHaveBeenLastCalledWith({
      text: '',
      category: '',
      startDate: '',
      endDate: '',
      status: '',
    });
    expect(input.value).toBe('');
  });

  it('muestra aviso si hasResults es false', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="CITIZEN" hasResults={false} />);
    expect(
      screen.getByText(/No se encontraron incidencias con los filtros aplicados/i)
    ).toBeInTheDocument();
  });
});
