# Arquitectura del Sistema: UrbanPulse

Documento oficial de arquitectura de **UrbanPulse**, diseñado bajo el modelo **C4 (Contexto, Contenedores y Componentes)** para representar la organización estática y dinámica de la plataforma conforme a la constitución del proyecto y el código fuente real implementado.

---

## 1. Diagrama de Contexto (C4: Nivel 1)

El modelo de contexto identifica a los diferentes usuarios (actores) que interactúan con la plataforma UrbanPulse y las dependencias con los sistemas externos o fuentes de datos del entorno urbano de Málaga.

```mermaid
flowchart TD
    subgraph Actores["Actores del Sistema"]
        Citizen["👤 Ciudadano (CITIZEN)<br/>Registra avisos geolocalizados y hace seguimiento"]
        Operator["👤 Operador Municipal (OPERATOR)<br/>Valida, prioriza y gestiona el ciclo de vida"]
        Technician["👤 Técnico (TECHNICIAN)<br/>Atiende incidencias y documenta actuaciones"]
        Admin["👤 Administrador (ADMIN)<br/>Gobierno de usuarios y configuración"]
        Analyst["👤 Analista (ANALYST)<br/>Consulta métricas e indicadores de respuesta"]
    end

    System["🏢 UrbanPulse<br/><b>Sistema Central de Gestión de Incidencias Urbanas</b><br/>Gestión en tiempo real, ciclo de vida, trazabilidad y geolocalización"]

    subgraph Externos["Sistemas Externos y Fuentes de Datos"]
        OSM["🗺️ OpenStreetMap / Cartografía Abierta<br/>Provisión de teselas de mapa y geocodificación"]
        MalagaOpenData["🏛️ Portal Málaga Open Data (Málaga24h / WFS)<br/>Catálogos de activos urbanos y datos territoriales"]
        WeatherAPI["🌤️ Servicio Meteorológico (AEMET / OpenMeteo)<br/>Enriquecimiento contextual de observaciones"]
        NotifService["📧 Servicio de Notificaciones (Email / Push)<br/>Avisos ante cambios de estado de incidencias"]
    end

    Citizen -->|"Registra y consulta incidencias [HTTPS]"| System
    Operator -->|"Valida, clasifica y actualiza estados [HTTPS]"| System
    Technician -->|"Consulta asignaciones y resuelve avisos [HTTPS]"| System
    Admin -->|"Administra usuarios y parámetros [HTTPS]"| System
    Analyst -->|"Analiza patrones y tiempos de respuesta [HTTPS]"| System

    System -->|"Renderiza teselas y georreferencia incidencias [HTTPS]"| OSM
    System -->|"Consulta activos municipales e información territorial [REST / HTTPS]"| MalagaOpenData
    System -->|"Consulta condiciones climatológicas [REST / HTTPS]"| WeatherAPI
    System -->|"Despacha avisos y actualizaciones [HTTPS / SMTP]"| NotifService
```

### Actores y Sistemas Externos (Contexto)

| Elemento | Tipo | Descripción y Responsabilidad |
|---|---|---|
| **Ciudadano (`CITIZEN`)** | Persona | Registra reportes con ubicación geográfica y evidencias multimedia, y consulta su historial y avance en tiempo real. |
| **Operador municipal (`OPERATOR`)** | Persona | Valida, clasifica, prioriza incidencias y actualiza su estado operativo a lo largo del flujo de trabajo. |
| **Técnico (`TECHNICIAN`)** | Persona | Consulta asignaciones de trabajo sobre el terreno, documenta actuaciones y marca resoluciones. |
| **Administrador (`ADMIN`)** | Persona | Gestiona usuarios, permisos, roles, departamentos y parámetros del sistema. |
| **Analista (`ANALYST`)** | Persona | Analiza métricas operativas, tiempos de atención, degradación de equipamiento y distribución territorial de avisos. |
| **Sistema UrbanPulse** | Software System | Plataforma central desacoplada que gestiona el ciclo de vida, la georreferenciación y la trazabilidad inmutable de incidencias urbanas. |
| **OpenStreetMap / Cartografía** | Sistema Externo | Suministra teselas cartográficas y soporte geoespacial para la visualización y geocodificación. |
| **Málaga Open Data / Datos Urbanos** | Sistema Externo | Provee datos del término municipal de Málaga (barrios, distritos y catálogo de activos de infraestructura municipal). |
| **Servicio Meteorológico** | Sistema Externo | Provee datos del clima para enriquecer contextualmente las observaciones asociadas a incidencias. |
| **Servicio de Notificaciones** | Sistema Externo | Canal para la entrega de correos electrónicos y alertas ante cambios de estado en las incidencias. |

