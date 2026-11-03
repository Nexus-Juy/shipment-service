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

- `CourierFeignClient`: interfaz `@FeignClient` que declara la llamada. No lleva URL: se resuelve por el nombre
  lógico `courier-service` registrado en Eureka (Spring Cloud LoadBalancer elige la instancia).
- `CourierFeignConfig`: configuración propia del cliente (registra el `ErrorDecoder`).
- `CourierErrorDecoder`: traduce las respuestas de error: `404` → 404 (repartidor inexistente); `5xx` u otro
  error → 503.
- `CourierFeignFallbackFactory`: fallback cuando la llamada falla (courier caído, timeout, sin instancias en
  Eureka o circuito abierto) → 503 con `ProblemDetail`. No devuelve un "disponible" inventado.
- `ResilienceConfig`: circuit breaker (Resilience4j). Se abre si falla la mitad de las últimas 10 llamadas (con
  al menos 5 hechas) y queda abierto 10 s; un 404 no cuenta como falla. Límite de tiempo de 6 s.
- `CourierClientImpl`: toma el JWT de la petición en curso y se lo pasa al cliente Feign. El token viaja como
  parámetro (no con un interceptor) porque el circuit breaker puede ejecutar la llamada en otro hilo.
- Timeouts por defecto en `application.yml` (`connectTimeout` 2 s, `readTimeout` 5 s).
