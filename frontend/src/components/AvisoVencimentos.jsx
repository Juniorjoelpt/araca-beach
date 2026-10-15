import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { BellRing } from 'lucide-react'
import { financeiroService } from '../services/financeiroService.js'

const DIAS_ALERTA = 5
const brl = (v) => `R$ ${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

function diasAte(vencimento) {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return Math.round((new Date(vencimento + 'T00:00:00') - hoje) / 86400000)
}

/** Alerta de mensalidades/matrículas que vencem em até 5 dias (ou vencem hoje). Some se não houver nenhuma. */
export default function AvisoVencimentos() {
  const [lista, setLista] = useState([])
  const { pathname } = useLocation()

  useEffect(() => {
    financeiroService
      .cobrancasPendentes()
      .then((cobrancas) =>
        setLista(
          cobrancas
            .map((c) => ({ ...c, dias: diasAte(c.vencimento) }))
            .filter((c) => c.dias >= 0 && c.dias <= DIAS_ALERTA)
        )
      )
      .catch(() => setLista([]))
  }, [pathname])

  if (lista.length === 0) return null

  const quando = (d) => (d === 0 ? 'vence hoje' : d === 1 ? 'vence amanhã' : `vence em ${d} dias`)

  return (
    <div className="border border-amber-300 bg-amber-50 text-amber-900 rounded-lg px-3 py-2 text-sm mb-4">
      <p className="flex items-center gap-2 font-semibold">
        <BellRing size={16} />
        {lista.length} cobrança(s) vence(m) nos próximos {DIAS_ALERTA} dias
        <Link to="/financeiro" className="ml-auto text-xs font-medium underline">Ver no Financeiro</Link>
      </p>
      <ul className="mt-1 text-xs list-disc pl-6">
        {lista.map((c) => (
          <li key={`${c.tipo}-${c.pagamentoId}`}>
            {c.clienteNome} · {c.descricao} · {brl(c.valor)} · {quando(c.dias)}
          </li>
        ))}
      </ul>
    </div>
  )
}
