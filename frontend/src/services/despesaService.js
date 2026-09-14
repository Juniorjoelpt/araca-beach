import api from './api.js'

export const despesaService = {
  listar: (inicio, fim) => api.get('/despesas', { params: { inicio, fim } }).then((r) => r.data),
  resumo: (inicio, fim) => api.get('/despesas/resumo', { params: { inicio, fim } }).then((r) => r.data),
  criar: (dados) => api.post('/despesas', dados).then((r) => r.data),
  marcarComoPaga: (id) => api.patch(`/despesas/${id}/pagar`).then((r) => r.data),
  remover: (id) => api.delete(`/despesas/${id}`).then((r) => r.data),
}
