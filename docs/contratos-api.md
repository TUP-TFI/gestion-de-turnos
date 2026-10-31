# Contratos de lo que falta: Pasos 6 a 10

Borrador para acordar entre Nico y Brune. Las decisiones marcadas **D1** a **D7** están al final.

## Reglas comunes

- **Errores:** `{ "code": "...", "message": "..." }`. El front decide por `code` y muestra `message`.
- **Instantes:** ISO 8601 en UTC (`startDateTime`, `starts`). **Fechas:** `AAAA-MM-DD`, fecha local de la empresa.
- **Empresa y usuario:** salen siempre de la sesión o del slug, nunca del body.
- **Acciones:** `POST /{id}/accion`, responden `200` con el recurso actualizado (igual que `/deactivate`).
- **Códigos comunes, no repetidos abajo:** `400 VALIDATION_ERROR`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `403 COMPANY_INACTIVE`.
- **Paginado (D5):** `?page=0&size=20`, responde `{ content, page, size, totalElements, totalPages }`. Es un DTO propio, no el `Page` de Spring.

## Ya en `main`

| Endpoint | Historia |
| :--- | :--- |
| `GET /api/public/companies/{slug}` | US-06.1 |
| `GET /api/public/companies/{slug}/services/{id}/availability?date=` | US-06.2 |
| `POST /api/public/companies/{slug}/register` y `/login` | US-03.5, 03.6 |
| `GET /api/client/appointments?status=` | US-07.6 |

## Paso 7: reserva (back Nico, front Brune)

### `POST /api/client/appointments`

```json
{ "serviceId": "uuid", "startDateTime": "2026-11-02T12:00:00Z" }
```

`201` con `ClientAppointment`.

| Error | Cuándo | Qué hace el front |
| :--- | :--- | :--- |
| `400 BOOKING_TOO_SOON` | Antes de la anticipación mínima, o ya pasó | Muestra el mensaje y vuelve a pedir horarios |
| `400 BOOKING_TOO_FAR` | Después de la anticipación máxima | Igual |
| `404 SERVICE_NOT_FOUND` | Inexistente, inactivo o de otra empresa | Vuelve a cargar la página pública |
| `409 SLOT_UNAVAILABLE` | Ocupado o fuera de la grilla | Muestra el mensaje y vuelve a pedir horarios, sin recargar |

### `ClientAppointment`

Lo que ya devuelve `GET /api/client/appointments`, más los campos de D1:

```json
{
  "id": "uuid",
  "serviceId": "uuid",
  "serviceName": "Corte de pelo",
  "startDateTime": "2026-11-02T12:00:00Z",
  "endDateTime": "2026-11-02T12:30:00Z",
  "status": "PENDING",
  "rescheduleCount": 0,
  "priceSnapshot": 8000.00,
  "durationMinutesSnapshot": 30,
  "canCancel": true,
  "canReschedule": false,
  "rescheduleBlockedReason": "LIMIT_REACHED"
}
```

`rescheduleBlockedReason`: `null`, `DEADLINE_PASSED`, `LIMIT_REACHED`, `SERVICE_INACTIVE` o `NOT_PENDING`.

## Paso 8: cancelar y reprogramar (back Brune, front Nico)

### `POST /api/client/appointments/{id}/cancel`

Sin body. `200` con `ClientAppointment`. Queda `cancelledBy = CLIENT`.

| Error | Cuándo |
| :--- | :--- |
| `404 APPOINTMENT_NOT_FOUND` | Inexistente, o de otro cliente u otra empresa |
| `409 APPOINTMENT_NOT_PENDING` | Ya cancelado o completado |
| `400 CANCELLATION_DEADLINE_PASSED` | Pasó `cancellationDeadlineHours` |

### `GET /api/client/appointments/{id}/availability?date=` (D2)

`200` con `{ date, starts }`, igual que la disponibilidad pública, pero sin contar como ocupado al propio turno y con la duración copiada en el turno. Mismos `404` y `409` que el `cancel`.

### `POST /api/client/appointments/{id}/reschedule`

```json
{ "startDateTime": "2026-11-03T15:00:00Z" }
```

`200` con `ClientAppointment`. Mismo `id`, sigue `PENDING`, guarda `previousStartDateTime` y suma `rescheduleCount`.

