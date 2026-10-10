# Alcance del MVP y roadmap

Roadmap de historias de usuario y tareas técnicas del proyecto, con el alcance del MVP ya aplicado. Es la única versión vigente del roadmap: reemplaza a `tareas_v2.md`, que queda solo como el original histórico.

Las primeras secciones resumen qué cambió respecto del roadmap original y por qué. Debajo está el roadmap completo, con cada cambio aplicado en su historia y marcado con ✂️ **Alcance MVP**.

## Motivo

El objetivo es cerrar el proyecto a tiempo. Para eso se prioriza el núcleo que sostiene la defensa (aislamiento por empresa, disponibilidad, reserva sin doble turno) y se recortan las piezas que aportan poco a esa demostración.

## Recortes

| Ítem del roadmap | Decisión | Alcance que queda | Responsable |
| :--- | :--- | :--- | :--- |
| US-03.7 — Admin recupera su contraseña y US-03.8 — Cliente recupera su contraseña | Pospuestas juntas (opcionales) | Comparten el mecanismo de token y envío del link de la activación (US-03.4: por WhatsApp, con mail solo como reemplazo si no se llega con WhatsApp): si sobra tiempo al final del Paso 3, se hacen en un solo PR. Mientras tanto, un admin sin acceso puede pedir ayuda al superadmin, y un cliente que olvida su contraseña no tiene cómo recuperarla: se acepta para el MVP. | Nico |
| US-03.9 y US-03.10 | Unificadas con US-03.1/US-03.2 | Se entregan en un solo PR de autenticación: login, JWT, `companyId` de la sesión y formato único de error. Mismo alcance, menos ciclos de review. | Nico |
| US-04.6 — Dashboard del superadmin | Reducida | Dos indicadores: empresas activas/inactivas y cantidad total de turnos. Sin altas por mes, sin ranking por empresa, sin distribución por rubro y sin gráficos. | Brune (back) / Nico (front) |
| T-12.3 — Cobertura de tests por regla de negocio | Reducida | Tests solo de las reglas críticas: cálculo de disponibilidad (incluida la zona horaria), concurrencia y doble reserva, anticipación mínima y plazos de cancelación y reprogramación. No se recorre regla por regla. | Nico y Brune |
| Paso 13 — Documentación y pulido | Reducido | Se actualiza solo la documentación que haya quedado desfasada respecto de lo construido. Sin revisión cruzada completa por módulo. | Nico y Brune |

## Reasignaciones

Para emparejar la carga de back entre los dos:

| Ítem del roadmap | Nuevo responsable | Nota |
| :--- | :--- | :--- |
| US-05.6 — Excepción de calendario (back) | Brune | Ya tiene el front de esa historia. El conteo de turnos afectados se coordina con Nico. |
| US-10.4 — Reintento de notificaciones | Brune | Usa `Notification`; casi no se cruza con el resto. |

## Decisiones de diseño

- **Email único global para `COMPANY_ADMIN`.** El login del panel (`/login`) recibe solo email y contraseña, sin empresa, y lo usan tanto los admins como el superadmin. Como el índice de la base permite el mismo email en dos empresas, y también separa a los superadmins de los usuarios con empresa, el login podría encontrar más de un usuario con el mismo email y no sabría cuál elegir. Se resuelve validando en el alta de empresa (US-04.1) que el email del admin no exista como `COMPANY_ADMIN` en ninguna otra empresa ni como `SUPERADMIN`. No requiere migración. Los clientes siguen con email único por empresa.
- **Superadmin en producción.** El seed corre solo en desarrollo, así que en producción no hay ningún superadmin y nadie podría dar de alta la primera empresa. Se carga un solo superadmin a mano, una vez, en la base de producción. El procedimiento (cómo generar el hash BCrypt de la contraseña y el `INSERT` exacto) se documenta en el README del backend, para poder repetirlo si se recrea la base.
- **Un solo PR de autenticación.** US-03.1, US-03.2, US-03.9 y US-03.10 comparten infraestructura (Spring Security, JWT, manejador de errores) y se prueban juntas.

## Lo que no se recorta

Estas historias son el núcleo de la defensa y se mantienen completas:

| Historia | Motivo |
| :--- | :--- |
| US-03.9 — Aislamiento de datos por empresa | Es la regla de seguridad más importante del sistema multi-tenant. |
| US-05.5 — Cálculo de disponibilidad | Es la lógica central del producto, incluida la conversión con `company.timezone`. |
| US-07.3 — Sin doble reserva | Lock pesimista más constraint de exclusión: es la garantía más fuerte del modelo. |
| US-07.4 — Anticipación mínima | Regla configurable desde `PlatformSettings`, compartida con el cálculo de disponibilidad. |

## Confirmado

- **Con Brune:** los recortes que afectan su parte (US-04.6, T-12.3, Paso 13), la validación del email global de admin en el backend de US-04.1 y las reasignaciones.

## Pendiente de confirmar

- Que la copia de `tareas_v2.md` usada como base de este roadmap sea igual a la de Brune.

---

## Roadmap

Desglose paso a paso de las 15 etapas de desarrollo del proyecto. Los Pasos 1 y 2 son puesta a punto técnica (sin pantalla de usuario todavía) y se listan como tareas técnicas, no como historias de usuario. Del Paso 3 en adelante se usa el formato de historia de usuario ("Como [rol] quiero... para...") con criterios de aceptación simples.

> **Convención de responsables:** cada tarea tiene un backend y un frontend, cada uno con su responsable. Los roles (quién hace backend, quién hace frontend) se alternan por paso a lo largo del proyecto para que ambos toquen backend y frontend en proporción similar. Notificaciones (Paso 10) queda para Nico por su mayor experiencia integrando WhatsApp. Si en algún momento la carga se siente desbalanceada, las tareas se pueden reasignar distinto — por ejemplo, Brune enfocándose más en el pulido y testing de los últimos pasos para compensar.

---

### Paso 1 — Poner todo a andar

#### T-01.1 — Backend desplegado con endpoint de prueba
Responsable: Nico

Tareas:
- Crear el proyecto Spring Boot (Maven).
- Definir la estructura de paquetes por feature (ver arquitectura.md / README del backend).
- Configurar la conexión a PostgreSQL (Neon).
- Desplegar en Render con un endpoint tipo `GET /ping` que responda algo simple.

Listo cuando: el endpoint de prueba responde correctamente desde la URL pública de Render.

#### T-01.2 — Frontend desplegado con pantalla vacía
Responsable: Brune

Tareas:
- Crear el proyecto Vite + React + TS.
- Definir la estructura de carpetas (ver README del frontend: `api/`, `pages/`, `components/`, `hooks/`, `context/`, `types/`, `lib/`).
- Configurar Tailwind: instalar el paquete, agregar el plugin de Vite (o PostCSS), y sumar las directivas de Tailwind al CSS global.
- Desplegar en Vercel (aunque sea una pantalla vacía).

Listo cuando: la URL de Vercel carga sin errores.

#### T-01.3 — Conectar frontend y backend desplegados

- **Backend (Nico):** habilitar CORS en el backend desplegado para aceptar requests desde el dominio de Vercel.
- **Frontend (Brune):** desde el frontend ya desplegado, hacer un pedido real (`fetch`) al `/ping` del backend, y confirmar que la respuesta se muestra en pantalla (aunque sea texto plano).

Listo cuando: el frontend desplegado en Vercel le pega al backend desplegado en Render y muestra la respuesta.

Nota: acá alcanza con confirmar a mano que los dos entornos ya desplegados se hablan entre sí. Que las pruebas corran solas en cada Pull Request es la tarea siguiente (T-01.4).

#### T-01.4 — Integración continua en cada Pull Request

`convenciones.md` pone el CI como el segundo factor de calidad de código, después de la regla de capas: *"una regla que verifica una máquina vale más que una escrita"*. Se configura ahora, con el proyecto vacío, porque después nadie frena a armarlo.

- **Backend (Nico):** workflow de GitHub Actions que corre el build y los tests del backend en cada PR contra `main`.
- **Frontend (Brune):** workflow que corre el build y los tests del frontend en cada PR contra `main`.

Tareas:
- Configurar la rama `main` como protegida: sin merge si el workflow falla.
- Dejar anotado que, cuando se elijan formatter y linter (pendiente en ambos READMEs), se suman a estos mismos workflows.

Listo cuando: abrir un PR dispara los dos workflows y un test roto bloquea el merge.

---

### Paso 2 — El modelo de datos completo

Listo cuando (del paso completo): la base tiene las 9 tablas creadas, el seed cargado, y los dos entienden el modelo completo — no solo "su" parte.

#### T-02.1 — Entidades JPA
El diccionario de datos define **9 entidades**: `PlatformSettings`, `User`, `UserToken`, `Company`, `Service`, `BusinessHours`, `ScheduleException`, `Appointment`, `Notification`. Divididas para no pisarse:

- **Backend (Nico):** `User`, `UserToken`, `PlatformSettings`, `Appointment` — el eje de sesión/autenticación más el corazón transaccional del turno.
- **Backend (Brune):** `Company`, `Service`, `BusinessHours`, `ScheduleException`, `Notification` — catálogo, configuración de agenda y notificaciones.

El tipo de PK es `UUID` en las 9 entidades (`gen_random_uuid()`), según el diccionario de datos, justamente para que los IDs no sean enumerables en URLs públicas.

Listo cuando: las 9 entidades compilan con sus relaciones (FK, incluidas las compuestas de `Appointment` hacia `Service` y `User`) definidas.

