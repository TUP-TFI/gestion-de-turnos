import { defineConfig, devices } from '@playwright/test'

// Tests de punta a punta (T-12.1). Corren contra el sistema local: este front en localhost:5173 y el back que indica
// VITE_API_URL en el .env, levantado aparte con el seed. Nunca contra producción.
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5173',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // Levanta el front si no está corriendo. Si ya está (npm run dev en otra terminal), usa ese.
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
})
