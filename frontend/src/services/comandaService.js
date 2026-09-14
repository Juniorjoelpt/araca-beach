import api from './api.js'

export const comandaService = {
  listarAbertas: () => api.get('/comandas/abertas').then((r) => r.data),
  buscar: (id) => api.get(`/comandas/${id}`).then((r) => r.data),
  abrir: (dados) => api.post('/comandas', dados).then((r) => r.data),
  adicionarItem: (id, dados) => api.post(`/comandas/${id}/itens`, dados).then((r) => r.data),
  fechar: (id) => api.patch(`/comandas/${id}/fechar`).then((r) => r.data),
}
