# UrbanPulse

Plataforma integral para la gestión, monitorización y resolución de incidencias urbanas en tiempo real. UrbanPulse centraliza el reporte ciudadano geolocalizado, la coordinación interdepartamental, la supervisión operativa y el seguimiento del ciclo de vida de cada aviso en la ciudad.

Proyecto desarrollado para la asignatura Desarrollo Software en Plataformas en la Nube (DSPN), Escuela Técnica Superior de Ingeniería Informática (ETSII).

---

## Tabla de contenidos

1. [Visión general](#visión-general)
2. [Arquitectura y tecnologías](#arquitectura-y-tecnologías)
3. [Estructura del repositorio](#estructura-del-repositorio)
4. [Requisitos previos](#requisitos-previos)
5. [Instalación y ejecución](#instalación-y-ejecución)
   - [Paso 1: Base de datos (Docker)](#paso-1-base-de-datos-docker)
   - [Paso 2: Backend (Spring Boot)](#paso-2-backend-spring-boot)
   - [Paso 3: Frontend (React + Vite)](#paso-3-frontend-react--vite)
6. [Cuentas de prueba preconfiguradas](#cuentas-de-prueba-preconfiguradas)
7. [Estándares y documentación técnica](#estándares-y-documentación-técnica)
8. [Equipo](#equipo)

---

## Visión general

UrbanPulse aborda la problemática del mantenimiento de infraestructuras y servicios municipales mediante una plataforma centralizada que ofrece:

- Reporte de incidencias geolocalizadas mediante coordenadas decimales (latitud y longitud).
- Adjuntos multimedia (fotografías y evidencias) asociados a cada reporte.
- Panel de triaje y operaciones urbanas con control de acceso basado en roles (RBAC).
- Asignación a departamentos municipales y seguimiento de operarios y técnicos.
- Trazabilidad y auditoría inmutable de los cambios de estado.
- Enriquecimiento contextual futuro (sensores ambientales, condiciones climáticas y tráfico).

---

## Arquitectura y tecnologías

El proyecto sigue una arquitectura desacoplada basada en microservicios/monolito modular:

- **Backend:**
  - Lenguaje: Java 21
  - Framework: Spring Boot 3.4.3 (Spring Security 6, Spring Data JPA)
  - Autenticación: Tokens JWT (HMAC-SHA256, JJWT 0.12.6) y cifrado de contraseñas con BCrypt
  - Gestor de dependencias: Apache Maven
  - Directorio principal: `Backend/` (paquete base `org.bulbaseros`)

- **Frontend:**
  - Biblioteca de interfaz: React 19
  - Entorno y empaquetador: Vite 6
  - Enrutamiento: React Router 7 con guardias de rol (`RoleGuard`, `ProtectedRoute`)
  - Cliente HTTP: Axios con interceptor automático para cabeceras Bearer
  - Linter: ESLint
  - Directorio principal: `frontend/`

- **Persistencia:**
  - Motor relacional: PostgreSQL 16 (Alpine) orquestado mediante Docker Compose
  - Base de datos de testing: H2 Database en memoria (modo PostgreSQL)
  - Mapeo de enumeraciones: `VARCHAR` con restricciones `CHECK` para total compatibilidad con JPA (ADR-01)
  - Directorio de scripts: `database/`

---

## Estructura del repositorio

```text
.
├── Backend/                 # Servidor de API REST y lógica de negocio (Java 21 / Spring Boot)
│   ├── pom.xml              # Fichero de configuración y dependencias Maven
│   └── src/                 # Código fuente (config, controller, entity, security, service) y tests
├── frontend/                # Aplicación cliente web SPA (React 19 / Vite)
│   ├── package.json         # Dependencias y scripts de Node.js
│   ├── vite.config.js       # Configuración de Vite
│   └── src/                 # Vistas (pages), componentes, estilos y servicios Axios
├── database/                # Configuración y scripts de persistencia
│   ├── init/
│   │   └── diagramUrbanPulse.sql  # Esquema DDL, claves foráneas, índices, triggers y seed
│   └── README.md            # Parámetros y comandos de gestión de base de datos
├── constitution/            # Directrices de gobernanza y arquitectura
│   ├── mission.md           # Misión, principios y alcance del sistema
│   ├── roadmap.md           # Hoja de ruta y estado de las funcionalidades
│   └── tech-stack.md        # Especificación técnica detallada y convenciones
├── docs/                    # Documentación técnica de diseño
│   ├── ADR/                 # Architectural Decision Records (ADR-01: Persistencia de Enums con JPA)
│   └── api.md               # Especificación de contratos de API REST
├── features/                # Especificaciones SDD (spec.md, plan.md, tasks.md)
├── docker-compose.yml       # Orquestación del contenedor PostgreSQL
└── README.md                # Este documento
```

---

## Requisitos previos

Para ejecutar y desarrollar en este proyecto se requiere:

- **Java Development Kit (JDK):** Versión 21 recomendada (mínimo 17).
- **Apache Maven:** 3.8 o superior.
- **Node.js:** Versión 20 LTS o superior (incluye `npm`).
- **Docker Desktop:** Con Docker Compose v2+ en ejecución.

---

## Instalación y ejecución

### Paso 1: Base de datos (Docker)

Desde la **raíz del proyecto**, levanta el contenedor de PostgreSQL (que ejecutará automáticamente el script de inicialización con las tablas y datos semilla):

```bash
docker compose up -d
```

Para verificar que la base de datos está lista y saludable:

```bash
docker compose ps
docker exec -it db_postgres psql -U admin -d urban_pulse_db -c "SELECT email, role FROM users;"
```

### Paso 2: Backend (Spring Boot)

1. Acceder al directorio del backend:
   ```bash
   cd Backend
   ```

2. Ejecutar los tests unitarios y de integración:
   ```bash
   mvn clean test
   ```

3. Iniciar el servidor Spring Boot conectado a PostgreSQL:
   ```bash
   mvn spring-boot:run
   ```
   El backend arrancará escuchando en `http://localhost:8080`.

### Paso 3: Frontend (React + Vite)

1. Acceder al directorio del frontend:
   ```bash
   cd frontend
   ```

2. Instalar dependencias:
   ```bash
   npm install
   ```

3. Iniciar el servidor local de desarrollo:
   ```bash
   npm run dev
   ```
   La aplicación web estará disponible en `http://localhost:5173`.

4. Validar el código con el linter:
   ```bash
   npm run lint
   ```

---

## Cuentas de prueba preconfiguradas

La base de datos incluye usuarios semilla precargados con contraseñas seguras hasheadas mediante BCrypt para evaluar los flujos de autenticación y control de acceso (RBAC):

| Rol | Correo electrónico | Contraseña (sensible a mayúsculas) | Permisos y vistas |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@urbanpulse.es` | `Admin1234` | Acceso a administración y triaje operativo completo |
| **OPERATOR** | `operator@urbanpulse.es` | `Operator1234` | Acceso al Panel de Operador (`/operator-panel`) y cambio de estados |
| **CITIZEN** | `citizen@urbanpulse.es` | `Citizen1234` | Portal ciudadano (`/`), consulta y reporte de incidencias |

*(Nuevos ciudadanos pueden registrarse libremente a través de la vista `/register` recibiendo automáticamente el rol `CITIZEN`).*

---

## Estándares y documentación técnica

- **Misión y Gobernanza:** Consulta los principios rectores en [`constitution/mission.md`](constitution/mission.md).
- **Especificación Técnica:** Revisa las directrices de arquitectura y límites en [`constitution/tech-stack.md`](constitution/tech-stack.md).
- **Decisiones de Arquitectura:** Registradas en [`docs/ADR/`](docs/ADR/) (ver [`ADR-01: Persistencia de Enums con JPA`](docs/ADR/ADR-01-persistencia-enums-jpa.md)).
- **Gestión de Funcionalidades (SDD):** Cada funcionalidad cuenta con su especificación (`spec.md`), plan (`plan.md`) y tareas (`tasks.md`) dentro de [`features/`](features/).

---

## Equipo

Proyecto desarrollado por el equipo **Bulbaseros**.
