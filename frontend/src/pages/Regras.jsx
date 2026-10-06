import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { SlidersHorizontal, Trash2 } from 'lucide-react'
import { regrasService } from '../services/regrasService.js'
import { quadraService } from '../services/quadraService.js'

const DIAS = [
  { value: 'MONDAY', label: 'Seg' },
  { value: 'TUESDAY', label: 'Ter' },
  { value: 'WEDNESDAY', label: 'Qua' },
  { value: 'THURSDAY', label: 'Qui' },
  { value: 'FRIDAY', label: 'Sex' },
  { value: 'SATURDAY', label: 'Sáb' },
  { value: 'SUNDAY', label: 'Dom' },
]
const diaLabel = (v) => DIAS.find((d) => d.value === v)?.label ?? v

const ABAS = [
  { id: 'cancelamento', label: 'Cancelamento e descontos' },
  { id: 'precos', label: 'Preço por horário' },
  { id: 'bloqueios', label: 'Bloqueios de quadra' },
  { id: 'espera', label: 'Lista de espera' },
]

const input = 'w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde'
const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function Regras() {
  const [aba, setAba] = useState('cancelamento')
  const [quadras, setQuadras] = useState([])
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const [config, setConfig] = useState(null)
  const [precos, setPrecos] = useState([])
  const [bloqueios, setBloqueios] = useState([])
  const [espera, setEspera] = useState([])

  const [formPreco, setFormPreco] = useState({ nome: '', quadraId: '', diasSemana: [], horaInicio: '18:00', horaFim: '22:00', valorHora: '' })
  const [formBloqueio, setFormBloqueio] = useState({ quadraId: '', inicio: '', fim: '', motivo: '' })

  async function carregar() {
    try {
      const [c, p, b, e, q] = await Promise.all([
        regrasService.configuracao(),
        regrasService.precos(),
        regrasService.bloqueios(),
        regrasService.listaEspera(),
        quadraService.listar(),
      ])
      setConfig(c)
      setPrecos(p)
      setBloqueios(b)
      setEspera(e)
      setQuadras(q)
    } catch {
      setErro('Não foi possível carregar as regras.')
    }
  }

  useEffect(() => { carregar() }, [])

  function falha(err, padrao) {
    setSucesso('')
    setErro(err.response?.data?.mensagem || padrao)
  }

  async function salvarConfig(e) {
    e.preventDefault()
    setErro('')
    try {
      await regrasService.salvarConfiguracao({
        horasCancelamentoGratis: Number(config.horasCancelamentoGratis),
        percentualMulta: Number(config.percentualMulta),
        percentualMultaNoShow: Number(config.percentualMultaNoShow),
        descontoMensalistaPercentual: Number(config.descontoMensalistaPercentual),
        listaEsperaAtiva: !!config.listaEsperaAtiva,
        horasAvisoFaltaAula: Number(config.horasAvisoFaltaAula),
      })
      setSucesso('Regras salvas.')
    } catch (err) {
      falha(err, 'Não foi possível salvar as regras.')
    }
  }

  async function criarPreco(e) {
    e.preventDefault()
    setErro('')
    try {
      await regrasService.criarPreco({
        nome: formPreco.nome,
        quadraId: formPreco.quadraId ? Number(formPreco.quadraId) : null,
        diasSemana: formPreco.diasSemana,
        horaInicio: formPreco.horaInicio,
        horaFim: formPreco.horaFim,
        valorHora: Number(formPreco.valorHora),
        ativa: true,
      })
      setFormPreco({ nome: '', quadraId: '', diasSemana: [], horaInicio: '18:00', horaFim: '22:00', valorHora: '' })
      setSucesso('Regra de preço criada.')
      carregar()
    } catch (err) {
      falha(err, 'Não foi possível criar a regra de preço.')
    }
  }

  async function alternarPreco(r) {
    try {
      await regrasService.atualizarPreco(r.id, {
        nome: r.nome, quadraId: r.quadraId, diasSemana: r.diasSemana,
        horaInicio: r.horaInicio, horaFim: r.horaFim, valorHora: r.valorHora, ativa: !r.ativa,
      })
      carregar()
    } catch (err) {
      falha(err, 'Não foi possível atualizar a regra.')
    }
  }

  async function removerPreco(id) {
    if (!confirm('Excluir esta regra de preço?')) return
    try { await regrasService.removerPreco(id); carregar() } catch (err) { falha(err, 'Não foi possível excluir.') }
  }

  async function criarBloqueio(e) {
    e.preventDefault()
    setErro('')
    try {
      const criado = await regrasService.criarBloqueio({
        quadraId: formBloqueio.quadraId ? Number(formBloqueio.quadraId) : null,
        inicio: formBloqueio.inicio + ':00',
        fim: formBloqueio.fim + ':00',
        motivo: formBloqueio.motivo,
      })
      setFormBloqueio({ quadraId: '', inicio: '', fim: '', motivo: '' })
      setSucesso(criado.reservasAfetadas > 0
        ? `Bloqueio criado, mas ${criado.reservasAfetadas} reserva(s) já confirmada(s) caem nele — cancele ou remarque manualmente na Agenda.`
        : 'Bloqueio criado.')
      carregar()
    } catch (err) {
      falha(err, 'Não foi possível criar o bloqueio.')
    }
  }

  async function removerBloqueio(id) {
    if (!confirm('Remover este bloqueio?')) return
    try { await regrasService.removerBloqueio(id); carregar() } catch (err) { falha(err, 'Não foi possível remover.') }
  }

  async function removerEspera(id) {
    try { await regrasService.removerDaListaEspera(id); carregar() } catch (err) { falha(err, 'Não foi possível remover.') }
  }

  function campoNumero(chave, rotulo, sufixo, ajuda) {
    return (
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">{rotulo}</label>
        <div className="flex items-center gap-2">
          <input type="number" min="0" step="any" className={input} value={config[chave]}
            onChange={(e) => setConfig({ ...config, [chave]: e.target.value })} required />
          <span className="text-sm text-gray-500 whitespace-nowrap">{sufixo}</span>
        </div>
        {ajuda && <p className="text-xs text-gray-400 mt-1">{ajuda}</p>}
      </div>
    )
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <SlidersHorizontal size={22} className="text-araca-verde-escuro" />
        Regras de reserva
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

      {aba === 'cancelamento' && config && (
        <form onSubmit={salvarConfig} className="bg-white rounded-xl shadow p-6 max-w-2xl space-y-4">
          <div className="grid sm:grid-cols-2 gap-4">
            {campoNumero('horasCancelamentoGratis', 'Cancelamento gratuito até', 'horas antes', 'Depois desse prazo, cobra multa.')}
            {campoNumero('percentualMulta', 'Multa de cancelamento tardio', '% da reserva')}
            {campoNumero('percentualMultaNoShow', 'Multa de não comparecimento', '% da reserva')}
            {campoNumero('descontoMensalistaPercentual', 'Desconto para mensalista', '%', 'Vale para clientes com mensalidade ativa.')}
            {campoNumero('horasAvisoFaltaAula', 'Aviso de falta em aula de pacote', 'horas antes', 'Dentro do prazo, o crédito do pacote é devolvido.')}
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input type="checkbox" checked={!!config.listaEsperaAtiva}
              onChange={(e) => setConfig({ ...config, listaEsperaAtiva: e.target.checked })} />
            Lista de espera ativa (avisa por e-mail quando um horário liberar)
          </label>
          <p className="text-xs text-gray-400">
            Reservas de recorrência/mensalidade e aulas de pacote não pagam multa automaticamente. A recepção pode isentar a multa caso a caso na Agenda.
          </p>
          <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-5 py-2 rounded-lg hover:opacity-90 transition">
            Salvar regras
          </button>
        </form>
      )}

      {aba === 'precos' && (
        <div className="space-y-6">
          <form onSubmit={criarPreco} className="bg-white rounded-xl shadow p-6 grid sm:grid-cols-6 gap-3 items-end">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
              <input className={input} placeholder="Ex.: Horário de pico" value={formPreco.nome}
                onChange={(e) => setFormPreco({ ...formPreco, nome: e.target.value })} required />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Quadra</label>
              <select className={input} value={formPreco.quadraId} onChange={(e) => setFormPreco({ ...formPreco, quadraId: e.target.value })}>
                <option value="">Todas as quadras</option>
                {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Das</label>
              <input type="time" className={input} value={formPreco.horaInicio} onChange={(e) => setFormPreco({ ...formPreco, horaInicio: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
              <input type="time" className={input} value={formPreco.horaFim} onChange={(e) => setFormPreco({ ...formPreco, horaFim: e.target.value })} required />
            </div>
            <div className="sm:col-span-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Dias (vazio = todos)</label>
              <div className="flex flex-wrap gap-1.5">
                {DIAS.map((d) => {
                  const ativo = formPreco.diasSemana.includes(d.value)
                  return (
                    <button type="button" key={d.value}
                      onClick={() => setFormPreco({ ...formPreco, diasSemana: ativo ? formPreco.diasSemana.filter((x) => x !== d.value) : [...formPreco.diasSemana, d.value] })}
                      className={`px-2.5 py-1 rounded-full text-xs border ${ativo ? 'bg-araca-verde border-araca-verde text-araca-azul font-semibold' : 'bg-white text-gray-500 border-gray-200'}`}>
                      {d.label}
                    </button>
                  )
                })}
              </div>
            </div>
            <div className="sm:col-span-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Valor/hora (R$)</label>
              <input type="number" min="0.01" step="0.01" className={input} value={formPreco.valorHora}
                onChange={(e) => setFormPreco({ ...formPreco, valorHora: e.target.value })} required />
            </div>
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">Adicionar</button>
          </form>

          <div className="bg-white rounded-xl shadow overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr><th className="px-4 py-3">Regra</th><th className="px-4 py-3">Quadra</th><th className="px-4 py-3">Dias</th><th className="px-4 py-3">Horário</th><th className="px-4 py-3">Valor/hora</th><th className="px-4 py-3" /></tr>
              </thead>
              <tbody>
                {precos.length === 0 && (
                  <tr><td colSpan="6" className="px-4 py-6 text-gray-400 text-center">Sem regras: todas as reservas usam o valor/hora cadastrado na quadra.</td></tr>
                )}
                {precos.map((r) => (
                  <tr key={r.id} className={`border-t ${r.ativa ? '' : 'opacity-50'}`}>
                    <td className="px-4 py-3 font-medium text-araca-azul">{r.nome}</td>
                    <td className="px-4 py-3">{r.quadraNome}</td>
                    <td className="px-4 py-3">{r.diasSemana.length === 0 ? 'Todos' : r.diasSemana.map(diaLabel).join(', ')}</td>
                    <td className="px-4 py-3">{r.horaInicio.slice(0, 5)}–{r.horaFim.slice(0, 5)}</td>
                    <td className="px-4 py-3">{brl(r.valorHora)}</td>
                    <td className="px-4 py-3 text-right whitespace-nowrap">
                      <button onClick={() => alternarPreco(r)} className="text-xs text-araca-azul underline mr-3">{r.ativa ? 'Desativar' : 'Ativar'}</button>
                      <button onClick={() => removerPreco(r.id)} aria-label="Excluir" className="text-gray-400 hover:text-red-600"><Trash2 size={16} /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="text-xs text-gray-400">Se várias regras valerem para o mesmo horário, vale a da quadra específica; entre iguais, a mais recente. O preço é calculado em blocos de 15 minutos.</p>
        </div>
      )}

      {aba === 'bloqueios' && (
        <div className="space-y-6">
          <form onSubmit={criarBloqueio} className="bg-white rounded-xl shadow p-6 grid sm:grid-cols-5 gap-3 items-end">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Quadra</label>
              <select className={input} value={formBloqueio.quadraId} onChange={(e) => setFormBloqueio({ ...formBloqueio, quadraId: e.target.value })}>
                <option value="">Todas</option>
                {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Início</label>
              <input type="datetime-local" className={input} value={formBloqueio.inicio} onChange={(e) => setFormBloqueio({ ...formBloqueio, inicio: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Fim</label>
              <input type="datetime-local" className={input} value={formBloqueio.fim} onChange={(e) => setFormBloqueio({ ...formBloqueio, fim: e.target.value })} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Motivo</label>
              <input className={input} placeholder="Manutenção, evento..." value={formBloqueio.motivo} onChange={(e) => setFormBloqueio({ ...formBloqueio, motivo: e.target.value })} />
            </div>
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">Bloquear</button>
          </form>

          <div className="bg-white rounded-xl shadow overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr><th className="px-4 py-3">Quadra</th><th className="px-4 py-3">Período</th><th className="px-4 py-3">Motivo</th><th className="px-4 py-3">Reservas no período</th><th className="px-4 py-3" /></tr>
              </thead>
              <tbody>
                {bloqueios.length === 0 && <tr><td colSpan="5" className="px-4 py-6 text-gray-400 text-center">Nenhum bloqueio futuro.</td></tr>}
                {bloqueios.map((b) => (
                  <tr key={b.id} className="border-t">
                    <td className="px-4 py-3 font-medium text-araca-azul">{b.quadraNome}</td>
                    <td className="px-4 py-3">{format(new Date(b.inicio), 'dd/MM HH:mm')} → {format(new Date(b.fim), 'dd/MM HH:mm')}</td>
                    <td className="px-4 py-3">{b.motivo || '—'}</td>
                    <td className="px-4 py-3">{b.reservasAfetadas > 0 ? <span className="text-red-600 font-medium">{b.reservasAfetadas} para tratar</span> : '—'}</td>
                    <td className="px-4 py-3 text-right">
                      <button onClick={() => removerBloqueio(b.id)} aria-label="Remover" className="text-gray-400 hover:text-red-600"><Trash2 size={16} /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="text-xs text-gray-400">Bloqueios impedem novas reservas (painel e portal) e a geração de recorrências nesses horários. Reservas já confirmadas não são canceladas automaticamente.</p>
        </div>
      )}

      {aba === 'espera' && (
        <div className="bg-white rounded-xl shadow overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-gray-500">
              <tr><th className="px-4 py-3">Cliente</th><th className="px-4 py-3">Quadra</th><th className="px-4 py-3">Horário</th><th className="px-4 py-3">Situação</th><th className="px-4 py-3" /></tr>
            </thead>
            <tbody>
              {espera.length === 0 && <tr><td colSpan="5" className="px-4 py-6 text-gray-400 text-center">Ninguém na lista de espera.</td></tr>}
              {espera.map((e) => (
                <tr key={e.id} className="border-t">
                  <td className="px-4 py-3 font-medium text-araca-azul">{e.clienteNome}</td>
                  <td className="px-4 py-3">{e.quadraNome}</td>
                  <td className="px-4 py-3">{format(new Date(e.inicio), 'dd/MM HH:mm')}–{format(new Date(e.fim), 'HH:mm')}</td>
                  <td className="px-4 py-3">{e.status === 'NOTIFICADO' ? 'Avisado (vaga liberou)' : 'Aguardando'}</td>
                  <td className="px-4 py-3 text-right">
                    <button onClick={() => removerEspera(e.id)} aria-label="Remover" className="text-gray-400 hover:text-red-600"><Trash2 size={16} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
