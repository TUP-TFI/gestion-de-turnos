# 🗄️ Esquema físico de base de datos

Traducción a PostgreSQL del [diccionario de datos](diccionario-datos.md). Contiene el **DDL de referencia** (base de la primera migración de Flyway) y el **DBML** para regenerar el diagrama en [dbdiagram.io](https://dbdiagram.io).

- Nomenclatura física: `snake_case`, según [convenciones](convenciones.md).
- Los enums del modelo lógico se implementan como `varchar` + `CHECK`. Ver la justificación en el diccionario.
- Las migraciones se versionan con **Flyway** (`src/main/resources/db/migration`, formato `V<n>__<descripcion>.sql`). `ddl-auto` queda en `validate`: el esquema lo define este archivo, no Hibernate.

> ⚠️ Buena parte de lo que sigue —índices parciales, `CHECK`, constraints de exclusión— **Hibernate no lo puede generar**. Es la razón principal por la que el proyecto usa migraciones versionadas en lugar de `ddl-auto: update`.

---

## 1. Extensiones

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;    -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS btree_gist;  -- permite mezclar = y && en una constraint de exclusión
```

---

## 2. DDL

### 2.1 `platform_settings`

```sql
CREATE TABLE platform_settings (
  id                            smallint     PRIMARY KEY DEFAULT 1,
  min_booking_notice_minutes    integer      NOT NULL DEFAULT 30,
  cancellation_deadline_hours   integer      NOT NULL DEFAULT 3,
  max_reschedule_count          integer      NOT NULL DEFAULT 2,
  max_booking_advance_days      integer      NOT NULL DEFAULT 90, -- ⚠️ valor a confirmar
  updated_at                    timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT chk_settings_singleton    CHECK (id = 1),
  CONSTRAINT chk_settings_notice       CHECK (min_booking_notice_minutes > 0),
  CONSTRAINT chk_settings_deadline     CHECK (cancellation_deadline_hours > 0),
  CONSTRAINT chk_settings_reschedule   CHECK (max_reschedule_count >= 0),
  CONSTRAINT chk_settings_advance      CHECK (max_booking_advance_days > 0)
);

-- La fila única se inserta en la misma migración: la aplicación nunca la crea.
INSERT INTO platform_settings (id) VALUES (1);
```

### 2.2 `company`

```sql
CREATE TABLE company (
  id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  name           varchar(120) NOT NULL,
  slug           varchar(60)  NOT NULL UNIQUE,
  description    text,
  address        varchar(200),
  phone          varchar(30),
  contact_email  varchar(150),
  category       varchar(30)  NOT NULL,
  timezone       varchar(50)  NOT NULL DEFAULT 'America/Argentina/Cordoba',
  logo_url       varchar(500),
  primary_color  varchar(7),
  status         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at     timestamptz  NOT NULL DEFAULT now(),
  updated_at     timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT chk_company_status   CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT chk_company_category CHECK (category IN (
      'BARBERSHOP', 'HAIR_SALON', 'BEAUTY_CENTER', 'MEDICAL_OFFICE', 'DENTAL_OFFICE',
      'VETERINARY', 'NUTRITION', 'PSYCHOLOGY', 'PHYSIOTHERAPY', 'TATTOO_STUDIO', 'SPA', 'OTHER')),
  CONSTRAINT chk_company_slug     CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT chk_company_color    CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX idx_company_status_created ON company (status, created_at);
```

> La lista de **slugs reservados** (`api`, `login`, `superadmin`, …) se valida en la capa de servicio, no acá: es una regla de ruteo de la aplicación, y mantenerla en SQL obligaría a una migración cada vez que se agrega una ruta.

### 2.3 `app_user`

```sql
CREATE TABLE app_user (
  id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name  varchar(80)  NOT NULL,
  last_name   varchar(80)  NOT NULL,
  email       varchar(150) NOT NULL,
  phone       varchar(30)  NOT NULL,
  password    varchar(100),
  role        varchar(20)  NOT NULL,
  company_id  uuid         REFERENCES company (id),
  status      varchar(25)  NOT NULL,
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT chk_user_role   CHECK (role IN ('SUPERADMIN', 'COMPANY_ADMIN', 'CLIENT')),
  CONSTRAINT chk_user_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'INACTIVE')),

  -- companyId es nulo si y solo si el rol es SUPERADMIN
  CONSTRAINT chk_user_company_by_role CHECK (
      (role =  'SUPERADMIN' AND company_id IS NULL) OR
      (role <> 'SUPERADMIN' AND company_id IS NOT NULL)),

  -- necesario para que appointment pueda referenciar (client_id, company_id)
  CONSTRAINT uq_user_id_company UNIQUE (id, company_id)
);

