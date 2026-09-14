import { useEffect, useState } from 'react'
import { clienteService } from '../services/clienteService.js'
import { produtoService } from '../services/produtoService.js'
import { comandaService } from '../services/comandaService.js'
import { ShoppingBag, Plus, Pencil, Trash2, X, Wine, UtensilsCrossed, Dumbbell, Package } from 'lucide-react'

const CATEGORIAS = [
  { value: 'BEBIDA', label: 'Bebida', icon: Wine },
  { value: 'ALIMENTO', label: 'Alimento', icon: UtensilsCrossed },
  { value: 'EQUIPAMENTO', label: 'Equipamento', icon: Dumbbell },
  { value: 'OUTRO', label: 'Outro', icon: Package },
]

function categoriaInfo(value) {
  return CATEGORIAS.find((c) => c.value === value) || CATEGORIAS[3]
}

const produtoVazio = { nome: '', categoria: 'BEBIDA', preco: '', estoque: '', ehAluguel: false }

export default function Loja() {
  const [produtos, setProdutos] = useState([])
  const [clientes, setClientes] = useState([])
  const [comandasAbertas, setComandasAbertas] = useState([])
  const [comandaAtiva, setComandaAtiva] = useState(null)
  const [erro, setErro] = useState('')

  const [modalProdutoAberto, setModalProdutoAberto] = useState(false)
  const [produtoEditando, setProdutoEditando] = useState(null) // null = novo produto
  const [formProduto, setFormProduto] = useState(produtoVazio)

  const [novoClienteId, setNovoClienteId] = useState('')
  const [itemSelecionado, setItemSelecionado] = useState({ produtoId: '', quantidade: 1 })

  async function carregarBase() {
    try {
      const [listaProdutos, listaClientes, listaComandas] = await Promise.all([
        produtoService.listar(),
        clienteService.listar(),
        comandaService.listarAbertas(),
      ])
      setProdutos(Array.isArray(listaProdutos) ? listaProdutos : [])
      setClientes(Array.isArray(listaClientes) ? listaClientes : [])
      setComandasAbertas(Array.isArray(listaComandas) ? listaComandas : [])
    } catch {
      setErro('Não foi possível carregar os dados da loja.')
    }
  }

  useEffect(() => { carregarBase() }, [])

  function abrirNovoProduto() {
    setProdutoEditando(null)
    setFormProduto(produtoVazio)
    setModalProdutoAberto(true)
  }

  function abrirEdicaoProduto(produto) {
    setProdutoEditando(produto)
    setFormProduto({
      nome: produto.nome,
      categoria: produto.categoria,
      preco: String(produto.preco),
      estoque: String(produto.estoque),
      ehAluguel: produto.ehAluguel,
    })
    setModalProdutoAberto(true)
  }

  async function handleSalvarProduto(e) {
    e.preventDefault()
    setErro('')
    const payload = {
      ...formProduto,
      preco: Number(formProduto.preco),
      estoque: formProduto.estoque ? Number(formProduto.estoque) : 0,
    }
    try {
      if (produtoEditando) {
        await produtoService.atualizar(produtoEditando.id, payload)
      } else {
        await produtoService.criar(payload)
      }
      setModalProdutoAberto(false)
      carregarBase()
    } catch {
      setErro('Não foi possível salvar o produto.')
    }
  }

  async function handleRemoverProduto(produto) {
    if (!confirm(`Remover "${produto.nome}" do catálogo?`)) return
    setErro('')
    try {
      await produtoService.remover(produto.id)
      carregarBase()
    } catch {
      setErro('Não foi possível remover o produto (pode já ter sido usado em alguma comanda).')
    }
  }

  async function handleAbrirComanda(e) {
    e.preventDefault()
    setErro('')
    try {
      const comanda = await comandaService.abrir({ clienteId: Number(novoClienteId) })
      setNovoClienteId('')
      setComandaAtiva(comanda)
      carregarBase()
    } catch {
      setErro('Não foi possível abrir a comanda.')
    }
  }

  async function abrirComandaExistente(id) {
    setErro('')
    try {
      const comanda = await comandaService.buscar(id)
      setComandaAtiva(comanda)
    } catch {
      setErro('Não foi possível abrir esta comanda.')
    }
  }

  async function handleAdicionarItem(e) {
    e.preventDefault()
    setErro('')
    try {
      const atualizada = await comandaService.adicionarItem(comandaAtiva.id, {
        produtoId: Number(itemSelecionado.produtoId),
        quantidade: Number(itemSelecionado.quantidade),
      })
      setComandaAtiva(atualizada)
      setItemSelecionado({ produtoId: '', quantidade: 1 })
      carregarBase()
    } catch {
      setErro('Não foi possível adicionar o item.')
    }
  }

  async function handleFecharComanda() {
    if (!confirm('Fechar esta comanda?')) return
    try {
      await comandaService.fechar(comandaAtiva.id)
      setComandaAtiva(null)
      carregarBase()
    } catch {
      setErro('Não foi possível fechar a comanda.')
    }
  }

  const totalComanda = (comandaAtiva?.itens || []).reduce(
    (soma, item) => soma + Number(item.precoUnitario) * item.quantidade,
    0
  )

  function totalDaComanda(comanda) {
    return (comanda.itens || []).reduce(
      (soma, item) => soma + Number(item.precoUnitario) * item.quantidade,
      0
    )
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <ShoppingBag size={22} />
        Loja
      </h2>

      {/* Catalogo de produtos, em cards */}
      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-araca-azul">Produtos</h3>
          <button
            onClick={abrirNovoProduto}
            className="flex items-center gap-2 bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-xl hover:opacity-90 hover:shadow-glow transition"
          >
            <Plus size={16} />
            Novo Produto
          </button>
        </div>

        {produtos.length === 0 && (
          <p className="text-gray-400 text-sm">Nenhum produto cadastrado ainda.</p>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {produtos.map((p) => {
            const { label, icon: Icon } = categoriaInfo(p.categoria)
            const semEstoque = p.estoque <= 0
            const estoqueBaixo = p.estoque > 0 && p.estoque <= 5
            return (
              <div key={p.id} className="border rounded-xl overflow-hidden hover:shadow-md transition">
                <div className="bg-araca-areia-escura/60 h-20 flex items-center justify-center">
                  <Icon size={32} className="text-araca-azul-claro" />
                </div>
                <div className="p-4">
                  <p className="text-xs text-gray-400 mb-0.5">Cód.: {p.id}</p>
                  <p className="font-semibold text-araca-azul truncate" title={p.nome}>{p.nome}</p>
                  <p className="text-xs text-gray-500 mb-2">{label}{p.ehAluguel ? ' · Aluguel' : ''}</p>
                  <p className="text-lg font-bold text-araca-azul">R$ {Number(p.preco).toFixed(2)}</p>
                  <p className={`text-xs mt-1 font-medium ${
                    semEstoque ? 'text-red-600' : estoqueBaixo ? 'text-amber-600' : 'text-gray-500'
                  }`}>
                    {semEstoque ? 'Sem estoque' : `Estoque: ${p.estoque}`}
                  </p>
                  <div className="flex items-center gap-3 mt-3 pt-3 border-t">
                    <button
                      onClick={() => abrirEdicaoProduto(p)}
                      className="flex items-center gap-1 text-xs text-araca-verde-escuro hover:underline"
                    >
                      <Pencil size={13} /> Editar
                    </button>
                    <button
                      onClick={() => handleRemoverProduto(p)}
                      className="flex items-center gap-1 text-xs text-red-600 hover:underline"
                    >
                      <Trash2 size={13} /> Excluir
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Comandas */}
      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-araca-azul">Comandas abertas</h3>
        </div>
        <form onSubmit={handleAbrirComanda} className="flex gap-2 mb-4">
          <select
            className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={novoClienteId}
            onChange={(e) => setNovoClienteId(e.target.value)}
            required
          >
            <option value="">Selecione o cliente...</option>
            {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
          <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
            Abrir comanda
          </button>
        </form>

        {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}

        {comandasAbertas.length === 0 && <p className="text-gray-400 text-sm">Nenhuma comanda aberta.</p>}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {comandasAbertas.map((c) => {
            const qtdItens = (c.itens || []).reduce((soma, i) => soma + i.quantidade, 0)
            return (
              <button
                key={c.id}
                onClick={() => abrirComandaExistente(c.id)}
                className="text-left border rounded-xl p-4 hover:shadow-md hover:border-araca-verde transition"
              >
                <p className="font-semibold text-araca-azul">Comanda #{c.id}</p>
                <p className="text-sm text-gray-600 truncate">{c.cliente?.nome}</p>
                <div className="flex items-center justify-between mt-3 pt-3 border-t">
                  <span className="text-xs text-gray-500">{qtdItens} {qtdItens === 1 ? 'item' : 'itens'}</span>
                  <span className="font-semibold text-araca-verde-escuro">R$ {totalDaComanda(c).toFixed(2)}</span>
                </div>
              </button>
            )
          })}
        </div>
      </div>

      {/* Modal: detalhes da comanda selecionada */}
      {comandaAtiva && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">
                Comanda #{comandaAtiva.id} — {comandaAtiva.cliente?.nome}
              </h3>
              <button onClick={() => setComandaAtiva(null)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>

            {!comandaAtiva.fechada && (
              <form onSubmit={handleAdicionarItem} className="flex gap-2 mb-4">
                <select
                  className="flex-1 min-w-0 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={itemSelecionado.produtoId}
                  onChange={(e) => setItemSelecionado({ ...itemSelecionado, produtoId: e.target.value })}
                  required
                >
                  <option value="">Selecione o produto...</option>
                  {produtos.map((p) => (
                    <option key={p.id} value={p.id} disabled={p.estoque <= 0}>
                      {p.nome} — R$ {Number(p.preco).toFixed(2)}{p.estoque <= 0 ? ' (sem estoque)' : ''}
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="1"
                  className="w-16 flex-shrink-0 border rounded-lg px-2 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={itemSelecionado.quantidade}
                  onChange={(e) => setItemSelecionado({ ...itemSelecionado, quantidade: e.target.value })}
                />
                <button type="submit" className="flex-shrink-0 bg-araca-verde text-araca-azul text-sm font-semibold px-3 py-2 rounded-lg hover:opacity-90 transition">
                  Adicionar
                </button>
              </form>
            )}

            <ul className="text-sm divide-y border rounded-lg max-h-64 overflow-y-auto">
              {(comandaAtiva.itens || []).length === 0 && (
                <li className="text-gray-400 py-2 px-3">Nenhum item ainda.</li>
              )}
              {(comandaAtiva.itens || []).map((item) => (
                <li key={item.id} className="py-2 px-3 flex justify-between text-araca-azul">
                  <span>{item.produto?.nome} × {item.quantidade}</span>
                  <span className="font-medium">R$ {(Number(item.precoUnitario) * item.quantidade).toFixed(2)}</span>
                </li>
              ))}
            </ul>


            <div className="flex items-center justify-between pt-4 mt-2">
              <span className="font-semibold text-araca-azul">Total: R$ {totalComanda.toFixed(2)}</span>
              {!comandaAtiva.fechada && (
                <button onClick={handleFecharComanda} className="text-red-600 text-sm hover:underline">
                  Fechar comanda
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal: novo produto / editar produto */}
      {modalProdutoAberto && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <form onSubmit={handleSalvarProduto} className="bg-white rounded-xl shadow-lg p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">
                {produtoEditando ? 'Editar produto' : 'Novo produto'}
              </h3>
              <button type="button" onClick={() => setModalProdutoAberto(false)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
                <input
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formProduto.nome}
                  onChange={(e) => setFormProduto({ ...formProduto, nome: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Categoria</label>
                <select
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formProduto.categoria}
                  onChange={(e) => setFormProduto({ ...formProduto, categoria: e.target.value })}
                >
                  {CATEGORIAS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
                </select>
              </div>
              <div className="flex gap-3">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Preço (R$)</label>
                  <input
                    type="number" step="0.01" min="0"
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formProduto.preco}
                    onChange={(e) => setFormProduto({ ...formProduto, preco: e.target.value })}
                    required
                  />
                </div>
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Estoque</label>
                  <input
                    type="number" min="0"
                    className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                    value={formProduto.estoque}
                    onChange={(e) => setFormProduto({ ...formProduto, estoque: e.target.value })}
                  />
                </div>
              </div>
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={formProduto.ehAluguel}
                  onChange={(e) => setFormProduto({ ...formProduto, ehAluguel: e.target.checked })}
                />
                Item de aluguel (ex: raquete)
              </label>
            </div>

            <div className="flex gap-2 pt-5">
              <button
                type="button"
                onClick={() => setModalProdutoAberto(false)}
                className="flex-1 border rounded-lg py-2 text-gray-600 hover:bg-gray-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="flex-1 bg-araca-verde text-araca-azul font-semibold rounded-lg py-2 hover:opacity-90"
              >
                {produtoEditando ? 'Salvar' : 'Adicionar'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
