import api from './api.js'

export const comissaoService = {
  resumo: (inicio, fim) => api.get('/comissoes/resumo', { params: { inicio, fim } }).then((r) => r.data),
  listar: (params) => api.get('/comissoes', { params }).then((r) => r.data),
  pagar: (dados) => api.post('/comissoes/pagar', dados).then((r) => r.data),
}