#### T-02.2 — Migración inicial de Flyway
Responsable: Nico (backend del Paso 3, donde se usa primero).

El esquema arranca con **una sola migración**, `V1__initial_schema.sql`, con el DDL ya escrito y validado en [`esquema-bd.md`](tecnologias/esquema-bd.md). No se reparte una migración por entidad, por tres motivos:

- Los `V<n>` se ordenan **globalmente**: dos personas numerando en paralelo colisionan en el mismo número y Flyway falla al arrancar.
- Las tablas no son independientes entre sí. `appointment` necesita la extensión `btree_gist` para su constraint de exclusión, y los `UNIQUE (id, company_id)` de `service` y `app_user` para sus FK compuestas — o sea que las tablas de uno dependen de las del otro.
- El DDL ya está escrito y probado contra PostgreSQL 16; dividirlo es reescribirlo.

El reparto del Paso 2 sigue existiendo, pero está en T-02.1 (entidades JPA) y T-02.3 (tipos TS), que sí son divisibles sin acoplamiento.

Tareas:
- Copiar el DDL de `esquema-bd.md` a `src/main/resources/db/migration/V1__initial_schema.sql`, incluidas las extensiones (`pgcrypto`, `btree_gist`) y el `INSERT` de la fila única de `platform_settings`.
- Configurar `spring.jpa.hibernate.ddl-auto: validate`, para que Hibernate verifique el esquema contra las entidades de T-02.1 sin modificarlo.
- Verificar que corre limpia sobre una base vacía y que la validación de Hibernate pasa.

Listo cuando: `V1__initial_schema.sql` crea las 9 tablas con sus índices y constraints, la fila de `platform_settings` queda insertada, y la aplicación levanta con `ddl-auto: validate` sin reportar diferencias.

> A partir de acá, una migración aplicada **nunca se edita**: cada cambio de esquema es una `V2__`, `V3__`… nueva. Para evitar colisiones de número entre los dos, conviene avisar en el canal antes de crear una.

#### T-02.3 — Tipos TypeScript espejados
Mismo criterio de división que T-02.1, para que cada uno tipe lo que ya modeló:

- **Nico:** tipos de `User`, `UserToken`, `PlatformSettings`, `Appointment`.
- **Brune:** tipos de `Company`, `Service`, `BusinessHours`, `ScheduleException`, `Notification`.

Tareas:
- Crear en `types/` los tipos TS, `PascalCase` en inglés, alineados en nombre de campos (`camelCase`) al diccionario de datos.

Listo cuando: existen los 9 tipos y coinciden campo a campo con las entidades JPA.

#### T-02.4 — Seed mínimo
Responsable: Brune

El seed no es decorativo: hay cuatro historias del Paso 3 al 6 que dependen de él para poder probarse de forma aislada, sin esperar a que el flujo que crea esos datos esté terminado.

Tareas — cargar, vía un `CommandLineRunner` restringido al perfil local/dev (`@Profile("dev")`):
- Un **superadmin** en `ACTIVE` → US-03.1.
- Una **empresa de ejemplo** con `timezone` cargada, un par de **servicios** y su **horario semanal** (`BusinessHours`, con al menos un día de jornada partida) → US-05.5, US-06.1, US-06.2.
- Su **admin en `ACTIVE`**, con contraseña ya definida → US-03.2, que se prueba sin necesidad de tener lista la activación.
- Un segundo **admin en `PENDING_ACTIVATION`** con su `UserToken` de tipo `ACTIVATION` sin usar → US-03.4, que se prueba sin necesidad de tener listo el alta de empresa del Paso 4.
- Un par de **clientes** de esa empresa → US-03.6, US-09.4, US-09.7.
- No tocar `platform_settings`: esa fila ya la insertó la migración inicial (T-02.2), no es parte del seed.

Listo cuando: al levantar el proyecto localmente, la base queda con el seed cargado y las historias de arriba se pueden probar cada una por su cuenta.

---

### Paso 3 — Login y sesiones

Listo cuando (del paso completo): un superadmin, un admin de empresa y un cliente se pueden loguear cada uno en su pantalla correcta, y las dos reglas transversales del backend —aislamiento por empresa y formato único de error— quedan montadas antes de que se escriba el primer endpoint de negocio.

#### US-03.1 — Superadmin inicia sesión en el panel de sistema

> ✂️ **Alcance MVP:** US-03.1, US-03.2, US-03.9 y US-03.10 se entregan en un solo PR de autenticación (login, JWT, `companyId` de la sesión y formato único de error). Mismo alcance, menos ciclos de review.

Como superadmin, quiero loguearme con email y contraseña en `/login`, para acceder a mi dashboard.

Criterios de aceptación:
- Login válido redirige a `/superadmin/dashboard`.
- Login inválido muestra un error genérico, sin indicar si falló el email o la contraseña.
- El JWT devuelto incluye `userId` y `role=SUPERADMIN` (sin `companyId`).

Tareas técnicas:
- Backend (Nico): endpoint de login que valida contra `User` y genera el JWT.
- Frontend (Brune): pantalla de login del panel de sistema, guardar el JWT, redirect según `role`.

Dependencias: Paso 2 (entidad `User`) + seed con un superadmin cargado.

#### US-03.2 — Admin de empresa inicia sesión en el panel de sistema
Como admin de empresa ya activo, quiero loguearme con email y contraseña en `/login`, para acceder al dashboard de mi negocio.

Criterios de aceptación:
- Login válido redirige a `/company/dashboard`.
- El JWT incluye `userId`, `role=COMPANY_ADMIN` y `companyId`.
- Un admin en estado `PENDING_ACTIVATION` no puede loguearse todavía — ver US-03.4.

Tareas técnicas:
- Backend (Nico): mismo endpoint que US-03.1, armando el JWT según el `role`; bloquear login si `status = PENDING_ACTIVATION`.
- Frontend (Brune): mismo formulario, redirect según el `role` recibido.

Dependencias: US-03.1 (comparten endpoint y pantalla). No hace falta tener US-03.4 (activación) terminada para probar esta historia — alcanza con seedear un admin directo en `ACTIVE` (ver T-02.4).

#### US-03.3 — Rutas protegidas según el rol
Como sistema, necesito que `/superadmin/*` y `/company/*` solo sean accesibles para el rol correspondiente, para que un admin no pueda entrar al panel de superadmin y viceversa.

Criterios de aceptación:
- Un usuario sin sesión que intenta entrar a una ruta protegida es redirigido a `/login`.
- Un `COMPANY_ADMIN` que intenta entrar a `/superadmin/*` es redirigido, no ve la pantalla.
- Un `SUPERADMIN` que intenta entrar a `/company/*` es redirigido (no tiene `companyId` asociado).

Tareas técnicas:
- Frontend (Brune): componente de ruta protegida que lee el `role` del JWT guardado.

Dependencias: US-03.1, US-03.2.

#### US-03.4 — Admin activa su cuenta con el link de activación
Como admin de empresa recién creado, quiero abrir el link de activación y definir mi contraseña, para poder loguearme por primera vez.

Criterios de aceptación:
- El link contiene un token único de un solo uso asociado a ese `User`.
- Al confirmar la contraseña, el `User` pasa de `PENDING_ACTIVATION` a `ACTIVE`.
- Un token ya usado o vencido muestra un error y no permite activar la cuenta desde ahí.

Tareas técnicas:
- Backend (Nico): endpoint de activación (validar token, setear password, cambiar status).
- Frontend (Brune): pantalla "definí tu contraseña" que recibe el token por URL.

Dependencias: el link en sí lo genera el Paso 4 (alta de empresa). Para probar esta historia de forma aislada, puede insertarse manualmente un `User` en `PENDING_ACTIVATION` con su token en el seed o directo en la base.

#### US-03.5 — Cliente se registra en la página pública de una empresa
Como visitante de la página pública de una empresa, quiero registrarme con email y contraseña, para poder reservar turnos en esa empresa.

Criterios de aceptación:
- El registro crea un `User` con `role=CLIENT` y `companyId` fijo a esa empresa.
- Si el email ya está registrado como cliente de otra empresa, el registro igual funciona (son cuentas independientes).
- Si el email ya está registrado como cliente de esa misma empresa, se rechaza con un mensaje claro.

Tareas técnicas:
- Backend (Nico): endpoint de registro de cliente, scoped por el `companyId` que resuelve el `{slug}`.
- Frontend (Brune): formulario de registro dentro de `/{slug}`.

Dependencias: Paso 2 (entidad `User`) — alcanza con una empresa cargada en el seed para probarla.

#### US-03.6 — Cliente inicia sesión en la página pública de una empresa
Como cliente ya registrado en una empresa, quiero loguearme con email y contraseña dentro de `/{slug}`, para reservar turnos en esa empresa.

Criterios de aceptación:
- El JWT recibido tiene `companyId` fijo a esa empresa; no sirve para operar en otra.
- El login funciona con ese email aunque el mismo email exista como cliente en otra empresa (son cuentas distintas).

Tareas técnicas:
- Backend (Nico): endpoint de login scoped por `companyId`.
- Frontend (Brune): formulario de login dentro de `/{slug}`, sesión separada de la del panel de sistema.

Dependencias: US-03.5.

#### US-03.7 — Admin recupera su contraseña olvidada