### Relaciones del Contexto

| Origen | Destino | Interacción / Propósito | Protocolo / Formato |
|---|---|---|---|
| Ciudadano | Sistema UrbanPulse | Registra reportes y consulta el estado de sus avisos | HTTPS / JSON |
| Operador municipal | Sistema UrbanPulse | Valida, clasifica y gestiona transiciones de estado | HTTPS / JSON |
| Técnico | Sistema UrbanPulse | Consulta tareas de campo y actualiza estado de resolución | HTTPS / JSON |
| Administrador | Sistema UrbanPulse | Administra cuentas de usuario y configuraciones globales | HTTPS / JSON |
| Analista | Sistema UrbanPulse | Consulta analíticas de rendimiento y distribución espacial | HTTPS / JSON |
| Sistema UrbanPulse | OpenStreetMap | Renderiza mapas interactivos y capas cartográficas | HTTPS / Teselas Web |
| Sistema UrbanPulse | Málaga Open Data | Consulta activos urbanos y datos territoriales | HTTPS / REST / WFS |
| Sistema UrbanPulse | Servicio Meteorológico | Obtiene condiciones meteorológicas históricas o en tiempo real | HTTPS / REST API |
| Sistema UrbanPulse | Servicio de Notificaciones | Despacha notificaciones a los interesados | HTTPS / SMTP |

---

## 2. Diagrama de Contenedores (C4: Nivel 2)

Estructura de contenedores de software que componen la plataforma UrbanPulse, detallando responsabilidades tecnológicas, vías de comunicación y fronteras de persistencia.

```mermaid
flowchart TD
    subgraph Usuarios["Usuarios"]
        UserBrowser["🌐 Navegador Web del Usuario<br/>(Ciudadano, Operador, Técnico, Admin)"]
    end

    subgraph UrbanPulseSystem["Contenedores UrbanPulse"]
        Frontend["💻 Web Frontend (SPA)<br/><b>React 19 + Vite 6 + React Router 7</b><br/>Interfaz de usuario responsiva, mapas Leaflet y gestión de sesión"]
        Backend["⚙️ Backend API REST<br/><b>Java 21 + Spring Boot 3.4.3</b><br/>Lógica de negocio, control de acceso RBAC, JWT, validaciones y mappers"]
        Database[("🗄️ Base de Datos Relacional<br/><b>PostgreSQL 16 (Alpine)</b><br/>Persistencia relacional, tablas de incidencias, usuarios, trazabilidad inmutable")]
    end

    subgraph ServiciosExternos["Sistemas y APIs Externas"]
        ExtOSM["🗺️ OpenStreetMap Tile Server"]
        ExtMalaga["🏛️ Málaga Open Data Portal"]
        ExtWeather["🌤️ OpenMeteo / Weather API"]
        ExtMail["📧 Notification Service"]
    end

    UserBrowser -->|"Interacciona con la aplicación [HTTPS]"| Frontend
    Frontend -->|"Peticiones API REST (Bearer JWT) [HTTPS / JSON]"| Backend
    Frontend -->|"Descarga teselas de mapa [HTTPS]"| ExtOSM

    Backend -->|"Lectura y persistencia transaccional [JDBC / JPA]"| Database
    Backend -->|"Consulta datos territoriales y activos [HTTPS / REST]"| ExtMalaga
    Backend -->|"Consulta contexto meteorológico [HTTPS / REST]"| ExtWeather
    Backend -->|"Envío de avisos y correos [SMTP / HTTPS]"| ExtMail
```

### Contenedores Principales

