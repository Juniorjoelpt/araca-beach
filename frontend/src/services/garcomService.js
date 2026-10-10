import api from './api.js'

const base = '/garcom'

export const garcomService = {
  cardapio: () => api.get(`${base}/cardapio`).then((r) => r.data),
  comandas: () => api.get(`${base}/comandas`).then((r) => r.data),
  comanda: (id) => api.get(`${base}/comandas/${id}`).then((r) => r.data),
  abrir: (dados) => api.post(`${base}/comandas`, dados).then((r) => r.data),
  lancarPedido: (id, itens) => api.post(`${base}/comandas/${id}/pedidos`, { itens }).then((r) => r.data),
  clientes: (busca) => api.get(`${base}/clientes`, { params: { busca } }).then((r) => r.data),
}
