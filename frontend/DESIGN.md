# Sistema de diseño

Guía visual del front: panel de sistema, panel de empresa y página pública de reserva. Los tokens viven en `src/styles/index.css` y las clases compartidas en `src/lib/formStyles.ts`.

## 1. Principios

- **Casi monocromático.** Negro, grises y blanco. El color de marca es solo un acento, así no choca con el `primaryColor` de cada empresa.
- **Plano.** Bordes de 1px en vez de sombras.
- **Sin modo oscuro** en el MVP.

## 2. Colores

| Token (Tailwind) | Valor | Uso |
| :--- | :--- | :--- |
| `ink` | `#111111` | Títulos, botón principal, links de acción |
| `ink-active` | `#242424` | Botón principal presionado o con hover |
| `body` | `#374151` | Texto de cuerpo |
| `muted` | `#6b7280` | Texto secundario, cargando, lista vacía, texto de botón deshabilitado |
| `hairline` | `#e5e7eb` | Bordes y fondo de botón deshabilitado |
| `surface` | `#f8f9fa` | Fondo de página |
| `surface-card` | `#f5f5f5` | Encabezado de tabla, hover de botón secundario, badge neutro |
| `white` | `#ffffff` | Cards, tablas, header, inputs |
| `brand` | `#356378` | Título del panel |
| `brand-dark` | `#2c5568` | Ítem activo de la navegación |
| `brand-light` | `#74a4bc` | Anillo de foco |

Semánticos (paleta de Tailwind, sin tokens propios): éxito `emerald`, advertencia `amber`, error `red`.

**Qué evitar:** el acento `brand` nunca va en el botón principal; solo en el título del panel, la navegación activa y el foco.

## 3. Tipografía

Inter para todo (paquete `@fontsource-variable/inter`, sin CDN).

| Uso | Tamaño / peso |
| :--- | :--- |
| Título de página | 22px / 600 |
| Título de card | 18px / 600 |
| Cuerpo | 14–16px / 400 |
| Etiquetas y badges | 13–14px / 500 |
| Botones | 14px / 600 |

No usar peso mayor a 600.

## 4. Radios, espaciado y elevación

- **Radios:** 8px en botones e inputs; 12px en cards y tablas; píldora en badges. Ningún radio de card mayor a 16px.
- **Espaciado:** base de 4px. Padding de cards: 24px (16px en mobile).
- **Elevación:** plana. Como mucho `shadow-sm` en cards.

## 5. Componentes

- **Botones e inputs:** 40px de alto (`h-10`). El `textarea` no lleva alto fijo.
- **Ancho de los botones:** por defecto, el del texto. Los botones que cierran un form o una tarjeta (guardar, iniciar sesión, activar) llevan `w-full`.
- **Botón principal:** fondo `ink`, texto blanco. Deshabilitado: fondo `hairline`, texto `muted`.
- **Botón secundario:** fondo blanco, borde `hairline`, texto `ink`.
- **Botones de fila (tablas):** secundario compacto de 32px (`h-8`), con borde, para que las acciones se distingan del texto. Las destructivas (desactivar) llevan texto y borde rojos.
- **Foco:** anillo `brand-light` en inputs, botones y links.
- **Hover:** solo se oscurece el botón principal y se subraya el link. Sin otros efectos.
- **Links de acción:** `ink` con subrayado al hover.

## 6. Layout del panel

- Header blanco con borde inferior `hairline`.
- Ancho máximo `max-w-5xl` en el header y en el contenido.
- Título del panel en `brand`; ítem activo de la navegación en `brand-dark` con subrayado; inactivos en `muted`.

## 7. Tablas

Dentro de una card con borde (`overflow-x-auto` para mobile). Encabezado en `surface-card`, filas separadas por divisor `hairline`.

## 8. Estados de pantalla

- **Cargando:** texto `muted`.
- **Error:** alerta roja.
- **Lista vacía:** texto `muted` centrado.
- **Éxito:** alerta verde.

## 9. Badges de estado

Píldora de 13px / 500.

| Variante | Estados |
| :--- | :--- |
| `success` (verde) | Activo, confirmado |
| `neutral` (gris) | Inactivo, cancelado |
| `warning` (ámbar) | Pendiente |

Sirve de guía para los estados de turnos.

## 10. Modal de confirmación

Para acciones destructivas (desactivar empresa, cancelar turno). El botón destructivo va en rojo. Todavía no hay modal implementado.

## 11. Página pública

El color de acción de la página pública `/{slug}` es el `primaryColor` de la empresa; si no tiene, el neutro (`ink`). El wrapper de la página lo aplica con `style`.

**Pendiente:** el color del texto sobre ese fondo se decide al implementar la página (contraste con colores claros).

## 12. Cómo usarlo

Toda pantalla nueva usa las constantes de `src/lib/formStyles.ts`. No se arman botones, inputs, tablas ni badges con clases sueltas. Si falta una variante, se agrega ahí.
