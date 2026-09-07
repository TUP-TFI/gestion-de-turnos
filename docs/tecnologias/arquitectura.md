# 🛠️ 2. Tecnologías y Arquitectura

A continuación se detallan las herramientas, marcos de trabajo e infraestructura seleccionados para el desarrollo del proyecto, junto con la justificación técnica de cada elección:

## 📋 Matriz de Stack Tecnológico

| Capa | Tecnología | Motivo / Justificación Técnica |
| :--- | :--- | :--- |
| **Backend** | **Java + Spring Boot** | Stack ya utilizado en la cursada; ecosistema robusto y maduro para autenticación (`Spring Security` + `JWT`), manejo de transacciones y validaciones de negocio. |
| **Frontend** | **React + TypeScript (Vite)** | Arquitectura basada en componentes reutilizables (ideal para las páginas públicas de cada empresa); tipado fuerte para modelar las entidades del dominio de forma segura. |
| **Base de Datos** | **PostgreSQL** *(ej. Neon)* | Motor relacional idóneo por las relaciones estrictas entre empresas, servicios, usuarios y turnos. Aporta además tres cosas que el modelo usa explícitamente: índices únicos parciales, constraints `CHECK` y constraints de **exclusión** sobre rangos temporales. |
| **ORM** | **Spring Data JPA / Hibernate** | Abstracción para el manejo de entidades y soporte de *locking* pesimista para evitar carreras de condición (*race conditions*) en reservas concurrentes. |
| **Migraciones** | **Flyway** | El esquema es la fuente de verdad y se versiona en SQL (`V<n>__<descripcion>.sql`). Ver justificación abajo. |
| **Autenticación** | **JWT (Spring Security)** | Dos JWT *stateless* independientes: uno para el panel de sistema (`SUPERADMIN` / `COMPANY_ADMIN`) y uno por sesión de cliente, atado a una `companyId` puntual. |
| **Notificaciones** | **NotificationService** *(Desacoplado)* | Canal objetivo: WhatsApp (proveedor a definir). Si no se llega a implementar a tiempo, se reemplaza por mail — es una decisión de proyecto, no un fallback en tiempo real. Cada envío se persiste con su estado para permitir reintentos idempotentes. |
| **Deploy Backend** | **Render** *(Free Tier)* | Plataforma de despliegue gratuita adecuada para el alcance de la tesis. |
| **Deploy Frontend** | **Vercel** | Despliegue continuo de alto rendimiento y sin complicaciones de configuración para la escala del proyecto. |
| **CI/CD** | **GitHub Actions** | Automatización de flujos de trabajo para la ejecución de tests automáticos en cada *Pull Request*. |

---

## 🗄️ Migraciones: por qué Flyway y no `ddl-auto`

`spring.jpa.hibernate.ddl-auto` queda en **`validate`**: Hibernate verifica que el esquema coincida con las entidades, pero no lo modifica. El esquema lo definen las migraciones versionadas en `src/main/resources/db/migration`.

El motivo no es de estilo. Buena parte de las garantías del modelo de datos **Hibernate no las puede generar**:

| Garantía | Dónde vive |
| :--- | :--- |
| Índices únicos **parciales** (unicidad de email por empresa vs. global para superadmin) | Solo SQL |
| Constraints `CHECK` de coherencia (turno contactable, cancelación completa, rol vs. `companyId`) | Solo SQL |
| Constraint de **exclusión** contra superposición de turnos (`EXCLUDE USING gist`) | Solo SQL |
| **FK compuestas** que garantizan el aislamiento entre empresas | Solo SQL |
| Extensiones (`pgcrypto`, `btree_gist`) | Solo SQL |

Con `ddl-auto: update` esas constraints simplemente no existirían en la base, y el modelo perdería justamente lo que lo hace robusto. El DDL completo está en [esquema-bd.md](esquema-bd.md).

---

## 🔍 Detalle por Componente del Sistema

### ⚙️ Backend & Persistencia
* **Framework:** Java + Spring Boot
* **ORM:** Spring Data JPA / Hibernate
* **Base de Datos:** PostgreSQL
* **Migraciones:** Flyway (`ddl-auto: validate`)

#### Estrategia de concurrencia

Dos barreras, no una:

