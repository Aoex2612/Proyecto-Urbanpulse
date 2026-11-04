# Guía de despliegue — UrbanPulse

Esta guía técnica describe el proceso y la estrategia recomendada para desplegar la plataforma **UrbanPulse** en diferentes entornos (pruebas, *staging* y producción), asegurando despliegues reproducibles, automatizables, consistentes y con tiempo de inactividad mínimo (*zero downtime*).

---

## 1. Objetivo

Establecer un flujo estandarizado para la compilación, empaquetado, contenerización, publicación y ejecución de los servicios de UrbanPulse (Backend Spring Boot, Frontend React/Vite y base de datos PostgreSQL), permitiendo despliegues controlados, monitorizados y con capacidad de reversión (*rollback*).

---

## 2. Requisitos previos

Para ejecutar el proceso de despliegue se debe disponer de:

- **Java Development Kit (JDK):** Versión 21 (LTS).
- **Apache Maven:** Versión 3.9 o superior.
- **Node.js & npm:** Versión 20 LTS.
- **Docker Engine & Docker Compose:** Versión 2+.
- **Git:** Control de versiones para trazabilidad de tags y commits.
- **Acceso a un registro de contenedores:** Docker Hub o GitHub Container Registry (`ghcr.io`).
- **Acceso al entorno de despliegue:** Acceso SSH / VPS o servidor de despliegue.

---

## 3. Preparar la aplicación

Antes de construir las imágenes o generar una nueva versión de producción, es obligatorio sincronizar el código y superar el ciclo de validación:

### 3.1. Sincronizar el repositorio local
```bash
git switch main
git pull
```

### 3.2. Ejecutar las pruebas unitarias y de integración
```bash
# Backend (JUnit + JaCoCo)
cd Backend
mvn test

# Frontend (Vitest)
cd ../frontend
npm test
```

### 3.3. Ejecutar la validación completa del proyecto
```bash
# Backend (validación completa y verificación de plugins)
cd ../Backend
mvn verify

# Frontend (análisis estático y linter)
cd ../frontend
npm run lint
```

### 3.4. Generar los artefactos de la aplicación
```bash
# Generar el paquete .jar ejecutable del backend
cd ../Backend
mvn clean package -DskipTests
```
El archivo `.jar` resultante se generará en el directorio `Backend/target/` (por ejemplo, `Backend-0.1.0.jar`).

```bash
# Compilar los activos estáticos optimizados del frontend
cd ../frontend
npm run build
```
Los artefactos estáticos listos para producción se ubicarán en `frontend/dist/`.

---

## 4. Construcción y publicación de imágenes Docker

### 4.1. Construir las imágenes

Se recomienda etiquetar siempre las imágenes con la versión semántica explícita del proyecto, evitando depender exclusivamente de la etiqueta `latest`:

```bash
# Construir imagen del backend (ejemplo para versión 0.1.0)
docker build -t ghcr.io/aoex2612/urbanpulse-backend:0.1.0 ./Backend

# Construir imagen del frontend (servido mediante Nginx para producción)
docker build -t ghcr.io/aoex2612/urbanpulse-frontend:0.1.0 ./frontend
```

> [!TIP]
> Ejemplos de versionado para trazabilidad:
> - `urbanpulse-backend:0.1.0`
> - `urbanpulse-backend:0.2.0`
> - `urbanpulse-backend:1.0.0`

### 4.2. Probar las imágenes localmente

Antes de publicar cualquier imagen en el registro, validar que arranca y responde correctamente:

```bash
# Probar el backend localmente en puerto 8080
docker run --rm -p 8080:8080 ghcr.io/aoex2612/urbanpulse-backend:0.1.0
```

Verificar que la aplicación responde en el endpoint de salud:
```bash
curl -f http://localhost:8080/actuator/health
```

### 4.3. Publicar las imágenes en el registro de contenedores

Autenticarse en el registro:
```bash
docker login ghcr.io
```

Subir las imágenes construidas:
```bash
docker push ghcr.io/aoex2612/urbanpulse-backend:0.1.0
docker push ghcr.io/aoex2612/urbanpulse-frontend:0.1.0
```

---

## 5. Variables de entorno y configuración

La configuración sensible de cada entorno **no debe incluirse** en el código fuente ni quedar horneada dentro de las imágenes Docker. El mismo contenedor debe servir para todos los entornos ajustando únicamente sus variables externas.

