import { defineConfig, devices } from '@playwright/test'

// Tests de punta a punta (T-12.1). Corren contra el sistema local: este front en localhost:5173 y el back en
// localhost:8080, levantado aparte con el seed. Nunca contra producción: el test registra clientes y reserva turnos.
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5173',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // Levanta su propio front apuntando al back local: Vite prioriza esta variable sobre la del .env. No reutiliza un
  // npm run dev que ya esté corriendo, porque podría apuntar a otra API.
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    env: { VITE_API_URL: 'http://localhost:8080' },
  },
})
