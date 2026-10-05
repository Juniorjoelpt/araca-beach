import api from './api.js'

export const mensalidadeService = {
  listar: () => api.get('/mensalidades').then((r) => r.data),
  criar: (dados) => api.post('/mensalidades', dados).then((r) => r.data),
  desativar: (id) => api.patch(`/mensalidades/${id}/desativar`).then((r) => r.data),
  listarPagamentos: (id) => api.get(`/mensalidades/${id}/pagamentos`).then((r) => r.data),
  registrarPagamento: (pagamentoId, dados) =>
    api.patch(`/mensalidades/pagamentos/${pagamentoId}/pagar`, dados).then((r) => r.data),
}

// Matrícula de cliente com mensalidade única cobrindo vários dias/horários
// de quadra ao mesmo tempo (ex.: terça e quinta às 17h na mesma matrícula).
export const matriculaClienteService = {
  listar: () => api.get('/mensalidades/matriculas').then((r) => r.data),
  criar: (dados) => api.post('/mensalidades/matriculas', dados).then((r) => r.data),
  desativar: (id) => api.patch(`/mensalidades/matriculas/${id}/desativar`).then((r) => r.data),
  desativarHorario: (horarioId) =>
    api.patch(`/mensalidades/matriculas/horarios/${horarioId}/desativar`).then((r) => r.data),
  listarPagamentos: (id) => api.get(`/mensalidades/matriculas/${id}/pagamentos`).then((r) => r.data),
  registrarPagamento: (pagamentoId, dados) =>
    api.patch(`/mensalidades/matriculas/pagamentos/${pagamentoId}/pagar`, dados).then((r) => r.data),
}
