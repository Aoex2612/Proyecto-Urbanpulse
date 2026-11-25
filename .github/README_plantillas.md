# Guia de Plantillas para Pull Requests e Issues en UrbanPulse

Este directorio contiene las plantillas estandarizadas para GitHub Web, GitHub CLI (`gh`) y herramientas automatizadas (como el MCP de GitHub).

---

## Estructura de archivos

```text
.github/
├── PULL_REQUEST_TEMPLATE.md              # Plantilla por defecto para Pull Requests
├── pull_request_template.md              # Alternativa de PR soportada por GitHub
├── SOFTWARE_DESIGN_DOCUMENT_TEMPLATE.md  # Plantilla de Software Design Documents (DD)
└── ISSUE_TEMPLATE/                       # Plantillas para nuevos Issues
    ├── feature_request.md         # Plantilla para proponer funcionalidades
    └── bug_report.md              # Plantilla para reportar incidencias/bugs
```

---

## 1. Como anadir tus propias plantillas

### Para Pull Requests:
Edita directamente el fichero [`.github/PULL_REQUEST_TEMPLATE.md`](file:///.github/PULL_REQUEST_TEMPLATE.md) y pega el contenido que tengas preparado.

### Para Issues:
Crea o edita archivos dentro de [`.github/ISSUE_TEMPLATE/`](file:///.github/ISSUE_TEMPLATE/). 
Para que GitHub Web y GitHub CLI muestren el selector interactivo con titulo y descripcion, los archivos deben incluir cabeceras frontmatter YAML al principio. Por ejemplo:

```markdown
---
name: Nombre descriptivo (ej. Reporte de Bug)
about: Breve descripcion de para que sirve esta plantilla
title: "prefijo: [Breve titulo]"
labels: bug
assignees: ''
---

[Aqui va el contenido de tu plantilla]
```

Si prefieres tener una unica plantilla predeterminada general para cualquier issue (sin selector web), puedes crear [`.github/ISSUE_TEMPLATE.md`](file:///.github/ISSUE_TEMPLATE.md).

---

## 2. Como usar las plantillas

### Desde la Web de GitHub
- **Pull Requests:** Al hacer clic en *New Pull Request*, GitHub autocompleta automaticamente la descripcion con el contenido de `.github/PULL_REQUEST_TEMPLATE.md`.
- **Issues:** Al pulsar en *New Issue*, GitHub mostrara una pantalla para elegir la plantilla adecuada (Feature, Bug, etc.) y rellenara el formulario.

### Desde GitHub CLI (`gh`)
- **Crear PR:**
  ```bash
  # Abre el editor con la plantilla precargada automaticamente:
  gh pr create
  ```
- **Crear Issue:**
  ```bash
  # Te permite elegir interactivamente entre las plantillas disponibles en ISSUE_TEMPLATE/:
  gh issue create
  ```

### Desde el MCP de GitHub (Asistente de IA)
Al solicitar la creacion de una PR o Issue via MCP (`create_pull_request`, `issue_write`), el asistente lee el contenido de estos ficheros en `.github/` y genera el cuerpo del mensaje respetando los apartados exactos de tu plantilla.

---

## 3. Plantilla de Software Design Document

[`SOFTWARE_DESIGN_DOCUMENT_TEMPLATE.md`](SOFTWARE_DESIGN_DOCUMENT_TEMPLATE.md) es la plantilla para los Design Docs de UrbanPulse (no confundir con los `spec.md` del flujo *Spec-Driven Development*).

Para crear uno:

1. Copia la plantilla a `../docs/SWDD` usando el siguiente número libre del [índice](../docs/SWDD/README.md).
2. Sustituye todos los campos entre corchetes y elimina las indicaciones de la plantilla.
3. Incluye la sección **8.1 Decisiones arquitectónicas derivadas** con enlaces a los ADR existentes; si la decisión es nueva, crea primero su ADR con [`docs/ADR/ADR-NN.md`](../docs/ADR/ADR-NN.md).
4. Añade el enlace inverso en cada ADR implicado (sección *Design Doc relacionado*).
5. Registra el nuevo DD en [`../docs/SWDD`](../docs/SWDD/README.md) y usa siempre rutas relativas.
