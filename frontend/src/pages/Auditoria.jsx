import { useEffect, useState } from 'react'
import { format, subDays } from 'date-fns'
import { ShieldCheck } from 'lucide-react'
import { auditoriaService } from '../services/auditoriaService.js'

const TIPO = { EQUIPE: 'Equipe', CLIENTE: 'Cliente (portal)', SISTEMA: 'Sistema' }
const iso = (d) => format(d, 'yyyy-MM-dd')

export default function Auditoria() {
  const [inicio, setInicio] = useState(iso(subDays(new Date(), 6)))
  const [fim, setFim] = useState(iso(new Date()))
  const [usuario, setUsuario] = useState('')
  const [recurso, setRecurso] = useState('')
  const [tipoUsuario, setTipoUsuario] = useState('')
  const [texto, setTexto] = useState('')
  const [recursos, setRecursos] = useState([])
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState({ itens: [], totalPaginas: 0, total: 0 })
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function buscar(p = 0) {
    setCarregando(true)
    setErro('')
    try {
      const params = { inicio, fim, pagina: p, tamanho: 50 }
      if (usuario) params.usuario = usuario
      if (recurso) params.recurso = recurso
      if (tipoUsuario) params.tipoUsuario = tipoUsuario
      if (texto) params.texto = texto
      setResultado(await auditoriaService.listar(params))
      setPagina(p)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível carregar a auditoria.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    buscar(0)
    auditoriaService.recursos().then(setRecursos).catch(() => {})
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-2 flex items-center gap-2"><ShieldCheck size={24} /> Auditoria</h2>
      <p className="text-sm text-gray-500 mb-6">Registro de quem fez cada alteração no sistema (reservas, pagamentos, usuários, regras etc.). Somente administradores veem esta tela.</p>

      <form onSubmit={(e) => { e.preventDefault(); buscar(0) }} className="bg-white rounded-xl shadow p-4 mb-6 grid grid-cols-2 lg:grid-cols-6 gap-3 items-end">
        <div>
          <label className="block text-xs text-gray-600 mb-1">De</label>
          <input type="date" className="border rounded-lg px-2 py-2 w-full" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Até</label>
          <input type="date" className="border rounded-lg px-2 py-2 w-full" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Usuário</label>
          <input className="border rounded-lg px-2 py-2 w-full" placeholder="login ou e-mail" value={usuario} onChange={(e) => setUsuario(e.target.value)} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Módulo</label>
          <select className="border rounded-lg px-2 py-2 w-full" value={recurso} onChange={(e) => setRecurso(e.target.value)}>
            <option value="">Todos</option>
            {recursos.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Quem</label>
          <select className="border rounded-lg px-2 py-2 w-full" value={tipoUsuario} onChange={(e) => setTipoUsuario(e.target.value)}>
            <option value="">Todos</option>
            <option value="EQUIPE">Equipe</option>
            <option value="CLIENTE">Cliente (portal)</option>
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Busca no texto</label>
          <input className="border rounded-lg px-2 py-2 w-full" placeholder="ação ou detalhe" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </div>
        <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg col-span-2 lg:col-span-1">Filtrar</button>
      </form>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="bg-white rounded-xl shadow overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-600">
            <tr>
              <th className="px-3 py-2">Quando</th>
              <th className="px-3 py-2">Quem</th>
              <th className="px-3 py-2">Ação</th>
              <th className="px-3 py-2">Detalhe</th>
              <th className="px-3 py-2">IP</th>
            </tr>
          </thead>
          <tbody>
            {resultado.itens.map((r) => (
              <tr key={r.id} className="border-t align-top">
                <td className="px-3 py-2 whitespace-nowrap">{format(new Date(r.criadoEm), 'dd/MM/yyyy HH:mm:ss')}</td>
                <td className="px-3 py-2">
                  <div className="font-medium">{r.usuario || '—'}</div>
                  <div className="text-xs text-gray-400">{TIPO[r.tipoUsuario] || r.tipoUsuario}{r.perfil && r.perfil !== 'CLIENTE' ? ` · ${r.perfil}` : ''}</div>
                </td>
                <td className="px-3 py-2">
                  <div>{r.acao}{r.recursoId ? <span className="text-gray-400"> #{r.recursoId}</span> : null}</div>
                  {r.metodo && <div className="text-xs text-gray-400">{r.metodo} {r.caminho}</div>}
                </td>
                <td className="px-3 py-2 text-gray-600 max-w-md break-words">{r.detalhe || ''}</td>
                <td className="px-3 py-2 text-gray-400 whitespace-nowrap">{r.ip}</td>
              </tr>
            ))}
            {!carregando && resultado.itens.length === 0 && (
              <tr><td colSpan={5} className="px-3 py-6 text-center text-gray-400">Nenhum registro no período.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
        <span>{resultado.total} registro(s)</span>
        <div className="flex items-center gap-3">
          <button disabled={pagina === 0 || carregando} onClick={() => buscar(pagina - 1)} className="px-3 py-1 border rounded-lg disabled:opacity-40">Anterior</button>
          <span>Página {resultado.totalPaginas === 0 ? 0 : pagina + 1} de {resultado.totalPaginas}</span>
          <button disabled={pagina + 1 >= resultado.totalPaginas || carregando} onClick={() => buscar(pagina + 1)} className="px-3 py-1 border rounded-lg disabled:opacity-40">Próxima</button>
        </div>
      </div>
    </div>
  )
}
