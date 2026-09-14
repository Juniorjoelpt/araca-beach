import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import { CalendarCheck2, Wallet, ShoppingBag, TrendingUp, GraduationCap, AlertCircle, BarChart3 } from 'lucide-react'
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import { dashboardService } from '../services/dashboardService.js'

const badgePagamento = {
  PAGO: 'bg-green-100 text-green-700',
  PARCIAL: 'bg-yellow-100 text-yellow-700',
  PENDENTE: 'bg-red-100 text-red-700',
}

function TooltipCustomizado({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white rounded-lg shadow-lg border px-3 py-2 text-sm">
      <p className="font-semibold text-araca-azul mb-1">{label}</p>
      {payload.map((item) => (
        <p key={item.dataKey} style={{ color: item.color }}>
          {item.name}: {typeof item.value === 'number' && item.dataKey !== 'quantidadeReservas'
            ? `R$ ${item.value.toFixed(2)}`
            : item.value}
        </p>
      ))}
    </div>
  )
}

function CardIndicador({ icon: Icon, label, valor, sublinha, destaque, delay }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay, ease: [0.16, 1, 0.3, 1] }}
      className={`rounded-xl shadow p-6 ${destaque ? 'bg-araca-verde' : 'bg-white'}`}
    >
      <div className="flex items-center gap-2 mb-1">
        <Icon size={16} className={destaque ? 'text-araca-azul/70' : 'text-gray-400'} />
        <p className={`text-sm ${destaque ? 'text-araca-azul/70' : 'text-gray-500'}`}>{label}</p>
      </div>
      <p className="text-3xl font-bold text-araca-azul mt-1">{valor}</p>
      {sublinha && <p className="text-xs text-gray-400 mt-1">{sublinha}</p>}
    </motion.div>
  )
}