> ✂️ **Alcance MVP:** **pospuesta (opcional)**, junto con US-03.8. Comparten el mecanismo de token y envío del link de la activación (US-03.4: por WhatsApp, con mail solo como reemplazo si no se llega con WhatsApp): si sobra tiempo al final del Paso 3, se hacen las dos en un solo PR. Mientras tanto, un admin sin acceso pide ayuda al superadmin.

Como admin de empresa con cuenta ya activa, quiero pedir un link de recuperación de contraseña desde el login, para poder volver a entrar si la olvidé.

Criterios de aceptación:
- Disponible solo para cuentas en estado `ACTIVE` (no reemplaza el flujo de activación de US-03.4).
- El link usa el mismo mecanismo de token de un solo uso que la activación, con su propio vencimiento (1 hora).
- Un token ya usado o vencido muestra error y no permite resetear la contraseña desde ahí.

Tareas técnicas:
- Backend (Nico): endpoint de "olvidé mi contraseña" (generar token, endpoint de reseteo).
- Frontend (Brune): pantalla de "olvidé mi contraseña", reutilizando la pantalla "definí tu contraseña" de US-03.4.

Dependencias: US-03.4 (mismo mecanismo de token).

#### US-03.8 — Cliente recupera su contraseña olvidada

> ✂️ **Alcance MVP:** **pospuesta (opcional)**, junto con US-03.7. Mientras no se haga, un cliente que olvida su contraseña no tiene cómo recuperarla: se acepta para el MVP.

Como cliente ya registrado en una empresa, quiero pedir un link de recuperación desde el login de esa empresa, para volver a entrar si la olvidé.

Criterios de aceptación:
- Mismo mecanismo de token de un solo uso que US-03.7 (`UserToken`, tipo `PASSWORD_RESET`, vencimiento de 1 hora).
- El link lleva de vuelta al contexto de esa empresa puntual (`/{slug}`), no al login del panel de sistema.
- Un token ya usado o vencido muestra error y no permite el cambio.

Tareas técnicas:
- Backend (Nico): endpoint de "olvidé mi contraseña" scoped al cliente/empresa, reutilizando `UserToken`.
- Frontend (Brune): pantalla de "olvidé mi contraseña" y de reseteo, dentro de `/{slug}`.

Dependencias: US-03.6 (login de cliente), mismo mecanismo que US-03.7.

> ❓ **A confirmar en la documentación de negocio:** ninguno de `requerimientos-funcionales.md` ni `detalles-flujos.md` menciona esto explícitamente para el cliente — solo `casos-usos.md` (UC14) lo incluye como actor ("Admin de empresa / Cliente"). Conviene sumarla también a `requerimientos-funcionales.md` (sección Cliente) y a `detalles-flujos.md` §7.1 para que la historia no quede "huérfana" en la documentación.

#### US-03.9 — Sistema aísla los datos de cada empresa
Como sistema, necesito que ninguna consulta pueda devolver datos de una empresa distinta a la de la sesión, para que un cliente o un admin no vea información de otro negocio.

Es la regla de seguridad más importante del backend y es **transversal a todos los features** (ver `arquitectura.md` → *Aislamiento multi-tenant*, `backend/README.md` y `esquema-bd.md §4`). Va acá, y no más adelante, por una razón de secuencia: si los repositorios de los Pasos 4 y 5 se escriben sin `companyId`, para el Paso 9 hay que reescribir toda la capa de datos.

Criterios de aceptación:
- Todo método de repositorio que devuelva datos con dueño recibe el `companyId` como parámetro obligatorio: `findByIdAndCompanyId(id, companyId)`, nunca `findById(id)`.
- El `companyId` sale siempre del JWT de la sesión, nunca de un parámetro del request ni del body.
- Un `COMPANY_ADMIN` o un `CLIENT` que pide por ID un recurso de otra empresa recibe 404, no 403 (no se confirma que el recurso exista).
- La tercera barrera ya está en la base: las FK compuestas de `Appointment` impiden que un registro mezcle dos empresas aunque el código falle.

Tareas técnicas:
- Backend (Nico): resolver el `companyId` de la sesión desde el JWT y dejarlo disponible para la capa de servicio; escribir la regla de firmas de repositorio en el README del backend con un ejemplo bueno y uno malo.

Dependencias: US-03.1, US-03.2, US-03.6 (el `companyId` nace con el JWT). Los tests de integración de las tres barreras se completan en el Paso 12.

#### US-03.10 — Sistema devuelve los errores en un formato único
Como sistema, necesito un manejador centralizado de errores que traduzca las excepciones a códigos HTTP y a un cuerpo de error consistente, para que el frontend no tenga que interpretar un formato distinto por endpoint.

Criterios de aceptación:
- Un único `@RestControllerAdvice` concentra el mapeo; ningún controller arma respuestas de error a mano.
- El cuerpo de error tiene siempre la misma forma y su mensaje está en español (según `convenciones.md`).
- Mapeo base: validación de request → 400, sin sesión → 401, sin permiso → 403, recurso inexistente o de otra empresa → 404.
- El mapeo de `ConstraintViolationException` a **409** se agrega en el Paso 7, cuando aparece la constraint de exclusión (US-07.3), sin tocar la estructura definida acá.

Tareas técnicas:
- Backend (Nico): `@RestControllerAdvice` + clase de respuesta de error.
- Frontend (Brune): en `api/`, un único punto que interpreta ese formato y lo expone tipado al resto de la aplicación.

Dependencias: US-03.1 (el primer error real es el de login inválido).

---

### Paso 4 — Empresas y panel de superadmin

Listo cuando (del paso completo): el superadmin puede dar de alta una empresa nueva, ver su dashboard con datos reales, y ajustar los parámetros globales de la plataforma.

> 📨 **El primer envío del sistema ocurre en este paso**, no en el 7: US-04.1 y US-04.8 disparan el link de activación. Hasta el Paso 10 el canal va **mockeado/logueado** — el link se imprime por consola y se copia a mano — pero la interfaz `NotificationService` ya se define acá, para que el Paso 7 solo tenga que sumarle eventos y el Paso 10 solo tenga que reemplazar el canal. La fila en `Notification` recién se empieza a persistir en US-07.5.

#### US-04.1 — Superadmin da de alta una empresa nueva
Como superadmin, quiero completar un formulario con los datos de la empresa y del futuro admin, para crear la empresa y disparar la activación del admin.

Criterios de aceptación:
- Al confirmar, se crean `Company` (`ACTIVE`) y `User` admin (`PENDING_ACTIVATION`) en una misma transacción — si algo falla, no queda ninguno de los dos creado.
- El slug se genera automáticamente a partir del nombre, validando que no exista (agregando un sufijo si hace falta) y que no choque con la lista de slugs reservados.
- El formulario incluye la **zona horaria** de la empresa (`timezone`, formato IANA), con `America/Argentina/Cordoba` como valor por defecto. Es obligatoria: sin ella, el cálculo de disponibilidad del Paso 5 toma la zona del proceso Java —que en Render es UTC— y ofrece turnos corridos tres horas.
- Se dispara el link de activación al admin recién creado.
- El email del admin no puede existir como `COMPANY_ADMIN` en otra empresa ni como `SUPERADMIN` (ver *Decisiones de diseño* → email único global). Se valida en el backend, sin migración.
- La URL pública `/{slug}` ya resuelve desde ese momento, mostrando el estado "todavía sin servicios cargados".

Tareas técnicas:
- Backend (Brune): endpoint transaccional de alta, generación de slug único, disparo del link de activación al admin recién creado (token único, expira en 48hs, enviado vía notification).
- Frontend (Nico): formulario de alta con datos de empresa + datos del admin.

Dependencias: Paso 2 (`Company`), Paso 3 (`User`, activación).

#### US-04.2 — Superadmin ve el listado de empresas
Como superadmin, quiero ver un listado de todas las empresas, para acceder a cada una y gestionarla.

Criterios de aceptación:
- El listado muestra nombre, estado (activa/inactiva) y rubro de cada empresa.
- Desde cada fila se puede entrar al detalle/edición y a "ver página pública".

Tareas técnicas:
- Backend (Brune): endpoint de listado.
- Frontend (Nico): tabla/listado de empresas.

Dependencias: US-04.1.

#### US-04.3 — Superadmin edita los datos de una empresa
Como superadmin, quiero editar los datos de una empresa (nombre, descripción, dirección, teléfono, email de contacto, rubro, zona horaria), para mantenerlos actualizados.

Criterios de aceptación:
- El slug no es editable desde este formulario.
- La zona horaria sí es editable, con la misma validación que en el alta.
- Los cambios se reflejan en la página pública de esa empresa.

Tareas técnicas:
- Backend (Brune): endpoint de edición, excluyendo el slug.
- Frontend (Nico): formulario de edición.

Dependencias: US-04.1.

> 📌 **El logo lo pueden editar los dos, a propósito.** `detalles-flujos.md` §7.2 se lo da al superadmin (que lo carga en el alta y puede corregirlo desde acá) y `requerimientos-funcionales.md` se lo da al admin como parte de su personalización (US-05.7). No es una contradicción a resolver: es el mismo campo con dos puertas de entrada, igual que en cualquier panel con soporte. Ambos endpoints escriben `logoUrl` y el último que guarda gana.

#### US-04.4 — Superadmin desactiva una empresa
Como superadmin, quiero desactivar una empresa, para dar de baja el negocio sin perder su historial.

