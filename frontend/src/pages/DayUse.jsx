import { useCallback, useEffect, useMemo, useState } from 'react'
import { addDays, format, parseISO } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { Ticket, Plus, Minus, X, Printer, ChevronLeft, ChevronRight, Settings } from 'lucide-react'
import { dayUseService } from '../services/dayUseService.js'
import { imprimirDayUse } from '../utils/ticketTermico.js'

const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const iso = (d) => format(d, 'yyyy-MM-dd')
const msg = (err, padrao) => err?.response?.data?.mensagem || padrao
const FORMAS = [['PIX', 'Pix'], ['DINHEIRO', 'Dinheiro'], ['CARTAO_CREDITO', 'Cartão de crédito'], ['CARTAO_DEBITO', 'Cartão de débito']]
const FORMA_LABEL = Object.fromEntries(FORMAS)
const num = (t) => Number(String(t).replace(',', '.'))

function usuarioAtual() {
  try { return JSON.parse(localStorage.getItem('araca_beach_usuario')) } catch { return null }
}

export default function DayUse() {
  const admin = usuarioAtual()?.perfil === 'ADMIN'
  const [data, setData] = useState(iso(new Date()))
  const [dia, setDia] = useState(null)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [nova, setNova] = useState(false)
  const [config, setConfig] = useState(false)
  const [confirmando, setConfirmando] = useState(null)

  const carregar = useCallback(async () => {
    try { setDia(await dayUseService.dia(data)); setErro('') } catch (err) { setErro(msg(err, 'Não foi possível carregar o Day Use.')) }
  }, [data])
  useEffect(() => { carregar() }, [carregar])
  // portaria: atualiza sozinho para mostrar vendas feitas em outro computador
  useEffect(() => {
    const t = setInterval(carregar, 30000)
    return () => clearInterval(t)
  }, [carregar])

  async function acao(fn, ok) {
    setErro('')
    try { await fn(); setAviso(ok || ''); await carregar() } catch (err) { setErro(msg(err, 'Operação não concluída.')) }
  }

  function entrada(v) {
    const restam = v.pessoas - v.entraram
    let q = 1
    if (restam > 1) {
      const resp = window.prompt(`Quantas pessoas estão entrando agora? (restam ${restam})`, String(restam))
      if (resp === null) return
      q = parseInt(resp, 10)
      if (!q || q < 1) return
    }
    acao(() => dayUseService.entrada(v.id, q), `Entrada registrada (${q}).`)
  }

  function cancelar(v) {
    const motivo = window.prompt(`Cancelar a venda #${v.id} (${v.pessoas} pessoa(s), ${brl(v.valorTotal)})? O valor sai do caixa.\n\nMotivo (opcional):`)
    if (motivo === null) return
    acao(() => dayUseService.cancelar(v.id, motivo), `Venda #${v.id} cancelada.`)
  }

  const r = dia?.resumo
  const hoje = iso(new Date())
  const tituloData = format(parseISO(data), "EEEE, dd 'de' MMMM", { locale: ptBR })

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h2 className="font-title text-2xl text-araca-verde flex items-center gap-2"><Ticket size={24} /> Day Use</h2>
        <div className="flex items-center gap-2">
          {admin && (
            <button onClick={() => setConfig(true)} className="bg-white text-araca-azul border px-3 py-2 rounded-lg text-sm flex items-center gap-1.5"><Settings size={16} /> Ingressos e capacidade</button>
          )}
          <button onClick={() => setNova(true)} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg flex items-center gap-2"><Plus size={18} /> Nova venda</button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 mb-4">
        <button onClick={() => setData(iso(addDays(parseISO(data), -1)))} className="p-2 border rounded-lg bg-white" aria-label="Dia anterior"><ChevronLeft size={16} /></button>
        <input type="date" value={data} onChange={(e) => e.target.value && setData(e.target.value)} className="border rounded-lg px-2 py-1.5 text-sm" />
        <button onClick={() => setData(iso(addDays(parseISO(data), 1)))} className="p-2 border rounded-lg bg-white" aria-label="Próximo dia"><ChevronRight size={16} /></button>
        {data !== hoje && <button onClick={() => setData(hoje)} className="text-sm underline text-araca-azul">Hoje</button>}
        <span className="text-sm text-gray-600 capitalize">{tituloData}</span>
      </div>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {aviso && <p className="text-green-700 text-sm mb-3">{aviso}</p>}

      {r && (
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 mb-4">
          <Tile rotulo="Ingressos vendidos" valor={r.pessoasVendidas} sub={`${r.capacidade ? `de ${r.capacidade} (restam ${r.vagasRestantes})` : 'sem limite de capacidade'}${r.pessoasReservadas ? ` · ${r.pessoasReservadas} reservado(s) aguardando pagamento` : ''}`} alerta={r.capacidade && r.vagasRestantes === 0} />
          <Tile rotulo="Já entraram" valor={r.pessoasEntraram} sub={`${r.pessoasVendidas - r.pessoasEntraram} ainda por entrar`} />
          <Tile rotulo="Vendas" valor={r.vendas} sub={r.canceladas ? `${r.canceladas} cancelada(s)` : ''} />
          <Tile rotulo="Valor vendido" valor={brl(r.valorTotal)} sub="entra no caixa na hora da venda" />
        </div>
      )}

      <div className="bg-white rounded-xl shadow divide-y">
        {dia && dia.vendas.length === 0 && <p className="p-4 text-sm text-gray-400">Nenhuma venda para este dia.</p>}
        {dia?.vendas.map((v) => {
          const cancelada = v.status === 'CANCELADA'
          const pendente = v.status === 'PENDENTE'
          const completo = v.entraram >= v.pessoas
          return (
            <div key={v.id} className={`p-3 flex flex-wrap items-center gap-3 ${cancelada ? 'opacity-60' : ''}`}>
              <div className="flex-1 min-w-[220px]">
                <p className={`font-medium text-sm ${cancelada ? 'line-through' : ''}`}>
                  #{v.id} · {v.nomeCliente || 'Sem nome'}{v.telefone ? ` · ${v.telefone}` : ''}
                  {pendente && <span className="ml-2 text-[10px] px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">Aguardando pagamento</span>}
                  {v.origem === 'PORTAL' && <span className="ml-1 text-[10px] px-1.5 py-0.5 rounded bg-blue-100 text-blue-700">Portal</span>}
                </p>
                <p className="text-xs text-gray-500">{v.itens.map((i) => `${i.quantidade}x ${i.tipoNome}${i.pessoasPorUnidade > 1 ? ` (${i.pessoas} pessoas)` : ''}`).join(', ')}{v.forma ? ` · ${FORMA_LABEL[v.forma] || v.forma}` : ''}{v.operador ? ` · ${v.operador}` : ''}</p>
                {v.observacao && <p className="text-xs text-gray-500">Obs.: {v.observacao}</p>}
                {cancelada && <p className="text-xs text-red-600">Cancelada{v.motivoCancelamento ? `: ${v.motivoCancelamento}` : ''}</p>}
              </div>
              <div className="text-right">
                <p className="font-bold text-araca-azul">{brl(v.valorTotal)}</p>
                {!cancelada && !pendente && <p className={`text-xs ${completo ? 'text-green-700' : 'text-gray-500'}`}>Entraram {v.entraram}/{v.pessoas}</p>}
                {pendente && <p className="text-xs text-gray-500">{v.pessoas} pessoa(s)</p>}
              </div>
              {!cancelada && (
                <div className="flex gap-2 text-sm">
                  {pendente && (
                    <button onClick={() => setConfirmando(v)} className="bg-araca-verde text-araca-azul font-semibold px-3 py-1.5 rounded-lg">Confirmar pagamento</button>
                  )}
                  {!pendente && !completo && data === hoje && (
                    <button onClick={() => entrada(v)} className="bg-araca-azul text-white px-3 py-1.5 rounded-lg">Registrar entrada</button>
                  )}
                  {!pendente && <button onClick={() => imprimirDayUse(v)} className="border px-2 py-1.5 rounded-lg" title="Imprimir comprovante"><Printer size={16} /></button>}
                  {v.entraram === 0 && <button onClick={() => cancelar(v)} className="border border-red-300 text-red-600 px-3 py-1.5 rounded-lg">Cancelar</button>}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {nova && (
        <NovaVenda
          dataInicial={data < hoje ? hoje : data}
          onFechar={() => setNova(false)}
          onVendida={(v) => { setNova(false); setAviso(`Venda #${v.id} registrada.`); imprimirDayUse(v); setData(v.data); carregar() }}
        />
      )}
      {confirmando && (
        <ConfirmarPagamento
          venda={confirmando}
          onFechar={() => setConfirmando(null)}
          onConfirmada={(v) => { setConfirmando(null); setAviso(`Pagamento da venda #${v.id} confirmado.`); imprimirDayUse(v); carregar() }}
        />
      )}
      {config && <Configurar onFechar={() => { setConfig(false); carregar() }} />}
    </div>
  )
}

function ConfirmarPagamento({ venda, onFechar, onConfirmada }) {
  const [forma, setForma] = useState('PIX')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  async function confirmar() {
    setSalvando(true)
    setErro('')
    try { onConfirmada(await dayUseService.confirmarPagamento(venda.id, forma)) } catch (err) { setErro(msg(err, 'Não foi possível confirmar.')); setSalvando(false) }
  }
  return (
    <Modal titulo={`Confirmar pagamento · venda #${venda.id}`} onFechar={onFechar}>
      <div className="space-y-3 text-sm">
        <p>{venda.nomeCliente || 'Cliente'} · {venda.pessoas} pessoa(s) · <strong>{brl(venda.valorTotal)}</strong></p>
        <select value={forma} onChange={(e) => setForma(e.target.value)} className="border rounded-lg px-3 py-2 w-full">
          {FORMAS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
        </select>
        <p className="text-xs text-gray-500">O valor entra no caixa agora e a entrada fica liberada na data do ingresso.</p>
        {erro && <p className="text-red-600">{erro}</p>}
        <div className="flex justify-end gap-2">
          <button onClick={onFechar} className="px-4 py-2">Voltar</button>
          <button onClick={confirmar} disabled={salvando} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-50">{salvando ? 'Confirmando...' : 'Confirmar recebimento'}</button>
        </div>
      </div>
    </Modal>
  )
}

function Tile({ rotulo, valor, sub, alerta }) {
  return (
    <div className={`bg-white rounded-xl shadow p-3 ${alerta ? 'ring-2 ring-red-300' : ''}`}>
      <p className="text-xs text-gray-500">{rotulo}</p>
      <p className="text-xl font-bold text-araca-azul">{valor}</p>
      {sub && <p className="text-[11px] text-gray-500">{sub}</p>}
    </div>
  )
}

function Modal({ titulo, onFechar, children }) {
  return (
    <div className="fixed inset-0 bg-black/40 z-30 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl p-5 w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-3">
          <h3 className="font-semibold text-araca-azul text-lg">{titulo}</h3>
          <button onClick={onFechar} aria-label="Fechar"><X size={20} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

function NovaVenda({ dataInicial, onFechar, onVendida }) {
  const [data, setData] = useState(dataInicial)
  const [tipos, setTipos] = useState([])
  const [qtd, setQtd] = useState({})
  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')
  const [obs, setObs] = useState('')
  const [forma, setForma] = useState('PIX')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  useEffect(() => { dayUseService.tipos(data).then(setTipos).catch((e) => setErro(msg(e, 'Não foi possível carregar os ingressos.'))) }, [data])

  const total = useMemo(() => tipos.reduce((s, t) => s + Number(t.preco) * (qtd[t.id] || 0), 0), [tipos, qtd])
  const pessoas = tipos.reduce((s, t) => s + (qtd[t.id] || 0) * (t.pessoasPorUnidade || 1), 0)
  const ingressos = Object.values(qtd).reduce((s, q) => s + q, 0)
  const mudar = (id, d) => setQtd((q) => ({ ...q, [id]: Math.max(0, Math.min(200, (q[id] || 0) + d)) }))

  async function salvar() {
    if (ingressos === 0) { setErro('Escolha ao menos um ingresso.'); return }
    setSalvando(true)
    setErro('')
    try {
      const itens = tipos.filter((t) => qtd[t.id] > 0).map((t) => ({ tipoId: t.id, quantidade: qtd[t.id] }))
      onVendida(await dayUseService.vender({ data, nomeCliente: nome.trim() || null, telefone: telefone.trim() || null, observacao: obs.trim() || null, forma, itens }))
    } catch (err) {
      setErro(msg(err, 'Não foi possível registrar a venda.'))
      setSalvando(false)
    }
  }

  return (
    <Modal titulo="Nova venda de Day Use" onFechar={onFechar}>
      <div className="space-y-3 text-sm">
        <div>
          <label className="text-xs text-gray-600">Dia de uso</label>
          <input type="date" min={iso(new Date())} value={data} onChange={(e) => e.target.value && setData(e.target.value)} className="border rounded-lg px-3 py-2 w-full" />
        </div>
        <div className="space-y-2">
          {tipos.length === 0 && <p className="text-xs text-amber-700">Nenhum ingresso cadastrado. Peça ao administrador para cadastrar em "Ingressos e capacidade".</p>}
          {tipos.map((t) => (
            <div key={t.id} className="flex items-center justify-between border rounded-lg px-3 py-2">
              <div><p className="font-medium">{t.nome}</p><p className="text-xs text-gray-500">{brl(t.preco)} {t.pessoasPorUnidade > 1 ? `· pacote para ${t.pessoasPorUnidade} pessoas` : 'cada'}</p></div>
              <div className="flex items-center gap-2">
                <button onClick={() => mudar(t.id, -1)} className="w-8 h-8 rounded-full border flex items-center justify-center" aria-label="Menos"><Minus size={14} /></button>
                <span className="w-6 text-center font-semibold">{qtd[t.id] || 0}</span>
                <button onClick={() => mudar(t.id, 1)} className="w-8 h-8 rounded-full bg-araca-azul text-white flex items-center justify-center" aria-label="Mais"><Plus size={14} /></button>
              </div>
            </div>
          ))}
        </div>
        <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Nome (opcional)" maxLength={80} className="border rounded-lg px-3 py-2 w-full" />
        <input value={telefone} onChange={(e) => setTelefone(e.target.value)} placeholder="Telefone (opcional)" maxLength={30} className="border rounded-lg px-3 py-2 w-full" />
        <input value={obs} onChange={(e) => setObs(e.target.value)} placeholder="Observação (opcional)" maxLength={200} className="border rounded-lg px-3 py-2 w-full" />
        <select value={forma} onChange={(e) => setForma(e.target.value)} className="border rounded-lg px-3 py-2 w-full">
          {FORMAS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
        </select>
        <div className="flex justify-between items-center border-t pt-3">
          <span>{pessoas} pessoa(s)</span>
          <strong className="text-lg text-araca-azul">{brl(total)}</strong>
        </div>
        {erro && <p className="text-red-600">{erro}</p>}
        <div className="flex justify-end gap-2">
          <button onClick={onFechar} className="px-4 py-2">Cancelar</button>
          <button onClick={salvar} disabled={salvando || ingressos === 0} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-50">
            {salvando ? 'Registrando...' : 'Registrar e imprimir'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function Configurar({ onFechar }) {
  const [tipos, setTipos] = useState([])
  const [capacidade, setCapacidade] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [novo, setNovo] = useState({ nome: '', precoSemana: '', precoFimSemana: '', pessoasPorUnidade: '1', custo: '' })
  const [erro, setErro] = useState('')
  const [ok, setOk] = useState('')

  const carregar = useCallback(async () => {
    try {
      const [t, c] = await Promise.all([dayUseService.todosOsTipos(), dayUseService.config()])
      setTipos(t)
      setCapacidade(c.capacidadeDiaria ? String(c.capacidadeDiaria) : '')
      setMensagem(c.mensagemPortal || '')
    } catch (err) { setErro(msg(err, 'Não foi possível carregar.')) }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  const payload = (t) => ({
    nome: t.nome.trim(),
    precoSemana: num(t.precoSemana) || 0,
    precoFimSemana: String(t.precoFimSemana ?? '').trim() === '' ? null : num(t.precoFimSemana),
    pessoasPorUnidade: Math.max(1, parseInt(t.pessoasPorUnidade, 10) || 1),
    custo: String(t.custo ?? '').trim() === '' ? null : num(t.custo),
    ativo: t.ativo ?? true,
  })

  async function salvarTipo(t) {
    setErro(''); setOk('')
    if (!(num(t.precoSemana) > 0)) { setErro('Informe o preço (Seg–Sex). Se for o mesmo todos os dias, deixe Sáb/Dom vazio.'); return }
    try { await dayUseService.atualizarTipo(t.id, payload(t)); setOk('Ingresso salvo.'); await carregar() } catch (err) { setErro(msg(err, 'Não foi possível salvar.')) }
  }
  async function criar() {
    setErro(''); setOk('')
    if (!novo.nome.trim()) { setErro('Informe o nome do ingresso.'); return }
    if (!(num(novo.precoSemana) > 0)) { setErro('Informe o preço (Seg–Sex). Se for o mesmo todos os dias, deixe Sáb/Dom vazio.'); return }
    try { await dayUseService.criarTipo(payload(novo)); setNovo({ nome: '', precoSemana: '', precoFimSemana: '', pessoasPorUnidade: '1', custo: '' }); setOk('Ingresso criado.'); await carregar() } catch (err) { setErro(msg(err, 'Não foi possível criar.')) }
  }
  async function salvarCapacidade() {
    setErro(''); setOk('')
    try { await dayUseService.salvarConfig(capacidade === '' ? 0 : Math.max(0, parseInt(capacidade, 10) || 0), mensagem.trim() || null); setOk('Configuração salva.') } catch (err) { setErro(msg(err, 'Não foi possível salvar.')) }
  }
  const edit = (id, campo, valor) => setTipos((ts) => ts.map((t) => (t.id === id ? { ...t, [campo]: valor } : t)))

  return (
    <Modal titulo="Ingressos e capacidade" onFechar={onFechar}>
      <div className="space-y-4 text-sm">
        <div>
          <label className="text-xs text-gray-600">Capacidade por dia (pessoas). Vazio ou 0 = sem limite</label>
          <div className="flex gap-2">
            <input value={capacidade} onChange={(e) => setCapacidade(e.target.value)} inputMode="numeric" className="border rounded-lg px-3 py-2 w-32" />
          </div>
          <label className="text-xs text-gray-600 mt-2 block">Mensagem para o cliente no portal (ex.: como pagar a reserva)</label>
          <textarea value={mensagem} onChange={(e) => setMensagem(e.target.value)} maxLength={300} rows={2} className="border rounded-lg px-3 py-2 w-full" placeholder="Ex.: Pague na recepção ou por Pix até a data. Pix: ..." />
          <button onClick={salvarCapacidade} className="bg-araca-azul text-white px-3 py-2 rounded-lg mt-1">Salvar capacidade e mensagem</button>
        </div>
        <div>
          <p className="text-xs text-gray-600 mb-1">Ingressos: preço de semana, preço opcional de sábado e domingo, "pessoas por ingresso" (use mais de 1 para pacote de grupo, ex.: 10 pessoas por R$ 300) e custo opcional.</p>
          <div className="space-y-2">
            {tipos.map((t) => (
              <div key={t.id} className="border rounded-lg p-2 grid grid-cols-[1fr_90px_90px] gap-2 items-center">
                <input value={t.nome} onChange={(e) => edit(t.id, 'nome', e.target.value)} className="border rounded px-2 py-1" />
                <label className="text-[10px] text-gray-500">Seg–Sex (R$)<input value={t.precoSemana ?? ''} onChange={(e) => edit(t.id, 'precoSemana', e.target.value)} inputMode="decimal" placeholder="0,00" className="border rounded px-2 py-1 w-full text-sm text-gray-800" /></label>
                <label className="text-[10px] text-gray-500">Sáb/Dom (R$)<input value={t.precoFimSemana ?? ''} onChange={(e) => edit(t.id, 'precoFimSemana', e.target.value)} inputMode="decimal" placeholder="igual" className="border rounded px-2 py-1 w-full text-sm text-gray-800" /></label>
                <label className="text-[11px] text-gray-500 flex items-center gap-1">Pessoas por ingresso <input value={t.pessoasPorUnidade ?? 1} onChange={(e) => edit(t.id, 'pessoasPorUnidade', e.target.value)} inputMode="numeric" className="border rounded px-2 py-1 w-14 text-sm text-gray-800" /></label>
                <input value={t.custo ?? ''} onChange={(e) => edit(t.id, 'custo', e.target.value)} inputMode="decimal" placeholder="Custo" title="Custo por ingresso (opcional, para o lucro)" className="border rounded px-2 py-1 col-span-2" />
                <label className="flex items-center gap-1 text-xs col-span-2"><input type="checkbox" checked={t.ativo} onChange={(e) => edit(t.id, 'ativo', e.target.checked)} /> Ativo (à venda)</label>
                <button onClick={() => salvarTipo(t)} className="bg-araca-azul text-white px-2 py-1 rounded text-xs">Salvar</button>
              </div>
            ))}
            <div className="border border-dashed rounded-lg p-2 grid grid-cols-[1fr_90px_90px] gap-2 items-center">
              <input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} placeholder="Novo ingresso (ex.: Adulto, Pacote 10 pessoas)" className="border rounded px-2 py-1" />
              <label className="text-[10px] text-gray-500">Seg–Sex (R$)<input value={novo.precoSemana} onChange={(e) => setNovo({ ...novo, precoSemana: e.target.value })} inputMode="decimal" placeholder="0,00" className="border rounded px-2 py-1 w-full text-sm text-gray-800" /></label>
              <label className="text-[10px] text-gray-500">Sáb/Dom (R$)<input value={novo.precoFimSemana} onChange={(e) => setNovo({ ...novo, precoFimSemana: e.target.value })} inputMode="decimal" placeholder="igual" className="border rounded px-2 py-1 w-full text-sm text-gray-800" /></label>
              <label className="text-[11px] text-gray-500 flex items-center gap-1">Pessoas por ingresso <input value={novo.pessoasPorUnidade} onChange={(e) => setNovo({ ...novo, pessoasPorUnidade: e.target.value })} inputMode="numeric" className="border rounded px-2 py-1 w-14 text-sm text-gray-800" /></label>
              <input value={novo.custo} onChange={(e) => setNovo({ ...novo, custo: e.target.value })} inputMode="decimal" placeholder="Custo (opcional)" className="border rounded px-2 py-1 col-span-2" />
              <button onClick={criar} className="bg-araca-verde text-araca-azul font-semibold px-2 py-1 rounded text-xs col-span-3">Adicionar ingresso</button>
            </div>
          </div>
        </div>
        {erro && <p className="text-red-600">{erro}</p>}
        {ok && <p className="text-green-700">{ok}</p>}
      </div>
    </Modal>
  )
}
