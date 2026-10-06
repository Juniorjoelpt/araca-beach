import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { Wallet } from 'lucide-react'

import { portalReservaService } from '../../services/portalReservaService.js'

const TIPO = {
  MENSALIDADE: 'Mensalidade',
  MATRICULA: 'Matrícula',
  TURMA: 'Turma',
  PACOTE: 'Pacote',
  RESERVA: 'Reserva',
  MULTA: 'Multa',
}

const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function PortalConta() {
  const [cobrancas, setCobrancas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    portalReservaService.cobrancas()
      .then(setCobrancas)
      .catch(() => setErro('Não foi possível carregar suas cobranças.'))
      .finally(() => setCarregando(false))
  }, [])

  const total = cobrancas.reduce((soma, c) => soma + Number(c.valor), 0)
  const vencidas = cobrancas.filter((c) => c.vencida)

  return (
    <div>
      <h1 className="font-title text-xl text-araca-azul mb-4">Minha conta</h1>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}

      {!carregando && !erro && (
        <div className="bg-white rounded-xl shadow p-4 mb-4 flex items-center gap-3">
          <Wallet className="text-araca-verde-escuro" size={28} />
          <div>
            <p className="text-xs text-gray-500">Total em aberto</p>
            <p className="text-2xl font-bold text-araca-azul leading-none">{brl(total)}</p>
            {vencidas.length > 0 && (
              <p className="text-xs text-red-600 mt-1">{vencidas.length} cobrança(s) vencida(s)</p>
            )}
          </div>
        </div>
      )}

      {!carregando && !erro && cobrancas.length === 0 && (
        <p className="text-gray-400 text-sm">Você não tem nenhuma cobrança em aberto. 🎉</p>
      )}

      <div className="space-y-2">
        {cobrancas.map((c, i) => (
          <div key={i} className="bg-white rounded-xl shadow p-4 flex items-start justify-between gap-3">
            <div>
              <p className="text-xs text-gray-400 uppercase">{TIPO[c.tipo] || c.tipo}</p>
              <p className="text-sm font-medium text-araca-azul">{c.descricao}</p>
              <p className={`text-xs mt-0.5 ${c.vencida ? 'text-red-600' : 'text-gray-500'}`}>
                {c.vencida ? 'Venceu em ' : 'Vence em '}{format(new Date(c.vencimento + 'T00:00:00'), 'dd/MM/yyyy')}
              </p>
            </div>
            <p className="font-semibold text-araca-azul whitespace-nowrap">{brl(c.valor)}</p>
          </div>
        ))}
      </div>
      {cobrancas.length > 0 && (
        <p className="text-xs text-gray-400 mt-4">O pagamento é feito na recepção da arena.</p>
      )}
    </div>
  )
}
