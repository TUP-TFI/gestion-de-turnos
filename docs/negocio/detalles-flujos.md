## 7. Detalle de flujos por rol

### 7.1 Dos sistemas de login separados

**Panel de sistema** (`/login`): acá se loguean superadmin y admins de empresa, en una URL propia que no pertenece a ninguna empresa en particular. El backend valida contra `User` y devuelve un JWT con `userId`, `role` y `companyId` (si es `COMPANY_ADMIN`). El frontend redirige:

- `SUPERADMIN` → `/superadmin/dashboard`
- `COMPANY_ADMIN` → `/company/dashboard`

El admin de empresa **no se autoregistra**: su cuenta la crea el superadmin al dar de alta la empresa, con un link de activación.

**Link de activación:** el link contiene un token único de un solo uso, con vencimiento de 48 hs, asociado a ese `User` admin recién creado. Se persiste en la tabla `UserToken`, y de él **solo se guarda el hash** — nunca el valor en claro. El valor en claro existe una única vez, al momento de armar el link; así, si alguien obtuviera lectura de la base, no podría activar cuentas ajenas.

Al abrirlo, el admin ve un formulario de "definí tu contraseña"; al confirmarla, el `User` pasa de `PENDING_ACTIVATION` a `ACTIVE`, el token se sella con `usedAt` (garantizando el uso único) y ya puede loguearse normalmente. Si el token vence sin usarse, el superadmin puede reenviarlo desde el detalle de la empresa: eso invalida los tokens de activación anteriores de ese usuario y emite uno nuevo.

El mismo mecanismo, con `type = PASSWORD_RESET` y vencimiento de 1 hora, cubre el flujo de "olvidé mi contraseña" que el propio usuario dispara desde el login. Se envía por el mismo canal definido para las notificaciones (ver 7.6).

**Login/registro por empresa** (dentro de `plataforma.com/{slug}`): exclusivo para el cliente que quiere sacar un turno en esa empresa — no lo usa nadie del lado del negocio. El cliente se registra o inicia sesión con email y contraseña, siempre en el contexto de esa empresa puntual. El JWT que recibe incluye `companyId` fijo a esa empresa — ese token solo sirve para operar dentro de esa página pública. Si el mismo email ya está registrado como cliente de otra empresa, no hay conflicto: son cuentas independientes, la unicidad de email es por empresa, no global.

> Consecuencia práctica: no existe ningún punto del sistema donde una sesión de cliente "cruce" entre empresas. Cada `plataforma.com/{slug}` es, en términos de sesión, un compartimento aislado.

> Segunda consecuencia, esta vez restrictiva: como la unicidad es por `(companyId, email)` sin discriminar el rol, el admin de una empresa no puede además registrarse como cliente de su propia empresa con el mismo email. Es una decisión asumida a propósito; si quiere reservarse un turno, lo carga como reserva manual desde su turnero.

---

### 7.2 ¿Qué ve el superadmin?

Al loguearse ve `/superadmin/dashboard` con:

- Empresas activas vs. inactivas.
- Altas de empresas por mes.
- Ranking de empresas por cantidad de turnos generados.
- Distribución por rubro.

**Altas por mes:** se agrupa por `company.createdAt`. Es la razón por la que las tablas llevan columnas de auditoría: sin `createdAt` la métrica directamente no se puede calcular.

**Ranking por cantidad de turnos:** es un `COUNT(*)` sobre la tabla `Appointment` agrupado por `companyId`:

```sql
SELECT company_id, COUNT(*)
FROM appointment
GROUP BY company_id
ORDER BY COUNT(*) DESC
```

No hace falta que el superadmin vea el detalle de cada turno — el conteo alcanza, y no expone datos de clientes ni de ingresos de cada negocio. "Generados" cuenta todos los turnos creados, sin importar el estado, porque mide actividad/uso de la plataforma, no rendimiento del negocio.

**Distribución por rubro:** se agrupa por `company.category`, que es un enum cerrado y no texto libre. Con texto libre, `Peluquería`, `peluqueria` y `Peluqueria` contarían como tres rubros distintos y el gráfico quedaría inutilizable.

