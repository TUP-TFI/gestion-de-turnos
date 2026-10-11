# Backend — API

API REST del sistema de gestión de turnos. Java + Spring Boot + PostgreSQL.

> Documentación de negocio y modelo de datos: [`/docs`](../docs). Esquema físico y DDL: [`esquema-bd.md`](../docs/tecnologias/esquema-bd.md). Convenciones transversales (idiomas, git, calidad): [`convenciones.md`](../docs/tecnologias/convenciones.md).

## Puesta en marcha

Requisitos: **JDK 21** y **Docker**. Maven no hace falta: el proyecto usa el wrapper `./mvnw`.

```bash
# 1. Levantar PostgreSQL 18 en el puerto 5433
docker compose up -d

# 2. Arrancar la API (perfil dev por defecto)
./mvnw spring-boot:run

# 3. Verificar
curl http://localhost:8080/ping             # {"status":"ok"}
curl http://localhost:8080/actuator/health  # {"status":"UP"}
```

Tests y formato: `./mvnw verify`. No necesitan la base local: los de integración levantan su propio PostgreSQL con Testcontainers (solo hace falta Docker corriendo). Si falla el formato, `./mvnw spotless:apply` lo corrige.

Para frenar la base: `docker compose down` (los datos quedan en el volumen; `-v` los borra).

### Variables de entorno

El perfil `dev` trae valores por defecto que coinciden con el `docker-compose.yml`, así que en local no hay que definir nada. Las variables solo son obligatorias en el perfil `prod`, y están documentadas en [`.env.example`](.env.example). El `.env` real nunca se commitea.

| Variable | Perfil | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` (default) o `prod`. Va en el comando, no en el `.env`: ese archivo lo importa el perfil `prod`, que para leerlo ya tiene que estar activo. |
| `DB_URL` | prod | URL JDBC de la base. En Neon, el host **directo** (sin `-pooler`): Flyway necesita locks de sesión. |
| `DB_USERNAME` | prod | Usuario de la base. |
| `DB_PASSWORD` | prod | Contraseña de la base. |
| `PORT` | prod | Puerto HTTP. Lo inyecta Render; en local vale 8080. |
| `CORS_ALLOWED_ORIGINS` | prod | Origenes que el backend acepta por CORS, separados por coma. Esquema + host, sin path. |
| `JWT_SECRET` | prod | Secreto con el que se firma el JWT de sesión (HS256). Al menos 32 bytes; si es más corto, la app no arranca. En dev hay un valor fijo. |

Para correr en local contra la base de producción, creá un `backend/.env` con esas variables: el perfil `prod` lo importa si existe (`spring.config.import: optional:file:.env[.properties]`).

El [`.env.example`](.env.example) trae dos bloques de base, uno activo y el otro comentado: **A** apunta a Neon y **B** al Postgres de `docker-compose`. El bloque B sirve para probar que el perfil `prod` está bien armado sin tocar los datos reales.

```bash
./mvnw spring-boot:run                              # base local, ignora el .env
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run  # base de Neon, lee el .env
```

> ⚠️ Apuntar a producción desde local aplica las migraciones de Flyway sobre los datos reales. Para experimentar conviene crear una branch en Neon y apuntar el `.env` ahí.

### Perfiles

| Perfil | Base | Notas |
| :--- | :--- | :--- |
| `dev` | `localhost:5433` (docker-compose) | `show-sql: true`. Es el default si no se define `SPRING_PROFILES_ACTIVE`. |
| `prod` | Neon, por variables de entorno | Sin defaults: si falta una variable, la app no arranca. Pool de Hikari limitado a 5 conexiones por el free tier. |

## Deploy

La API se despliega en **Render** (free tier) contra una base **Neon**, ambos en la región Ohio.

- URL: https://gestion-de-turnos-api.onrender.com
- Java no es un runtime nativo de Render, así que el deploy usa el [`Dockerfile`](Dockerfile): etapa de build con `eclipse-temurin:21-jdk` + `./mvnw`, y runtime con `eclipse-temurin:21-jre-alpine`.
- El contenedor corre con `TZ=UTC` y `-Duser.timezone=UTC`, y con `-XX:MaxRAMPercentage=75` por los 512 MB de la instancia.

Configuración del servicio en Render:

| Campo | Valor |
| :--- | :--- |
| Language | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/actuator/health` |
| Auto-Deploy | On Commit |

