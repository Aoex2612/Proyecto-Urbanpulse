# Guía de despliegue — UrbanPulse

> [!NOTE]
> La versión canónica de esta guía se encuentra en [`docs/deployment.md`](docs/deployment.md) siguiendo la estructura de documentación técnica del proyecto.

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
- **Acceso al entorno de despliegue:** Acceso SSH / VPS o credenciales del clúster de producción.
- **Para despliegues en Kubernetes:**
  - `kubectl` instalado y configurado en el equipo local / runner CI.
  - Conexión autorizada al clúster (`~/.kube/config`).

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
El archivo `.jar` resultante se generará en el directorio `Backend/target/` (por ejemplo, `Backend-1.0-SNAPSHOT.jar`).

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
# Construir imagen del backend (ejemplo para versión 1.0.0)
docker build -t ghcr.io/aoex2612/urbanpulse-backend:1.0.0 ./Backend

# Construir imagen del frontend (servido mediante Nginx para producción)
docker build -t ghcr.io/aoex2612/urbanpulse-frontend:1.0.0 ./frontend
```

> [!TIP]
> Ejemplos de versionado para trazabilidad:
> - `urbanpulse-backend:1.0.0`
> - `urbanpulse-backend:1.1.0`
> - `urbanpulse-backend:2.0.0`

### 4.2. Probar las imágenes localmente

Antes de publicar cualquier imagen en el registro, validar que arranca y responde correctamente:

```bash
# Probar el backend localmente en puerto 8080
docker run --rm -p 8080:8080 ghcr.io/aoex2612/urbanpulse-backend:1.0.0
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
docker push ghcr.io/aoex2612/urbanpulse-backend:1.0.0
docker push ghcr.io/aoex2612/urbanpulse-frontend:1.0.0
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

## 6. Despliegue con Docker Compose (Pruebas y Staging)

Para entornos de prueba locales o servidores de integración (*staging*), se puede utilizar Docker Compose:

```bash
# Descargar las imágenes más recientes del registro
docker compose pull

# Levantar los servicios en segundo plano
docker compose up -d
```

### 6.1. Comprobaciones y diagnóstico
```bash
# Comprobar el estado y salud de los contenedores
docker compose ps

# Consultar los logs en tiempo real
docker compose logs -f

# Detener los servicios
docker compose down
```

---

## 7. Despliegue con Kubernetes (Producción)

En entornos de producción de alta disponibilidad, los servicios de UrbanPulse se gestionan mediante manifiestos de Kubernetes (`Deployment`, `Service`, `Secret` y `ConfigMap`).

### 7.1. Manifiesto simplificado (`k8s/deployment-backend.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: urbanpulse-backend
  labels:
    app: urbanpulse-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: urbanpulse-backend
  template:
    metadata:
      labels:
        app: urbanpulse-backend
    spec:
      containers:
        - name: backend
          image: ghcr.io/aoex2612/urbanpulse-backend:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: DB_HOST
              value: "postgres-service"
            - name: DB_PORT
              value: "5432"
            - name: DB_NAME
              value: "urban_pulse_db"
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: urbanpulse-secrets
                  key: db-user
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: urbanpulse-secrets
                  key: db-password
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: urbanpulse-secrets
                  key: jwt-secret
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
```

### 7.2. Aplicar la configuración
```bash
kubectl apply -f k8s/deployment-backend.yaml
```

### 7.3. Comprobar el estado del despliegue
```bash
kubectl get deployments
kubectl get pods -l app=urbanpulse-backend
```

---

## 8. Estrategia de actualización y reversión (Rolling Update y Rollback)

### 8.1. Rolling Update

Kubernetes sustituye progresivamente los Pods de la versión anterior por nuevos Pods de la versión entrante sin interrumpir el servicio:

```bash
# Actualizar a una nueva versión de la imagen
kubectl set image deployment/urbanpulse-backend backend=ghcr.io/aoex2612/urbanpulse-backend:1.1.0
```

Visualizar el progreso de la actualización en vivo:
```bash
kubectl rollout status deployment/urbanpulse-backend
```

### 8.2. Rollback (Reversión ante fallos)

Si la nueva versión presenta anomalías o fallos críticos tras el despliegue, es posible revertir inmediatamente a la versión estable previa:

```bash
# Deshacer el último despliegue y regresar a la versión anterior
kubectl rollout undo deployment/urbanpulse-backend
```

Consultar el historial de despliegues y versiones:
```bash
kubectl rollout history deployment/urbanpulse-backend
```

---

## 9. Comprobaciones posteriores al despliegue (*Post-Deployment*) y Health Checks

Tras finalizar el despliegue, el operador o el pipeline de CI/CD debe verificar:

1. **Disponibilidad:** Todos los Pods o contenedores se encuentran en estado `Running` y listos (`Ready`).
2. **Conectividad a base de datos:** El backend conecta correctamente con PostgreSQL sin excepciones de conexión.
3. **Respuesta de endpoints principales:** La API responde en `/api/auth` y endpoints de incidencias.
4. **Logs limpios:** No aparecen excepciones no controladas ni errores `FATAL` en los logs:
   ```bash
   kubectl logs -l app=urbanpulse-backend --tail=100
   ```
5. **Comprobación de salud (*Health Checks*):**
   - `livenessProbe`: Comprueba que el contenedor sigue vivo. Si falla, el orquestador lo reinicia.
   - `readinessProbe`: Comprueba si el contenedor está preparado para recibir tráfico del balanceador. Si falla, se aísla temporalmente.

---

## 10. Flujo general de despliegue

```text
  [ Nueva versión ]
         │
         ▼
  [ Compilación y tests (mvn verify, npm test) ]
         │
         ▼
  [ Construcción de imagen Docker (:1.0.0) ]
         │
         ▼
  [ Publicación en registro (docker push) ]
         │
         ▼
  [ Actualización del Deployment ]
         │
         ▼
  [ Rolling Update progresivo ]
         │
         ▼
  [ Comprobaciones posteriores (Health checks) ]
         │
  ┌──────┴────────────────┐
  │ ¿Correcto?            │
  ▼                       ▼
[ Producción OK ]       [ Problema detectado ]
                          │
                          ▼
                        [ Rollback inmediato ]
                          │
                          ▼
                        [ Versión anterior estable ]
```

---

## 11. Resumen de buenas prácticas de despliegue

1. **Nunca desplegar desde código sin revisar:** Solo desplegar código validado y fusionado mediante Pull Requests.
2. **Imágenes inmutables con versión explícita:** Usar números de versión semánticos (ej. `:1.0.0`) y no depender de `:latest`.
3. **Cero secretos en imágenes o repositorios:** Inyectar claves y credenciales mediante variables de entorno o Secrets de Kubernetes.
4. **Separación estricta de entornos:** El mismo binario/imagen debe poder ejecutarse en desarrollo, pruebas y producción sin recompilar.
5. **Automatización:** Delegar el proceso de construcción y despliegue a pipelines de CI/CD (GitHub Actions).
6. **Health checks activos:** Configurar siempre probes de liveness y readiness para asegurar tolerancia a fallos.
7. **Estrategia de Rolling Update:** Minimizar o eliminar el tiempo de inactividad durante las actualizaciones.
8. **Plan de Rollback siempre listo:** Conocer de antemano el comando y procedimiento de reversión si una versión falla.