#### CRUD de empresas, en detalle

- **Alta (Create):** un formulario con los datos de la empresa (nombre, descripción, dirección, teléfono, email de contacto, rubro, zona horaria, logo) y los datos del futuro admin (nombre, apellido, email, teléfono). Al confirmar, en una sola transacción: se genera el slug a partir del nombre (validando que no exista ni colisione con una ruta reservada, agregando un sufijo si hace falta), se crea `Company` en estado `ACTIVE`, se crea el `User` admin en estado `PENDING_ACTIVATION` —sin contraseña todavía—, y se dispara el link de activación (ver 7.1). La URL pública `plataforma.com/{slug}` ya resuelve desde ese momento, mostrando el estado de "todavía sin servicios cargados".
- **Baja (Delete lógico):** desactivar la empresa (`status = INACTIVE`), nunca eliminar el registro. Esto dispara: se bloquea el login del admin, la página pública deja de mostrar servicios/reserva y en su lugar informa que el negocio no está disponible por este medio, y los turnos pendientes se cancelan automáticamente notificando a cada cliente afectado. El historial (turnos pasados) se conserva por si la empresa se reactiva más adelante.
- **Reactivación:** desde el mismo listado, un botón inverso vuelve la empresa a `ACTIVE` y reactiva el login del admin. Como servicios y horarios no se borran al desactivar, la página pública queda funcionando de nuevo sin que el admin tenga que volver a cargar nada — salvo los turnos que se cancelaron durante la baja, que quedan cancelados.
- **Edición (Update):** campos editables sin mayor riesgo: nombre, descripción, dirección, teléfono, email de contacto, logo, rubro, zona horaria. El slug (URL pública) **no** se hace editable desde este formulario — cambiarlo rompe cualquier link ya compartido; si en algún momento hace falta cambiarlo, que sea una acción aparte y explícita.
- **Consulta (Read):** vista simple con los datos de la empresa y un link directo a "Ver página pública".

> **Cómo se bloquea el login del admin al desactivar la empresa:** no se toca el `status` del `User`. El filtro de autenticación consulta el estado de la empresa asociada y rechaza el login si está `INACTIVE`. La alternativa —marcar al usuario como inactivo— parece equivalente pero no lo es: si ese admin ya estaba desactivado *antes* de la baja de la empresa, al reactivarla quedaría reactivado por error. Derivarlo del estado de la empresa hace la operación idempotente y elimina un estado que sincronizar.

Al entrar a "Ver página pública" desde el CRUD, el superadmin la ve como cualquier visitante sin loguear: puede mirar servicios y horarios, pero para reservar necesitaría loguearse ahí como cliente de esa empresa, igual que cualquier persona — el login de sistema y el de cliente son mundos separados, no hay sesión que se filtre de uno a otro.

#### Configuración global de la plataforma

Una pantalla con tres campos, que el superadmin edita y quedan vigentes de inmediato para todas las empresas:

| Parámetro | Valor inicial |
| :--- | :--- |
| Anticipación mínima para reservar | 30 minutos |
| Plazo máximo de cancelación/reprogramación | 3 horas |
| Máximo de reprogramaciones por turno | 2 |

Están en base de datos (`PlatformSettings`, tabla de una sola fila) y no en el archivo de configuración del backend, justamente porque están documentados como ajustables: si vivieran en el `application.yml`, ajustarlos implicaría un redeploy. Cambiarlos no afecta retroactivamente a los turnos ya reservados; rige sobre las validaciones de las operaciones posteriores.

---

### 7.3 ¿Qué ve el admin de empresa?

Al loguearse ve `/company/dashboard`, con navegación a: Dashboard, Servicios, Horarios, Excepciones, Turnos, Clientes, Mi página pública.

#### CRUD de Servicios y baja con turnos pendientes

