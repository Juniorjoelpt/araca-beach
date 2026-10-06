import { useEffect, useState } from 'react'
import { format, addDays, isSameDay } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { CheckCircle2, BellRing } from 'lucide-react'
import { portalReservaService } from '../../services/portalReservaService.js'

const DIAS_VISIVEIS = 7

const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function PortalAgendar() {
  const [quadras, setQuadras] = useState([])
  const [quadraId, setQuadraId] = useState('')
  const [diaEscolhido, setDiaEscolhido] = useState(new Date())
  const [slots, setSlots] = useState([])
  const [slotSelecionado, setSlotSelecionado] = useState(null)
  const [preco, setPreco] = useState(null)
  const [politica, setPolitica] = useState(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const dias = Array.from({ length: DIAS_VISIVEIS }, (_, i) => addDays(new Date(), i))

  useEffect(() => {
    portalReservaService.quadras()
      .then((lista) => {
        setQuadras(lista)
        if (lista.length > 0) setQuadraId(String(lista[0].id))
      })
      .catch(() => setErro('Não foi possível carregar as quadras.'))
    portalReservaService.politica().then(setPolitica).catch(() => {})
  }, [])

  async function carregarSlots() {
    const dados = await portalReservaService.disponibilidade(quadraId, format(diaEscolhido, 'yyyy-MM-dd'))
    setSlots(dados)
  }

  useEffect(() => {
    if (!quadraId) return
    setCarregando(true)
    setSlotSelecionado(null)
    setErro('')
    carregarSlots()
      .catch(() => setErro('Não foi possível carregar os horários.'))
      .finally(() => setCarregando(false))
  }, [quadraId, diaEscolhido])

  // Preço final (com desconto de mensalista) do horário escolhido
  useEffect(() => {
    setPreco(null)
    if (!slotSelecionado || !slotSelecionado.disponivel) return
    portalReservaService.preco(quadraId, slotSelecionado.inicio, slotSelecionado.fim)
      .then(setPreco)
      .catch(() => {})
  }, [slotSelecionado, quadraId])

  const quadraSelecionada = quadras.find((q) => String(q.id) === String(quadraId))

  async function handleConfirmar() {
    if (!slotSelecionado) return
    setErro('')
    setSucesso('')
    try {
      await portalReservaService.criarReserva({
        quadraId: Number(quadraId),
        inicio: slotSelecionado.inicio,
        fim: slotSelecionado.fim,
      })
      setSucesso('Reserva confirmada! Você pode acompanhá-la em "Reservas".')
      setSlotSelecionado(null)
      await carregarSlots()
    } catch (err) {
      if (err.response?.status === 409) {
        setErro(err.response?.data?.mensagem || 'Esse horário já foi reservado. Escolha outro.')
        carregarSlots().catch(() => {})
      } else {
        setErro(err.response?.data?.mensagem || 'Não foi possível criar a reserva.')
      }
    }
  }

  async function handleListaEspera() {
    if (!slotSelecionado) return
    setErro('')
    setSucesso('')
    try {
      await portalReservaService.entrarNaListaEspera({
        quadraId: Number(quadraId),
        inicio: slotSelecionado.inicio,
        fim: slotSelecionado.fim,
      })
      setSucesso('Você entrou na lista de espera. Avisaremos por e-mail se o horário liberar.')
      setSlotSelecionado(null)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível entrar na lista de espera.')
    }
  }

  return (
    <div>
      <h1 className="font-title text-xl text-araca-azul mb-4">Agendar quadra</h1>

      {quadras.length === 0 && !erro && (
        <p className="text-gray-400 text-sm">Carregando quadras...</p>
      )}

      {quadras.length > 0 && (
        <>
          <label className="block text-sm font-medium text-gray-700 mb-1">Quadra</label>
          <select
            className="w-full border rounded-lg px-3 py-2.5 mb-4 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={quadraId}
            onChange={(e) => setQuadraId(e.target.value)}
          >
            {quadras.map((q) => (
              <option key={q.id} value={q.id}>{q.nome}</option>
            ))}
          </select>

          <label className="block text-sm font-medium text-gray-700 mb-2">Data</label>
          <div className="flex gap-2 overflow-x-auto pb-2 mb-4 -mx-4 px-4">
            {dias.map((dia) => {
              const ativo = isSameDay(dia, diaEscolhido)
              return (
                <button
                  key={dia.toISOString()}
                  onClick={() => setDiaEscolhido(dia)}
                  className={`flex-shrink-0 flex flex-col items-center justify-center w-16 h-16 rounded-xl border transition ${
                    ativo ? 'bg-araca-verde border-araca-verde text-araca-azul font-semibold' : 'bg-white border-gray-200 text-gray-600'
                  }`}
                >
                  <span className="text-[11px] uppercase">{format(dia, 'EEE', { locale: ptBR })}</span>
                  <span className="text-lg font-bold">{format(dia, 'dd')}</span>
                </button>
              )
            })}
          </div>

          <label className="block text-sm font-medium text-gray-700 mb-2">Horários</label>
          {carregando && <p className="text-gray-400 text-sm">Carregando horários...</p>}
          {!carregando && slots.length === 0 && <p className="text-gray-400 text-sm">Nenhum horário configurado para esse dia.</p>}

          <div className="grid grid-cols-3 gap-2 mb-2">
            {slots.map((slot) => {
              const selecionado = slotSelecionado?.inicio === slot.inicio
              const ocupado = slot.motivoIndisponivel === 'OCUPADO'
              const clicavel = slot.disponivel || ocupado
              return (
                <button
                  key={slot.inicio}
                  disabled={!clicavel}
                  onClick={() => setSlotSelecionado(slot)}
                  className={`py-2 rounded-lg text-sm font-medium border transition leading-tight ${
                    selecionado
                      ? 'bg-araca-verde border-araca-verde text-araca-azul'
                      : slot.disponivel
                      ? 'bg-white border-gray-200 text-araca-azul hover:border-araca-verde'
                      : ocupado
                      ? 'bg-amber-50 border-amber-200 text-amber-700'
                      : 'bg-gray-100 text-gray-300 border-gray-100 cursor-not-allowed'
                  }`}
                >
                  {format(new Date(slot.inicio), 'HH:mm')}
                  <span className="block text-[11px] font-normal">
                    {slot.disponivel ? brl(slot.preco) : ocupado ? 'Lista de espera' : slot.motivoIndisponivel === 'BLOQUEADO' ? 'Bloqueado' : '—'}
                  </span>
                </button>
              )
            })}
          </div>
          <p className="text-[11px] text-gray-400 mb-4">Horários de pico podem ter valor diferente. Em horário ocupado, entre na lista de espera.</p>

          {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
          {sucesso && (
            <p className="text-green-700 text-sm mb-3 flex items-center gap-1.5">
              <CheckCircle2 size={16} /> {sucesso}
            </p>
          )}

          {slotSelecionado?.disponivel && (
            <div className="bg-white rounded-xl shadow p-4 sticky bottom-20">
              <p className="text-sm text-gray-500">Resumo da reserva</p>
              <p className="font-semibold text-araca-azul">
                {quadraSelecionada?.nome} · {format(diaEscolhido, 'dd/MM')} · {format(new Date(slotSelecionado.inicio), 'HH:mm')}–{format(new Date(slotSelecionado.fim), 'HH:mm')}
              </p>
              <p className="text-sm text-gray-500">
                Valor: <strong className="text-araca-azul">{brl(preco ? preco.valorTotal : slotSelecionado.preco)}</strong>
                {preco?.mensalista && Number(preco.desconto) > 0 && (
                  <span className="text-green-700"> (desconto de mensalista: −{brl(preco.desconto)})</span>
                )}
                {' '}· pagamento na recepção
              </p>
              {politica && (
                <p className="text-xs text-gray-400 mb-3 mt-1">
                  Cancelamento gratuito até {politica.horasCancelamentoGratis}h antes; depois disso, multa de {Number(politica.percentualMulta)}% do valor.
                </p>
              )}
              <button
                onClick={handleConfirmar}
                className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 transition"
              >
                Confirmar reserva
              </button>
            </div>
          )}

          {slotSelecionado && !slotSelecionado.disponivel && (
            <div className="bg-white rounded-xl shadow p-4 sticky bottom-20">
              <p className="font-semibold text-araca-azul flex items-center gap-1.5">
                <BellRing size={16} /> Horário ocupado
              </p>
              <p className="text-sm text-gray-500 mb-3">
                {format(diaEscolhido, 'dd/MM')} · {format(new Date(slotSelecionado.inicio), 'HH:mm')}–{format(new Date(slotSelecionado.fim), 'HH:mm')}.
                Se alguém cancelar, avisamos você por e-mail e quem reservar primeiro garante a vaga.
              </p>
              <button
                onClick={handleListaEspera}
                className="w-full bg-araca-azul text-white font-semibold py-3 rounded-lg hover:opacity-90 transition"
              >
                Entrar na lista de espera
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