-- Unicidad de email: por empresa para admins y clientes...
CREATE UNIQUE INDEX ux_user_email_per_company
  ON app_user (company_id, lower(email))
  WHERE company_id IS NOT NULL;

-- ...y global entre superadmins. Hacen falta los dos índices porque en PostgreSQL
-- los NULL se consideran distintos entre sí: un UNIQUE (company_id, email) dejaría
-- pasar dos superadmins con el mismo email.
CREATE UNIQUE INDEX ux_superadmin_email
  ON app_user (lower(email))
  WHERE company_id IS NULL;
```

> `password` es nullable a propósito: un `COMPANY_ADMIN` recién creado está en `PENDING_ACTIVATION` y todavía no definió ninguna. Es preferible a guardar un hash ficticio.

### 2.4 `user_token`

```sql
CREATE TABLE user_token (
  id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  token_hash  varchar(100) NOT NULL UNIQUE,
  type        varchar(20)  NOT NULL,
  expires_at  timestamptz  NOT NULL,
  used_at     timestamptz,
  created_at  timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT chk_token_type CHECK (type IN ('ACTIVATION', 'PASSWORD_RESET'))
);

-- Busca el token vigente de un usuario al reenviar un link o al validar uno entrante.
CREATE INDEX idx_user_token_lookup ON user_token (user_id, type) WHERE used_at IS NULL;
```

### 2.5 `service`

```sql
CREATE TABLE service (
  id                uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id        uuid          NOT NULL REFERENCES company (id),
  name              varchar(120)  NOT NULL,
  description       text,
  price             numeric(10,2) NOT NULL,
  duration_minutes  integer       NOT NULL,
  status            varchar(20)   NOT NULL DEFAULT 'ACTIVE',
  created_at        timestamptz   NOT NULL DEFAULT now(),
  updated_at        timestamptz   NOT NULL DEFAULT now(),

  CONSTRAINT chk_service_status   CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT chk_service_price    CHECK (price >= 0),
  CONSTRAINT chk_service_duration CHECK (duration_minutes > 0),

  CONSTRAINT uq_service_id_company UNIQUE (id, company_id)
);

