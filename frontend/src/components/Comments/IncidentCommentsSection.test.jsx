import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import IncidentCommentsSection from './IncidentCommentsSection';
import triageService from '../../services/triageService';
import authService from '../../services/authService';

vi.mock('../../services/triageService', () => ({
  default: {
    getIncidentComments: vi.fn(),
    addIncidentComment: vi.fn(),
    updateIncidentComment: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  default: {
    getCurrentUser: vi.fn(),
    getUser: vi.fn(),
  },
}));

describe('IncidentCommentsSection', () => {
  const sampleComments = [
    {
      id: 1,
      incidentId: 10,
      authorId: 2,
      authorEmail: 'operador@malaga.es',
      authorName: 'Carlos Operador',
      authorRole: 'OPERATOR',
      comment: 'Revisión inicial del aviso sobre el terreno completada.',
      isInternal: false,
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: null,
    },
    {
      id: 2,
      incidentId: 10,
      authorId: 3,
      authorEmail: 'tecnico@malaga.es',
      authorName: 'Laura Técnica',
      authorRole: 'TECHNICIAN',
      comment: 'Nota técnica interna: Se requiere reemplazo de cableado subterráneo de media tensión.',
      isInternal: true,
      createdAt: '2026-11-01T11:00:00Z',
      updatedAt: '2026-11-01T11:30:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    triageService.getIncidentComments.mockResolvedValue(sampleComments);
    authService.getCurrentUser.mockReturnValue({
      id: 2,
      email: 'operador@malaga.es',
      role: 'OPERATOR',
    });
  });

  it('renderiza la lista de comentarios diferenciando notas internas y avisos públicos', async () => {
    render(<IncidentCommentsSection incidentId={10} incidentStatus="ASSIGNED" />);

    await waitFor(() => {
      expect(screen.getByText('Revisión inicial del aviso sobre el terreno completada.')).toBeInTheDocument();
    });

    expect(screen.getByText('Nota técnica interna: Se requiere reemplazo de cableado subterráneo de media tensión.')).toBeInTheDocument();

    // Comprobar badges de visibilidad
    const publicBadges = screen.getAllByText('Aviso Público');
    expect(publicBadges.length).toBeGreaterThanOrEqual(1);

    const internalBadges = screen.getAllByText('Nota Interna');
    expect(internalBadges.length).toBeGreaterThanOrEqual(1);

    // Nombres y roles
    expect(screen.getByText('Carlos Operador')).toBeInTheDocument();
    expect(screen.getByText('Laura Técnica')).toBeInTheDocument();
  });

  it('muestra el indicador de edición cuando updatedAt es diferente de createdAt', async () => {
    render(<IncidentCommentsSection incidentId={10} incidentStatus="ASSIGNED" />);

    await waitFor(() => {
      expect(screen.getByText('Laura Técnica')).toBeInTheDocument();
    });

    // El comentario con id 2 tiene updatedAt diferente
    expect(screen.getByText(/\(Editado el/i)).toBeInTheDocument();
  });

  it('permite editar un comentario al autor del mismo', async () => {
    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="ASSIGNED"
        currentUser={{ id: 2, email: 'operador@malaga.es', role: 'OPERATOR' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Carlos Operador')).toBeInTheDocument();
    });

    // El usuario con id: 2 es el autor del comentario 1, pero no del 2
    const editButtons = screen.getAllByRole('button', { name: /Editar/i });
    expect(editButtons.length).toBe(1);

    // Iniciar edición
    fireEvent.click(editButtons[0]);

    // Aparece el textarea de edición con el texto actual
    const editTextarea = screen.getByDisplayValue('Revisión inicial del aviso sobre el terreno completada.');
    expect(editTextarea).toBeInTheDocument();

    // Modificar texto
    fireEvent.change(editTextarea, {
      target: { value: 'Revisión inicial completada con éxito.' },
    });

    // Mock respuesta de actualización
    triageService.updateIncidentComment.mockResolvedValueOnce({
      id: 1,
      comment: 'Revisión inicial completada con éxito.',
      updatedAt: '2026-11-01T12:00:00Z',
    });

    // Guardar
    const saveButton = screen.getByRole('button', { name: /Guardar/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(triageService.updateIncidentComment).toHaveBeenCalledWith(
        10,
        1,
        { comment: 'Revisión inicial completada con éxito.' }
      );
    });

    await waitFor(() => {
      expect(screen.getByText('Revisión inicial completada con éxito.')).toBeInTheDocument();
    });
  });

  it('permite a un usuario ADMIN editar cualquier comentario aunque no sea el autor', async () => {
    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="ASSIGNED"
        currentUser={{ id: 99, email: 'admin@malaga.es', role: 'ADMIN' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Carlos Operador')).toBeInTheDocument();
    });

    // El admin ve botones de editar para todos los comentarios
    const editButtons = screen.getAllByRole('button', { name: /Editar/i });
    expect(editButtons.length).toBe(2);
  });

  it('deniega la edición a un tercero que no es autor ni ADMIN', async () => {
    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="ASSIGNED"
        currentUser={{ id: 50, email: 'otro@malaga.es', role: 'TECHNICIAN' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Carlos Operador')).toBeInTheDocument();
    });

    // Ningún botón de editar visible
    expect(screen.queryByRole('button', { name: /Editar/i })).not.toBeInTheDocument();
  });

  it('bloquea la creación y edición de comentarios si la incidencia está CLOSED o REJECTED', async () => {
    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="CLOSED"
        isClosedOrRejected={true}
        currentUser={{ id: 2, email: 'operador@malaga.es', role: 'OPERATOR' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/La incidencia está cerrada o rechazada\. No se admiten nuevos comentarios\./i)).toBeInTheDocument();
    });

    // Formulario de nuevo comentario bloqueado / no mostrado
    expect(screen.queryByRole('button', { name: /Publicar comentario/i })).not.toBeInTheDocument();

    // Tampoco permite editar comentarios anteriores
    expect(screen.queryByRole('button', { name: /Editar/i })).not.toBeInTheDocument();
  });

  it('permite al personal municipal publicar un nuevo comentario o nota interna', async () => {
    const handleCommentAdded = vi.fn();

    triageService.addIncidentComment.mockResolvedValueOnce({
      id: 3,
      incidentId: 10,
      authorId: 2,
      authorName: 'Carlos Operador',
      authorRole: 'OPERATOR',
      comment: 'Nuevo aviso público de finalización preliminar.',
      isInternal: false,
      createdAt: '2026-11-01T13:00:00Z',
      updatedAt: null,
    });

    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="IN_PROGRESS"
        currentUser={{ id: 2, email: 'operador@malaga.es', role: 'OPERATOR' }}
        onCommentAdded={handleCommentAdded}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Añadir Comentario o Nota Operativa/i)).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText(/Añadir Comentario o Nota Operativa/i);
    const internalCheckbox = screen.getByRole('checkbox', { name: /Nota interna/i });
    const submitBtn = screen.getByRole('button', { name: /Publicar comentario/i });

    // Escribir comentario
    fireEvent.change(textarea, {
      target: { value: 'Nuevo aviso público de finalización preliminar.' },
    });

    // Publicar sin marcar nota interna (es aviso público)
    expect(internalCheckbox.checked).toBe(false);
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(triageService.addIncidentComment).toHaveBeenCalledWith(10, {
        comment: 'Nuevo aviso público de finalización preliminar.',
        isInternal: false,
      });
    });

    expect(handleCommentAdded).toHaveBeenCalledTimes(1);
    await waitFor(() => {
      expect(screen.getByText('Nuevo aviso público de finalización preliminar.')).toBeInTheDocument();
    });
  });

  it('permite publicar una nota interna marcando el selector de visibilidad', async () => {
    triageService.addIncidentComment.mockResolvedValueOnce({
      id: 4,
      incidentId: 10,
      authorId: 2,
      authorName: 'Carlos Operador',
      authorRole: 'OPERATOR',
      comment: 'Nota interna confidencial sobre piezas defectuosas.',
      isInternal: true,
      createdAt: '2026-11-01T14:00:00Z',
      updatedAt: null,
    });

    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="IN_PROGRESS"
        currentUser={{ id: 2, email: 'operador@malaga.es', role: 'OPERATOR' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Añadir Comentario o Nota Operativa/i)).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText(/Añadir Comentario o Nota Operativa/i);
    const internalCheckbox = screen.getByRole('checkbox', { name: /Nota interna/i });
    const submitBtn = screen.getByRole('button', { name: /Publicar comentario/i });

    fireEvent.change(textarea, {
      target: { value: 'Nota interna confidencial sobre piezas defectuosas.' },
    });
    fireEvent.click(internalCheckbox);
    expect(internalCheckbox.checked).toBe(true);

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(triageService.addIncidentComment).toHaveBeenCalledWith(10, {
        comment: 'Nota interna confidencial sobre piezas defectuosas.',
        isInternal: true,
      });
    });
  });

  it('no muestra el formulario de redacción a ciudadanos pero sí el aviso informativo', async () => {
    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="IN_PROGRESS"
        currentUser={{ id: 1, email: 'ciudadano@malaga.es', role: 'CITIZEN' }}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Revisión inicial del aviso sobre el terreno completada.')).toBeInTheDocument();
    });

    // Formulario de nuevo comentario no disponible para ciudadanos
    expect(screen.queryByLabelText(/Añadir Comentario o Nota Operativa/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Publicar comentario/i })).not.toBeInTheDocument();

    // Mensaje informativo visible
    expect(screen.getByText(/Los avisos oficiales e informes de actuación/i)).toBeInTheDocument();
  });

  it('muestra mensaje amigable y opción de reintento si falla la carga de comentarios', async () => {
    triageService.getIncidentComments.mockRejectedValueOnce(
      new Error('Fallo de red al consultar comentarios')
    );

    render(
      <IncidentCommentsSection
        incidentId={10}
        incidentStatus="ASSIGNED"
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/No se pudieron cargar los comentarios/i)).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole('button', { name: /Reintentar/i });
    expect(retryBtn).toBeInTheDocument();

    triageService.getIncidentComments.mockResolvedValueOnce(sampleComments);
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Revisión inicial del aviso sobre el terreno completada.')).toBeInTheDocument();
    });
  });
});
