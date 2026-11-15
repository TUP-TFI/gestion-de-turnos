# gestion-de-turnos

## 1. Resumen del proyecto

Plataforma web donde múltiples empresas de servicios por turno (barberías, centros de estética, consultorios, veterinarias, etc.) gestionan sus servicios, horarios y turnos desde un panel propio, y cada una cuenta con una página pública independiente (plataforma.com/{slug}) donde sus clientes pueden reservar, cancelar o reprogramar turnos.

El cliente final también tiene cuenta y se loguea, pero esto no significa un login único para toda la plataforma: hay dos sistemas de login separados.
- **Panel interno de sistema:** ahí se loguean superadmin y admins de empresa, en una URL propia (no pública, no vinculada a ninguna empresa en particular).
- **Página pública de cada empresa** (plataforma.com/{slug}): ahí se registra/loguea el cliente, de forma independiente por empresa. Si una persona quiere sacar turno en dos negocios distintos de la plataforma, se registra por separado en cada uno — no existe una cuenta de cliente compartida entre empresas.

**Problema que resuelve:** hoy estos negocios coordinan turnos por WhatsApp, llamadas o agenda en papel/Excel, lo que genera turnos duplicados, tiempo perdido respondiendo mensajes uno por uno, y falta de aviso claro ante cambios (cancelaciones, reprogramaciones).

**Valor agregado:** el sistema bloquea automáticamente un horario apenas se reserva (evitando la doble reserva que hoy depende de que el dueño esté atento), y el cliente cancela o reprograma solo dentro del plazo. Para avisar cada cambio, el panel arma el mensaje de WhatsApp listo para enviar con un toque (Click to Chat): el sistema no manda mensajes por su cuenta.

## 2. Estructura del repositorio

```
.
├── backend/      # API (Java + Spring Boot) — ver su README
├── frontend/     # Cliente web (React + TypeScript) — ver su README
└── docs/
    ├── negocio/       # Especificación funcional
    └── tecnologias/   # Arquitectura y decisiones técnicas
```

La documentación está dividida según su ciclo de vida: en `docs/` vive **el qué y el por qué** (negocio, modelo de datos, decisiones transversales), que cambia cuando cambia el producto. En el README de cada proyecto vive **el cómo** (estructura, capas, tests, setup), que cambia cuando cambia el código.

## 3. Documentación

### Proyectos
- [Backend](backend/README.md) — estructura de paquetes, regla de capas, endpoints, testing, seeds.
- [Frontend](frontend/README.md) — estructura de carpetas, regla de capas, testing.

### Plan
- [Alcance del MVP](docs/alcance-mvp.md) — historias implementadas, recortes y lo que quedó fuera del MVP.
- [Contratos de la API](docs/contratos-api.md) — endpoints y decisiones acordadas entre front y back.

### Negocio
- [Requerimientos funcionales](docs/negocio/requerimientos-funcionales.md)
- [Casos de uso](docs/negocio/casos-usos.md)
- [Reglas de negocio](docs/negocio/reglas-negocio.md)
- [Detalle de flujos por rol](docs/negocio/detalles-flujos.md)

### Tecnologías
- [Arquitectura y stack](docs/tecnologias/arquitectura.md)
- [Diccionario de datos](docs/tecnologias/diccionario-datos.md) — modelo lógico: entidades, atributos y reglas de integridad.
- [Esquema de base de datos](docs/tecnologias/esquema-bd.md) — DDL de PostgreSQL, índices, constraints y DBML del diagrama.
- [Convenciones técnicas](docs/tecnologias/convenciones.md)
- [Workflow y herramientas](docs/tecnologias/workflow-y-herramientas.md) — flujo de trabajo, CI, Dependabot, despliegue y uso de IA.

## 4. Stack

Java + Spring Boot (backend), React + TypeScript (frontend), PostgreSQL. Detalle completo y justificación en [arquitectura y stack](docs/tecnologias/arquitectura.md).

