# Frontend — Cliente web

Interfaz del sistema de gestión de turnos. React + TypeScript + Vite.

> Documentación de negocio y modelo de datos: [`/docs`](../docs). Convenciones transversales (idiomas, git, calidad): [`convenciones.md`](../docs/tecnologias/convenciones.md).

## Puesta en marcha

Requisitos: **Node 24** (fijado en `engines` del `package.json`) y **npm**. Los estilos usan **Tailwind**.

```bash
# 1. Instalar dependencias
npm install

# 2. Crear el .env a partir de la plantilla (en PowerShell: Copy-Item .env.example .env)
cp .env.example .env

# 3. Levantar el servidor de desarrollo en http://localhost:5173
npm run dev
```

| Comando | Qué hace |
| :--- | :--- |
| `npm run dev` | Servidor de desarrollo. |
| `npm run build` | Chequeo de tipos (`tsc -b`) y build de producción en `dist/`. |
| `npm run lint` | ESLint sobre todo el proyecto. |
| `npm run preview` | Sirve el build de producción en local. |

Tests: todavía no hay ninguna herramienta instalada (ver [Testing](#testing)).

### Variables de entorno

Se documentan en [`.env.example`](.env.example). El `.env` real nunca se commitea.

| Variable | Descripción |
| :--- | :--- |
| `VITE_API_URL` | URL base de la API, sin barra final. Local: `http://localhost:8080`. Producción: `https://gestion-de-turnos-api.onrender.com`. |

- Vite lee el `.env` solo al arrancar: si lo cambiás, reiniciá `npm run dev`.
- Toda variable `VITE_*` queda dentro del JS que baja el navegador, así que **nunca va un secreto ahí**.
- Para desarrollar conviene correr también el backend en local (ver el [README del backend](../backend/README.md)): el backend desplegado solo acepta pedidos del dominio de producción (ver [Deploy](#deploy)), y desde `localhost` el navegador los bloquea por CORS.

## Deploy

El frontend se despliega en **Vercel**, conectado al repositorio: cada push a `main` actualiza producción y cada rama genera un *preview* con su propia URL.

- Producción: https://gestion-de-turnos-eight.vercel.app (el sufijo `-eight` lo asigna Vercel solo).
- `VITE_API_URL` está cargada en Vercel (Settings → Environment Variables) para Production, Preview y Development. Vite la incorpora en el build, así que **al cambiarla hay que redeployar**.
- ⚠️ El backend solo permite CORS desde el dominio de producción. Los previews de cada rama tienen otra URL y quedan bloqueados, por lo que la conexión con la API se prueba en producción o corriendo todo en local.

## CI

Cada PR contra `main` corre el workflow [`frontend.yml`](../.github/workflows/frontend.yml) en GitHub Actions: `npm ci`, `npm run lint` y `npm run build`, con la versión de Node de `engines`. Si alguno falla, el check **Frontend (lint + build)** queda en rojo en el PR. Antes de pushear conviene correr los mismos comandos en local.

## Las tres áreas de la aplicación

Conviven en el mismo proyecto pero son contextos separados, con sesiones distintas (ver [detalle de flujos](../docs/negocio/detalles-flujos.md)):

| Área | Ruta | Quién entra |
| :--- | :--- | :--- |
| **Panel de sistema** | `/login`, `/superadmin/*` | Superadmin |
| **Panel de empresa** | `/login`, `/company/*` | Admin de empresa |
| **Página pública** | `/{slug}` | Cliente final (o visitante sin login) |

Los dos primeros comparten el login del panel de sistema; la página pública tiene su propio registro/login por empresa. El `{slug}` se deriva del nombre del negocio, por eso esa ruta queda en español.

## Estructura de carpetas

Las carpetas que todavía están vacías tienen un `.gitkeep` para que Git las conserve; se borra al agregar el primer archivo.

```
src/
├── api/           # ÚNICA capa que habla con el backend
├── pages/         # una vista por ruta, agrupadas por área
│   ├── superadmin/
│   ├── company/
│   └── public/
├── components/    # componentes reutilizables entre áreas
├── hooks/         # lógica reutilizable (incluye el consumo de api/)
├── context/       # sesión, empresa activa
├── types/         # tipos del dominio, alineados al diccionario de datos
├── lib/           # utilidades (fechas, formato, validaciones)
└── styles/
```

## Regla de capas

**Ésta es la regla que evita el código spaghetti del lado del frontend.**

```
Component  ──►  Hook  ──►  api/  ──►  Backend
```

| Capa | Responsabilidad | Qué NO hace |
| :--- | :--- | :--- |
| **Component / Page** | Renderizar e interactuar con el usuario. | No llama al backend. No contiene reglas de negocio. |
| **Hook** | Estado, orquestación y consumo de `api/`, con **TanStack Query**. | No arma URLs ni headers a mano. |
| **`api/`** | Llamadas HTTP, tipado de request/response, manejo de errores. | No conoce React ni componentes. |

> Hoy hay un único ejemplo de la cadena: `App.tsx` → `hooks/usePing.ts` → `api/ping.ts` (T-01.3). Ese hook usa `useState`/`useEffect` porque es un solo llamado sin caché; TanStack Query se instala con el primer hook que consuma datos reales del dominio.

Reglas duras:

- ❌ Nada de `fetch`/`axios` suelto dentro de un componente.
- ❌ Nada de reglas de negocio duplicadas del backend. El frontend **muestra** la disponibilidad y los plazos, pero la validación real siempre es del servidor.
- ✅ Las fechas llegan del backend en UTC y se convierten a hora local en esta capa, nunca antes (ver [reglas de negocio](../docs/negocio/reglas-negocio.md)).
- ✅ Los tipos de `types/` reflejan el [diccionario de datos](../docs/tecnologias/diccionario-datos.md); si cambia una entidad, se actualizan juntos.

## Convenciones de nombres

- **Componentes:** `PascalCase`, un componente por archivo — `AppointmentCard.tsx`.
- **Hooks:** prefijo `use` — `useAvailability.ts`.
- **Funciones de `api/`:** verbo + recurso — `getCompanyServices()`, `createAppointment()`.
- **Tipos:** `PascalCase` en inglés, alineados al diccionario de datos — `Appointment`, `AppointmentStatus`.
- **Tests:** `<archivo>.test.tsx` junto al archivo que testean.

## Testing

> ⏳ *Herramientas a confirmar; la estrategia ya está definida. Hoy no hay ninguna instalada: ni Vitest ni Playwright figuran en `package.json`.*

**Unitarios / de componente** — Vitest + Testing Library. Foco en lo que tiene lógica real, no en renderizar todo:

- Selector de horarios disponibles.
- Formularios de reserva y sus validaciones.
- Conversión y formato de fechas.
- Habilitado/deshabilitado de cancelar y reprogramar según el plazo.

**E2E** — recorridos completos sobre la aplicación real, con **Playwright**. Los dos flujos que sí o sí conviene cubrir:

1. Cliente reserva un turno desde la página pública de una empresa (T-12.1).
2. Admin cancela un turno desde su turnero (T-12.2).

**Pendiente:** definir si los E2E corren en CI o solo en local. Los unitarios se suman al workflow de [CI](#ci) cuando se instale Vitest.

## Pendiente de definir

- Librería de componentes, si hiciera falta alguna además de Tailwind.
- Prettier, para sumar al workflow de CI. ESLint ya está configurado (`npm run lint`).
- Si los E2E corren en CI o solo en local.

Tres puntos que estaban acá ya dejaron de ser decisiones abiertas y pasaron a ser trabajo planificado en `tareas.md`: las rutas protegidas por rol (US-03.3), la personalización por empresa (US-05.7 la carga, US-06.1 la aplica) y el autocomplete de clientes del turnero manual (US-09.4, US-09.7).
