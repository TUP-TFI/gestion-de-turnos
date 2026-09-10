## 3. Requerimientos funcionales

### Superadmin
- Login (panel de sistema).
- CRUD de empresas (alta, consulta, edición, desactivación — nunca eliminación física).
- Alta de empresa: formulario único que crea en la misma transacción la `Company` (estado `ACTIVE`) y el `User` admin asociado (estado `PENDING_ACTIVATION`), generando el slug de la URL pública —validado contra la lista de slugs reservados— y disparando el link de activación por WhatsApp.
- Reenviar el link de activación si el admin no lo usó a tiempo (invalidando el anterior).
- **Configurar los parámetros globales de la plataforma:** anticipación mínima para reservar, plazo máximo de cancelación/reprogramación, máximo de reprogramaciones por turno y máximo de anticipación para reservar. Se editan desde el panel, sin necesidad de redeploy.
- Ver dashboard general de la plataforma (empresas activas/inactivas, altas por mes, ranking de empresas por cantidad de turnos generados, distribución por rubro).
- Ver la página pública de cualquier empresa (botón "Ver página pública"), sin poder reservar con esa sesión.

### Admin de empresa
- Login (panel de sistema).
- Definir su contraseña la primera vez, vía link de activación de un solo uso.
- Recuperar su contraseña vía link con token, disparado desde el propio login.
- Ver dashboard de su propio negocio (turnos de hoy, próximos turnos).
- CRUD de servicios (nombre, descripción, precio, duración, estado activo/inactivo). Al desactivar un servicio con turnos futuros, éstos se mantienen y se atienden con normalidad.
- CRUD de horarios de atención: configuración recurrente por día de la semana (rango horario + intervalo), admitiendo **varias franjas por día** (jornada partida). Se configura una sola vez y es editable cuando haga falta. Al editarla, si hay turnos ya reservados fuera del nuevo rango, el sistema avisa que se mantienen igual (no se cancelan solos).
- **CRUD de excepciones de calendario:** feriados, vacaciones y jornadas con horario especial, cargados por fecha concreta. Una excepción reemplaza por completo la configuración semanal de esa fecha. Igual que al editar horarios, el sistema avisa cuántos turnos quedan fuera pero no los cancela.
- Personalización básica de su página pública (logo, color).
- Ver turnero/agenda de su empresa, filtrable por estado y por origen (reservado online / cargado manualmente).
- **Ver el listado de clientes de su empresa**, con búsqueda por nombre, teléfono o email. Es una vista de solo lectura: alimenta el desplegable de vinculación al cargar un turno manual.
- Reservar un turno manualmente: si el cliente tiene cuenta, vincularlo buscándolo y seleccionándolo desde ese listado; si no tiene cuenta, cargar sus datos sueltos (nombre, teléfono).
- Cancelar cualquier turno de su empresa (reservado online o cargado manualmente), sin restricción horaria, dejando registrado el motivo.
- Ver "Mi página pública" (solo lectura, se arma sola con lo que carga acá).

### Cliente
- Registro/login, dentro de la página pública de una empresa puntual.
- Ver la página pública de una empresa (servicios y disponibilidad) sin necesidad de login.
- Reservar un turno (requiere estar logueado como cliente de esa empresa), respetando la anticipación mínima permitida.
- Ver "Mis turnos" (listado propio de esa empresa), filtrable por estado y distinguiendo los turnos que fueron reprogramados.
- Cancelar un turno propio, dentro del plazo permitido.
- Reprogramar un turno propio, dentro del plazo permitido y hasta el máximo de reprogramaciones configurado.
- Recibir notificación ante confirmación, cancelación o reprogramación de sus turnos.

### Sistema
- Cálculo automático de disponibilidad en el momento (horario + intervalo − duración del servicio − turnos ya ocupados − excepciones de calendario), sin persistir una tabla de slots. (slots = cada horario de inicio candidato que el sistema podría ofrecer).
- Interpretación de todas las horas de pared de la agenda contra la zona horaria de la empresa (`company.timezone`), nunca contra la del servidor.
- Control de superposición de turnos con locking pesimista sobre la fila de la empresa (evita doble reserva concurrente), respaldado por una constraint de exclusión en la base de datos.
- Registro del precio y la duración del servicio en el propio turno al momento de reservar, para que editar el catálogo no altere turnos ya confirmados.
- Validación de anticipación mínima, de plazo máximo de cancelación/reprogramación y de máximo de reprogramaciones, tomando los tres valores de la configuración global.
- Aislamiento multi-tenant: toda consulta de datos de una empresa exige el `companyId` de la sesión, y la base impide por integridad referencial que un turno mezcle entidades de dos empresas distintas.
- Envío de notificaciones automáticas (`NotificationService` desacoplado del canal) ante reserva confirmada, cancelación y reprogramación, emitidas después de confirmada la transacción.
- Registro persistente de cada notificación con su estado y cantidad de intentos, permitiendo reintentar los envíos fallidos sin duplicar los ya enviados.
- Cancelación automática de turnos pendientes y notificación a los clientes afectados cuando una empresa se desactiva.
- Transición automática de turnos a `COMPLETED` una vez pasado su horario, vía job programado (excepto los `CANCELLED`, que quedan como están).
