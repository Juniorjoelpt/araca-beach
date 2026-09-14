import api from './api.js'

export const usuarioService = {
  listar: () => api.get('/usuarios').then((r) => r.data),
  criar: (dados) => api.post('/usuarios', dados).then((r) => r.data),
  alterarStatus: (id, ativo) => api.patch(`/usuarios/${id}/status`, { ativo }).then((r) => r.data),
  trocarSenha: (id, novaSenha) => api.patch(`/usuarios/${id}/senha`, { novaSenha }).then((r) => r.data),
}
