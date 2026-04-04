# Observability

Configuración base profesional para el flujo EDA:

- Prometheus scrape de todos los servicios: `observability/prometheus/prometheus.yml`
- Grafana datasource provisioning: `observability/grafana/provisioning/datasources.yml`
- Grafana dashboards provisioning: `observability/grafana/provisioning/dashboards/dashboards.yml`
- Dashboard base EDA: `observability/grafana/dashboards/eda-overview.json`
- Runbook troubleshooting end-to-end: `docs/runbooks/eda-observability-troubleshooting.md`