Criterios de aceptación:
- La empresa pasa a estado `INACTIVE` — el registro nunca se borra.
- El login del admin de esa empresa queda bloqueado (derivado de `company.status` en el filtro de autenticación, sin tocar `User.status`).
- Los turnos pendientes de esa empresa se cancelan automáticamente, notificando a cada cliente afectado.
- La página pública de esa empresa deja de ofrecer reservas y muestra un estado de "no disponible".

Tareas técnicas:
- Backend (Brune): endpoint de desactivación con la cascada (cancelación de turnos pendientes + notificación).
- Frontend (Nico): botón de desactivar con confirmación.

Dependencias: US-04.1. La cascada de cancelación se puede escribir con esta historia, pero probarla con turnos reales necesita el Paso 7 — hay un pulido de este punto previsto en el Paso 11.

#### US-04.5 — Superadmin reactiva una empresa
Como superadmin, quiero reactivar una empresa previamente desactivada, para que vuelva a operar con la configuración que ya tenía.

Criterios de aceptación:
- La empresa vuelve a estado `ACTIVE`.
- El login del admin se desbloquea, sin pedir un nuevo link de activación si ya tenía contraseña definida.
- La página pública vuelve a aceptar reservas con los servicios/horarios ya cargados.
- Los turnos cancelados al desactivar no se recuperan.

Tareas técnicas:
- Backend (Brune): endpoint de reactivación.
- Frontend (Nico): botón de reactivar.

Dependencias: US-04.4.

#### US-04.6 — Superadmin ve el dashboard con datos reales

> ✂️ **Alcance MVP:** **reducida** a dos indicadores. Sin altas por mes, sin ranking por empresa, sin distribución por rubro y sin gráficos.

Como superadmin, quiero ver la cantidad de empresas activas/inactivas y la cantidad total de turnos, para tener una visión general de la plataforma.

Criterios de aceptación:
- Empresas activas e inactivas, contadas por `company.status`.
- Cantidad total de turnos: un `COUNT(*)` sobre todos los turnos generados, sin importar el estado.
- El dashboard no expone datos de clientes ni de ingresos de cada negocio.

Tareas técnicas:
- Backend (Brune): endpoint de estadísticas agregadas.
- Frontend (Nico): pantalla de dashboard con los dos indicadores.

Dependencias: US-04.1 para tener empresas que mostrar; el total de turnos necesita el Paso 7 para tener datos reales (el endpoint puede escribirse antes, devolviendo 0).

#### US-04.7 — Superadmin ve la página pública de una empresa desde el CRUD
Como superadmin, quiero un botón "ver página pública" desde el listado/detalle de una empresa, para revisarla sin buscar la URL a mano.

Criterios de aceptación:
- El botón abre `/{slug}` tal como la vería cualquier visitante sin loguear.
- El superadmin no puede reservar con esa sesión.

Tareas técnicas:
- Frontend (Nico): botón/link a la página pública.

Dependencias: Paso 6 (la página pública tiene que existir para que el botón lleve a algo real; el botón en sí puede construirse antes).

#### US-04.8 — Superadmin reenvía el link de activación vencido
Como superadmin, quiero reenviar el link de activación a un admin que no lo usó a tiempo, para que pueda activar su cuenta igual.

Criterios de aceptación:
- Disponible mientras el admin siga en estado `PENDING_ACTIVATION`.
- Genera un nuevo token (invalidando el anterior) y dispara un nuevo envío por el mismo canal.
- Un admin ya `ACTIVE` no tiene esta opción disponible.

Tareas técnicas:
- Backend (Brune): endpoint de reenvío (nuevo token, nuevo envío vía notification).
- Frontend (Nico): botón de reenvío desde el detalle de la empresa, visible solo si el admin sigue `PENDING_ACTIVATION`.

Dependencias: US-04.1 (mismo mecanismo de token/envío).

#### US-04.9 — Superadmin configura los parámetros globales de la plataforma
Como superadmin, quiero editar la anticipación mínima para reservar, el plazo máximo de cancelación/reprogramación y el máximo de reprogramaciones por turno, para ajustar esas reglas sin necesitar un redeploy.

Criterios de aceptación:
- Los tres valores se leen y editan desde `PlatformSettings` (fila única), nunca desde un archivo de configuración.
- Al guardar, los nuevos valores rigen de inmediato para todas las empresas.
- No se recalculan ni afectan retroactivamente turnos ya reservados — solo aplican a las validaciones de operaciones posteriores.
- Se agrega un cuarto parámetro editable: **máximo de anticipación para reservar** (`maxBookingAdvanceDays`, en días).

> ❓ **Valor pendiente de definir entre Nico y Brune.** La historia se puede implementar con un valor provisorio y ajustarlo después sin tocar código, ya que vive en `PlatformSettings`.

Tareas técnicas:
- Backend (Brune): endpoint de lectura/edición de la fila única de `PlatformSettings`.
- Frontend (Nico): pantalla con los tres campos, dentro del panel de superadmin.

Dependencias: Paso 2 (`PlatformSettings`, con su fila ya insertada por la migración inicial).

---

### Paso 5 — Servicios y horarios de cada empresa

Roles invertidos respecto al Paso 4: acá backend lo lidera Nico, frontend lo hace Brune.

Listo cuando (del paso completo): un admin carga sus servicios, su horario semanal, sus excepciones de calendario y la marca de su página pública, y el sistema calcula disponibilidad real para un servicio puntual.

#### US-05.1 — Admin crea y edita servicios
Como admin de empresa, quiero crear y editar servicios (nombre, descripción, precio, duración), para ofrecerlos en mi página pública.

Criterios de aceptación:
- Un servicio recién creado queda en estado `ACTIVE`.
- La duración en minutos es obligatoria — es la que usa el cálculo de disponibilidad.

Tareas técnicas:
- Backend (Nico): CRUD de `Service`.
- Frontend (Brune): formulario de alta/edición de servicios.

Dependencias: Paso 2 (`Service`), Paso 3 (sesión de admin).

#### US-05.2 — Admin desactiva un servicio con turnos futuros
Como admin de empresa, quiero desactivar un servicio, para dejar de ofrecerlo sin afectar los turnos ya reservados.

Criterios de aceptación:
- Antes de confirmar, se muestra cuántos turnos futuros tiene ese servicio.
- Al desactivarlo, el servicio deja de ofrecerse para nuevas reservas.
- Los turnos ya reservados con ese servicio no se cancelan ni se modifican automáticamente.

Tareas técnicas:
- Backend (Nico): endpoint de desactivación + conteo de turnos futuros asociados.
- Frontend (Brune): confirmación mostrando el conteo antes de desactivar.

Dependencias: US-05.1. El conteo real necesita el Paso 7 — antes puede escribirse mostrando 0.

#### US-05.3 — Admin configura su horario semanal
Como admin de empresa, quiero definir un horario habitual por día de la semana (rango horario + intervalo), para que el sistema calcule disponibilidad sobre esa base.

Criterios de aceptación:
- La configuración es una plantilla recurrente ("todos los lunes de 9 a 18"), no se carga semana a semana.
- Un día sin configuración se interpreta como cerrado ese día.
- Se admiten varias franjas por día (jornada partida), siempre que no se superpongan entre sí.

Tareas técnicas:
- Backend (Nico): CRUD de `BusinessHours` (crear/editar/eliminar filas por día).
- Frontend (Brune): tabla de configuración por día de la semana.

Dependencias: Paso 2 (`BusinessHours`), Paso 3.

#### US-05.4 — Admin edita el horario con turnos ya reservados fuera del nuevo rango
Como admin de empresa, quiero que el sistema me avise si hay turnos reservados fuera del nuevo rango horario que estoy por guardar, para decidir si los dejo o los cancelo a mano.

Criterios de aceptación:
- Antes de guardar el cambio, se informa cuántos turnos quedan fuera del nuevo rango.
- Al guardar, esos turnos no se cancelan ni se modifican automáticamente.

Tareas técnicas:
- Backend (Nico): al editar `BusinessHours`, calcular turnos futuros fuera del nuevo rango.
- Frontend (Brune): mostrar el aviso antes de confirmar el guardado.

Dependencias: US-05.3. El conteo real necesita el Paso 7 para probarse con datos reales.

#### US-05.5 — Sistema calcula la disponibilidad real para un servicio
Como sistema, necesito calcular los horarios de inicio disponibles para un servicio puntual, combinando el horario semanal, la duración del servicio y los turnos ya ocupados, para que el cliente solo vea horarios que realmente puede reservar.

Criterios de aceptación:
- Genera los horarios de inicio candidatos según el intervalo configurado (ej. 09:00, 09:30, 10:00...).
- **Convierte cada candidato a instante UTC usando `company.timezone`, nunca la zona horaria del proceso.** Es el paso 3 del algoritmo de `detalles-flujos.md` §7.3 y la razón de ser de la columna: el mismo código desplegado en Render (que corre en UTC) ofrecería horarios corridos tres horas.
- Descarta los candidatos cuyo rango (inicio + duración del servicio) se superpone con un turno ya reservado.
- Un servicio corto puede tener disponible un horario que un servicio largo no puede ocupar, aunque compartan la misma configuración de horarios.
- Descarta los candidatos que no cumplen la **anticipación mínima** y los que ya pasaron, tomando el valor de `PlatformSettings` (paso 6 del algoritmo). Es la misma validación que US-07.4 aplica al guardar: acá evita ofrecer un horario que después el backend va a rechazar.
- No persiste una tabla de "horarios disponibles" — se calcula al vuelo en cada consulta.
- No se generan candidatos más allá de `hoy + maxBookingAdvanceDays`: es una ventana móvil, se recalcula sola día a día sin necesidad de ningún job.

