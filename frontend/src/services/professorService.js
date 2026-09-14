import api from './api.js'

export const professorService = {
  listar: () => api.get('/professores').then((r) => r.data),
  criar: (dados) => api.post('/professores', dados).then((r) => r.data),
}
