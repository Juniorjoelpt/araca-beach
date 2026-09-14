import { useEffect, useState } from 'react'
import { format, startOfMonth } from 'date-fns'
import { Boxes, ArrowDownCircle, ArrowUpCircle, SlidersHorizontal, X } from 'lucide-react'
import { produtoService } from '../services/produtoService.js'
import { estoqueService } from '../services/estoqueService.js'

const TIPO_INFO = {
  ENTRADA: { label: 'Entrada', cor: 'bg-green-100 text-green-700' },
  SAIDA: { label: 'Saída', cor: 'bg-red-100 text-red-700' },
  AJUSTE: { label: 'Ajuste', cor: 'bg-amber-100 text-amber-700' },
  VENDA: { label: 'Venda', cor: 'bg-blue-100 text-blue-700' },
}

const acaoVazia = { tipo: null, produtoId: '', quantidade: '', motivo: '' }

export default function Estoque() {
  const [produtos, setProdutos] = useState([])
  const [movimentacoes, setMovimentacoes] = useState([])
  const [periodo, setPeriodo] = useState({ inicio: format(startOfMonth(new Date()), 'yyyy-MM-dd'), fim: format(new Date(), 'yyyy-MM-dd') })
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [acao, setAcao] = useState(acaoVazia)

  async function carregarProdutos() {
    try {
      setProdutos(await produtoService.listar())
    } catch {
      setErro('Não foi possível carregar os produtos.')
    }
  }

  async function carregarMovimentacoes() {
    setCarregando(true)
    try {
      const dados = await estoqueService.listarMovimentacoes(null, periodo.inicio, periodo.fim)
      setMovimentacoes(dados)
    } catch {
      setErro('Não foi possível carregar as movimentações.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregarProdutos() }, [])
  useEffect(() => { carregarMovimentacoes() }, [periodo]) // eslint-disable-line react-hooks/exhaustive-deps

  function abrirAcao(tipo, produtoId) {
    setErro('')
    setAcao({ tipo, produtoId: String(produtoId), quantidade: '', motivo: '' })
  }

  async function handleConfirmarAcao(e) {
    e.preventDefault()
    setErro('')
    try {
      const payload = { produtoId: Number(acao.produtoId), quantidade: Number(acao.quantidade), motivo: acao.motivo }
      if (acao.tipo === 'ENTRADA') {
        await estoqueService.registrarEntrada(payload)
      } else if (acao.tipo === 'SAIDA') {
        await estoqueService.registrarSaida(payload)
      } else if (acao.tipo === 'AJUSTE') {
        await estoqueService.registrarAjuste({ produtoId: Number(acao.produtoId), novoEstoque: Number(acao.quantidade), motivo: acao.motivo })
      }
      setAcao(acaoVazia)
      carregarProdutos()
      carregarMovimentacoes()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível registrar a movimentação.')
    }
  }

  const produtoDaAcao = produtos.find((p) => String(p.id) === acao.produtoId)

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Boxes size={22} />
        Estoque
      </h2>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {/* Produtos e acoes rapidas */}
      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <h3 className="font-semibold text-araca-azul mb-4">Produtos</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-left">
              <tr>
                <th className="px-4 py-3">Produto</th>
                <th className="px-4 py-3">Categoria</th>
                <th className="px-4 py-3">Estoque atual</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {produtos.length === 0 && (
                <tr><td className="px-4 py-4 text-gray-400" colSpan={4}>Nenhum produto cadastrado.</td></tr>
              )}
              {produtos.map((p) => {
                const semEstoque = p.estoque <= 0
                const estoqueBaixo = p.estoque > 0 && p.estoque <= 5
                return (
                  <tr key={p.id} className="border-t">
                    <td className="px-4 py-3 font-medium text-araca-azul">{p.nome}</td>
                    <td className="px-4 py-3">{p.categoria}</td>
                    <td className={`px-4 py-3 font-semibold ${semEstoque ? 'text-red-600' : estoqueBaixo ? 'text-amber-600' : 'text-araca-azul'}`}>
                      {p.estoque}
                    </td>
                    <td className="px-4 py-3 text-right space-x-3 whitespace-nowrap">
                      <button onClick={() => abrirAcao('ENTRADA', p.id)} className="inline-flex items-center gap-1 text-green-700 hover:underline">
                        <ArrowUpCircle size={14} /> Entrada
                      </button>
                      <button onClick={() => abrirAcao('SAIDA', p.id)} className="inline-flex items-center gap-1 text-red-600 hover:underline">
                        <ArrowDownCircle size={14} /> Saída
                      </button>
                      <button onClick={() => abrirAcao('AJUSTE', p.id)} className="inline-flex items-center gap-1 text-araca-verde-escuro hover:underline">
                        <SlidersHorizontal size={14} /> Ajustar
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Historico de movimentacoes */}
      <div className="bg-white rounded-xl shadow p-6">
        <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
          <h3 className="font-semibold text-araca-azul">Histórico de movimentações</h3>
          <div className="flex gap-2">
            <input
              type="date"
              className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={periodo.inicio}
              onChange={(e) => setPeriodo({ ...periodo, inicio: e.target.value })}
            />
            <input
              type="date"
              className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={periodo.fim}
              onChange={(e) => setPeriodo({ ...periodo, fim: e.target.value })}
            />
          </div>
        </div>

        {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}
        {!carregando && movimentacoes.length === 0 && <p className="text-gray-400 text-sm">Nenhuma movimentação nesse período.</p>}

        <ul className="text-sm divide-y">
          {movimentacoes.map((m) => {
            const info = TIPO_INFO[m.tipo] || { label: m.tipo, cor: 'bg-gray-100 text-gray-500' }
            return (
              <li key={m.id} className="py-2.5 flex items-center justify-between gap-3">
                <div>
                  <p className="font-medium text-araca-azul">{m.produtoNome}</p>
                  {m.motivo && <p className="text-xs text-gray-500">{m.motivo}</p>}
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <span className="text-xs text-gray-400">{format(new Date(m.criadoEm), 'dd/MM HH:mm')}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${info.cor}`}>
                    {info.label} · {m.quantidade}
                  </span>
                </div>
              </li>
            )
          })}
        </ul>
      </div>

      {/* Modal de acao (entrada/saida/ajuste) */}
      {acao.tipo && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <form onSubmit={handleConfirmarAcao} className="bg-white rounded-xl shadow-lg p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">
                {acao.tipo === 'ENTRADA' && 'Registrar entrada'}
                {acao.tipo === 'SAIDA' && 'Registrar saída'}
                {acao.tipo === 'AJUSTE' && 'Ajustar estoque'}
                {produtoDaAcao ? ` — ${produtoDaAcao.nome}` : ''}
              </h3>
              <button type="button" onClick={() => setAcao(acaoVazia)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>

            {acao.tipo !== 'AJUSTE' && produtoDaAcao && (
              <p className="text-sm text-gray-500 mb-3">Estoque atual: <strong className="text-araca-azul">{produtoDaAcao.estoque}</strong></p>
            )}

            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  {acao.tipo === 'AJUSTE' ? 'Novo valor do estoque' : 'Quantidade'}
                </label>
                <input
                  type="number"
                  min={acao.tipo === 'AJUSTE' ? 0 : 1}
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={acao.quantidade}
                  onChange={(e) => setAcao({ ...acao, quantidade: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Motivo (opcional)</label>
                <input
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  placeholder={acao.tipo === 'ENTRADA' ? 'Ex: compra do fornecedor X' : acao.tipo === 'SAIDA' ? 'Ex: produto vencido, quebra' : 'Ex: contagem de inventário'}
                  value={acao.motivo}
                  onChange={(e) => setAcao({ ...acao, motivo: e.target.value })}
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full mt-5 bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition"
            >
              Confirmar
            </button>
          </form>
        </div>
      )}
    </div>
  )
}
