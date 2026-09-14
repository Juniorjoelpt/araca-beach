import api from './api.js'

export const aulaService = {
  listarPorDia: (data) => api.get('/aulas', { params: { data } }).then((r) => r.data),
  criar: (dados) => api.post('/aulas', dados).then((r) => r.data),
  remover: (id) => api.delete(`/aulas/${id}`).then((r) => r.data),
  comissao: (professorId, inicio, fim) =>
    api.get('/aulas/comissao', { params: { professorId, inicio, fim } }).then((r) => r.data),
}
