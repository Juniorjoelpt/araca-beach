import api from './api.js'

export const auditoriaService = {
  listar: (params) => api.get('/auditoria', { params }).then((r) => r.data),
  recursos: () => api.get('/auditoria/recursos').then((r) => r.data),
}
