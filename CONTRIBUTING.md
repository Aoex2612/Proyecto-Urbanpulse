# Guia de contribucion — UrbanPulse

Para colaborar en UrbanPulse, todo el desarrollo se integra a traves de la rama `develop` siguiendo estas pautas de ramas y commits.

---

## 1. Flujo de trabajo

1. **Sincronizar develop:** Actualiza tu rama local con los ultimos cambios:
   ```bash
   git checkout develop
   git pull origin develop
   ```

2. **Crear rama de trabajo:** Crea siempre tu rama partiendo de `develop` aplicando la convencion de nombres:
   ```bash
   git checkout -b feature/autenticacion-usuarios
   ```

3. **Realizar cambios y comprobaciones:** Ejecuta los tests y linters antes de confirmar:
   - Backend: `cd Backend && mvn clean test`
   - Frontend: `cd frontend && npm run lint`

4. **Crear commits:** Registra tus cambios siguiendo la convencion de commits en espanol.

5. **Abrir Pull Request:** Sube la rama y abre el Pull Request con destino exclusivo a la rama `develop` (nunca directamente a `main`).

6. **Revision:** Espera la revision y aprobacion del equipo antes de fusionar.

---

## 2. Convencion de ramas

Las ramas deben crearse en minusculas, con palabras separadas por guiones (`kebab-case`) y bajo la siguiente estructura: `<tipo>/<descripcion>`:

| Tipo | Uso | Ejemplo |
| :--- | :--- | :--- |
| `feature/...` | Nueva funcionalidad | `feature/autenticacion-usuarios` |
| `fix/...` | Correccion de bug | `fix/error-servicio-incidencias` |
| `docs/...` | Cambios de documentacion | `docs/actualizar-readme` |
| `refactor/...` | Refactorizacion de codigo | `refactor/estructura-proyecto` |
| `test/...` | Pruebas unitarias o de integracion | `test/pruebas-servicio-alertas` |
| `hotfix/...` | Correccion urgente | `hotfix/fallo-conexion-base-datos` |

---

## 3. Convencion de commits

Se utiliza el estandar **Conventional Commits**:

`<tipo>: <descripcion>`

### Tipos permitidos:
- `feat`: Nueva funcionalidad
- `fix`: Correccion de bug
- `docs`: Documentacion
- `test`: Pruebas
- `refactor`: Refactorizacion de codigo sin alterar funcionalidad
- `chore`: Tareas internas, configuracion o dependencias

### Reglas de idioma y caracteres:
- Todo mensaje debe redactarse en **espanol**.
- Si una palabra lleva la letra `ñ`, se debe sustituir siempre por la letra `n` (por ejemplo: `anadir`, `contrasena`, `diseno`).
- Evitar el uso de tildes y caracteres especiales.

### Ejemplos:
```bash
git commit -m "feat: anadir endpoint de creacion de incidencias"
git commit -m "fix: corregir lectura de coordenadas nulas"
git commit -m "docs: actualizar instrucciones de despliegue local"
git commit -m "test: anadir pruebas para el modulo de usuarios"
git commit -m "refactor: simplificar controladores de autenticacion"
git commit -m "chore: actualizar dependencias de maven"
```
