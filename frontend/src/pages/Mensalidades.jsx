import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { CalendarCheck2, ChevronDown, ChevronUp, Plus, Trash2 } from 'lucide-react'
import { mensalidadeService, matriculaClienteService } from '../services/mensalidadeService.js'
import { recorrenciaService } from '../services/recorrenciaService.js'
import { clienteService } from '../services/clienteService.js'
import { quadraService } from '../services/quadraService.js'

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

const horarioVazio = { quadraId: '', diaSemana: 'MONDAY', horaInicio: '08:00', horaFim: '09:00' }
const formMatriculaVazio = {
  clienteId: '', valorMensal: '', diaVencimento: '5',
  dataInicio: format(new Date(), 'yyyy-MM-dd'),
  horarios: [{ ...horarioVazio }],
}

export default function Mensalidades() {
  const [aba, setAba] = useState('recorrencia')

  const [mensalidades, setMensalidades] = useState([])
  const [recorrencias, setRecorrencias] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [expandida, setExpandida] = useState(null)
  const [pagamentosPorMensalidade, setPagamentosPorMensalidade] = useState({})
  const [formaSelecionada, setFormaSelecionada] = useState({})

  // --- Matrícula com múltiplos dias/horários ---
  const [clientes, setClientes] = useState([])
  const [quadras, setQuadras] = useState([])
  const [matriculas, setMatriculas] = useState([])
  const [carregandoMatriculas, setCarregandoMatriculas] = useState(true)
  const [formMatricula, setFormMatricula] = useState(formMatriculaVazio)
  const [matriculaExpandidaId, setMatriculaExpandidaId] = useState(null)
  const [pagamentosPorMatricula, setPagamentosPorMatricula] = useState({})
  const [formaSelecionadaMatricula, setFormaSelecionadaMatricula] = useState({})
  const [sucessoMatricula, setSucessoMatricula] = useState('')

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

  async function carregarMatriculas() {
    setCarregandoMatriculas(true)
    setErro('')
    try {
      const [listaMatriculas, listaClientes, listaQuadras] = await Promise.all([
        matriculaClienteService.listar(),
        clienteService.listar(),
        quadraService.listar(),
      ])
      setMatriculas(listaMatriculas)
      setClientes(listaClientes)
      setQuadras(listaQuadras)
    } catch {
      setErro('Não foi possível carregar as matrículas.')
    } finally {
      setCarregandoMatriculas(false)
    }
  }

  useEffect(() => { carregar() }, [])
  useEffect(() => { if (aba === 'matricula') carregarMatriculas() }, [aba]) // eslint-disable-line react-hooks/exhaustive-deps

  function adicionarHorario() {
    setFormMatricula({ ...formMatricula, horarios: [...formMatricula.horarios, { ...horarioVazio }] })
  }

  function removerHorario(index) {
    setFormMatricula({ ...formMatricula, horarios: formMatricula.horarios.filter((_, i) => i !== index) })
  }

  function atualizarHorario(index, campo, valor) {
    const horarios = formMatricula.horarios.map((h, i) => (i === index ? { ...h, [campo]: valor } : h))
    setFormMatricula({ ...formMatricula, horarios })
  }

  async function handleCriarMatricula(e) {
    e.preventDefault()
    setErro('')
    setSucessoMatricula('')
    try {
      const resultado = await matriculaClienteService.criar({
        clienteId: Number(formMatricula.clienteId),
        valorMensal: Number(formMatricula.valorMensal),
        diaVencimento: Number(formMatricula.diaVencimento),
        dataInicio: formMatricula.dataInicio,
        horarios: formMatricula.horarios.map((h) => ({ ...h, quadraId: Number(h.quadraId) })),
      })
      setFormMatricula(formMatriculaVazio)
      setSucessoMatricula(resultado?.resumoGeracao || 'Matrícula criada.')
      carregarMatriculas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar a matrícula.')
    }
  }

  async function handleDesativarMatricula(id) {
    if (!confirm('Desativar esta matrícula? Todos os horários dela param de gerar novas aulas e as futuras já agendadas são canceladas.')) return
    try {
      await matriculaClienteService.desativar(id)
      carregarMatriculas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível desativar a matrícula.')
    }
  }

  async function handleDesativarHorario(horarioId) {
    if (!confirm('Remover este horário da matrícula? As aulas futuras desse horário são canceladas.')) return
    try {
      await matriculaClienteService.desativarHorario(horarioId)
      carregarMatriculas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível remover o horário.')
    }
  }

  async function toggleExpandirMatricula(id) {
    if (matriculaExpandidaId === id) {
      setMatriculaExpandidaId(null)
      return
    }
    setMatriculaExpandidaId(id)
    if (!pagamentosPorMatricula[id]) {
      try {
        const pagamentos = await matriculaClienteService.listarPagamentos(id)
        setPagamentosPorMatricula((prev) => ({ ...prev, [id]: pagamentos }))
      } catch {
        setErro('Não foi possível carregar as cobranças dessa matrícula.')
      }
    }
  }

  async function handlePagarMatricula(matriculaId, pagamentoId) {
    const formaPagamento = formaSelecionadaMatricula[pagamentoId] || 'PIX'
    try {
      await matriculaClienteService.registrarPagamento(pagamentoId, { formaPagamento })
      const pagamentos = await matriculaClienteService.listarPagamentos(matriculaId)
      setPagamentosPorMatricula((prev) => ({ ...prev, [matriculaId]: pagamentos }))
    } catch {
      setErro('Não foi possível registrar o pagamento.')
    }
  }

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

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        <button
          onClick={() => setAba('recorrencia')}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition ${
            aba === 'recorrencia' ? 'border-araca-verde-escuro text-araca-verde-escuro' : 'border-transparent text-gray-500 hover:text-araca-azul'
          }`}
        >
          Mensalidade por recorrência
        </button>
        <button
          onClick={() => setAba('matricula')}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition ${
            aba === 'matricula' ? 'border-araca-verde-escuro text-araca-verde-escuro' : 'border-transparent text-gray-500 hover:text-araca-azul'
          }`}
        >
          Matrícula (vários dias/horários)
        </button>
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {aba === 'recorrencia' && (
      <>
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
      </>
      )}

      {aba === 'matricula' && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow p-6">
            <h3 className="font-semibold text-araca-azul mb-1">
              Nova matrícula (mensalidade única cobrindo vários dias/horários)
            </h3>
            <p className="text-xs text-gray-500 mb-4">
              Use quando o cliente joga em mais de um dia da semana (ex.: terça e quinta) e você quer cobrar
              um valor mensal único, em vez de uma mensalidade separada por recorrência. As aulas/jogos de
              cada horário entram automaticamente na Agenda.
            </p>
            <form onSubmit={handleCriarMatricula} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Cliente</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formMatricula.clienteId}
                    onChange={(e) => setFormMatricula({ ...formMatricula, clienteId: e.target.value })}
                    required
                  >
                    <option value="">Selecione...</option>
                    {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Valor mensal total (R$)</label>
                  <input
                    type="number" step="0.01" min="0.01"
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formMatricula.valorMensal}
                    onChange={(e) => setFormMatricula({ ...formMatricula, valorMensal: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Dia de vencimento</label>
                  <input
                    type="number" min="1" max="28"
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formMatricula.diaVencimento}
                    onChange={(e) => setFormMatricula({ ...formMatricula, diaVencimento: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Início da matrícula</label>
                <input
                  type="date"
                  className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formMatricula.dataInicio}
                  onChange={(e) => setFormMatricula({ ...formMatricula, dataInicio: e.target.value })}
                  required
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-sm font-medium text-gray-700">Dias e horários</label>
                  <button
                    type="button"
                    onClick={adicionarHorario}
                    className="flex items-center gap-1 text-araca-verde-escuro text-sm hover:underline"
                  >
                    <Plus size={14} /> Adicionar dia
                  </button>
                </div>
                <div className="space-y-3">
                  {formMatricula.horarios.map((h, index) => (
                    <div key={index} className="flex flex-wrap items-end gap-3 bg-araca-areia/40 rounded-lg p-3">
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Quadra</label>
                        <select
                          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                          value={h.quadraId}
                          onChange={(e) => atualizarHorario(index, 'quadraId', e.target.value)}
                          required
                        >
                          <option value="">Selecione...</option>
                          {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
                        </select>
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Dia da semana</label>
                        <select
                          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                          value={h.diaSemana}
                          onChange={(e) => atualizarHorario(index, 'diaSemana', e.target.value)}
                        >
                          {Object.entries(DIA_SEMANA_LABEL).map(([valor, label]) => (
                            <option key={valor} value={valor}>{label}</option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Início</label>
                        <input
                          type="time"
                          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                          value={h.horaInicio}
                          onChange={(e) => atualizarHorario(index, 'horaInicio', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Fim</label>
                        <input
                          type="time"
                          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                          value={h.horaFim}
                          onChange={(e) => atualizarHorario(index, 'horaFim', e.target.value)}
                        />
                      </div>
                      {formMatricula.horarios.length > 1 && (
                        <button
                          type="button"
                          onClick={() => removerHorario(index)}
                          className="text-red-600 hover:underline text-xs flex items-center gap-1 mb-0.5"
                        >
                          <Trash2 size={13} /> Remover
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
                Criar matrícula
              </button>
            </form>

            {sucessoMatricula && (
              <p className="text-araca-verde-escuro text-sm mt-3">{sucessoMatricula}</p>
            )}
          </div>

          <div className="bg-white rounded-xl shadow overflow-hidden">
            {carregandoMatriculas && <p className="px-4 py-4 text-gray-400 text-sm">Carregando...</p>}
            {!carregandoMatriculas && matriculas.length === 0 && (
              <p className="px-4 py-4 text-gray-400 text-sm">Nenhuma matrícula cadastrada ainda.</p>
            )}
            <ul className="divide-y">
              {matriculas.map((m) => {
                const abertaEsta = matriculaExpandidaId === m.id
                const pagamentos = pagamentosPorMatricula[m.id] || []
                return (
                  <li key={m.id} className="p-4">
                    <div className="flex items-center justify-between">
                      <button
                        onClick={() => toggleExpandirMatricula(m.id)}
                        className="flex items-center gap-2 text-left flex-1"
                      >
                        {abertaEsta ? <ChevronUp size={16} className="text-araca-verde-escuro" /> : <ChevronDown size={16} className="text-gray-400" />}
                        <div>
                          <p className="font-medium text-araca-azul">{m.clienteNome}</p>
                          <p className="text-xs text-gray-500">
                            {m.horarios.map((h) => `${DIA_SEMANA_LABEL[h.diaSemana] || h.diaSemana} ${h.horaInicio}-${h.horaFim} (${h.quadraNome})`).join(' · ')}
                            {' '}· vence todo dia {m.diaVencimento}
                          </p>
                        </div>
                      </button>
                      <div className="flex items-center gap-4">
                        <span className="font-bold text-araca-azul">R$ {Number(m.valorMensal).toFixed(2)}/mês</span>
                        <button onClick={() => handleDesativarMatricula(m.id)} className="text-red-600 hover:underline text-sm">
                          Desativar
                        </button>
                      </div>
                    </div>

                    {abertaEsta && (
                      <div className="mt-4 ml-6 border-l-2 border-araca-verde/30 pl-4 space-y-4">
                        <div>
                          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">Horários</p>
                          <ul className="text-sm space-y-1">
                            {m.horarios.map((h) => (
                              <li key={h.id} className="flex items-center justify-between">
                                <span>{DIA_SEMANA_LABEL[h.diaSemana] || h.diaSemana} {h.horaInicio}-{h.horaFim} · {h.quadraNome}</span>
                                <button
                                  onClick={() => handleDesativarHorario(h.id)}
                                  className="text-red-600 text-xs hover:underline"
                                >
                                  Remover horário
                                </button>
                              </li>
                            ))}
                          </ul>
                        </div>

                        <div>
                          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">Cobranças</p>
                          {pagamentos.length === 0 && (
                            <p className="text-xs text-gray-400">
                              Nenhuma cobrança gerada ainda. Cobranças são geradas automaticamente todo dia 1h25 da manhã.
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
                                      value={formaSelecionadaMatricula[p.id] || 'PIX'}
                                      onChange={(e) => setFormaSelecionadaMatricula((prev) => ({ ...prev, [p.id]: e.target.value }))}
                                    >
                                      {FORMAS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                                    </select>
                                    <button
                                      onClick={() => handlePagarMatricula(m.id, p.id)}
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
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}
