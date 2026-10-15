import api from './api.js'

export const turmaService = {
  listar: () => api.get('/turmas').then((r) => r.data),
  criar: (dados) => api.post('/turmas', dados).then((r) => r.data),
  agenda: (quadraId, de, ate) => api.get('/turmas/agenda', { params: { quadraId, de, ate } }).then((r) => r.data),
  atualizar: (id, dados) => api.put(`/turmas/${id}`, dados).then((r) => r.data),
  excluir: (id) => api.delete(`/turmas/${id}`),
  desativar: (id) => api.patch(`/turmas/${id}/desativar`).then((r) => r.data),
  listarMatriculas: (id) => api.get(`/turmas/${id}/matriculas`).then((r) => r.data),
}

export const matriculaService = {
  listar: () => api.get('/matriculas').then((r) => r.data),
  criar: (dados) => api.post('/matriculas', dados).then((r) => r.data),
  financeiro: () => api.get('/matriculas/financeiro').then((r) => r.data),
  atualizarFormaPagamento: (id, formaPagamento) =>
    api.patch(`/matriculas/${id}/forma-pagamento`, { formaPagamento }).then((r) => r.data),
  cancelar: (id) => api.patch(`/matriculas/${id}/cancelar`).then((r) => r.data),
  listarPorCliente: (clienteId) => api.get(`/matriculas/cliente/${clienteId}`).then((r) => r.data),
  listarPagamentos: (id) => api.get(`/matriculas/${id}/pagamentos`).then((r) => r.data),
  registrarPagamento: (pagamentoId, dados) =>
    api.patch(`/matriculas/pagamentos/${pagamentoId}/pagar`, dados).then((r) => r.data),
}
