import api from './api.js'

export const dashboardService = {
  obter: (data) => api.get('/dashboard', { params: data ? { data } : {} }).then((r) => r.data),
  faturamentoPeriodo: (dias) => api.get('/dashboard/faturamento-periodo', { params: { dias } }).then((r) => r.data),
  ocupacaoQuadras: (inicio, fim) => api.get('/dashboard/ocupacao-quadras', { params: { inicio, fim } }).then((r) => r.data),
  gerencial: (inicio, fim) => api.get('/dashboard/gerencial', { params: { inicio, fim } }).then((r) => r.data),
}
