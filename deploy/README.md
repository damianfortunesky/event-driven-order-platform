# Deployment en Kubernetes

Este directorio deja el proyecto en estado **deployable en Kubernetes para entorno dev/local** usando **Kustomize**.

## 1) Estructura `deploy/k8s`

```text
deploy/k8s/
├── base/
│   ├── kustomization.yaml
│   ├── order-service.yaml
│   ├── payment-service.yaml
│   ├── inventory-service.yaml
│   └── notification-service.yaml
└── overlays/
    ├── dev/
    │   ├── kustomization.yaml
    │   ├── namespace.yaml
    │   ├── ingress.yaml
    │   ├── patch-images.yaml
    │   ├── patch-config-dev.yaml
    │   └── patch-secrets-dev.yaml
    └── local/
        ├── kustomization.yaml
        ├── namespace.yaml
        ├── patch-images-local.yaml
        ├── patch-config-local.yaml
        └── patch-secrets-local.yaml
```

## 2) Qué incluye

En `base` (común a todos los entornos):
- `Deployment` por servicio (`order`, `payment`, `inventory`, `notification`).
- `Service` ClusterIP por servicio.
- `ConfigMap` por servicio con configuración no sensible.
- `Secret` por servicio con placeholders (`CHANGE_ME`).
- `livenessProbe` + `readinessProbe` apuntando a `/actuator/health/liveness` y `/actuator/health/readiness`.
- `resources` con requests/limits razonables para dev:
  - requests: `cpu: 100m`, `memory: 256Mi`
  - limits: `cpu: 500m`, `memory: 512Mi`

En `overlays/dev`:
- Namespace `eda-dev`.
- Ingress opcional (host `eda-dev.local`).
- Parches de imágenes (`:dev`).
- Parches de config/secrets para dev.

En `overlays/local`:
- Namespace `eda-local`.
- Parches de imágenes (`:local`) y endpoints locales.

## 3) Estrategia de configuración por entorno

Se usa patrón **base + overlays** con Kustomize:
- `base`: manifiestos estables de aplicación.
- `overlays/*`: diferencias por entorno (namespace, imágenes, URLs de DB/Kafka, secretos).

Esto evita duplicar manifests y deja explícitas las variaciones por ambiente.

## 4) Infraestructura: app vs datos/mensajería

Separación recomendada:
- **Infra de aplicación (este repo / esta carpeta):** Deployments, Services, ConfigMaps, Secrets, Ingress.
- **Infra de datos/mensajería (separada):** PostgreSQL y Kafka/Redpanda.

> Nota: para este ejemplo se referencia Kafka como broker simple (`redpanda` o `kafka` por DNS), pensado para **laboratorio/dev**. No se modela aquí un Kafka productivo complejo (multi-AZ, operadores, tuning, etc.).

## 5) Guía paso a paso de deployment

### Requisitos
- Kubernetes 1.27+
- `kubectl`
- `kustomize` (o `kubectl apply -k`)
- Imágenes publicadas de los 4 servicios (o cargadas al cluster local)

### A. Validar manifests
```bash
kustomize build deploy/k8s/overlays/dev
```

### B. Deploy en entorno dev
```bash
kubectl apply -k deploy/k8s/overlays/dev
```

### C. Verificar recursos
```bash
kubectl get ns eda-dev
kubectl -n eda-dev get deploy,svc,ingress,configmap,secret
kubectl -n eda-dev get pods -w
```

### D. Verificar probes
```bash
kubectl -n eda-dev describe pod <pod-name>
kubectl -n eda-dev logs deploy/order-service --tail=100
```

### E. Prueba rápida de conectividad
```bash
kubectl -n eda-dev port-forward svc/order-service 8080:80
curl -s http://localhost:8080/actuator/health
```

## 6) Checklist post-deploy

- [ ] Namespace creado (`eda-dev` o `eda-local`).
- [ ] 4 Deployments en estado `Available=True`.
- [ ] 4 Services resolviendo endpoints (`kubectl get endpoints`).
- [ ] Readiness y liveness en `OK` (sin reinicios frecuentes).
- [ ] Variables de `ConfigMap` aplicadas correctamente.
- [ ] `Secrets` reemplazados por valores reales (no placeholders) en ambientes reales.
- [ ] Conectividad a DB y broker validada desde cada servicio.
- [ ] Ingress responde en host/path esperados (si aplica).

## 7) Rollback mínimo

### Opción 1: rollback de Deployment
```bash
kubectl -n eda-dev rollout history deploy/order-service
kubectl -n eda-dev rollout undo deploy/order-service --to-revision=<N>
kubectl -n eda-dev rollout status deploy/order-service
```

Repetir para `payment-service`, `inventory-service`, `notification-service`.

### Opción 2: rollback declarativo del overlay
Si versionás cambios en Git, volver al commit anterior y reaplicar:
```bash
git checkout <commit-estable>
kubectl apply -k deploy/k8s/overlays/dev
```

### Opción 3: rollback de emergencia (escala a 0)
```bash
kubectl -n eda-dev scale deploy/order-service deploy/payment-service deploy/inventory-service deploy/notification-service --replicas=0
```

Útil para cortar impacto mientras se corrige configuración o dependencia externa.
