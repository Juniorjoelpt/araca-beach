import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { CalendarCheck2, ChevronDown, ChevronUp } from 'lucide-react'
import { mensalidadeService } from '../services/mensalidadeService.js'
import { recorrenciaService } from '../services/recorrenciaService.js'

const FORMAS = [
  { value: 'PIX', label: 'Pix' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
]

const DIA_SEMANA_LABEL = {
  MONDAY: 'Segunda', TUESDAY: 'Terça', WEDNESDAY: 'Quarta', THURSDAY: 'Quinta',
  FRIDAY: 'Sexta', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
}

const formVazio = { reservaRecorrenteId: '', valorMensal: '', diaVencimento: '5' }

export default function Mensalidades() {
  const [mensalidades, setMensalidades] = useState([])
  const [recorrencias, setRecorrencias] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [expandida, setExpandida] = useState(null)
  const [pagamentosPorMensalidade, setPagamentosPorMensalidade] = useState({})
  const [formaSelecionada, setFormaSelecionada] = useState({})

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const [listaMensalidades, listaRecorrencias] = await Promise.all([
        mensalidadeService.listar(),
        recorrenciaService.listar(),
      ])
      setMensalidades(listaMensalidades)
      setRecorrencias(listaRecorrencias)
    } catch {
      setErro('Não foi possível carregar as mensalidades.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  const recorrenciasSemMensalidade = recorrencias.filter(
    (r) => !mensalidades.some((m) => m.reservaRecorrenteId === r.id)
  )

  async function handleCriar(e) {
    e.preventDefault()
    setErro('')
    try {
      await mensalidadeService.criar({
        reservaRecorrenteId: Number(form.reservaRecorrenteId),
        valorMensal: Number(form.valorMensal),
        diaVencimento: Number(form.diaVencimento),
      })
      setForm(formVazio)
      carregar()
    } catch (err) {
      setErro(err?.response?.data?.message || 'Não foi possível criar a mensalidade. Verifique se a recorrência já não possui uma.')
    }
  }

  async function handleDesativar(id) {
    if (!confirm('Desativar esta mensalidade? O cliente volta a ser cobrado por sessão.')) return
    try {
      await mensalidadeService.desativar(id)
      carregar()
    } catch {
      setErro('Não foi possível desativar a mensalidade.')
    }
  }

  async function toggleExpandir(mensalidadeId) {
    if (expandida === mensalidadeId) {
      setExpandida(null)
      return
    }
    setExpandida(mensalidadeId)
    if (!pagamentosPorMensalidade[mensalidadeId]) {
      try {
        const pagamentos = await mensalidadeService.listarPagamentos(mensalidadeId)
        setPagamentosPorMensalidade((prev) => ({ ...prev, [mensalidadeId]: pagamentos }))
      } catch {
        setErro('Não foi possível carregar as cobranças dessa mensalidade.')
      }
    }
  }

  async function handlePagar(mensalidadeId, pagamentoId) {
    const formaPagamento = formaSelecionada[pagamentoId] || 'PIX'
    try {
      await mensalidadeService.registrarPagamento(pagamentoId, { formaPagamento })
      const pagamentos = await mensalidadeService.listarPagamentos(mensalidadeId)
      setPagamentosPorMensalidade((prev) => ({ ...prev, [mensalidadeId]: pagamentos }))
    } catch {
      setErro('Não foi possível registrar o pagamento.')
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <CalendarCheck2 size={22} className="text-araca-verde-escuro" />
        Mensalidades
      </h2>

      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <h3 className="font-semibold text-araca-azul mb-4">
          Nova mensalidade (cobrança fixa mensal em vez de por sessão)
        </h3>
        <form onSubmit={handleCriar} className="grid grid-cols-1 md:grid-cols-5 gap-4 items-end">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Reserva recorrente</label>
            <select
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.reservaRecorrenteId}
              onChange={(e) => setForm({ ...form, reservaRecorrenteId: e.target.value })}
              required
            >
              <option value="">Selecione...</option>
              {recorrenciasSemMensalidade.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.clienteNome} · {r.quadraNome} · {DIA_SEMANA_LABEL[r.diaSemana] || r.diaSemana} {r.horaInicio}-{r.horaFim}
                </option>
              ))}
            </select>
            {recorrenciasSemMensalidade.length === 0 && (
              <p className="text-xs text-gray-400 mt-1">
                Todas as reservas recorrentes ativas já têm mensalidade, ou não há recorrências cadastradas.
              </p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Valor mensal (R$)</label>
            <input
              type="number" step="0.01" min="0.01"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.valorMensal}
              onChange={(e) => setForm({ ...form, valorMensal: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Dia de vencimento</label>
            <input
              type="number" min="1" max="28"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.diaVencimento}
              onChange={(e) => setForm({ ...form, diaVencimento: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition h-fit">
            Criar mensalidade
          </button>
        </form>
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        {carregando && <p className="px-4 py-4 text-gray-400 text-sm">Carregando...</p>}
        {!carregando && mensalidades.length === 0 && (
          <p className="px-4 py-4 text-gray-400 text-sm">Nenhuma mensalidade cadastrada.</p>
        )}
        <ul className="divide-y">
          {mensalidades.map((m) => {
            const abertaEsta = expandida === m.id
            const pagamentos = pagamentosPorMensalidade[m.id] || []
            return (
              <li key={m.id} className="p-4">
                <div className="flex items-center justify-between">
                  <button
                    onClick={() => toggleExpandir(m.id)}
                    className="flex items-center gap-2 text-left flex-1"
                  >
                    {abertaEsta ? <ChevronUp size={16} className="text-araca-verde-escuro" /> : <ChevronDown size={16} className="text-gray-400" />}
                    <div>
                      <p className="font-medium text-araca-azul">{m.clienteNome}</p>
                      <p className="text-xs text-gray-500">
                        {m.quadraNome} · {DIA_SEMANA_LABEL[m.diaSemana] || m.diaSemana} {m.horaInicio}-{m.horaFim} · vence todo dia {m.diaVencimento}
                      </p>
                    </div>
                  </button>
                  <div className="flex items-center gap-4">
                    <span className="font-bold text-araca-azul">R$ {Number(m.valorMensal).toFixed(2)}/mês</span>
                    <button onClick={() => handleDesativar(m.id)} className="text-red-600 hover:underline text-sm">
                      Desativar
                    </button>
                  </div>
                </div>

                {abertaEsta && (
                  <div className="mt-4 ml-6 border-l-2 border-araca-verde/30 pl-4">
                    {pagamentos.length === 0 && (
                      <p className="text-xs text-gray-400">
                        Nenhuma cobrança gerada ainda. Cobranças são geradas automaticamente todo dia 1h da manhã.
                      </p>
                    )}
                    <ul className="text-sm divide-y">
                      {pagamentos.map((p) => (
                        <li key={p.id} className="py-2 flex items-center justify-between">
                          <span>
                            {p.referenciaMes} · vencimento {format(new Date(`${p.vencimento}T00:00:00`), 'dd/MM/yyyy')} · R$ {Number(p.valor).toFixed(2)}
                          </span>
                          {p.pago ? (
                            <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                              Pago em {format(new Date(`${p.dataPagamento}T00:00:00`), 'dd/MM/yyyy')} ({FORMAS.find((f) => f.value === p.formaPagamento)?.label})
                            </span>
                          ) : (
                            <div className="flex items-center gap-2">
                              <select
                                className="border rounded-lg px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-araca-verde"
                                value={formaSelecionada[p.id] || 'PIX'}
                                onChange={(e) => setFormaSelecionada((prev) => ({ ...prev, [p.id]: e.target.value }))}
                              >
                                {FORMAS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                              </select>
                              <button
                                onClick={() => handlePagar(m.id, p.id)}
                                className="text-araca-verde-escuro hover:underline text-xs font-medium"
                              >
                                Marcar como paga
                              </button>
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}