Probar la imagen antes de subirla:

```bash
docker build -t turnos-backend:test .
```

> ⚠️ El free tier duerme el servicio tras 15 minutos sin tráfico y el arranque en frío tarda alrededor de 2 minutos. Conviene despertarlo antes de una demo.

### CORS

El frontend corre en otro dominio que la API, así que el navegador bloquea las respuestas salvo que el backend las autorice. [`CorsConfig`](src/main/java/com/grupo140/turnos/config/CorsConfig.java) habilita los métodos REST sobre `/**` para los origenes de `cors.allowed-origins`.

| Perfil | Origen permitido |
| :--- | :--- |
| `dev` | `http://localhost:5173` (Vite), fijo en `application-dev.yml`. |
| `prod` | Lo que valga `CORS_ALLOWED_ORIGINS`. En Render: `https://gestion-de-turnos-eight.vercel.app`. |

El valor es un origen (`https://host`): esquema, host y puerto, sin path, porque es lo que el navegador manda en el header `Origin`. La barra final Spring la ignora al comparar, pero un path sí rompe el match y la request queda bloqueada.

Los preview deploys de Vercel usan un subdominio distinto por rama, así que no entran en esta lista.

> 💬 El filtro de seguridad corre antes que el MVC, así que `SecurityConfig` activa CORS aparte con `http.cors(Customizer.withDefaults())`, que reusa esta misma configuración.

## Seguridad y sesión

La API es stateless: cada request lleva `Authorization: Bearer <jwt>`. El token lo emite `POST /api/auth/login` (HS256, vence a las 8 h) y trae `sub` (id del usuario), `role` y `companyId` (ausente para el superadmin).

El admin recién creado activa su cuenta con `POST /api/auth/activate` y el body `{ "token": "...", "password": "..." }`, donde `token` es el valor en claro del link. La contraseña va de 8 a 72 caracteres (72 es el tope de BCrypt). Responde `204` sin JWT: después de activar, el usuario entra por el login.

### Rutas por prefijo

La autorización se define por prefijo en `SecurityConfig`. Los endpoints nuevos tienen que respetarlo:

| Prefijo | Acceso |
| :--- | :--- |
| `/ping`, `/actuator/health`, `POST /api/auth/login`, `POST /api/auth/activate`, `/api/public/**` | Sin sesión |
| `/api/superadmin/**` | Solo `SUPERADMIN` |
| `/api/company/**` | Solo `COMPANY_ADMIN` |
| `/api/client/**` | Solo `CLIENT` |
| Cualquier otra | Autenticado (ej. `GET /api/auth/me`) |

### Formato de error

Toda respuesta de error tiene el cuerpo `{ "code": "...", "message": "..." }`. El `code` es estable y el frontend decide según él; el `message` está en español.

| Código | Status | Cuándo |
| :--- | :--- | :--- |
| `INVALID_CREDENTIALS` | 401 | Email o contraseña incorrectos, o usuario inactivo |
| `ACCOUNT_NOT_ACTIVATED` | 403 | La cuenta todavía no definió contraseña |
| `COMPANY_INACTIVE` | 403 | La empresa del admin está desactivada |
| `INVALID_ACTIVATION_TOKEN` | 400 | El token de activación no existe, ya se usó o la cuenta ya no está pendiente |
| `ACTIVATION_TOKEN_EXPIRED` | 400 | El token de activación venció: hay que pedir el reenvío al superadmin |
| `UNAUTHORIZED` | 401 | Falta el token o es inválido |
| `FORBIDDEN` | 403 | El rol no alcanza para la ruta |
| `VALIDATION_ERROR` | 400 | Body inválido |
| `NOT_FOUND` | 404 | La ruta no existe |
| `INTERNAL_ERROR` | 500 | Error no controlado |

Para devolver un error de negocio, el service lanza `ApiException(status, code, message)` (en `common/error`). El `GlobalExceptionHandler` lo traduce; los services nunca arman la respuesta HTTP.

### Usuario de la sesión

