import { expect, test } from '@playwright/test'

// Empresa del seed del back (APP_SEED_ENABLED=true), con el servicio "Corte de pelo".
const SLUG = 'barberia-central'

// Fecha de dentro de `days` días, en el formato del campo de fecha: AAAA-MM-DD. Usa la fecha de la compu, que en
// local es la misma que la de la empresa.
function dateInDays(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

// Reserva de punta a punta (T-12.1): elegir servicio y horario, registrarse, confirmar y ver el turno en "Mis turnos".
// Se registra un cliente nuevo en cada corrida, así no depende de la contraseña del seed ni de turnos anteriores.
test('un cliente nuevo reserva un turno y lo ve en Mis turnos', async ({ page }) => {
  await page.goto(`/${SLUG}`)
  await page.getByRole('button', { name: /Corte de pelo/ }).click()

  // El primer día con horarios libres desde mañana: el domingo está cerrado y cada corrida ocupa un horario.
  const slots = page.getByRole('button', { name: /^\d{2}:\d{2}$/ })
  const noSlots = page.getByText('No hay horarios disponibles para este día')
  for (let days = 1; days <= 14; days++) {
    await page.getByLabel('Fecha', { exact: true }).fill(dateInDays(days))
    await expect(slots.first().or(noSlots)).toBeVisible()
    if ((await slots.count()) > 0) {
      break
    }
  }
  await slots.first().click()
  await page.getByRole('button', { name: 'Reservar' }).click()

  // Sin sesión, reservar lleva al login, y de ahí al registro, que vuelve a la página con el horario elegido.
  await page.getByRole('link', { name: 'Registrate' }).click()
  await page.getByLabel('Nombre').fill('Prueba')
  await page.getByLabel('Apellido').fill('E2E')
  await page.getByLabel('Email').fill(`e2e-${Date.now()}@example.com`)
  await page.getByLabel('Teléfono').fill('+5493510000099')
  await page.getByLabel('Contraseña', { exact: true }).fill('Clave.prueba.2026')
  await page.getByLabel('Repetir contraseña').fill('Clave.prueba.2026')
  await page.getByRole('button', { name: 'Crear cuenta' }).click()

  const modal = page.getByRole('dialog', { name: 'Confirmá tu turno' })
  await expect(modal).toContainText('Corte de pelo')
  await modal.getByRole('button', { name: 'Confirmar' }).click()
  await expect(page.getByText('Tu turno quedó reservado.')).toBeVisible()

  await page.getByRole('link', { name: 'Ver mis turnos' }).click()
  const appointment = page.getByRole('row').filter({ hasText: 'Corte de pelo' })
  await expect(appointment).toContainText('Pendiente')
})
