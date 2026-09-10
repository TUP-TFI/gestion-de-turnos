## 6. Casos de uso

### UC1 - Alta de empresa
**Actor:** Superadmin

El superadmin completa datos de la empresa y del futuro admin. El sistema crea `Company` + `User` admin (estado `PENDING_ACTIVATION`) en una sola transacción, genera el slug de la URL pública —validándolo contra la lista de slugs reservados— y envía un link de activación de un solo uso para que el admin defina su contraseña. La URL pública queda disponible desde el primer momento, mostrando un estado de "todavía sin servicios cargados".

### UC2 - Configurar servicios y horarios
**Actor:** Admin de empresa

El admin crea/edita servicios (nombre, precio, duración) y define, una sola vez, su horario habitual por día de semana + intervalo, pudiendo cargar más de una franja por día si trabaja con jornada partida. El sistema usa esos datos para calcular disponibilidad real en cada reserva. Si más adelante edita el horario y hay turnos ya reservados fuera del nuevo rango, el sistema le avisa pero no los toca.

### UC3 - Registro/login de cliente
**Actor:** Cliente

Desde la página pública de una empresa puntual, una persona se registra o inicia sesión con email y contraseña. Esa cuenta solo sirve para reservar turnos en esa empresa; si quiere sacar turno en otra empresa de la plataforma, se registra de nuevo ahí (aunque use el mismo email, es una cuenta distinta).

### UC4 - Reservar un turno
**Actor:** Cliente

El cliente entra a la página pública de una empresa, elige un servicio, ve los horarios disponibles (ya filtrados por duración, por excepciones de calendario y por la anticipación mínima permitida), confirma la reserva y el turno queda guardado como `PENDING` de inmediato, con el precio y la duración del servicio copiados en el propio turno. Recibe una notificación informativa de confirmación; no necesita confirmar nada más. El turno queda visible en "Mis turnos".

### UC5 - Cancelar o reprogramar un turno propio
**Actor:** Cliente

Desde "Mis turnos", dentro del plazo permitido, el cliente cancela (el turno queda marcado como cancelado, visible en gris, sin reactivación posible) o reprograma. Al reprogramar se actualiza el mismo turno con la nueva fecha, conservando la anterior e incrementando su contador de reprogramaciones; el turno sigue en estado `PENDING`. Si ya alcanzó el máximo configurado de reprogramaciones, el botón queda deshabilitado y debe contactar al negocio.

### UC6 - El negocio cancela un turno
**Actor:** Admin de empresa

Desde el turnero de su panel, el admin cancela un turno (reservado online o cargado manualmente) sin restricción horaria, registrando el motivo. El sistema deja asentado quién canceló y cuándo. El cliente recibe notificación del cambio, si tiene forma de recibirla.

### UC7 - Desactivar una empresa
**Actor:** Superadmin

El superadmin desactiva una empresa. Se bloquea el login de su admin, la página pública deja de ofrecer reservas, y los turnos pendientes se cancelan automáticamente notificando a cada cliente afectado. Cada notificación queda registrada con su estado, de modo que si el proceso se interrumpe se puede reintentar solo sobre las que fallaron, sin volver a avisarle a quien ya recibió el mensaje.

### UC8 - Reservar un turno manualmente
**Actor:** Admin de empresa

El admin usa el mismo turnero para cargar un turno manual. Si el cliente tiene cuenta en la plataforma, lo busca y selecciona desde el listado de clientes de su empresa, vinculando el turno a su `clientId` — después puede autogestionarlo desde "Mis turnos" como cualquier otro. Si no tiene cuenta, el admin carga sus datos sueltos (nombre, teléfono); en ese caso, para cancelar debe contactar al negocio y es el admin quien lo hace desde el turnero. En ambos casos el turno queda marcado con origen `MANUAL`.

### UC9 - Dar de baja un servicio con turnos pendientes
**Actor:** Admin de empresa

El admin desactiva un servicio. El sistema le muestra cuántos turnos futuros tiene ese servicio antes de confirmar. Al desactivarlo, el servicio deja de ofrecerse para nuevas reservas, pero los turnos ya reservados no se cancelan automáticamente; se atienden con normalidad —conservando el precio y la duración con que fueron reservados— salvo que el admin los cancele a mano, turno por turno.

### UC10 - Editar horarios con turnos ya reservados
**Actor:** Admin de empresa

El admin modifica el rango horario de un día (por ejemplo, deja de atender los sábados). El sistema detecta si hay turnos futuros reservados fuera del nuevo rango y se lo informa antes de guardar, pero no los cancela ni los modifica de forma automática.

### UC11 - Reactivar una empresa
**Actor:** Superadmin

El superadmin reactiva una empresa previamente desactivada. Vuelve a estado `ACTIVE`, se desbloquea el login de su admin y la página pública vuelve a mostrar servicios/horarios y aceptar reservas, con la configuración que ya tenía cargada. Los turnos que se habían cancelado al desactivar no se recuperan.

### UC12 - Cargar una excepción de calendario
**Actor:** Admin de empresa

El admin carga una fecha concreta como excepción a su horario habitual: un feriado o día de vacaciones (cerrado todo el día), o una jornada con horario especial (abre en un rango distinto al de esa plantilla semanal). La fecha debe ser hoy o posterior; no se pueden cargar excepciones para fechas ya pasadas. La excepción reemplaza por completo la configuración de ese día: a partir de ahí el sistema no ofrece ningún horario para esa fecha, o solo los del rango especial. Igual que en UC10, si ya había turnos reservados en esa fecha, el sistema informa cuántos quedan fuera pero no los cancela: el admin decide si los cancela a mano.

### UC13 - Ajustar los parámetros globales de la plataforma
**Actor:** Superadmin

Desde su panel, el superadmin modifica la anticipación mínima para reservar, el plazo máximo de cancelación/reprogramación, el máximo de reprogramaciones por turno, o el máximo de anticipación para reservar. Los nuevos valores rigen de inmediato para todas las empresas, sin necesidad de redeploy. No afectan retroactivamente a los turnos ya reservados: solo a las validaciones de las operaciones que se hagan a partir de ese momento.

### UC14 - Recuperar la contraseña
**Actor:** Admin de empresa / Cliente

Desde la pantalla de login, el usuario solicita recuperar su contraseña. El sistema emite un token de un solo uso con vencimiento y lo envía por el canal de notificaciones. Al abrir el link, define una contraseña nueva y el token queda sellado como usado. Un link vencido o ya utilizado no permite el cambio y hay que solicitar uno nuevo.