Errores: los del `cancel`, cambiando el plazo por `400 RESCHEDULE_DEADLINE_PASSED`, más `409 RESCHEDULE_LIMIT_REACHED`, `400 BOOKING_TOO_SOON`, `400 BOOKING_TOO_FAR`, `409 SLOT_UNAVAILABLE` y `409 SERVICE_INACTIVE`.

## Paso 9: turnero (back Brune, front Nico)

### `AdminAppointment`

```json
{
  "id": "uuid",
  "serviceId": "uuid",
  "serviceName": "Corte de pelo",
  "client": { "id": "uuid o null", "name": "Lucía Pérez", "phone": "+54..." },
  "startDateTime": "...",
  "endDateTime": "...",
  "status": "PENDING",
  "origin": "ONLINE",
  "rescheduleCount": 0,
  "previousStartDateTime": null,
  "priceSnapshot": 8000.00,
  "durationMinutesSnapshot": 30,
  "cancelledBy": null,
  "cancellationReason": null,
  "cancelledAt": null
}
```

`client` tiene la misma forma con cuenta y sin cuenta: en un turno manual sin cuenta, `id` es `null`.

### Endpoints

| Endpoint | Respuesta | Errores propios | Historia |
| :--- | :--- | :--- | :--- |
| `GET /api/company/appointments/summary` | `{ today: [], upcoming: [] }`, sin cancelados (D4) | — | US-09.1 |
| `GET /api/company/appointments?status=&origin=&rescheduled=&from=&to=&page=&size=` | Paginado de `AdminAppointment`. `from` y `to` son fechas locales opcionales | — | US-09.2 |
| `POST /api/company/appointments/{id}/cancel` con `{ "reason": "opcional" }` | `200` con `AdminAppointment`, `cancelledBy = COMPANY` | `404 APPOINTMENT_NOT_FOUND`, `409 APPOINTMENT_NOT_PENDING` | US-09.3 |
| `GET /api/company/clients?q=&page=&size=` | Paginado de `{ id, firstName, lastName, email, phone }`. Sin `q`, todos | — | US-09.4, 09.7 |
| `GET /api/company/services/{id}/availability?date=` (D3) | `{ date, starts }` | `404 SERVICE_NOT_FOUND` | US-09.5, 09.6 |
| `POST /api/company/appointments` | `201` con `AdminAppointment`, `origin = MANUAL` | Los de la reserva, más `404 CLIENT_NOT_FOUND` y `400 INVALID_CLIENT_IDENTITY` | US-09.5, 09.6 |

Body de la carga manual, con una sola de las dos identidades:

```json
{ "serviceId": "uuid", "startDateTime": "...", "clientId": "uuid" }
{ "serviceId": "uuid", "startDateTime": "...", "manualClientName": "Ana Gil", "manualClientPhone": "+54..." }
```

### Zona horaria en el panel (D6)

El panel tiene que mostrar los turnos en hora local, y hoy no conoce la zona de la empresa. Propuesta: sumar `timezone` a `GET /api/company/branding`.

## Paso 10: notificaciones (back Nico y Brune)

No agrega endpoints. Es un contrato interno entre la integración de WhatsApp (US-10.1 a 10.3, Nico) y el reintento (US-10.4, Brune). Parte del diseño de #73: la fila `PENDING` se escribe en la transacción del turno y el envío va después del commit.

### Publicar un aviso

Dentro de la transacción del cambio, quien cambia el turno publica `NotificationRequestedEvent(appointmentId, userId, recipientPhone, eventType, message)`. Sin transacción, el listener falla (`MANDATORY`). Sin teléfono, no se publica.

