import { useEffect, useMemo, useState } from 'react'
import { format } from 'date-fns'
import { Minus, Plus, Ticket, Users } from 'lucide-react'

import { portalDayUseService } from '../../services/portalDayUseService.js'

const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const hoje = () => format(new Date(), 'yyyy-MM-dd')
const dataBr = (s) => (s ? s.split('-').reverse().join('/') : '')

const STATUS = {
  PENDENTE: { texto: 'Aguardando pagamento', cor: 'bg-amber-100 text-amber-700' },
  ATIVA: { texto: 'Confirmado', cor: 'bg-green-100 text-green-700' },
  CANCELADA: { texto: 'Cancelado', cor: 'bg-gray-100 text-gray-500' },
}

export default function PortalDayUse() {
  const [data, setData] = useState(hoje())
  const [disp, setDisp] = useState(null)
  const [qtd, setQtd] = useState({})
  const [obs, setObs] = useState('')
  const [minhas, setMinhas] = useState([])
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [ok, setOk] = useState('')

  async function carregarMinhas() {
    try { setMinhas(await portalDayUseService.minhas()) } catch { /* ignora */ }
  }

  useEffect(() => { carregarMinhas() }, [])

  useEffect(() => {
    setErro('')
    setQtd({})
    portalDayUseService.disponibilidade(data).then(setDisp).catch(() => {
      setDisp(null)
      setErro('Não foi possível carregar a disponibilidade.')
    })
  }, [data])

  const itens = useMemo(
    () => Object.entries(qtd).filter(([, q]) => q > 0).map(([tipoId, quantidade]) => ({ tipoId: Number(tipoId), quantidade })),
    [qtd],
  )
  const { total, pessoas } = useMemo(() => {
    let t = 0
    let p = 0
    for (const tp of disp?.tipos || []) {
      const q = qtd[tp.id] || 0
      t += q * Number(tp.preco)
      p += q * tp.pessoasPorUnidade
    }
    return { total: t, pessoas: p }
  }, [qtd, disp])

  const vagas = disp?.vagasRestantes
  const excede = vagas != null && pessoas > vagas

  function ajustar(id, delta) {
    setQtd((q) => ({ ...q, [id]: Math.max(0, (q[id] || 0) + delta) }))
  }

  async function reservar() {
    setErro(''); setOk(''); setEnviando(true)
    try {
      await portalDayUseService.reservar({ data, observacao: obs || null, itens })
      setOk('Reserva feita! Ela fica confirmada quando o pagamento for registrado na recepção.')
      setQtd({}); setObs('')
      carregarMinhas()
      portalDayUseService.disponibilidade(data).then(setDisp).catch(() => {})
    } catch (e) {
      setErro(e?.response?.data?.message || 'Não foi possível reservar.')
    } finally {
      setEnviando(false)
    }
  }

  async function cancelar(id) {
    if (!window.confirm('Cancelar esta reserva?')) return
    try {
      await portalDayUseService.cancelar(id)
      carregarMinhas()
    } catch (e) {
      setErro(e?.response?.data?.message || 'Não foi possível cancelar.')
    }
  }

  return (
    <div className="space-y-5 pb-6">
      <div className="flex items-center gap-2">
        <Ticket className="text-araca-azul" size={22} />
        <h1 className="font-title text-xl text-araca-azul">Day Use</h1>
      </div>

      {disp?.mensagemPortal && (
        <p className="text-sm bg-white rounded-xl p-3 text-gray-600 shadow-sm">{disp.mensagemPortal}</p>
      )}

      <div className="bg-white rounded-xl p-4 shadow-sm space-y-4">
        <label className="block text-sm font-medium text-gray-700">
          Data
          <input type="date" min={hoje()} value={data} onChange={(e) => e.target.value && setData(e.target.value)}
            className="mt-1 w-full border rounded-lg px-3 py-2" />
        </label>

        {vagas != null && (
          <p className={`text-sm flex items-center gap-1 ${vagas === 0 ? 'text-red-600' : 'text-gray-500'}`}>
            <Users size={14} /> {vagas === 0 ? 'Dia lotado' : `${vagas} vagas restantes`}
          </p>
        )}

        {(disp?.tipos || []).length === 0 && !erro && (
          <p className="text-sm text-gray-500">Nenhum ingresso disponível para esta data.</p>
        )}

        {(disp?.tipos || []).map((t) => (
          <div key={t.id} className="flex items-center justify-between gap-3 border-t pt-3">
            <div>
              <p className="font-medium text-gray-800">{t.nome}</p>
              <p className="text-sm text-gray-500">
                {brl(t.preco)}{t.pessoasPorUnidade > 1 && ` · pacote para ${t.pessoasPorUnidade} pessoas`}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button type="button" onClick={() => ajustar(t.id, -1)} className="w-8 h-8 rounded-full border flex items-center justify-center"><Minus size={14} /></button>
              <span className="w-6 text-center">{qtd[t.id] || 0}</span>
              <button type="button" onClick={() => ajustar(t.id, 1)} className="w-8 h-8 rounded-full bg-araca-azul text-white flex items-center justify-center"><Plus size={14} /></button>
            </div>
          </div>
        ))}

        {itens.length > 0 && (
          <>
            <textarea value={obs} onChange={(e) => setObs(e.target.value)} maxLength={300} rows={2}
              placeholder="Observação (opcional)" className="w-full border rounded-lg px-3 py-2 text-sm" />
            <div className="flex justify-between text-sm">
              <span>{pessoas} {pessoas === 1 ? 'pessoa' : 'pessoas'}</span>
              <span className="font-semibold">{brl(total)}</span>
            </div>
            {excede && <p className="text-sm text-red-600">Só restam {vagas} vagas neste dia.</p>}
            <button type="button" disabled={enviando || excede} onClick={reservar}
              className="w-full bg-araca-azul text-white rounded-lg py-3 font-medium disabled:opacity-50">
              {enviando ? 'Reservando...' : 'Reservar'}
            </button>
          </>
        )}

        {erro && <p className="text-sm text-red-600">{erro}</p>}
        {ok && <p className="text-sm text-green-700">{ok}</p>}
      </div>

      <div className="space-y-3">
        <h2 className="font-title text-araca-azul">Minhas reservas de Day Use</h2>
        {minhas.length === 0 && <p className="text-sm text-gray-500">Você ainda não tem reservas.</p>}
        {minhas.map((r) => {
          const st = STATUS[r.status] || STATUS.PENDENTE
          return (
            <div key={r.id} className="bg-white rounded-xl p-4 shadow-sm space-y-1">
              <div className="flex justify-between items-center">
                <p className="font-medium">{dataBr(r.data)}</p>
                <span className={`text-xs px-2 py-0.5 rounded-full ${st.cor}`}>{st.texto}</span>
              </div>
              <p className="text-sm text-gray-600">
                {r.itens.map((i) => `${i.quantidade}x ${i.tipoNome}`).join(', ')} · {r.pessoas} {r.pessoas === 1 ? 'pessoa' : 'pessoas'}
              </p>
              <p className="text-sm font-semibold">{brl(r.valorTotal)}</p>
              {r.status === 'PENDENTE' && (
                <button type="button" onClick={() => cancelar(r.id)} className="text-sm text-red-600 underline">Cancelar</button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
