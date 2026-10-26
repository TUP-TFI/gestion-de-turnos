# Backend — API

API REST del sistema de gestión de turnos. Java + Spring Boot + PostgreSQL.

> Documentación de negocio y modelo de datos: [`/docs`](../docs). Esquema físico y DDL: [`esquema-bd.md`](../docs/tecnologias/esquema-bd.md). Convenciones transversales (idiomas, git, calidad): [`convenciones.md`](../docs/tecnologias/convenciones.md).

## Puesta en marcha

Requisitos: **JDK 21** y **Docker**. Maven no hace falta: el proyecto usa el wrapper `./mvnw`.

```bash
# 1. Levantar PostgreSQL 18 en el puerto 5433
docker compose up -d

# 2. Arrancar la API (con Maven, el perfil por defecto es dev)
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
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` o `prod`. Con `./mvnw spring-boot:run`, sin la variable se usa `dev`; el `.jar` sin la variable no arranca (ver [Perfiles](#perfiles)). Va en el comando, no en el `.env`: ese archivo lo importa el perfil `prod`, que para leerlo ya tiene que estar activo. |
| `DB_URL` | prod | URL JDBC de la base. En Neon, el host **directo** (sin `-pooler`): Flyway necesita locks de sesión. |
| `DB_USERNAME` | prod | Usuario de la base. |
| `DB_PASSWORD` | prod | Contraseña de la base. |
| `PORT` | prod | Puerto HTTP. Lo inyecta Render; en local vale 8080. |
| `CORS_ALLOWED_ORIGINS` | prod | Origenes que el backend acepta por CORS, separados por coma. Esquema + host, sin path. |
| `FRONTEND_URL` | prod | Dirección del frontend, sin barra final. Con ella se arma el link de activación (`{FRONTEND_URL}/activate?token=...`). En Render: `https://gestion-de-turnos-eight.vercel.app`. En dev está fija en `http://localhost:5173`. |
| `JWT_SECRET` | prod | Secreto con el que se firma el JWT de sesión (HS256). Al menos 32 bytes; si es más corto, la app no arranca. En dev hay un valor fijo. |
| `APP_SEED_ENABLED` | ambos | `true` carga el seed de demo al arrancar (ver [Seeds y datos de prueba](#seeds-y-datos-de-prueba)). Sin la variable, el seed no corre. |
| `SEED_PASSWORD` | prod | Contraseña de los usuarios activos del seed. Obligatoria solo con `APP_SEED_ENABLED=true`; al menos 8 caracteres. Nunca la de dev. En dev hay un valor fijo. |
| `SEED_ACTIVATION_TOKEN` | prod | Token del link de activación del admin pendiente del seed. Obligatorio solo con `APP_SEED_ENABLED=true`. En dev hay un valor fijo. |

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
| `dev` | `localhost:5433` (docker-compose) | `show-sql: true`. Es el default de `./mvnw spring-boot:run` y de los tests, si no se define `SPRING_PROFILES_ACTIVE`. |
| `prod` | Neon, por variables de entorno | Sin defaults: si falta una variable, la app no arranca. Pool de Hikari limitado a 5 conexiones por el free tier. |

> ⚠️ **No hay perfil por defecto en `application.yml`, a propósito.** El default `dev` lo ponen Maven (`spring-boot:run`) y failsafe (tests) en el `pom.xml`. El `.jar` que corre en Render no lo hereda: si ahí faltara `SPRING_PROFILES_ACTIVE=prod`, la app no arranca, en vez de arrancar en `dev` contra Neon con el secreto del JWT y la contraseña del seed que están en el repositorio. Si corrés la app desde el IDE (botón Run sobre `TurnosApplication`), definí el perfil `dev` en la configuración de ejecución.

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
| Environment | `SPRING_PROFILES_ACTIVE=prod` y las variables `prod` de la tabla de [Variables de entorno](#variables-de-entorno) |
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

La API es stateless: cada request lleva `Authorization: Bearer <jwt>`. El token lo emite `POST /api/auth/login` (HS256, vence a las 8 h) y trae `sub` (id del usuario), `role` y `companyId` (ausente para el superadmin). La audiencia (`aud`) es `panel` para el superadmin y el admin de empresa, y `client` para el cliente final; el decoder no la valida, y lo que separa las rutas es el rol, por prefijo.

El admin recién creado activa su cuenta con `POST /api/auth/activate` y el body `{ "token": "...", "password": "..." }`, donde `token` es el valor en claro del link. La contraseña va de 8 a 72 caracteres (72 es el tope de BCrypt). Responde `204` sin JWT: después de activar, el usuario entra por el login.

Ese link lo genera el alta de empresa, `POST /api/superadmin/companies`, que crea la empresa, su admin (`PENDING_ACTIVATION`) y el token (vence a las 48 h) en una sola transacción. Hasta que exista el envío por WhatsApp, `NotificationService` escribe el link en el log de la aplicación y se copia a mano: en local sale en la terminal, y en producción en la pestaña *Logs* de Render.

Si el link vence o se pierde, el superadmin lo reenvía con `POST /api/superadmin/companies/{id}/resend-activation` (responde `204`): se genera un token nuevo de 48 h y los anteriores dejan de servir. Solo funciona mientras el admin siga `PENDING_ACTIVATION`; si ya activó su cuenta, responde `409 ADMIN_ALREADY_ACTIVE`. Para saber si mostrar esa opción, el detalle de la empresa (`GET /api/superadmin/companies/{id}`) incluye `admin: { email, status }`. El reenvío también funciona con la empresa desactivada: el admin puede definir su contraseña, pero no entra hasta que la empresa se reactive (ver abajo).

### Registro y login de clientes

El cliente final entra desde la página pública de la empresa, por su `slug`. Las dos rutas están bajo `/api/public/**`, así que no piden sesión:

| Ruta | Respuesta |
| :--- | :--- |
| `POST /api/public/companies/{slug}/register` | `201` con `{ token, user }`: el cliente queda con la sesión iniciada. Body: `firstName`, `lastName`, `email`, `phone` y `password` (8 a 72 caracteres), todos obligatorios. `409 EMAIL_ALREADY_IN_USE` si el email ya está en uso en esa empresa. |
| `POST /api/public/companies/{slug}/login` | `200` con `{ token, user }`, o `401 INVALID_CREDENTIALS`. Solo entra un usuario con rol `CLIENT`: un admin con ese email no sirve. |

En las dos, un slug inexistente responde `404 COMPANY_NOT_FOUND` y una empresa desactivada `403 COMPANY_INACTIVE`. El email es único por empresa, sin distinguir mayúsculas: la misma persona puede registrarse en dos empresas distintas con cuentas separadas. El token trae `role = CLIENT` y `companyId`, y sirve para las rutas `/api/client/**`.

### Marca de la empresa

El admin personaliza su página pública con `GET /api/company/branding` y `PUT /api/company/branding`. Las dos usan la empresa de la sesión, nunca una del body ni de la URL. Responden `200` con `{ slug, logoUrl, primaryColor }`. `logoUrl` y `primaryColor` pueden ser `null`; `slug` es de solo lectura y sirve para armar el link a la página pública, porque la sesión del admin solo trae el `companyId`.

El `PUT` reemplaza los dos campos: lo que llega en `null`, o no llega, se borra. `primaryColor` tiene el formato `#RRGGBB` (el mismo `CHECK` de la base) y `logoUrl` hasta 500 caracteres, la misma validación que usa el superadmin al crear o editar la empresa, porque los dos escriben el mismo campo. Un valor inválido responde `400 VALIDATION_ERROR`. No hay storage: el logo es una URL cargada a mano.

### Desactivar y reactivar una empresa

`POST /api/superadmin/companies/{id}/deactivate` y `POST /api/superadmin/companies/{id}/reactivate`, sin body. Responden `200` con la empresa actualizada, o `404 COMPANY_NOT_FOUND`. Si la empresa ya estaba en ese estado, responden `200` igual y no hacen nada.

- **Desactivar** nunca borra la empresa: pasa a `INACTIVE`. El login de su admin queda bloqueado (`403 COMPANY_INACTIVE`), porque se deriva del estado de la empresa y no del usuario. El bloqueo también corta las sesiones abiertas: `ActiveCompanyFilter` revisa el estado de la empresa en cada request con token de un admin o un cliente y responde `403 COMPANY_INACTIVE`, sin esperar a que el token venza. Cuesta una consulta por request; el filtro solo conoce la interfaz `CompanyStatusChecker` (en `common/security`), que implementa `CompanyService`, porque `common` y `config` no pueden depender de `company`. Todos sus turnos `PENDING` se cancelan (`cancelledBy = COMPANY`, con fecha y motivo) y cada cliente recibe un aviso; hasta el Paso 10, el aviso se escribe en el log, igual que el link de activación.
- **Reactivar** la devuelve a `ACTIVE` con los servicios y horarios que ya tenía, y el admin vuelve a entrar con su contraseña. Los turnos cancelados no se recuperan.

La cancelación vive en `appointment`, que no puede ser llamado desde `company` (formaría un ciclo, ver [Regla de dependencias entre features](#regla-de-dependencias-entre-features-horizontal)). `CompanyService` publica `CompanyDeactivatedEvent` y `appointment/CompanyDeactivationListener` lo escucha con `@EventListener`, dentro de la misma transacción: si la cancelación falla, la empresa no queda desactivada.

### Rutas por prefijo

La autorización se define por prefijo en `SecurityConfig`. Los endpoints nuevos tienen que respetarlo:

| Prefijo | Acceso |
| :--- | :--- |
| `/ping`, `/actuator/health`, `POST /api/auth/login`, `POST /api/auth/activate`, `/api/public/**` (incluye el registro y el login de clientes) | Sin sesión |
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
| `COMPANY_INACTIVE` | 403 | La empresa está desactivada: lo reciben el login del admin, el registro y el login de clientes, y cualquier request con una sesión abierta de esa empresa |
| `INVALID_ACTIVATION_TOKEN` | 400 | El token de activación no existe, ya se usó o la cuenta ya no está pendiente |
| `ACTIVATION_TOKEN_EXPIRED` | 400 | El token de activación venció: hay que pedir el reenvío al superadmin |
| `EMAIL_ALREADY_IN_USE` | 409 | El email del admin de la empresa nueva ya es de otro admin o de un superadmin, o el cliente que se registra ya tiene cuenta en esa empresa |
| `COMPANY_NOT_FOUND` | 404 | La empresa pedida no existe: el detalle o la edición del superadmin, o el slug del registro y el login de clientes |
| `ADMIN_ALREADY_ACTIVE` | 409 | Se pidió reenviar el link de activación, pero el admin ya activó su cuenta |
| `COMPANY_WITHOUT_ADMIN` | 409 | Se pidió reenviar el link de activación, pero la empresa no tiene un administrador |
| `SERVICE_NOT_FOUND` | 404 | El servicio pedido no existe o es de otra empresa |
| `INVALID_BUSINESS_HOURS` | 400 | El horario semanal tiene una franja que termina antes de empezar, o dos franjas del mismo día superpuestas |
| `UNAUTHORIZED` | 401 | Falta el token o es inválido |
| `FORBIDDEN` | 403 | El rol no alcanza para la ruta |
| `VALIDATION_ERROR` | 400 | Body inválido, o un valor de la dirección con formato incorrecto (por ejemplo, un id que no es UUID) |
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
└── seed/              # datos de prueba, solo con app.seed.enabled=true
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
| `settings` | Los cuatro parámetros globales (anticipación mínima, plazo de cancelación, máximo de reprogramaciones, anticipación máxima) los edita el superadmin y los consulta `appointment`. No pertenecen a ningún feature de negocio. |
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
seed           ← solo con app.seed.enabled: fuera del grafo (ver excepción abajo)
```

- ❌ Un feature **nunca** importa un repository de otro feature: se habla con su `Service`.
- ❌ No se permiten ciclos. Si aparece uno, se resuelve invirtiendo la dependencia o moviendo la lógica.
- ✅ `auth` depende de `user`: el login y la activación leen y actualizan `User`, pero a través de los services de `user`, nunca de `UserRepository`.
- ✅ `appointment` depende de `user` y de `notification`: al cancelar turnos busca el teléfono del cliente con `UserService.findPhone` y lo avisa con `NotificationService`. Ninguno de los dos depende de `appointment`, así que no hay ciclo.
- ⚠️ **Excepción: el alta de empresa** (US-04.1). `CompanyService.create` crea en una sola transacción la empresa, su admin y el token de activación, así que `company` usa `UserService` (de `user`), `ActivationService` (de `auth`) y `NotificationService`. Como `auth` ya usa `CompanyService` en el login, entre `auth` y `company` queda un ciclo de paquetes. Se acepta porque no hay ciclo entre clases (`ActivationService` no depende de `CompanyService`) y porque evitarlo obligaría a mover el token de activación fuera de `auth`. Sigue valiendo que `company` no importa los repositories de esos features.
- ⚠️ **Otra excepción: el paquete `seed`** (T-02.4). Corre solo con `app.seed.enabled=true` y carga datos de prueba de varios features, así que puede usar sus repositories directamente. Nada de código de producción importa `seed`, y `seed` no expone nada para los demás features.

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

> 💬 **Excepciones:**
>
> - `UserRepository.findByIdAndStatus(id, status)`, que usa la activación de cuenta. Ahí no hay sesión: el token del link prueba la identidad del usuario, y el filtro por `PENDING_ACTIVATION` acota la búsqueda a cuentas pendientes.
> - `CompanyRepository.findById(id)` y `findAllByOrderByNameAsc()`, que usan el listado y la edición de empresas. La empresa es el propio tenant, no un dato que pertenezca a una, y solo el superadmin llega a esas consultas (`/api/superadmin/**`).
> - `CompanyRepository.countByStatus(status)` y `AppointmentRepository.count()`, que usa el dashboard del superadmin (`GET /api/superadmin/stats`). Devuelven un solo número de toda la plataforma, no datos de una empresa.

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

`seed/DemoDataSeeder` carga datos de prueba al arrancar, para poder probar el login, la activación de cuenta, los servicios y los horarios sin crear esos datos a mano. Corre en local, en los tests de integración y en Render, para que el tutor pruebe la UI desplegada.

**No corre solo.** Necesita la propiedad `app.seed.enabled=true`, en cualquier perfil. Sin ella, el bean no se crea.

```bash
# bash
APP_SEED_ENABLED=true ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:APP_SEED_ENABLED='true'; .\mvnw.cmd spring-boot:run
```

> ⚠️ `app.seed.enabled` **no se define en ningún `application*.yml`**, a propósito: se prende a mano en local y con la variable `APP_SEED_ENABLED` en Render. Apagarlo es sacar la variable.
>
> En Render tiene que estar `SPRING_PROFILES_ACTIVE=prod`. Sin perfil, la app no arranca (ver [Perfiles](#perfiles)), así que el seed nunca corre con los valores de dev contra Neon.

### Contraseña y token

La contraseña de los usuarios activos y el token de activación del admin pendiente no están en el código: salen de `app.seed.password` y `app.seed.activation-token`.

| Perfil | De dónde salen |
| :--- | :--- |
| `dev` | Valores fijos en `application-dev.yml`: contraseña `Turnos.dev.2026`, token `dev-activation-token`. Los tests los repiten en `SeedTestValues`. |
| `prod` | Variables `SEED_PASSWORD` y `SEED_ACTIVATION_TOKEN`, sin valor por defecto. Con el seed prendido y sin ellas, la app no arranca. |

> ⚠️ En Render, **nunca** usar los valores de dev: están en el repositorio, y cualquiera podría entrar como superadmin. Las credenciales de Render se pasan por un canal privado, nunca por el repo ni por un issue.

### Cada arranque crea solo lo que falta

Cada bloque del seed revisa su propia clave (slug de la empresa, email del usuario) y crea solo lo que no existe. Render vuelve a correr el seed en cada deploy, así que:

- Un segundo arranque no duplica datos.
- Lo que alguien cambió desde la UI no se pisa. Los servicios y el horario de una empresa se crean solo junto con la empresa: si alguien los borra, no vuelven.
- **Al agregar una funcionalidad**, sumá su bloque al seed con su propio chequeo de existencia (por ejemplo, `seedAppointments()` que no crea nada si la empresa ya tiene turnos). El próximo deploy lo carga en la base que ya tenía el resto. Actualizá también las tablas de abajo.

`TurnosApplicationIT` corre el seed dos veces sobre la misma base para verificar que no choca con los índices únicos.

### Recrear la base local

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

### Datos que carga

| Dato | Detalle |
| :--- | :--- |
| Empresa | Barbería Central (`/barberia-central`), rubro `BARBERSHOP` |
| Servicios | Corte de pelo (30 min) y Corte y barba (60 min) |
| Horario | Lunes a viernes 09–13 y 16–20, sábado 09–13, domingo cerrado. Intervalo de 30 min |
| Segunda empresa | Estética Pendiente (`/estetica-pendiente`), rubro `BEAUTY_CENTER`, sin servicios ni horarios: como queda una empresa recién dada de alta |

Cada empresa tiene un solo admin, igual que las que crea el alta.

| Email | Rol | Empresa | Estado |
| :--- | :--- | :--- | :--- |
| `superadmin@example.com` | `SUPERADMIN` | — | `ACTIVE` |
| `admin@example.com` | `COMPANY_ADMIN` | Barbería Central | `ACTIVE` |
| `cliente1@example.com` | `CLIENT` | Barbería Central | `ACTIVE` |
| `cliente2@example.com` | `CLIENT` | Barbería Central | `ACTIVE` |
| `admin.pendiente@example.com` | `COMPANY_ADMIN` | Estética Pendiente | `PENDING_ACTIVATION` (sin contraseña, con token de activación) |

Los usuarios activos entran directo con la contraseña del seed, sin pasar por el link de activación. El admin pendiente se activa en `{FRONTEND_URL}/activate?token=<token del seed>`; el token sirve una sola vez.

Los datos de demo para la presentación (más empresas y turnos en distintos estados) se suman a este mismo seed más adelante, antes del despliegue final.

> La fila única de `platform_settings` **no es parte del seed**: la inserta la migración inicial, porque la aplicación la asume siempre presente.

## Pendiente de definir

- Linter (Checkstyle o similar). El formatter ya está: Spotless con palantir-java-format, en `mvn verify`.
- Documentación de la API (Swagger / OpenAPI).
- Proveedor de la API de WhatsApp Business.
- Storage externo para los logos de empresa (`company.logoUrl`).
- Frecuencia concreta de los jobs programados (cierre de turnos, reintento de notificaciones, purga de tokens).
- Mostrar el link de activación en la UI del superadmin mientras no exista el envío por WhatsApp. Hoy el alta de empresa y el reenvío solo lo escriben en el log, y quien prueba el despliegue sin acceso a Render no puede activar al admin de una empresa nueva. Propuesta: que `POST /api/superadmin/companies` y `POST .../resend-activation` devuelvan el link en la respuesta (solo mientras `NotificationService` sea el de log) y que el frontend lo muestre para copiarlo. PR aparte, back y front.
- Paginación del listado de empresas (`GET /api/superadmin/companies`). Por el tamaño del MVP devuelve todas juntas; se puede agregar después del MVP si la cantidad de empresas lo pide.
