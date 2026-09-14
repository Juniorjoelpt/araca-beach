import api from './api.js'

/**
 * Decodifica o payload de um JWT (sem validar a assinatura - isso é so
 * pra checagem de conveniencia no frontend) e verifica se o campo "exp"
 * ja passou. Qualquer token mal formado ou sem "exp" e tratado como expirado,
 * por seguranca.
 */
function tokenExpirado(token) {
  try {
    const payloadBase64 = token.split('.')[1]
    const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(payloadJson)
    if (!payload.exp) return true
    const agora = Math.floor(Date.now() / 1000)
    return payload.exp < agora
  } catch {
    return true
  }
}

export const authService = {
  login: async (login, senha) => {
    const { data } = await api.post('/auth/login', { login, senha })
    localStorage.setItem('araca_beach_token', data.token)
    localStorage.setItem('araca_beach_usuario', JSON.stringify({ nome: data.nome, perfil: data.perfil }))
    return data
  },
  logout: () => {
    localStorage.removeItem('araca_beach_token')
    localStorage.removeItem('araca_beach_usuario')
  },
  /** Retorna true somente se existir um token salvo E ele ainda nao tiver expirado. */
  estaAutenticado: () => {
    const token = localStorage.getItem('araca_beach_token')
    if (!token) return false
    if (tokenExpirado(token)) {
      localStorage.removeItem('araca_beach_token')
      localStorage.removeItem('araca_beach_usuario')
      return false
    }
    return true
  },
}