Con esto quedan cubiertos los 7 pasos del algoritmo de `detalles-flujos.md` §7.3. El paso 1 (precedencia de `ScheduleException` sobre `BusinessHours`) se completa en US-05.6, que extiende este mismo service.

Tareas técnicas:
- Backend (Nico): service de cálculo de disponibilidad (recibe `companyId` + `serviceId` + fecha, devuelve horarios de inicio libres).
- Backend (Nico): test unitario del cálculo con una `company.timezone` distinta de la del proceso, forzando la zona del test a UTC.

> 📦 **Dónde vive:** `AvailabilityService` va en el paquete **`appointment`**, no en `schedule`. Necesita las dos cosas —la plantilla de atención y los turnos ocupados— y ponerlo en `schedule` genera el ciclo `schedule ↔ appointment`. `schedule` queda como módulo puro: dada una fecha responde qué franjas hay, sin saber que existen los turnos. Está resuelto en `backend/README.md` → *Regla de dependencias entre features*.

Dependencias: US-05.1, US-05.3, US-04.9 (la anticipación mínima sale de `PlatformSettings`).

#### US-05.6 — Admin carga una excepción de calendario
Como admin de empresa, quiero cargar una fecha concreta como excepción a mi horario habitual (cerrado todo el día, o un rango especial), para que el sistema no ofrezca turnos fuera de esa configuración.

Criterios de aceptación:
- Una excepción se carga para una fecha concreta (`exceptionDate`), no recurrente, y no puede repetirse la fecha en la misma empresa.
- Si `isClosed = true`, no se piden horarios; si `isClosed = false`, se piden `startTime`/`endTime`/`intervalMinutes`, igual que `BusinessHours`.
- La excepción reemplaza por completo la configuración semanal de esa fecha (no se combinan).
- La fecha de la excepción (`exceptionDate`) debe ser igual o posterior a la fecha actual, calculada en `company.timezone`. Un intento de cargar una fecha pasada se rechaza con un mensaje claro.
- Antes de guardar, si hay turnos futuros reservados en esa fecha que quedan fuera de la excepción, se informa cuántos son — no se cancelan automáticamente.

Tareas técnicas:
- Backend (Brune): CRUD de `ScheduleException` + cálculo de turnos futuros afectados en esa fecha. *(Reasignada de Nico; el conteo de turnos afectados se coordina con Nico, que lleva `appointment`.)*
- Frontend (Brune): pantalla de excepciones (listado + alta/edición), reutilizando el patrón de aviso de US-05.4.

Dependencias: Paso 2 (`ScheduleException`), Paso 3. El conteo real necesita el Paso 7 — antes puede mostrar 0, igual que US-05.2/US-05.4.

#### US-05.7 — Admin personaliza la marca de su página pública
Como admin de empresa, quiero cargar el logo y el color primario de mi negocio y poder ver cómo queda mi página pública, para que refleje mi marca sin depender del superadmin.

Es el requerimiento *"personalización básica de su página pública (logo, color)"* y el ítem de navegación *"Mi página pública"* de `detalles-flujos.md` §7.3. Va en este paso porque el Paso 6 depende de que estos dos campos estén cargados.

Criterios de aceptación:
- El admin sube un logo y elige un color primario; el color se valida como HEX de 6 dígitos (`^#[0-9A-Fa-f]{6}$`, igual que el `CHECK` de la base).
- Ambos campos son opcionales: sin logo ni color, la página pública usa el estilo por defecto y no se rompe.
- "Mi página pública" es solo un link que abre `/{slug}` tal como la ve un visitante — no es un editor campo por campo: el resto del contenido se arma solo con Servicios, Horarios y los datos de la empresa.
- El logo también lo puede tocar el superadmin desde US-04.3 (lo carga en el alta y puede corregirlo). Son dos puertas al mismo campo, decidido así a propósito; el color primario, en cambio, solo se edita desde acá.

Tareas técnicas:
- Backend (Nico): endpoint de actualización de `logoUrl` y `primaryColor`, scoped por el `companyId` de la sesión.
- Frontend (Brune): pantalla de personalización con vista previa del color, y el link a "Mi página pública" en la navegación del panel.

Dependencias: Paso 3 (sesión de admin), US-04.1 (la empresa existe).

> ⏳ **Bloqueante a resolver antes de empezar esta historia:** `backend/README.md` lista el *storage* externo para los logos como pendiente de definir. Mientras no esté resuelto, el color se puede implementar completo y el logo queda como un campo de URL cargada a mano.

---

### Paso 6 — Página pública de cada empresa

Roles invertidos respecto al paso anterior: acá backend lo lidera Brune, frontend lo hace Nico.

Listo cuando (del paso completo): entrando a `plataforma.com/{slug}` cualquiera, sin loguearse, puede ver los servicios y elegir un horario disponible.

#### US-06.1 — Visitante ve la página pública de una empresa
Como visitante sin login, quiero entrar a `plataforma.com/{slug}` y ver los datos de la empresa (logo, color, servicios), para conocer el negocio antes de reservar.

Criterios de aceptación:
- La página resuelve sin necesidad de sesión.
- Muestra logo y color primario de la empresa.
- Muestra el listado de servicios activos con nombre, descripción, precio y duración.
- Si la empresa está `INACTIVE`, en vez de servicios muestra el estado "no disponible" (ver US-04.4).

Tareas técnicas:
- Backend (Brune): endpoint público (sin login) que resuelve datos de empresa + servicios activos a partir del slug.
- Frontend (Nico): página pública con el layout base, aplicando logo/color dinámicamente. Todas las empresas comparten el mismo componente — solo cambia lo que trae el slug.

Dependencias: Paso 4 (`Company` dada de alta), US-05.7 (logo y color cargados por el admin), Paso 5 (`Service`).

#### US-06.2 — Visitante elige un servicio y ve los horarios disponibles
Como visitante en la página pública, quiero elegir un servicio y ver los horarios disponibles para ese servicio, para poder reservar un turno.

Criterios de aceptación:
- Al elegir un servicio, se consulta la disponibilidad real (US-05.5) para ese servicio y esa empresa.
- Solo se muestran horarios de inicio efectivamente libres.
- Si no hay horarios disponibles en el rango consultado, se indica claramente (no una lista vacía sin explicación).

Tareas técnicas:
- Backend (Brune): expone el cálculo de disponibilidad del Paso 5 vía endpoint público, sin requerir sesión.
- Frontend (Nico): selector de servicio + selector de horario disponible.

Dependencias: US-06.1, US-05.5.

---

### Paso 7 — Reservar un turno (el corazón del sistema)

Vuelve el esquema del Paso 3: backend lo lidera Nico, frontend lo hace Brune.

Listo cuando (del paso completo): un cliente puede reservar un turno de punta a punta y le llega la notificación.

#### US-07.1 — Cliente inicia sesión antes de confirmar una reserva
Como cliente sin sesión iniciada en la empresa cuyo turno quiero reservar, quiero que se me pida login o registro al intentar confirmar, para poder asociar el turno a mi cuenta.

Criterios de aceptación:
- Si el cliente no tiene sesión activa para esa empresa (`{slug}`), al intentar reservar se lo redirige a login/registro antes del modal de confirmación.
- Si ya tiene sesión activa para esa empresa, el flujo continúa directo al modal sin pedir login.
- Tras loguearse o registrarse, vuelve al mismo punto del flujo de reserva (mismo servicio y horario elegidos), sin repetir la selección.

Tareas técnicas:
- Frontend (Brune): interceptar el paso "confirmar reserva" y chequear sesión; conservar servicio/horario elegidos en estado mientras el cliente pasa por login/registro.

Dependencias: US-03.6 (login de cliente), US-06.2.

#### US-07.2 — Cliente confirma la reserva en un modal
Como cliente que ya eligió servicio y horario, quiero ver un modal de confirmación antes de que el turno se guarde, para revisar los datos antes de confirmar.

Criterios de aceptación:
- El modal muestra: nombre del servicio, fecha y hora elegida (en hora local), nombre de la empresa.
- El turno no se guarda hasta que el cliente confirma explícitamente en el modal.
- Si el cliente cancela el modal, vuelve a la selección de horario sin haber reservado nada.

Tareas técnicas:
- Frontend (Brune): componente modal + llamada a `createAppointment()` en `api/` solo al confirmar.

Dependencias: US-07.1.

#### US-07.3 — Sistema evita doble reserva del mismo horario
Como sistema, necesito garantizar que dos clientes no puedan reservar el mismo horario si confirman casi al mismo tiempo, para no generar turnos superpuestos.

Criterios de aceptación:
- Ante dos requests simultáneos por el mismo horario, solo uno se guarda como `PENDING`; el otro recibe un error indicando que el horario ya no está disponible.
- La validación de superposición y el guardado ocurren dentro de la misma transacción con lock pesimista (`SELECT ... FOR UPDATE` sobre `company`).
- **El `INSERT` copia el precio y la duración del servicio en el propio turno** (`priceSnapshot`, `durationMinutesSnapshot`), y escribe `endDateTime` a partir de esa duración. Las tres columnas son `NOT NULL`: sin la copia, editar la duración de un servicio alargaría retroactivamente todos los turnos ya reservados y los haría superponerse entre sí ([regla 24](negocio/reglas-negocio.md)).
- La `ConstraintViolationException` de la constraint de exclusión se mapea a **HTTP 409** en el manejador centralizado de US-03.10 — es la segunda barrera, y actúa aunque el lock falle.
- El cliente que pierde la carrera ve un mensaje claro y puede elegir otro horario sin recargar toda la página.

