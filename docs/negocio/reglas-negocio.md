## 4. Reglas de negocio

### Autenticación y cuentas
1. Existen dos sistemas de login separados: uno para superadmin y admins de empresa (panel de sistema), y uno propio por cada empresa para sus clientes (dentro de plataforma.com/{slug}). No comparten pantalla ni JWT entre sí.
2. Una cuenta de cliente pertenece a una única empresa. Un mismo email puede repetirse en distintas empresas (son cuentas independientes); lo que no puede repetirse es el email dentro de una misma empresa. La comparación es **case-insensitive**: `Juan@mail.com` y `juan@mail.com` son el mismo email.
3. La unicidad del email dentro de una empresa **no distingue el rol**. En consecuencia, el admin de una empresa no puede tener además una cuenta de cliente en su propia empresa con ese mismo email; si quiere reservarse un turno, lo carga como reserva manual desde su turnero.
4. La página pública de cada empresa es visible sin login (consulta de servicios y horarios), pero reservar un turno requiere estar logueado como cliente de esa empresa puntual.
5. El JWT no tiene refresh token y expira a las 24 horas; pasado ese tiempo el usuario (de cualquiera de los dos sistemas de login) debe volver a loguearse.
6. Los links de activación de cuenta y de recuperación de contraseña son **de un solo uso** y tienen vencimiento (48 hs para activación, 1 hora para recuperación). Del token se persiste únicamente su hash, nunca el valor en claro. Reenviar un link de activación invalida los anteriores de ese usuario.

### Empresas

7. Dar de baja una empresa significa desactivarla, nunca eliminar el registro. Al desactivar: se bloquea el login de su admin, la página pública muestra un estado de "no disponible", los turnos pendientes se cancelan automáticamente notificando a cada cliente afectado, y el historial se conserva por si se reactiva.
8. El bloqueo del login del admin al desactivar la empresa **se deriva de `company.status`** en el filtro de autenticación; no se modifica el `status` del usuario. De lo contrario, un admin que ya estaba inactivo antes de la baja quedaría reactivado por error al reactivar la empresa.
9. Reactivar una empresa es lo inverso: vuelve a estado `ACTIVE`, se desbloquea el login del admin (sin necesitar un nuevo link de activación si ya tenía contraseña definida) y la página pública vuelve a aceptar reservas con la configuración de servicios/horarios que ya tenía cargada. Los turnos cancelados durante la baja quedan cancelados — no se recrean automáticamente.
10. Cada empresa tiene una **zona horaria** propia (`timezone`, formato IANA). Toda conversión entre las horas de pared de la configuración de agenda y los instantes UTC de los turnos usa esa zona, nunca la zona horaria del servidor.
11. El `slug` de la URL pública no es editable y no puede coincidir con una ruta propia de la aplicación (`api`, `login`, `superadmin`, etc.): el alta valida contra una lista de slugs reservados.

### Parámetros configurables

12. Tres parámetros de negocio son **globales de la plataforma** y los edita el superadmin desde su panel (no viven en el archivo de configuración, para no requerir un redeploy al ajustarlos):

| Parámetro | Valor inicial | Qué controla |
| :--- | :--- | :--- |
| Anticipación mínima para reservar | 30 minutos | Regla 14 |
| Plazo máximo de cancelación/reprogramación | 3 horas | Regla 16 |
| Máximo de reprogramaciones por turno | 2 | Regla 18 |

### Disponibilidad y concurrencia
13. Un turno reservado no puede superponerse con otro del mismo negocio en la misma franja horaria (validado por rango, no por horario exacto, dado que los servicios tienen duración variable).
14. La validación de superposición y el guardado del turno ocurren dentro de una misma transacción con lock pesimista. La fila que se bloquea es la de la **empresa** (`SELECT ... FOR UPDATE` sobre `company`), porque el turno a crear todavía no existe y no hay fila propia que bloquear; eso serializa las reservas de ese negocio sin afectar a los demás. Como respaldo, la base declara una constraint de exclusión que impide la superposición aunque el código falle.
15. No se puede reservar un turno con menos de la anticipación mínima configurada sobre el horario elegido, ni en horarios ya pasados.

### Cancelación y reprogramación
16. El cliente puede cancelar o reprogramar su turno hasta el plazo configurado antes del mismo. Pasado ese plazo, debe contactar al negocio por fuera del sistema; el admin puede cancelar ese turno desde su turnero sin restricción horaria, liberando el horario para otra persona.
17. El admin de empresa puede cancelar cualquier turno de su negocio sin restricción horaria, dejando registrado quién lo canceló (`CLIENT` o `COMPANY`), cuándo, y un motivo en texto libre opcional.
18. Reprogramar un turno actualiza el mismo registro (nueva fecha/hora, se conserva la fecha anterior, se incrementa el contador de reprogramaciones), en vez de crear un turno nuevo y cancelar el anterior. **El turno sigue en estado `PENDING`**: reprogramar no es un estado del ciclo de vida, es un evento que se contabiliza. Alcanzado el máximo configurado de reprogramaciones, el botón queda deshabilitado y el cliente debe contactar al negocio.
19. Un turno cancelado no puede reactivarse desde "Mis turnos" del cliente; si quiere el mismo horario debe reservarlo de nuevo (sujeto a disponibilidad).

