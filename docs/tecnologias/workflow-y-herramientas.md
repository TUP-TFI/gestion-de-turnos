# Workflow y herramientas

Cómo trabaja el equipo día a día y qué herramientas sostienen ese trabajo, más allá del stack de la aplicación (que está en [arquitectura y stack](arquitectura.md)).

- **Flujo de trabajo:** cómo un cambio pasa de una rama a producción.
- **Integración continua:** qué se verifica automáticamente en cada PR.
- **Dependencias:** cómo se detectan las versiones vulnerables.
- **Despliegue:** dónde corre cada parte.
- **Asistentes de IA:** cómo se usa Claude Code y con qué reglas.

## 1. Flujo de trabajo

```text
rama feat/... ──► commits (Conventional Commits) ──► PR contra main
                                                        │
                         CI en GitHub Actions ◄─────────┤
                         review del otro integrante ◄───┘
                                                        │
                                                 merge a main
                                                        │
                          ┌─────────────────────────────┴───────────┐
                          ▼                                         ▼
                Vercel despliega el frontend           Render despliega el backend
```

1. **Rama por tarea.** Cada historia de usuario o tarea técnica del [roadmap](../alcance-mvp.md) se trabaja en su propia rama `<tipo>/<descripción-corta>`, por ejemplo `feat/company-create-backend` o `chore/backend-ci`. No se commitea directo sobre `main`.
2. **Commits** en formato [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`). En el mensaje se referencia la historia cuando aplica, por ejemplo `(US-05.1, US-05.2)`.
3. **Checks en local** antes de abrir el PR: los mismos que corre el CI (ver [§2](#2-integración-continua)).
4. **Pull Request contra `main`.** Lo revisa el otro integrante. La documentación afectada se actualiza en el mismo PR que el código.
5. **Merge.** Se hace solo con el CI en verde y la review aprobada. Con el merge se despliega automáticamente (ver [§4](#4-despliegue)).

Las tareas se reparten entre los dos integrantes según el [alcance del MVP](../alcance-mvp.md), que asigna un responsable por ítem. En general, backend y frontend de una misma historia van en PRs separados.


## 2. Integración continua

Todo corre en **GitHub Actions**, con los workflows de [`.github/workflows`](../../.github/workflows). Cada check aparece en el PR: si uno bloqueante falla, el PR no se mergea.

| Workflow | Cuándo corre | Qué verifica | Bloquea |
| :--- | :--- | :--- | :--- |
| `backend-ci.yml` — Build, tests y formato | PR y push a `main` | `./mvnw verify`: compila, corre los tests unitarios y de integración contra un PostgreSQL 18 real (Testcontainers), aplica las migraciones de Flyway sobre una base limpia y chequea el formato con Spotless. | Sí |
| `backend-ci.yml` — Migraciones inmutables | PR | Que no se modifique, renombre ni borre una migración `V*.sql` ya existente. | Sí |
| `backend-ci.yml` — Imagen Docker | PR y push a `main` | Que el `Dockerfile` construya (es la misma imagen que despliega Render). Escanea la imagen con **Trivy** en busca de CVEs altas o críticas, solo como información. | Sí (el build) |
| `frontend.yml` — Frontend (lint + build) | PR | `npm ci`, `npm run lint` (ESLint) y `npm run build` (chequeo de tipos con `tsc` y build de Vite). | Sí |
| `codeql.yml` — CodeQL (java) | PR, push a `main` y semanal (lunes) | Análisis estático de seguridad (SAST) del backend. Los hallazgos aparecen en *Security → Code scanning* y como anotaciones en el PR. | Sí |
| `dependency-review.yml` | PR | Que el PR no agregue dependencias con vulnerabilidades conocidas de severidad alta o crítica. | Sí |

Decisiones de diseño del CI:

- **Saltear en vez de filtrar.** Los jobs del backend se saltean si el PR no toca `backend/`. Un job salteado cuenta como aprobado, mientras que un workflow filtrado con `on.paths` dejaría el check en "pending" para siempre.
- **Sin secrets.** Ningún workflow necesita credenciales: la base de los tests la levanta Testcontainers dentro del runner.
- **Actions fijadas por SHA** en los workflows del backend, para que una versión nueva y comprometida de una action no se ejecute sin que nadie la revise.
- **Cancelación de corridas viejas.** Un push nuevo al mismo PR cancela la corrida anterior.
- **Reportes de tests** subidos como artefacto cuando falla el build, para diagnosticar sin reproducir en local.

Para correr lo mismo en local:

```bash
# backend/ (requiere Docker corriendo)
./mvnw verify
./mvnw spotless:apply   # corrige el formato si falla

# frontend/
npm run lint
npm run build
```

> ⚠️ `frontend.yml` todavía no está alineado con el backend: sus actions no están fijadas por SHA y el workflow no corre en push a `main`. Ver [#48](https://github.com/TUP-TFI/gestion-de-turnos/issues/48).

## 3. Dependencias: Dependabot

**Dependabot** es el bot de GitHub que detecta dependencias desactualizadas o vulnerables y abre PRs para actualizarlas.

En este repositorio está configurado solo en modo ***Dependabot security updates***, activado desde *Settings → Code security* del repo:

- Abre un PR **solo cuando una dependencia tiene una vulnerabilidad conocida** (por ejemplo, los PRs que actualizaron `vite`, `source-map-js` y `brace-expansion`).
- Ese PR pasa por el mismo CI y la misma review que cualquier otro.
- **No hay actualizaciones de versión periódicas.** Al principio estaban activadas con un `.github/dependabot.yml` semanal para Maven, la imagen de Docker y las actions. Se quitaron (commit `12b17af`) porque generaban más PRs de los que valía la pena revisar para el alcance del proyecto. Por eso el repo no tiene ese archivo.
- Las actions fijadas por SHA se actualizan a mano.

Se complementa con dos controles más del CI: **Dependency review** frena una dependencia vulnerable *antes* de que entre, y **Trivy** reporta CVEs de la imagen Docker.

## 4. Despliegue

| Parte | Plataforma | Cómo se despliega | Detalle |
| :--- | :--- | :--- | :--- |
| Frontend | **Vercel** | Automático: cada push a `main` actualiza producción y cada rama genera un *preview* con su propia URL. | [README del frontend](../../frontend/README.md#deploy) |
| Backend | **Render** (free tier, Docker) | Automático al llegar un commit a `main`, pero solo cuando los checks del CI de ese commit terminan en verde (*Auto-Deploy: After CI Checks Pass*). | [README del backend](../../backend/README.md#deploy) |
| Base de datos | **Neon** (PostgreSQL 18) | Las migraciones de Flyway se aplican al arrancar el backend. | [README del backend](../../backend/README.md#base-de-datos-y-migraciones) |

El repositorio está en la organización `TUP-TFI` justamente para que cada integrante conecte su propia cuenta de despliegue: el frontend se despliega desde una cuenta y el backend desde otra (ver [§7 del README](../../README.md#7-repositorio)).

Por eso `backend-ci.yml` también corre en cada push a `main`: Render espera a que esos checks pasen antes de desplegar. Si el CI falla después de un merge, producción se queda en la versión anterior.

## 5. Asistentes de IA: Claude Code

Durante el desarrollo se usa **Claude Code**, el asistente de programación de Anthropic que corre en la terminal, como apoyo para:

- **Generación de código:** propone implementaciones a partir de la historia de usuario o la tarea, que el desarrollador revisa y ajusta antes de commitear.
- **Revisiones:** revisa los cambios de una rama o de un PR en busca de errores, inconsistencias con la documentación y violaciones de las convenciones del proyecto.
- **Refinamiento:** mejora código y documentación ya escritos, por ejemplo simplificando una implementación, aplicando el feedback de una review o corrigiendo la redacción.

### Reglas de uso

Las reglas están en [`AGENTS.md`](../../AGENTS.md), en la raíz del repo, y valen para cualquier agente de IA (Claude Code, Copilot, Cursor, etc.). La idea central es que **el agente propone y el desarrollador decide**:

- El agente **no** puede commitear, pushear, abrir o mergear PRs, ni correr build, tests o lint por su cuenta. Solo lo hace cuando el desarrollador lo pide de forma explícita para esa acción puntual.
- Una autorización vale solo para la acción pedida. No se extiende a las siguientes.
- Ante un pedido ambiguo, el agente prepara los cambios y pregunta antes de seguir.

### Trazabilidad

- Los commits hechos con ayuda del agente llevan la línea `Co-Authored-By: Claude ...`, así queda registrado en el historial qué cambios tuvieron asistencia de IA.
- El código generado pasa por el mismo CI y la misma review del otro integrante que el código escrito a mano. El responsable del cambio es siempre el desarrollador que lo commitea.

> TODO (equipo): confirmar si ambos integrantes usan Claude Code (u otro asistente) y si se siguió la convención de `Co-Authored-By` en todos los casos. Hoy hay un solo commit con esa línea en el historial.

## 6. Otras herramientas

| Herramienta | Para qué |
| :--- | :--- |
| **Docker / Docker Compose** | PostgreSQL local (`backend/docker-compose.yml`) e imagen del backend para Render. |
| **Testcontainers** | PostgreSQL real y descartable para los tests de integración, en local y en CI. |
| **Spotless** | Formato del código Java, verificado en CI. |
| **ESLint** | Linter del frontend, verificado en CI. |
| **Maven Wrapper (`./mvnw`)** | Build del backend sin instalar Maven. |
| **Spring Boot Actuator** | `/actuator/health`, que Render usa como health check. |
| **GitHub CLI (`gh`)** | Gestión de PRs y ramas desde la terminal. |

> TODO (equipo): agregar las herramientas que falten, por ejemplo IDE, cliente HTTP para probar la API (Postman, Bruno, etc.), o tablero de tareas si se usa alguno.
