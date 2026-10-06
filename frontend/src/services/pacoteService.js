import api from './api.js'

export const pacoteService = {
  planos: (somenteAtivos = false) => api.get('/pacotes/planos', { params: { somenteAtivos } }).then((r) => r.data),
  criarPlano: (dados) => api.post('/pacotes/planos', dados).then((r) => r.data),
  atualizarPlano: (id, dados) => api.put(`/pacotes/planos/${id}`, dados).then((r) => r.data),
  vendas: (clienteId) => api.get('/pacotes/vendas', { params: { clienteId: clienteId || undefined } }).then((r) => r.data),
  vender: (dados) => api.post('/pacotes/vendas', dados).then((r) => r.data),
  pagar: (id, formaPagamento) => api.patch(`/pacotes/vendas/${id}/pagar`, { formaPagamento }).then((r) => r.data),
  cancelar: (id) => api.patch(`/pacotes/vendas/${id}/cancelar`).then((r) => r.data),
  aulasDoPacote: (id) => api.get(`/pacotes/vendas/${id}/aulas`).then((r) => r.data),
  agendarAula: (id, dados) => api.post(`/pacotes/vendas/${id}/aulas`, dados).then((r) => r.data),
  aulasDoDia: (data) => api.get('/pacotes/aulas', { params: { data } }).then((r) => r.data),
  presenca: (aulaId, status) => api.patch(`/pacotes/aulas/${aulaId}/presenca`, { status }).then((r) => r.data),
}
