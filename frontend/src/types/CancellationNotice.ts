// Un turno cancelado por la baja de la empresa. El teléfono es nulo si el cliente no tiene: no hay a quién avisar.
export interface CancellationNotice {
  appointmentId: string
  clientName: string
  phone: string | null
  serviceName: string
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
}

// Respuesta de GET /api/superadmin/companies/{id}/cancellation-notices. Las fechas se muestran en timezone, que es la
// zona de la empresa, y van ordenadas por fecha.
export interface CancellationNotices {
  timezone: string
  notices: CancellationNotice[]
}
