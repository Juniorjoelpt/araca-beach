import api from './api.js'

function baixarArquivo(blob, nomeArquivo) {
  const url = window.URL.createObjectURL(new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', nomeArquivo)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const limpar = (p) => Object.fromEntries(Object.entries(p).filter(([, v]) => v !== '' && v != null))

export const caixaService = {
  historico: (params) => api.get('/caixa/historico', { params: limpar(params) }).then((r) => r.data),
  operadores: () => api.get('/caixa/operadores').then((r) => r.data),
  async baixarPdf(params) {
    const resposta = await api.get('/caixa/historico/pdf', { params: limpar(params), responseType: 'blob' })
    baixarArquivo(resposta.data, `historico-caixa-${params.inicio}${params.inicio === params.fim ? '' : `-a-${params.fim}`}.pdf`)
  },
}
