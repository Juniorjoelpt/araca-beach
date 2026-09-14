import api from './api.js'

export const quadraService = {
  listar: () => api.get('/quadras').then((r) => r.data),
  criar: (dados) => api.post('/quadras', dados).then((r) => r.data),
  atualizar: (id, dados) => api.put(`/quadras/${id}`, dados).then((r) => r.data),
  remover: (id) => api.delete(`/quadras/${id}`).then((r) => r.data),
}