Al desactivar un servicio, si tiene turnos futuros asociados, la opción más simple es la mejor: el servicio deja de estar disponible para nuevas reservas, pero los turnos ya reservados no se tocan — se atienden con normalidad, como si el servicio siguiera activo para esos casos puntuales. Esto evita disparar una notificación masiva a cada cliente avisando que "su servicio fue dado de baja", porque nadie pierde su turno. Si el admin específicamente quiere cancelar esos turnos, lo hace como una acción aparte, turno por turno, desde el turnero — no como consecuencia automática de dar de baja el servicio. Igual tiene sentido un `confirm` antes de desactivar, mostrando cuántos turnos futuros tiene ese servicio.

> **Editar precio o duración de un servicio tampoco toca los turnos ya reservados.** Cada turno guarda su propia copia del precio y la duración con que fue reservado. Sin esa copia, subir la duración de "Corte" de 30 a 60 minutos alargaría retroactivamente todos los turnos de "Corte" ya agendados y los haría superponerse entre sí: la agenda se rompería sola, sin que nadie hubiera tocado un turno.

#### CRUD de Horarios, en detalle

No es un listado de turnos, es la configuración general de disponibilidad. El admin define, por día de la semana, un rango horario y un intervalo. Por ejemplo:

| Día | Desde | Hasta | Intervalo |
|---|---|---|---|
| Lunes a viernes | 09:00 | 13:00 | 30 min |
| Lunes a viernes | 16:00 | 20:00 | 30 min |
| Sábado | 09:00 | 13:00 | 30 min |
| Domingo | — sin configurar (cerrado) — | | |

Se admiten **varias franjas para el mismo día** (jornada partida, como en el ejemplo), siempre que no se superpongan entre sí. Un día sin ninguna franja cargada significa que no se atiende ese día.

El CRUD acá es sobre esas filas: crear un rango para un día, editarlo, o eliminarlo. No se cargan turnos ni horarios sueltos a mano — esta configuración es el insumo que el sistema usa después para calcular disponibilidad.

La configuración se hace **una sola vez**: es una plantilla recurrente por día de la semana ("todos los lunes de 9 a 13 y de 16 a 20"), no algo que se cargue semana a semana. Rige indefinidamente hacia adelante hasta que el admin la edite. El sistema, al calcular disponibilidad para una fecha puntual, mira qué día de la semana es y aplica la configuración correspondiente.

**Qué pasa con turnos existentes al editar esta tabla:** no se tocan. El `Appointment` guarda su propia fecha/hora ya confirmada, independiente de la configuración de horarios. Si el admin achica el rango (ej. cierra los sábados) y hay turnos ya reservados en sábado, esos turnos quedan como están — el sistema solo avisa: *"tenés X turnos ya reservados fuera del nuevo horario, van a mantenerse igual"*, y el admin decide si los cancela a mano o los deja.

#### Excepciones de calendario

La plantilla semanal resuelve el caso habitual, pero no alcanza para las fechas puntuales: feriados, vacaciones, o un día que se atiende con horario especial. Para eso está esta pantalla, donde el admin carga excepciones **por fecha concreta** (no recurrentes):

| Fecha | Tipo | Motivo |
|---|---|---|
| 25/12/2026 | Cerrado todo el día | Feriado |
| 02/01/2027 al 15/01/2027 | Cerrado todo el día | Vacaciones (una fila por fecha) |
| 24/12/2026 | Abierto 09:00–13:00, intervalo 30 min | Media jornada |

**Regla de precedencia:** si existe una excepción para una fecha, **reemplaza por completo** la configuración semanal de ese día. No se combinan ni se restan: o rige la excepción, o rige la plantilla. Es la regla más simple de explicar y la más previsible para el admin.

Igual que al editar horarios, cargar una excepción no cancela turnos ya reservados en esa fecha: el sistema informa cuántos quedan fuera y el admin decide.

#### Cálculo de disponibilidad, con ejemplo concreto

El sistema no guarda una tabla de "horarios disponibles"; la calcula al vuelo cada vez que alguien elige un servicio. Con el ejemplo de arriba (lunes a viernes 09:00–13:00, intervalo 30 min) y un servicio de 90 minutos (ej. "Color"):