Tareas técnicas:
- Backend (Nico): `@Lock(PESSIMISTIC_WRITE)` sobre `company` antes de guardar.
- Backend (Nico): mapeo de `ConstraintViolationException` a 409 en el `@RestControllerAdvice` ya existente.
- Backend (Nico): test de integración con Testcontainers simulando dos reservas concurrentes sobre el mismo horario.
- Frontend (Brune): manejar el error de "horario ya no disponible" en el modal, sin romper el flujo.

Dependencias: US-07.2.

#### US-07.4 — Sistema valida la anticipación mínima
Como sistema, necesito rechazar reservas con menos de 30 minutos de anticipación (o en horarios ya pasados), para evitar turnos que el negocio no llega a ver a tiempo.

Criterios de aceptación:
- Un intento de reserva con menos de 30 minutos de anticipación sobre el horario elegido se rechaza con un mensaje claro.
- Un intento de reserva sobre un horario ya pasado se rechaza igual.
- Un intento de reserva con más de `maxBookingAdvanceDays` de anticipación (`PlatformSettings`) se rechaza con un mensaje claro. *(Valor a definir — ver US-04.9.)*
- El valor de 30 minutos sale de `PlatformSettings`, no está hardcodeado.

Tareas técnicas:
- Backend (Nico): validación en el service antes del guardado, con test unitario.

Dependencias: ninguna directa — se puede desarrollar en paralelo con US-07.3.

#### US-07.5 — Cliente recibe notificación al confirmarse la reserva
Como cliente, quiero recibir una notificación por WhatsApp cuando mi turno queda confirmado, para tener un comprobante fuera del sistema.

Criterios de aceptación:
- Al guardarse el turno como `PENDING`, se dispara una notificación al `phone` del cliente.
- El mensaje es informativo, no requiere que el cliente responda ni confirme nada.
- **Cada notificación se persiste como una fila en `Notification`** con su `eventType`, `channel`, `recipientPhone`, estado (`PENDING` → `SENT`/`FAILED`), `attemptCount` y `errorMessage`. La tabla ya existe desde el Paso 2 y hasta acá nadie escribía en ella.
- `recipientPhone` se copia en la fila, no se lee por FK: el registro histórico tiene que reflejar a dónde se envió realmente, aunque el cliente cambie su teléfono después.
- Si el envío falla, el turno igual queda guardado y la notificación queda en `FAILED` para que el reintento del Paso 10 la levante.

Tareas técnicas:
- Backend (Nico): `NotificationService` que persiste la fila y despacha por el canal, invocado con `@TransactionalEventListener(AFTER_COMMIT)` desde `appointment` — así `notification` queda como hoja del grafo de dependencias (ver `backend/README.md`).

Dependencias: el canal real de WhatsApp se integra recién en el Paso 10 — hasta entonces el despacho va mockeado/logueado, **pero la fila en `Notification` se escribe igual desde acá**.

#### US-07.6 — Cliente ve su listado de "Mis turnos"
Como cliente logueado en una empresa, quiero una pantalla con todos mis turnos de esa empresa, filtrable por estado, para tener en un solo lugar lo que reservé.

Es la pantalla base sobre la que se apoyan US-07.7, todo el Paso 8 y el E2E del Paso 12. Hasta esta revisión ninguna historia la construía: se la daba por existente.

Criterios de aceptación:
- Lista únicamente los turnos del cliente de la sesión, en la empresa de la sesión — dos barreras: el `clientId` del JWT y el `companyId` (US-03.9).
- Muestra fecha/hora (convertida a hora local desde UTC), servicio, empresa y estado.
- Permite filtrar por estado (`PENDING`, `COMPLETED`, `CANCELLED`) y distinguir los turnos que fueron reprogramados por `rescheduleCount > 0`.
- No hay una sección de historial aparte: los `COMPLETED` cumplen esa función ([regla 34](negocio/reglas-negocio.md)).

Tareas técnicas:
- Backend (Nico): endpoint de listado de turnos del cliente, scoped por `clientId` + `companyId`, con filtro de estado.
- Frontend (Brune): pantalla "Mis turnos" dentro de `/{slug}` con la tabla y los filtros.

Dependencias: US-03.6 (sesión de cliente), Paso 2 (`Appointment`). Se puede construir en paralelo con US-07.1–US-07.5 usando turnos seedeados.

#### US-07.7 — Cliente ve el turno reservado en "Mis turnos"
Como cliente, quiero ver el turno recién reservado en mi listado de "Mis turnos", para confirmar que quedó guardado.

Criterios de aceptación:
- Inmediatamente después de confirmar, el turno aparece en "Mis turnos" con estado `PENDING`.
- Se muestran fecha/hora en hora local (convertida desde UTC en el frontend), servicio y empresa.

Tareas técnicas:
- Frontend (Brune): invalidar/refrescar la query de turnos (TanStack Query) tras un `createAppointment()` exitoso.

Dependencias: US-07.6 (la pantalla y el endpoint), US-07.2. Ninguna del Paso 8 — esta historia solo lee el turno recién creado.

---

### Paso 8 — Mis turnos: cancelar y reprogramar

Roles invertidos: backend lo lidera Brune, frontend lo hace Nico.

Listo cuando (del paso completo): un cliente puede cancelar o reprogramar su propio turno dentro del plazo permitido, y no puede hacerlo fuera de plazo.

#### US-08.1 — Cliente cancela su propio turno dentro del plazo
Como cliente, quiero cancelar mi turno hasta 3 horas antes, para liberar el horario si ya no lo necesito.

Criterios de aceptación:
- Disponible hasta el plazo configurado antes del turno (valor ajustable en `PlatformSettings`).
- El turno pasa a estado `CANCELLED` y se muestra en gris en "Mis turnos".
- Un turno cancelado no puede reactivarse desde "Mis turnos".
- Fuera del plazo, la acción de cancelar queda deshabilitada.

Tareas técnicas:
- Backend (Brune): endpoint de cancelación validando el plazo contra `PlatformSettings.cancellationDeadlineHours` (inicialmente 3 horas, pero **el valor no se hardcodea**), cambio de estado.
- Frontend (Nico): botón de cancelar en "Mis turnos", habilitado/deshabilitado según el plazo.

Dependencias: Paso 7 (turno reservado existente).

#### US-08.2 — Cliente reprograma su propio turno dentro del plazo
Como cliente, quiero reprogramar mi turno a un nuevo horario disponible dentro del plazo permitido, para cambiar la fecha sin perder mi reserva.

Criterios de aceptación:
- Disponible hasta 3 horas antes del turno original (mismo plazo que cancelar).
- Se actualiza el mismo registro de turno (no se crea uno nuevo), guardando la fecha anterior (`previousStartDateTime`) e incrementando `rescheduleCount`. **El turno sigue en estado `PENDING`** — reprogramar no es un estado, es un evento contabilizado (ver nota de diseño en `diccionario-datos.md`).
- El nuevo horario elegido pasa por el mismo cálculo de disponibilidad y las mismas validaciones que una reserva nueva (anticipación mínima, no superposición).
- El tope de reprogramaciones sale de `PlatformSettings.maxRescheduleCount` (inicialmente 2), no de una constante en el código. Alcanzado el tope, el endpoint rechaza y el botón queda deshabilitado.

Tareas técnicas:
- Backend (Brune): endpoint de reprogramación (valida plazo, valida disponibilidad del nuevo horario, actualiza el registro y suma `rescheduleCount`).
- Frontend (Nico): flujo de reprogramación desde "Mis turnos", reutilizando el selector de horario disponible.

Dependencias: US-08.1 (mismo plazo), US-05.5 (cálculo de disponibilidad), US-07.3 y US-07.4 (las mismas validaciones de concurrencia y anticipación aplican al nuevo horario).

#### US-08.3 — Cliente ve en "Mis turnos" qué acciones puede hacer sobre cada turno
Como cliente, quiero ver en "Mis turnos" si puedo cancelar o reprogramar cada turno, para saber qué opciones tengo sin tener que intentarlo.

Criterios de aceptación:
- Cada turno muestra sus botones de cancelar/reprogramar habilitados o deshabilitados según falte más o menos de 3 horas para el horario.
- Los turnos `CANCELLED` o `COMPLETED` no muestran acciones sobre ese registro.
- Un turno reprogramado se distingue visualmente por `rescheduleCount > 0`, según el modelo de `diccionario-datos.md`.

Tareas técnicas:
- Frontend (Nico): lógica de habilitado/deshabilitado en el listado, basada en `startDateTime - now()`, y badge de "reprogramado" según `rescheduleCount`.

Dependencias: US-08.1, US-08.2.

---

### Paso 9 — Turnero del admin (reserva y cancelación manual)

Roles invertidos respecto al Paso 7: backend lo lidera Brune, frontend lo hace Nico.

Listo cuando (del paso completo): el admin puede ver su dashboard, su agenda completa, el listado de clientes de su empresa, cancelar cualquier turno, y cargar un turno manual (con cliente existente o cliente sin cuenta).

#### US-09.1 — Admin ve turnos de hoy y próximos turnos en su dashboard
Como admin de empresa, quiero ver en mi dashboard los turnos de hoy y los próximos turnos, para tener una visión rápida de mi agenda sin entrar al turnero completo.

