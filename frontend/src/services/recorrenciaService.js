import api from './api.js'

export const recorrenciaService = {
  listar: () => api.get('/reservas-recorrentes').then((r) => r.data),
  criar: (dados) => api.post('/reservas-recorrentes', dados).then((r) => r.data),
  cancelar: (id) => api.patch(`/reservas-recorrentes/${id}/cancelar`).then((r) => r.data),
}
