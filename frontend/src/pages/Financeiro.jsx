import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { financeiroService } from '../services/financeiroService.js'
import { mensalidadeService, matriculaClienteService } from '../services/mensalidadeService.js'
import { matriculaService } from '../services/turmaService.js'
import { Wallet, Clock } from 'lucide-react'

const FORMAS = [
  { value: 'PIX', label: 'Pix' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
]

const badge = {
  PAGO: 'bg-green-100 text-green-700',
  PARCIAL: 'bg-yellow-100 text-yellow-700',
  PENDENTE: 'bg-red-100 text-red-700',
  CANCELADA: 'bg-gray-100 text-gray-500',
}

const TIPO_LABEL = {
  MENSALIDADE: 'Mensalidade',
  MATRICULA_CLIENTE: 'Matrícula',
  MATRICULA_TURMA: 'Matrícula de turma',
}

export default function Financeiro() {
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [reservas, setReservas] = useState([])
  const [resumo, setResumo] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [reservaSelecionada, setReservaSelecionada] = useState(null)
  const [pagamento, setPagamento] = useState({ valor: '', formaPagamento: 'PIX', ehSinal: false })

  const [cobrancas, setCobrancas] = useState([])
  const [carregandoCobrancas, setCarregandoCobrancas] = useState(true)
  const [cobrancaSelecionada, setCobrancaSelecionada] = useState(null)
  const [formaCobranca, setFormaCobranca] = useState('PIX')

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const [listaReservas, resumoDia] = await Promise.all([
        financeiroService.visaoDoDia(data),
        financeiroService.resumo(data),
      ])
      listaReservas.sort((a, b) => new Date(a.inicio) - new Date(b.inicio))
      setReservas(listaReservas)
      setResumo(resumoDia)
    } catch {
      setErro('Não foi possível carregar os dados financeiros do dia.')
    } finally {
      setCarregando(false)
    }
  }

  async function carregarCobrancas() {
    setCarregandoCobrancas(true)
    try {
      const lista = await financeiroService.cobrancasPendentes()
      setCobrancas(lista)
    } catch {
      // painel opcional: falha em carregar cobranças não bloqueia o restante da página
    } finally {
      setCarregandoCobrancas(false)
    }
  }

  useEffect(() => {
    carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data])

  useEffect(() => {
    carregarCobrancas()
  }, [])

  function abrirPagamento(reserva) {
    const pendente = Number(reserva.valorTotal) - Number(reserva.valorPago)
    setReservaSelecionada(reserva)
    setPagamento({ valor: pendente > 0 ? pendente.toFixed(2) : '', formaPagamento: 'PIX', ehSinal: false })
  }

  async function handleRegistrarPagamento(e) {
    e.preventDefault()
    setErro('')
    try {
      await financeiroService.registrarPagamento({
        reservaId: reservaSelecionada.reservaId,
        valor: Number(pagamento.valor),
        formaPagamento: pagamento.formaPagamento,
        ehSinal: pagamento.ehSinal,
      })
      setReservaSelecionada(null)
      carregar()
    } catch {
      setErro('Não foi possível registrar o pagamento.')
    }
  }

  function abrirPagamentoCobranca(cobranca) {
    setCobrancaSelecionada(cobranca)
    setFormaCobranca('PIX')
  }

  async function handlePagarCobranca(e) {
    e.preventDefault()
    const cobranca = cobrancaSelecionada
    try {
      const dados = { formaPagamento: formaCobranca }
      if (cobranca.tipo === 'MENSALIDADE') {
        await mensalidadeService.registrarPagamento(cobranca.pagamentoId, dados)
      } else if (cobranca.tipo === 'MATRICULA_CLIENTE') {
        await matriculaClienteService.registrarPagamento(cobranca.pagamentoId, dados)
      } else if (cobranca.tipo === 'MATRICULA_TURMA') {
        await matriculaService.registrarPagamento(cobranca.pagamentoId, dados)
      }
      setCobrancaSelecionada(null)
      carregarCobrancas()
      carregar()
    } catch {
      setErro('Não foi possível registrar o pagamento dessa cobrança.')
    }
  }

  const formaLabel = (f) => FORMAS.find((x) => x.value === f)?.label ?? f

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Wallet size={22} className="text-araca-verde-escuro" />
        Financeiro
      </h2>

      <div className="bg-white rounded-xl shadow p-6 mb-6 flex flex-wrap gap-6 items-end">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Data</label>
          <input
            type="date"
            className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={data}
            onChange={(e) => setData(e.target.value)}
          />
        </div>
        {resumo && (
          <>
            <div>
              <p className="text-xs text-gray-500">Total recebido no dia</p>
              <p className="text-2xl font-bold text-araca-azul">R$ {Number(resumo.totalRecebido).toFixed(2)}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Pagamentos registrados</p>
              <p className="text-2xl font-bold text-araca-azul">{resumo.quantidadePagamentos}</p>
            </div>
            {Object.entries(resumo.porFormaPagamento || {}).map(([forma, valor]) => (
              <div key={forma}>
                <p className="text-xs text-gray-500">{formaLabel(forma)}</p>
                <p className="font-semibold text-araca-azul">R$ {Number(valor).toFixed(2)}</p>
              </div>
            ))}
          </>
        )}
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {!carregandoCobrancas && cobrancas.length > 0 && (
        <div className="bg-white rounded-xl shadow overflow-hidden mb-6">
          <div className="px-4 py-3 bg-gray-50 border-b flex items-center gap-2">
            <Clock size={16} className="text-araca-verde-escuro" />
            <h3 className="font-semibold text-araca-azul text-sm">
              Cobranças pendentes (mensalidades e matrículas)
            </h3>
          </div>
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-left">
              <tr>
                <th className="px-4 py-2">Tipo</th>
                <th className="px-4 py-2">Cliente</th>
                <th className="px-4 py-2">Referência</th>
                <th className="px-4 py-2">Vencimento</th>
                <th className="px-4 py-2">Valor</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {cobrancas.map((c) => (
                <tr key={`${c.tipo}-${c.pagamentoId}`} className="border-t">
                  <td className="px-4 py-2">{TIPO_LABEL[c.tipo] || c.tipo}</td>
                  <td className="px-4 py-2 font-medium text-araca-azul">{c.clienteNome}</td>
                  <td className="px-4 py-2">{c.descricao}</td>
                  <td className="px-4 py-2">
                    <span className={c.vencida ? 'text-red-600 font-medium' : ''}>
                      {format(new Date(c.vencimento + 'T00:00:00'), 'dd/MM/yyyy')}
                    </span>
                  </td>
                  <td className="px-4 py-2">R$ {Number(c.valor).toFixed(2)}</td>
                  <td className="px-4 py-2 text-right">
                    <button
                      onClick={() => abrirPagamentoCobranca(c)}
                      className="text-araca-verde-escuro hover:underline font-medium"
                    >
                      Marcar como pago
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 text-left">
            <tr>
              <th className="px-4 py-3">Horário</th>
              <th className="px-4 py-3">Quadra</th>
              <th className="px-4 py-3">Cliente</th>
              <th className="px-4 py-3">Total</th>
              <th className="px-4 py-3">Pago</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={7}>Carregando...</td></tr>
            )}
            {!carregando && reservas.length === 0 && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={7}>Nenhuma reserva nesse dia.</td></tr>
            )}
            {reservas.map((r) => {
              const semCobrancaPropria = Number(r.valorTotal) === 0
              return (
                <tr key={r.reservaId} className="border-t">
                  <td className="px-4 py-3">
                    {format(new Date(r.inicio), 'HH:mm')} - {format(new Date(r.fim), 'HH:mm')}
                  </td>
                  <td className="px-4 py-3">{r.quadraNome}</td>
                  <td className="px-4 py-3 font-medium text-araca-azul">{r.clienteNome}</td>
                  <td className="px-4 py-3">R$ {Number(r.valorTotal).toFixed(2)}</td>
                  <td className="px-4 py-3">R$ {Number(r.valorPago).toFixed(2)}</td>
                  <td className="px-4 py-3">
                    {semCobrancaPropria ? (
                      <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                        MENSALIDADE/MATRÍCULA
                      </span>
                    ) : (
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${badge[r.statusPagamento] || ''}`}>
                        {r.statusPagamento}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {semCobrancaPropria ? (
                      <span className="text-xs text-gray-400">
                        Cobrado via mensalidade/matrícula
                      </span>
                    ) : (
                      r.statusPagamento !== 'PAGO' && r.statusReserva !== 'CANCELADA' && (
                        <button
                          onClick={() => abrirPagamento(r)}
                          className="text-araca-verde-escuro hover:underline font-medium"
                        >
                          Registrar pagamento
                        </button>
                      )
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {reservaSelecionada && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <form
            onSubmit={handleRegistrarPagamento}
            className="bg-white rounded-xl shadow-xl p-6 w-full max-w-sm space-y-4"
          >
            <h3 className="font-semibold text-araca-azul">
              Registrar pagamento — {reservaSelecionada.clienteNome}
            </h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Valor (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={pagamento.valor}
                onChange={(e) => setPagamento({ ...pagamento, valor: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Forma de pagamento</label>
              <select
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={pagamento.formaPagamento}
                onChange={(e) => setPagamento({ ...pagamento, formaPagamento: e.target.value })}
              >
                {FORMAS.map((f) => (
                  <option key={f.value} value={f.value}>{f.label}</option>
                ))}
              </select>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={pagamento.ehSinal}
                onChange={(e) => setPagamento({ ...pagamento, ehSinal: e.target.checked })}
              />
              Este pagamento é um sinal/caução
            </label>
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setReservaSelecionada(null)}
                className="flex-1 border rounded-lg py-2 text-gray-600 hover:bg-gray-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="flex-1 bg-araca-verde text-araca-azul font-semibold rounded-lg py-2 hover:opacity-90"
              >
                Confirmar
              </button>
            </div>
          </form>
        </div>
      )}

      {cobrancaSelecionada && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <form
            onSubmit={handlePagarCobranca}
            className="bg-white rounded-xl shadow-xl p-6 w-full max-w-sm space-y-4"
          >
            <h3 className="font-semibold text-araca-azul">
              Marcar como pago — {cobrancaSelecionada.clienteNome}
            </h3>
            <p className="text-sm text-gray-600">
              {TIPO_LABEL[cobrancaSelecionada.tipo] || cobrancaSelecionada.tipo} · {cobrancaSelecionada.descricao} ·
              {' '}R$ {Number(cobrancaSelecionada.valor).toFixed(2)}
            </p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Forma de pagamento</label>
              <select
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formaCobranca}
                onChange={(e) => setFormaCobranca(e.target.value)}
              >
                {FORMAS.map((f) => (
                  <option key={f.value} value={f.value}>{f.label}</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setCobrancaSelecionada(null)}
                className="flex-1 border rounded-lg py-2 text-gray-600 hover:bg-gray-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="flex-1 bg-araca-verde text-araca-azul font-semibold rounded-lg py-2 hover:opacity-90"
              >
                Confirmar
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
