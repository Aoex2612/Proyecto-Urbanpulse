import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ProtectedRoute from './ProtectedRoute'

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirige a /login cuando no existe token en localStorage', () => {
    render(
      <MemoryRouter initialEntries={['/panel']}>
        <Routes>
          <Route path="/login" element={<div>Vista de Login</div>} />
          <Route
            path="/panel"
            element={
              <ProtectedRoute>
                <div>Contenido Privado</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Vista de Login')).toBeInTheDocument()
    expect(screen.queryByText('Contenido Privado')).not.toBeInTheDocument()
  })

  it('permite renderizar los hijos cuando existe token valido', () => {
    localStorage.setItem('token', 'fake-jwt-token')

    render(
      <MemoryRouter initialEntries={['/panel']}>
        <Routes>
          <Route
            path="/panel"
            element={
              <ProtectedRoute>
                <div>Contenido Privado</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Contenido Privado')).toBeInTheDocument()
  })
})