1. Determina qué configuración rige para la fecha pedida: si hay una `ScheduleException`, usa esa; si no, las franjas de `BusinessHours` de ese día de la semana. Si no hay ninguna de las dos, no hay disponibilidad.
2. Genera los horarios de inicio candidatos según el intervalo: 09:00, 09:30, 10:00, 10:30…
3. Convierte cada candidato a un instante UTC usando la **zona horaria de la empresa** (`company.timezone`), no la del servidor. Sin esto, el mismo código desplegado en Render —que corre en UTC— ofrecería horarios corridos tres horas.
4. Para cada candidato, calcula el rango que ocuparía el servicio elegido (ej. eligiendo 10:00 → ocuparía 10:00 a 11:30) y descarta el que se pase del cierre de la franja.
5. Descarta los candidatos cuyo rango se superpone con un turno ya reservado en esa franja.
6. Descarta los que no cumplen la anticipación mínima configurada, y los que ya pasaron.
7. Devuelve al frontend solo los horarios de inicio que quedan libres para ese servicio puntual.

Por eso un servicio corto (ej. "Corte", 30 min) puede tener disponible un horario que un servicio largo (ej. "Color", 90 min) no puede ocupar, aunque ambos usen la misma configuración de horarios.

#### Clientes

Vista de **solo lectura** con los clientes registrados en esa empresa, buscable por nombre, teléfono o email. No es un CRUD: las cuentas de cliente las crea el propio cliente al registrarse en la página pública, y el admin no las edita ni las da de baja. Su función es alimentar el desplegable de vinculación al cargar un turno manual (ver abajo).

Como toda consulta del panel, está acotada al `companyId` de la sesión: un admin nunca ve clientes de otra empresa.

#### Turnero — reserva manual del admin

Es una vista parecida a la del cliente (mismo selector de servicio/horario). Antes de cargar los datos, el admin indica si el cliente tiene cuenta en la plataforma:

- **Si tiene cuenta:** lo busca y selecciona desde el listado de Clientes (por nombre, teléfono o email) y el turno queda vinculado a su `clientId`, igual que una reserva online. Después, ese cliente puede loguearse y autogestionar el turno desde "Mis turnos" como cualquier otro.
- **Si no tiene cuenta:** el admin carga nombre y teléfono sueltos (`manualClientName`/`manualClientPhone`), sin `clientId`. Ese turno no es autogestionable por el cliente.

En ambos casos el turno queda marcado con `origin = MANUAL`, y el turnero permite filtrar por eso. Sin ese campo, un turno manual vinculado a una cuenta existente sería indistinguible de uno que reservó el propio cliente.

Al guardar, se dispara la misma notificación de confirmación que en una reserva online. **La cancelación de un turno sin cuenta vinculada se hace desde acá, desde el turnero del panel.**

> **Problema detectado:** si el turno queda sin cuenta vinculada (cliente sin cuenta en la plataforma), esa persona no puede entrar a "Mis turnos" para cancelarlo por su cuenta.
>
> **Solución para el MVP:** el admin cancela desde el turnero. Un link con token en el mensaje de confirmación para autogestionar la cancelación sin cuenta es técnicamente simple de agregar (la tabla `UserToken` ya provee el mecanismo), pero queda anotado como **mejora post-MVP** para no sumar otro flujo de cancelación distinto al ya definido. Por eso, en esta versión, el mensaje de confirmación **no lleva ningún link**.

El mensaje de confirmación es puramente informativo (tipo *"reservaste tal turno, gracias"*), no requiere que el cliente confirme nada: el turno queda guardado como `PENDING` en el momento en que se completa el flujo de reserva (con el lock pesimista), no cuando llega el mensaje.

#### Mi página pública

El admin no la edita directamente campo por campo; se arma sola a partir de lo que carga en Servicios/Horarios/datos de empresa. Solo tiene un link de acceso para verla como la ve un cliente.

---

### 7.4 Página pública y flujo de reserva del cliente

