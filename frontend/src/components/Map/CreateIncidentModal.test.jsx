import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import CreateIncidentModal from './CreateIncidentModal'
import incidentService from '../../services/incidentService'

vi.mock('../../services/incidentService', () => ({
  default: {
    createIncident: vi.fn(),
    uploadIncidentAttachment: vi.fn(),
    reverseGeocode: vi.fn(),
  },
}))

describe('CreateIncidentModal', () => {
  const mockLocation = {
    latitude: 36.7213,
    longitude: -4.4214,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:http://localhost/fake-preview')
    globalThis.URL.revokeObjectURL = vi.fn()
    incidentService.reverseGeocode.mockResolvedValue({
      address: null,
      district: null,
      neighborhood: null,
    })
  })

  it('permite crear una incidencia sin adjuntar ninguna imagen (0 adjuntos)', async () => {
    const handleCreated = vi.fn()
    const handleClose = vi.fn()

    incidentService.createIncident.mockResolvedValueOnce({
      id: 99,
      title: 'Semáforo averiado',
      description: 'El semáforo parpadea constantemente en ámbar.',
      category: 'Alumbrado',
      status: 'REPORTED',
      priority: 'MEDIUM',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onCreated={handleCreated}
        onClose={handleClose}
      />
    )

    fireEvent.change(screen.getByLabelText(/Título/i), {
      target: { value: 'Semáforo averiado' },
    })

    fireEvent.change(screen.getByLabelText(/Categoría/i), {
      target: { value: 'Alumbrado' },
    })

    fireEvent.change(screen.getByLabelText(/Descripción/i), {
      target: { value: 'El semáforo parpadea constantemente en ámbar.' },
    })

    const submitBtn = screen.getByRole('button', { name: /Guardar incidencia/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(incidentService.createIncident).toHaveBeenCalledTimes(1)
      expect(incidentService.createIncident).toHaveBeenCalledWith({
        title: 'Semáforo averiado',
        description: 'El semáforo parpadea constantemente en ámbar.',
        category: 'Alumbrado',
        latitude: 36.7213,
        longitude: -4.4214,
      })
      // No debe haberse llamado a uploadIncidentAttachment
      expect(incidentService.uploadIncidentAttachment).not.toHaveBeenCalled()
      expect(handleCreated).toHaveBeenCalledTimes(1)
    })
  })

  it('muestra un error si el usuario selecciona un archivo con formato no permitido', async () => {
    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    const fileInput = screen.getByLabelText(/Seleccionar imagen adjunta/i)
    const invalidFile = new File(['dummy content'], 'report.pdf', { type: 'application/pdf' })

    fireEvent.change(fileInput, { target: { files: [invalidFile] } })

    expect(
      await screen.findByText(/Solo se aceptan imágenes JPEG y PNG/i)
    ).toBeInTheDocument()
  })

  it('permite adjuntar imagen válida, previsualizarla y enviarla tras crear la incidencia', async () => {
    const handleCreated = vi.fn()

    incidentService.createIncident.mockResolvedValueOnce({
      id: 101,
      title: 'Bache con fotografía',
      description: 'Gran socavón en el asfalto junto a la acera.',
      category: 'Vías Públicas',
    })

    incidentService.uploadIncidentAttachment.mockResolvedValueOnce({
      id: 5,
      incidentId: 101,
      fileName: 'bache.jpg',
      fileUrl: '/uploads/uuid-bache.jpg',
      mimeType: 'image/jpeg',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onCreated={handleCreated}
      />
    )

    fireEvent.change(screen.getByLabelText(/Título/i), {
      target: { value: 'Bache con fotografía' },
    })

    fireEvent.change(screen.getByLabelText(/Categoría/i), {
      target: { value: 'Vías Públicas' },
    })

    fireEvent.change(screen.getByLabelText(/Descripción/i), {
      target: { value: 'Gran socavón en el asfalto junto a la acera.' },
    })

    const fileInput = screen.getByLabelText(/Seleccionar imagen adjunta/i)
    const validFile = new File(['fake image data'], 'bache.jpg', { type: 'image/jpeg' })

    fireEvent.change(fileInput, { target: { files: [validFile] } })

    // Debe mostrar la vista previa
    expect(screen.getByAltText(/Vista previa de la fotografía adjunta/i)).toBeInTheDocument()
    expect(screen.getByText('bache.jpg')).toBeInTheDocument()

    const submitBtn = screen.getByRole('button', { name: /Guardar incidencia/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(incidentService.createIncident).toHaveBeenCalledTimes(1)
      expect(incidentService.uploadIncidentAttachment).toHaveBeenCalledTimes(1)
      expect(incidentService.uploadIncidentAttachment).toHaveBeenCalledWith(101, validFile)
      expect(handleCreated).toHaveBeenCalledTimes(1)
    })
  })

  it('invoca reverseGeocode al abrirse con coordenadas y muestra dirección, barrio y distrito resueltos', async () => {
    incidentService.reverseGeocode.mockResolvedValueOnce({
      address: 'Calle Marqués de Larios, 10',
      district: 'Centro',
      neighborhood: 'Centro Histórico',
    })

    incidentService.createIncident.mockResolvedValueOnce({
      id: 102,
      title: 'Farola rota',
      description: 'Cristal roto y bombilla desprendida en acera peatonal.',
      category: 'Alumbrado',
      address: 'Calle Marqués de Larios, 10',
      district: 'Centro',
      neighborhood: 'Centro Histórico',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    await waitFor(() => {
      expect(incidentService.reverseGeocode).toHaveBeenCalledTimes(1)
      expect(incidentService.reverseGeocode).toHaveBeenCalledWith(36.7213, -4.4214)
    })

    // Comprobar que los valores territoriales resueltos se muestran en pantalla
    expect(await screen.findByDisplayValue('Calle Marqués de Larios, 10')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Centro')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Centro Histórico')).toBeInTheDocument()

    // Enviar el formulario y verificar que se incluyen los datos territoriales
    fireEvent.change(screen.getByLabelText(/Título/i), {
      target: { value: 'Farola rota' },
    })
    fireEvent.change(screen.getByLabelText(/Categoría/i), {
      target: { value: 'Alumbrado' },
    })
    fireEvent.change(screen.getByLabelText(/Descripción/i), {
      target: { value: 'Cristal roto y bombilla desprendida en acera peatonal.' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Guardar incidencia/i }))

    await waitFor(() => {
      expect(incidentService.createIncident).toHaveBeenCalledWith({
        title: 'Farola rota',
        description: 'Cristal roto y bombilla desprendida en acera peatonal.',
        category: 'Alumbrado',
        latitude: 36.7213,
        longitude: -4.4214,
        address: 'Calle Marqués de Larios, 10',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      })
    })
  })

  it('verifica que los campos de dirección, barrio y distrito son de sólo lectura', async () => {
    incidentService.reverseGeocode.mockResolvedValueOnce({
      address: 'Calle Granada, 22',
      district: 'Centro',
      neighborhood: 'La Judería',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    const addressInput = await screen.findByLabelText(/Dirección postal/i)
    const neighborhoodInput = screen.getByLabelText(/Barrio/i)
    const districtInput = screen.getByLabelText(/Distrito/i)

    expect(addressInput).toHaveAttribute('readonly')
    expect(neighborhoodInput).toHaveAttribute('readonly')
    expect(districtInput).toHaveAttribute('readonly')
  })

  it('muestra "Sin asignar" cuando reverseGeocode retorna campos nulos', async () => {
    incidentService.reverseGeocode.mockResolvedValueOnce({
      address: null,
      district: null,
      neighborhood: null,
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    await waitFor(() => {
      expect(incidentService.reverseGeocode).toHaveBeenCalledTimes(1)
    })

    // Comprobar que los inputs muestran el valor "Sin asignar"
    const unassignedInputs = await screen.findAllByDisplayValue('Sin asignar')
    expect(unassignedInputs.length).toBe(3)

    // Comprobar que las etiquetas informativas "Sin asignar" se renderizan
    const unassignedBadges = screen.getAllByText('Sin asignar')
    expect(unassignedBadges.length).toBeGreaterThanOrEqual(3)
  })

  it('muestra "Sin asignar" si la llamada a reverseGeocode falla', async () => {
    incidentService.reverseGeocode.mockRejectedValueOnce(new Error('Servicio de geocodificación no disponible'))

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    await waitFor(() => {
      expect(incidentService.reverseGeocode).toHaveBeenCalledTimes(1)
    })

    const unassignedInputs = await screen.findAllByDisplayValue('Sin asignar')
    expect(unassignedInputs.length).toBe(3)
  })

  it('descarta los datos del formulario y llama a onClose al pulsar Cancelar', async () => {
    const handleClose = vi.fn()

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onClose={handleClose}
      />
    )

    await waitFor(() => {
      expect(incidentService.reverseGeocode).toHaveBeenCalled()
    })

    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i })
    fireEvent.click(cancelBtn)

    expect(handleClose).toHaveBeenCalledTimes(1)
  })

  it('descarta los datos del formulario y llama a onCancel cuando no se pasa onClose', async () => {
    const handleCancel = vi.fn()

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onCancel={handleCancel}
      />
    )

    await waitFor(() => {
      expect(incidentService.reverseGeocode).toHaveBeenCalled()
    })

    const closeBtn = screen.getByLabelText(/Cerrar modal/i)
    fireEvent.click(closeBtn)

    expect(handleCancel).toHaveBeenCalledTimes(1)
  })

  it('valida que los campos requeridos no estén vacíos antes de enviar', async () => {
    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    const submitBtn = screen.getByRole('button', { name: /Guardar incidencia/i })
    fireEvent.click(submitBtn)

    expect(await screen.findByText('El título de la incidencia es obligatorio.')).toBeInTheDocument()
    expect(screen.getByText('Debe seleccionar una categoría del catálogo.')).toBeInTheDocument()
    expect(screen.getByText('La descripción es obligatoria.')).toBeInTheDocument()

    expect(incidentService.createIncident).not.toHaveBeenCalled()
  })

  it('muestra el indicador de carga mientras resuelve la dirección', async () => {
    incidentService.reverseGeocode.mockReturnValueOnce(new Promise(() => {}))

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    await waitFor(() => {
      expect(screen.getAllByText(/Resolviendo dirección.../i).length).toBeGreaterThanOrEqual(1)
    })
  })
})
