# Componente Frontend NotificationBell y Servicio de Notificaciones (HU21)

## Resumen del Componente
Implementación de la campana interactiva in-app (`NotificationBell.jsx`) y el cliente HTTP Axios (`notificationService.js`) para la visualización, sondeo y gestión individual de avisos en tiempo real en UrbanPulse.

## Funcionalidades Clave
1. **Badge Numérico Reactivo:** Muestra el conteo de notificaciones no leídas (`unreadCount`) mediante consulta periódica (polling cada 60 segundos).
2. **Menú Desplegable:** Presenta el histórico cronológico de avisos clasificados visualmente con punto azul para las no leídas.
3. **Marcado Individual:** Botón contextual en cada notificación pendiente para actualizar su estado a leído mediante `PATCH /api/notifications/{id}/read`.
4. **Navegación Asistida:** Soporta prop `onIncidentClick` para abrir directamente la ficha o modal de detalle de la incidencia vinculada al hacer clic sobre el título del aviso.
5. **Integración Transversal:** Se inserta en la barra de navegación principal `Navbar.jsx`, accesible desde todas las rutas autenticadas.