| Evento | Quién lo publica | Historia |
| :--- | :--- | :--- |
| `BOOKING_CONFIRMED` | Reserva online (#73) y carga manual | US-07.5, 09.5, 09.6 |
| `APPOINTMENT_CANCELLED` | Cascada (#73), cancelación del cliente y del admin | US-04.4, 08.1, 09.3 |
| `APPOINTMENT_RESCHEDULED` | Reprogramación, con la fecha nueva en el mensaje | US-08.2 |

En un turno manual sin cuenta, `userId` es `null` y el teléfono es `manualClientPhone`.

### Enviar

`NotificationService.dispatch(phone, message)` es el único punto de envío. Lanza una excepción si el proveedor falla. Lo implementa Nico, y el reintento lo usa sin conocer al proveedor. **La firma puede cambiar:** WhatsApp Business suele exigir plantillas aprobadas para los mensajes que inicia el negocio. Nico la confirma al elegir el proveedor y avisa antes de cambiarla.

### Reintentar

- **Toma:** filas `PENDING` o `FAILED`, con `attemptCount` menor al tope. Las `PENDING` recién creadas se saltean, para no competir con el envío después del commit.
- **Cada intento:** suma `attemptCount`. Si sale bien, queda `SENT` con `sentAt`. Si falla, queda `FAILED` con `errorMessage`.
- **Nunca reenvía** una fila `SENT`.
- **En el tope:** queda `FAILED` y no se reintenta más.

## Contratos internos

### Front: página pública y reserva

| Pieza | Dueño | Contrato |
| :--- | :--- | :--- |
| `usePublicCompany(slug)` y `api/publicCompany.ts` | Nico | `{ company, services, error, isLoading }`. También lo usan el login y el registro para el nombre de la empresa y el slug inválido |
| `ServiceSlotPicker` | Nico | Props: `slug`, `services`, `timezone`, `onSelect({ service, startDateTime })`. Se reutiliza al reprogramar y en la carga manual, con la fuente de horarios por prop |
| Página `/:slug` | Nico | Renderiza el picker y le pasa el `onSelect` que da Brune |
| `useBookingFlow(slug)` | Brune | Recibe la selección, pide login si hace falta, abre el modal y reserva |
| Volver del login | Brune | `/:slug/login?serviceId=...&start=...`. Los mismos parámetros en `/:slug`, y ahí se reabre el modal |
| `formatInCompanyZone(instant, timezone)` en `lib/` | Nico | Lo usan todas las pantallas con horarios |
| `request(path, { auth: 'client' })` (#72) | Brune | Lo necesita antes "Mis turnos" o la reserva, y después el Paso 8 |
| Pantalla "Mis turnos" | Brune | Ruta `/:slug/my-appointments`. Nico le suma las acciones del Paso 8 |

### Back: lo que Brune reutiliza del Paso 7

- **Reserva:** la secuencia de lock, validaciones e insert queda en un método interno de `AppointmentService` que también usa la carga manual. Cambian `origin` y la identidad del cliente.
- **Notificaciones:** cancelar y reprogramar publican el mismo evento de notificación que define el PR 7, y `notification` lo escucha después del commit.
- **Disponibilidad:** se agrega una variante de `availableStarts` que recibe la duración y el turno a excluir. El método actual sigue igual para los demás usos.

## Decisiones

| # | Tema | Problema | Propuesta |
| :--- | :--- | :--- | :--- |
| D1 | Botones de "Mis turnos" | `cancellationDeadlineHours` y `maxRescheduleCount` solo se leen en `/api/superadmin/settings`. El front del cliente no los conoce, y no se pueden hardcodear | El back calcula `canCancel`, `canReschedule` y `rescheduleBlockedReason`. Cambia la tarea técnica de US-08.3 |
| D2 | Disponibilidad al reprogramar | `availableStarts` cuenta el propio turno como ocupado: no se puede correr 30 minutos un turno de 60. Además usa la duración actual del servicio, no la copiada, y rechaza servicios inactivos | Endpoint propio que excluye el turno y usa `durationMinutesSnapshot`. Un servicio inactivo no se puede reprogramar |
| D3 | Disponibilidad en el panel | El admin podría usar la ruta pública, pero tendría que conseguir su slug | Ruta propia bajo `/api/company`, con la empresa de la sesión |
| D4 | "Próximos turnos" | `alcance-mvp.md` lo deja sin definir | `upcoming`: los próximos 5 turnos después de hoy, en hora local de la empresa |
| D5 | Paginado | US-09.7 lo pide para clientes. US-09.2 no lo menciona, pero la agenda crece sin límite | Paginar agenda y clientes con el mismo DTO |
| D6 | Zona en el panel | Falta el dato | `timezone` en `GET /api/company/branding` |
| D7 | Datos del reintento | La fila no guarda el mensaje, y `notification` no puede leer el turno para rearmarlo sin dejar de ser hoja del grafo. US-10.4 tampoco define el tope ni cuándo una `PENDING` se considera abandonada | Migración `V2` con una columna `message` en `notification`. Tope de 5 intentos. Una `PENDING` se reintenta a los 5 minutos de creada. Si el proveedor exige plantillas, la columna guarda los parámetros |