### 5.1. Variables principales

| Variable | Descripción | Ejemplo de valor |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Perfil activo de Spring | `production`, `staging`, `dev` |
| `PORT` | Puerto HTTP del backend | `8080` |
| `DB_HOST` | Host o dirección de la base de datos | `db-postgres` o IP de clúster |
| `DB_PORT` | Puerto de la base de datos | `5432` |
| `DB_NAME` | Nombre de la base de datos | `urban_pulse_db` |
| `DB_USER` | Usuario administrador de PostgreSQL | `admin` |
| `DB_PASSWORD` | Contraseña segura del usuario de base de datos | `<contraseña-segura>` |
| `JWT_SECRET` | Clave secreta (mínimo 512 bits) para firma de JWT | `<clave-hex-segura>` |
| `JWT_EXPIRATION` | Tiempo de expiración del token en ms | `86400000` (24 horas) |

---

## 6. Despliegue con Docker Compose (Pruebas, Staging y Producción)

Actualmente, **UrbanPulse** utiliza **Docker Compose** como motor de orquestación y despliegue para todos sus entornos (desarrollo local, pruebas/staging y servidores de producción en VPS o instancias cloud).

### 6.1. Definición de servicios para producción (`docker-compose.yml` / `docker-compose.prod.yml`)

Para desplegar la solución completa en un servidor de producción o staging, se orquestan los tres componentes esenciales: la base de datos PostgreSQL 16, el backend Spring Boot y el frontend web servido mediante un servidor web de producción (Nginx):

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: db_postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-urban_pulse_db}
      POSTGRES_USER: ${DB_USER:-admin}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-database}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/init/diagramUrbanPulse.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-admin} -d ${DB_NAME:-urban_pulse_db}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    image: ghcr.io/aoex2612/urbanpulse-backend:${APP_VERSION:-0.1.0}
    container_name: urbanpulse_backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-urban_pulse_db}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-admin}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
      APP_UPLOAD_DIR: uploads
    volumes:
      - backend_uploads:/app/uploads
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 3
      start_period: 30s

  frontend:
    image: ghcr.io/aoex2612/urbanpulse-frontend:${APP_VERSION:-0.1.0}
    container_name: urbanpulse_frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  postgres_data:
  backend_uploads:
```

### 6.2. Puesta en marcha en el servidor

1. **Configurar el entorno en el servidor:**
   Clonar el repositorio o situar la plantilla `docker-compose.yml` junto al archivo `.env` configurado con las credenciales de producción:
   ```bash
   cp .env.example .env
   # Editar .env con las claves y contraseñas reales
   nano .env
   ```

2. **Descargar las imágenes más recientes del registro:**
   ```bash
   docker compose pull
   ```

3. **Levantar los servicios en segundo plano:**
   ```bash
   docker compose up -d
   ```

### 6.3. Comprobaciones y diagnóstico
```bash
# Comprobar el estado y salud de los contenedores
docker compose ps

# Consultar los logs en tiempo real (todos los servicios)
docker compose logs -f

# Consultar los logs de un servicio concreto (ej. backend)
docker compose logs -f backend

