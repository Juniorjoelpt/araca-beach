import { useCallback, useEffect, useState } from 'react'
import { format } from 'date-fns'
import { History, FileDown } from 'lucide-react'
import { caixaService } from '../services/caixaService.js'

const brl = (v) => `R$ ${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const iso = (d) => format(d, 'yyyy-MM-dd')

const FORMA = { PIX: 'Pix', CARTAO_CREDITO: 'Cartão de crédito', CARTAO_DEBITO: 'Cartão de débito', DINHEIRO: 'Dinheiro' }
const ORIGEM = {
  RESERVA: 'Quadras', RESTAURANTE: 'Restaurante', LOJA: 'Loja', MENSALIDADE: 'Mensalidade',
  MATRICULA: 'Aulas', PACOTE: 'Pacote de aulas', OUTROS: 'Outros',
}

function msgErro(err, padrao) {
  return err?.response?.data?.mensagem || padrao
}

function Totais({ titulo, dados, rotulos }) {
  const linhas = Object.entries(dados || {})
  return (
    <div className="bg-araca-areia rounded-lg p-3">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-araca-verde-escuro mb-1">{titulo}</p>
      {linhas.length === 0 && <p className="text-sm text-gray-400">—</p>}
      {linhas.map(([k, v]) => (
        <div key={k} className="flex justify-between text-sm gap-2">
          <span className="text-gray-700 truncate">{rotulos ? rotulos[k] || k : k}</span>
          <span className="font-semibold text-araca-azul whitespace-nowrap">{brl(v)}</span>
        </div>
      ))}
    </div>
  )
}

export default function HistoricoCaixa() {
  const usuario = JSON.parse(localStorage.getItem('araca_beach_usuario') || 'null')
  const admin = usuario?.perfil === 'ADMIN'
  const hoje = iso(new Date())
  const [filtros, setFiltros] = useState({ inicio: hoje, fim: hoje, operador: '', origem: '', forma: '' })
  const [dados, setDados] = useState(null)
  const [operadores, setOperadores] = useState([])
  const [carregando, setCarregando] = useState(false)
  const [baixando, setBaixando] = useState(false)
  const [erro, setErro] = useState('')

  const buscar = useCallback(async (f) => {
    setErro('')
    setCarregando(true)
    try {
      setDados(await caixaService.historico(f))
    } catch (err) {
      setDados(null)
      setErro(msgErro(err, 'Não foi possível carregar o histórico de caixa.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    buscar(filtros)
    if (admin) caixaService.operadores().then(setOperadores).catch(() => {})
    // carga inicial apenas
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const set = (campo) => (e) => setFiltros((f) => ({ ...f, [campo]: e.target.value }))

  async function exportarPdf() {
    setErro('')
    setBaixando(true)
    try {
      await caixaService.baixarPdf(filtros)
    } catch (err) {
      let m = 'Não foi possível gerar o PDF.'
      const corpo = err?.response?.data
      if (corpo instanceof Blob) {
        try { m = JSON.parse(await corpo.text())?.mensagem || m } catch { /* mantém padrão */ }
      }
      setErro(m)
    } finally {
      setBaixando(false)
    }
  }

  const input = 'border rounded-lg px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-araca-verde'

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-1 flex items-center gap-2">
        <History size={22} className="text-araca-verde-escuro" />
        Histórico de caixa
      </h2>
      <p className="text-sm text-araca-azul/70 mb-4">
        {admin ? 'Todos os recebimentos da arena, com o operador que registrou cada um.' : 'Os recebimentos que você registrou.'}
      </p>

      <div className="bg-white rounded-xl shadow p-4 mb-4">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
          <label className="text-xs text-gray-600">De
            <input type="date" className={input} value={filtros.inicio} onChange={set('inicio')} />
          </label>
          <label className="text-xs text-gray-600">Até
            <input type="date" className={input} value={filtros.fim} onChange={set('fim')} />
          </label>
          <label className="text-xs text-gray-600">Origem
            <select className={input} value={filtros.origem} onChange={set('origem')}>
              <option value="">Todas</option>
              {Object.entries(ORIGEM).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          <label className="text-xs text-gray-600">Forma
            <select className={input} value={filtros.forma} onChange={set('forma')}>
              <option value="">Todas</option>
              {Object.entries(FORMA).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          {admin && (
            <label className="text-xs text-gray-600">Operador
              <select className={input} value={filtros.operador} onChange={set('operador')}>
                <option value="">Todos</option>
                {operadores.map((o) => <option key={o.login} value={o.login}>{o.nome}</option>)}
              </select>
            </label>
          )}
        </div>
        <div className="flex flex-wrap gap-2 mt-3">
          <button onClick={() => buscar(filtros)} disabled={carregando} className="bg-araca-azul text-white font-semibold px-4 py-2 rounded-lg text-sm disabled:opacity-50">
            {carregando ? 'Buscando...' : 'Buscar'}
          </button>
          <button onClick={exportarPdf} disabled={baixando} className="flex items-center gap-2 bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg text-sm disabled:opacity-50">
            <FileDown size={16} /> {baixando ? 'Gerando...' : 'Exportar PDF'}
          </button>
        </div>
      </div>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}

      {dados && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
            <div className="bg-araca-verde rounded-lg p-3">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-araca-azul">Total recebido</p>
              <p className="text-2xl font-bold text-araca-azul">{brl(dados.total)}</p>
              <p className="text-xs text-araca-azul">{dados.quantidade} recebimento(s)</p>
            </div>
            <Totais titulo="Por forma de pagamento" dados={dados.porForma} rotulos={FORMA} />
            <Totais titulo="Por origem" dados={dados.porOrigem} rotulos={ORIGEM} />
            <Totais titulo="Por operador" dados={dados.porOperador} />
          </div>

          <div className="bg-white rounded-xl shadow overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-araca-azul text-araca-verde text-left">
                <tr>
                  <th className="px-3 py-2">Data/hora</th>
                  <th className="px-3 py-2">Origem</th>
                  <th className="px-3 py-2">Descrição</th>
                  <th className="px-3 py-2">Forma</th>
                  <th className="px-3 py-2">Operador</th>
                  <th className="px-3 py-2 text-right">Valor</th>
                </tr>
              </thead>
              <tbody>
                {dados.lancamentos.length === 0 && (
                  <tr><td colSpan={6} className="px-3 py-6 text-center text-gray-400">Nenhum recebimento neste período.</td></tr>
                )}
                {dados.lancamentos.map((l) => (
                  <tr key={l.id} className="border-t odd:bg-araca-areia/50">
                    <td className="px-3 py-2 whitespace-nowrap">{format(new Date(l.dataHora), 'dd/MM/yyyy HH:mm')}</td>
                    <td className="px-3 py-2">{ORIGEM[l.origem] || l.origem}</td>
                    <td className="px-3 py-2">{l.descricao}</td>
                    <td className="px-3 py-2 whitespace-nowrap">{FORMA[l.forma] || l.forma}</td>
                    <td className="px-3 py-2 whitespace-nowrap">{l.operadorNome || <span className="text-gray-400">—</span>}</td>
                    <td className="px-3 py-2 text-right font-semibold text-araca-azul whitespace-nowrap">{brl(l.valor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {admin && dados.lancamentos.some((l) => !l.operador) && (
            <p className="text-xs text-gray-500 mt-2">
              Recebimentos anteriores a esta função aparecem sem operador, pois o sistema ainda não registrava quem recebeu.
            </p>
          )}
        </>
      )}
    </div>
  )
}