Los controllers declaran un parámetro `SessionUser session` (`userId`, `role`, `companyId`), que se arma desde el JWT. Es la única fuente válida del `companyId` (ver [Aislamiento multi-tenant](#aislamiento-multi-tenant)).

## Estructura de paquetes

> 💬 **Propuesta a confirmar entre ambos antes de escribir la primera clase.** Es la decisión más cara de revertir después.

Organización **por feature**, con las capas adentro de cada una. Con dos personas trabajando en paralelo reduce bastante los conflictos de merge: cada uno toca su carpeta.

```
src/main/java/com/<grupo>/turnos/
├── config/            # configuración de Spring, seguridad, CORS, beans
├── common/            # excepciones, respuestas de error, utilidades compartidas
├── auth/              # login, JWT, activación de cuenta, recuperar contraseña (UserToken)
├── user/              # User: consulta y búsqueda de clientes de una empresa
├── settings/          # PlatformSettings: parámetros globales de la plataforma
├── company/           # Company: CRUD, alta transaccional, activar/desactivar
├── catalog/           # Service: catálogo de servicios de cada empresa
├── schedule/          # BusinessHours + ScheduleException: plantilla de atención
├── appointment/       # Appointment: disponibilidad, reserva, cancelación, reprogramación
├── notification/      # NotificationService desacoplado del canal + reintentos
├── reporting/         # dashboards del superadmin y del admin de empresa
└── seed/              # datos de prueba, solo con el perfil dev
```

Dentro de cada feature:

```
appointment/
├── AppointmentController.java
├── AppointmentService.java
├── AvailabilityService.java
├── AppointmentRepository.java
├── Appointment.java           # entidad JPA
├── job/                       # tareas @Scheduled propias del feature
└── dto/                       # requests y responses
```

### Por qué estos módulos y no los del borrador anterior

| Módulo | Por qué existe |
| :--- | :--- |
| `user` | La búsqueda de clientes para vincular un turno manual (UC8) y el listado de Clientes del panel no son parte de `auth` (no tienen nada que ver con autenticarse) ni de `company`. `User` es la entidad más transversal del modelo y no tenía paquete propio. |
| `settings` | Los tres parámetros globales (anticipación mínima, plazo de cancelación, máximo de reprogramaciones) los edita el superadmin y los consulta `appointment`. No pertenecen a ningún feature de negocio. |
| `reporting` | Los dashboards del superadmin y del admin son queries de agregación que cruzan varias entidades. Meterlos en `company` o en `appointment` los convertiría en cajones de sastre. |
| `schedule` | Pasa a contener también `ScheduleException` (feriados y horarios especiales). |

### ⚠️ Choque de nombres: `Service`

La entidad de dominio se llama `Service` (ver [diccionario de datos](../docs/tecnologias/diccionario-datos.md)) y choca con el sufijo `Service` de la capa de negocio — `ServiceService` no es aceptable.

**Propuesta:** el paquete se llama `catalog`, las clases de aplicación son `CatalogController` / `CatalogService`, y la entidad JPA mantiene el nombre `Service`.

La anotación `@Service` de Spring también choca con la entidad. En los archivos de `catalog` que usen las dos, la anotación se escribe con el nombre completo (`@org.springframework.stereotype.Service`) y la entidad se usa normal.

## Regla de capas (vertical)

**Ésta es la regla que evita el código spaghetti. Si se respeta, casi todo lo demás se acomoda solo.**

```
Controller  ──►  Service  ──►  Repository
```

| Capa | Responsabilidad | Qué NO hace |
| :--- | :--- | :--- |
| **Controller** | Solo HTTP: recibir el request, validar formato, mapear DTOs, devolver códigos de estado. | No contiene lógica de negocio. |
| **Service** | Reglas de negocio y transacciones (`@Transactional`). Único lugar donde vive la lógica. | No arma respuestas HTTP ni toca `HttpServletRequest`. Solo indica el status de un error de negocio al lanzar `ApiException`. |
| **Repository** | Acceso a datos. | No contiene lógica de negocio. |

Reglas duras:

- ❌ Un controller **nunca** llama directo a un repository.
- ❌ Las entidades JPA **no salen** de la capa de service: el controller recibe y devuelve DTOs.
- ❌ Nada de lógica de negocio en los controllers, ni en las entidades.
- ✅ Las validaciones de negocio (superposición de turnos, anticipación mínima, plazos de cancelación) viven en services, con sus tests unitarios.

## Regla de dependencias entre features (horizontal)

La regla de capas ordena lo vertical, pero no dice nada sobre si un feature puede llamar a otro — y ahí es donde aparecen las dependencias circulares. La dirección permitida es **siempre hacia la derecha**:

```
reporting ──┐
            │
appointment ┼──► schedule ──► company ──► settings
            │        │           ▲
user ───────┘     catalog ───────┘
 ▲                               ▲
auth ────────────────────────────┘

notification   ← hoja: no depende de ningún feature de negocio
common, config ← transversales: cualquiera puede usarlos, ellos no usan a nadie
seed           ← solo perfil dev: fuera del grafo (ver excepción abajo)
```

- ❌ Un feature **nunca** importa un repository de otro feature: se habla con su `Service`.
- ❌ No se permiten ciclos. Si aparece uno, se resuelve invirtiendo la dependencia o moviendo la lógica.
- ✅ `auth` depende de `user`: el login y la activación leen y actualizan `User`, pero a través de los services de `user`, nunca de `UserRepository`.
- ⚠️ **Única excepción: el paquete `seed`** (T-02.4). Corre solo con `@Profile("dev")` y carga datos de prueba de varios features, así que puede usar sus repositories directamente. Nada de código de producción importa `seed`, y `seed` no expone nada para los demás features.

### Los dos casos concretos que esta regla resuelve

**1. ¿Dónde vive el cálculo de disponibilidad?**

Necesita dos cosas: la plantilla de atención (`schedule`) y los turnos ya ocupados (`appointment`). Puesto en `schedule`, genera el ciclo `schedule ↔ appointment`.

**Decisión:** `AvailabilityService` vive en **`appointment`**.

- `schedule` es un módulo puro: dada una fecha, responde *"estas son las franjas de inicio candidatas"* aplicando `BusinessHours` y `ScheduleException`. **No sabe que existen los turnos.**
- `appointment` toma esos candidatos y les resta los turnos ocupados, la duración del servicio y la anticipación mínima.

Cada módulo queda con una sola responsabilidad y el grafo sin ciclos.

**2. ¿Cómo avisa `appointment` a `notification` sin depender de él?**

No lo hace directamente. `appointment` publica un evento de dominio y `notification` lo escucha:

```java
// en appointment/
events.publishEvent(new AppointmentBookedEvent(appointmentId));

// en notification/
@TransactionalEventListener(phase = AFTER_COMMIT)
void on(AppointmentBookedEvent event) { ... }
```

Así `notification` queda como hoja del grafo, y el "desacoplado" de la documentación deja de ser solo una intención de diseño para volverse una propiedad estructural verificable. El `AFTER_COMMIT` además garantiza que la llamada HTTP al proveedor no ocurra dentro de la transacción que tiene tomado el lock de la empresa.

## Aislamiento multi-tenant

Es la regla de seguridad más importante del backend y **atraviesa todos los features**.

> Todo método de repository que devuelva datos pertenecientes a una empresa recibe el `companyId` como parámetro obligatorio.

```java
// ✅
Optional<Appointment> findByIdAndCompanyId(UUID id, UUID companyId);

// ❌ nunca, para entidades con dueño
Optional<Appointment> findById(UUID id);
```

El `companyId` sale del JWT de la sesión, nunca del request. Es más verboso que un filtro implícito, pero es imposible de olvidar sin que el código deje de compilar.

> 💬 **Única excepción:** `UserRepository.findByIdAndStatus(id, status)`, que usa la activación de cuenta. Ahí no hay sesión: el token del link prueba la identidad del usuario, y el filtro por `PENDING_ACTIVATION` acota la búsqueda a cuentas pendientes.

Un controller recibe la sesión como parámetro `SessionUser` y le pasa su `companyId` al service:

```java
// ✅ el companyId viene de la sesión (JWT)
@GetMapping("/api/company/appointments/{id}")
public AppointmentResponse get(@PathVariable UUID id, SessionUser session) {
    return appointmentService.get(id, session.companyId());
}

// ❌ nunca desde el request: el cliente podría pedir los datos de otra empresa
public AppointmentResponse get(@PathVariable UUID id, @RequestParam UUID companyId) { ... }
```

Sin esto, el ataque es trivial: un cliente de la empresa A pide `GET /api/appointments/{id}` con el ID de un turno de la empresa B y lo ve completo. Como tercera barrera, las FK compuestas de la base impiden que un registro mezcle empresas aunque las capas anteriores fallen (ver [esquema-bd.md §4](../docs/tecnologias/esquema-bd.md#4-aislamiento-multi-tenant)).

## Base de datos y migraciones

- **Flyway**, con las migraciones en `src/main/resources/db/migration` y formato `V<n>__<descripcion>.sql`.
- `spring.jpa.hibernate.ddl-auto: validate`. Hibernate verifica el esquema, no lo modifica.
- Una migración nunca se edita después de haberse aplicado en un entorno compartido: se agrega una nueva.

El DDL de referencia está en [esquema-bd.md](../docs/tecnologias/esquema-bd.md) y es la base de `V1__initial_schema.sql`. Se eligió Flyway sobre `ddl-auto: update` porque buena parte de las garantías del modelo —índices únicos parciales, `CHECK`, `EXCLUDE USING gist`, FK compuestas, extensiones— Hibernate no las puede generar.

## Convenciones de nombres

- **Endpoints:** REST en inglés, sustantivos en plural — `GET /api/companies/{id}/services`. Versionado a definir.
- **Clases:** `<Feature>Controller`, `<Feature>Service`, `<Feature>Repository`.
- **DTOs:** `CreateAppointmentRequest`, `AppointmentResponse`.
- **Tests:** `<ClaseTesteada>Test` para unitarios, `<ClaseTesteada>IT` para integración.
- **Métodos de test:** nombre descriptivo del caso, en inglés.

## Testing

> ⏳ *Herramientas a confirmar; la estrategia ya está definida.*

**Unitarios** — el grueso de la suite. Services aislados con Mockito, sin levantar el contexto de Spring. Prioridad sobre la lógica crítica:

- Cálculo de disponibilidad: intervalos vs. duración del servicio, jornada partida, precedencia de `ScheduleException` sobre `BusinessHours`, y conversión de hora de pared a UTC con una `company.timezone` distinta de la del proceso.
- Validación de superposición de turnos por rango.
- Anticipación mínima para reservar, plazo máximo de cancelación/reprogramación y tope de reprogramaciones.

**Integración** — `@SpringBootTest` sobre repositories y flujos completos. Puntos clave a cubrir, que no se pueden validar con un test unitario:

- El **lock pesimista** en la reserva concurrente.
- La **constraint de exclusión**: dos `INSERT` superpuestos deben terminar en `ConstraintViolationException` mapeada a 409.
- Los **índices únicos parciales** de email: mismo email en dos empresas distintas debe pasar; repetido en la misma empresa debe fallar.
- Las **FK compuestas**: un turno que apunta a un servicio de otra empresa debe ser rechazado por la base.

> ⚠️ Estos cuatro puntos dependen de constraints específicas de PostgreSQL que **H2 no soporta**. Si se quiere cubrirlos, la elección es **Testcontainers**, no H2.

**Decidido: Testcontainers** (`postgres:18`, la misma versión que Neon y que el `docker-compose.yml`), por los cuatro puntos de arriba. Los tests que los cubren son US-07.3 (lock pesimista) y T-12.4 (los otros tres).

**Cobertura:** no se exige un mínimo en CI. Los tests cubren solo los componentes principales listados arriba, y un porcentaje global bloquearía cualquier PR de CRUD sin tests.

## Integración continua

Workflows en [`.github/workflows`](../.github/workflows). En cada PR contra `main`:

| Check | Workflow | Qué verifica | Bloquea |
| :--- | :--- | :--- | :--- |
| Build, tests y formato | `backend-ci.yml` | `./mvnw verify`: compila, corre unitarios (`*Test`) e integración (`*IT`) contra PostgreSQL 18, aplica las migraciones de Flyway sobre base limpia y chequea formato con Spotless. | Sí |
| Migraciones inmutables | `backend-ci.yml` | Que el PR no modifique, renombre ni borre un `V*.sql` existente. | Sí |
| Imagen Docker | `backend-ci.yml` | Que el `Dockerfile` construya (es lo que despliega Render). Escanea la imagen con Trivy, solo informativo. | Sí (el build) |
| CodeQL (java) | `codeql.yml` | Análisis estático de seguridad. También corre semanalmente. | Sí |
| Dependency review | `dependency-review.yml` | Que el PR no agregue dependencias con CVEs altas o críticas. | Sí |

Los jobs de `backend-ci.yml` se saltean si el PR no toca `backend/`; un job salteado cuenta como aprobado. Dependabot solo abre PRs cuando una dependencia tiene una vulnerabilidad conocida (*Dependabot security updates*, activado en la configuración del repo). No hay actualizaciones de versión periódicas: las actions fijadas por SHA se actualizan a mano.

## Seeds y datos de prueba

`seed/DevDataSeeder` carga los datos mínimos al arrancar, para poder probar el login, la activación de cuenta, los servicios y los horarios sin crear esos datos a mano. 
**No corre solo.** Necesita dos condiciones a la vez: el perfil `dev` y la propiedad `app.seed.enabled=true`. Si falta cualquiera de las dos, el bean no se crea.

```bash
# bash
APP_SEED_ENABLED=true ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:APP_SEED_ENABLED='true'; .\mvnw.cmd spring-boot:run
```

> ⚠️ `app.seed.enabled` **no se define en ningún `application*.yml`**, a propósito. `dev` es el perfil por defecto: si en Render faltara `SPRING_PROFILES_ACTIVE=prod`, la aplicación arrancaría en `dev` contra Neon. Con la propiedad en un archivo, el seed correría ahí y crearía usuarios con una contraseña que está en el repositorio. Pasándola a mano, solo existe en la máquina de quien la escribe.

Si la empresa `barberia-central` ya está en la base, no hace nada, así que un segundo arranque no duplica datos.

Para recrear la base local desde cero con el seed cargado (por ejemplo, para volver a tener el token de activación sin usar), son tres comandos. Borran **toda** la base local, no solo los datos del seed:

```bash
# bash
docker compose down -v
docker compose up -d --wait
APP_SEED_ENABLED=true ./mvnw spring-boot:run
```

```powershell
# PowerShell
docker compose down -v
docker compose up -d --wait
$env:APP_SEED_ENABLED='true'; .\mvnw.cmd spring-boot:run
```

| Dato | Detalle |
| :--- | :--- |
| Empresa | Barbería Central (`/barberia-central`), rubro `BARBERSHOP` |
| Servicios | Corte de pelo (30 min) y Corte y barba (60 min) |
| Horario | Lunes a viernes 09–13 y 16–20, sábado 09–13, domingo cerrado. Intervalo de 30 min |

| Email | Rol | Estado |
| :--- | :--- | :--- |
| `superadmin@example.com` | `SUPERADMIN` | `ACTIVE` |
| `admin@example.com` | `COMPANY_ADMIN` | `ACTIVE` |
| `admin.pendiente@example.com` | `COMPANY_ADMIN` | `PENDING_ACTIVATION` (sin contraseña, con token de activación) |
| `cliente1@example.com` | `CLIENT` | `ACTIVE` |
| `cliente2@example.com` | `CLIENT` | `ACTIVE` |

La contraseña de los usuarios activos y el valor del token de activación están en las constantes `DEV_PASSWORD` y `DEV_ACTIVATION_TOKEN` de `DevDataSeeder`.

Los datos de demo para la presentación (más empresas y turnos en distintos estados) se cargan más adelante, antes del despliegue final.

> La fila única de `platform_settings` **no es parte del seed**: la inserta la migración inicial, porque la aplicación la asume siempre presente.

## Pendiente de definir

- Linter (Checkstyle o similar). El formatter ya está: Spotless con palantir-java-format, en `mvn verify`.
- Documentación de la API (Swagger / OpenAPI).
- Proveedor de la API de WhatsApp Business.
- Storage externo para los logos de empresa (`company.logoUrl`).
- Frecuencia concreta de los jobs programados (cierre de turnos, reintento de notificaciones, purga de tokens).