| Contenedor / Servicio | Tecnología | Responsabilidad |
|---|---|---|
| **Web Frontend (SPA)** | React 19, Vite 6, React Router 7, Leaflet 1.9, Axios | Aplicación cliente en el navegador. Proporciona interfaz interactiva con mapas, formularios georreferenciados, paneles de control según RBAC y trazabilidad histórica. |
| **Backend API REST** | Java 21, Spring Boot 3.4.3, Spring Security 6, JJWT, Spring Data JPA | Núcleo del servidor: autenticación perimetral sin estado (Stateless JWT), control de acceso por roles, lógica de incidencias, transformaciones DTOs/entidades con mappers y transacciones JPA. |
| **Base de Datos Relacional** | PostgreSQL 16 (Alpine) con Docker Compose | Almacenamiento persistente de datos relacionales: cuentas de usuario, incidencias geolocalizadas, histórico inmutable de cambios de estado (`status_changes`), adjuntos y asociaciones a activos. |
| **OpenStreetMap Tile Server** | Externo (HTTPS) | Suministro de capas cartográficas para la representación gráfica en Leaflet. |
| **Málaga Open Data** | Externo (REST API / GeoJSON) | Proveedor de datos abiertos municipales de Málaga para activos y áreas urbanas. |
| **Servicio Meteorológico** | Externo (REST API) | Suministro de datos meteorológicos para contextualización de observaciones en vía pública. |
| **Servicio de Notificaciones** | Externo (SMTP / HTTPS) | Infraestructura para el envío de notificaciones y confirmaciones. |

### Relaciones de Contenedores

| Origen | Destino | Interacción / Propósito | Protocolo / Transporte |
|---|---|---|---|
| Navegador Web | Web Frontend (SPA) | Renderiza páginas, paneles y controles de interfaz | HTTPS / HTML / JS |
| Web Frontend | Backend API REST | Peticiones autenticadas de login, registro, consulta y mutación de incidencias | HTTPS / JSON / Bearer JWT |
| Web Frontend | OpenStreetMap | Carga de mapas base y teselas cartográficas | HTTPS / PNG Tiles |
| Backend API REST | PostgreSQL | Persistencia relacional y transaccional mediante repositorios Spring Data JPA | JDBC / TCP (Puerto 5432) |
| Backend API REST | Málaga Open Data | Enriquecimiento con activos e información geográfica local | HTTPS / REST / JSON |
| Backend API REST | Servicio Meteorológico | Consulta contextual de condiciones climáticas | HTTPS / REST API |
| Backend API REST | Servicio de Notificaciones | Despacho de notificaciones por eventos del ciclo de vida | HTTPS / SMTP |

---

## 3. Diagramas de Componentes (C4: Nivel 3)

### 3.1. Componentes del Contenedor Web Frontend (React 19)

Desglose de los componentes, páginas, servicios y utilidades del cliente web, alineados con la estructura real del directorio `frontend/src/`.

```mermaid
flowchart TD
    subgraph UI_Pages["Vistas y Páginas (src/pages)"]
        HomePage["HomePage<br/>Presentación y navegación principal"]
        LoginPage["LoginPage<br/>Formulario de autenticación"]
        RegisterPage["RegisterPage<br/>Formulario de registro ciudadano"]
        MapPage["MapPage<br/>Visualización global y filtrado geográfico"]
        MyIncidentsPage["MyIncidentsPage<br/>Listado y seguimiento del ciudadano"]
        OperatorPanelPage["OperatorPanelPage<br/>Bandeja de gestión del operador"]
    end

    subgraph UI_Guards["Seguridad y Rutas (src/components)"]
        ProtectedRoute["ProtectedRoute<br/>Verificación de sesión activa"]
        RoleGuard["RoleGuard<br/>Control de acceso por rol RBAC"]
    end

    subgraph UI_Components["Componentes Reutilizables (src/components)"]
        IncidentsMap["IncidentsMap<br/>Mapa Leaflet interactivo con pines y clusters"]
        CreateIncidentModal["CreateIncidentModal<br/>Modal de alta con geolocalización"]
        IncidentDetailModal["IncidentDetailModal<br/>Detalle completo de incidencia"]
        MapDateFilter["MapDateFilter<br/>Filtro temporal de incidencias"]
        StatusTimeline["StatusTimeline<br/>Línea temporal de cambios de estado"]
    end

    subgraph UI_Services["Capa de Servicios y API (src/services, src/utils)"]
        AuthService["authService.js<br/>Gestión de token JWT y credenciales"]
        IncidentService["incidentService.js<br/>Llamadas al catálogo y ciclo de vida"]
        ApiClient["api.js (Axios Instance)<br/>Interceptor Authorization Bearer"]
        DateUtils["dateUtils.js<br/>Formateo y normalización temporal"]
        LeafletIcons["leafletIcons.js<br/>Iconos por estado y categoría"]
    end

    LoginPage --> AuthService
    RegisterPage --> AuthService
    ProtectedRoute --> AuthService
    RoleGuard --> AuthService

    HomePage --> IncidentsMap
    MapPage --> IncidentsMap
    MapPage --> MapDateFilter
    IncidentsMap --> IncidentDetailModal
    IncidentsMap --> CreateIncidentModal
    IncidentsMap --> LeafletIcons

    MyIncidentsPage --> IncidentService
    MyIncidentsPage --> StatusTimeline
    OperatorPanelPage --> IncidentService
    OperatorPanelPage --> StatusTimeline
    IncidentDetailModal --> StatusTimeline

    CreateIncidentModal --> IncidentService
    IncidentDetailModal --> IncidentService

    IncidentService --> ApiClient
    AuthService --> ApiClient
    StatusTimeline --> DateUtils
    MyIncidentsPage --> DateUtils
```

