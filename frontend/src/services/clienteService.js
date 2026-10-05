import api from './api.js'

export const clienteService = {
  listar: () => api.get('/clientes').then((r) => r.data),
  criar: (dados) => api.post('/clientes', dados).then((r) => r.data),
  atualizar: (id, dados) => api.put(`/clientes/${id}`, dados).then((r) => r.data),
  deletar: (id) => api.delete(`/clientes/${id}`).then((r) => r.data),
  historico: (id) => api.get(`/clientes/${id}/historico`).then((r) => r.data),
}
