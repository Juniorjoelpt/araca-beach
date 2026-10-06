import { useEffect, useMemo, useState } from 'react'
import { format, startOfMonth, endOfMonth, subDays } from 'date-fns'
import { TrendingUp, TrendingDown, Minus, BarChart3, AlertTriangle, Users, Clock } from 'lucide-react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { dashboardService } from '../services/dashboardService.js'

const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const DIAS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']
const FORMA = { PIX: 'Pix', CARTAO_CREDITO: 'Cartão crédito', CARTAO_DEBITO: 'Cartão débito', DINHEIRO: 'Dinheiro' }
const iso = (d) => format(d, 'yyyy-MM-dd')

/** Variacao percentual vs periodo anterior. "melhorQuandoMenor" inverte a cor (despesas, cancelamentos). */
function Variacao({ atual, anterior, melhorQuandoMenor = false }) {
  const a = Number(atual || 0)
  const b = Number(anterior || 0)
  if (b === 0 && a === 0) return <span className="text-xs text-gray-400 flex items-center gap-1"><Minus size={12} /> sem dados anteriores</span>
  if (b === 0) return <span className="text-xs text-gray-400">novo no período</span>
  const pct = ((a - b) / Math.abs(b)) * 100
  const subiu = pct > 0
  const bom = melhorQuandoMenor ? !subiu : subiu
  const Icon = pct === 0 ? Minus : subiu ? TrendingUp : TrendingDown
  const cor = pct === 0 ? 'text-gray-400' : bom ? 'text-green-600' : 'text-red-600'
  return (
    <span className={`text-xs flex items-center gap-1 ${cor}`}>
      <Icon size={12} /> {pct > 0 ? '+' : ''}{pct.toFixed(1).replace('.', ',')}% vs. período anterior
    </span>
  )
}

function Card({ label, valor, children, destaque }) {
  return (
    <div className={`rounded-xl shadow p-5 ${destaque ? 'bg-araca-verde' : 'bg-white'}`}>
      <p className={`text-sm ${destaque ? 'text-araca-azul/70' : 'text-gray-500'}`}>{label}</p>
      <p className="text-2xl font-bold text-araca-azul mt-1">{valor}</p>
      <div className="mt-1">{children}</div>
    </div>
  )
}

