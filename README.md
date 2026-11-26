# Projet_SOA

## Distributed tracing with Zipkin

The four business services (`dons`, `registre-service`, `inventaire-service`, and
`notification-service`) send Micrometer/Brave traces to Zipkin. The setup traces
HTTP requests, carries trace context over the SOAP and gRPC calls, and enables
Kafka producer/consumer observations so the asynchronous notification work can
be followed from the originating request.

Start the application with Docker Compose and open [Zipkin](http://localhost:9411).
In Compose, sampling is set to `1.0` for local diagnosis. Lower it for production
traffic to reduce trace volume. The local service configuration also defaults to
Zipkin at `localhost:9411`.

## Notification history

`notification-service` stores notification metadata in its own PostgreSQL
database (`notification_db`, host port `5437`). It records the event ID, type,
recipient email, reference, delivery status, attempt count, timestamps, and a
safe error-class summary; it does not persist the rendered email body. Flyway
creates the schema, and records older than 30 days are purged at startup and
daily.

Read the newest records through the authenticated, read-only endpoint:

```text
GET http://localhost:8084/api/notifications?page=0&size=20
```

HTTP Basic credentials are configured with `NOTIFICATION_HISTORY_API_USERNAME`
and `NOTIFICATION_HISTORY_API_PASSWORD`. Compose uses `history` and
`change-me-for-local-development` as local demo defaults; override both before
using this outside local development, and put the endpoint behind HTTPS. Page
size is limited to 100. Failed email sends are recorded and rethrown to Kafka
so the listener container can apply its retry handling; retries update the
same history record for a Kafka message.