#### Componentes del Frontend

| Componente / Módulo | Ubicación en Código | Responsabilidad |
|---|---|---|
| **`LoginPage` / `RegisterPage`** | `src/pages/` | Vistas de autenticación y registro de ciudadanos con validaciones en cliente. |
| **`HomePage` / `MapPage`** | `src/pages/` | Vistas públicas y operativas que integran el mapa de incidencias, filtros y accesos directos. |
| **`MyIncidentsPage`** | `src/pages/` | Vista privada del ciudadano para consultar sus reportes, estados e historial inmutable. |
| **`OperatorPanelPage`** | `src/pages/` | Panel operativo con permisos `OPERATOR`/`ADMIN` para validar, clasificar y transicionar incidencias. |
| **`ProtectedRoute` / `RoleGuard`** | `src/components/` | Componentes de guardia que protegen rutas privadas y verifican roles autorizados (`CITIZEN`, `OPERATOR`, `ADMIN`). |
| **`IncidentsMap`** | `src/components/Map/` | Componente central cartográfico basado en React-Leaflet; gestiona marcadores, eventos de clic y selección. |
| **`CreateIncidentModal`** | `src/components/Map/` | Modal emergente con captura de coordenadas, título, descripción, categoría y subida de archivos adjuntos. |
| **`IncidentDetailModal`** | `src/components/Map/` | Modal de inspección de detalles de una incidencia, observaciones, adjuntos y trazabilidad de estados. |
| **`StatusTimeline`** | `src/components/Timeline/` | Línea temporal que visualiza el historial auditable de transiciones de estado de una incidencia. |
| **`authService`** | `src/services/authService.js` | Métodos de login, logout, registro y almacenamiento seguro del token JWT en sesión. |
| **`incidentService`** | `src/services/incidentService.js` | Operaciones CRUD y de gestión: listado paginado, detalle, creación, cambio de estado y geocodificación inversa. |
| **`api` (Axios Client)** | `src/services/api.js` | Cliente HTTP centralizado con interceptores que inyectan la cabecera `Authorization: Bearer <token>` y manejan códigos de error. |
| **`leafletIcons` / `dateUtils`** | `src/utils/` | Utilidades de renderizado de marcadores Leaflet según estado y formateo de fechas. |

---

### 3.2. Componentes del Contenedor Backend API (Spring Boot 3.4.3)

Desglose de la arquitectura por capas del servidor, reflejando fielmente los paquetes y clases bajo `Backend/src/main/java/org/bulbaseros/`.

