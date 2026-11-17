# TrackFlow Reports API

API 2 de TrackFlow: HU-03 rastreo, HU-05 tiempos de tránsito, HU-06 envíos
retrasados y HU-07 volumen. Mantiene los contratos HTTP del módulo `reports`.
El monolito se conserva intacto como respaldo.

Repositorio: https://github.com/TrackflowFE/trackflow-reports-api

## Estado del despliegue

URL pública: pendiente de crear/verificar el servicio en Render con las
credenciales del equipo. No se debe tratar una URL estimada como desplegada.
`render.yaml` define el servicio independiente `trackflow-reports-api`, su
Dockerfile y health check. API 1 debe migrar primero la base Supabase hasta V24.

Verificación local: `mvnw test` y `mvnw verify`, 212 pruebas; arranque HTTP en
8082 con esquema aislado de pruebas y Hibernate `validate`. Las pruebas de
integración verifican JSON sin cabeceras de clases Java, tres proyecciones,
rollback atómico ante error de BD, reentregas, rastreo/retrasos,
JWT, roles, sesiones cerradas/inactivas y CORS. El broker real, PostgreSQL de
Supabase y la prueba entre las tres APIs aún requieren el entorno del equipo.
GitHub Actions también verificó Maven y construyó la imagen Docker:
https://github.com/TrackflowFE/trackflow-reports-api/actions/runs/38679103117

## Ejecución

Requisitos: JDK 21, PostgreSQL/Supabase con esquema creado por API 1 y RabbitMQ.
No se necesita Maven instalado: el wrapper está incluido.

```bash
./mvnw test
./mvnw spring-boot:run
```

En Windows: `./mvnw.cmd test` y `./mvnw.cmd spring-boot:run`.
Swagger: `http://localhost:8082/swagger-ui/index.html`.
Health: `GET /actuator/health` (incluye disponibilidad de BD y RabbitMQ).

```bash
docker build -t trackflow-reports-api .
docker run --rm --env-file .env -p 8082:8082 trackflow-reports-api
```

El archivo `.env` debe ser local, nunca versionado. En Render las variables se
definen en el dashboard. No se incluyen migraciones, seeding, login, endpoints
de escritura ni clientes HTTP de otras APIs.

## Variables de Entorno

| Variable | Uso / valor por defecto |
| --- | --- |
| `PORT` | Puerto HTTP, `8082`; Render lo asigna |
| `TRACKFLOW_DB_URL` | JDBC PostgreSQL; en Supabase usar SSL y el pooler del equipo |
| `TRACKFLOW_DB_USER`, `TRACKFLOW_DB_PASSWORD` | Credenciales de la base única |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Broker; local `localhost:5672`, Render TLS `5671` |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | Credenciales/vhost del mismo bus que APIs 1 y 3 |
| `RABBITMQ_SSL` | `false` local, `true` en Render |
| `TRACKFLOW_JWT_SECRET` | Obligatorio, al menos 32 bytes, exactamente el secreto de API 1 |
| `TRACKFLOW_SECURITY_ENABLED` | `true`; `false` solo para desarrollo aislado |
| `TRACKFLOW_INACTIVIDAD_MINUTOS` | `30`, igual a API 1 |
| `TRACKFLOW_CORS_ORIGINS` | Origen(es) completos separados por coma; `http://localhost:5173` |
| `TRACKFLOW_SWAGGER_ENABLED` | `true` |
| `TRACKFLOW_EVENTS_EXCHANGE` | Topic durable `trackflow.events` |
| `TRACKFLOW_SHIPMENT_CREATED_KEY` | `shipment.created` |
| `TRACKFLOW_LOGISTICS_REGISTERED_KEY` | `logistics.event.registered` |
| `TRACKFLOW_REPORTS_SHIPMENT_QUEUE` | `trackflow.reports.shipment-created` |
| `TRACKFLOW_REPORTS_LOGISTICS_QUEUE` | `trackflow.reports.logistics-registered` |
| `TRACKFLOW_REPORTS_DLX` | `trackflow.reports.dlx` |
| `TRACKFLOW_REPORTS_DLQ` | `trackflow.reports.events.dlq` |
| `TRACKFLOW_REPORTS_INICIO_OPERACION` | Primer día con envíos, `2026-01-01`; antes de esa fecha los reportes responden 400 |
| `TRACKFLOW_REPORTS_PERIODO_MAXIMO_DIAS` | Días máximos de un periodo, `366` |
| `TRACKFLOW_TRACKING_LIMITE_IP` | Consultas de rastreo por IP y ventana, `60` |
| `TRACKFLOW_TRACKING_LIMITE_VENTANA_SEGUNDOS` | Ventana del límite de rastreo, `60` |
| `TRACKFLOW_TRACKING_LIMITE_SALTOS_PROXY` | Posición desde la derecha en `X-Forwarded-For`; `0` lo desactiva |
| `TRACKFLOW_TRACKING_LIMITE_PROXIES_CONFIABLES` | IP o rangos CIDR de proxies confiables, separados por coma |

