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

export const relatorioService = {
  async baixarFechamentoCaixa(data) {
    const resposta = await api.get('/relatorios/fechamento-caixa', { params: { data }, responseType: 'blob' })
    baixarArquivo(resposta.data, `fechamento-caixa-${data}.pdf`)
  },
  async baixarComissoes(inicio, fim) {
    const resposta = await api.get('/relatorios/comissoes', { params: { inicio, fim }, responseType: 'blob' })
    baixarArquivo(resposta.data, `comissoes-${inicio}-a-${fim}.pdf`)
  },
  async baixarReservas(inicio, fim) {
    const resposta = await api.get('/relatorios/reservas', { params: { inicio, fim }, responseType: 'blob' })
    baixarArquivo(resposta.data, `reservas-${inicio}-a-${fim}.xlsx`)
  },
  async baixarClientes() {
    const resposta = await api.get('/relatorios/clientes', { responseType: 'blob' })
    baixarArquivo(resposta.data, 'clientes.xlsx')
  },
}
