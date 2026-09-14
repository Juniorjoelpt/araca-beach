import api from './api.js'

export const estoqueService = {
  registrarEntrada: (dados) => api.post('/estoque/entrada', dados).then((r) => r.data),
  registrarSaida: (dados) => api.post('/estoque/saida', dados).then((r) => r.data),
  registrarAjuste: (dados) => api.post('/estoque/ajuste', dados).then((r) => r.data),
  listarMovimentacoes: (produtoId, inicio, fim) =>
    api.get('/estoque/movimentacoes', { params: { produtoId: produtoId || undefined, inicio, fim } }).then((r) => r.data),
}
