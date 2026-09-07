# Frontend — Cliente web

Interfaz del sistema de gestión de turnos. React + TypeScript + Vite.

> Documentación de negocio y modelo de datos: [`/docs`](../docs). Convenciones transversales (idiomas, git, calidad): [`convenciones.md`](../docs/tecnologias/convenciones.md).

## Puesta en marcha

> ⏳ *A completar cuando exista el proyecto.*

- Requisitos: Node (versión a definir), gestor de paquetes (npm/pnpm, a definir), **Tailwind** para estilos.
- Variables de entorno: documentar en un `.env.example` versionado (`VITE_API_URL`, etc.). El `.env` real nunca se commitea.
- Comandos: desarrollo, build, tests.

## Las tres áreas de la aplicación

Conviven en el mismo proyecto pero son contextos separados, con sesiones distintas (ver [detalle de flujos](../docs/negocio/detalles-flujos.md)):

| Área | Ruta | Quién entra |
| :--- | :--- | :--- |
| **Panel de sistema** | `/login`, `/superadmin/*` | Superadmin |
| **Panel de empresa** | `/login`, `/company/*` | Admin de empresa |
| **Página pública** | `/{slug}` | Cliente final (o visitante sin login) |

Los dos primeros comparten el login del panel de sistema; la página pública tiene su propio registro/login por empresa. El `{slug}` se deriva del nombre del negocio, por eso esa ruta queda en español.

## Estructura de carpetas

> 💬 **Propuesta a confirmar entre ambos antes de escribir el primer componente.**

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

> ⏳ *Herramientas a confirmar; la estrategia ya está definida.*

**Unitarios / de componente** — Vitest + Testing Library. Foco en lo que tiene lógica real, no en renderizar todo:

- Selector de horarios disponibles.
- Formularios de reserva y sus validaciones.
- Conversión y formato de fechas.
- Habilitado/deshabilitado de cancelar y reprogramar según el plazo.

**E2E** — recorridos completos sobre la aplicación real, con **Playwright**. Los dos flujos que sí o sí conviene cubrir:

1. Cliente reserva un turno desde la página pública de una empresa (T-12.1).
2. Admin cancela un turno desde su turnero (T-12.2).

**Pendiente:** definir si los E2E corren en CI o solo en local — los unitarios sí corren en cada PR (T-01.4).

## Pendiente de definir

- Librería de componentes, si hiciera falta alguna además de Tailwind.
- Versión de Node y gestor de paquetes (npm/pnpm).
- ESLint + Prettier, para sumar al workflow de CI que monta T-01.4.
- Si los E2E corren en CI o solo en local.

Tres puntos que estaban acá ya dejaron de ser decisiones abiertas y pasaron a ser trabajo planificado en `tareas.md`: las rutas protegidas por rol (US-03.3), la personalización por empresa (US-05.7 la carga, US-06.1 la aplica) y el autocomplete de clientes del turnero manual (US-09.4, US-09.7).
