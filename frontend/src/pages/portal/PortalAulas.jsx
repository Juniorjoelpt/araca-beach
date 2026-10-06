import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { Package, GraduationCap } from 'lucide-react'

import { portalReservaService } from '../../services/portalReservaService.js'

const DIA = {
  MONDAY: 'Segunda', TUESDAY: 'Terça', WEDNESDAY: 'Quarta', THURSDAY: 'Quinta',
  FRIDAY: 'Sexta', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
}

const STATUS_AULA = {
  AGENDADA: { texto: 'Agendada', cor: 'bg-green-100 text-green-700' },
  REALIZADA: { texto: 'Realizada', cor: 'bg-blue-100 text-blue-700' },
  FALTA_AVISADA: { texto: 'Falta avisada (crédito devolvido)', cor: 'bg-amber-100 text-amber-700' },
  FALTA_SEM_AVISO: { texto: 'Falta sem aviso', cor: 'bg-red-100 text-red-700' },
  CANCELADA: { texto: 'Cancelada', cor: 'bg-gray-100 text-gray-500' },
}

export default function PortalAulas() {
  const [pacotes, setPacotes] = useState([])
  const [aulas, setAulas] = useState([])
  const [turmas, setTurmas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')

  async function carregar() {
    try {
      const [p, a, t] = await Promise.all([
        portalReservaService.pacotes(),
        portalReservaService.aulas(),
        portalReservaService.turmas(),
      ])
      setPacotes(p)
      setAulas(a)
      setTurmas(t)
    } catch {
      setErro('Não foi possível carregar suas aulas.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  async function handleAvisarFalta(aula) {
    if (!confirm('Avisar que você não vai à aula? O crédito volta para o seu pacote para reposição.')) return
    setErro('')
    setAviso('')
    try {
      await portalReservaService.avisarFalta(aula.id)
      setAviso('Falta avisada. O crédito voltou para o seu pacote — combine a reposição com a recepção.')
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível avisar a falta.')
    }
  }

  const agora = new Date()
  const proximas = aulas.filter((a) => a.status === 'AGENDADA' && new Date(a.fim) >= agora)
    .sort((a, b) => new Date(a.inicio) - new Date(b.inicio))
  const anteriores = aulas.filter((a) => !proximas.includes(a))
  const pacotesAtivos = pacotes.filter((p) => p.situacao === 'ATIVO')

  return (
    <div>
      <h1 className="font-title text-xl text-araca-azul mb-4">Minhas aulas</h1>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {aviso && <p className="text-green-700 text-sm mb-3">{aviso}</p>}
      {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}

      {!carregando && pacotes.length === 0 && turmas.length === 0 && (
        <p className="text-gray-400 text-sm">Você ainda não tem pacotes nem turmas. Fale com a recepção para começar suas aulas.</p>
      )}

      {pacotesAtivos.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1.5"><Package size={15} /> Meus pacotes</h2>
          <div className="space-y-2">
            {pacotesAtivos.map((p) => (
              <div key={p.id} className="bg-white rounded-xl shadow p-4">
                <div className="flex items-start justify-between">
                  <p className="font-semibold text-araca-azul">{p.planoNome}</p>
                  <span className="text-2xl font-bold text-araca-verde-escuro leading-none">{p.saldo}</span>
                </div>
                <p className="text-xs text-gray-500">
                  {p.saldo} de {p.aulasTotal} aulas restantes · válido até {format(new Date(p.validade + 'T00:00:00'), 'dd/MM/yyyy')}
                </p>
                <div className="h-1.5 bg-gray-100 rounded-full mt-2 overflow-hidden">
                  <div className="h-full bg-araca-verde" style={{ width: `${(p.aulasConsumidas / p.aulasTotal) * 100}%` }} />
                </div>
                {!p.pago && <p className="text-xs text-amber-600 mt-2">Pagamento pendente na recepção</p>}
              </div>
            ))}
          </div>
        </div>
      )}

      {proximas.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Próximas aulas</h2>
          <div className="space-y-2">
            {proximas.map((a) => (
              <div key={a.id} className="bg-white rounded-xl shadow p-4">
                <p className="font-semibold text-araca-azul">{a.planoNome}</p>
                <p className="text-sm text-gray-600">{format(new Date(a.inicio), 'dd/MM/yyyy · HH:mm')} – {format(new Date(a.fim), 'HH:mm')}</p>
                <p className="text-xs text-gray-500">Prof. {a.professorNome} · {a.quadraNome}</p>
                <button onClick={() => handleAvisarFalta(a)} className="text-red-600 text-sm mt-2 hover:underline">
                  Avisar que vou faltar
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {turmas.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1.5"><GraduationCap size={15} /> Minhas turmas</h2>
          <div className="space-y-2">
            {turmas.map((t) => (
              <div key={t.matriculaId} className="bg-white rounded-xl shadow p-4">
                <p className="font-semibold text-araca-azul">{t.turmaNome}</p>
                <p className="text-sm text-gray-600">{DIA[t.diaSemana] || t.diaSemana} · {t.horaInicio.slice(0, 5)}–{t.horaFim.slice(0, 5)}</p>
                <p className="text-xs text-gray-500">Prof. {t.professorNome} · {t.quadraNome}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {anteriores.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Histórico de aulas</h2>
          <div className="space-y-2">
            {anteriores.map((a) => {
              const st = STATUS_AULA[a.status] || { texto: a.status, cor: 'bg-gray-100 text-gray-500' }
              return (
                <div key={a.id} className="bg-white rounded-xl shadow p-3 flex items-center justify-between">
                  <div>
                    <p className="text-sm text-araca-azul">{format(new Date(a.inicio), 'dd/MM/yyyy · HH:mm')}</p>
                    <p className="text-xs text-gray-500">{a.planoNome} · Prof. {a.professorNome}</p>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-[11px] font-medium text-right ${st.cor}`}>{st.texto}</span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