La propiedad Spring `trackflow.reports.retraso.max-horas.in-custody` configura
el límite sin movimiento en custodia, por defecto 24 horas. Puede establecerse
mediante la variable `TRACKFLOW_REPORTS_RETRASO_MAX_HORAS_IN_CUSTODY`.

El pool Hikari tiene máximo **3 conexiones**, mínimo 1. Hibernate solo valida
el esquema; Flyway está desactivado y no forma parte de las dependencias.
No regenerar el secreto JWT en Render: debe coincidir entre los tres servicios.

## Endpoints

| Método / ruta | Acceso | Historia |
| --- | --- | --- |
| `GET /api/tracking/{trackingNumber}` | Público | HU-03 |
| `GET /api/reports/period-limits` | OPERADOR o ADMIN | Límites del periodo para el formulario |
| `GET /api/reports/transit-times?from=YYYY-MM-DD&to=YYYY-MM-DD` | OPERADOR o ADMIN | HU-05 |
| `GET /api/reports/delayed-shipments` | OPERADOR o ADMIN | HU-06 |
| `GET /api/reports/shipment-volume?from=YYYY-MM-DD&to=YYYY-MM-DD` | OPERADOR o ADMIN | HU-07 |

Fechas de reportes en `America/Bogota`, días inclusivos; sin fechas, el día
actual. Fechas inválidas/invertidas, inicio futuro, inicio anterior a
`TRACKFLOW_REPORTS_INICIO_OPERACION` o un rango mayor al máximo: 400 con el
motivo. `GET /api/reports/period-limits` devuelve `{ "minDate", "maxDays" }`
para que el formulario no ofrezca un periodo que el servidor va a rechazar.
Rastreo inexistente: 404. Sin datos: 200 con mensaje, no error. Token
ausente/inválido: 401; rol insuficiente: 403. El token se obtiene
exclusivamente del login de API 1.

### Rastreo público

El rastreo sigue siendo público porque el número de seguimiento es la
credencial del cliente (HU-03 y HU-14). Un anónimo ve el estado, las ciudades,
las fechas y el último punto, pero los nombres del remitente y del destinatario
llegan enmascarados (`L*** G***`), la dirección de entrega siempre es `null` y
la respuesta trae `restricted: true`. Un operador o administrador con token ve
la respuesta completa y `restricted: false`. La respuesta lleva
`Cache-Control: no-store` para que un proxy no guarde la versión del operador.

El número tiene 12 caracteres hexadecimales (16^12 combinaciones), así que
adivinarlo ya es inviable; el límite de consultas es una segunda barrera. Cada
IP puede hacer `TRACKFLOW_TRACKING_LIMITE_IP` consultas por ventana; al
superarlo responde 429 con `Retry-After`. Con `X-Forwarded-For` sin configurar
todos los clientes comparten la cuota del proxy, por eso los proxies
confiables se declaran por IP o rango CIDR.

### Retrasos en días y horas

`GET /api/reports/delayed-shipments` conserva `hoursStopped`, `hoursExpected` y
`hoursOver` y agrega `stopped`, `expected` y `over` con `{ seconds, text }`. El
texto usa días y, si queda una parte menor a un día, horas: `86 d 4 h`, `2 d`,
`23 h` o `menos de 1 h`. Los promedios de `transit-times` siguen en
`1 d 4 h 30 min` porque ahí los minutos sí importan.

## Contrato RabbitMQ

Esta API **solo consume**. Las colas de solicitudes de registro del monolito
no son eventos de integración y no deben usarse como sustituto. API 1 y API 3
deben publicar los contratos siguientes al topic configurado, con mensajes
persistentes, JSON `application/json` y las routing keys acordadas.
El consumidor infiere el tipo por la cola, sin necesitar nombres de clases
Java de los productores ni cambiar los campos existentes.