```mermaid
flowchart TD
    subgraph SecurityLayer["Capa de Seguridad (org.bulbaseros.security & config)"]
        SecurityConfig["SecurityConfig<br/>Configuración perimetral, CORS y RBAC"]
        JwtFilter["JwtAuthenticationFilter<br/>Filtro perimetral de inspección Bearer JWT"]
        JwtService["JwtService<br/>Generación y validación criptográfica HMAC-SHA256"]
        AuthEntryPoint["JwtAuthenticationEntryPoint & AccessDenied<br/>Manejadores 401 y 403"]
    end

    subgraph ControllerLayer["Capa de Controladores REST (org.bulbaseros.controller)"]
        AuthController["AuthController<br/>POST /api/auth/register, /api/auth/login"]
        IncidentController["IncidentController<br/>GET, POST, PATCH /api/incidents/**"]
        GlobalException["GlobalExceptionHandler<br/>Controlador @ControllerAdvice para respuestas 4xx/5xx"]
    end

    subgraph ServiceLayer["Capa de Servicios de Negocio (org.bulbaseros.service)"]
        AuthService["AuthService<br/>Lógica de autenticación, hash BCrypt y registro"]
        IncidentService["IncidentService<br/>Gestión del ciclo de vida, transiciones y permisos"]
        AttachmentService["AttachmentService<br/>Gestión y almacenamiento de archivos adjuntos"]
        MalagaOpenDataService["MalagaOpenDataService<br/>Integración con portal de datos abiertos y geocodificación"]
    end

    subgraph MapperLayer["Capa de Mappers y DTOs (org.bulbaseros.mappers & dto)"]
        IncidentMapper["IncidentMapper<br/>Incidencia <-> IncidentResponse / CreateIncidentRequest"]
        StatusChangeMapper["StatusChangeMapper<br/>CambiosEstado <-> StatusChangeResponse"]
        AttachmentMapper["AttachmentMapper<br/>ArchivoAdjunto <-> AttachmentResponse"]
    end

    subgraph RepositoryLayer["Capa de Acceso a Datos (org.bulbaseros.repository)"]
        UserRepo["UserRepository<br/>Consulta de usuarios por email"]
        IncidentRepo["IncidenciaRepository<br/>Consultas paginadas y filtros espaciales/estado"]
        CambiosEstadoRepo["CambiosEstadoRepository<br/>Trazabilidad inmutable de transiciones"]
        ArchivoAdjuntoRepo["ArchivoAdjuntoRepository<br/>Metadatos de evidencias multimedia"]
    end

    subgraph EntityLayer["Capa de Entidades JPA (org.bulbaseros.entity)"]
        UsuarioEntity["Usuario (users)"]
        IncidenciaEntity["Incidencia (incidents)"]
        CambiosEstadoEntity["CambiosEstado (status_changes)"]
        ArchivoAdjuntoEntity["ArchivoAdjunto (attachments)"]
    end

    JwtFilter --> JwtService
    JwtFilter --> UserRepo
    AuthController --> AuthService
    IncidentController --> IncidentService

    AuthService --> JwtService
    AuthService --> UserRepo
    AuthService --> SecurityConfig

    IncidentService --> MalagaOpenDataService
    IncidentService --> AttachmentService
    IncidentService --> IncidentMapper
    IncidentService --> StatusChangeMapper
    IncidentService --> IncidentRepo
    IncidentService --> CambiosEstadoRepo
    AttachmentService --> ArchivoAdjuntoRepo
    AttachmentService --> AttachmentMapper

    IncidentRepo --> IncidenciaEntity
    UserRepo --> UsuarioEntity
    CambiosEstadoRepo --> CambiosEstadoEntity
    ArchivoAdjuntoRepo --> ArchivoAdjuntoEntity
```

#### Componentes del Backend

