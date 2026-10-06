import api from './api.js'

export const regrasService = {
  configuracao: () => api.get('/regras/configuracao').then((r) => r.data),
  salvarConfiguracao: (dados) => api.put('/regras/configuracao', dados).then((r) => r.data),
  bloqueios: () => api.get('/regras/bloqueios').then((r) => r.data),
  criarBloqueio: (dados) => api.post('/regras/bloqueios', dados).then((r) => r.data),
  removerBloqueio: (id) => api.delete(`/regras/bloqueios/${id}`),
  precos: () => api.get('/regras/precos').then((r) => r.data),
  criarPreco: (dados) => api.post('/regras/precos', dados).then((r) => r.data),
  atualizarPreco: (id, dados) => api.put(`/regras/precos/${id}`, dados).then((r) => r.data),
  removerPreco: (id) => api.delete(`/regras/precos/${id}`),
  listaEspera: () => api.get('/lista-espera').then((r) => r.data),
  removerDaListaEspera: (id) => api.delete(`/lista-espera/${id}`),
}
