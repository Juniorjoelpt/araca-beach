import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { financeiroService } from '../services/financeiroService.js'
import { Wallet } from 'lucide-react'

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
}

export default function Financeiro() {
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [reservas, setReservas] = useState([])
  const [resumo, setResumo] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [reservaSelecionada, setReservaSelecionada] = useState(null)
  const [pagamento, setPagamento] = useState({ valor: '', formaPagamento: 'PIX', ehSinal: false })

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

  useEffect(() => {
    carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data])

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
            {reservas.map((r) => (
              <tr key={r.reservaId} className="border-t">
                <td className="px-4 py-3">
                  {format(new Date(r.inicio), 'HH:mm')} - {format(new Date(r.fim), 'HH:mm')}
                </td>
                <td className="px-4 py-3">{r.quadraNome}</td>
                <td className="px-4 py-3 font-medium text-araca-azul">{r.clienteNome}</td>
                <td className="px-4 py-3">R$ {Number(r.valorTotal).toFixed(2)}</td>
                <td className="px-4 py-3">R$ {Number(r.valorPago).toFixed(2)}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${badge[r.statusPagamento] || ''}`}>
                    {r.statusPagamento}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  {r.statusPagamento !== 'PAGO' && r.statusReserva !== 'CANCELADA' && (
                    <button
                      onClick={() => abrirPagamento(r)}
                      className="text-araca-verde-escuro hover:underline font-medium"
                    >
                      Registrar pagamento
                    </button>
                  )}
                </td>
              </tr>
            ))}
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
    </div>
  )
}
