import api from './api.js'

export const mensalidadeService = {
  listar: () => api.get('/mensalidades').then((r) => r.data),
  criar: (dados) => api.post('/mensalidades', dados).then((r) => r.data),
  desativar: (id) => api.patch(`/mensalidades/${id}/desativar`).then((r) => r.data),
  listarPagamentos: (id) => api.get(`/mensalidades/${id}/pagamentos`).then((r) => r.data),
  registrarPagamento: (pagamentoId, dados) =>
    api.patch(`/mensalidades/pagamentos/${pagamentoId}/pagar`, dados).then((r) => r.data),
}
