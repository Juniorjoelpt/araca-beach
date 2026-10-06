import { useEffect, useState } from 'react'
import { format, startOfMonth, endOfMonth } from 'date-fns'
import { Percent } from 'lucide-react'
import { comissaoService } from '../services/comissaoService.js'

const ORIGEM = {
  AULA_AVULSA: 'Aula avulsa',
  AULA_PACOTE: 'Aula de pacote',
  MATRICULA: 'Mensalidade de turma',
}

const brl = (v) => `R$ ${Number(v).toFixed(2).replace('.', ',')}`

export default function Comissoes() {
  const [inicio, setInicio] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'))
  const [fim, setFim] = useState(format(endOfMonth(new Date()), 'yyyy-MM-dd'))
  const [resumo, setResumo] = useState([])
  const [professorId, setProfessorId] = useState(null)
  const [lancamentos, setLancamentos] = useState([])
  const [selecionados, setSelecionados] = useState([])
  const [lancarDespesa, setLancarDespesa] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  async function carregarResumo() {
    setErro('')
    try {
      setResumo(await comissaoService.resumo(inicio, fim))
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível carregar as comissões.')
    }
  }

  async function carregarLancamentos(id) {
    try {
      const dados = await comissaoService.listar({ professorId: id, inicio, fim })
      setLancamentos(dados)
      setSelecionados(dados.filter((l) => l.status === 'PENDENTE').map((l) => l.id))
    } catch {
      setErro('Não foi possível carregar os lançamentos.')
    }
  }

  useEffect(() => {
    carregarResumo()
    setProfessorId(null)
    setLancamentos([])
  }, [inicio, fim])

  function abrir(id) {
    setProfessorId(id)
    setSucesso('')
    carregarLancamentos(id)
  }

  function alternar(id) {
    setSelecionados((atual) => (atual.includes(id) ? atual.filter((x) => x !== id) : [...atual, id]))
  }

  const totalSelecionado = lancamentos
    .filter((l) => selecionados.includes(l.id))
    .reduce((soma, l) => soma + Number(l.valor), 0)

  async function pagar() {
    if (selecionados.length === 0) return
    if (!confirm(`Marcar ${selecionados.length} lançamento(s) como pagos (${brl(totalSelecionado)})?`)) return
    setErro('')
    try {
      await comissaoService.pagar({ professorId, ids: selecionados, lancarDespesa })
      setSucesso(`Comissão de ${brl(totalSelecionado)} paga${lancarDespesa ? ' e lançada em Despesas' : ''}.`)
      carregarResumo()
      carregarLancamentos(professorId)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível registrar o pagamento.')
    }
  }

  const pendentes = lancamentos.filter((l) => l.status === 'PENDENTE')

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Percent size={22} className="text-araca-verde-escuro" />
        Comissões dos professores
      </h2>

      <div className="bg-white rounded-xl shadow p-4 mb-6 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
          <input type="date" className="border rounded-lg px-3 py-2" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
          <input type="date" className="border rounded-lg px-3 py-2" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
        <p className="text-xs text-gray-400 max-w-md">
          As comissões são lançadas automaticamente: aula avulsa cadastrada, mensalidade de turma paga e aula de pacote realizada (ou falta sem aviso).
          Usa o % de comissão do cadastro do professor.
        </p>
      </div>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}
      {sucesso && <p className="text-green-700 text-sm mb-4">{sucesso}</p>}

      <div className="bg-white rounded-xl shadow overflow-x-auto mb-6">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr><th className="px-4 py-3">Professor</th><th className="px-4 py-3">%</th><th className="px-4 py-3">Lançamentos pendentes</th><th className="px-4 py-3">A pagar</th><th className="px-4 py-3">Já pago</th><th className="px-4 py-3" /></tr>
          </thead>
          <tbody>
            {resumo.length === 0 && <tr><td colSpan="6" className="px-4 py-6 text-gray-400 text-center">Nenhuma comissão no período.</td></tr>}
            {resumo.map((r) => (
              <tr key={r.professorId} className={`border-t ${professorId === r.professorId ? 'bg-araca-areia/50' : ''}`}>
                <td className="px-4 py-3 font-medium text-araca-azul">{r.professorNome}</td>
                <td className="px-4 py-3">{Number(r.percentual)}%</td>
                <td className="px-4 py-3">{r.quantidadePendentes}</td>
                <td className="px-4 py-3 font-semibold">{brl(r.valorPendente)}</td>
                <td className="px-4 py-3">{brl(r.valorPago)}</td>
                <td className="px-4 py-3 text-right"><button onClick={() => abrir(r.professorId)} className="text-xs text-araca-azul underline">Ver lançamentos</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {professorId && (
        <div className="bg-white rounded-xl shadow overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-gray-500">
              <tr><th className="px-4 py-3" /><th className="px-4 py-3">Data</th><th className="px-4 py-3">Origem</th><th className="px-4 py-3">Descrição</th><th className="px-4 py-3">Base</th><th className="px-4 py-3">%</th><th className="px-4 py-3">Comissão</th><th className="px-4 py-3">Situação</th></tr>
            </thead>
            <tbody>
              {lancamentos.map((l) => (
                <tr key={l.id} className="border-t">
                  <td className="px-4 py-3">
                    {l.status === 'PENDENTE' && <input type="checkbox" checked={selecionados.includes(l.id)} onChange={() => alternar(l.id)} />}
                  </td>
                  <td className="px-4 py-3">{format(new Date(l.competencia + 'T00:00:00'), 'dd/MM/yyyy')}</td>
                  <td className="px-4 py-3">{ORIGEM[l.origem] || l.origem}</td>
                  <td className="px-4 py-3">{l.descricao}</td>
                  <td className="px-4 py-3">{brl(l.base)}</td>
                  <td className="px-4 py-3">{Number(l.percentual)}%</td>
                  <td className="px-4 py-3 font-medium">{brl(l.valor)}</td>
                  <td className="px-4 py-3">{l.status === 'PAGA' ? `Paga em ${format(new Date(l.dataPagamento + 'T00:00:00'), 'dd/MM')}` : 'Pendente'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {pendentes.length > 0 && (
            <div className="p-4 border-t flex flex-wrap items-center gap-4">
              <p className="text-sm">Selecionado: <strong>{brl(totalSelecionado)}</strong></p>
              <label className="flex items-center gap-2 text-sm text-gray-600">
                <input type="checkbox" checked={lancarDespesa} onChange={(e) => setLancarDespesa(e.target.checked)} />
                Lançar como despesa paga (categoria Comissão)
              </label>
              <button onClick={pagar} disabled={selecionados.length === 0}
                className="bg-araca-verde text-araca-azul font-semibold px-5 py-2 rounded-lg hover:opacity-90 transition disabled:opacity-40">
                Marcar como pago
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