Criterios de aceptación:
- Muestra los turnos de la empresa con `startDateTime` de hoy.
- Muestra un listado breve de los próximos turnos.
- No incluye turnos `CANCELLED`.

Tareas técnicas:
- Backend (Brune): endpoint de resumen (turnos de hoy + próximos) scoped por `companyId`.
- Frontend (Nico): bloque de dashboard con ambos listados.

Dependencias: Paso 7 (turnos existentes), Paso 3 (sesión de admin).

> ❓ A definir: el criterio de corte para "próximos turnos" (¿los próximos N turnos? ¿los de los próximos X días?) no está definido en la documentación de negocio — puede construirse con un criterio provisorio (ej. próximos 5) y ajustarlo después.

#### US-09.2 — Admin ve su agenda/turnero filtrable por estado y origen
Como admin de empresa, quiero ver el turnero completo de mi negocio, filtrable por estado y por origen, para tener visibilidad de todos los turnos.

Criterios de aceptación:
- Muestra todos los turnos de la empresa (reservados online y cargados manualmente).
- Permite filtrar por estado (`PENDING`, `COMPLETED`, `CANCELLED`), por **origen** (`ONLINE` / `MANUAL`) y, aparte, por si fue reprogramado (`rescheduleCount > 0`).
- El filtro por origen es la razón de ser de la columna: sin él, un turno manual vinculado a una cuenta existente es indistinguible de uno que reservó el propio cliente ([regla 22](negocio/reglas-negocio.md)).

Tareas técnicas:
- Backend (Brune): endpoint de listado de turnos por empresa, con filtros de estado y de origen.
- Frontend (Nico): vista de turnero/agenda con los filtros.

Dependencias: Paso 7 (turnos existentes), Paso 3 (sesión de admin).

#### US-09.3 — Admin cancela cualquier turno de su empresa sin restricción horaria
Como admin de empresa, quiero cancelar cualquier turno (reservado online o cargado manualmente), sin restricción horaria, dejando registrado el motivo, para poder gestionar excepciones que el cliente ya no puede resolver por su cuenta.

Criterios de aceptación:
- No aplica el límite de 3hs que tiene el cliente — el admin puede cancelar en cualquier momento.
- El turno pasa a `CANCELLED`, con `cancelledBy=COMPANY` y un motivo en texto libre opcional.
- El cliente recibe notificación del cambio, si tiene forma de recibirla.
- Al cancelar, el horario queda liberado para otra persona.

Tareas técnicas:
- Backend (Brune): endpoint de cancelación por admin (sin validación de plazo), registrando `cancelledBy` y `cancellationReason`.
- Frontend (Nico): acción de cancelar desde el turnero, con campo de motivo opcional.

Dependencias: US-09.2.

#### US-09.4 — Admin busca un cliente existente para cargar un turno manual
Como admin de empresa, quiero buscar un cliente por nombre, teléfono o email, para vincular un turno manual a su cuenta.

Criterios de aceptación:
- La búsqueda filtra por nombre, teléfono o email, dentro de los clientes (`CLIENT`) de esa empresa.
- El resultado se muestra en un desplegable/lista para seleccionar.

Tareas técnicas:
- Backend (Brune): endpoint de búsqueda de clientes, scoped por `companyId`.
- Frontend (Nico): componente de búsqueda/autocomplete de clientes.

Dependencias: Paso 3 (clientes ya registrados en esa empresa).

#### US-09.5 — Admin carga un turno manual para un cliente con cuenta
Como admin de empresa, quiero cargar un turno manual vinculado a un cliente existente, para registrar una reserva que llegó por fuera del sistema (WhatsApp, teléfono).

Criterios de aceptación:
- El turno queda vinculado al `clientId` del cliente seleccionado y con `origin = MANUAL`.
- Pasa por el mismo cálculo de disponibilidad y las mismas validaciones que una reserva online (anticipación mínima, no superposición, lock pesimista, copia de precio y duración).
- Se dispara la misma notificación de confirmación que en una reserva online.
- El cliente puede después autogestionar ese turno desde "Mis turnos" como cualquier otro.

Tareas técnicas:
- Backend (Brune): endpoint de carga manual con `clientId`, reutilizando las validaciones de reserva (US-07.3, US-07.4).
- Frontend (Nico): formulario de carga manual, con el buscador de US-09.4 integrado.

Dependencias: US-09.4, US-07.3, US-07.4, US-05.5.

#### US-09.6 — Admin carga un turno manual para un cliente sin cuenta
Como admin de empresa, quiero cargar un turno manual con los datos sueltos de un cliente sin cuenta (nombre, teléfono), para registrar una reserva de alguien que no está en la plataforma.

Criterios de aceptación:
- El turno queda con `clientId` nulo, `manualClientName` y `manualClientPhone` cargados, y `origin = MANUAL`.
- El `CHECK` de la base exige exactamente una de las dos identidades: o `clientId`, o nombre + teléfono. Nunca ninguna, nunca las dos ([regla 21](negocio/reglas-negocio.md)).
- Pasa por las mismas validaciones de disponibilidad, anticipación mínima y superposición.
- Se dispara la notificación de confirmación al teléfono cargado.
- Ese turno no es autogestionable por el cliente — si necesita cancelarlo, debe contactar al negocio y es el admin quien lo cancela desde el turnero (US-09.3). El mensaje de confirmación no lleva ningún link de autogestión (decisión explícita para el MVP, según `detalles-flujos.md`).

Tareas técnicas:
- Backend (Brune): endpoint de carga manual sin `clientId`, con `manualClientName`/`manualClientPhone`.
- Frontend (Nico): mismo formulario de carga manual, rama "sin cuenta" (datos sueltos en vez de buscador).

Dependencias: US-09.5 (comparten formulario y validaciones), Paso 10 para el canal real de notificación.

#### US-09.7 — Admin ve el listado de clientes de su empresa
Como admin de empresa, quiero una pantalla con los clientes registrados en mi negocio, buscable por nombre, teléfono o email, para consultarlos sin tener que abrir el formulario de turno manual.

Es el ítem "Clientes" de la navegación del panel (`detalles-flujos.md` §7.3) y el requerimiento *"ver el listado de clientes de su empresa, con búsqueda"*. US-09.4 resolvió el buscador embebido en el formulario; esto es la pantalla en sí.

Criterios de aceptación:
- Es una vista de **solo lectura**: las cuentas las crea el propio cliente al registrarse, y el admin no las edita ni las da de baja.
- Lista únicamente usuarios con `role = CLIENT` de la empresa de la sesión — un admin nunca ve clientes de otra empresa (US-03.9).
- Reutiliza la misma búsqueda de US-09.4 (nombre, teléfono o email), con paginado.
- No expone el hash de contraseña ni ningún dato de sesión.

Tareas técnicas:
- Backend (Brune): extender el endpoint de US-09.4 con paginado y listado sin filtro, devolviendo un DTO acotado.
- Frontend (Nico): pantalla de Clientes con la tabla y el buscador, más el ítem en la navegación del panel.

Dependencias: US-09.4 (comparten endpoint), Paso 3 (clientes registrados).

---

### Paso 10 — Notificaciones (WhatsApp)

Este paso queda con **Nico**, que ya integró la API de WhatsApp Business en un proyecto anterior, salvo US-10.4 (reintento), que pasa a Brune para emparejar la carga de back. No tiene pantalla de frontend propia: mientras Nico integra, Brune aprovecha en paralelo para pulir UI/tests pendientes de pasos anteriores.

Listo cuando (del paso completo): los tres eventos (reserva, cancelación, reprogramación) disparan un mensaje real de WhatsApp al cliente, y un envío que falla se reintenta solo sin duplicar el mensaje.

#### US-10.1 — Sistema envía notificación real por WhatsApp al confirmarse una reserva
Como sistema, necesito enviar un mensaje real de WhatsApp al cliente cuando su turno queda confirmado (online o manual), para reemplazar el envío mockeado/logueado usado hasta ahora.

Criterios de aceptación:
- El mensaje llega al número de teléfono (`phone`) del cliente o del turno manual.
- El proveedor de WhatsApp Business queda configurado.
- Si el envío falla, el turno ya guardado no se ve afectado — no revierte la reserva.

Tareas técnicas:
- Backend (Nico): integración real con la API de WhatsApp Business en el `NotificationService`, reemplazando el mock.

Dependencias: US-07.5, US-09.5, US-09.6 (ya disparan la notificación; acá solo falta que el canal sea real).

#### US-10.2 — Sistema envía notificación real por WhatsApp al cancelar un turno
Como sistema, necesito enviar un mensaje real de WhatsApp al cliente cuando su turno se cancela (por él mismo o por el admin), para avisarle del cambio.

Criterios de aceptación:
- Se dispara tanto si cancela el cliente (US-08.1) como si cancela el admin (US-09.3).
- El mensaje llega al `phone` del cliente, si tiene forma de recibirlo.

Tareas técnicas:
- Backend (Nico): reutiliza la integración de US-10.1, disparada desde los eventos de cancelación.

Dependencias: US-10.1, US-08.1, US-09.3.

#### US-10.3 — Sistema envía notificación real por WhatsApp al reprogramar un turno
Como sistema, necesito enviar un mensaje real de WhatsApp al cliente cuando su turno se reprograma, para avisarle de la nueva fecha.

Criterios de aceptación:
- Se dispara al confirmarse la reprogramación (US-08.2).
- El mensaje incluye la nueva fecha/hora.

