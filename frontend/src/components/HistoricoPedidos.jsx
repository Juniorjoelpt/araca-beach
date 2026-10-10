import { useCallback, useEffect, useMemo, useState } from 'react'
import { format, subDays, startOfMonth, endOfMonth, subMonths, startOfWeek } from 'date-fns'
import { Search, ChevronDown, ChevronRight } from 'lucide-react'
import { restauranteService } from '../services/restauranteService.js'

const brl = (v) => `R$ ${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const iso = (d) => format(d, 'yyyy-MM-dd')
const hora = (dt) => (dt ? format(new Date(dt), 'dd/MM HH:mm') : '—')
const FORMA = { PIX: 'Pix', DINHEIRO: 'Dinheiro', CARTAO_CREDITO: 'Crédito', CARTAO_DEBITO: 'Débito' }
const STATUS = {
  ABERTA: { label: 'Aberta', cls: 'bg-blue-100 text-blue-700' },
  FECHADA: { label: 'Fechada', cls: 'bg-green-100 text-green-700' },
  CANCELADA: { label: 'Cancelada', cls: 'bg-red-100 text-red-700' },
}
const PRESETS = [
  ['Hoje', () => [new Date(), new Date()]],
  ['Ontem', () => [subDays(new Date(), 1), subDays(new Date(), 1)]],
  ['Esta semana', () => [startOfWeek(new Date(), { weekStartsOn: 1 }), new Date()]],
  ['Últimos 7 dias', () => [subDays(new Date(), 6), new Date()]],
  ['Este mês', () => [startOfMonth(new Date()), new Date()]],
  ['Mês passado', () => [startOfMonth(subMonths(new Date(), 1)), endOfMonth(subMonths(new Date(), 1))]],
]

export default function HistoricoPedidos() {
  const [filtros, setFiltros] = useState({ inicio: iso(new Date()), fim: iso(new Date()), status: 'TODAS', abertaPor: '', busca: '' })
  const [dados, setDados] = useState(null)
  const [quemAbriu, setQuemAbriu] = useState([])
  const [abertas, setAbertas] = useState({})
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  const buscar = useCallback(async (f) => {
    if (f.fim < f.inicio) { setErro('A data final deve ser igual ou posterior à inicial.'); return }
    setCarregando(true)
    setErro('')
    try {
      const r = await restauranteService.historicoPedidos(f)
      setDados(r)
      setQuemAbriu((atual) => [...new Set([...atual, ...r.comandas.map((c) => c.abertaPor).filter(Boolean)])].sort())
    } catch (err) {
      setDados(null)
      setErro(err?.response?.data?.mensagem || 'Não foi possível carregar o histórico.')
    } finally {
      setCarregando(false)
    }
  }, [])

  // recarrega ao mudar periodo/status/quem abriu; a busca por texto tem um pequeno atraso
  useEffect(() => {
    const t = setTimeout(() => buscar(filtros), filtros.busca ? 350 : 0)
    return () => clearTimeout(t)
  }, [filtros, buscar])

  const set = (campo) => (e) => setFiltros((f) => ({ ...f, [campo]: e.target.value }))
  const resumo = useMemo(() => {
    const cs = dados?.comandas || []
    const pedidos = cs.reduce((s, c) => s + c.pedidos.length, 0)
    const itens = cs.reduce((s, c) => s + c.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado).reduce((q, i) => q + i.quantidade, 0), 0)
    const cancelados = cs.reduce((s, c) => s + c.pedidos.flatMap((p) => p.itens).filter((i) => i.cancelado).length, 0)
    const vendido = cs.filter((c) => c.status === 'FECHADA' && !c.cortesia).reduce((s, c) => s + Number(c.total), 0)
    return { comandas: cs.length, pedidos, itens, cancelados, vendido }
  }, [dados])

  return (
    <div>
      <div className="bg-white rounded-xl shadow p-3 mb-4 space-y-3">
        <div className="flex flex-wrap gap-2">
          {PRESETS.map(([label, faixa]) => {
            const [i, f] = faixa()
            const ativo = iso(i) === filtros.inicio && iso(f) === filtros.fim
            return (
              <button key={label} onClick={() => setFiltros((x) => ({ ...x, inicio: iso(i), fim: iso(f) }))}
                className={`text-sm px-3 py-1.5 rounded-full border ${ativo ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600 hover:bg-araca-areia'}`}>{label}</button>
            )
          })}
        </div>
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <input type="date" value={filtros.inicio} max={filtros.fim} onChange={set('inicio')} className="border rounded-lg px-2 py-1.5" />
          <span>até</span>
          <input type="date" value={filtros.fim} min={filtros.inicio} onChange={set('fim')} className="border rounded-lg px-2 py-1.5" />
          <select value={filtros.status} onChange={set('status')} className="border rounded-lg px-2 py-1.5">
            <option value="TODAS">Todas as situações</option>
            <option value="ABERTA">Abertas</option>
            <option value="FECHADA">Fechadas</option>
            <option value="CANCELADA">Canceladas</option>
          </select>
          <select value={filtros.abertaPor} onChange={set('abertaPor')} className="border rounded-lg px-2 py-1.5">
            <option value="">Quem abriu: todos</option>
            {quemAbriu.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
          <div className="relative flex-1 min-w-[200px]">
            <Search size={14} className="absolute left-2.5 top-2.5 text-gray-400" />
            <input value={filtros.busca} onChange={set('busca')} placeholder="Buscar mesa, cliente, nº da comanda ou produto"
              className="border rounded-lg pl-8 pr-2 py-1.5 w-full" />
          </div>
        </div>
      </div>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {carregando && <p className="text-gray-500 text-sm mb-3">Carregando...</p>}

      {dados && (
        <>
          <div className="grid grid-cols-2 xl:grid-cols-5 gap-3 mb-4">
            {[['Comandas', resumo.comandas], ['Pedidos enviados', resumo.pedidos], ['Itens vendidos', resumo.itens],
              ['Itens cancelados', resumo.cancelados], ['Vendido (fechadas)', brl(resumo.vendido)]].map(([l, v]) => (
              <div key={l} className="bg-white rounded-xl shadow p-3"><p className="text-xs text-gray-500">{l}</p><p className="text-lg font-bold text-araca-azul">{v}</p></div>
            ))}
          </div>
          {dados.truncado && (
            <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mb-3">
              Mostrando as {dados.comandas.length} mais recentes de {dados.totalEncontrado}. Refine o período ou a busca para ver as demais (o resumo acima considera só as listadas).
            </p>
          )}
          <div className="bg-white rounded-xl shadow divide-y">
            {dados.comandas.length === 0 && <p className="p-4 text-sm text-gray-400">Nenhuma comanda encontrada.</p>}
            {dados.comandas.map((c) => {
              const aberto = !!abertas[c.id]
              const st = STATUS[c.status] || { label: c.status, cls: 'bg-gray-100 text-gray-600' }
              const qtd = c.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado).length
              return (
                <div key={c.id}>
                  <button onClick={() => setAbertas((a) => ({ ...a, [c.id]: !a[c.id] }))} className="w-full text-left px-3 py-2.5 flex items-center gap-2 hover:bg-araca-areia">
                    {aberto ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    <span className="text-xs text-gray-500 w-24 shrink-0">{hora(c.abertaEm)}</span>
                    <span className="font-medium text-sm truncate flex-1">#{c.id} · {c.rotulo}</span>
                    {c.cortesia && <span className="text-[10px] px-1.5 py-0.5 rounded bg-purple-100 text-purple-700">Cortesia</span>}
                    <span className={`text-[10px] px-1.5 py-0.5 rounded ${st.cls}`}>{st.label}</span>
                    <span className="text-xs text-gray-500 hidden md:inline w-28 truncate">{c.abertaPor || '—'}</span>
                    <span className="text-xs text-gray-500 w-16 text-right">{qtd} item(ns)</span>
                    <strong className="text-araca-azul w-24 text-right text-sm">{c.cortesia ? '—' : brl(c.total)}</strong>
                  </button>
                  {aberto && (
                    <div className="px-9 pb-3 text-sm space-y-2 bg-gray-50">
                      <p className="text-xs text-gray-500 pt-2">
                        Aberta {hora(c.abertaEm)}{c.abertaPor ? ` por ${c.abertaPor}` : ''}{c.fechadaEm ? ` · encerrada ${hora(c.fechadaEm)}` : ''}
                        {c.mesa ? ` · Mesa ${c.mesa}` : ''}{c.cortesia && c.cortesiaMotivo ? ` · Cortesia: ${c.cortesiaMotivo}` : ''}
                      </p>
                      {c.pedidos.length === 0 && <p className="text-xs text-gray-400">Nenhum pedido lançado.</p>}
                      {c.pedidos.map((p) => (
                        <div key={p.id} className="border rounded-lg bg-white px-3 py-2">
                          <p className="text-xs font-semibold text-araca-azul mb-1">Pedido {p.numero} · {hora(p.criadoEm)}{p.lancadoPor ? ` · ${p.lancadoPor}` : ''}</p>
                          {p.itens.map((i) => (
                            <div key={i.id} className={`flex justify-between gap-2 text-xs ${i.cancelado ? 'line-through text-gray-400' : ''}`}>
                              <span>{i.quantidade}x {i.nome}{i.observacao ? ` (${i.observacao})` : ''}{i.cancelado && i.motivoCancelamento ? ` — cancelado: ${i.motivoCancelamento}` : ''}</span>
                              <span>{brl(i.subtotal)}</span>
                            </div>
                          ))}
                        </div>
                      ))}
                      {!c.cortesia && (
                        <div className="text-xs text-gray-600 flex flex-wrap gap-x-4 gap-y-1">
                          <span>Subtotal {brl(c.subtotal)}</span>
                          {Number(c.taxaServico) > 0 && <span>Taxa de serviço {brl(c.taxaServico)}</span>}
                          {Number(c.desconto) > 0 && <span>Desconto −{brl(c.desconto)}{c.descontoMotivo ? ` (${c.descontoMotivo})` : ''}</span>}
                          <strong>Total {brl(c.total)}</strong>
                          {c.pagamentos.length > 0 && <span>Pagamentos: {c.pagamentos.map((p) => `${FORMA[p.forma] || p.forma} ${brl(p.valor)}`).join(' · ')}</span>}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
