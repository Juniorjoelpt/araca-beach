import api from './api.js'

export const modalidadeService = {
  listar: () => api.get('/modalidades').then((r) => r.data),
  criar: (nome) => api.post('/modalidades', { nome }).then((r) => r.data),
  renomear: (id, nome) => api.put(`/modalidades/${id}`, { nome }).then((r) => r.data),
  excluir: (id) => api.delete(`/modalidades/${id}`).then((r) => r.data),
}