### Turnos manuales (sin cuenta)
20. Al cargar un turno manualmente, el admin indica si el cliente tiene cuenta en la plataforma. Si tiene, lo busca y selecciona desde un desplegable, y el turno queda vinculado a su `clientId` — después puede loguearse y autogestionarlo desde "Mis turnos" como cualquier otro. Si no tiene cuenta, el admin carga sus datos sueltos (nombre, teléfono) sin `clientId`; en ese caso, para cancelarlo debe contactar al negocio, y es el admin quien lo cancela desde el turnero.
21. Todo turno debe ser contactable: o está vinculado a una cuenta, o trae nombre y teléfono cargados. Nunca ninguna de las dos cosas.
22. Todo turno registra su **origen** (`ONLINE` o `MANUAL`), porque una reserva manual vinculada a un cliente con cuenta es de otro modo indistinguible de una reserva hecha por el propio cliente.

### Servicios y horarios
23. Desactivar un servicio con turnos futuros asociados no cancela esos turnos: dejan de ofrecerse a nuevas reservas, pero los ya reservados se mantienen y se atienden con normalidad. Cancelarlos, si el admin lo decide, es una acción manual aparte.
24. El turno guarda una **copia del precio y la duración** del servicio al momento de reservarse. Editar un servicio afecta solo a las reservas futuras: sin esa copia, cambiar la duración de un servicio modificaría retroactivamente todos los turnos ya reservados y los haría superponerse entre sí.
25. La configuración de horarios admite **varias franjas por día** (jornada partida, ej. 09:00–13:00 y 16:00–20:00), siempre que no se superpongan entre sí. Un día sin ninguna franja cargada significa que la empresa no atiende ese día.
26. Además de la plantilla semanal, cada admin puede cargar **excepciones por fecha concreta** (feriados, vacaciones, jornadas con horario especial). Una excepción **reemplaza por completo** la configuración semanal de esa fecha: no se combinan.
27. Editar la configuración de horarios, o cargar una excepción, no afecta a los turnos ya reservados que queden fuera del nuevo rango: se mantienen igual, y el sistema solo avisa al admin de cuántos turnos quedan en esa situación.

### Notificaciones
28. Se envía notificación por WhatsApp cuando: se confirma una reserva (online o manual), se cancela un turno (por cliente o negocio), o se reprograma un turno. La notificación es informativa — el turno ya queda confirmado en el sistema en el momento de guardarse, no depende de que el cliente confirme el aviso.
29. Cada notificación se persiste con su estado (`PENDING`, `SENT`, `FAILED`) y su cantidad de intentos. Sin ese registro, un timeout del proveedor externo dejaría al sistema sin saber si el mensaje salió, sin poder reintentar y sin poder evitar un envío duplicado — algo crítico en la cancelación masiva de la regla 7.
30. La notificación se emite **después** de confirmada la transacción del turno, nunca dentro de ella: una llamada HTTP a un servicio externo dentro de la transacción mantendría tomado el lock de la empresa durante toda la latencia de red.

### Alcance
31. No se maneja dinero dentro del sistema (sin señas ni pagos online): se descartó explícitamente por el riesgo de reintroducir conflictos de doble reserva mientras se espera confirmación de un pago externo.
32. Fechas/horas de los turnos se almacenan en UTC (`Instant`/`timestamptz` en el backend); la conversión a hora local se hace en el frontend, y en el backend siempre contra `company.timezone`.
33. No se manejan múltiples empleados/profesionales por empresa en esta versión: la disponibilidad es a nivel de negocio, no de persona.

### Historial
34. Un job programado pasa automáticamente los turnos `PENDING` a `COMPLETED` una vez que su `endDateTime` ya pasó — el corte es la hora de fin y no la de inicio, porque un turno de 90 minutos no está cumplido a los 5 minutos de haber empezado. `CANCELLED` es un estado terminal y nunca pasa a `COMPLETED`. "Mis turnos" no tiene una sección de historial aparte: lista todo, ordenable/filtrable por estado, y los turnos `COMPLETED` cumplen esa función. Los turnos que fueron reprogramados siguen identificables después de completarse, porque el dato vive en un contador y no en el estado.
