# Guía de desarrollo — UrbanPulse

Esta guía técnica describe el proceso para configurar el entorno de trabajo, ejecutar la aplicación, pasar las pruebas de calidad y colaborar mediante el flujo de trabajo con Git y GitHub en el proyecto **UrbanPulse**.

---

## 1. Requisitos

Para trabajar en el desarrollo de UrbanPulse es necesario disponer en el equipo local de las siguientes herramientas:

- **Java Development Kit (JDK):** Versión 21 (LTS).
- **Apache Maven:** Versión 3.9 o superior.
- **Node.js:** Versión 20 LTS recomendada (mínimo v18).
- **npm:** Gestor de paquetes de Node (incluido con Node.js).
- **Docker & Docker Compose:** Versión 2+ (para el servicio de base de datos PostgreSQL).
- **Git:** Control de versiones.

> [!NOTE]
> No es necesario instalar PostgreSQL 16 directamente en el sistema operativo local si se ejecuta el servicio de base de datos mediante Docker Compose.

---

## 2. Preparar el entorno

### 2.1. Clonar el repositorio

Clonar el repositorio remoto y situarse en la raíz del proyecto:

```bash
git clone https://github.com/Aoex2612/Proyecto-Urbanpulse.git
cd Proyecto-Urbanpulse
```

### 2.2. Configuración de variables de entorno

Siguiendo las recomendaciones de *12-factor apps*, los valores sensibles y dependientes del entorno no deben subirse al repositorio Git. 

1. Crear el fichero local `.env` a partir de la plantilla de ejemplo:
   ```bash
   cp .env.example .env
   ```
2. Revisar las variables definidas en `.env` (puerto de Postgres, usuario, contraseña y base de datos) y ajustar los valores si fuera necesario.

### 2.3. Iniciar servicios auxiliares con Docker Compose

La base de datos PostgreSQL 16 (Alpine) y su script de inicialización se ejecutan mediante Docker Compose:

```bash
# Iniciar servicios en segundo plano
docker compose up -d
```

Para comprobar el estado y salud del contenedor (`db_postgres`):

```bash
docker compose ps
```

Para consultar los logs del contenedor de base de datos en tiempo real:

```bash
docker compose logs -f postgres
```

Para detener el entorno auxiliar cuando finalice la sesión:

```bash
docker compose down
```

---

## 3. Ejecutar la aplicación

UrbanPulse consta de una arquitectura desacoplada con backend en Spring Boot y frontend en React con Vite.

### 3.1. Backend (Spring Boot)

Con la base de datos PostgreSQL levantada y lista para recibir conexiones:

```bash
cd Backend
mvn spring-boot:run
```

La aplicación backend arrancará por defecto en `http://localhost:8080` utilizando la configuración definida para el entorno local y las variables de entorno correspondientes.

### 3.2. Frontend (React + Vite)

En una terminal independiente, acceder a la carpeta del cliente web:

```bash
cd frontend
npm install
npm run dev
```

El servidor de desarrollo de Vite arrancará en `http://localhost:5173`.

---

## 4. Ejecutar pruebas

Antes de integrar cualquier cambio es imprescindible verificar que la suite de pruebas del proyecto se ejecuta con éxito.

### 4.1. Pruebas del Backend

Para ejecutar los tests unitarios y de integración de la aplicación Java:

```bash
cd Backend
mvn test
```

Este comando ejecuta las pruebas asociadas a la fase `test` del ciclo de vida de Maven (JUnit 5, Spring Boot Starter Test, Spring Security Test) y genera el informe de cobertura de código con **JaCoCo** en `Backend/target/site/jacoco/index.html`.

### 4.2. Pruebas del Frontend

Para ejecutar la suite de pruebas unitarias y de componentes en el frontend (Vitest + Testing Library):

```bash
cd frontend
npm test
```

Para generar además el reporte de cobertura de código en el cliente:

```bash
npm run test:coverage
```

---

## 5. Ejecutar validación y análisis de calidad

Para ejecutar todas las fases del ciclo de vida de Maven hasta `verify`:

```bash
cd Backend
mvn verify
```

Este comando:
1. Compila el código fuente con el JDK 21.
2. Ejecuta las pruebas unitarias y de integración.
3. Ejecuta comprobaciones adicionales de calidad, validación y plugins configurados en `pom.xml`.

