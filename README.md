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
0. Ejecutar en consola : export JAVA_TOOL_OPTIONS="-javaagent:./opentelemetry-javaagent.jar -Dotel.service.name=wallet-transfers -Dotel.exporter.otlp.endpoint=http://localhost:4317 -Dotel.metrics.exporter=none"
1. Levantar el stack de observabilidad:
   ```bash
   cd docker
   docker compose up -d
   ```
Servicios:
   - Jaeger UI + OTLP gRPC: http://localhost:16686 (collector expuesto en `127.0.0.1:4317`)
   - WireMock demo (pagos HTTP de salida): http://localhost:8089/__admin

2. Arrancar la aplicación:
   ```bash
   cd ..
   mvn spring-boot:run
   ```
   > El servicio intenta usar el puerto definido en `SERVER_PORT` (default 8080).  
   > Si está ocupado, buscará automáticamente otro disponible y lo indicará en el log de arranque.

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
   - Buscar servicio: `wallet-transfers` (definido en `application.yml`)
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
server:
  port: ${SERVER_PORT:8080}  # puede sobrescribirse via env var y auto-fallback
app:
  server:
    port-fallback-attempts: 20
observability:
  apm:
    vendor: otel
    applicationName: "wallet-platform"
    serviceName: "wallet-transfers"
    serviceVersion: "1.0.0"
    environment: "local"
    endpoint: "http://127.0.0.1:4317"
    metricsEnabled: false
```
> El `endpoint` apunta al Jaeger local (con OTLP habilitado). Es posible exportar métricas, se requiere un Collector propio modificar `metricsEnabled: true`.

## Extensión a otros vendors
Implementar otra clase que extienda `AbstractApmClient` (ej.: `NewRelicApmClient`, `DatadogApmClient`) respetando `ApmClient`.
El dominio (controllers/services/repos) no cambia. El selector de vendor está en `ApmAutoConfiguration`.

## Métricas
El ejemplo incluye métodos para contadores e histogramas vía OpenTelemetry Metrics. Es posible integrar Prometheus/Grafana agregando
un exporter en el Collector (fuera de scope de este prototipo).

## Seguridad y PII (recomendación importante LEER!!!)
- Evitar `recordArgs=true` en `@TraceSpan` para métodos con PII. (es importante definirlos para no estar almacenando datos sensibles)
- Normalizar rutas (`/loans/{id}`) en `setTransactionName` para agregación.

## Troubleshooting
- ¿No se ven los spans? Verificar:
  - La app apunta a `http://127.0.0.1:4317` (Jaeger con OTLP habilitado).
  - Jaeger (docker) corriendo y exponiendo `16686/4317`.
  - Reloj del sistema correcto.
- ¿Puerto 8080 ocupado? Seteá `SERVER_PORT` o deja que el auto-fallback elija uno libre (se puede ver cuando levanta la app de spring boot).
- ¿WireMock en 8089 no responde? Revisar que `docker compose up -d` esté corriendo (servicio `wiremock`).
- Logs de Collector/Jaeger: `docker compose logs -f` dentro de `docker/`.

---

**Licencia**: MIT (uso libre para demos/productivo con las debidas modificaciones).
