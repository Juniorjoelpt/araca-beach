import api from './api.js'

export const turmaService = {
  listar: () => api.get('/turmas').then((r) => r.data),
  criar: (dados) => api.post('/turmas', dados).then((r) => r.data),
  desativar: (id) => api.patch(`/turmas/${id}/desativar`).then((r) => r.data),
  listarMatriculas: (id) => api.get(`/turmas/${id}/matriculas`).then((r) => r.data),
}

export const matriculaService = {
  listar: () => api.get('/matriculas').then((r) => r.data),
  criar: (dados) => api.post('/matriculas', dados).then((r) => r.data),
  cancelar: (id) => api.patch(`/matriculas/${id}/cancelar`).then((r) => r.data),
  listarPorCliente: (clienteId) => api.get(`/matriculas/cliente/${clienteId}`).then((r) => r.data),
  listarPagamentos: (id) => api.get(`/matriculas/${id}/pagamentos`).then((r) => r.data),
  registrarPagamento: (pagamentoId, dados) =>
    api.patch(`/matriculas/pagamentos/${pagamentoId}/pagar`, dados).then((r) => r.data),
}
