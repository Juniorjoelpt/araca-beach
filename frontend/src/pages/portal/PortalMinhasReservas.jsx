import { useEffect, useState } from 'react'
import { format } from 'date-fns'

import { portalReservaService } from '../../services/portalReservaService.js'

const STATUS_LABEL = {
  CONFIRMADA: { texto: 'Confirmada', cor: 'bg-green-100 text-green-700' },
  CANCELADA: { texto: 'Cancelada', cor: 'bg-gray-100 text-gray-500' },
  CONCLUIDA: { texto: 'Concluída', cor: 'bg-blue-100 text-blue-700' },
  NAO_COMPARECEU: { texto: 'Não compareceu', cor: 'bg-red-100 text-red-700' },
}

export default function PortalMinhasReservas() {
  const [reservas, setReservas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  async function carregar() {
    setCarregando(true)
    try {
      const dados = await portalReservaService.minhasReservas()
      dados.sort((a, b) => new Date(b.inicio) - new Date(a.inicio))
      setReservas(dados)
    } catch {
      setErro('Não foi possível carregar suas reservas.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  async function handleCancelar(id) {
    if (!confirm('Cancelar esta reserva?')) return
    setErro('')
    try {
      await portalReservaService.cancelar(id)
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível cancelar a reserva.')
    }
  }

  return (
    <div>
      <h1 className="font-title text-xl text-araca-azul mb-4">Minhas reservas</h1>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}
      {!carregando && reservas.length === 0 && (
        <p className="text-gray-400 text-sm">Você ainda não tem nenhuma reserva.</p>
      )}

      <div className="space-y-3">
        {reservas.map((r) => {
          const status = STATUS_LABEL[r.status] || { texto: r.status, cor: 'bg-gray-100 text-gray-500' }
          const podeCancel = r.status === 'CONFIRMADA' && new Date(r.inicio) > new Date()
          return (
            <div key={r.id} className="bg-white rounded-xl shadow p-4">
              <div className="flex items-start justify-between mb-1">
                <p className="font-semibold text-araca-azul">{r.quadraNome}</p>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${status.cor}`}>{status.texto}</span>
              </div>
              <p className="text-sm text-gray-600">
                {format(new Date(r.inicio), 'dd/MM/yyyy · HH:mm')} – {format(new Date(r.fim), 'HH:mm')}
              </p>
              {r.valorTotal && (
                <p className="text-sm text-gray-500 mt-1">
                  R$ {Number(r.valorTotal).toFixed(2)}
                  {r.statusPagamento === 'PENDENTE' && <span className="text-amber-600"> · Pagamento na recepção</span>}
                  {r.statusPagamento === 'PAGO' && <span className="text-green-700"> · Pago</span>}
                </p>
              )}
              {podeCancel && (
                <button
                  onClick={() => handleCancelar(r.id)}
                  className="text-red-600 text-sm mt-2 hover:underline"
                >
                  Cancelar reserva
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