| Componente / Clase | Paquete / Capa | Responsabilidad |
|---|---|---|
| **`AuthController`** | `org.bulbaseros.controller` | Endpoints REST públicos `/api/auth/register` y `/api/auth/login`. Valida peticiones DTO y delega en `AuthService`. |
| **`IncidentController`** | `org.bulbaseros.controller` | Endpoints REST `/api/incidents`: alta con adjuntos (`multipart/form-data`), listado paginado, filtrado por fechas/estado, detalle por ID, actualización de estado (RBAC `OPERATOR`/`ADMIN`) y geocodificación inversa. |
| **`GlobalExceptionHandler`** | `org.bulbaseros.exception` | Interceptor global `@ControllerAdvice` que unifica los formatos de error JSON (`ErrorResponse`) con códigos semánticos (400, 401, 403, 404, 409). |
| **`SecurityConfig`** | `org.bulbaseros.config` | Define la cadena de filtros `SecurityFilterChain`, CORS permisivo para desarrollo, política sin estado (`SessionCreationPolicy.STATELESS`) y reglas de acceso RBAC por ruta. |
| **`JwtAuthenticationFilter`** | `org.bulbaseros.security` | Extrae el header `Authorization: Bearer <token>`, valida la firma con `JwtService` y establece la autenticación en el `SecurityContextHolder`. |
| **`JwtService`** | `org.bulbaseros.security` | Generación, firma digital (HMAC-SHA256) y extracción de claims (email, rol) de los tokens de sesión. |
| **`AuthService`** | `org.bulbaseros.service` | Registro de usuarios con validación de email duplicado, cifrado de contraseñas con BCrypt y autenticación de credenciales. |
| **`IncidentService`** | `org.bulbaseros.service` | Orquestador central del dominio de incidencias. Controla validaciones, reglas de transición de estado, persistencia del histórico en `CambiosEstado`, enriquecimiento territorial y delegación en mappers. |
| **`AttachmentService`** | `org.bulbaseros.service` | Gestiona el almacenamiento físico/lógico de ficheros multimedia y persiste sus metadatos asociados a la incidencia. |
| **`MalagaOpenDataService`** | `org.bulbaseros.service` | Servicio de integración para geocodificación inversa y consulta de activos e infraestructura del término municipal de Málaga. |
| **`IncidentMapper`** | `org.bulbaseros.mappers` | Transforma entidades `Incidencia` a DTOs de salida (`IncidentResponse`, `IncidentStatusResponse`) y peticiones `CreateIncidentRequest` a entidades. Garantiza que las entidades JPA no salgan de la capa de servicio. |
| **`StatusChangeMapper`** | `org.bulbaseros.mappers` | Convierte registros inmutables de auditoría `CambiosEstado` en `StatusChangeResponse` para el frontend. |
| **`AttachmentMapper`** | `org.bulbaseros.mappers` | Transforma entidades `ArchivoAdjunto` a objetos `AttachmentResponse`. |
| **`IncidenciaRepository`** | `org.bulbaseros.repository` | Acceso Spring Data JPA a la tabla `incidents` con soporte para ordenación, paginación y filtros por rango de fechas o usuario reportador. |
| **`CambiosEstadoRepository`** | `org.bulbaseros.repository` | Repositorio JPA para la consulta ordenada de la trazabilidad inmutable de estados de cada incidencia. |
| **`UserRepository`** | `org.bulbaseros.repository` | Consulta de cuentas de usuario por email y verificación de existencia. |
| **`ArchivoAdjuntoRepository`** | `org.bulbaseros.repository` | Acceso JPA a la persistencia de metadatos de archivos multimedia vinculados a incidencias. |
| **`UrbanAssetService`** | `org.bulbaseros.service` | Cálculo de proximidad geodésica (Haversine en radio <= 50m), cálculo de confianza probabilística, asociación y gestión operativa de confirmación/desvinculación de activos urbanos. |
| **`ExternalContextService`** | `org.bulbaseros.service` | Orquestación resiliente de llamadas a Open-Meteo y conector de tráfico municipal, protegido con Circuit Breaker y fallback seguro. |
| **`CircuitBreaker`** | `org.bulbaseros.service.resilience` | Componente de resiliencia con estados CLOSED, OPEN y HALF-OPEN, ventana móvil y cooldown de 30 segundos para evitar saturación ante fallos externos. |
| **`ActivoUrbanoRepository`** | `org.bulbaseros.repository` | Consultas espaciales con prefiltrado por ventana de latitud y longitud sobre el catálogo de activos urbanos. |
| **`AsociacionIncidenciaActivoRepository`** | `org.bulbaseros.repository` | Persistencia y gestión del ciclo de vida de los vínculos incidencia-activo (`incident_assets`). |
| **`ObservacionesExternasRepository`** / **`ContextoIncidenciaRepository`** | `org.bulbaseros.repository` | Almacenamiento y consulta de observaciones contextuales externas (ambientales y de tráfico). |
| **`UrbanAssetMapper`** / **`UrbanContextMapper`** | `org.bulbaseros.mappers` | Transformación de entidades de activos y observaciones a DTOs de salida (`IncidentAssetResponse`, `ExternalObservationResponse`). |

---

## 4. Arquitectura de Enriquecimiento Espacial y Resiliencia (Feature 007)

UrbanPulse incorpora un subsistema de contextualización automática al registrar cada incidencia que enriquece el reporte ciudadano con información espacial del mobiliario urbano y condiciones ambientales en tiempo real, garantizando alta resiliencia y desacoplamiento de dependencias externas.

### 4.1. Detección y Asociación Geodésica de Activos Urbanos

Para evitar consultas espaciales costosas sobre toda la base de datos, el cálculo de proximidad implementa una estrategia en dos etapas:

