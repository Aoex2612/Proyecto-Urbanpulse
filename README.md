# UrbanPulse

Plataforma para la gestión, monitorización y resolución de incidencias urbanas en tiempo real. UrbanPulse permite centralizar el reporte ciudadano, la coordinación interdepartamental y el seguimiento del ciclo de vida de cada incidencia en la ciudad.

Proyecto desarrollado para la asignatura Desarrollo Software en Plataformas en la Nube (DSPN), Escuela Técnica Superior de Ingeniería Informática (ETSII).

---

## Tabla de contenidos

1. [Vision general](#vision-general)
2. [Arquitectura y tecnologias](#arquitectura-y-tecnologias)
3. [Estructura del repositorio](#estructura-del-repositorio)
4. [Requisitos previos](#requisitos-previos)
5. [Instalacion y ejecucion](#instalacion-y-ejecucion)
   - [Backend](#backend)
   - [Frontend](#frontend)
6. [Estandares y documentacion tecnica](#estandares-y-documentacion-tecnica)
7. [Equipo](#equipo)

---

## Vision general

UrbanPulse aborda la problematica del mantenimiento de infraestructuras y servicios municipales mediante una plataforma centralizada que ofrece:

- Reporte de incidencias geolocalizadas (coordenadas de latitud y longitud).
- Adjuncion de contenido multimedia (fotografias y videos) asociado a cada reporte.
- Asignacion a departamentos municipales y seguimiento de operarios.
- Trazabilidad y auditoria inmutable de los cambios de estado.
- Enriquecimiento contextual (sensores, condiciones climaticas o trafico).

---

## Arquitectura y tecnologias

El proyecto esta disenado siguiendo una arquitectura desacoplada:

- **Backend:**
  - Lenguaje: Java
  - Gestor de dependencias y construccion: Apache Maven
  - Modulo principal: `Backend/`
  - Paquete base: `org.bulbaseros`

- **Frontend:**
  - Biblioteca de interfaz: React 19
  - Entorno de desarrollo y empaquetador: Vite
  - Linter: ESLint
  - Directorio principal: `frontend/`

- **Persistencia:**
  - Base de datos relacional: PostgreSQL
  - Diseno centrado en la entidad de incidencias y registro de estados

---

## Estructura del repositorio

```text
.
├── Backend/                 # Codigo fuente y configuracion del servidor (Java/Maven)
│   ├── pom.xml              # Fichero de configuracion y dependencias Maven
│   └── src/                 # Codigo fuente y tests de la aplicacion Java
├── frontend/                # Aplicacion cliente en React y Vite
│   ├── package.json         # Dependencias y scripts de Node.js
│   ├── vite.config.js       # Configuracion de Vite
│   └── src/                 # Componentes y logica de la interfaz de usuario
├── constitution/            # Definicion de directrices y convenciones del proyecto
│   ├── mission.md           # Mision, alcance y principios rectores
│   ├── roadmap.md           # Hoja de ruta y estado de las funcionalidades
│   └── tech-stack.md        # Especificacion tecnica de tecnologias y convenciones
├── docs/                    # Documentacion tecnica de diseno
│   └── ADR/                 # Architectural Decision Records (ADRs)
├── features/                # Especificaciones, planes y tareas por funcionalidad
└── README.md                # Documentacion principal del repositorio
```

---

## Requisitos previos

Para ejecutar y desarrollar en este proyecto se recomienda disponer de:

- Java Development Kit (JDK) 17 o superior
- Apache Maven 3.8+
- Node.js (version 20 LTS recomendada, minimo v18)
- npm (incluido con Node.js)
- Docker y Docker Compose (opcional, para servicios auxiliares y base de datos)

---

## Instalacion y ejecucion

### Backend

1. Acceder al directorio del backend:
   ```bash
   cd Backend
   ```

2. Compilar el proyecto y empaquetar:
   ```bash
   mvn clean package
   ```

3. Ejecutar los tests unitarios:
   ```bash
   mvn test
   ```

4. Ejecutar la aplicacion:
   ```bash
   mvn exec:java -Dexec.mainClass="org.bulbaseros.App"
   ```

### Frontend

1. Acceder al directorio del frontend:
   ```bash
   cd frontend
   ```

2. Instalar las dependencias:
   ```bash
   npm install
   ```

3. Iniciar el servidor de desarrollo local:
   ```bash
   npm run dev
   ```

4. Validar el codigo con el linter:
   ```bash
   npm run lint
   ```

5. Generar la compilacion para produccion:
   ```bash
   npm run build
   ```

---

## Estandares y documentacion tecnica

- **Mision y Gobernanza:** Consulta los principios y alcance del sistema en [`constitution/mission.md`](constitution/mission.md).
- **Decisiones de Arquitectura:** Las decisiones tecnicas significativas se documentan mediante el formato ADR en [`docs/ADR/`](docs/ADR/).
- **Gestion de Funcionalidades:** Cada nueva funcionalidad se define dentro del directorio [`features/`](features/) siguiendo la estructura de especificacion (`spec.md`), plan tecnico (`plan.md`) y lista de tareas (`tasks.md`).

---

## Equipo

Proyecto desarrollado por el equipo **Bulbaseros**.
