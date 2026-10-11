import apiPortal from './apiPortal.js'

export const portalDayUseService = {
  disponibilidade: (data) => apiPortal.get('/dayuse/disponibilidade', { params: { data } }).then((r) => r.data),
  reservar: (dados) => apiPortal.post('/dayuse/reservas', dados).then((r) => r.data),
  minhas: () => apiPortal.get('/dayuse/reservas').then((r) => r.data),
  cancelar: (id) => apiPortal.patch(`/dayuse/reservas/${id}/cancelar`).then((r) => r.data),
}