La página pública (`plataforma.com/{slug}`) es visible sin login: cualquiera ve servicios y puede explorar horarios. Todas las empresas comparten el mismo componente de página (card de servicio, selector de horario, formulario de reserva), instanciado con los datos propios de cada una vía slug — solo cambia logo, color y contenido, no la estructura.

**Flujo:**

1. El visitante ve los servicios de la empresa.
2. Elige un servicio → el sistema calcula la disponibilidad real.
3. Elige un horario disponible.
4. Si no está logueado como cliente de esa empresa, se le pide login/registro (propio de ese `{slug}`) antes de confirmar.
5. Confirma la reserva en un modal de verificación.
6. El turno se guarda y aparece en "Mis turnos". Se dispara la notificación de confirmación.

**Qué pasa exactamente en el paso 6**, dentro de una única transacción:

```text
1. SELECT ... FOR UPDATE sobre la fila de company   -- lock pesimista
2. valida anticipación mínima (config global)
3. valida contra BusinessHours / ScheduleException
4. valida superposición por rango contra los turnos de esa empresa
5. INSERT del turno, copiando precio y duración del servicio
6. COMMIT                                          -- recién acá se libera el lock
7. (después del commit) se emite la notificación
```

> **Por qué se bloquea la fila de `company` y no otra cosa:** el turno que se quiere crear todavía no existe, así que no hay una fila propia para bloquear. Sin un objeto de bloqueo explícito, dos personas reservando el mismo horario al mismo tiempo leerían ambas "está libre" e insertarían las dos. Bloquear la empresa serializa las reservas de ese negocio — que es exactamente el grano que hace falta — sin frenar a las demás empresas de la plataforma.
>
> Como red de seguridad, la base declara además una constraint de exclusión que impide físicamente dos turnos superpuestos en la misma empresa. El lock da el mensaje de error claro; la constraint garantiza que ningún camino del código pueda romper la invariante.

> **Por qué la notificación va después del commit:** llamar a la API de WhatsApp dentro de la transacción mantendría tomado el lock de la empresa durante toda la latencia de la red. Un proveedor lento serializaría las reservas de todo el negocio detrás suyo.

---

### 7.5 Mis turnos, cancelación y reprogramación

"Mis turnos" es una tabla simple: fecha, servicio, empresa, estado. No hay una sección de historial aparte — un job programado pasa los turnos a `COMPLETED` automáticamente una vez que terminaron, es decir cuando su `endDateTime` ya pasó (los `CANCELLED` quedan como están), y la tabla se ordena/filtra por estado; los `COMPLETED` cumplen la función de historial.

- **Cancelar:** disponible hasta el plazo configurado antes del turno (inicialmente 3 horas). El turno pasa a estado `CANCELLED` y se muestra en gris, sin acción de reactivación desde ahí. Queda registrado quién canceló y cuándo.
- **Reprogramar:** dentro del mismo plazo, el cliente elige un nuevo horario disponible; se actualiza el mismo registro de turno (no se crea uno nuevo), guardando la fecha anterior y sumando uno a su contador de reprogramaciones. **El turno sigue en estado `PENDING`.**
- **Límite de reprogramaciones:** cada turno puede reprogramarse hasta el máximo configurado (inicialmente **2 veces**). Alcanzado el tope, el botón queda deshabilitado con la leyenda correspondiente y el cliente debe contactar al negocio.
- **Pasado el plazo**, el cliente ya no puede autogestionar el cambio desde "Mis turnos" — el botón queda deshabilitado. Si necesita cancelar igual, contacta al negocio por fuera del sistema (WhatsApp, teléfono); ahí es el admin quien cancela desde su turnero (sin restricción horaria), liberando el horario automáticamente para que otra persona lo reserve. El mismo mecanismo aplica para los turnos cargados manualmente sin cuenta (ver 7.3).
- **Anticipación mínima para reservar:** no se puede reservar un turno con menos de la anticipación configurada (inicialmente **30 minutos**) sobre el horario elegido, además de no dejar reservar en horarios ya pasados. Evita reservas de último segundo que el negocio no llega a ver a tiempo.