# Detener los servicios conservando volúmenes de datos
docker compose down
```

---

## 7. Estrategia de actualización y reversión (*Rollback*)

Al emplear Docker Compose, el proceso de actualización sin interrupciones prolongadas y la reversión rápida ante fallos críticos se gestionan mediante el versionado explícito de etiquetas de imagen:

### 7.1. Actualización de versión

1. **Actualizar el tag de versión en el archivo `.env` del servidor:**
   ```bash
   # Modificar la variable a la nueva versión publicada
   sed -i 's/APP_VERSION=.*/APP_VERSION=0.2.0/' .env
   ```

2. **Descargar la nueva imagen sin alterar los contenedores en ejecución:**
   ```bash
   docker compose pull backend
   ```

3. **Recrear y sustituir únicamente el contenedor actualizado:**
   ```bash
   docker compose up -d --no-deps backend
   ```
   > Docker Compose sustituye el contenedor del backend manteniendo intacta la base de datos PostgreSQL y los volúmenes de datos persistentes (`postgres_data`, `backend_uploads`).

### 7.2. Rollback (Reversión ante fallos)

Si la nueva versión presenta anomalías o fallos críticos tras el despliegue, la reversión a la versión estable previa se realiza en segundos:

1. **Revertir la variable de versión en `.env` al tag estable previo:**
   ```bash
   sed -i 's/APP_VERSION=.*/APP_VERSION=0.1.0/' .env
   ```

2. **Reaplicar el contenedor con la versión anterior:**
   ```bash
   docker compose up -d --no-deps backend
   ```

3. **Comprobar que el servicio responde con normalidad:**
   ```bash
   docker compose ps
   docker compose logs --tail=50 backend
   ```

---

## 8. Comprobaciones posteriores al despliegue (*Post-Deployment*) y Health Checks

Tras finalizar el despliegue o actualización, el operador o el pipeline de CI/CD debe verificar:

1. **Disponibilidad:** Todos los contenedores se encuentran en estado `Up` y con estado de salud `healthy`:
   ```bash
   docker compose ps
   ```
2. **Conectividad a base de datos:** El backend conecta correctamente con PostgreSQL sin excepciones en el pool HikariCP.
3. **Respuesta de endpoints principales:** La API responde en `/api/auth/login` y endpoints de incidencias.
4. **Logs limpios:** No aparecen excepciones no controladas ni errores `FATAL` en los registros:
   ```bash
   docker compose logs --tail=100 backend
   ```
5. **Comprobación de salud (*Health Checks*):**
   - Comprobación HTTP del endpoint de Spring Boot Actuator:
     ```bash
     curl -f http://localhost:8080/actuator/health
     ```
   - Si se define la directiva `healthcheck` en Docker Compose, el demonio Docker vigila periódicamente el estado del contenedor y reinicia el servicio automáticamente si este no responde.

---

## 9. Flujo general de despliegue

```text
  [ Nueva versión (release/0.1.0) ]
                 │
                 ▼
  [ Compilación y tests (mvn verify, npm test) ]
                 │
                 ▼
  [ Construcción de imágenes Docker (:0.1.0) ]
                 │
                 ▼
  [ Publicación en registro (docker push) ]
                 │
                 ▼
  [ Actualización de versión en servidor (.env) ]
                 │
                 ▼
  [ Despliegue con Docker Compose (pull & up -d) ]
                 │
                 ▼
  [ Comprobaciones posteriores (Health check) ]
                 │
          ┌──────┴────────────────┐
          │ ¿Correcto?            │
          ▼                       ▼
   [ Producción OK ]      [ Problema detectado ]
                                  │
                                  ▼
                        [ Rollback inmediato ]
                        (Revertir tag y up -d)
                                  │
                                  ▼
                        [ Versión anterior estable ]
```

---

## 10. Resumen de buenas prácticas de despliegue

1. **Nunca desplegar desde código sin revisar:** Solo desplegar código validado y fusionado mediante Pull Requests a las ramas principales.
2. **Imágenes inmutables con versión explícita:** Usar números de versión semánticos (ej. `:0.1.0`) y no depender exclusivamente de la etiqueta `:latest`.
3. **Cero secretos en imágenes o repositorios:** Inyectar claves y credenciales exclusivamente mediante variables de entorno en archivos `.env` protegidos en el servidor o gestores de secretos.
4. **Separación estricta de entornos:** El mismo binario/imagen Docker debe poder ejecutarse en desarrollo, pruebas y producción sin recompilar el código.
5. **Automatización:** Delegar la validación, construcción y pruebas a pipelines de CI/CD (GitHub Actions).
6. **Health checks activos:** Configurar y verificar siempre comprobaciones de salud del servicio (Spring Boot Actuator y directivas `healthcheck` de Docker).
7. **Estrategia de Rollback siempre definida:** Mantener las imágenes anteriores disponibles en el registro para una reversión inmediata cambiando la etiqueta en `.env`.

> [!NOTE]
> **Evolución futura hacia orquestadores:** Aunque actualmente UrbanPulse se despliega íntegramente mediante **Docker y Docker Compose** por ajustarse de forma óptima a las necesidades de la fase actual y mantener la infraestructura simple, la arquitectura modular y desacoplada de microservicios contenerizados (OCI) está preparada para una transición limpia a plataformas como Kubernetes si en el futuro se requiere escalado horizontal masivo y alta disponibilidad multirregión.