function MapaDeCalor({ dados }) {
  const max = Math.max(1, ...dados.map((d) => d.quantidade))
  const mapa = useMemo(() => {
    const m = {}
    dados.forEach((d) => { m[`${d.diaSemana}-${d.hora}`] = d.quantidade })
    return m
  }, [dados])
  const horas = dados.length ? [...new Set(dados.map((d) => d.hora))].sort((a, b) => a - b) : []
  if (!horas.length) return <p className="text-gray-400 text-sm">Sem reservas no período.</p>
  const faixa = []
  for (let h = horas[0]; h <= horas[horas.length - 1]; h++) faixa.push(h)

  return (
    <div className="overflow-x-auto">
      <table className="text-xs border-separate border-spacing-1">
        <thead>
          <tr>
            <th />
            {DIAS.map((d) => <th key={d} className="text-gray-500 font-medium w-10">{d}</th>)}
          </tr>
        </thead>
        <tbody>
          {faixa.map((h) => (
            <tr key={h}>
              <td className="text-gray-500 pr-2 text-right">{String(h).padStart(2, '0')}h</td>
              {DIAS.map((_, i) => {
                const q = mapa[`${i + 1}-${h}`] || 0
                return (
                  <td
                    key={i}
                    title={`${DIAS[i]} ${h}h: ${q} reserva(s)`}
                    className="h-6 w-10 rounded text-center"
                    style={{ background: q ? `rgba(0, 150, 136, ${0.15 + 0.85 * (q / max)})` : '#f3f4f6', color: q / max > 0.6 ? '#fff' : '#374151' }}
                  >
                    {q || ''}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function Gerencial() {
  const hoje = new Date()
  const [inicio, setInicio] = useState(iso(startOfMonth(hoje)))
  const [fim, setFim] = useState(iso(endOfMonth(hoje)))
  const [dados, setDados] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  async function carregar(i = inicio, f = fim) {
    setCarregando(true)
    setErro('')
    try {
      setDados(await dashboardService.gerencial(i, f))
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível carregar os indicadores.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function atalho(i, f) {
    setInicio(iso(i))
    setFim(iso(f))
    carregar(iso(i), iso(f))
  }

  const a = dados?.atual
  const b = dados?.anterior
  const serie = (dados?.receitaDiaria || []).map((d) => ({
    ...d,
    label: format(new Date(`${d.data}T00:00:00`), 'dd/MM'),
    receita: Number(d.receita),
    despesa: Number(d.despesa),
  }))
  const totalForma = dados ? Object.values(dados.receitaPorForma).reduce((s, v) => s + Number(v), 0) : 0
  const totalCategoria = dados ? Object.values(dados.despesaPorCategoria).reduce((s, v) => s + Number(v), 0) : 0

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6">Painel gerencial</h2>

      <div className="bg-white rounded-xl shadow p-4 mb-6 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-sm text-gray-600 mb-1">De</label>
          <input type="date" className="border rounded-lg px-3 py-2" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">Até</label>
          <input type="date" className="border rounded-lg px-3 py-2" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
        <button onClick={() => carregar()} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg">Aplicar</button>
        <div className="flex gap-2 text-sm ml-auto">
          <button className="underline text-gray-600" onClick={() => atalho(subDays(hoje, 6), hoje)}>7 dias</button>
          <button className="underline text-gray-600" onClick={() => atalho(subDays(hoje, 29), hoje)}>30 dias</button>
          <button className="underline text-gray-600" onClick={() => atalho(startOfMonth(hoje), endOfMonth(hoje))}>Este mês</button>
        </div>
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}
      {carregando && !dados && <p className="text-gray-500">Carregando…</p>}

      {dados && a && b && (
        <div className={carregando ? 'opacity-60 transition' : ''}>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-4">
            <Card label="Receita total" valor={brl(a.receitaTotal)} destaque>
              <Variacao atual={a.receitaTotal} anterior={b.receitaTotal} />
            </Card>
            <Card label="Despesas pagas" valor={brl(a.despesasPagas)}>
              <Variacao atual={a.despesasPagas} anterior={b.despesasPagas} melhorQuandoMenor />
            </Card>
            <Card label="Resultado (receita − despesas)" valor={brl(a.resultado)}>
              <Variacao atual={a.resultado} anterior={b.resultado} />
            </Card>
            <Card label="Ticket médio por reserva" valor={brl(a.ticketMedioReserva)}>
              <Variacao atual={a.ticketMedioReserva} anterior={b.ticketMedioReserva} />
            </Card>
            <Card label="Reservas realizadas" valor={a.reservas}>
              <Variacao atual={a.reservas} anterior={b.reservas} />
            </Card>
            <Card label="Horas reservadas" valor={`${Number(a.horasReservadas).toFixed(1).replace('.', ',')} h`}>
              <Variacao atual={a.horasReservadas} anterior={b.horasReservadas} />
            </Card>
            <Card label="Cancelamentos + no-show" valor={`${a.canceladas + a.naoCompareceu} (${Number(a.taxaCancelamentoPercentual).toFixed(1).replace('.', ',')}%)`}>
              <Variacao atual={a.taxaCancelamentoPercentual} anterior={b.taxaCancelamentoPercentual} melhorQuandoMenor />
              <p className="text-xs text-gray-400 mt-1">{a.canceladas} cancel. · {a.naoCompareceu} no-show · multas {brl(a.multasCobradas)}</p>
            </Card>
            <Card label="Clientes novos" valor={a.clientesNovos}>
              <Variacao atual={a.clientesNovos} anterior={b.clientesNovos} />
            </Card>
          </div>

          <div className="bg-white rounded-xl shadow p-6 mb-6">
            <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
              <BarChart3 size={18} className="text-araca-verde-escuro" /> Receita × despesas por dia
            </h3>
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={serie}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} minTickGap={20} />
                <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `R$${v}`} width={64} />
                <Tooltip formatter={(v) => brl(v)} />
                <Legend />
                <Area type="monotone" dataKey="receita" name="Receita" stroke="#009688" fill="#00968833" />
                <Area type="monotone" dataKey="despesa" name="Despesas" stroke="#e11d48" fill="#e11d4822" />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul mb-4">Ocupação por quadra</h3>
              {dados.ocupacaoPorQuadra.length === 0 && <p className="text-gray-400 text-sm">Nenhuma quadra disponível.</p>}
              <div className="space-y-3">
                {dados.ocupacaoPorQuadra.map((q) => (
                  <div key={q.quadra}>
                    <div className="flex justify-between text-sm mb-1">
                      <span>{q.quadra}</span>
                      <span className="text-gray-500">{Number(q.horasReservadas).toFixed(1).replace('.', ',')} h de {Number(q.horasDisponiveis)} h · <strong>{Number(q.ocupacaoPercentual).toFixed(1).replace('.', ',')}%</strong></span>
                    </div>
                    <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden">
                      <div className="h-full bg-araca-verde rounded-full" style={{ width: `${Math.min(100, Number(q.ocupacaoPercentual))}%` }} />
                    </div>
                  </div>
                ))}
              </div>
              <p className="text-xs text-gray-400 mt-3">Base: horário de funcionamento do portal, todos os dias do período.</p>
            </div>

            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
                <Clock size={18} className="text-araca-verde-escuro" /> Horários mais procurados
              </h3>
              <MapaDeCalor dados={dados.mapaDeCalor} />
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul mb-4">Receita por forma de pagamento</h3>
              {Object.keys(dados.receitaPorForma).length === 0 && <p className="text-gray-400 text-sm">Sem recebimentos no período.</p>}
              {Object.entries(dados.receitaPorForma).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm py-1 border-b last:border-0">
                  <span>{FORMA[k] || k}</span>
                  <span>{brl(v)} <span className="text-gray-400">({totalForma ? Math.round((Number(v) / totalForma) * 100) : 0}%)</span></span>
                </div>
              ))}
              <p className="text-xs text-gray-400 mt-3">Loja ({brl(a.receitaLoja)}) é contabilizada à parte, sem forma de pagamento.</p>
            </div>
            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul mb-4">Despesas por categoria</h3>
              {Object.keys(dados.despesaPorCategoria).length === 0 && <p className="text-gray-400 text-sm">Sem despesas pagas no período.</p>}
              {Object.entries(dados.despesaPorCategoria).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm py-1 border-b last:border-0">
                  <span>{k.charAt(0) + k.slice(1).toLowerCase()}</span>
                  <span>{brl(v)} <span className="text-gray-400">({totalCategoria ? Math.round((Number(v) / totalCategoria) * 100) : 0}%)</span></span>
                </div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
                <Users size={18} className="text-araca-verde-escuro" /> Melhores clientes do período
              </h3>
              {dados.topClientes.length === 0 && <p className="text-gray-400 text-sm">Sem reservas no período.</p>}
              <ol className="space-y-2">
                {dados.topClientes.map((c, i) => (
                  <li key={c.clienteId} className="flex justify-between text-sm">
                    <span>{i + 1}. {c.nome}</span>
                    <span className="text-gray-500">{c.reservas} reserva(s) · <strong>{brl(c.valor)}</strong></span>
                  </li>
                ))}
              </ol>
            </div>
            <div className="bg-white rounded-xl shadow p-6">
              <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
                <AlertTriangle size={18} className="text-red-500" /> Inadimplência (posição de hoje)
              </h3>
              <p className="text-3xl font-bold text-red-600">{brl(dados.inadimplencia.valorEmAtraso)}</p>
              <p className="text-sm text-gray-500 mb-3">
                {dados.inadimplencia.quantidadeEmAtraso} cobrança(s) em atraso
                {dados.inadimplencia.quantidadeEmAtraso > 0 && ` · maior atraso: ${dados.inadimplencia.maiorAtrasoDias} dia(s)`}
              </p>
              <p className="text-sm text-gray-600">A vencer: <strong>{brl(dados.inadimplencia.valorAVencer)}</strong> ({dados.inadimplencia.quantidadeAVencer} cobrança(s))</p>
              <p className="text-xs text-gray-400 mt-3">Mensalidades, matrículas e pacotes. Detalhes na tela Financeiro.</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
