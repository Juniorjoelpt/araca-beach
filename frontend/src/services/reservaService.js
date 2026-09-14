import api from './api.js'

export const reservaService = {
  listar: () => api.get('/reservas').then((r) => r.data),
  agenda: (quadraId, inicio, fim) =>
    api.get('/reservas/agenda', { params: { quadraId, inicio, fim } }).then((r) => r.data),
  criar: (dados) => api.post('/reservas', dados).then((r) => r.data),
  cancelar: (id) => api.patch(`/reservas/${id}/cancelar`).then((r) => r.data),
}
