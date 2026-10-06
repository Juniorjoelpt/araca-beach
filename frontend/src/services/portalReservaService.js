import apiPortal from './apiPortal.js'

export const portalReservaService = {
  quadras: () => apiPortal.get('/quadras').then((r) => r.data),
  disponibilidade: (quadraId, data) => apiPortal.get('/disponibilidade', { params: { quadraId, data } }).then((r) => r.data),
  preco: (quadraId, inicio, fim) => apiPortal.get('/preco', { params: { quadraId, inicio, fim } }).then((r) => r.data),
  criarReserva: (dados) => apiPortal.post('/reservas', dados).then((r) => r.data),
  minhasReservas: () => apiPortal.get('/reservas').then((r) => r.data),
  cancelar: (id) => apiPortal.patch(`/reservas/${id}/cancelar`).then((r) => r.data),
  politica: () => apiPortal.get('/politica').then((r) => r.data),
  listaEspera: () => apiPortal.get('/lista-espera').then((r) => r.data),
  entrarNaListaEspera: (dados) => apiPortal.post('/lista-espera', dados).then((r) => r.data),
  sairDaListaEspera: (id) => apiPortal.delete(`/lista-espera/${id}`),
  cobrancas: () => apiPortal.get('/cobrancas').then((r) => r.data),
  turmas: () => apiPortal.get('/turmas').then((r) => r.data),
  pacotes: () => apiPortal.get('/pacotes').then((r) => r.data),
  aulas: () => apiPortal.get('/aulas').then((r) => r.data),
  avisarFalta: (id) => apiPortal.patch(`/aulas/${id}/avisar-falta`).then((r) => r.data),
}
