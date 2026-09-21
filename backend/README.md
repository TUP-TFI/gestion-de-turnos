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

Tests: `./mvnw test`. Necesitan la base levantada.

Para frenar la base: `docker compose down` (los datos quedan en el volumen; `-v` los borra).

### Variables de entorno

El perfil `dev` trae valores por defecto que coinciden con el `docker-compose.yml`, así que en local no hay que definir nada. Las variables solo son obligatorias en el perfil `prod`, y están documentadas en [`.env.example`](.env.example). El `.env` real nunca se commitea.

| Variable | Perfil | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` (default) o `prod`. |
| `DB_URL` | prod | URL JDBC de la base. En Neon, el host **directo** (sin `-pooler`): Flyway necesita locks de sesión. |
| `DB_USERNAME` | prod | Usuario de la base. |
| `DB_PASSWORD` | prod | Contraseña de la base. |
| `PORT` | prod | Puerto HTTP. Lo inyecta Render; en local vale 8080. |

Para correr en local contra la base de producción, creá un `backend/.env` con esas variables: el perfil `prod` lo importa si existe (`spring.config.import: optional:file:.env[.properties]`).

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
└── reporting/         # dashboards del superadmin y del admin de empresa
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

## Regla de capas (vertical)

**Ésta es la regla que evita el código spaghetti. Si se respeta, casi todo lo demás se acomoda solo.**

```
Controller  ──►  Service  ──►  Repository
```

| Capa | Responsabilidad | Qué NO hace |
| :--- | :--- | :--- |
| **Controller** | Solo HTTP: recibir el request, validar formato, mapear DTOs, devolver códigos de estado. | No contiene lógica de negocio. |
| **Service** | Reglas de negocio y transacciones (`@Transactional`). Único lugar donde vive la lógica. | No conoce nada de HTTP (ni `HttpServletRequest`, ni códigos de estado). |
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
                                 ▲
auth ────────────────────────────┘

notification   ← hoja: no depende de ningún feature de negocio
common, config ← transversales: cualquiera puede usarlos, ellos no usan a nadie
```

- ❌ Un feature **nunca** importa un repository de otro feature: se habla con su `Service`.
- ❌ No se permiten ciclos. Si aparece uno, se resuelve invirtiendo la dependencia o moviendo la lógica.

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

**Pendiente:** definir si se exige una cobertura mínima en CI.

## Seeds y datos de prueba

> ⏳ *A definir.*

Necesarios para levantar el entorno con un superadmin, la fila de `platform_settings`, un par de empresas de ejemplo con servicios, horarios y alguna excepción de calendario cargada, y turnos en distintos estados. A resolver: mecanismo (migración `R__seed.sql` de Flyway limitada al perfil de desarrollo, o un `CommandLineRunner` por perfil) y cómo se aísla del entorno productivo.

> La fila única de `platform_settings` **no es un seed opcional**: la inserta la migración inicial, porque la aplicación la asume siempre presente.

## Pendiente de definir

- Formatter y linter (Spotless, Checkstyle), para sumar al workflow de CI que monta T-01.4.
- Formato exacto del cuerpo de error que devuelve el `@RestControllerAdvice` de US-03.10.
- Documentación de la API (Swagger / OpenAPI).
- Proveedor de la API de WhatsApp Business.
- Storage externo para los logos de empresa (`company.logoUrl`).
- Frecuencia concreta de los jobs programados (cierre de turnos, reintento de notificaciones, purga de tokens).
