import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, Plus, Minus, RefreshCw, Search, ArrowLeft, X, Send } from 'lucide-react'
import { garcomService } from '../services/garcomService.js'
import { authService } from '../services/authService.js'

const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const msg = (err, padrao) => err.response?.data?.mensagem || padrao
const norm = (s) => (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

function usuarioAtual() {
  try { return JSON.parse(localStorage.getItem('araca_beach_usuario')) } catch { return null }
}

/** Tela do garcom para celular: abre comandas e lanca pedidos. Pagamento e fechamento sao do caixa. */
export default function Garcom() {
  const navigate = useNavigate()
  const usuario = usuarioAtual()
  const [comandas, setComandas] = useState([])
  const [cardapio, setCardapio] = useState([])
  const [selecionadaId, setSelecionadaId] = useState(null)
  const [novaAberta, setNovaAberta] = useState(false)
  const [soMinhas, setSoMinhas] = useState(false)
  const [erro, setErro] = useState('')
  const [ok, setOk] = useState('')
  const [carregando, setCarregando] = useState(true)

  const carregar = useCallback(async () => {
    try {
      const [abertas, menu] = await Promise.all([garcomService.comandas(), garcomService.cardapio()])
      setComandas(abertas)
      setCardapio(menu)
      setErro('')
    } catch (err) {
      setErro(msg(err, 'Não foi possível carregar. Verifique a conexão.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => { carregar() }, [carregar])
  useEffect(() => {
    const t = setInterval(() => { garcomService.comandas().then(setComandas).catch(() => {}) }, 15000)
    return () => clearInterval(t)
  }, [])
  useEffect(() => {
    if (!ok) return undefined
    const t = setTimeout(() => setOk(''), 3000)
    return () => clearTimeout(t)
  }, [ok])

  const selecionada = useMemo(() => comandas.find((c) => c.id === selecionadaId) || null, [comandas, selecionadaId])
  const lista = soMinhas ? comandas.filter((c) => c.abertaPor === usuario?.nome) : comandas

  function sair() {
    authService.logout()
    navigate('/login')
  }

  function atualizarComanda(nova) {
    setComandas((atuais) => (atuais.some((c) => c.id === nova.id) ? atuais.map((c) => (c.id === nova.id ? nova : c)) : [...atuais, nova]))
  }

  return (
    <div className="min-h-screen bg-araca-areia pb-28">
      <header className="bg-araca-azul text-white px-4 py-3 flex items-center justify-between sticky top-0 z-20">
        <div className="flex items-center gap-2 min-w-0">
          {selecionada && (
            <button onClick={() => setSelecionadaId(null)} aria-label="Voltar" className="p-1 -ml-1"><ArrowLeft size={22} /></button>
          )}
          <div className="min-w-0">
            <div className="font-semibold truncate">{selecionada ? selecionada.rotulo : 'Araça Beach · Garçom'}</div>
            <div className="text-xs text-white/70 truncate">{usuario?.nome}</div>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={carregar} aria-label="Atualizar" className="p-2"><RefreshCw size={20} /></button>
          <button onClick={sair} aria-label="Sair" className="p-2"><LogOut size={20} /></button>
        </div>
      </header>

      {erro && <div className="m-3 p-3 rounded-lg bg-red-50 text-red-700 text-sm">{erro}</div>}
      {ok && <div className="m-3 p-3 rounded-lg bg-green-50 text-green-700 text-sm">{ok}</div>}

      {!selecionada && (
        <main className="p-3 space-y-3">
          <div className="flex gap-2 text-sm">
            <button onClick={() => setSoMinhas(false)} className={`flex-1 py-2 rounded-lg border ${!soMinhas ? 'bg-araca-azul text-white' : 'bg-white'}`}>Todas ({comandas.length})</button>
            <button onClick={() => setSoMinhas(true)} className={`flex-1 py-2 rounded-lg border ${soMinhas ? 'bg-araca-azul text-white' : 'bg-white'}`}>Minhas</button>
          </div>
          {carregando && <p className="text-center text-gray-500 text-sm">Carregando...</p>}
          {!carregando && lista.length === 0 && <p className="text-center text-gray-500 text-sm py-8">Nenhuma comanda aberta.</p>}
          {lista.map((c) => (
            <button key={c.id} onClick={() => setSelecionadaId(c.id)} className="w-full text-left bg-white rounded-xl border p-3 shadow-sm active:bg-araca-areia-escura">
              <div className="flex justify-between gap-2">
                <span className="font-semibold text-araca-azul truncate">{c.rotulo}</span>
                <strong className="text-araca-azul">{brl(c.total)}</strong>
              </div>
              <div className="text-xs text-gray-500 mt-0.5">
                {c.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado).length} item(ns){c.abertaPor ? ` · ${c.abertaPor}` : ''}
              </div>
            </button>
          ))}
        </main>
      )}

      {!selecionada && (
        <button onClick={() => setNovaAberta(true)} className="fixed bottom-5 right-5 z-20 bg-[#B7E90C] text-araca-azul font-semibold rounded-full shadow-lg px-5 py-4 flex items-center gap-2">
          <Plus size={20} /> Nova comanda
        </button>
      )}

      {selecionada && (
        <ComandaGarcom
          comanda={selecionada}
          cardapio={cardapio}
          onAtualizada={atualizarComanda}
          onEnviado={(texto) => { setOk(texto); setSelecionadaId(null) }}
          onErro={setErro}
        />
      )}

      {novaAberta && (
        <NovaComanda
          onFechar={() => setNovaAberta(false)}
          onCriada={(c) => { atualizarComanda(c); setNovaAberta(false); setSelecionadaId(c.id) }}
        />
      )}
    </div>
  )
}

function NovaComanda({ onFechar, onCriada }) {
  const [modo, setModo] = useState('MESA')
  const [mesa, setMesa] = useState('')
  const [nome, setNome] = useState('')
  const [busca, setBusca] = useState('')
  const [achados, setAchados] = useState([])
  const [cliente, setCliente] = useState(null)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (modo !== 'CLIENTE' || busca.trim().length < 2) { setAchados([]); return undefined }
    const t = setTimeout(() => { garcomService.clientes(busca.trim()).then(setAchados).catch(() => setAchados([])) }, 300)
    return () => clearTimeout(t)
  }, [busca, modo])

  async function abrir() {
    setErro('')
    if (modo === 'MESA' && !mesa.trim() && !nome.trim()) { setErro('Informe a mesa ou um nome.'); return }
    if (modo === 'CLIENTE' && !cliente) { setErro('Escolha o cliente.'); return }
    setSalvando(true)
    try {
      const dados = modo === 'CLIENTE'
        ? { clienteId: cliente.id, mesa: mesa.trim() || null }
        : { mesa: mesa.trim() || null, nome: nome.trim() || null }
      onCriada(await garcomService.abrir(dados))
    } catch (err) {
      setErro(msg(err, 'Não foi possível abrir a comanda.'))
      setSalvando(false)
    }
  }

  return (
    <div className="fixed inset-0 z-30 bg-black/50 flex items-end sm:items-center justify-center">
      <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-2xl p-4 space-y-3">
        <div className="flex justify-between items-center">
          <h2 className="font-semibold text-araca-azul text-lg">Nova comanda</h2>
          <button onClick={onFechar} aria-label="Fechar"><X size={22} /></button>
        </div>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <button onClick={() => setModo('MESA')} className={`py-2 rounded-lg border ${modo === 'MESA' ? 'bg-araca-azul text-white' : ''}`}>Mesa / nome</button>
          <button onClick={() => setModo('CLIENTE')} className={`py-2 rounded-lg border ${modo === 'CLIENTE' ? 'bg-araca-azul text-white' : ''}`}>Cliente cadastrado</button>
        </div>
        <input value={mesa} onChange={(e) => setMesa(e.target.value)} placeholder="Mesa (ex.: 5)" className="border rounded-lg px-3 py-3 w-full text-base" />
        {modo === 'MESA' && (
          <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Nome (opcional, ex.: João)" className="border rounded-lg px-3 py-3 w-full text-base" />
        )}
        {modo === 'CLIENTE' && (
          <div className="space-y-2">
            {cliente ? (
              <div className="flex justify-between items-center border rounded-lg px-3 py-3 bg-araca-areia">
                <span className="font-medium">{cliente.nome}</span>
                <button onClick={() => setCliente(null)} className="text-xs underline">trocar</button>
              </div>
            ) : (
              <>
                <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar por nome ou telefone" className="border rounded-lg px-3 py-3 w-full text-base" />
                <div className="max-h-48 overflow-y-auto space-y-1">
                  {achados.map((c) => (
                    <button key={c.id} onClick={() => setCliente(c)} className="w-full text-left border rounded-lg px-3 py-2 text-sm">
                      {c.nome} <span className="text-gray-400">{c.telefone}</span>
                    </button>
                  ))}
                  {busca.trim().length >= 2 && achados.length === 0 && <p className="text-xs text-gray-500">Nenhum cliente encontrado. Use "Mesa / nome".</p>}
                </div>
              </>
            )}
          </div>
        )}
        {erro && <p className="text-sm text-red-600">{erro}</p>}
        <button onClick={abrir} disabled={salvando} className="w-full bg-araca-azul text-white font-semibold rounded-lg py-3 disabled:opacity-60">
          {salvando ? 'Abrindo...' : 'Abrir comanda'}
        </button>
      </div>
    </div>
  )
}

function ComandaGarcom({ comanda, cardapio, onAtualizada, onEnviado, onErro }) {
  const [categoriaAtiva, setCategoriaAtiva] = useState(cardapio[0]?.id ?? null)
  const [busca, setBusca] = useState('')
  const [carrinho, setCarrinho] = useState([]) // {item, quantidade, observacao}
  const [enviando, setEnviando] = useState(false)

  const itens = useMemo(() => {
    const q = norm(busca.trim())
    if (q) return cardapio.flatMap((c) => c.itens).filter((i) => norm(i.nome).includes(q) || norm(i.descricao).includes(q))
    return cardapio.find((c) => c.id === categoriaAtiva)?.itens || []
  }, [cardapio, categoriaAtiva, busca])

  const totalCarrinho = carrinho.reduce((s, l) => s + Number(l.item.preco) * l.quantidade, 0)
  const qtdCarrinho = carrinho.reduce((s, l) => s + l.quantidade, 0)

  function mudar(item, delta) {
    setCarrinho((atual) => {
      const existe = atual.find((l) => l.item.id === item.id)
      if (!existe) return delta > 0 ? [...atual, { item, quantidade: 1, observacao: '' }] : atual
      const q = existe.quantidade + delta
      return q <= 0 ? atual.filter((l) => l.item.id !== item.id) : atual.map((l) => (l.item.id === item.id ? { ...l, quantidade: Math.min(q, 99) } : l))
    })
  }

  function obs(itemId, texto) {
    setCarrinho((atual) => atual.map((l) => (l.item.id === itemId ? { ...l, observacao: texto } : l)))
  }

  async function enviar() {
    setEnviando(true)
    onErro('')
    try {
      const nova = await garcomService.lancarPedido(comanda.id, carrinho.map((l) => ({
        itemId: l.item.id, quantidade: l.quantidade, observacao: l.observacao.trim() || null,
      })))
      onAtualizada(nova)
      setCarrinho([])
      onEnviado('Pedido enviado!')
    } catch (err) {
      onErro(msg(err, 'Não foi possível enviar o pedido.'))
    } finally {
      setEnviando(false)
    }
  }

  const jaPedidos = comanda.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado)

  return (
    <main className="p-3 space-y-3">
      {jaPedidos.length > 0 && (
        <details className="bg-white rounded-xl border p-3 text-sm">
          <summary className="font-medium text-araca-azul">Já pedido ({jaPedidos.length}) · {brl(comanda.subtotal)}</summary>
          <ul className="mt-2 space-y-1">
            {jaPedidos.map((i) => (
              <li key={i.id} className="flex justify-between"><span>{i.quantidade}x {i.nome}</span><span>{brl(i.subtotal)}</span></li>
            ))}
          </ul>
        </details>
      )}

      <div className="relative">
        <Search size={16} className="absolute left-3 top-3.5 text-gray-400" />
        <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar produto" className="border rounded-lg pl-9 pr-3 py-3 w-full text-base bg-white" />
      </div>

      {!busca.trim() && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {cardapio.map((c) => (
            <button key={c.id} onClick={() => setCategoriaAtiva(c.id)}
              className={`whitespace-nowrap text-sm px-3 py-2 rounded-full border ${c.id === categoriaAtiva ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600'}`}>
              {c.nome}
            </button>
          ))}
        </div>
      )}

      <div className="space-y-2">
        {itens.map((i) => {
          const linha = carrinho.find((l) => l.item.id === i.id)
          return (
            <div key={i.id} className={`bg-white border rounded-xl p-3 ${i.pausado ? 'opacity-50' : ''}`}>
              <div className="flex justify-between gap-2 items-center">
                <div className="min-w-0">
                  <div className="font-medium text-sm">{i.nome}</div>
                  <div className="text-sm text-araca-azul font-semibold">{brl(i.preco)}</div>
                  {i.pausado && <div className="text-xs text-red-600">Indisponível (acabou)</div>}
                </div>
                {!i.pausado && (
                  <div className="flex items-center gap-2">
                    {linha && (
                      <>
                        <button onClick={() => mudar(i, -1)} className="w-9 h-9 rounded-full border flex items-center justify-center" aria-label="Menos"><Minus size={16} /></button>
                        <span className="w-6 text-center font-semibold">{linha.quantidade}</span>
                      </>
                    )}
                    <button onClick={() => mudar(i, 1)} className="w-9 h-9 rounded-full bg-araca-azul text-white flex items-center justify-center" aria-label="Mais"><Plus size={16} /></button>
                  </div>
                )}
              </div>
              {linha && (
                <input value={linha.observacao} onChange={(e) => obs(i.id, e.target.value)} maxLength={200}
                  placeholder="Observação (ex.: sem gelo)" className="border rounded-lg px-3 py-2 w-full text-sm mt-2" />
              )}
            </div>
          )
        })}
        {itens.length === 0 && <p className="text-center text-gray-400 text-sm py-6">Nenhum item encontrado.</p>}
      </div>

      {carrinho.length > 0 && (
        <div className="fixed bottom-0 inset-x-0 z-20 bg-white border-t p-3 shadow-[0_-4px_12px_rgba(0,0,0,0.08)]">
          <button onClick={enviar} disabled={enviando} className="w-full bg-[#B7E90C] text-araca-azul font-bold rounded-xl py-4 flex items-center justify-center gap-2 disabled:opacity-60">
            <Send size={18} /> {enviando ? 'Enviando...' : `Enviar pedido · ${qtdCarrinho} item(ns) · ${brl(totalCarrinho)}`}
          </button>
        </div>
      )}
    </main>
  )
}
