import api from './api.js'

export const reservaService = {
  listar: () => api.get('/reservas').then((r) => r.data),
  agenda: (quadraId, inicio, fim) =>
    api.get('/reservas/agenda', { params: { quadraId, inicio, fim } }).then((r) => r.data),
  criar: (dados) => api.post('/reservas', dados).then((r) => r.data),
  cancelar: (id, isentarMulta = false) =>
    api.patch(`/reservas/${id}/cancelar`, null, { params: { isentarMulta } }).then((r) => r.data),
  naoCompareceu: (id, isentarMulta = false) =>
    api.patch(`/reservas/${id}/nao-compareceu`, null, { params: { isentarMulta } }).then((r) => r.data),
  politicaCancelamento: (id) => api.get(`/reservas/${id}/politica-cancelamento`).then((r) => r.data),
  preco: (quadraId, clienteId, inicio, fim) =>
    api.get('/regras/preco', { params: { quadraId, clienteId: clienteId || undefined, inicio, fim } }).then((r) => r.data),
}
