import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';
import IncidentFilterBar from './IncidentFilterBar';

const DEPARTMENTS = [
  { id: 1, name: 'Alumbrado Público' },
  { publicId: 'uuid-parques', name: 'Parques y Jardines' },
];

const EMPTY_FILTERS = {
  text: '',
  category: '',
  startDate: '',
  endDate: '',
  status: '',
  district: '',
  priority: '',
  department: '',
};

describe('IncidentFilterBar', () => {
  it('renderiza solo filtros públicos (texto, categoría, fechas y distrito) para CITIZEN', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="CITIZEN" />);

    expect(screen.getByPlaceholderText(/Título o descripción/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Categoría:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Desde:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Hasta:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Distrito:/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Estado:/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Prioridad:/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Departamento:/i)).not.toBeInTheDocument();
  });

  it('oculta los filtros internos también para visitantes anónimos (sin rol)', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} />);

    expect(screen.getByLabelText(/Distrito:/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Estado:/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Prioridad:/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Departamento:/i)).not.toBeInTheDocument();
  });

  it('renderiza todos los filtros para roles internos (OPERATOR)', () => {
    render(
      <IncidentFilterBar
        onFilterChange={vi.fn()}
        userRole="OPERATOR"
        departments={DEPARTMENTS}
      />
    );

    expect(screen.getByLabelText(/Estado:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Prioridad:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Departamento:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Distrito:/i)).toBeInTheDocument();
  });

  it('renderiza las opciones de prioridad con etiquetas legibles', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="OPERATOR" />);

    const priority = screen.getByLabelText(/Prioridad:/i);
    expect(priority).toHaveValue('');
    expect(screen.getByRole('option', { name: 'Todas las prioridades' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Baja' })).toHaveValue('LOW');
    expect(screen.getByRole('option', { name: 'Media' })).toHaveValue('MEDIUM');
    expect(screen.getByRole('option', { name: 'Alta' })).toHaveValue('HIGH');
    expect(screen.getByRole('option', { name: 'Crítica' })).toHaveValue('CRITICAL');
  });

  it('renderiza sin romper cuando departments es undefined o vacío', () => {
    const { unmount } = render(
      <IncidentFilterBar onFilterChange={vi.fn()} userRole="OPERATOR" />
    );

    const department = screen.getByLabelText(/Departamento:/i);
    expect(department).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Todos los departamentos' })).toBeInTheDocument();
    expect(
      screen.getByRole('option', { name: 'Sin departamento asignado' })
    ).toBeInTheDocument();

    unmount();

    render(
      <IncidentFilterBar onFilterChange={vi.fn()} userRole="OPERATOR" departments={[]} />
    );
    expect(screen.getByLabelText(/Departamento:/i)).toBeInTheDocument();
  });

  it('incluye las opciones de departamento recibidas por props', () => {
    render(
      <IncidentFilterBar
        onFilterChange={vi.fn()}
        userRole="OPERATOR"
        departments={DEPARTMENTS}
      />
    );

    const department = screen.getByLabelText(/Departamento:/i);
    expect(within(department).getByRole('option', { name: 'Alumbrado Público' })).toHaveValue(
      '1'
    );
    expect(
      within(department).getByRole('option', { name: 'Parques y Jardines' })
    ).toHaveValue('uuid-parques');
  });

  it('notifica onFilterChange al cambiar el texto de búsqueda', () => {
    const handleFilterChange = vi.fn();
    render(<IncidentFilterBar onFilterChange={handleFilterChange} userRole="CITIZEN" />);

    fireEvent.change(screen.getByPlaceholderText(/Título o descripción/i), {
      target: { value: 'farola' },
    });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({ text: 'farola' })
    );
  });

  it('emite el distrito seleccionado en onFilterChange', () => {
    const handleFilterChange = vi.fn();
    render(<IncidentFilterBar onFilterChange={handleFilterChange} userRole="CITIZEN" />);

    fireEvent.change(screen.getByLabelText(/Distrito:/i), {
      target: { value: 'Centro' },
    });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({ district: 'Centro' })
    );
  });

  it('emite la prioridad seleccionada en onFilterChange', () => {
    const handleFilterChange = vi.fn();
    render(<IncidentFilterBar onFilterChange={handleFilterChange} userRole="OPERATOR" />);

    fireEvent.change(screen.getByLabelText(/Prioridad:/i), {
      target: { value: 'HIGH' },
    });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({ priority: 'HIGH' })
    );
  });

  it('emite el departamento seleccionado o "unassigned" en onFilterChange', () => {
    const handleFilterChange = vi.fn();
    render(
      <IncidentFilterBar
        onFilterChange={handleFilterChange}
        userRole="OPERATOR"
        departments={DEPARTMENTS}
      />
    );

    const department = screen.getByLabelText(/Departamento:/i);
    fireEvent.change(department, { target: { value: '1' } });
    expect(handleFilterChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ department: '1' })
    );

    fireEvent.change(department, { target: { value: 'unassigned' } });
    expect(handleFilterChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ department: 'unassigned' })
    );
  });

  it('limpia todos los filtros al pulsar Limpiar filtros', () => {
    const handleFilterChange = vi.fn();
    render(
      <IncidentFilterBar
        onFilterChange={handleFilterChange}
        userRole="OPERATOR"
        departments={DEPARTMENTS}
      />
    );

    const input = screen.getByPlaceholderText(/Título o descripción/i);
    fireEvent.change(input, { target: { value: 'bache' } });
    fireEvent.change(screen.getByLabelText(/Distrito:/i), { target: { value: 'Centro' } });
    fireEvent.change(screen.getByLabelText(/Prioridad:/i), { target: { value: 'HIGH' } });
    fireEvent.change(screen.getByLabelText(/Departamento:/i), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: /Limpiar filtros/i }));

    expect(handleFilterChange).toHaveBeenLastCalledWith(EMPTY_FILTERS);
    expect(input.value).toBe('');
    expect(screen.getByLabelText(/Distrito:/i)).toHaveValue('');
    expect(screen.getByLabelText(/Prioridad:/i)).toHaveValue('');
    expect(screen.getByLabelText(/Departamento:/i)).toHaveValue('');
  });

  it('muestra aviso si hasResults es false', () => {
    render(<IncidentFilterBar onFilterChange={vi.fn()} userRole="CITIZEN" hasResults={false} />);
    expect(
      screen.getByText(/No se encontraron incidencias con los filtros aplicados/i)
    ).toBeInTheDocument();
  });
});