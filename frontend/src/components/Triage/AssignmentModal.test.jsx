import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AssignmentModal from './AssignmentModal';
import triageService from '../../services/triageService';

vi.mock('../../services/triageService', () => ({
  default: {
    getDepartments: vi.fn(),
    getTechniciansByDepartment: vi.fn(),
    assignDepartmentsAndTechnicians: vi.fn(),
    deactivateAssignments: vi.fn(),
  },
}));

describe('AssignmentModal', () => {
  const mockDepartments = [
    { id: 1, name: 'Alumbrado Público', description: 'Farolas y tendido' },
    { id: 2, name: 'Vías Públicas', description: 'Pavimento y calzadas' },
  ];

  const mockTechniciansDept1 = [
    { id: 10, fullName: 'Carlos Electricista', email: 'carlos@malaga.es' },
  ];

  const mockTechniciansDept2 = [
    { id: 20, fullName: 'María Asfalto', email: 'maria@malaga.es' },
  ];

  const mockIncident = {
    id: 42,
    title: 'Hoyo profundo en calzada',
    status: 'VALIDATED',
    assignments: [
      {
        id: 101,
        departmentId: 1,
        departmentName: 'Alumbrado Público',
        technicianId: 10,
        technicianName: 'Carlos Electricista',
        isActive: true,
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    triageService.getDepartments.mockResolvedValue(mockDepartments);
    triageService.getTechniciansByDepartment.mockImplementation((deptId) => {
      if (deptId === 1) return Promise.resolve(mockTechniciansDept1);
      if (deptId === 2) return Promise.resolve(mockTechniciansDept2);
      return Promise.resolve([]);
    });
  });

  it('no renderiza nada si isOpen es falso o incident es nulo', () => {
    const { container: c1 } = render(
      <AssignmentModal isOpen={false} incident={mockIncident} onClose={vi.fn()} />
    );
    expect(c1.firstChild).toBeNull();

    const { container: c2 } = render(
      <AssignmentModal isOpen={true} incident={null} onClose={vi.fn()} />
    );
    expect(c2.firstChild).toBeNull();
  });

  it('renderiza el modal accesible y carga el catálogo de departamentos', async () => {
    render(
      <AssignmentModal isOpen={true} incident={mockIncident} onClose={vi.fn()} />
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Asignación de Departamentos y Técnicos/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(triageService.getDepartments).toHaveBeenCalledTimes(1);
      expect(screen.getAllByText('Alumbrado Público').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Vías Públicas')).toBeInTheDocument();
    });
  });

  it('muestra las asignaciones activas existentes y permite desactivarlas', async () => {
    const onAssignmentsUpdated = vi.fn();
    triageService.deactivateAssignments.mockResolvedValueOnce([]);

    render(
      <AssignmentModal
        isOpen={true}
        incident={mockIncident}
        onAssignmentsUpdated={onAssignmentsUpdated}
        onClose={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Asignaciones Activas Actuales')).toBeInTheDocument();
      expect(screen.getByText(/Técnico asignado: Carlos Electricista/i)).toBeInTheDocument();
    });

    const deactivateBtn = screen.getByRole('button', { name: /Desactivar/i });
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(triageService.deactivateAssignments).toHaveBeenCalledWith(42, {
        departmentIds: [1],
        technicianIds: [10],
      });
      expect(onAssignmentsUpdated).toHaveBeenCalledWith([]);
      expect(screen.getByText(/Asignación desactivada exitosamente./i)).toBeInTheDocument();
    });
  });

  it('muestra error de validación al intentar guardar sin departamentos seleccionados', async () => {
    render(
      <AssignmentModal isOpen={true} incident={mockIncident} onClose={vi.fn()} />
    );

    await waitFor(() => {
      expect(screen.getByText('Vías Públicas')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /Guardar Asignación/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(
        screen.getByText(/Debe seleccionar al menos un departamento municipal para asignar./i)
      ).toBeInTheDocument();
    });
    expect(triageService.assignDepartmentsAndTechnicians).not.toHaveBeenCalled();
  });

  it('permite seleccionar múltiples departamentos y técnicos y guardar las asignaciones', async () => {
    const onAssignmentsUpdated = vi.fn();
    const onClose = vi.fn();

    const mockResponseAssignments = [
      { id: 201, departmentId: 2, technicianId: 20, isActive: true },
    ];
    triageService.assignDepartmentsAndTechnicians.mockResolvedValueOnce(
      mockResponseAssignments
    );

    render(
      <AssignmentModal
        isOpen={true}
        incident={mockIncident}
        onAssignmentsUpdated={onAssignmentsUpdated}
        onClose={onClose}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Vías Públicas')).toBeInTheDocument();
    });

    // Seleccionar departamento Vías Públicas (id 2)
    const viasCheckbox = screen.getByLabelText(/Seleccionar departamento Vías Públicas/i);
    fireEvent.click(viasCheckbox);

    // Debe cargar técnicos de Vías Públicas
    await waitFor(() => {
      expect(triageService.getTechniciansByDepartment).toHaveBeenCalledWith(2);
      expect(screen.getByText('María Asfalto')).toBeInTheDocument();
    });

    // Seleccionar técnico María Asfalto
    const techCheckbox = screen.getByLabelText(/Asignar a María Asfalto/i);
    fireEvent.click(techCheckbox);

    // Guardar asignación
    const saveButton = screen.getByRole('button', { name: /Guardar Asignación/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(triageService.assignDepartmentsAndTechnicians).toHaveBeenCalledWith(42, {
        departmentIds: [2],
        technicianIds: [20],
      });
      expect(onAssignmentsUpdated).toHaveBeenCalledWith(mockResponseAssignments);
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  it('cierra el modal al pulsar Cancelar o pulsar Escape', async () => {
    const onClose = vi.fn();
    render(
      <AssignmentModal isOpen={true} incident={mockIncident} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Vías Públicas')).toBeInTheDocument();
    });

    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    fireEvent.click(cancelBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
