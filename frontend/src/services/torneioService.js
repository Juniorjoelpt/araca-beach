import api from './api.js'

export const torneioService = {
  listar: () => api.get('/torneios').then((r) => r.data),
  criar: (dados) => api.post('/torneios', dados).then((r) => r.data),
  listarInscricoes: (torneioId) => api.get(`/torneios/${torneioId}/inscricoes`).then((r) => r.data),
  inscrever: (dados) => api.post('/torneios/inscricoes', dados).then((r) => r.data),
}

export const chaveamentoService = {
  gerar: (torneioId, categoria) => api.post(`/torneios/${torneioId}/chaveamento`, null, { params: { categoria } }).then((r) => r.data),
  listar: (torneioId, categoria) => api.get(`/torneios/${torneioId}/chaveamento`, { params: { categoria } }).then((r) => r.data),
  registrarResultado: (confrontoId, dados) => api.patch(`/torneios/confrontos/${confrontoId}/resultado`, dados).then((r) => r.data),
}
