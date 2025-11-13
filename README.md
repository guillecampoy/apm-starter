# APM Starter (OpenTelemetry + Collector + Jaeger)

Proyecto de ejemplo **funcional** que empaqueta una jerarquía de clases para integrar un APM de forma **vendor-agnostic**,
con un **adaptador OpenTelemetry** y un stack local (Docker Compose) con **OpenTelemetry Collector + Jaeger** para ver trazas.
Incluye ejemplos de uso en **Controller, Service, Repository** y **HTTP saliente** con propagación de contexto.

## Objetivos
- Definir un **puerto** (`ApmClient`) y entidades de observabilidad desacopladas del vendor.
- Implementar un **adaptador OpenTelemetry** programático (`OtelApmClient`) con exportación OTLP.
- Exponer ejemplos reales: HTTP entrante/saliente, spans en Service y DB (H2).
- Ejecutar localmente y observar en **Jaeger**.

## Requisitos
- Java 21
- Maven 3.9+
- Docker y Docker Compose

## Puesta en marcha
1. Levantar el stack de observabilidad:
   ```bash
   cd docker
   docker compose up -d
   ```
   Servicios:
   - Jaeger UI: http://localhost:16686
   - OTLP gRPC Collector: `localhost:4317`

2. Arrancar la aplicación:
   ```bash
   cd ..
   mvn spring-boot:run
   ```

3. Probar endpoints (crear spans):
   ```bash
   # flujo "approve loan" con DB + HTTP saliente a WireMock
   curl -X POST http://localhost:8080/loans/1/approve

   # consulta simple
   curl http://localhost:8080/loans/1

   # llamada HTTP saliente "ping external"
   curl http://localhost:8080/external/ping
   ```

4. Ver las trazas en Jaeger:
   - Abrir **Jaeger UI**: http://localhost:16686
   - Buscar servicio: `wallet-transfers` (o el que definas en `application.yml`)
   - Explorar spans: `HTTP GET/POST`, `approve_loan`, `db_*`, `HTTP OUT`

## Estructura
```
apm-starter/
├─ docker/
│  ├─ docker-compose.yml
│  └─ otel-collector-config.yaml
├─ src/main/java/com/acme/...
│  ├─ obs/apm/...(puerto + abstracciones)
│  ├─ obs/apm/impl/OtelApmClient.java (adaptador OpenTelemetry)
│  ├─ obs/apm/TracingAspect + TraceSpan (AOP)
│  ├─ obs/apm/WebTracingFilter (span raíz por request + MDC)
│  ├─ config/ApmAutoConfiguration (beans)
│  └─ apmdemo/... (App, Controller, Service, Repository)
├─ src/main/resources/
│  ├─ application.yml (propiedades observability.apm.*)
│  ├─ schema.sql + data.sql (H2 demo)
└─ pom.xml
```

## Configuración clave
`src/main/resources/application.yml`:
```yaml
observability:
  apm:
    vendor: otel
    applicationName: "wallet-platform"
    serviceName: "wallet-transfers"
    serviceVersion: "1.0.0"
    environment: "local"
    endpoint: "http://localhost:4317"
```
> El `endpoint` apunta al **Collector**. El Collector exporta a **Jaeger** (ver `docker/otel-collector-config.yaml`).

## Extensión a otros vendors
Implementar otra clase que extienda `AbstractApmClient` (ej.: `NewRelicApmClient`, `DatadogApmClient`) respetando `ApmClient`.
El dominio (controllers/services/repos) no cambia. El selector de vendor está en `ApmAutoConfiguration`.

## Métricas
El ejemplo incluye métodos para contadores e histogramas vía OpenTelemetry Metrics. Podés integrar Prometheus/Grafana agregando
un exporter en el Collector (no incluido para mantenerlo simple).

## Seguridad y PII
- Evitá `recordArgs=true` en `@TraceSpan` para métodos con PII.
- Normalizá rutas (`/loans/{id}`) en `setTransactionName` para agregación.

## Troubleshooting
- ¿No ves spans? Verificar:
  - La app apunta al Collector (`endpoint`).
  - Collector escuchando en `4317` y Jaeger en `16686`.
  - Reloj del sistema correcto.
- Logs de Collector/Jaeger: `docker compose logs -f` dentro de `docker/`.

---

**Licencia**: MIT (uso libre para demos/productivo con las debidas modificaciones).