CREATE INDEX idx_service_company_status ON service (company_id, status);
```

> `numeric(10,2)` y no `decimal` sin precisión ni `double`: evita errores de redondeo en valores monetarios.

### 2.6 `business_hours`

```sql
CREATE TABLE business_hours (
  id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id        uuid        NOT NULL REFERENCES company (id) ON DELETE CASCADE,
  day_of_week       varchar(10) NOT NULL,
  start_time        time        NOT NULL,
  end_time          time        NOT NULL,
  interval_minutes  integer     NOT NULL,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT chk_hours_day      CHECK (day_of_week IN (
      'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
  CONSTRAINT chk_hours_range    CHECK (end_time > start_time),
  CONSTRAINT chk_hours_interval CHECK (interval_minutes > 0),

  -- Se admiten varias franjas por día (jornada partida), pero nunca superpuestas.
  -- El offset sobre una fecha fija convierte `time` en `timestamp` para poder usar tsrange;
  -- PostgreSQL no trae un tipo de rango nativo para `time`.
  CONSTRAINT excl_hours_overlap EXCLUDE USING gist (
      company_id  WITH =,
      day_of_week WITH =,
      tsrange(DATE '2000-01-01' + start_time, DATE '2000-01-01' + end_time) WITH &&
  )
);

CREATE INDEX idx_hours_company_day ON business_hours (company_id, day_of_week);
```

### 2.7 `schedule_exception`

```sql
CREATE TABLE schedule_exception (
  id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id        uuid         NOT NULL REFERENCES company (id) ON DELETE CASCADE,
  exception_date    date         NOT NULL,
  is_closed         boolean      NOT NULL DEFAULT true,
  start_time        time,
  end_time          time,
  interval_minutes  integer,
  reason            varchar(200),
  created_at        timestamptz  NOT NULL DEFAULT now(),
  updated_at        timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT uq_exception_company_date UNIQUE (company_id, exception_date),

  -- Cerrado: sin horario. Abierto con horario especial: los tres campos cargados.
  CONSTRAINT chk_exception_shape CHECK (
      (is_closed = true  AND start_time IS NULL AND end_time IS NULL AND interval_minutes IS NULL) OR
      (is_closed = false AND start_time IS NOT NULL AND end_time IS NOT NULL AND interval_minutes IS NOT NULL)),
  CONSTRAINT chk_exception_range    CHECK (end_time IS NULL OR end_time > start_time),
  CONSTRAINT chk_exception_interval CHECK (interval_minutes IS NULL OR interval_minutes > 0)
);

CREATE INDEX idx_exception_company_date ON schedule_exception (company_id, exception_date);
```
> ⚠️ **`exception_date` no se valida con un `CHECK` en la base** porque la regla depende de `company.timezone` (el "hoy" varía según la zona horaria de cada empresa), y un `CHECK` no puede consultar otra tabla para resolver ese valor. La validación de "no fechas pasadas" se hace en la capa de service del backend, donde sí hay acceso al `timezone` de la empresa.

### 2.8 `appointment`

```sql
CREATE TABLE appointment (
  id                         uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id                 uuid          NOT NULL REFERENCES company (id),
  service_id                 uuid          NOT NULL,
  client_id                  uuid,
  manual_client_name         varchar(160),
  manual_client_phone        varchar(30),
  price_snapshot             numeric(10,2) NOT NULL,
  duration_minutes_snapshot  integer       NOT NULL,
  start_date_time            timestamptz   NOT NULL,
  end_date_time              timestamptz   NOT NULL,
  previous_start_date_time   timestamptz,
  reschedule_count           integer       NOT NULL DEFAULT 0,
  origin                     varchar(10)   NOT NULL,
  status                     varchar(20)   NOT NULL DEFAULT 'PENDING',
  cancelled_by               varchar(10),
  cancellation_reason        text,
  cancelled_at               timestamptz,
  created_at                 timestamptz   NOT NULL DEFAULT now(),
  updated_at                 timestamptz   NOT NULL DEFAULT now(),

  -- FK compuestas: garantizan a nivel motor que el servicio y el cliente
  -- pertenecen a la MISMA empresa que el turno.
  CONSTRAINT fk_appointment_service FOREIGN KEY (service_id, company_id)
      REFERENCES service (id, company_id),
  CONSTRAINT fk_appointment_client  FOREIGN KEY (client_id, company_id)
      REFERENCES app_user (id, company_id),

  CONSTRAINT chk_appointment_status   CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT chk_appointment_origin   CHECK (origin IN ('ONLINE', 'MANUAL')),
  CONSTRAINT chk_appointment_price    CHECK (price_snapshot >= 0),
  CONSTRAINT chk_appointment_duration CHECK (duration_minutes_snapshot > 0),
  CONSTRAINT chk_appointment_end      CHECK (end_date_time > start_date_time),

  -- O el turno está vinculado a una cuenta, o trae datos de contacto sueltos.
  -- Nunca ninguna de las dos: sería un turno que nadie puede contactar.
  CONSTRAINT chk_appointment_client_identity CHECK (
      (client_id IS NOT NULL AND manual_client_name IS NULL     AND manual_client_phone IS NULL) OR
      (client_id IS NULL     AND manual_client_name IS NOT NULL AND manual_client_phone IS NOT NULL)),

  CONSTRAINT chk_appointment_cancellation CHECK (
      (status =  'CANCELLED' AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL) OR
      (status <> 'CANCELLED' AND cancelled_by IS NULL     AND cancelled_at IS NULL)),
  CONSTRAINT chk_appointment_cancelled_by CHECK (
      cancelled_by IS NULL OR cancelled_by IN ('CLIENT', 'COMPANY')),

  CONSTRAINT chk_appointment_reschedule CHECK (
      (reschedule_count = 0 AND previous_start_date_time IS NULL) OR
      (reschedule_count > 0 AND previous_start_date_time IS NOT NULL)),

  -- Red de seguridad del motor contra la doble reserva: dentro de una misma empresa,
  -- dos turnos no cancelados no pueden solapar sus rangos horarios.
  CONSTRAINT excl_appointment_overlap EXCLUDE USING gist (
      company_id WITH =,
      tstzrange(start_date_time, end_date_time) WITH &&
  ) WHERE (status <> 'CANCELLED')
);
```

**Índices:**

```sql
-- Turnero del admin, filtrable por estado
CREATE INDEX idx_appointment_company_status_start
  ON appointment (company_id, status, start_date_time);

-- "Mis turnos" del cliente
CREATE INDEX idx_appointment_client_start
  ON appointment (client_id, start_date_time)
  WHERE client_id IS NOT NULL;

-- UC9: contar turnos futuros de un servicio antes de desactivarlo
CREATE INDEX idx_appointment_service_pending
  ON appointment (service_id)
  WHERE status = 'PENDING';

-- Job de cierre automático: sin este índice recorre la tabla completa en cada corrida
CREATE INDEX idx_appointment_pending_start
  ON appointment (start_date_time)
  WHERE status = 'PENDING';
```

> 📌 **`end_date_time` es una columna común, no generada.** La expresión `timestamptz + interval` es `STABLE` en PostgreSQL (depende de la zona horaria de sesión), y las columnas generadas exigen expresiones `IMMUTABLE`. La escribe la capa de servicio en el alta y en cada reprogramación: `start.plus(Duration.ofMinutes(durationMinutesSnapshot))`.

> 📌 **La constraint de exclusión no reemplaza al lock pesimista**, lo respalda. El lock produce un mensaje de negocio claro ("ese horario ya fue tomado"); la constraint garantiza que ningún camino del código —un job, una migración, un `INSERT` manual— pueda romper la invariante. Hibernate no la conoce, así que su violación llega como `ConstraintViolationException` y se mapea a **HTTP 409** en el `@RestControllerAdvice`.

### 2.9 `notification`

```sql
CREATE TABLE notification (
  id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  appointment_id  uuid         REFERENCES appointment (id) ON DELETE SET NULL,
  user_id         uuid         REFERENCES app_user (id) ON DELETE SET NULL,
  recipient_phone varchar(30)  NOT NULL,
  event_type      varchar(30)  NOT NULL,
  channel         varchar(15)  NOT NULL,
  status          varchar(15)  NOT NULL DEFAULT 'PENDING',
  attempt_count   integer      NOT NULL DEFAULT 0,
  sent_at         timestamptz,
  error_message   text,
  created_at      timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT chk_notification_event CHECK (event_type IN (
      'BOOKING_CONFIRMED', 'APPOINTMENT_CANCELLED', 'APPOINTMENT_RESCHEDULED',
      'ACCOUNT_ACTIVATION', 'PASSWORD_RESET')),
  CONSTRAINT chk_notification_channel CHECK (channel IN ('WHATSAPP', 'EMAIL')),
  CONSTRAINT chk_notification_status  CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
  CONSTRAINT chk_notification_sent    CHECK (
      (status = 'SENT' AND sent_at IS NOT NULL) OR (status <> 'SENT' AND sent_at IS NULL))
);

-- Cola de pendientes y reintentos
CREATE INDEX idx_notification_pending ON notification (created_at)
  WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_notification_appointment ON notification (appointment_id);
```

---

## 3. Estrategia de concurrencia en la reserva

La [regla 14](../negocio/reglas-negocio.md) exige lock pesimista, pero el turno a crear **todavía no existe**: no hay fila propia que bloquear. Sin un objeto de bloqueo explícito, dos requests simultáneos leerían ambos "no hay superposición" y ambos insertarían.

La fila que se bloquea es la de **`company`**, que serializa las reservas de ese negocio — exactamente el grano que la regla necesita, sin afectar a las demás empresas:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Company c where c.id = :id")
Company lockForBooking(@Param("id") UUID id);
```

Secuencia dentro de una única `@Transactional`:

```text
1. lockForBooking(companyId)          -- SELECT ... FOR UPDATE sobre company
2. validar anticipación mínima        -- platform_settings.min_booking_notice_minutes
3. validar contra business_hours / schedule_exception
4. validar superposición por rango    -- contra appointment del mismo company_id
5. INSERT appointment                 -- la constraint de exclusión actúa como última barrera
6. COMMIT                             -- recién acá se libera el lock
```

La notificación se emite **después del commit** (`@TransactionalEventListener(AFTER_COMMIT)`): una llamada HTTP a la API de WhatsApp dentro de la transacción mantendría el lock tomado durante toda la latencia de red, serializando el negocio entero contra un servicio externo.

---

## 4. Aislamiento multi-tenant

Es la preocupación más crítica del sistema y es transversal a todos los módulos. Se sostiene en tres capas:

1. **JWT** — el token del cliente lleva el `companyId` fijo de su empresa (ver [7.7](../negocio/detalles-flujos.md)).
2. **Repository** — toda consulta que devuelva datos de una empresa recibe el `companyId` como parámetro obligatorio en la firma del método: `findByIdAndCompanyId(id, companyId)`, nunca `findById(id)`. Es más verboso que un filtro implícito, pero es imposible de olvidar sin que el código deje de compilar.
3. **Base de datos** — las FK compuestas de `appointment` (§2.8) impiden que un registro mezcle empresas aunque las dos capas anteriores fallen.

Sin la capa 2, el ataque es trivial: un cliente de la empresa A pide `GET /api/appointments/{id}` con el ID de un turno de la empresa B y lo ve completo.

---

## 5. DBML para dbdiagram.io

```dbml
Enum company_status { ACTIVE INACTIVE }
Enum company_category {
  BARBERSHOP HAIR_SALON BEAUTY_CENTER MEDICAL_OFFICE DENTAL_OFFICE VETERINARY
  NUTRITION PSYCHOLOGY PHYSIOTHERAPY TATTOO_STUDIO SPA OTHER
}
Enum user_role { SUPERADMIN COMPANY_ADMIN CLIENT }
Enum user_status { PENDING_ACTIVATION ACTIVE INACTIVE }
Enum token_type { ACTIVATION PASSWORD_RESET }
Enum service_status { ACTIVE INACTIVE }
Enum day_of_week { MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY SUNDAY }
Enum appointment_status { PENDING COMPLETED CANCELLED }
Enum appointment_origin { ONLINE MANUAL }
Enum cancelled_by { CLIENT COMPANY }
Enum notification_event {
  BOOKING_CONFIRMED APPOINTMENT_CANCELLED APPOINTMENT_RESCHEDULED
  ACCOUNT_ACTIVATION PASSWORD_RESET
}
Enum notification_channel { WHATSAPP EMAIL }
Enum notification_status { PENDING SENT FAILED }

Table platform_settings {
  id                          smallint    [primary key, default: 1, note: 'CHECK (id = 1): fila única']
  min_booking_notice_minutes  integer     [not null, default: 30]
  cancellation_deadline_hours integer     [not null, default: 3]
  max_reschedule_count        integer     [not null, default: 2]
  max_booking_advance_days   integer     [not null, note: 'Valor a confirmar']
  updated_at                  timestamptz [not null]
  Note: 'Parámetros de negocio globales, editables por el SUPERADMIN.'
}

Table company {
  id             uuid              [primary key, default: `gen_random_uuid()`]
  name           varchar           [not null]
  slug           varchar           [not null, unique, note: 'plataforma.com/{slug}. No editable. Valida contra slugs reservados.']
  description    text
  address        varchar
  phone          varchar
  contact_email  varchar
  category       company_category  [not null, note: 'Enum: alimenta el gráfico de rubros del superadmin']
  timezone       varchar           [not null, default: 'America/Argentina/Cordoba', note: 'IANA. Interpreta las horas de pared de business_hours.']
  logo_url       varchar
  primary_color  varchar           [note: 'CHECK formato #RRGGBB']
  status         company_status    [not null, note: 'Borrado lógico']
  created_at     timestamptz       [not null, note: 'Insumo del dashboard: altas por mes']
  updated_at     timestamptz       [not null]

  indexes { (status, created_at) }
}

Table app_user {
  id          uuid        [primary key, default: `gen_random_uuid()`]
  first_name  varchar     [not null]
  last_name   varchar     [not null]
  email       varchar     [not null, note: 'Único por empresa (índice parcial sobre lower(email)); único global para SUPERADMIN']
  phone       varchar     [not null, note: 'Usado por NotificationService para WhatsApp']
  password    varchar     [note: 'Hash. NULL mientras status = PENDING_ACTIVATION']
  role        user_role   [not null]
  company_id  uuid        [note: 'CHECK: NULL si y solo si role = SUPERADMIN']
  status      user_status [not null]
  created_at  timestamptz [not null]
  updated_at  timestamptz [not null]

  indexes {
    (id, company_id) [unique, name: 'uq_user_id_company']
  }
}

Table user_token {
  id          uuid        [primary key, default: `gen_random_uuid()`]
  user_id     uuid        [not null]
  token_hash  varchar     [not null, unique, note: 'Hash del token, nunca el valor en claro']
  type        token_type  [not null]
  expires_at  timestamptz [not null, note: '48hs activación / 1h reset']
  used_at     timestamptz [note: 'NULL = sin usar. Garantiza el uso único.']
  created_at  timestamptz [not null]
}

Table service {
  id                uuid           [primary key, default: `gen_random_uuid()`]
  company_id        uuid           [not null]
  name              varchar        [not null]
  description       text
  price             "numeric(10,2)" [not null]
  duration_minutes  integer        [not null, note: 'Valor vigente; el turno guarda su propia copia']
  status            service_status [not null, note: 'Desactivar no afecta turnos ya reservados']
  created_at        timestamptz    [not null]
  updated_at        timestamptz    [not null]

  indexes {
    (company_id, status)
    (id, company_id) [unique, name: 'uq_service_id_company']
  }
}

Table business_hours {
  id                uuid        [primary key, default: `gen_random_uuid()`]
  company_id        uuid        [not null]
  day_of_week       day_of_week [not null]
  start_time        time        [not null, note: 'Hora de pared en company.timezone']
  end_time          time        [not null, note: 'CHECK > start_time']
  interval_minutes  integer     [not null]
  created_at        timestamptz [not null]
  updated_at        timestamptz [not null]

  Note: 'Varias filas por día = jornada partida. EXCLUDE impide franjas superpuestas.'
  indexes { (company_id, day_of_week) }
}

Table schedule_exception {
  id                uuid        [primary key, default: `gen_random_uuid()`]
  company_id        uuid        [not null]
  exception_date    date        [not null]
  is_closed         boolean     [not null, default: true]
  start_time        time        [note: 'Solo si is_closed = false']
  end_time          time        [note: 'Solo si is_closed = false']
  interval_minutes  integer     [note: 'Solo si is_closed = false']
  reason            varchar
  created_at        timestamptz [not null]
  updated_at        timestamptz [not null]

  Note: 'Feriados, vacaciones y jornadas con horario especial. Reemplaza por completo al business_hours de esa fecha.'
  indexes {
    (company_id, exception_date) [unique]
  }
}

Table appointment {
  id                        uuid               [primary key, default: `gen_random_uuid()`]
  company_id                uuid               [not null]
  service_id                uuid               [not null, note: 'FK compuesta con company_id']
  client_id                 uuid               [note: 'FK compuesta con company_id. NULL si es reserva manual sin cuenta.']
  manual_client_name        varchar            [note: 'Solo si client_id es NULL']
  manual_client_phone       varchar            [note: 'Solo si client_id es NULL']
  price_snapshot            "numeric(10,2)"    [not null, note: 'Precio pactado; no cambia si se edita el servicio']
  duration_minutes_snapshot integer            [not null, note: 'Duración pactada; no cambia si se edita el servicio']
  start_date_time           timestamptz        [not null]
  end_date_time             timestamptz        [not null, note: 'start + duración. La escribe el service layer.']
  previous_start_date_time  timestamptz        [note: 'Horario inmediatamente anterior si fue reprogramado']
  reschedule_count          integer            [not null, default: 0, note: 'Tope: platform_settings.max_reschedule_count']
  origin                    appointment_origin [not null]
  status                    appointment_status [not null]
  cancelled_by              cancelled_by
  cancellation_reason       text
  cancelled_at              timestamptz
  created_at                timestamptz        [not null]
  updated_at                timestamptz        [not null]

  Note: 'EXCLUDE USING gist impide superposición de rangos dentro de la misma empresa (status <> CANCELLED).'
  indexes {
    (company_id, status, start_date_time)
    (client_id, start_date_time)
    (service_id)
    (start_date_time)
  }
}

Table notification {
  id              uuid                 [primary key, default: `gen_random_uuid()`]
  appointment_id  uuid                 [note: 'NULL en eventos de cuenta']
  user_id         uuid                 [note: 'NULL si el destinatario es un cliente manual sin cuenta']
  recipient_phone varchar              [not null, note: 'Copiado, no leído por FK: refleja a dónde se envió realmente']
  event_type      notification_event   [not null]
  channel         notification_channel [not null]
  status          notification_status  [not null]
  attempt_count   integer              [not null, default: 0]
  sent_at         timestamptz
  error_message   text
  created_at      timestamptz          [not null]

  indexes { (appointment_id) }
}

// Relaciones
Ref: app_user.company_id            >? company.id
Ref: user_token.user_id             >  app_user.id
Ref: service.company_id             >  company.id
Ref: business_hours.company_id      >  company.id
Ref: schedule_exception.company_id  >  company.id
Ref: appointment.company_id         >  company.id
Ref: appointment.service_id         >  service.id
Ref: appointment.client_id          >? app_user.id
Ref: notification.appointment_id    >? appointment.id
Ref: notification.user_id           >? app_user.id
```

> ℹ️ DBML no expresa FK compuestas, índices parciales ni constraints de exclusión. El diagrama las anota como comentarios; **la fuente de verdad es el DDL de la sección 2.**