export default function Dashboard() {
  const [dados, setDados] = useState(null)
  const [faturamentoPeriodo, setFaturamentoPeriodo] = useState([])
  const [ocupacaoQuadras, setOcupacaoQuadras] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const hoje = format(new Date(), 'yyyy-MM-dd')
      const seteDiasAtras = format(new Date(Date.now() - 6 * 86400000), 'yyyy-MM-dd')
      const [resposta, faturamento, ocupacao] = await Promise.all([
        dashboardService.obter(hoje),
        dashboardService.faturamentoPeriodo(7),
        dashboardService.ocupacaoQuadras(seteDiasAtras, hoje),
      ])
      setDados(resposta)
      setFaturamentoPeriodo(faturamento.map((f) => ({
        ...f,
        diaLabel: format(new Date(`${f.data}T00:00:00`), 'dd/MM'),
      })))
      setOcupacaoQuadras(ocupacao)
    } catch {
      setErro('Não foi possível carregar os indicadores do dia.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  const percentualOcupacao = dados && dados.totalQuadras > 0
    ? Math.round((dados.reservasAtivas / dados.totalQuadras) * 100)
    : null

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6">Visão geral</h2>

      {carregando && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-xl shadow p-6 h-24 overflow-hidden relative">
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-araca-areia-escura to-transparent bg-[length:200%_100%] animate-shimmer" />
            </div>
          ))}
        </div>
      )}
      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {dados && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <CardIndicador
              icon={CalendarCheck2}
              label="Reservas ativas hoje"
              valor={dados.reservasAtivas}
              sublinha={percentualOcupacao !== null ? `${percentualOcupacao}% de ${dados.totalQuadras} ${dados.totalQuadras === 1 ? 'quadra' : 'quadras'}` : null}
              delay={0}
            />
            <CardIndicador
              icon={Wallet}
              label="Faturamento — reservas"
              valor={`R$ ${Number(dados.faturamentoReservas).toFixed(2)}`}
              delay={0.05}
            />
            <CardIndicador
              icon={ShoppingBag}
              label="Faturamento — loja"
              valor={`R$ ${Number(dados.faturamentoLoja).toFixed(2)}`}
              delay={0.1}
            />
            <CardIndicador
              icon={TrendingUp}
              label="Faturamento total do dia"
              valor={`R$ ${Number(dados.faturamentoTotal).toFixed(2)}`}
              destaque
              delay={0.15}
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.18 }}
              className="bg-white rounded-xl shadow p-6"
            >
              <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
                <BarChart3 size={18} className="text-araca-verde-escuro" />
                Faturamento — últimos 7 dias
              </h3>
              {faturamentoPeriodo.every((f) => Number(f.faturamentoTotal) === 0) ? (
                <p className="text-gray-400 text-sm">Nenhum faturamento registrado nesse período ainda.</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={faturamentoPeriodo}>
                    <defs>
                      <linearGradient id="corFaturamento" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#2C6906" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="#2C6906" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" vertical={false} />
                    <XAxis dataKey="diaLabel" tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} width={40} />
                    <Tooltip content={<TooltipCustomizado />} />
                    <Area
                      type="monotone"
                      dataKey="faturamentoTotal"
                      name="Faturamento"
                      stroke="#2C6906"
                      strokeWidth={2}
                      fill="url(#corFaturamento)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.22 }}
              className="bg-white rounded-xl shadow p-6"
            >
              <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
                <CalendarCheck2 size={18} className="text-araca-verde-escuro" />
                Ocupação por quadra — últimos 7 dias
              </h3>
              {ocupacaoQuadras.length === 0 ? (
                <p className="text-gray-400 text-sm">Nenhuma reserva nesse período ainda.</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={ocupacaoQuadras}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" vertical={false} />
                    <XAxis dataKey="quadraNome" tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 12, fill: '#6B7280' }} axisLine={false} tickLine={false} width={30} allowDecimals={false} />
                    <Tooltip content={<TooltipCustomizado />} cursor={{ fill: 'rgba(6, 18, 104, 0.05)' }} />
                    <Bar dataKey="quantidadeReservas" name="Reservas" fill="#4B58BE" radius={[6, 6, 0, 0]} maxBarSize={48} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </motion.div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.2 }}
              className="bg-white rounded-xl shadow p-6"
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-araca-azul flex items-center gap-2">
                  <GraduationCap size={18} className="text-araca-verde-escuro" />
                  Próximas aulas
                </h3>
                <Link to="/aulas" className="text-sm text-araca-verde-escuro hover:underline">Ver todas</Link>
              </div>
              {dados.proximasAulas.length === 0 && (
                <p className="text-gray-400 text-sm">Nenhuma aula pendente para hoje.</p>
              )}
              <ul className="space-y-2">
                {dados.proximasAulas.map((a) => (
                  <li key={a.aulaId} className="border-t pt-2 first:border-t-0 first:pt-0 text-sm">
                    <span className="font-medium text-araca-azul">{format(new Date(a.inicio), 'HH:mm')}</span>
                    {' — '}{a.professorNome} · {a.quadraNome}
                  </li>
                ))}
              </ul>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.25 }}
              className="bg-white rounded-xl shadow p-6"
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-araca-azul flex items-center gap-2">
                  <AlertCircle size={18} className="text-araca-verde-escuro" />
                  Reservas com pagamento pendente
                </h3>
                <Link to="/financeiro" className="text-sm text-araca-verde-escuro hover:underline">Ver financeiro</Link>
              </div>
              {dados.reservasPendentes.length === 0 && (
                <p className="text-gray-400 text-sm">Nenhuma pendência hoje. 🎉</p>
              )}
              <ul className="space-y-2">
                {dados.reservasPendentes.map((r) => (
                  <li key={r.reservaId} className="border-t pt-2 first:border-t-0 first:pt-0 text-sm flex items-center justify-between">
                    <span>
                      {format(new Date(r.inicio), 'HH:mm')} — {r.clienteNome} · {r.quadraNome}
                    </span>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgePagamento[r.statusPagamento]}`}>
                      {r.statusPagamento}
                    </span>
                  </li>
                ))}
              </ul>
            </motion.div>
          </div>
        </>
      )}
    </div>
  )
}
