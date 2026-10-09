import { useState } from 'react'
import { format, startOfMonth, endOfMonth } from 'date-fns'
import { FileDown, FileSpreadsheet, FileText, Wallet, Users, CalendarRange, Percent, History } from 'lucide-react'
import { relatorioService } from '../services/relatorioService.js'
import { caixaService } from '../services/caixaService.js'

function CardRelatorio({ icon: Icon, titulo, descricao, formato, children, onExportar, carregando }) {
  return (
    <div className="bg-white rounded-xl shadow p-6">
      <div className="flex items-start gap-3 mb-4">
        <div className="bg-araca-areia-escura/60 rounded-lg p-2.5">
          <Icon size={20} className="text-araca-verde-escuro" />
        </div>
        <div>
          <h3 className="font-semibold text-araca-azul">{titulo}</h3>
          <p className="text-xs text-gray-500">{descricao}</p>
        </div>
        <span className="ml-auto text-xs font-medium text-gray-400 border rounded-full px-2 py-0.5">{formato}</span>
      </div>
      {children}
      <button
        onClick={onExportar}
        disabled={carregando}
        className="mt-4 w-full flex items-center justify-center gap-2 bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 disabled:opacity-50 transition"
      >
        <FileDown size={16} />
        {carregando ? 'Gerando...' : 'Exportar'}
      </button>
    </div>
  )
}

export default function Relatorios() {
  const hoje = new Date()
  const [dataCaixa, setDataCaixa] = useState(format(hoje, 'yyyy-MM-dd'))
  const [periodoComissoes, setPeriodoComissoes] = useState({
    inicio: format(startOfMonth(hoje), 'yyyy-MM-dd'),
    fim: format(hoje, 'yyyy-MM-dd'),
  })
  const [periodoReservas, setPeriodoReservas] = useState({
    inicio: format(startOfMonth(hoje), 'yyyy-MM-dd'),
    fim: format(endOfMonth(hoje), 'yyyy-MM-dd'),
  })
  const [periodoHistorico, setPeriodoHistorico] = useState({
    inicio: format(hoje, 'yyyy-MM-dd'),
    fim: format(hoje, 'yyyy-MM-dd'),
  })
  const [carregando, setCarregando] = useState('')
  const [erro, setErro] = useState('')

  async function executar(chave, fn) {
    setErro('')
    setCarregando(chave)
    try {
      await fn()
    } catch (err) {
      setErro(await mensagemDeErro(err))
    } finally {
      setCarregando('')
    }
  }

  // Os downloads usam responseType: 'blob', entao um erro do backend (ex.:
  // "data final antes da data inicial") chega como Blob, nao como JSON pronto
  // - precisamos ler o texto do blob e fazer o parse manualmente para mostrar
  // a mensagem real em vez de um "não foi possível gerar" genérico.
  async function mensagemDeErro(err) {
    const dados = err.response?.data
    if (dados instanceof Blob) {
      try {
        const texto = await dados.text()
        const corpo = JSON.parse(texto)
        if (corpo?.mensagem) return corpo.mensagem
      } catch {
        // corpo não era JSON (ex.: erro de rede/HTML de proxy) - cai no padrão abaixo
      }
    } else if (dados?.mensagem) {
      return dados.mensagem
    }
    return 'Não foi possível gerar o relatório.'
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <FileText size={22} className="text-araca-verde-escuro" />
        Relatórios
      </h2>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <CardRelatorio
          icon={Wallet}
          titulo="Fechamento de caixa"
          descricao="Entradas, despesas pagas e saldo líquido do dia"
          formato="PDF"
          carregando={carregando === 'caixa'}
          onExportar={() => executar('caixa', () => relatorioService.baixarFechamentoCaixa(dataCaixa))}
        >
          <label className="block text-sm font-medium text-gray-700 mb-1">Data</label>
          <input
            type="date"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={dataCaixa}
            onChange={(e) => setDataCaixa(e.target.value)}
          />
        </CardRelatorio>

        <CardRelatorio
          icon={History}
          titulo="Histórico de caixa"
          descricao="Todos os recebimentos do período, com forma de pagamento e operador"
          formato="PDF"
          carregando={carregando === 'historico'}
          onExportar={() => executar('historico', () => caixaService.baixarPdf(periodoHistorico))}
        >
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoHistorico.inicio}
                onChange={(e) => setPeriodoHistorico({ ...periodoHistorico, inicio: e.target.value })}
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoHistorico.fim}
                onChange={(e) => setPeriodoHistorico({ ...periodoHistorico, fim: e.target.value })}
              />
            </div>
          </div>
          <p className="text-xs text-gray-500 mt-2">Com filtros por operador, origem e forma: abra “Histórico de caixa” no menu.</p>
        </CardRelatorio>

        <CardRelatorio
          icon={Percent}
          titulo="Comissão de professores"
          descricao="Aulas dadas, valor total e comissão por professor no período"
          formato="PDF"
          carregando={carregando === 'comissoes'}
          onExportar={() => executar('comissoes', () => relatorioService.baixarComissoes(periodoComissoes.inicio, periodoComissoes.fim))}
        >
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoComissoes.inicio}
                onChange={(e) => setPeriodoComissoes({ ...periodoComissoes, inicio: e.target.value })}
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoComissoes.fim}
                onChange={(e) => setPeriodoComissoes({ ...periodoComissoes, fim: e.target.value })}
              />
            </div>
          </div>
        </CardRelatorio>

        <CardRelatorio
          icon={CalendarRange}
          titulo="Reservas do período"
          descricao="Lista completa de reservas com data, quadra, cliente, status e valor"
          formato="Excel"
          carregando={carregando === 'reservas'}
          onExportar={() => executar('reservas', () => relatorioService.baixarReservas(periodoReservas.inicio, periodoReservas.fim))}
        >
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoReservas.inicio}
                onChange={(e) => setPeriodoReservas({ ...periodoReservas, inicio: e.target.value })}
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={periodoReservas.fim}
                onChange={(e) => setPeriodoReservas({ ...periodoReservas, fim: e.target.value })}
              />
            </div>
          </div>
        </CardRelatorio>

        <CardRelatorio
          icon={Users}
          titulo="Lista de clientes"
          descricao="Nome, telefone e e-mail de todos os clientes cadastrados"
          formato="Excel"
          carregando={carregando === 'clientes'}
          onExportar={() => executar('clientes', () => relatorioService.baixarClientes())}
        >
          <p className="text-sm text-gray-400">Exporta o cadastro completo, sem filtro de período.</p>
        </CardRelatorio>
      </div>
    </div>
  )
}