```text
1ª barrera — Lock pesimista (capa de servicio)
   SELECT ... FOR UPDATE sobre la fila de `company`, dentro de la
   transacción que valida y guarda el turno. Serializa las reservas
   de ese negocio (y solo de ese negocio).
   → Produce el mensaje de negocio: "ese horario ya fue tomado".

2ª barrera — Constraint de exclusión (motor)
   EXCLUDE USING gist sobre (company_id, rango horario) en appointment.
   Impide físicamente dos turnos superpuestos en la misma empresa.
   → Actúa aunque el código falle: un job, una migración, un INSERT manual.
```

Se bloquea la fila de `company` porque el turno a crear todavía no existe y no hay fila propia que bloquear. La violación de la constraint llega a la aplicación como `ConstraintViolationException` y se mapea a **HTTP 409** en el manejador centralizado de errores.

### 🎨 Frontend & Interfaz de Usuario
* **Librería/Framework:** React
* **Lenguaje:** TypeScript
* **Bundler/Tooling:** Vite
* **Enfoque de UI:** Componentes modulares reutilizados entre el panel administrativo y las *landings* públicas por empresa.

### 🔐 Seguridad & Control de Acceso
* **Esquema:** JSON Web Tokens (JWT) vía Spring Security
* **Roles del Sistema:**
  1. `SUPERADMIN` — Gestión global de empresas y plataforma.
  2. `COMPANY_ADMIN` — Administración de agenda, precios, servicios y horarios propios.
  3. `CLIENT` — Consulta de disponibilidad y reserva/gestión de citas.

#### Aislamiento multi-tenant

Es la preocupación de seguridad más crítica del sistema, y es **transversal a todos los módulos**. Se sostiene en tres capas independientes:

```text
1. JWT           el token lleva el companyId de la sesión (fijo, no elegible por el cliente)
2. Repository    toda consulta de datos de empresa recibe companyId como parámetro
                 obligatorio: findByIdAndCompanyId(id, companyId), nunca findById(id)
3. Base de datos las FK compuestas de `appointment` impiden que un registro
                 mezcle entidades de dos empresas distintas
```

Sin la capa 2 el ataque es trivial: un cliente de la empresa A pide `GET /api/appointments/{id}` con el ID de un turno de la empresa B y lo ve completo. La capa 2 es más verbosa que un filtro implícito, pero es imposible de olvidar sin que el código deje de compilar — que es exactamente la propiedad que se busca.

Detalle completo en [esquema-bd.md §4](esquema-bd.md#4-aislamiento-multi-tenant).

#### Manejo de secretos

* Contraseñas: hash (nunca en texto plano).
* Tokens de activación y recuperación: se persiste **el hash**, no el valor en claro. El valor en claro existe una sola vez, al armar el link.

### 🔔 Servicio de Notificaciones

```text
[ Evento: Reserva / Cancelación / Reprogramación / Activación ]
               │
               │  (después del COMMIT de la transacción del turno)
               ▼
   NotificationService (desacoplado del canal)
               │
               ├──► INSERT en `notification` (status = PENDING)
               │
               ▼
          [ WhatsApp ]  ──► SENT  (sent_at)
        (canal objetivo)  └─► FAILED (error_message, attempt_count++)
                                │
                                └──► reintento idempotente
```

Se emite **después** del commit: una llamada HTTP a un servicio externo dentro de la transacción mantendría tomado el lock de la empresa durante toda la latencia de red, serializando las reservas del negocio detrás del proveedor.

Persistir cada envío es lo que hace posible reintentar sin duplicar — crítico en la cancelación masiva que dispara la desactivación de una empresa.

### ⏰ Trabajos programados

| Job | Qué hace | Frecuencia orientativa |
| :--- | :--- | :--- |
| Cierre de turnos | Pasa a `COMPLETED` los turnos `PENDING` cuyo `endDateTime` ya pasó | cada 15 min |
| Reintento de notificaciones | Reprocesa las `notification` en `PENDING`/`FAILED` | cada 5 min |
| Purga de tokens | Elimina los `user_token` vencidos y ya usados | diaria |

### 🚀 Infraestructura & Integración Continua (DevOps)

* **Control de Versiones & CI/CD:** GitHub + GitHub Actions *(Ejecución automática de suites de prueba en PRs)*.
* **Hosting Frontend:** Vercel
* **Hosting Backend:** Render

> ⚠️ **Zona horaria del contenedor:** Render corre en UTC. El sistema no depende de eso — toda conversión entre horas de pared y instantes usa `company.timezone` — pero conviene fijar explícitamente `TZ=UTC` y `-Duser.timezone=UTC` para que ningún `LocalDate.now()` accidental introduzca la zona del servidor en un cálculo.
