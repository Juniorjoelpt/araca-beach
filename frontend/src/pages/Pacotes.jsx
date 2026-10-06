import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { Package, ChevronDown, ChevronUp } from 'lucide-react'
import { pacoteService } from '../services/pacoteService.js'
import { clienteService } from '../services/clienteService.js'
import { professorService } from '../services/professorService.js'
import { quadraService } from '../services/quadraService.js'

const FORMAS = [
  { value: 'PIX', label: 'Pix' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
]

const TIPOS = [
  { value: '', label: 'Qualquer modalidade' },
  { value: 'VOLEI', label: 'Vôlei' },
  { value: 'FUTEVOLEI', label: 'Futevôlei' },
  { value: 'BEACH_TENNIS', label: 'Beach Tennis' },
]

const SITUACAO_COR = {
  ATIVO: 'bg-green-100 text-green-700',
  ESGOTADO: 'bg-gray-100 text-gray-500',
  VENCIDO: 'bg-amber-100 text-amber-700',
  CANCELADO: 'bg-red-100 text-red-700',
}

const STATUS_AULA = {
  AGENDADA: 'Agendada',
  REALIZADA: 'Realizada',
  FALTA_AVISADA: 'Falta avisada',
  FALTA_SEM_AVISO: 'Falta sem aviso',
  CANCELADA: 'Cancelada',
}

const input = 'w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde'
const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function Pacotes() {
  const [aba, setAba] = useState('vendas')
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const [planos, setPlanos] = useState([])
  const [vendas, setVendas] = useState([])
  const [clientes, setClientes] = useState([])
  const [professores, setProfessores] = useState([])
  const [quadras, setQuadras] = useState([])

  const [formPlano, setFormPlano] = useState({ nome: '', tipo: '', quantidadeAulas: 10, valor: '', validadeDias: 90 })
  const [formVenda, setFormVenda] = useState({ clienteId: '', planoId: '', formaPagamento: '' })

  const [expandido, setExpandido] = useState(null)
  const [aulasDoPacote, setAulasDoPacote] = useState([])
  const [formAula, setFormAula] = useState({ professorId: '', quadraId: '', data: format(new Date(), 'yyyy-MM-dd'), hora: '18:00', duracao: 60, reposicaoDeAulaId: '' })

  const [dataChamada, setDataChamada] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [aulasDia, setAulasDia] = useState([])

  function falha(err, padrao) {
    setSucesso('')
    setErro(err.response?.data?.mensagem || padrao)
  }

  async function carregarBase() {
    try {
      const [p, v, c, pr, q] = await Promise.all([
        pacoteService.planos(),
        pacoteService.vendas(),
        clienteService.listar(),
        professorService.listar(),
        quadraService.listar(),
      ])
      setPlanos(p)
      setVendas(v)
      setClientes(c)
      setProfessores(pr)
      setQuadras(q)
    } catch {
      setErro('Não foi possível carregar os pacotes.')
    }
  }

  async function carregarChamada() {
    try {
      setAulasDia(await pacoteService.aulasDoDia(dataChamada))
    } catch {
      setErro('Não foi possível carregar as aulas do dia.')
    }
  }

  useEffect(() => { carregarBase() }, [])
  useEffect(() => { if (aba === 'chamada') carregarChamada() }, [aba, dataChamada])

  async function salvarPlano(e) {
    e.preventDefault()
    setErro('')
    try {
      await pacoteService.criarPlano({
        nome: formPlano.nome,
        tipo: formPlano.tipo || null,
        quantidadeAulas: Number(formPlano.quantidadeAulas),
        valor: Number(formPlano.valor),
        validadeDias: Number(formPlano.validadeDias),
        ativo: true,
      })
      setFormPlano({ nome: '', tipo: '', quantidadeAulas: 10, valor: '', validadeDias: 90 })
      setSucesso('Plano criado.')
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível criar o plano.')
    }
  }

  async function alternarPlano(p) {
    try {
      await pacoteService.atualizarPlano(p.id, {
        nome: p.nome, tipo: p.tipo, quantidadeAulas: p.quantidadeAulas,
        valor: p.valor, validadeDias: p.validadeDias, ativo: !p.ativo,
      })
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível atualizar o plano.')
    }
  }

  async function vender(e) {
    e.preventDefault()
    setErro('')
    try {
      await pacoteService.vender({
        clienteId: Number(formVenda.clienteId),
        planoId: Number(formVenda.planoId),
        formaPagamento: formVenda.formaPagamento || null,
      })
      setFormVenda({ clienteId: '', planoId: '', formaPagamento: '' })
      setSucesso(formVenda.formaPagamento ? 'Pacote vendido e pago.' : 'Pacote vendido — pagamento pendente (aparece no Financeiro).')
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível vender o pacote.')
    }
  }

  async function pagar(v, forma) {
    if (!forma) return
    try {
      await pacoteService.pagar(v.id, forma)
      setSucesso('Pagamento registrado.')
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível registrar o pagamento.')
    }
  }

  async function cancelarPacote(v) {
    if (!confirm('Cancelar este pacote? As aulas ainda agendadas serão canceladas. Estorno de valores não é automático.')) return
    try {
      await pacoteService.cancelar(v.id)
      carregarBase()
      if (expandido === v.id) setAulasDoPacote(await pacoteService.aulasDoPacote(v.id))
    } catch (err) {
      falha(err, 'Não foi possível cancelar o pacote.')
    }
  }

  async function alternarExpandido(v) {
    if (expandido === v.id) {
      setExpandido(null)
      return
    }
    setExpandido(v.id)
    setFormAula({ ...formAula, reposicaoDeAulaId: '' })
    try {
      setAulasDoPacote(await pacoteService.aulasDoPacote(v.id))
    } catch {
      setErro('Não foi possível carregar as aulas do pacote.')
    }
  }

  async function agendarAula(e, v) {
    e.preventDefault()
    setErro('')
    const inicio = new Date(`${formAula.data}T${formAula.hora}:00`)
    const fim = new Date(inicio.getTime() + formAula.duracao * 60000)
    try {
      await pacoteService.agendarAula(v.id, {
        professorId: Number(formAula.professorId),
        quadraId: Number(formAula.quadraId),
        inicio: format(inicio, "yyyy-MM-dd'T'HH:mm:ss"),
        fim: format(fim, "yyyy-MM-dd'T'HH:mm:ss"),
        reposicaoDeAulaId: formAula.reposicaoDeAulaId ? Number(formAula.reposicaoDeAulaId) : null,
      })
      setSucesso('Aula agendada (a quadra foi reservada).')
      setFormAula({ ...formAula, reposicaoDeAulaId: '' })
      setAulasDoPacote(await pacoteService.aulasDoPacote(v.id))
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível agendar a aula.')
    }
  }

  async function marcar(aula, status) {
    setErro('')
    try {
      await pacoteService.presenca(aula.id, status)
      setSucesso('Chamada registrada.')
      if (aba === 'chamada') carregarChamada()
      if (expandido) setAulasDoPacote(await pacoteService.aulasDoPacote(expandido))
      carregarBase()
    } catch (err) {
      falha(err, 'Não foi possível registrar a chamada.')
    }
  }

  function botoesChamada(aula) {
    if (aula.status !== 'AGENDADA') return null
    const passou = new Date(aula.inicio) <= new Date()
    return (
      <div className="flex flex-wrap gap-1.5">
        {passou && <button onClick={() => marcar(aula, 'REALIZADA')} className="px-2 py-1 rounded bg-green-100 text-green-700 text-xs font-medium">Presente</button>}
        {passou && <button onClick={() => marcar(aula, 'FALTA_SEM_AVISO')} className="px-2 py-1 rounded bg-red-100 text-red-700 text-xs font-medium">Faltou</button>}
        <button onClick={() => marcar(aula, 'FALTA_AVISADA')} className="px-2 py-1 rounded bg-amber-100 text-amber-700 text-xs font-medium">Faltou (avisou)</button>
        <button onClick={() => marcar(aula, 'CANCELADA')} className="px-2 py-1 rounded bg-gray-100 text-gray-600 text-xs font-medium">Cancelar aula</button>
      </div>
    )
  }

  const planosAtivos = planos.filter((p) => p.ativo)
  const ABAS = [
    { id: 'vendas', label: 'Pacotes vendidos' },
    { id: 'chamada', label: 'Chamada do dia' },
    { id: 'planos', label: 'Planos' },
  ]

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Package size={22} className="text-araca-verde-escuro" />
        Pacotes de aulas
      </h2>

      <div className="flex flex-wrap gap-2 mb-6">
        {ABAS.map((a) => (
          <button key={a.id} onClick={() => { setAba(a.id); setErro(''); setSucesso('') }}
            className={`px-4 py-2 rounded-lg text-sm font-medium border transition ${
              aba === a.id ? 'bg-araca-verde border-araca-verde text-araca-azul' : 'bg-white text-gray-600 border-gray-200'
            }`}>
            {a.label}
          </button>
        ))}
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}
      {sucesso && <p className="text-green-700 text-sm mb-4">{sucesso}</p>}

      {aba === 'planos' && (
        <div className="space-y-6">
          <form onSubmit={salvarPlano} className="bg-white rounded-xl shadow p-6 grid sm:grid-cols-6 gap-3 items-end">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
              <input className={input} placeholder="Ex.: 10 aulas de Futevôlei" value={formPlano.nome} onChange={(e) => setFormPlano({ ...formPlano, nome: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Modalidade</label>
              <select className={input} value={formPlano.tipo} onChange={(e) => setFormPlano({ ...formPlano, tipo: e.target.value })}>
                {TIPOS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Aulas</label>
              <input type="number" min="1" className={input} value={formPlano.quantidadeAulas} onChange={(e) => setFormPlano({ ...formPlano, quantidadeAulas: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Valor (R$)</label>
              <input type="number" min="0.01" step="0.01" className={input} value={formPlano.valor} onChange={(e) => setFormPlano({ ...formPlano, valor: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Validade (dias)</label>
              <input type="number" min="1" className={input} value={formPlano.validadeDias} onChange={(e) => setFormPlano({ ...formPlano, validadeDias: e.target.value })} required />
            </div>
            <button type="submit" className="sm:col-span-6 sm:w-40 bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">Criar plano</button>
          </form>

          <div className="bg-white rounded-xl shadow overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr><th className="px-4 py-3">Plano</th><th className="px-4 py-3">Aulas</th><th className="px-4 py-3">Valor</th><th className="px-4 py-3">Por aula</th><th className="px-4 py-3">Validade</th><th className="px-4 py-3" /></tr>
              </thead>
              <tbody>
                {planos.length === 0 && <tr><td colSpan="6" className="px-4 py-6 text-gray-400 text-center">Nenhum plano criado.</td></tr>}
                {planos.map((p) => (
                  <tr key={p.id} className={`border-t ${p.ativo ? '' : 'opacity-50'}`}>
                    <td className="px-4 py-3 font-medium text-araca-azul">{p.nome}</td>
                    <td className="px-4 py-3">{p.quantidadeAulas}</td>
                    <td className="px-4 py-3">{brl(p.valor)}</td>
                    <td className="px-4 py-3">{brl(p.valor / p.quantidadeAulas)}</td>
                    <td className="px-4 py-3">{p.validadeDias} dias</td>
                    <td className="px-4 py-3 text-right"><button onClick={() => alternarPlano(p)} className="text-xs text-araca-azul underline">{p.ativo ? 'Desativar' : 'Ativar'}</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {aba === 'vendas' && (
        <div className="space-y-6">
          <form onSubmit={vender} className="bg-white rounded-xl shadow p-6 grid sm:grid-cols-4 gap-3 items-end">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Cliente</label>
              <select className={input} value={formVenda.clienteId} onChange={(e) => setFormVenda({ ...formVenda, clienteId: e.target.value })} required>
                <option value="">Selecione...</option>
                {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Plano</label>
              <select className={input} value={formVenda.planoId} onChange={(e) => setFormVenda({ ...formVenda, planoId: e.target.value })} required>
                <option value="">Selecione...</option>
                {planosAtivos.map((p) => <option key={p.id} value={p.id}>{p.nome} — {brl(p.valor)}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Pagamento</label>
              <select className={input} value={formVenda.formaPagamento} onChange={(e) => setFormVenda({ ...formVenda, formaPagamento: e.target.value })}>
                <option value="">Pagar depois</option>
                {FORMAS.map((f) => <option key={f.value} value={f.value}>Pago agora — {f.label}</option>)}
              </select>
            </div>
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">Vender pacote</button>
          </form>

          <div className="space-y-3">
            {vendas.length === 0 && <p className="text-gray-400 text-sm">Nenhum pacote vendido ainda.</p>}
            {vendas.map((v) => (
              <div key={v.id} className="bg-white rounded-xl shadow">
                <div className="p-4 flex flex-wrap items-center gap-4 justify-between">
                  <div className="min-w-0">
                    <p className="font-semibold text-araca-azul">{v.clienteNome}</p>
                    <p className="text-sm text-gray-500">{v.planoNome} · {brl(v.valor)} · vence {format(new Date(v.validade + 'T00:00:00'), 'dd/MM/yyyy')}</p>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-center">
                      <p className="text-2xl font-bold text-araca-verde-escuro leading-none">{v.saldo}</p>
                      <p className="text-[11px] text-gray-400">de {v.aulasTotal} aulas</p>
                    </div>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${SITUACAO_COR[v.situacao]}`}>{v.situacao}</span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${v.pago ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{v.pago ? 'Pago' : 'Pagamento pendente'}</span>
                    {!v.pago && v.situacao !== 'CANCELADO' && (
                      <select className="border rounded px-2 py-1 text-xs" value="" onChange={(e) => pagar(v, e.target.value)}>
                        <option value="">Registrar pagamento...</option>
                        {FORMAS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                      </select>
                    )}
                    {v.situacao !== 'CANCELADO' && <button onClick={() => cancelarPacote(v)} className="text-xs text-red-600 underline">Cancelar</button>}
                    <button onClick={() => alternarExpandido(v)} aria-label="Aulas" className="text-gray-500">
                      {expandido === v.id ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                    </button>
                  </div>
                </div>

                {expandido === v.id && (
                  <div className="border-t p-4 space-y-4">
                    {v.situacao !== 'CANCELADO' && v.saldo > 0 && (
                      <form onSubmit={(e) => agendarAula(e, v)} className="grid sm:grid-cols-6 gap-2 items-end">
                        <div className="sm:col-span-1">
                          <label className="block text-xs text-gray-600 mb-1">Professor</label>
                          <select className={input} value={formAula.professorId} onChange={(e) => setFormAula({ ...formAula, professorId: e.target.value })} required>
                            <option value="">...</option>
                            {professores.filter((p) => p.ativo !== false).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Quadra</label>
                          <select className={input} value={formAula.quadraId} onChange={(e) => setFormAula({ ...formAula, quadraId: e.target.value })} required>
                            <option value="">...</option>
                            {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Data</label>
                          <input type="date" className={input} value={formAula.data} onChange={(e) => setFormAula({ ...formAula, data: e.target.value })} required />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Hora</label>
                          <input type="time" className={input} value={formAula.hora} onChange={(e) => setFormAula({ ...formAula, hora: e.target.value })} required />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Duração</label>
                          <select className={input} value={formAula.duracao} onChange={(e) => setFormAula({ ...formAula, duracao: Number(e.target.value) })}>
                            <option value={60}>1 hora</option>
                            <option value={90}>1h30</option>
                            <option value={120}>2 horas</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Reposição de</label>
                          <select className={input} value={formAula.reposicaoDeAulaId} onChange={(e) => setFormAula({ ...formAula, reposicaoDeAulaId: e.target.value })}>
                            <option value="">— (aula normal)</option>
                            {aulasDoPacote.filter((a) => a.status === 'FALTA_AVISADA').map((a) => (
                              <option key={a.id} value={a.id}>{format(new Date(a.inicio), 'dd/MM HH:mm')}</option>
                            ))}
                          </select>
                        </div>
                        <button type="submit" className="sm:col-span-6 sm:w-48 bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">Agendar aula</button>
                      </form>
                    )}

                    <table className="w-full text-sm">
                      <thead className="text-left text-gray-500"><tr><th className="py-2">Aula</th><th>Professor</th><th>Quadra</th><th>Situação</th><th /></tr></thead>
                      <tbody>
                        {aulasDoPacote.length === 0 && <tr><td colSpan="5" className="py-3 text-gray-400">Nenhuma aula agendada.</td></tr>}
                        {aulasDoPacote.map((a) => (
                          <tr key={a.id} className="border-t">
                            <td className="py-2">{format(new Date(a.inicio), 'dd/MM/yyyy HH:mm')}{a.reposicaoDeAulaId && <span className="text-xs text-gray-400"> (reposição)</span>}</td>
                            <td>{a.professorNome}</td>
                            <td>{a.quadraNome}</td>
                            <td>{STATUS_AULA[a.status] || a.status}</td>
                            <td>{botoesChamada(a)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {aba === 'chamada' && (
        <div className="space-y-4">
          <div className="bg-white rounded-xl shadow p-4 w-fit">
            <label className="block text-sm font-medium text-gray-700 mb-1">Data</label>
            <input type="date" className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde" value={dataChamada} onChange={(e) => setDataChamada(e.target.value)} />
          </div>
          <div className="bg-white rounded-xl shadow overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr><th className="px-4 py-3">Horário</th><th className="px-4 py-3">Aluno</th><th className="px-4 py-3">Pacote</th><th className="px-4 py-3">Professor</th><th className="px-4 py-3">Quadra</th><th className="px-4 py-3">Situação</th><th className="px-4 py-3">Chamada</th></tr>
              </thead>
              <tbody>
                {aulasDia.length === 0 && <tr><td colSpan="7" className="px-4 py-6 text-gray-400 text-center">Nenhuma aula de pacote nesta data.</td></tr>}
                {aulasDia.map((a) => (
                  <tr key={a.id} className="border-t">
                    <td className="px-4 py-3">{format(new Date(a.inicio), 'HH:mm')}–{format(new Date(a.fim), 'HH:mm')}</td>
                    <td className="px-4 py-3 font-medium text-araca-azul">{a.clienteNome}</td>
                    <td className="px-4 py-3">{a.planoNome}</td>
                    <td className="px-4 py-3">{a.professorNome}</td>
                    <td className="px-4 py-3">{a.quadraNome}</td>
                    <td className="px-4 py-3">{STATUS_AULA[a.status] || a.status}</td>
                    <td className="px-4 py-3">{botoesChamada(a)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