1. **Prefiltrado Bounding Box (JPA):**
   - Se delimita una ventana de latitud y longitud equivalente a un radio de tolerancia inicial:
     $$\Delta \text{lat} \approx \frac{50}{111.320} \approx 0.00045^\circ$$
     $$\Delta \text{lon} \approx \frac{50}{111.320 \cdot \cos(\text{lat})} \approx 0.00056^\circ$$
   - Se ejecuta `ActivoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween` aprovechando los índices en BD.

2. **Cálculo Geodésico Exacto (Fórmula de Haversine):**
   - Sobre el subconjunto prefiltrado se calcula la distancia exacta sobre la esfera terrestre ($R = 6.371.000\text{ m}$):
     $$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)$$
     $$d = 2 R \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$
   - Se descartan aquellos activos con distancia $d > 50\text{ m}$.

3. **Scoring de Confianza Probabilística y Ordenación:**
   - A cada activo dentro del radio se le asigna una confianza decreciente linealmente con la distancia:
     $$\text{confidenceScore} = \max\left(0.0, \, 1.0 - \frac{d}{50.0}\right)$$
   - Los activos se ordenan de menor a mayor distancia antes de asociarse como sugeridos (`isConfirmed = false`). Los operadores municipales pueden posteriormente confirmar (`isConfirmed = true`) o desvincular activos mediante endpoints asegurados con RBAC.

```mermaid
flowchart TD
    Report[Ciudadano reporta incidencia] --> Coords[Coordenadas Lat/Lon WGS84]
    Coords --> BBox[1. Prefiltrado Bounding Box JPA +-0.0005 deg]
    BBox --> CandidateAssets[(urban_assets en caja)]
    CandidateAssets --> Haversine[2. Calculo Haversine d en metros]
    Haversine --> Filter{d <= 50m?}
    Filter -- No --> Discard[Descartar candidato]
    Filter -- Si --> Score[3. Calcular confianza: 1.0 - d/50]
    Score --> Sort[Ordenar por proximidad ascendente]
    Sort --> Persist[Asociar como sugerido: isConfirmed = false]
```

---

### 4.2. Ingesta Resiliente de Contexto Externo y Circuit Breaker

La integración con APIs externas (como el servicio meteorológico Open-Meteo y los conectores de tráfico municipal) está aislada del ciclo principal de guardado de incidencias para asegurar que **la creación de una incidencia nunca falle (201 Created) si un proveedor externo sufre cortes, latencias elevadas o indisponibilidad**.

#### Máquina de Estados del Circuit Breaker

El patrón Circuit Breaker opera con una ventana deslizante de fallos y un periodo de enfriamiento (cooldown):

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN : Tasa de fallos >= umbral (5 fallos en ventana) / Timeout > 3s
    OPEN --> HALF_OPEN : Transcurrido cooldown (30 segundos)
    HALF_OPEN --> CLOSED : Peticion de prueba exitosa
    HALF_OPEN --> OPEN : Peticion de prueba fallida
```

1. **Estado `CLOSED` (Normal):** Las llamadas a las APIs externas se ejecutan a través de un `RestTemplate` configurado con un timeout estricto de conexión y lectura de **3.000 ms**.
2. **Estado `OPEN` (Circuito Abierto):** Ante 5 fallos consecutivos (timeouts, errores HTTP 5xx o excepciones de red), el circuito se abre inmediatamente. Todas las peticiones posteriores se desvían de forma instantánea al método de **fallback** sin ejecutar llamadas HTTP, evitando el agotamiento de hilos del servidor.
3. **Estado `HALF_OPEN` (Prueba):** Tras **30 segundos** de periodo de cooldown, se permite una llamada de sondeo. Si tiene éxito, el circuito vuelve a `CLOSED`; si falla, regresa a `OPEN` reseteando el temporizador.

#### Estrategia de Fallback y Degradación Controlada

- Si el circuito está abierto, o si se produce un timeout o error HTTP durante la llamada, el conector captura la excepción, registra una traza descriptiva (`WARN / ERROR`) y retorna una colección vacía de observaciones externas sin interrumpir la transacción de la incidencia.
- En la interfaz de usuario (modal de detalle de incidencia), el componente `IncidentContextCard` detecta la ausencia de métricas y muestra un estado degradado informativo, garantizando que el ciudadano y el operador puedan visualizar y gestionar la incidencia con normalidad.

