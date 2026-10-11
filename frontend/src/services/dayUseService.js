import api from './api.js'

const base = '/dayuse'

export const dayUseService = {
  dia: (data) => api.get(`${base}/dia`, { params: { data } }).then((r) => r.data),
  tipos: (data) => api.get(`${base}/tipos`, { params: { data } }).then((r) => r.data),
  config: () => api.get(`${base}/config`).then((r) => r.data),
  vender: (dados) => api.post(`${base}/vendas`, dados).then((r) => r.data),
  entrada: (id, quantidade) => api.patch(`${base}/vendas/${id}/entrada`, { quantidade }).then((r) => r.data),
  cancelar: (id, motivo) => api.patch(`${base}/vendas/${id}/cancelar`, null, { params: motivo ? { motivo } : {} }).then((r) => r.data),
  // gestao (ADMIN)
  todosOsTipos: () => api.get(`${base}/gestao/tipos`).then((r) => r.data),
  criarTipo: (d) => api.post(`${base}/gestao/tipos`, d).then((r) => r.data),
  atualizarTipo: (id, d) => api.put(`${base}/gestao/tipos/${id}`, d).then((r) => r.data),
  salvarConfig: (capacidadeDiaria) => api.put(`${base}/gestao/config`, { capacidadeDiaria }).then((r) => r.data),
}
