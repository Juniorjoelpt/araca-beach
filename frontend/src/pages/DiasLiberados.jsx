import { useEffect, useState } from 'react'
import { format, parseISO } from 'date-fns'
import { CalendarOff, Trash2 } from 'lucide-react'
import { diaLiberadoService } from '../services/diaLiberadoService.js'

const input = 'w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde'

function Lista({ titulo, itens, cor }) {
  if (!itens?.length) return null
  return (
    <div>
      <p className={`text-sm font-semibold ${cor}`}>{titulo} ({itens.length})</p>
      <ul className="text-sm text-gray-700 mt-1 space-y-0.5">
        {itens.map((i, idx) => (
          <li key={`${i.id}-${idx}`}>{i.hora} · {i.quadra} · {i.descricao}</li>
        ))}
      </ul>
    </div>
  )
}

export default function DiasLiberados() {
  const [dias, setDias] = useState([])
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [motivo, setMotivo] = useState('')
  const [previa, setPrevia] = useState(null)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)

  async function carregar() {
    try { setDias(await diaLiberadoService.listar()) } catch { setErro('Não foi possível carregar os dias liberados.') }
  }
  useEffect(() => { carregar() }, [])

  async function verPrevia(e) {
    e.preventDefault()
    setErro(''); setAviso(''); setPrevia(null)
    try { setPrevia(await diaLiberadoService.previa(data)) } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível calcular o que será afetado.')
    }
  }

  async function confirmar() {
    if (ocupado) return
    setOcupado(true); setErro('')
    try {
      await diaLiberadoService.liberar({ data, motivo })
      setAviso(`Dia ${format(parseISO(data), 'dd/MM/yyyy')} liberado para reservas.`)
      setPrevia(null); setMotivo('')
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível liberar o dia.')
    } finally { setOcupado(false) }
  }

  async function desfazer(d) {
    if (!window.confirm(`Desfazer a liberação de ${format(parseISO(d.data), 'dd/MM/yyyy')}? As turmas voltam a bloquear a quadra e as reservas fixas canceladas por ela são restauradas quando o horário estiver livre. Aulas avulsas canceladas não voltam.`)) return
    setErro(''); setAviso('')
    try { await diaLiberadoService.desfazer(d.id); setAviso('Liberação desfeita.'); carregar() } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível desfazer.')
    }
  }

  const nada = previa && !previa.reservasCanceladas.length && !previa.turmasLiberadas.length
    && !previa.aulasCanceladas.length && !previa.avisos.length

  return (
    <div>
      <h2 className="text-2xl font-bold text-araca-azul mb-1 flex items-center gap-2"><CalendarOff size={24} /> Feriados / dia liberado</h2>
      <p className="text-sm text-gray-500 mb-4">
        Libera a quadra para reserva normal em um dia específico: cancela só naquele dia as reservas fixas, as turmas e as aulas avulsas.
        Mensalidades continuam com o valor fixo. Reservas e aulas já pagas não são canceladas (viram aviso).
      </p>

      {erro && <p className="mb-3 text-red-600 text-sm">{erro}</p>}
      {aviso && <p className="mb-3 text-green-700 text-sm">{aviso}</p>}

      <form onSubmit={verPrevia} className="bg-white rounded-xl shadow p-4 mb-6 grid grid-cols-1 sm:grid-cols-3 gap-3 items-end">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Data do feriado</label>
          <input type="date" className={input} value={data} onChange={(e) => { setData(e.target.value); setPrevia(null) }} required />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Motivo</label>
          <input className={input} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Ex.: Proclamação da República" maxLength={200} />
        </div>
        <button type="submit" className="bg-araca-azul text-white font-semibold rounded-lg py-2 hover:opacity-90">Ver o que será afetado</button>
      </form>

      {previa && (
        <div className="bg-white rounded-xl shadow p-4 mb-6 space-y-3">
          <h3 className="font-semibold text-araca-azul">
            {format(parseISO(previa.data), 'dd/MM/yyyy')}
            {previa.jaLiberado && <span className="ml-2 text-amber-700 text-sm">(já está liberado)</span>}
          </h3>
          <Lista titulo="Reservas fixas que serão canceladas só nesse dia" itens={previa.reservasCanceladas} cor="text-red-700" />
          <Lista titulo="Turmas que deixam de ocupar a quadra" itens={previa.turmasLiberadas} cor="text-red-700" />
          <Lista titulo="Aulas avulsas que serão canceladas" itens={previa.aulasCanceladas} cor="text-red-700" />
          <Lista titulo="Avisos (não serão cancelados)" itens={previa.avisos} cor="text-amber-700" />
          {nada && <p className="text-sm text-gray-600">Nada fixo nesse dia: o dia será apenas marcado como liberado, para que recorrências e turmas não ocupem o horário.</p>}
          {!previa.jaLiberado && (
            <div className="flex gap-2 pt-2 flex-wrap">
              <button type="button" onClick={() => setPrevia(null)} className="border rounded-lg px-4 py-2 text-gray-600 hover:bg-gray-50">Cancelar</button>
              <button type="button" disabled={ocupado} onClick={confirmar}
                className="bg-araca-verde text-araca-azul font-semibold rounded-lg px-4 py-2 hover:opacity-90 disabled:opacity-50">
                Confirmar e liberar o dia
              </button>
            </div>
          )}
        </div>
      )}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="px-4 py-3 bg-gray-50 border-b"><h3 className="font-semibold text-araca-azul text-sm">Dias liberados</h3></div>
        {dias.length === 0 ? (
          <p className="p-4 text-sm text-gray-500">Nenhum dia liberado.</p>
        ) : (
          <ul>
            {dias.map((d) => (
              <li key={d.id} className="flex items-center justify-between gap-3 px-4 py-3 border-t first:border-t-0">
                <span className="text-sm"><strong>{format(parseISO(d.data), 'dd/MM/yyyy')}</strong> · {d.motivo}</span>
                <button type="button" onClick={() => desfazer(d)} className="text-red-600 hover:text-red-800" title="Desfazer liberação"><Trash2 size={18} /></button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
