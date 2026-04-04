# Propuesta CI/CD para monorepo `event-driven-order-platform`

## 1) Objetivos cubiertos

Esta estrategia implementa:

- Pipeline de **Pull Request** con quality gates obligatorios.
- Pipeline de **main** para build/test, tagging versionado y publicación de imágenes Docker.
- **Build por servicio** (matriz por microservicio).
- **Tests** automatizados por servicio (`mvn verify`).
- **Lint/Checkstyle si aplica** (se ejecuta automáticamente cuando existe configuración central de checkstyle).
- **Empaquetado Docker** por servicio.
- **Tagging versionado** por servicio y por contratos.
- **Publicación a registry** (GHCR).
- **Artefactos de deployment K8s** renderizados con Kustomize.

## 2) Workflows incluidos

### `PR CI` (`.github/workflows/pr-ci.yml`)

Se ejecuta en `pull_request` a `main`.

Flujo:
1. Detecta servicios/contratos cambiados.
2. Ejecuta build + tests por servicio en paralelo.
3. Ejecuta checkstyle condicional (si hay configuración).
4. Valida sintaxis JSON de contratos si hubo cambios en `contracts/`.
5. Ejecuta build Docker smoke (sin push).
6. Aplica gates finales de calidad.

### `Main CD` (`.github/workflows/main-cd.yml`)

Se ejecuta en `push` a `main` y manual (`workflow_dispatch`).

Flujo:
1. Detecta servicios/contratos cambiados.
2. Build + tests por servicio.
3. Valida contratos (si aplica).
4. Para cada servicio cambiado:
   - Calcula siguiente versión semántica (`major.minor.patch`) basada en tags previos.
   - Crea tag Git `service-name/vX.Y.Z`.
   - Construye y publica imagen Docker en GHCR con tags `vX.Y.Z` y `sha-...`.
5. Si cambiaron contratos:
   - Calcula versión y crea tag `contracts/vX.Y.Z`.
6. Renderiza manifiestos Kubernetes por overlay (`deploy/k8s/overlays/*`) y los publica como artefacto.

## 3) Estrategia de variables y secretos

### Variables no sensibles (recomendadas)

Definir como **Repository Variables** o en `env` por entorno:

- `JAVA_VERSION` (default `21`).
- `REGISTRY` (default `ghcr.io`).
- `IMAGE_NAMESPACE` (default owner del repo).
- `K8S_NAMESPACE` por entorno (dev/staging/prod).
- `KUSTOMIZE_OVERLAY` por entorno (dev/staging/prod).

### Secretos mínimos

Para GHCR con `GITHUB_TOKEN`, no se necesita secreto extra si permisos `packages:write`.

Para despliegues reales multi-entorno:

- `KUBE_CONFIG_DEV`, `KUBE_CONFIG_STG`, `KUBE_CONFIG_PROD` (o federación OIDC).
- `REGISTRY_USERNAME`, `REGISTRY_PASSWORD` si se usa registry externo.
- `SLACK_WEBHOOK_URL` / `TEAMS_WEBHOOK_URL` para notificaciones opcionales.

## 4) Gates mínimos de calidad (recomendados)

En PR (bloqueantes):

1. `mvn verify` exitoso por servicio impactado.
2. Build Docker exitoso por servicio impactado.
3. Validación de contratos cuando hubo cambios en `contracts/`.
4. Checkstyle cuando exista configuración declarada.

En `main`:

1. Repetir gates de PR antes de publicar.
2. Solo publicar imágenes si build/test pasó.
3. Solo taggear contratos si su validación pasó.

## 5) Estrategia de versionado (servicios + contratos)

### Servicios

- Tag por servicio: `order-service/v1.2.3`, `payment-service/v1.2.3`, etc.
- Regla actual en CI: incrementa **patch** respecto al último tag del servicio.
- Imagen Docker publicada con:
  - `vX.Y.Z` (estable, promocionable)
  - `sha-<short>` (inmutable, trazabilidad)

### Contratos

- Tag independiente: `contracts/vX.Y.Z`.
- Regla actual: incrementa patch cuando hay cambios en `contracts/`.
- Recomendación evolutiva:
  - **MAJOR**: ruptura de compatibilidad.
  - **MINOR**: compatibilidad hacia atrás, campos opcionales nuevos.
  - **PATCH**: correcciones no funcionales/metadatos.

## 6) Adaptación a distintos entornos

Propuesta de promoción:

1. **dev**: despliegue automático desde `main` con `overlay/dev`.
2. **staging**: despliegue manual (`workflow_dispatch`) con aprobación de environment.
3. **prod**: deploy solo desde tag release (o promoción de imagen ya publicada), con aprobación obligatoria.

Buenas prácticas a activar en GitHub:

- Environments (`dev`, `staging`, `prod`) con reviewers requeridos.
- Branch protection en `main` con required checks (`PR CI / quality-gates`).
- Políticas de retención de artefactos y limpieza de imágenes antiguas.

## 7) Extensiones recomendadas (siguientes pasos)

- Agregar escaneo de vulnerabilidades (`Trivy`/`Grype`) como gate adicional.
- Integrar firma de imágenes (Cosign) y SBOM (Syft).
- Reemplazar credenciales estáticas por OIDC para cloud/k8s.
- Incorporar contract-testing productor/consumidor (ej. Pact o esquema con compatibility checks).
