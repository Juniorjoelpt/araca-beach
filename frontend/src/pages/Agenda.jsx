import { useEffect, useMemo, useState } from 'react'
import { format, startOfWeek, addDays, isSameDay, isToday, differenceInMinutes } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { ChevronLeft, ChevronRight, Plus, X } from 'lucide-react'
import { quadraService } from '../services/quadraService.js'
import { clienteService } from '../services/clienteService.js'
import { reservaService } from '../services/reservaService.js'
import { financeiroService } from '../services/financeiroService.js'
import { recorrenciaService } from '../services/recorrenciaService.js'

const DURACOES = [
  { minutos: 30, label: '30 min' },
  { minutos: 60, label: '1 hora' },
  { minutos: 90, label: '1h30' },
  { minutos: 120, label: '2 horas' },
]

const DIAS_SEMANA = [
  { value: 'MONDAY', label: 'Segunda-feira' },
  { value: 'TUESDAY', label: 'Terça-feira' },
  { value: 'WEDNESDAY', label: 'Quarta-feira' },
  { value: 'THURSDAY', label: 'Quinta-feira' },
  { value: 'FRIDAY', label: 'Sexta-feira' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
]

function diaSemanaLabel(value) {
  return DIAS_SEMANA.find((d) => d.value === value)?.label ?? value
}

const HORA_INICIO_GRADE = 6
const HORA_FIM_GRADE = 23
const ALTURA_HORA_PX = 56

const CORES_STATUS_PAGAMENTO = {
  PAGO: 'bg-araca-verde-escuro border-araca-verde-escuro',
  PARCIAL: 'bg-amber-500 border-amber-500',
  PENDENTE: 'bg-araca-azul-claro border-araca-azul-claro',
}

export default function Agenda() {
  const [quadras, setQuadras] = useState([])
  const [clientes, setClientes] = useState([])
  const [quadraId, setQuadraId] = useState('')
  const [semanaBase, setSemanaBase] = useState(() => startOfWeek(new Date(), { locale: ptBR }))
  const [reservas, setReservas] = useState([])
  const [pagamentosPorReserva, setPagamentosPorReserva] = useState({})
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [reservaSelecionada, setReservaSelecionada] = useState(null)

  const [tipoReserva, setTipoReserva] = useState('unica')
  const [form, setForm] = useState({ clienteId: '', data: format(new Date(), 'yyyy-MM-dd'), horaInicio: '08:00', duracao: 60 })
  const [recorrencias, setRecorrencias] = useState([])
  const [formRecorrente, setFormRecorrente] = useState({
    clienteId: '', diaSemana: 'MONDAY', horaInicio: '08:00', duracao: 60, vigenciaFim: '',
  })

  const [precoPrevisto, setPrecoPrevisto] = useState(null)

  // Simula o preco (pico/fora de pico + desconto de mensalista) no modal de nova reserva
  useEffect(() => {
    setPrecoPrevisto(null)
    if (!modalAberto || tipoReserva !== 'unica' || !quadraId || !form.data || !form.horaInicio) return
    const inicio = new Date(`${form.data}T${form.horaInicio}:00`)
    if (Number.isNaN(inicio.getTime())) return
    const fim = new Date(inicio.getTime() + form.duracao * 60000)
    let cancelado = false
    reservaService
      .preco(quadraId, form.clienteId, format(inicio, "yyyy-MM-dd'T'HH:mm:ss"), format(fim, "yyyy-MM-dd'T'HH:mm:ss"))
      .then((p) => { if (!cancelado) setPrecoPrevisto(p) })
      .catch(() => {})
    return () => { cancelado = true }
  }, [modalAberto, tipoReserva, quadraId, form.clienteId, form.data, form.horaInicio, form.duracao])

  const diasDaSemana = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(semanaBase, i)),
    [semanaBase]
  )

  useEffect(() => {
    async function carregarBase() {
      try {
        const [listaQuadras, listaClientes] = await Promise.all([
          quadraService.listar(),
          clienteService.listar(),
        ])
        setQuadras(listaQuadras)
        setClientes(listaClientes)
        if (listaQuadras.length > 0) setQuadraId(String(listaQuadras[0].id))
      } catch {
        setErro('Não foi possível carregar quadras/clientes.')
      }
    }
    carregarBase()
    carregarRecorrencias()
  }, [])

  async function carregarRecorrencias() {
    try {
      setRecorrencias(await recorrenciaService.listar())
    } catch {
      // lista complementar - nao bloqueia a agenda
    }
  }

  async function carregarReservas() {
    if (!quadraId) return
    setCarregando(true)
    setErro('')
    try {
      const inicio = format(diasDaSemana[0], "yyyy-MM-dd'T'00:00:00")
      const fim = format(diasDaSemana[6], "yyyy-MM-dd'T'23:59:59")
      const dados = await reservaService.agenda(quadraId, inicio, fim)
      setReservas(dados)

      try {
        const dia = format(new Date(), 'yyyy-MM-dd')
        const visaoFinanceira = await financeiroService.visaoDoDia(dia)
        const mapa = {}
        visaoFinanceira.forEach((r) => { mapa[r.reservaId] = r.statusPagamento })
        setPagamentosPorReserva(mapa)
      } catch {
        setPagamentosPorReserva({})
      }
    } catch {
      setErro('Não foi possível carregar a agenda dessa quadra.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregarReservas()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [quadraId, semanaBase])

  const quadraSelecionada = useMemo(
    () => quadras.find((q) => String(q.id) === String(quadraId)),
    [quadras, quadraId]
  )

  function abrirModalNovaReserva(diaClicado, horaClicada) {
    setForm({
      clienteId: '',
      data: format(diaClicado ?? new Date(), 'yyyy-MM-dd'),
      horaInicio: horaClicada ?? '08:00',
      duracao: 60,
    })
    setTipoReserva('unica')
    setModalAberto(true)
  }

  async function handleCriarReserva(e) {
    e.preventDefault()
    setErro('')
    setSucesso('')
    if (!form.clienteId) {
      setErro('Selecione um cliente.')
      return
    }
    const inicio = new Date(`${form.data}T${form.horaInicio}:00`)
    const fim = new Date(inicio.getTime() + form.duracao * 60000)
    try {
      await reservaService.criar({
        quadraId: Number(quadraId),
        clienteId: Number(form.clienteId),
        inicio: format(inicio, "yyyy-MM-dd'T'HH:mm:ss"),
        fim: format(fim, "yyyy-MM-dd'T'HH:mm:ss"),
      })
      setSucesso('Reserva criada com sucesso.')
      setModalAberto(false)
      carregarReservas()
    } catch (err) {
      const mensagem = err.response?.data?.mensagem
      if (mensagem) {
        setErro(mensagem)
      } else if (err.response?.status === 409) {
        setErro('Já existe uma reserva para esta quadra nesse horário.')
      } else {
        setErro('Não foi possível criar a reserva.')
      }
    }
  }

  async function handleCriarRecorrencia(e) {
    e.preventDefault()
    setErro('')
    setSucesso('')
    if (!formRecorrente.clienteId) {
      setErro('Selecione um cliente.')
      return
    }
    const horaFim = new Date(`2000-01-01T${formRecorrente.horaInicio}:00`)
    horaFim.setMinutes(horaFim.getMinutes() + formRecorrente.duracao)
    try {
      const resultado = await recorrenciaService.criar({
        quadraId: Number(quadraId),
        clienteId: Number(formRecorrente.clienteId),
        diaSemana: formRecorrente.diaSemana,
        horaInicio: formRecorrente.horaInicio,
        horaFim: format(horaFim, 'HH:mm'),
        vigenciaInicio: format(semanaBase, 'yyyy-MM-dd'),
        vigenciaFim: formRecorrente.vigenciaFim || null,
      })
      setSucesso(`Recorrência criada: ${resultado.datasCriadas.length} reserva(s) gerada(s)` +
        (resultado.datasComConflito.length > 0 ? `, ${resultado.datasComConflito.length} com conflito.` : '.'))
      setModalAberto(false)
      carregarReservas()
      carregarRecorrencias()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar a reserva recorrente.')
    }
  }

  async function handleCancelar(reserva) {
    let isentarMulta = false
    try {
      const politica = await reservaService.politicaCancelamento(reserva.id)
      if (politica.gratis) {
        if (!confirm('Cancelar esta reserva? (sem multa)')) return
      } else {
        const taxa = Number(politica.taxa).toFixed(2)
        if (!confirm(`${politica.mensagem}\n\nCancelar COBRANDO a multa de R$ ${taxa}?`)) {
          if (!confirm('Cancelar SEM cobrar a multa (isentar)?')) return
          isentarMulta = true
        }
      }
    } catch {
      if (!confirm('Cancelar esta reserva?')) return
    }
    try {
      const resp = await reservaService.cancelar(reserva.id, isentarMulta)
      setReservaSelecionada(null)
      if (resp.taxaCancelamento && Number(resp.taxaCancelamento) > 0) {
        setSucesso(`Reserva cancelada. Multa de R$ ${Number(resp.taxaCancelamento).toFixed(2)} lançada no Financeiro.`)
      }
      carregarReservas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível cancelar a reserva.')
    }
  }

  async function handleNaoCompareceu(reserva) {
    let isentarMulta = false
    if (!confirm('Registrar que o cliente NÃO COMPARECEU? A multa de no-show será lançada no Financeiro.')) return
    if (confirm('Isentar a multa deste cliente? (OK = isentar, Cancelar = cobrar)')) isentarMulta = true
    try {
      const resp = await reservaService.naoCompareceu(reserva.id, isentarMulta)
      setReservaSelecionada(null)
      setSucesso(resp.taxaCancelamento && Number(resp.taxaCancelamento) > 0
        ? `No-show registrado. Multa de R$ ${Number(resp.taxaCancelamento).toFixed(2)} lançada no Financeiro.`
        : 'No-show registrado sem multa.')
      carregarReservas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível registrar o não comparecimento.')
    }
  }

  async function handleCancelarRecorrencia(id) {
    if (!confirm('Cancelar esta recorrência? As reservas futuras já geradas também serão canceladas.')) return
    try {
      await recorrenciaService.cancelar(id)
      carregarRecorrencias()
      carregarReservas()
    } catch {
      setErro('Não foi possível cancelar a recorrência.')
    }
  }

  const horasDaGrade = Array.from({ length: HORA_FIM_GRADE - HORA_INICIO_GRADE + 1 }, (_, i) => HORA_INICIO_GRADE + i)

  function reservasDoDia(dia) {
    return reservas.filter((r) => isSameDay(new Date(r.inicio), dia))
  }

  function estiloBloco(reserva) {
    const inicio = new Date(reserva.inicio)
    const fim = new Date(reserva.fim)
    const minutosDesdeInicioGrade = (inicio.getHours() - HORA_INICIO_GRADE) * 60 + inicio.getMinutes()
    const duracaoMin = differenceInMinutes(fim, inicio)
    return {
      top: `${(minutosDesdeInicioGrade / 60) * ALTURA_HORA_PX}px`,
      height: `${Math.max((duracaoMin / 60) * ALTURA_HORA_PX - 3, 20)}px`,
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h2 className="font-title text-2xl text-araca-verde">Agenda / Reservas</h2>
        <button
          onClick={() => abrirModalNovaReserva()}
          className="flex items-center gap-2 bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-xl hover:opacity-90 hover:shadow-glow transition"
        >
          <Plus size={18} />
          Novo Agendamento
        </button>
      </div>

      <div className="bg-white rounded-xl shadow p-4 mb-6 flex flex-wrap items-center gap-4">
        <select
          className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
          value={quadraId}
          onChange={(e) => setQuadraId(e.target.value)}
        >
          {quadras.length === 0 && <option value="">Nenhuma quadra cadastrada</option>}
          {quadras.map((q) => (
            <option key={q.id} value={q.id}>{q.nome}</option>
          ))}
        </select>

        <div className="flex items-center gap-1">
          <button
            onClick={() => setSemanaBase(startOfWeek(new Date(), { locale: ptBR }))}
            className="px-3 py-1.5 text-sm border rounded-lg text-araca-azul hover:bg-araca-areia-escura transition"
          >
            Hoje
          </button>
          <button
            onClick={() => setSemanaBase(addDays(semanaBase, -7))}
            className="p-1.5 border rounded-lg text-araca-azul hover:bg-araca-areia-escura transition"
            aria-label="Semana anterior"
          >
            <ChevronLeft size={16} />
          </button>
          <button
            onClick={() => setSemanaBase(addDays(semanaBase, 7))}
            className="p-1.5 border rounded-lg text-araca-azul hover:bg-araca-areia-escura transition"
            aria-label="Próxima semana"
          >
            <ChevronRight size={16} />
          </button>
        </div>

        <p className="text-sm font-medium text-araca-azul">
          {format(diasDaSemana[0], 'dd/MM')} – {format(diasDaSemana[6], 'dd/MM/yyyy')}
        </p>

        {quadraSelecionada && (
          <p className="text-sm text-gray-500 ml-auto">
            Valor da hora (base): <span className="font-semibold text-araca-azul">R$ {Number(quadraSelecionada.valorHora).toFixed(2)}</span>
          </p>
        )}
      </div>

      {quadras.length === 0 && (
        <p className="text-gray-500 mb-6">
          Cadastre uma quadra na aba <strong>Quadras</strong> antes de criar reservas.
        </p>
      )}

      {quadras.length > 0 && (
        <div className="bg-white rounded-xl shadow overflow-hidden">
          {carregando && <p className="text-gray-400 text-sm p-4">Carregando...</p>}
          <div className="overflow-x-auto">
            <div className="min-w-[860px]">
              {/* Linha de cabecalho: canto vazio + um bloco por dia */}
              <div className="flex">
                <div className="w-14 flex-shrink-0 border-b border-r" />
                {diasDaSemana.map((dia) => (
                  <div
                    key={dia.toISOString()}
                    className={`flex-1 min-w-[120px] text-center py-2 border-b border-r ${isToday(dia) ? 'bg-araca-verde-claro/30' : ''}`}
                  >
                    <p className="text-xs text-gray-500 uppercase">{format(dia, 'EEE', { locale: ptBR })}</p>
                    <p className={`text-sm font-semibold ${isToday(dia) ? 'text-araca-verde-escuro' : 'text-araca-azul'}`}>
                      {format(dia, 'dd/MM')}
                    </p>
                  </div>
                ))}
              </div>

              {/* Corpo da grade: coluna de horas + uma coluna por dia (cada uma com altura natural = soma das horas) */}
              <div className="flex">
                <div className="w-14 flex-shrink-0">
                  {horasDaGrade.map((hora) => (
                    <div key={hora} className="relative border-r" style={{ height: ALTURA_HORA_PX }}>
                      <span className="absolute -top-2 right-2 text-xs text-gray-400">{String(hora).padStart(2, '0')}:00</span>
                    </div>
                  ))}
                </div>

                {diasDaSemana.map((dia) => (
                  <div key={dia.toISOString()} className="flex-1 min-w-[120px] relative border-r">
                    {horasDaGrade.map((hora) => (
                      <div
                        key={hora}
                        onClick={() => abrirModalNovaReserva(dia, `${String(hora).padStart(2, '0')}:00`)}
                        className={`border-b hover:bg-araca-areia-escura/60 cursor-pointer transition-colors ${isToday(dia) ? 'bg-araca-verde-claro/10' : ''}`}
                        style={{ height: ALTURA_HORA_PX }}
                      />
                    ))}

                    {reservasDoDia(dia).map((r) => {
                      const statusPagamento = pagamentosPorReserva[r.id] || 'PENDENTE'
                      const cancelada = r.status === 'CANCELADA' || r.status === 'NAO_COMPARECEU'
                      const cor = cancelada ? 'bg-gray-300 border-gray-300' : CORES_STATUS_PAGAMENTO[statusPagamento]
                      return (
                        <button
                          key={r.id}
                          onClick={cancelada ? undefined : (e) => { e.stopPropagation(); setReservaSelecionada(r) }}
                          className={`absolute left-0.5 right-0.5 rounded-lg border-l-4 px-2 py-1 text-left text-white text-[11px] leading-tight shadow-sm transition ${cor} ${
                            cancelada ? 'opacity-50 line-through pointer-events-none' : 'hover:shadow-md hover:brightness-110'
                          }`}
                          style={estiloBloco(r)}
                        >
                          <p className="font-semibold truncate">{r.cliente?.nome}</p>
                          <p className="truncate opacity-90">
                            {format(new Date(r.inicio), 'HH:mm')}–{format(new Date(r.fim), 'HH:mm')}
                          </p>
                        </button>
                      )
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="flex flex-wrap gap-4 mt-3 text-xs text-gray-500">
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-araca-verde-escuro inline-block" /> Pago</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-amber-500 inline-block" /> Parcial</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-araca-azul-claro inline-block" /> Pendente</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-gray-300 inline-block" /> Cancelada</span>
      </div>

      {recorrencias.length > 0 && (
        <div className="bg-white rounded-xl shadow p-6 mt-6">
          <h3 className="font-semibold text-araca-azul mb-4">Recorrências ativas</h3>
          <ul className="space-y-2 text-sm">
            {recorrencias.map((r) => (
              <li key={r.id} className="flex items-center justify-between border-t pt-2 first:border-t-0 first:pt-0">
                <span>
                  {r.quadraNome} · {r.clienteNome} · {diaSemanaLabel(r.diaSemana)} às {r.horaInicio.slice(0, 5)}
                  {r.vigenciaFim ? ` (até ${format(new Date(`${r.vigenciaFim}T00:00:00`), 'dd/MM/yyyy')})` : ''}
                </span>
                <button onClick={() => handleCancelarRecorrencia(r.id)} className="text-red-600 hover:underline">
                  Cancelar
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {erro && <p className="text-red-600 text-sm mt-4">{erro}</p>}
      {sucesso && <p className="text-green-700 text-sm mt-4">{sucesso}</p>}

      {/* Modal: detalhes de uma reserva existente */}
      {reservaSelecionada && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-sm">
            <div className="flex items-start justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">Detalhes da reserva</h3>
              <button onClick={() => setReservaSelecionada(null)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>
            <p className="text-sm text-gray-600 mb-1"><strong className="text-araca-azul">Cliente:</strong> {reservaSelecionada.cliente?.nome}</p>
            <p className="text-sm text-gray-600 mb-1">
              <strong className="text-araca-azul">Horário:</strong> {format(new Date(reservaSelecionada.inicio), 'dd/MM/yyyy HH:mm')} – {format(new Date(reservaSelecionada.fim), 'HH:mm')}
            </p>
            <p className="text-sm text-gray-600 mb-1"><strong className="text-araca-azul">Status:</strong> {reservaSelecionada.status}</p>
            {reservaSelecionada.valorTotal && (
              <p className="text-sm text-gray-600 mb-1"><strong className="text-araca-azul">Valor:</strong> R$ {Number(reservaSelecionada.valorTotal).toFixed(2)}</p>
            )}
            {reservaSelecionada.taxaCancelamento && (
              <p className="text-sm text-red-600 mb-1">Multa aplicada: R$ {Number(reservaSelecionada.taxaCancelamento).toFixed(2)}</p>
            )}
            <div className="mb-3" />
            {reservaSelecionada.status === 'CONFIRMADA' && (
              <div className="space-y-2">
                {new Date(reservaSelecionada.inicio) <= new Date() && (
                  <button
                    onClick={() => handleNaoCompareceu(reservaSelecionada)}
                    className="w-full border border-amber-300 text-amber-700 rounded-lg py-2 hover:bg-amber-50 transition"
                  >
                    Não compareceu (no-show)
                  </button>
                )}
                <button
                  onClick={() => handleCancelar(reservaSelecionada)}
                  className="w-full border border-red-200 text-red-600 rounded-lg py-2 hover:bg-red-50 transition"
                >
                  Cancelar reserva
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal: novo agendamento (unico ou recorrente) */}
      {modalAberto && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">Novo agendamento</h3>
              <button onClick={() => setModalAberto(false)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>

            <div className="flex text-xs border rounded-lg overflow-hidden mb-4 w-fit">
              <button
                type="button"
                onClick={() => setTipoReserva('unica')}
                className={`px-3 py-1 ${tipoReserva === 'unica' ? 'bg-araca-verde text-araca-azul font-semibold' : 'text-gray-500'}`}
              >
                Única
              </button>
              <button
                type="button"
                onClick={() => setTipoReserva('recorrente')}
                className={`px-3 py-1 ${tipoReserva === 'recorrente' ? 'bg-araca-verde text-araca-azul font-semibold' : 'text-gray-500'}`}
              >
                Recorrente
              </button>
            </div>

            {tipoReserva === 'unica' && (
              <form onSubmit={handleCriarReserva} className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Cliente</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={form.clienteId}
                    onChange={(e) => setForm({ ...form, clienteId: e.target.value })}
                    required
                  >
                    <option value="">Selecione...</option>
                    {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                  </select>
                </div>
                <div className="flex gap-3">
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Data</label>
                    <input
                      type="date"
                      className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={form.data}
                      onChange={(e) => setForm({ ...form, data: e.target.value })}
                      required
                    />
                  </div>
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Horário</label>
                    <input
                      type="time"
                      className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={form.horaInicio}
                      onChange={(e) => setForm({ ...form, horaInicio: e.target.value })}
                      required
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Duração</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={form.duracao}
                    onChange={(e) => setForm({ ...form, duracao: Number(e.target.value) })}
                  >
                    {DURACOES.map((d) => <option key={d.minutos} value={d.minutos}>{d.label}</option>)}
                  </select>
                </div>
                {precoPrevisto && (
                  <p className="text-sm text-gray-600 bg-gray-50 rounded-lg px-3 py-2">
                    Valor: <strong className="text-araca-azul">R$ {Number(precoPrevisto.valorTotal).toFixed(2)}</strong>
                    {precoPrevisto.mensalista && Number(precoPrevisto.desconto) > 0 && (
                      <span className="text-green-700"> (mensalista: −R$ {Number(precoPrevisto.desconto).toFixed(2)})</span>
                    )}
                  </p>
                )}
                <button type="submit" className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">
                  Criar reserva
                </button>
              </form>
            )}

            {tipoReserva === 'recorrente' && (
              <form onSubmit={handleCriarRecorrencia} className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Cliente</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formRecorrente.clienteId}
                    onChange={(e) => setFormRecorrente({ ...formRecorrente, clienteId: e.target.value })}
                    required
                  >
                    <option value="">Selecione...</option>
                    {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Dia da semana</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formRecorrente.diaSemana}
                    onChange={(e) => setFormRecorrente({ ...formRecorrente, diaSemana: e.target.value })}
                  >
                    {DIAS_SEMANA.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
                  </select>
                </div>
                <div className="flex gap-3">
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Horário</label>
                    <input
                      type="time"
                      className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={formRecorrente.horaInicio}
                      onChange={(e) => setFormRecorrente({ ...formRecorrente, horaInicio: e.target.value })}
                      required
                    />
                  </div>
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Duração</label>
                    <select
                      className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={formRecorrente.duracao}
                      onChange={(e) => setFormRecorrente({ ...formRecorrente, duracao: Number(e.target.value) })}
                    >
                      {DURACOES.map((d) => <option key={d.minutos} value={d.minutos}>{d.label}</option>)}
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Repetir até (opcional)</label>
                  <input
                    type="date"
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formRecorrente.vigenciaFim}
                    onChange={(e) => setFormRecorrente({ ...formRecorrente, vigenciaFim: e.target.value })}
                  />
                  <p className="text-xs text-gray-400 mt-1">Se não informar, gera as próximas 12 semanas.</p>
                </div>
                <button type="submit" className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">
                  Criar recorrência
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
