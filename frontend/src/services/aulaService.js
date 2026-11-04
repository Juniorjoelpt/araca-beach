import api from './api.js'

export const aulaService = {
  listarPorDia: (data) => api.get('/aulas', { params: { data } }).then((r) => r.data),
  criar: (dados) => api.post('/aulas', dados).then((r) => r.data),
  atualizar: (id, dados) => api.put(`/aulas/${id}`, dados).then((r) => r.data),
  pagar: (id, formaPagamento) => api.patch(`/aulas/${id}/pagamento`, { formaPagamento }).then((r) => r.data),
  adicionarAluno: (id, dados) => api.post(`/aulas/${id}/alunos`, dados).then((r) => r.data),
  pagarAluno: (id, alunoId, formaPagamento) => api.patch(`/aulas/${id}/alunos/${alunoId}/pagamento`, { formaPagamento }).then((r) => r.data),
  removerAluno: (id, alunoId) => api.delete(`/aulas/${id}/alunos/${alunoId}`).then((r) => r.data),
  estornar: (id) => api.delete(`/aulas/${id}/estornar`).then((r) => r.data),
  remover: (id) => api.delete(`/aulas/${id}`).then((r) => r.data),
  comissao: (professorId, inicio, fim) =>
    api.get('/aulas/comissao', { params: { professorId, inicio, fim } }).then((r) => r.data),
}
