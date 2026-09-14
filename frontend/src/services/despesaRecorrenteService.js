import api from './api.js'

export const despesaRecorrenteService = {
  listar: () => api.get('/despesas-recorrentes').then((r) => r.data),
  criar: (dados) => api.post('/despesas-recorrentes', dados).then((r) => r.data),
  desativar: (id) => api.patch(`/despesas-recorrentes/${id}/desativar`).then((r) => r.data),
}
