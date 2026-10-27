import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import RoleGuard from './RoleGuard'

function HomeStub() {
  const location = useLocation()
  return (
    <div>
      <span>Vista Home</span>
      {location.state?.accessDenied && (
        <p>{location.state.message}</p>
      )}
    </div>
  )
}

describe('RoleGuard', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirige a / con mensaje de acceso denegado si el usuario no tiene el rol permitido', () => {
    localStorage.setItem('user', JSON.stringify({ role: 'CITIZEN' }))

    render(
      <MemoryRouter initialEntries={['/panel']}>
        <Routes>
          <Route path="/" element={<HomeStub />} />
          <Route
            path="/panel"
            element={
              <RoleGuard allowedRoles={['OPERATOR', 'ADMIN']}>
                <div>Panel Administrativo</div>
              </RoleGuard>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Vista Home')).toBeInTheDocument()
    expect(screen.getByText(/Acceso denegado/i)).toBeInTheDocument()
    expect(screen.queryByText('Panel Administrativo')).not.toBeInTheDocument()
  })

  it('permite acceso y muestra contenido si el usuario tiene uno de los roles autorizados', () => {
    localStorage.setItem('user', JSON.stringify({ role: 'OPERATOR' }))

    render(
      <MemoryRouter initialEntries={['/panel']}>
        <Routes>
          <Route path="/" element={<HomeStub />} />
          <Route
            path="/panel"
            element={
              <RoleGuard allowedRoles={['OPERATOR', 'ADMIN']}>
                <div>Panel Administrativo</div>
              </RoleGuard>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Panel Administrativo')).toBeInTheDocument()
    expect(screen.queryByText('Vista Home')).not.toBeInTheDocument()
    expect(screen.queryByText(/Acceso denegado/i)).not.toBeInTheDocument()
  })
})
