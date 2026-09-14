import api from './api.js'

export const produtoService = {
  listar: () => api.get('/produtos').then((r) => r.data),
  criar: (dados) => api.post('/produtos', dados).then((r) => r.data),
  atualizar: (id, dados) => api.put(`/produtos/${id}`, dados).then((r) => r.data),
  remover: (id) => api.delete(`/produtos/${id}`).then((r) => r.data),
}