| Publicador | Evento copiado sin cambios | Routing key |
| --- | --- | --- |
| API 1 | `EnvioCreadoEvent` | `shipment.created` |
| API 3 | `EventoLogisticoRegistradoEvent` | `logistics.event.registered` |

`EnvioCreadoEvent`: `trackingNumber`, `status`, `senderName`, `originCityId`,
`originCity`, `recipientName`, `recipientAddress`, `destinationCityId`,
`destinationCity`, `registeredAt`, `occurredAt`.

`EventoLogisticoRegistradoEvent`: `logisticsEventId`, `trackingNumber`,
`eventType`, `resultingStatus`, `point`, `centerId`, `cityId`, `cityName`,
`movedAt`, `registeredAt`, `occurredAt`, `nextStopCityId`, `nextStopCity`,
`plannedLegHours`. Los tiempos viajan en ISO-8601 UTC; identificadores numéricos.
Datos de centros/ciudades pueden ser null para movimientos históricos; datos
de siguiente parada y duración son null salvo en despachos. La regla de retraso
usa `plannedLegHours` del evento, nunca tablas de logistics.
El movimiento `PICKED_UP` puede llegar con `resultingStatus: IN_CUSTODY` y sin
centro; actualiza el seguimiento y cuenta para retrasos, pero no es un hito de
tiempos de tránsito. API 3 debe producir este evento tras la recogida vinculada.

Los nombres de exchange/routing keys son configurables porque el PDF no los
fija y el monolito aún publica eventos de dominio dentro del proceso. Deben
confirmarse con los responsables de APIs 1 y 3 antes del corte. Las colas son
propias de reports: no competir con consumidores de otras APIs por una cola.

Cada mensaje confirma las tres proyecciones en una transacción antes del ACK.
Altas repetidas no borran movimientos; volumen y tiempos son idempotentes.
Movimientos anteriores al alta fallan para reintentar, no se descartan como
exitosos. Las dos colas tienen un consumidor y prefetch 1; el alta puede
avanzar mientras se reintenta un movimiento. Después de 6 intentos con backoff
(1, 2, 4, 8, 10 segundos), un fallo va a la DLQ durable. Revisar esa cola y
reentregar mensajes desde la administración del broker una vez corregida la
causa. No borrar proyecciones ni purgar colas de otras APIs.

## Límites y Acoplamientos

- Solo se escriben `reports_shipment_tracking`, `reports_shipment_transit` y
  `reports_shipment_volume`. Se conserva la FK del esquema a ciudades, sin
  consultar ni escribir el catálogo: las etiquetas llegan en los eventos.
- La validación del token lee `auth_sessions` y `auth_users` de API 1 como
  referencia compartida para respetar logout, expiración e inactividad. No
  crea, cierra ni renueva sesiones, ni escribe usuarios. La actividad se
  renueva con las llamadas del frontend a `/api/auth/sesion` en API 1 antes
  de cada reporte; otros clientes también deben renovar allí su actividad.
  Esta lectura de referencia debe comunicarse al equipo antes del despliegue.
- Reconstrucción: pedir republicación a API 1 (altas) y luego API 3
  (movimientos), respetando su orden. No se copia el endpoint multimódulo
  del monolito ni el `DataSeeder` a reports.
- El bus usa entrega al menos una vez; productores deben conservar el orden
  de movimientos de cada envío. No hay campo de versión/secuencia nuevo en
  el contrato: movimientos distintos con idéntica `movedAt` dependen del orden
  de entrega. Cambiar ese contrato exige coordinación de los tres responsables.

## Frontend y Cierre

La rama `feat/reports-api2-routing` de `TrackFlow-Frontend` agrega
`reportsApiBaseUrl`. Solo rastreo y reportes se dirigen a esa URL; login,
historial, rutas y registros conservan su destino. Hasta verificar Render,
el valor vacío conserva el monolito como respaldo, sin inventar una URL pública.

Pendiente para cerrar el entregable del PDF: merge/revisión de PRs, API 1
migrada, variables de Render, confirmar publicadores RabbitMQ, URL real en
frontend, y prueba de punta a punta registrar envío -> registrar movimiento
-> consultar rastreo y reporte de retrasados. Render gratuito puede tardar
cerca de un minuto en despertar cada API.