Para validar el estilo y buenas prácticas de código en el frontend con ESLint:

```bash
cd frontend
npm run lint
```

---

## 6. Flujo de trabajo con Git (GitHub Flow)

En este repositorio seguimos una variante estructurada de **GitHub Flow**. **No se debe trabajar ni hacer push directamente sobre las ramas protegidas (`main` o `develop`)**. Dichas ramas representan versiones estables del proyecto.

### 6.1. Ciclo de trabajo por funcionalidad

1. **Actualizar la rama base local:**
   Antes de comenzar una nueva tarea, asegurar que la base local está sincronizada con el repositorio remoto:
   ```bash
   git switch develop
   git pull
   ```

2. **Crear una rama específica para el cambio:**
   Crear la rama partiendo del estado actualizado:
   ```bash
   git switch -c feature/nombre-funcionalidad
   ```

3. **Tipos de ramas recomendadas:**
   El nombre de la rama debe incluir como prefijo la tipología del trabajo:

   | Tipo de rama | Uso | Ejemplo |
   | :--- | :--- | :--- |
   | `feature/...` | Nueva funcionalidad | `feature/user-authentication`, `feature/incident-filters` |
   | `fix/...` | Corrección de bug | `fix/token-expiration-validation` |
   | `docs/...` | Cambios en la documentación | `docs/update-development-guide` |
   | `refactor/...` | Refactorización de código sin alterar comportamiento | `refactor/auth-service-logic` |
   | `test/...` | Adición o modificación de pruebas | `test/user-repository-tests` |
   | `hotfix/...` | Corrección urgente y crítica en producción | `hotfix/security-bypass` |

4. **Realizar commits atómicos y claros:**
   ```bash
   git add .
   git commit -m "feat: add user registration endpoint"
   ```

5. **Subir la rama al repositorio remoto:**
   ```bash
   git push -u origin feature/nombre-funcionalidad
   ```

6. **Abrir un Pull Request (PR)** en GitHub para que el equipo revise e integre los cambios.

---

## 7. Convención de commits (Conventional Commits)

Los mensajes de commit deben seguir la especificación [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/). Esta práctica mantiene un historial limpio, trazable y posibilita la generación automática de changelogs y versionado semántico.

### 7.1. Estructura del mensaje

```text
<tipo>: <descripción corta en imperativo>
```

### 7.2. Tipos de commits habituales

| Tipo | Propósito | Ejemplo |
| :--- | :--- | :--- |
| `feat` | Nueva funcionalidad | `git commit -m "feat: add JWT authentication"` |
| `fix` | Corrección de error o bug | `git commit -m "fix: handle expired tokens"` |
| `docs` | Cambios exclusivos en documentación | `git commit -m "docs: add development guide"` |
| `style` | Formato, espacios, punto y coma, linting (sin cambios en lógica) | `git commit -m "style: format java service classes"` |
| `refactor` | Cambio interno de código sin alterar comportamiento externo | `git commit -m "refactor: simplify incident service mapping"` |
| `test` | Añadir o corregir pruebas unitarias o de integración | `git commit -m "test: add unit tests for auth controller"` |
| `chore` | Tareas de mantenimiento o configuración auxiliar | `git commit -m "chore: update gitignore for temp files"` |
| `ci` | Cambios en pipelines o flujos de integración continua | `git commit -m "ci: add GitHub Actions workflow"` |
| `build` | Cambios que afectan al sistema de compilación o dependencias | `git commit -m "build: bump spring-boot version to 3.4.3"` |

### 7.3. Buenas vs malas prácticas en commits

| Mala estrategia | Buena estrategia |
| :--- | :--- |
| `git commit -m "cambios"` | `git commit -m "feat: add user registration endpoint"` |
| `git commit -m "arreglos"` | `git commit -m "fix: validate empty task title"` |
| `git commit -m "cosas varias"` | `git commit -m "docs: update installation guide"` |

---

## 8. Pull Requests (PR)

Un Pull Request es una unidad de revisión y control de calidad; no consiste solo en solicitar la subida de código al repositorio.

### 8.1. Requisitos antes de abrir un Pull Request

