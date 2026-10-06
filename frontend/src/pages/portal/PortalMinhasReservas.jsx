import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { BellRing, X } from 'lucide-react'

import { portalReservaService } from '../../services/portalReservaService.js'

const STATUS_LABEL = {
  CONFIRMADA: { texto: 'Confirmada', cor: 'bg-green-100 text-green-700' },
  CANCELADA: { texto: 'Cancelada', cor: 'bg-gray-100 text-gray-500' },
  CONCLUIDA: { texto: 'Concluída', cor: 'bg-blue-100 text-blue-700' },
  NAO_COMPARECEU: { texto: 'Não compareceu', cor: 'bg-red-100 text-red-700' },
}

const ESPERA_LABEL = {
  AGUARDANDO: 'Aguardando vaga',
  NOTIFICADO: 'Vaga liberada! Reserve agora',
}

const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function PortalMinhasReservas() {
  const [reservas, setReservas] = useState([])
  const [espera, setEspera] = useState([])
  const [mostrarHistorico, setMostrarHistorico] = useState(false)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')

  async function carregar() {
    setCarregando(true)
    try {
      const [dados, fila] = await Promise.all([
        portalReservaService.minhasReservas(),
        portalReservaService.listaEspera().catch(() => []),
      ])
      dados.sort((a, b) => new Date(b.inicio) - new Date(a.inicio))
      setReservas(dados)
      setEspera(fila.filter((e) => e.status === 'AGUARDANDO' || e.status === 'NOTIFICADO'))
    } catch {
      setErro('Não foi possível carregar suas reservas.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  async function handleCancelar(r) {
    const aviso = r.taxaSeCancelarAgora && Number(r.taxaSeCancelarAgora) > 0
      ? `Cancelar agora gera multa de ${brl(r.taxaSeCancelarAgora)}. Deseja cancelar mesmo assim?`
      : 'Cancelar esta reserva? (sem multa)'
    if (!confirm(aviso)) return
    setErro('')
    setAviso('')
    try {
      const resp = await portalReservaService.cancelar(r.id)
      if (resp?.taxaCancelamento && Number(resp.taxaCancelamento) > 0) {
        setAviso(`Reserva cancelada. Multa de ${brl(resp.taxaCancelamento)} a pagar na recepção.`)
      } else {
        setAviso('Reserva cancelada sem multa.')
      }
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível cancelar a reserva.')
    }
  }

  async function handleSairEspera(id) {
    try {
      await portalReservaService.sairDaListaEspera(id)
      carregar()
    } catch {
      setErro('Não foi possível sair da lista de espera.')
    }
  }

  const agora = new Date()
  const proximas = reservas
    .filter((r) => r.status === 'CONFIRMADA' && new Date(r.fim) >= agora)
    .sort((a, b) => new Date(a.inicio) - new Date(b.inicio))
  const historico = reservas.filter((r) => !proximas.includes(r))

  function card(r) {
    const status = STATUS_LABEL[r.status] || { texto: r.status, cor: 'bg-gray-100 text-gray-500' }
    const podeCancel = r.status === 'CONFIRMADA' && new Date(r.inicio) > new Date()
    const multa = r.taxaCancelamento && Number(r.taxaCancelamento) > 0
    return (
      <div key={r.id} className="bg-white rounded-xl shadow p-4">
        <div className="flex items-start justify-between mb-1">
          <p className="font-semibold text-araca-azul">{r.quadraNome}</p>
          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${status.cor}`}>{status.texto}</span>
        </div>
        <p className="text-sm text-gray-600">
          {format(new Date(r.inicio), 'dd/MM/yyyy · HH:mm')} – {format(new Date(r.fim), 'HH:mm')}
        </p>
        {r.valorTotal != null && Number(r.valorTotal) > 0 && (
          <p className="text-sm text-gray-500 mt-1">
            {multa ? `Multa: ${brl(r.taxaCancelamento)}` : brl(r.valorTotal)}
            {r.statusPagamento === 'PENDENTE' && <span className="text-amber-600"> · Pagamento na recepção</span>}
            {r.statusPagamento === 'PARCIAL' && <span className="text-amber-600"> · Pago parcialmente</span>}
            {r.statusPagamento === 'PAGO' && <span className="text-green-700"> · Pago</span>}
          </p>
        )}
        {podeCancel && r.mensagemCancelamento && (
          <p className={`text-xs mt-1 ${Number(r.taxaSeCancelarAgora) > 0 ? 'text-red-600' : 'text-gray-400'}`}>
            {r.mensagemCancelamento}
            {Number(r.taxaSeCancelarAgora) > 0 && ` Multa hoje: ${brl(r.taxaSeCancelarAgora)}.`}
          </p>
        )}
        {podeCancel && (
          <button onClick={() => handleCancelar(r)} className="text-red-600 text-sm mt-2 hover:underline">
            Cancelar reserva
          </button>
        )}
      </div>
    )
  }

  return (
    <div>
      <h1 className="font-title text-xl text-araca-azul mb-4">Minhas reservas</h1>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {aviso && <p className="text-green-700 text-sm mb-3">{aviso}</p>}
      {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}
      {!carregando && reservas.length === 0 && espera.length === 0 && (
        <p className="text-gray-400 text-sm">Você ainda não tem nenhuma reserva.</p>
      )}

      {espera.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1.5"><BellRing size={15} /> Lista de espera</h2>
          <div className="space-y-2">
            {espera.map((e) => (
              <div key={e.id} className={`rounded-xl p-3 flex items-center justify-between ${e.status === 'NOTIFICADO' ? 'bg-green-50 border border-green-200' : 'bg-amber-50 border border-amber-200'}`}>
                <div>
                  <p className="text-sm font-medium text-araca-azul">{e.quadraNome}</p>
                  <p className="text-xs text-gray-600">{format(new Date(e.inicio), 'dd/MM · HH:mm')}–{format(new Date(e.fim), 'HH:mm')}</p>
                  <p className="text-xs text-gray-500">{ESPERA_LABEL[e.status]}</p>
                </div>
                <button onClick={() => handleSairEspera(e.id)} aria-label="Sair da lista" className="text-gray-400 hover:text-red-600">
                  <X size={18} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {proximas.length > 0 && (
        <>
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Próximas</h2>
          <div className="space-y-3 mb-5">{proximas.map(card)}</div>
        </>
      )}

      {historico.length > 0 && (
        <>
          <button onClick={() => setMostrarHistorico((v) => !v)} className="text-sm text-araca-azul font-medium mb-2 underline">
            {mostrarHistorico ? 'Ocultar histórico' : `Ver histórico (${historico.length})`}
          </button>
          {mostrarHistorico && <div className="space-y-3">{historico.map(card)}</div>}
        </>
      )}
    </div>
  )
}