Tareas técnicas:
- Backend (Nico): reutiliza la integración de US-10.1, disparada desde el evento de reprogramación.

Dependencias: US-10.1, US-08.2.

#### US-10.4 — Sistema reintenta los envíos fallidos sin duplicar mensajes
Como sistema, necesito reprocesar las notificaciones que quedaron en `PENDING` o `FAILED`, para que un timeout del proveedor no deje a un cliente sin aviso ni le mande el mensaje dos veces.

Con el canal mockeado esto no se podía probar —un mock nunca falla—, así que la historia entra acá, junto con la integración real.

Criterios de aceptación:
- Un job programado levanta las `Notification` en `PENDING`/`FAILED`, reintenta el envío e incrementa `attemptCount`.
- Una notificación en `SENT` **nunca** se reenvía: el estado es el que garantiza la idempotencia.
- Un envío exitoso sella `sentAt`; uno fallido guarda el `errorMessage` del proveedor.
- Se define un tope de intentos a partir del cual la notificación se deja de reintentar y queda visible como fallida.
- El caso que justifica todo esto es el de la [regla 7](negocio/reglas-negocio.md): al desactivar una empresa se cancelan N turnos y se notifica a N clientes; si el proceso se corta a la mitad, el reintento tiene que tomar solo las que quedaron pendientes.

Tareas técnicas:
- Backend (Brune): job `@Scheduled` de reintento, con su test. *(Reasignada de Nico.)*

Dependencias: US-07.5 (la fila de `Notification` ya se persiste desde el Paso 7), US-10.1 (canal real que efectivamente puede fallar).

---

### Paso 11 — Automatizaciones transversales

Tres tareas chicas, cada una con un único responsable.

#### US-11.1 — Sistema transiciona automáticamente los turnos vencidos a COMPLETED
Responsable: Nico (continuidad con el Paso 7, que lideró en backend).

Como sistema, necesito pasar automáticamente los turnos `PENDING` a `COMPLETED` una vez que su `endDateTime` ya pasó, para que "Mis turnos" y el turnero del admin reflejen el estado real sin intervención manual.

Criterios de aceptación:
- Un job programado recorre los turnos vencidos y actualiza su estado.
- El corte es `endDateTime`, no `startDateTime`: un turno de 90 minutos no está completado a los 5 minutos de haber empezado. Es justamente para esto que `endDateTime` se persiste en vez de calcularse al vuelo.
- Solo transiciona turnos en `PENDING` — `CANCELLED` es terminal y nunca pasa a `COMPLETED`. Los turnos reprogramados se identifican por `rescheduleCount > 0` y siguen en `PENDING` hasta que este mismo job los completa.
- El cambio se refleja en "Mis turnos" (US-08.3) y en el turnero del admin (US-09.2) sin que el cliente o el admin tengan que hacer nada.

Tareas técnicas:
- Backend (Nico): job programado (`@Scheduled`), con su test.

Dependencias: Paso 7-8 — necesita turnos reales ya reservados/cancelados/reprogramados para tener sentido probarlo.

#### US-11.2 — Pulido final de la cancelación en cascada al desactivar una empresa
Responsable: Brune (continuidad con el Paso 4, que lideró en backend).

Como superadmin, quiero que al desactivar una empresa (US-04.4) la cancelación en cascada de turnos pendientes quede prolija y sin casos borde sin cubrir, para confiar en que la baja de una empresa no deja turnos "colgados".

Criterios de aceptación:
- La cascada de US-04.4 cubre todos los turnos `PENDING` de esa empresa (incluidos los reprogramados, que siguen siendo `PENDING`).
- Cada cliente afectado recibe su notificación correspondiente (o queda registrado si el envío falla).
- La reactivación (US-04.5) no revive los turnos cancelados durante la baja.

Tareas técnicas:
- Backend (Brune): revisión y ajuste de la lógica ya escrita en US-04.4/US-04.5.

Dependencias: US-04.4, US-04.5. Conviene resolverlo después del Paso 7, para probar la cascada con turnos reales.

#### US-11.3 — Sistema purga los tokens vencidos y ya usados
Responsable: Brune. Es el más aislado de los tres jobs: no toca lógica de negocio, solo borra filas.

Como sistema, necesito eliminar periódicamente los `UserToken` vencidos y los ya usados, para que la tabla no crezca indefinidamente con credenciales muertas.

Criterios de aceptación:
- Un job programado diario borra los tokens con `expiresAt` en el pasado o con `usedAt` cargado.
- Es el tercero de los tres jobs previstos en `arquitectura.md`; los otros dos son US-11.1 y US-10.4.
- Borrar un token no afecta al `User`: son credenciales de un solo uso, no historial.

Tareas técnicas:
- Backend (Brune): job `@Scheduled` de purga, con su test.

Dependencias: Paso 3 (`UserToken` en uso real).

---

### Paso 12 — Testing de punta a punta

Verificaciones de que el sistema integrado funciona de punta a punta, con un responsable único cada una.

#### T-12.1 — E2E: reservar un turno desde la página pública
Responsable: Brune (lideró el frontend del flujo de reserva en el Paso 7).

Tareas:
- Test E2E con Playwright que recorre: entrar a `/{slug}` → elegir servicio → elegir horario → login/registro si hace falta → confirmar en el modal → verificar que el turno aparece en "Mis turnos".

Listo cuando: el test corre en verde contra el sistema integrado.

#### T-12.2 — E2E: cancelar un turno desde el turnero del admin
Responsable: Nico (lideró el frontend del turnero en el Paso 9).

Tareas:
- Test E2E con Playwright que recorre: login de admin → turnero → cancelar un turno → verificar que pasa a `CANCELLED` y el horario queda liberado.

Listo cuando: el test corre en verde contra el sistema integrado.

#### T-12.3 — Tests de las reglas de negocio críticas

> ✂️ **Alcance MVP:** **reducida**. No se recorre regla por regla ni caso de uso por caso de uso.

Responsables: Nico y Brune, cada uno sobre lo que lideró.

Tareas:
- Confirmar que hay tests de las reglas críticas: cálculo de disponibilidad (incluida la zona horaria), concurrencia y doble reserva, anticipación mínima, y plazos de cancelación y reprogramación.
- Completar los que falten.

Listo cuando: cada una de esas reglas tiene al menos un test identificado.

#### T-12.4 — Tests de integración de las garantías del esquema
Responsable: Brune. El cuarto de estos puntos —el lock pesimista— ya lo cubrió Nico en US-07.3; los otros tres quedan de este lado para balancear.

`backend/README.md` identifica cuatro garantías que **no se pueden validar con un test unitario ni con H2**, porque dependen de constraints específicas de PostgreSQL. Son las que sostienen el modelo, así que conviene tener el test que demuestra que están activas.

Tareas — con **Testcontainers** (`postgres:16`), no H2:
- **Constraint de exclusión:** dos `INSERT` de turnos superpuestos en la misma empresa terminan en `ConstraintViolationException` mapeada a 409. Un tercero con `status = CANCELLED` en el mismo horario debe pasar.
- **Índices únicos parciales de email:** el mismo email en dos empresas distintas pasa; repetido en la misma empresa falla; `Juan@mail.com` y `juan@mail.com` cuentan como el mismo.
- **FK compuestas:** un turno de la empresa A que apunta a un servicio o a un cliente de la empresa B es rechazado por la base, aunque la capa de servicio lo deje pasar. Es la tercera barrera de US-03.9.

Listo cuando: los tres tests corren en verde contra PostgreSQL real y en rojo si se quita la constraint que cada uno verifica.

---

### Paso 13 — Documentación y pulido

> ✂️ **Alcance MVP:** **reducido**. Sin revisión cruzada completa por módulo: se actualiza solo la documentación que haya quedado desfasada respecto de lo construido.

#### T-13.1 — Actualizar la documentación desfasada
Responsables: Nico y Brune, cada uno sobre los módulos que lideró.

Tareas:
- Revisar los READMEs y la documentación de negocio (`requerimientos-funcionales.md`, `detalles-flujos.md`, `casos-usos.md`, `reglas-negocio.md`) y corregir lo que no coincida con lo construido.
- Si se llegó a hacer US-03.8, sumarla a `requerimientos-funcionales.md` y a `detalles-flujos.md` §7.1.

Listo cuando: no queda ninguna discrepancia conocida entre la documentación y el sistema construido.

---

### Paso 14 — Despliegue final

#### T-14.1 — Despliegue final estable

- **Backend (Nico):** verificar que la versión final está desplegada y estable en Render, y que la base de producción tiene el superadmin cargado a mano según el procedimiento del README del backend (ver *Decisiones de diseño* → superadmin en producción).
- **Frontend (Brune):** verificar que la versión final está desplegada y estable en Vercel.

Listo cuando: ambos entornos responden sin errores con la última versión.

#### T-14.2 — Datos de demo para la presentación
Responsable: Brune (continuidad con T-02.4, que hizo el seed inicial).

Tareas:
- Cargar un par de empresas, servicios variados, y turnos en distintos estados (`PENDING`, `PENDING` con `rescheduleCount > 0`, `COMPLETED`, `CANCELLED`) para mostrar en la presentación.

Listo cuando: los datos de demo cubren los distintos estados y roles a mostrar.

---

### Paso 15 — Preparar la presentación

No hay información en la documentación sobre qué incluye la presentación (formato, duración, contenido esperado por la cátedra). Queda como punto abierto para completar con esos datos cuando los tengan.