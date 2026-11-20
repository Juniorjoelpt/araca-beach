import api from './api.js'

export const diaLiberadoService = {
  listar: () => api.get('/dias-liberados').then((r) => r.data),
  previa: (data) => api.get('/dias-liberados/previa', { params: { data } }).then((r) => r.data),
  liberar: (dados) => api.post('/dias-liberados', dados).then((r) => r.data),
  desfazer: (id) => api.delete(`/dias-liberados/${id}`),
}
