import axios from 'axios'

const apiPortal = axios.create({
  baseURL: '/api/portal',
})

apiPortal.interceptors.request.use((config) => {
  const token = localStorage.getItem('araca_beach_cliente_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiPortal.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('araca_beach_cliente_token')
      localStorage.removeItem('araca_beach_cliente_dados')
      if (!window.location.pathname.startsWith('/portal/entrar') && !window.location.pathname.startsWith('/portal/cadastro')) {
        window.location.href = '/portal/entrar'
      }
    }
    return Promise.reject(error)
  }
)

export default apiPortal
