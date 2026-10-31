# shipment-service · LogiTrack

Microservicio de **gestión y seguimiento de envíos** de LogiTrack (Desarrollo y Arquitecturas Avanzadas de
Software, FI UNJu 2026). Ciclo de vida de envíos, historial de estados y asignación de repartidores.

| | |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.0.9 · Spring Cloud 2025.1.1 |
| Puerto | `8081` |
| Base de datos | MySQL 8.4 (H2 en memoria para tests) |
| Seguridad | JWT (HS256) emitido por `security-service` |
| Comunicación entre servicios | Spring Cloud OpenFeign hacia `courier-service` (disponibilidad del repartidor) |
| Configuración | `configserver` + repositorio `logitrack-config` (perfiles `dev` / `prod`) |

## Variables de entorno

| Variable | Para qué |
|---|---|
| `DB_USER` / `DB_PASSWORD` | Credenciales de MySQL |
| `JWT_SECRET` | Clave de firma de los JWT (mínimo 32 caracteres, la misma en todos los servicios) |
| `courier.service.url` | URL de `courier-service` (llega por el config server; `http://localhost:8082` en dev, `http://courier-service:8082` en Docker) |
| `CONFIG_SERVER_URL` | URL del config server (por defecto `http://localhost:8888`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (IDE) o `prod` (Docker) |

## Ejecutar

```bash
./mvnw -B clean verify          # compila y corre los tests (no necesitan MySQL ni Docker)
./mvnw spring-boot:run          # requiere configserver y MySQL levantados
```

Swagger UI: http://localhost:8081/swagger-ui.html · Health: http://localhost:8081/actuator/health

## Docker

```bash
docker build -t macostag26/logitrack-shipment-service:dev .
```

## Seguridad

Las clases de seguridad (decoder JWT, respuestas 401/403 y roles) están en el paquete
`ar.edu.unju.fi.logitrack.shipment.security`. Antes vivían en el módulo compartido `common-security` del
monorepo original.

## Comunicación con courier-service (OpenFeign)

Para asignar un repartidor a un envío, `shipment-service` consulta
`GET /api/v1/repartidores/{id}/disponibilidad` de `courier-service` y solo asigna si el repartidor está
disponible.

- `CourierFeignClient`: interfaz `@FeignClient` que declara la llamada.
- `CourierFeignConfig`: reenvía el header `Authorization` del usuario (JWT) a `courier-service`.
- `CourierClientImpl`: usa el cliente Feign y traduce los errores: `404` → 404 (repartidor inexistente);
  `5xx`, timeout o conexión rechazada → 503.
- Timeouts por defecto en `application.yml` (`connectTimeout` 2 s, `readTimeout` 5 s).
