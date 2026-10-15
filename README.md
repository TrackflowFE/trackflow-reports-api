# TrackFlow Reports API

API 2 de TrackFlow: HU-03 rastreo, HU-05 tiempos de transito, HU-06 envios
retrasados y HU-07 volumen. Mantiene los contratos HTTP del modulo `reports`.
El monolito se conserva intacto como respaldo.

Repositorio: https://github.com/TrackflowFE/trackflow-reports-api

## Estado del despliegue

URL publica: pendiente de crear/verificar el servicio en Render con las
credenciales del equipo. No se debe tratar una URL estimada como desplegada.
`render.yaml` define el servicio independiente `trackflow-reports-api`, su
Dockerfile y health check. API 1 debe migrar primero la base Supabase hasta V22.

Verificacion local: `mvnw test` y `mvnw verify`, 26 pruebas; arranque HTTP en
8082 con esquema aislado de pruebas y Hibernate `validate`. Las pruebas de
integracion verifican JSON, tres proyecciones, reentregas, rastreo/retrasos,
JWT, roles, sesiones cerradas/inactivas y CORS. El broker real, PostgreSQL de
Supabase y la prueba entre las tres APIs aun requieren el entorno del equipo.

## Ejecucion

Requisitos: JDK 21, PostgreSQL/Supabase con esquema creado por API 1 y RabbitMQ.
No se necesita Maven instalado: el wrapper esta incluido.

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
| `TRACKFLOW_DB_USER`, `TRACKFLOW_DB_PASSWORD` | Credenciales de la base unica |
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

El pool Hikari tiene maximo **3 conexiones**, minimo 1. Hibernate solo valida
el esquema; Flyway esta desactivado y no forma parte de las dependencias.
No regenerar el secreto JWT en Render: debe coincidir entre los tres servicios.

## Endpoints

| Metodo / ruta | Acceso | Historia |
| --- | --- | --- |
| `GET /api/tracking/{trackingNumber}` | Publico | HU-03 |
| `GET /api/reports/transit-times?from=YYYY-MM-DD&to=YYYY-MM-DD` | OPERADOR o ADMIN | HU-05 |
| `GET /api/reports/delayed-shipments` | OPERADOR o ADMIN | HU-06 |
| `GET /api/reports/shipment-volume?from=YYYY-MM-DD&to=YYYY-MM-DD` | OPERADOR o ADMIN | HU-07 |

Fechas de reportes en `America/Bogota`, dias inclusivos; sin fechas, el dia
actual. Fechas invalidas/invertidas o inicio futuro: 400. Rastreo inexistente:
404. Sin datos: 200 con mensaje, no error. Token ausente/invalido: 401; rol
insuficiente: 403. El token se obtiene exclusivamente del login de API 1.

## Contrato RabbitMQ

Esta API **solo consume**. Las colas de solicitudes de registro del monolito
no son eventos de integracion y no deben usarse como sustituto. API 1 y API 3
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
`plannedLegHours`. Los tiempos viajan en ISO-8601 UTC; identificadores numericos.
Datos de centros/ciudades pueden ser null para movimientos historicos; datos
de siguiente parada y duracion son null salvo en despachos. La regla de retraso
usa `plannedLegHours` del evento, nunca tablas de logistics.

Los nombres de exchange/routing keys son configurables porque el PDF no los
fija y el monolito aun publica eventos de dominio dentro del proceso. Deben
confirmarse con los responsables de APIs 1 y 3 antes del corte. Las colas son
propias de reports: no competir con consumidores de otras APIs por una cola.

Cada mensaje confirma las tres proyecciones en una transaccion antes del ACK.
Altas repetidas no borran movimientos; volumen y tiempos son idempotentes.
Movimientos anteriores al alta fallan para reintentar, no se descartan como
exitosos. Las dos colas tienen un consumidor y prefetch 1; el alta puede
avanzar mientras se reintenta un movimiento. Despues de 6 intentos con backoff
(1, 2, 4, 8, 10 segundos), un fallo va a la DLQ durable. Revisar esa cola y
reentregar mensajes desde la administracion del broker una vez corregida la
causa. No borrar proyecciones ni purgar colas de otras APIs.

## Limites y Acoplamientos

- Solo se escriben `reports_shipment_tracking`, `reports_shipment_transit` y
  `reports_shipment_volume`. Se conserva la FK del esquema a ciudades, sin
  consultar ni escribir el catalogo: las etiquetas llegan en los eventos.
- La validacion del token lee `auth_sessions` y `auth_users` de API 1 como
  referencia compartida para respetar logout, expiracion e inactividad. No
  crea, cierra ni renueva sesiones, ni escribe usuarios. La actividad se
  renueva con las llamadas del frontend a `/api/auth/sesion` en API 1 antes
  de cada reporte; otros clientes tambien deben renovar alli su actividad.
  Esta lectura de referencia debe comunicarse al equipo antes del despliegue.
- Reconstruccion: pedir republicacion a API 1 (altas) y luego API 3
  (movimientos), respetando su orden. No se copia el endpoint multimodulo
  del monolito ni el `DataSeeder` a reports.
- El bus usa entrega al menos una vez; productores deben conservar el orden
  de movimientos de cada envio. No hay campo de version/secuencia nuevo en
  el contrato: movimientos distintos con identica `movedAt` dependen del orden
  de entrega. Cambiar ese contrato exige coordinacion de los tres responsables.

## Frontend y Cierre

La rama `feat/reports-api2-routing` de `TrackFlow-Frontend` agrega
`reportsApiBaseUrl`. Solo rastreo y reportes se dirigen a esa URL; login,
historial, rutas y registros conservan su destino. Hasta verificar Render,
el valor vacio conserva el monolito como respaldo, sin inventar una URL publica.

Pendiente para cerrar el entregable del PDF: merge/revision de PRs, API 1
migrada, variables de Render, confirmar publicadores RabbitMQ, URL real en
frontend, y prueba de punta a punta registrar envio -> registrar movimiento
-> consultar rastreo y reporte de retrasados. Render gratuito puede tardar
cerca de un minuto en despertar cada API.
