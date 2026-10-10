import { useCallback, useEffect, useState } from 'react'
import { format, startOfMonth, endOfMonth, subMonths, subDays, startOfYear, startOfWeek, differenceInCalendarDays, parseISO } from 'date-fns'
import { TrendingUp } from 'lucide-react'
import { lucroService } from '../services/lucroService.js'

const brl = (v) => `R$ ${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const iso = (d) => format(d, 'yyyy-MM-dd')
const pct = (lucro, receita) => (Number(receita) > 0 ? `${((Number(lucro) / Number(receita)) * 100).toFixed(1).replace('.', ',')}%` : '—')

function Linha({ rotulo, valor, forte, negativo }) {
  return (
    <div className={`flex justify-between py-1.5 text-sm ${forte ? 'border-t mt-1 pt-2 font-bold text-araca-azul' : 'text-gray-700'}`}>
      <span>{rotulo}</span>
      <span className={negativo ? 'text-red-600' : ''}>{negativo ? '− ' : ''}{valor}</span>
    </div>
  )
}

function Aviso({ itens }) {
  if (!itens || itens.length === 0) return null
  return (
    <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-3">
      {itens.length} item(ns) sem custo conhecido ({itens.join(', ')}): o lucro fica superestimado até informar o custo.
    </p>
  )
}

const PRESETS = [
  { id: 'hoje', label: 'Hoje', faixa: () => [new Date(), new Date()] },
  { id: 'ontem', label: 'Ontem', faixa: () => [subDays(new Date(), 1), subDays(new Date(), 1)] },
  { id: 'semana', label: 'Esta semana', faixa: () => [startOfWeek(new Date(), { weekStartsOn: 1 }), new Date()] },
  { id: '7dias', label: 'Últimos 7 dias', faixa: () => [subDays(new Date(), 6), new Date()] },
  { id: '30dias', label: 'Últimos 30 dias', faixa: () => [subDays(new Date(), 29), new Date()] },
  { id: 'mes', label: 'Este mês', faixa: () => [startOfMonth(new Date()), new Date()] },
  { id: 'mespassado', label: 'Mês passado', faixa: () => [startOfMonth(subMonths(new Date(), 1)), endOfMonth(subMonths(new Date(), 1))] },
  { id: 'ano', label: 'Este ano', faixa: () => [startOfYear(new Date()), new Date()] },
]

function presetAtivo(inicio, fim) {
  return PRESETS.find((p) => {
    const [i, f] = p.faixa()
    return iso(i) === inicio && iso(f) === fim
  })?.id
}

export default function Lucro() {
  const [inicio, setInicio] = useState(iso(new Date()))
  const [fim, setFim] = useState(iso(new Date()))
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')

  const carregar = useCallback(async () => {
    if (!inicio || !fim) return
    if (fim < inicio) { setErro('A data final deve ser igual ou posterior à inicial.'); return }
    if (differenceInCalendarDays(parseISO(fim), parseISO(inicio)) > 365) { setErro('O período máximo é de 366 dias.'); return }
    try {
      setErro('')
      setDados(await lucroService.gerar(inicio, fim))
    } catch (err) {
      setErro(err?.response?.data?.mensagem || 'Não foi possível calcular o lucro.')
    }
  }, [inicio, fim])

  useEffect(() => { carregar() }, [carregar])

  const r = dados?.restaurante
  const l = dados?.loja

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h1 className="text-2xl font-bold text-araca-azul flex items-center gap-2"><TrendingUp size={24} /> Lucro das vendas</h1>
      </div>
      <div className="bg-white rounded-xl shadow p-3 mb-4 space-y-3">
        <div className="flex flex-wrap gap-2">
          {PRESETS.map((p) => (
            <button key={p.id} onClick={() => { const [i, f] = p.faixa(); setInicio(iso(i)); setFim(iso(f)) }}
              className={`text-sm px-3 py-1.5 rounded-full border ${presetAtivo(inicio, fim) === p.id ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600 hover:bg-araca-areia'}`}>
              {p.label}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-2 text-sm text-gray-700">
          <span>Período:</span>
          <input type="date" value={inicio} max={fim || undefined} onChange={(e) => setInicio(e.target.value)} className="border rounded-lg px-2 py-1.5" />
          <span>até</span>
          <input type="date" value={fim} min={inicio || undefined} onChange={(e) => setFim(e.target.value)} className="border rounded-lg px-2 py-1.5" />
          <span className="text-xs text-gray-400">
            {inicio && fim && fim >= inicio ? `${differenceInCalendarDays(parseISO(fim), parseISO(inicio)) + 1} dia(s)` : ''}
          </span>
        </div>
      </div>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {dados && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
            <div className="bg-white rounded-xl shadow p-4"><p className="text-xs text-gray-500">Receita (após descontos)</p><p className="text-xl font-bold text-araca-azul">{brl(dados.receitaTotal)}</p></div>
            <div className="bg-white rounded-xl shadow p-4"><p className="text-xs text-gray-500">Custo (inclui perda de cortesias)</p><p className="text-xl font-bold text-araca-azul">{brl(dados.custoTotal)}</p></div>
            <div className="bg-white rounded-xl shadow p-4"><p className="text-xs text-gray-500">Lucro total · margem {pct(dados.lucroTotal, dados.receitaTotal)}</p><p className="text-xl font-bold text-green-700">{brl(dados.lucroTotal)}</p></div>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <section className="bg-white rounded-xl shadow p-4">
              <h2 className="font-semibold text-araca-azul mb-2">Restaurante · {r.comandas} comanda(s)</h2>
              <Linha rotulo="Receita de itens" valor={brl(r.receita)} />
              <Linha rotulo="Descontos" valor={brl(r.descontos)} negativo />
              <Linha rotulo="Custo dos itens (gravado na venda)" valor={brl(r.custo)} negativo />
              <Linha rotulo="Perda com cortesias" valor={`${brl(r.perdaCortesias)} (${r.cortesias})`} negativo />
              <Linha rotulo={`Lucro · margem ${pct(r.lucro, Number(r.receita) - Number(r.descontos))}`} valor={brl(r.lucro)} forte />
              <p className="text-[11px] text-gray-400 mt-2">A taxa de serviço não entra no lucro.</p>
              <Aviso itens={r.itensSemCusto} />
            </section>
            <section className="bg-white rounded-xl shadow p-4">
              <h2 className="font-semibold text-araca-azul mb-2">Loja · {l.comandas} comanda(s)</h2>
              <Linha rotulo="Receita" valor={brl(l.receita)} />
              <Linha rotulo="Custo dos produtos" valor={brl(l.custo)} negativo />
              <Linha rotulo={`Lucro · margem ${pct(l.lucro, l.receita)}`} valor={brl(l.lucro)} forte />
              <p className="text-[11px] text-gray-400 mt-2">Itens de aluguel não têm custo de mercadoria.</p>
              <Aviso itens={l.itensSemCusto} />
            </section>
          </div>
          <p className="text-xs text-gray-500 mt-4">
            O custo é gravado no momento da venda (ficha técnica ou insumo de mesmo nome; na Loja, o custo do produto). Vendas anteriores a esta versão usam o custo atual como estimativa.
          </p>
        </>
      )}
    </div>
  )
}