> **Por qué reprogramar no es un estado:** el modelo llevaba antes un estado `RESCHEDULED`, que mezclaba dos cosas distintas — el ciclo de vida del turno (pendiente → completado / cancelado) y un evento que le ocurrió. Traía dos problemas concretos: un turno reprogramado que ya había pasado se convertía en `COMPLETED` y perdía el dato de que había sido reprogramado; y, con un tope de dos reprogramaciones, el estado no alcanzaba para saber cuántas se llevaban usadas. Con un contador, el turno sigue siendo `PENDING` —que es lo que realmente es—, el filtro de "reprogramados" en esta pantalla sigue funcionando, y el dato sobrevive a la transición a `COMPLETED`.

---

### 7.6 Notificaciones

La idea de "desacoplado" es esta: el código que reserva/cancela/reprograma un turno no sabe ni le importa por qué canal se avisa al cliente. Cuando pasa uno de esos eventos, simplemente le dice al `NotificationService` "avisale al cliente X que pasó Y con su turno Z", y es el `NotificationService` el que decide cómo armar y mandar ese mensaje. Si mañana cambia el canal, no hace falta tocar la lógica de reservas — solo el servicio de notificaciones.

**Eventos que disparan una notificación:**
- Reserva confirmada (online o cargada manualmente por el admin).
- Cancelación (por cliente o por admin).
- Reprogramación (nueva fecha confirmada).
- Activación de cuenta y recuperación de contraseña (links con token, ver 7.1).

**Canal:** el objetivo es WhatsApp (vía alguna API de WhatsApp Business — proveedor aún sin definir). Es una decisión de proyecto, no un fallback en tiempo real: si llegado el momento no da el tiempo o la complejidad para integrar WhatsApp, se implementa el mismo `NotificationService` mandando por mail en su lugar. No se contempla tener ambos canales activos con reintento automático — eso implicaría implementar las dos integraciones más una lógica de detección de fallos, fuera del alcance definido.

**Cada notificación se persiste** en la tabla `Notification`, con su tipo de evento, canal, destinatario, estado (`PENDING`/`SENT`/`FAILED`), cantidad de intentos y el error del proveedor si lo hubo. El motivo es concreto: el canal es una API externa, y sin registro un timeout deja al sistema sin saber si el mensaje salió, sin poder reintentar y sin poder evitar un envío duplicado.

El caso más sensible es la desactivación de una empresa: se cancelan N turnos y hay que avisarle a N clientes. Si ese proceso se corta a la mitad, con el registro se reintenta solo sobre las que quedaron en `FAILED`/`PENDING`, sin volver a molestar a quien ya recibió el aviso. Sin el registro, la única opción sería reenviarle a todos.

Además, para la defensa del trabajo, es lo que permite **mostrar** que el flujo de notificaciones funciona, en lugar de afirmarlo.

**Momento del envío:** siempre después de confirmada la transacción del turno (`@TransactionalEventListener(AFTER_COMMIT)`), nunca dentro de ella. Ver la explicación en 7.4.

---

### 7.7 Autenticación — resumen técnico

- Dos JWT distintos según el sistema de login: uno para el panel de sistema (superadmin/admin de empresa) y uno por sesión de cliente, atado a un único `companyId`.
- Ambos tokens llevan `role` y `companyId` (si aplica) para que el backend valide permisos en cada endpoint sin volver a consultar la base en cada request.
- **Expiración:** sin refresh token (no suma la complejidad extra al alcance de la tesis). El JWT expira a las **24 horas**; pasado ese tiempo, hay que volver a loguearse, no hay renovación automática.
- Login con email + contraseña en ambos sistemas. La comparación del email es case-insensitive.
- El `companyId` del token no es solo informativo: es el parámetro con el que se filtra **toda** consulta de datos de empresa. Ver la estrategia completa de aislamiento multi-tenant en [esquema-bd.md](../tecnologias/esquema-bd.md#4-aislamiento-multi-tenant).