Antes de abrir el PR en GitHub, verificar localmente:
- [ ] El código compila sin errores.
- [ ] La suite de pruebas de backend pasa con éxito (`mvn test`).
- [ ] La suite de pruebas de frontend pasa con éxito (`npm test`).
- [ ] Las validaciones completas son satisfactorias (`mvn verify` y `npm run lint`).
- [ ] No se incluyen secretos, contraseñas, tokens ni ficheros `.env`.
- [ ] La documentación técnica ha sido actualizada si el cambio lo requiere.

### 8.2. Plantilla de Pull Request

El PR debe seguir la estructura recomendada:

```markdown
## Descripción
Explica brevemente qué cambios introduce este Pull Request.

## Tipo de cambio
- [ ] Nueva funcionalidad
- [ ] Corrección de bug
- [ ] Refactorización
- [ ] Documentación
- [ ] Tests
- [ ] CI/CD

## Cómo se ha probado
Describe las pruebas realizadas (unitarias, integración, manuales).

## Checklist
- [ ] El código compila correctamente.
- [ ] Los tests pasan.
- [ ] Se ha actualizado la documentación.
- [ ] No se han incluido secretos ni credenciales.
- [ ] El cambio no rompe compatibilidad hacia atrás.

## Issue relacionado
Closes #12
```

### 8.3. Vinculación con Issues

GitHub permite cerrar automáticamente el Issue asociado al fusionar el PR utilizando palabras clave:
- `Closes #<numero-issue>`
- `Fixes #<numero-issue>`
- `Resolves #<numero-issue>`

---

## 9. Variables de entorno y seguridad

- **Datos sensibles:** Las contraseñas, claves privadas y secretos jamás deben almacenarse en el control de versiones ni compilarse estáticamente dentro de imágenes de contenedor.
- **Fichero `.env.example`:** Debe mantenerse en el repositorio con las variables requeridas y valores explicativos o por defecto para desarrollo.
- **Fichero `.env`:** Fichero local con los valores reales para la máquina de desarrollo. Debe permanecer siempre en `.gitignore`.

Variables comunes del entorno:
```text
POSTGRES_PORT=5432
POSTGRES_DB=urban_pulse_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=database
```

---

## 10. Gestión de versiones y roles

### 10.1. Versionado semántico (SemVer)

El proyecto sigue el esquema `MAJOR.MINOR.PATCH`:
- **MAJOR:** Cambios incompatibles con versiones anteriores (breaking changes).
- **MINOR:** Nuevas funcionalidades compatibles hacia atrás.
- **PATCH:** Correcciones de errores compatibles hacia atrás.

### 10.2. Flujo completo: Issue → Rama → Commit → PR → Tag → Release

```text
  [ Issue ]
      │
      ▼
  [ Rama (feature/...) ]
      │
      ▼
  [ Commits (feat: ...) ]
      │
      ▼
  [ Pull Request (#PR) ]
      │
      ▼
  [ Merge en develop / main ]
      │
      ▼
  [ Tag (v1.0.0) ]
      │
      ▼
  [ Release en GitHub ]
      │
      ▼
  [ CHANGELOG.md actualizado ]
```

### 10.3. Roles del equipo

| Rol | Responsabilidad principal |
| :--- | :--- |
| **Maintainer** | Gestiona el repositorio, ramas principales, releases y configuración CI/CD. |
| **Developer** | Diseña e implementa nuevas funcionalidades y pruebas asociadas. |
| **Reviewer** | Revisa código en Pull Requests comprobando estándares y calidad. |
| **Tester** | Valida el correcto funcionamiento de las funcionalidades y casos de prueba. |
| **Product Owner / Leader** | Define requisitos y prioriza issues en los milestones. |

---

## 11. Resumen de buenas prácticas

1. **Ramas protegidas siempre estables:** No realizar commits directos sobre `main` o `develop`.
2. **Ramas independientes y de ciclo corto:** Crear una rama por cada funcionalidad, arreglo o cambio relevante.
3. **Commits atómicos y descriptivos:** Hacer commits pequeños con mensajes claros bajo Conventional Commits.
4. **Verificación previa:** Ejecutar `mvn verify`, `npm test` y `npm run lint` antes de solicitar revisión.
5. **Revisión obligatoria:** Toda integración debe pasar por Pull Request y revisión cruzada.
6. **Seguridad rigurosa:** Cero credenciales o secretos en Git.
7. **Documentación viva:** Mantener actualizada la documentación (`README.md`, `docs/`, especificaciones) con cada cambio significativo.
