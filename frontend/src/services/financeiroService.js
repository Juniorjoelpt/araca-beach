import api from './api.js'

export const financeiroService = {
  visaoDoDia: (data) => api.get('/pagamentos/dia', { params: { data } }).then((r) => r.data),
  resumo: (data) => api.get('/pagamentos/resumo', { params: { data } }).then((r) => r.data),
  registrarPagamento: (dados) => api.post('/pagamentos', dados).then((r) => r.data),
  excluirPagamento: (id) => api.delete(`/pagamentos/${id}`).then((r) => r.data),
  cobrancasPendentes: () => api.get('/pagamentos/cobrancas-pendentes').then((r) => r.data),
}
