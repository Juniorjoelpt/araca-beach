import apiPortal from './apiPortal.js'

export const portalReservaService = {
  quadras: () => apiPortal.get('/quadras').then((r) => r.data),
  disponibilidade: (quadraId, data) => apiPortal.get('/disponibilidade', { params: { quadraId, data } }).then((r) => r.data),
  criarReserva: (dados) => apiPortal.post('/reservas', dados).then((r) => r.data),
  minhasReservas: () => apiPortal.get('/reservas').then((r) => r.data),
  cancelar: (id) => apiPortal.patch(`/reservas/${id}/cancelar`).then((r) => r.data),
}
