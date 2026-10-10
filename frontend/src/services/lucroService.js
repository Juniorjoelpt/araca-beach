import api from './api.js'

export const lucroService = {
  gerar: (inicio, fim) => api.get('/lucro', { params: { inicio, fim } }).then((r) => r.data),
}
