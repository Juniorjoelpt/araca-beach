import api from './api.js'

export const comandaService = {
  listarAbertas: () => api.get('/comandas/abertas').then((r) => r.data),
  buscar: (id) => api.get(`/comandas/${id}`).then((r) => r.data),
  abrir: (dados) => api.post('/comandas', dados).then((r) => r.data),
  adicionarItem: (id, dados) => api.post(`/comandas/${id}/itens`, dados).then((r) => r.data),
  cortesiaItem: (id, itemId, cortesia) =>
    api.patch(`/comandas/${id}/itens/${itemId}/cortesia`, null, { params: { cortesia } }).then((r) => r.data),
  desconto: (id, valor, motivo) => api.patch(`/comandas/${id}/desconto`, { valor, motivo }).then((r) => r.data),
  fechar: (id, forma) => api.patch(`/comandas/${id}/fechar`, null, { params: forma ? { forma } : {} }).then((r) => r.data),
}
