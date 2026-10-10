import { useEffect, useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import { caixaService } from '../services/caixaService.js'

const brl = (v) => `R$ ${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

/** Avisa que comandas abertas (restaurante e loja) ainda nao entraram no caixa. Nao aparece se nao houver nenhuma. */
export default function AvisoComandasAbertas() {
  const [dados, setDados] = useState(null)

  useEffect(() => {
    caixaService.comandasAbertas().then(setDados).catch(() => {})
  }, [])

  if (!dados || dados.quantidade === 0) return null

  return (
    <div className="border border-amber-300 bg-amber-50 text-amber-900 rounded-lg px-3 py-2 text-sm mb-4">
      <p className="flex items-center gap-2 font-semibold">
        <AlertTriangle size={16} />
        {dados.quantidade} comanda(s) ainda aberta(s): {brl(dados.valor)} não entra(m) no caixa até serem fechadas.
      </p>
      <ul className="mt-1 text-xs list-disc pl-6">
        {dados.comandas.map((c) => (
          <li key={`${c.origem}-${c.id}`}>{c.origem} · {c.rotulo} · {brl(c.valor)}</li>
        ))}
      </ul>
    </div>
  )
}
