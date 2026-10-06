import apiPortal from './apiPortal.js'

function tokenExpirado(token) {
  try {
    const payloadBase64 = token.split('.')[1]
    const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(payloadJson)
    if (!payload.exp) return true
    return payload.exp < Math.floor(Date.now() / 1000)
  } catch {
    return true
  }
}

function salvarSessao(data) {
  localStorage.setItem('araca_beach_cliente_token', data.token)
  localStorage.setItem('araca_beach_cliente_dados', JSON.stringify({ id: data.clienteId, nome: data.nome, email: data.email }))
}

export const portalAuthService = {
  registrar: async (nome, email, telefone, senha) => {
    const { data } = await apiPortal.post('/auth/registro', { nome, email, telefone, senha })
    // Com confirmação de e-mail ligada, não há sessão ainda (data.sessao é nulo).
    if (data.sessao) salvarSessao(data.sessao)
    return data
  },
  confirmarEmail: async (token) => {
    const { data } = await apiPortal.post('/auth/confirmar-email', { token })
    salvarSessao(data)
    return data
  },
  reenviarConfirmacao: (email) => apiPortal.post('/auth/reenviar-confirmacao', { email }),
  login: async (email, senha) => {
    const { data } = await apiPortal.post('/auth/login', { email, senha })
    salvarSessao(data)
    return data
  },
  esqueciSenha: (email) => apiPortal.post('/auth/esqueci-senha', { email }),
  redefinirSenha: (token, novaSenha) => apiPortal.post('/auth/redefinir-senha', { token, novaSenha }),
  perfil: () => apiPortal.get('/perfil').then((r) => r.data),
  atualizarPerfil: async (nome, telefone) => {
    const { data } = await apiPortal.put('/perfil', { nome, telefone })
    const raw = localStorage.getItem('araca_beach_cliente_dados')
    const dados = raw ? JSON.parse(raw) : {}
    localStorage.setItem('araca_beach_cliente_dados', JSON.stringify({ ...dados, nome: data.nome }))
    return data
  },
  alterarSenha: (senhaAtual, novaSenha) => apiPortal.patch('/senha', { senhaAtual, novaSenha }),
  logout: () => {
    localStorage.removeItem('araca_beach_cliente_token')
    localStorage.removeItem('araca_beach_cliente_dados')
  },
  estaAutenticado: () => {
    const token = localStorage.getItem('araca_beach_cliente_token')
    if (!token) return false
    if (tokenExpirado(token)) {
      localStorage.removeItem('araca_beach_cliente_token')
      localStorage.removeItem('araca_beach_cliente_dados')
      return false
    }
    return true
  },
  dadosCliente: () => {
    const raw = localStorage.getItem('araca_beach_cliente_dados')
    return raw ? JSON.parse(raw) : null
  },
}