## 5. Sistema desplegado

- **Aplicación:** https://gestion-de-turnos-eight.vercel.app (frontend en Vercel). La página pública de una empresa es `/{slug}`, y el panel de superadmin y de admins entra por `/login`.
- **API:** https://gestion-de-turnos-api.onrender.com (backend en Render, base en Neon). `GET /ping` responde `{"status":"ok"}`.

El backend está en el plan gratuito de Render: se duerme después de un rato sin uso, y el primer pedido puede tardar hasta un minuto en responder. No es una caída.

## 6. Cómo correrlo en local

Requisitos: Docker Desktop, JDK 21 y Node 24. El paso a paso completo, con las variables de entorno, está en el [README del backend](backend/README.md) y en el [del frontend](frontend/README.md).

1. **Base de datos** (en `backend/`): `docker compose up -d`.
2. **Backend** (en `backend/`), con los datos de prueba: `APP_SEED_ENABLED=true ./mvnw spring-boot:run`. Queda en `http://localhost:8080`.
3. **Frontend** (en `frontend/`): `npm ci`, copiar `.env.example` a `.env` y `npm run dev`. Queda en `http://localhost:5173`.

El seed carga la empresa `barberia-central` (en `http://localhost:5173/barberia-central`) con sus servicios y horario, y los usuarios `superadmin@example.com`, `admin@example.com`, `cliente1@example.com` y `cliente2@example.com`. La contraseña de desarrollo está en `backend/src/main/resources/application-dev.yml` (`app.seed.password`).

**Pruebas:** `./mvnw verify` en `backend/` corre los tests unitarios y de integración contra un PostgreSQL de Testcontainers. `npm run test:e2e` en `frontend/` corre el recorrido de la reserva con Playwright contra el back local. El CI de cada pull request corre los del backend, y el lint y el build del frontend.

## 7. Estado del proyecto

**MVP implementado.** Están todas las historias de `docs/alcance-mvp.md` hasta el Paso 11 (empresas y superadmin, servicios y horarios, página pública, reserva, cancelación y reprogramación, turnero del admin con carga manual, avisos por WhatsApp y jobs automáticos) y los tests del Paso 12. Falta la etapa final: datos de demo, informe y video.

**Limitaciones conocidas:** el sistema no envía avisos solo (los manda una persona con Click to Chat), no hay recuperación de contraseña, y un turno tiene un solo servicio. Los recortes y su motivo están en el [alcance del MVP](docs/alcance-mvp.md).

## 8. Uso de IA

Usamos Claude Code como asistente de programación. Las reglas están en [`AGENTS.md`](AGENTS.md): el agente propone y el desarrollador decide, y los commits, los pushes y los pull requests los hace siempre una persona. El detalle está en [workflow y herramientas](docs/tecnologias/workflow-y-herramientas.md#5-asistentes-de-ia-claude-code).

## 9. Equipo

**Grupo 140**

- Giorda, Brunella de Lourdes
- Guzmán Olariaga, Facundo Nicolás

## 10. Repositorio

El repositorio vivía en `github.com/NicoGuzmanTUP/gestion-de-turnos` y fue transferido a la organización `TUP-TFI` el 14/09/2026. La URL actual es:

https://github.com/TUP-TFI/gestion-de-turnos

GitHub redirige automáticamente la URL anterior, así que los enlaces viejos siguen funcionando. Aun así, conviene actualizar el remote local:

```bash
git remote set-url origin https://github.com/TUP-TFI/gestion-de-turnos.git
```

**Motivo del cambio:** en un repositorio de cuenta personal, las integraciones de despliegue (Vercel y similares) solo puede instalarlas el dueño de la cuenta. Al estar bajo una organización, cada integrante conecta su propia plataforma de despliegue sobre el mismo repositorio: frontend y backend se despliegan por separado desde cuentas distintas.