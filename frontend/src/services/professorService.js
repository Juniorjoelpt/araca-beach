import api from './api.js'

export const professorService = {
  listar: () => api.get('/professores').then((r) => r.data),
  atualizar: (id, dados) => api.put(`/professores/${id}`, dados).then((r) => r.data),
  excluir: (id) => api.delete(`/professores/${id}`),
  criar: (dados) => api.post('/professores', dados).then((r) => r.data),
}
