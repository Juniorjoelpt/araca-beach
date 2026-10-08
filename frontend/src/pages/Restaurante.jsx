import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { format, formatDistanceToNowStrict } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { UtensilsCrossed, Plus, Minus, Printer, X, Receipt, Users, Search, Ban } from 'lucide-react'
import { restauranteService } from '../services/restauranteService.js'
import { clienteService } from '../services/clienteService.js'
import { imprimirPedido, imprimirConta, imprimirCancelamento } from '../utils/ticketTermico.js'

const FORMAS = [
  { value: 'PIX', label: 'Pix' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
]
const FORMA_LABEL = Object.fromEntries(FORMAS.map((f) => [f.value, f.label]))
const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const msg = (err, padrao) => err.response?.data?.mensagem || padrao

function usuarioAtual() {
  try { return JSON.parse(localStorage.getItem('araca_beach_usuario')) } catch { return null }
}

export default function Restaurante() {
  const ehAdmin = usuarioAtual()?.perfil === 'ADMIN'
  const location = useLocation()
  const [comandas, setComandas] = useState([])
  const [cardapio, setCardapio] = useState([])
  const [selecionadaId, setSelecionadaId] = useState(null)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [carrinho, setCarrinho] = useState([]) // { itemId, nome, preco, quantidade, observacao }
  const [categoriaAtiva, setCategoriaAtiva] = useState(null)
  const [busca, setBusca] = useState('')
  const [modalNova, setModalNova] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [fechando, setFechando] = useState(false)

  const [formaPag, setFormaPag] = useState('PIX')
  const [valorPag, setValorPag] = useState('')
  const [partes, setPartes] = useState(2)

  // Vindo da tela de reservas de mesa: ja abre a comanda indicada.
  useEffect(() => {
    const id = location.state?.comandaId
    if (id) setSelecionadaId(id)
  }, [location.state])

  const selecionada = useMemo(() => comandas.find((c) => c.id === selecionadaId) || null, [comandas, selecionadaId])

  const carregar = useCallback(async () => {
    try {
      const [abertas, menu] = await Promise.all([restauranteService.comandasAbertas(), restauranteService.cardapio()])
      setComandas(abertas)
      setCardapio(menu)
      setCategoriaAtiva((atual) => (atual && menu.some((c) => c.id === atual) ? atual : menu[0]?.id ?? null))
    } catch (err) {
      setErro(msg(err, 'Não foi possível carregar o restaurante.'))
    }
  }, [])

  useEffect(() => { carregar() }, [carregar])

  // Comandas de outros caixas/celulares: atualiza a lista a cada 30s.
  useEffect(() => {
    const t = setInterval(() => {
      restauranteService.comandasAbertas().then(setComandas).catch(() => {})
    }, 30000)
    return () => clearInterval(t)
  }, [])

  // Sugere o restante da conta no campo de pagamento ao trocar de comanda/total.
  useEffect(() => {
    if (selecionada) setValorPag(Number(selecionada.restante) > 0 ? Number(selecionada.restante).toFixed(2) : '')
  }, [selecionada?.id, selecionada?.restante]) // eslint-disable-line react-hooks/exhaustive-deps

  function atualizarComanda(nova) {
    setComandas((lista) => {
      const existe = lista.some((c) => c.id === nova.id)
      if (nova.status !== 'ABERTA') return lista.filter((c) => c.id !== nova.id)
      return existe ? lista.map((c) => (c.id === nova.id ? nova : c)) : [...lista, nova]
    })
  }

  async function executar(fn, sucesso) {
    setErro('')
    setAviso('')
    try {
      const resultado = await fn()
      if (sucesso) setAviso(sucesso)
      return resultado
    } catch (err) {
      setErro(msg(err, 'Não foi possível concluir a operação.'))
      return null
    }
  }

  // ----- carrinho -----

  function adicionarAoCarrinho(item) {
    if (!selecionada) { setErro('Abra ou selecione uma comanda primeiro.'); return }
    setCarrinho((c) => {
      const existente = c.find((l) => l.itemId === item.id && !l.observacao)
      if (existente) return c.map((l) => (l === existente ? { ...l, quantidade: l.quantidade + 1 } : l))
      return [...c, { chave: `${item.id}-${Date.now()}`, itemId: item.id, nome: item.nome, preco: item.preco, quantidade: 1, observacao: '' }]
    })
  }

  const alterarQtd = (chave, delta) =>
    setCarrinho((c) => c.map((l) => (l.chave === chave ? { ...l, quantidade: l.quantidade + delta } : l)).filter((l) => l.quantidade > 0))
  const alterarObs = (chave, observacao) => setCarrinho((c) => c.map((l) => (l.chave === chave ? { ...l, observacao } : l)))
  const totalCarrinho = carrinho.reduce((s, l) => s + Number(l.preco) * l.quantidade, 0)

  async function enviarPedido() {
    if (!selecionada || carrinho.length === 0) return
    setEnviando(true)
    const nova = await executar(() => restauranteService.lancarPedido(selecionada.id,
      carrinho.map((l) => ({ itemId: l.itemId, quantidade: l.quantidade, observacao: l.observacao || null }))))
    setEnviando(false)
    if (!nova) return
    atualizarComanda(nova)
    const pedido = nova.pedidos[nova.pedidos.length - 1]
    setCarrinho([])
    setAviso(`Pedido ${pedido.numero} enviado.`)
    imprimirPedido(nova, pedido)
  }

  // ----- comanda -----

  async function cancelarItem(item) {
    const motivo = window.prompt(`Cancelar ${item.quantidade}x ${item.nome}? Informe o motivo (opcional):`)
    if (motivo === null) return
    const nova = await executar(() => restauranteService.cancelarItem(selecionada.id, item.id, motivo))
    if (nova) {
      atualizarComanda(nova)
      imprimirCancelamento(nova, { ...item, motivoCancelamento: motivo })
    }
  }

  async function trocarMesa() {
    const mesa = window.prompt('Nova mesa (deixe em branco para remover):', selecionada.mesa || '')
    if (mesa === null) return
    const nova = await executar(() => restauranteService.trocarMesa(selecionada.id, mesa))
    if (nova) atualizarComanda(nova)
  }

  async function mudarTaxa(percentual) {
    const nova = await executar(() => restauranteService.taxaServico(selecionada.id, percentual))
    if (nova) atualizarComanda(nova)
  }

  async function aplicarDesconto() {
    const valor = window.prompt('Valor do desconto em R$:')
    if (valor === null) return
    const numero = Number(String(valor).replace(',', '.'))
    if (Number.isNaN(numero) || numero < 0) { setErro('Valor de desconto inválido.'); return }
    const motivo = window.prompt('Motivo do desconto:') || ''
    const nova = await executar(() => restauranteService.desconto(selecionada.id, numero, motivo))
    if (nova) atualizarComanda(nova)
  }

  async function registrarPagamento() {
    const valor = Number(String(valorPag).replace(',', '.'))
    if (!valor || valor <= 0) { setErro('Informe o valor do pagamento.'); return }
    const nova = await executar(() => restauranteService.pagar(selecionada.id, valor, formaPag))
    if (nova) atualizarComanda(nova)
  }

  function dividir() {
    const n = Math.max(1, Number(partes) || 1)
    const restante = Number(selecionada.restante)
    setValorPag((Math.round((restante / n) * 100) / 100).toFixed(2))
  }

  async function removerPagamento(id) {
    const nova = await executar(() => restauranteService.removerPagamento(selecionada.id, id))
    if (nova) atualizarComanda(nova)
  }

  async function fechar() {
    if (fechando) return
    setFechando(true)
    const nova = await executar(() => restauranteService.fechar(selecionada.id), 'Conta fechada e lançada no caixa.')
    setFechando(false)
    if (nova) {
      atualizarComanda(nova)
      setSelecionadaId(null)
      setCarrinho([])
    }
  }

  async function cancelarComanda() {
    if (!window.confirm('Cancelar esta comanda?')) return
    const nova = await executar(() => restauranteService.cancelarComanda(selecionada.id), 'Comanda cancelada.')
    if (nova) {
      atualizarComanda(nova)
      setSelecionadaId(null)
      setCarrinho([])
    }
  }

  // ----- cardápio filtrado -----

  const itensVisiveis = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    if (termo) {
      return cardapio.flatMap((c) => c.itens).filter((i) => i.nome.toLowerCase().includes(termo) || (i.descricao || '').toLowerCase().includes(termo))
    }
    return cardapio.find((c) => c.id === categoriaAtiva)?.itens || []
  }, [cardapio, categoriaAtiva, busca])

  const itensAtivosDaComanda = selecionada ? selecionada.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado) : []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="font-title text-2xl text-araca-verde flex items-center gap-2"><UtensilsCrossed size={24} /> Restaurante</h2>
        <button onClick={() => setModalNova(true)} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg flex items-center gap-2">
          <Plus size={18} /> Nova comanda
        </button>
      </div>

      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {aviso && <p className="text-green-700 text-sm mb-3">{aviso}</p>}

      <div className="grid grid-cols-1 xl:grid-cols-[230px_minmax(0,1fr)_320px] gap-4 items-start">
        {/* comandas abertas */}
        <div className="bg-white rounded-xl shadow p-3">
          <h3 className="font-semibold text-araca-azul mb-2 text-sm">Comandas abertas ({comandas.length})</h3>
          {comandas.length === 0 && <p className="text-gray-400 text-sm">Nenhuma comanda aberta.</p>}
          <div className="space-y-2">
            {comandas.map((c) => (
              <button
                key={c.id}
                onClick={() => { setSelecionadaId(c.id); setCarrinho([]) }}
                className={`w-full text-left rounded-lg border px-3 py-2 ${c.id === selecionadaId ? 'border-araca-azul bg-araca-areia' : 'border-gray-200 hover:bg-gray-50'}`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-sm truncate">{c.rotulo}</span>
                  <span className={`text-[10px] px-1.5 py-0.5 rounded ${c.vinculo === 'MESA' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'}`}>
                    {c.vinculo === 'MESA' ? 'Reserva' : 'Cliente'}
                  </span>
                </div>
                <div className="flex justify-between text-xs text-gray-500 mt-0.5">
                  <span>{formatDistanceToNowStrict(new Date(c.abertaEm), { locale: ptBR })}</span>
                  <strong className="text-araca-azul">{brl(c.total)}</strong>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* cardápio */}
        <div className="bg-white rounded-xl shadow p-4 min-w-0">
          {!selecionada ? (
            <p className="text-gray-400 text-sm py-10 text-center">Selecione ou abra uma comanda para lançar pedidos.</p>
          ) : (
            <>
              <div className="flex flex-wrap items-center gap-2 mb-3">
                <h3 className="font-semibold text-araca-azul">{selecionada.rotulo}</h3>
                <button onClick={trocarMesa} className="text-xs underline text-gray-500">trocar mesa</button>
              </div>
              <div className="relative mb-3">
                <Search size={16} className="absolute left-3 top-2.5 text-gray-400" />
                <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar no cardápio…" className="border rounded-lg pl-9 pr-3 py-2 w-full" />
              </div>
              {!busca && (
                <div className="flex gap-2 overflow-x-auto pb-2 mb-2">
                  {cardapio.map((c) => (
                    <button key={c.id} onClick={() => setCategoriaAtiva(c.id)}
                      className={`whitespace-nowrap text-sm px-3 py-1.5 rounded-full border ${c.id === categoriaAtiva ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600'}`}>
                      {c.nome}
                    </button>
                  ))}
                </div>
              )}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {itensVisiveis.map((i) => (
                  <button key={i.id} disabled={i.pausado} onClick={() => adicionarAoCarrinho(i)}
                    className={`text-left border rounded-lg p-3 ${i.pausado ? 'opacity-50 cursor-not-allowed bg-gray-50' : 'hover:border-araca-verde-escuro hover:bg-araca-areia'}`}>
                    <div className="flex justify-between gap-2">
                      <span className="font-medium text-sm">{i.nome}</span>
                      <span className="font-semibold text-araca-azul text-sm whitespace-nowrap">{brl(i.preco)}</span>
                    </div>
                    {i.porcao && <div className="text-xs text-araca-verde-escuro">{i.porcao}</div>}
                    {i.descricao && <div className="text-xs text-gray-500 line-clamp-2 mt-0.5">{i.descricao}</div>}
                    {i.pausado && <div className="text-xs text-red-600 mt-1">Indisponível (acabou)</div>}
                  </button>
                ))}
                {itensVisiveis.length === 0 && <p className="text-gray-400 text-sm">Nenhum item encontrado.</p>}
              </div>
            </>
          )}
        </div>

        {/* pedido + conta */}
        <div className="space-y-4 min-w-0">
          {selecionada && (
            <div className="bg-white rounded-xl shadow p-4">
              <h3 className="font-semibold text-araca-azul mb-2 text-sm">Novo pedido</h3>
              {carrinho.length === 0 && <p className="text-gray-400 text-sm">Toque nos itens do cardápio para adicionar.</p>}
              <div className="space-y-2">
                {carrinho.map((l) => (
                  <div key={l.chave} className="border rounded-lg p-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-medium flex-1">{l.nome}</span>
                      <div className="flex items-center gap-1">
                        <button onClick={() => alterarQtd(l.chave, -1)} className="p-1 rounded border"><Minus size={14} /></button>
                        <span className="w-6 text-center text-sm">{l.quantidade}</span>
                        <button onClick={() => alterarQtd(l.chave, 1)} className="p-1 rounded border"><Plus size={14} /></button>
                      </div>
                    </div>
                    <input value={l.observacao} onChange={(e) => alterarObs(l.chave, e.target.value)} placeholder="Observação (ex.: sem cebola, ao ponto)"
                      maxLength={200} className="border rounded px-2 py-1 text-xs w-full mt-1" />
                  </div>
                ))}
              </div>
              {carrinho.length > 0 && (
                <button onClick={enviarPedido} disabled={enviando}
                  className="mt-3 w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg flex items-center justify-center gap-2 disabled:opacity-60">
                  <Printer size={16} /> Enviar e imprimir ({brl(totalCarrinho)})
                </button>
              )}
            </div>
          )}

          {selecionada && (
            <div className="bg-white rounded-xl shadow p-4">
              <h3 className="font-semibold text-araca-azul mb-2 text-sm">Conta</h3>
              {itensAtivosDaComanda.length === 0 && <p className="text-gray-400 text-sm mb-2">Nenhum item enviado ainda.</p>}
              <div className="space-y-1 mb-3">
                {selecionada.pedidos.map((p) => (
                  <div key={p.id}>
                    <div className="flex items-center justify-between text-[11px] text-gray-400 mt-1">
                      <span>Pedido {p.numero} · {format(new Date(p.criadoEm), 'HH:mm')}</span>
                      <button onClick={() => imprimirPedido(selecionada, p)} className="underline">reimprimir</button>
                    </div>
                    {p.itens.map((i) => (
                      <div key={i.id} className={`flex items-start justify-between gap-2 text-sm ${i.cancelado ? 'line-through text-gray-400' : ''}`}>
                        <span className="flex-1">
                          {i.quantidade}x {i.nome}
                          {i.observacao && <span className="block text-xs text-gray-500 no-underline">» {i.observacao}</span>}
                        </span>
                        <span>{brl(i.subtotal)}</span>
                        {!i.cancelado && (
                          <button onClick={() => cancelarItem(i)} title="Cancelar item" className="text-red-500 p-0.5"><X size={14} /></button>
                        )}
                      </div>
                    ))}
                  </div>
                ))}
              </div>

              <div className="border-t pt-2 text-sm space-y-1">
                <div className="flex justify-between"><span>Subtotal</span><span>{brl(selecionada.subtotal)}</span></div>
                <div className="flex justify-between items-center">
                  <label className="flex items-center gap-2">
                    <input type="checkbox" checked={Number(selecionada.taxaServicoPercentual) > 0}
                      onChange={(e) => mudarTaxa(e.target.checked ? Number(selecionada.taxaServicoPadrao) || 10 : 0)} />
                    Serviço {Number(selecionada.taxaServicoPercentual) > 0 ? `(${Number(selecionada.taxaServicoPercentual)}%)` : ''}
                  </label>
                  <span>{brl(selecionada.taxaServico)}</span>
                </div>
                {Number(selecionada.desconto) > 0 && (
                  <div className="flex justify-between text-green-700"><span>Desconto{selecionada.descontoMotivo ? ` (${selecionada.descontoMotivo})` : ''}</span><span>-{brl(selecionada.desconto)}</span></div>
                )}
                {ehAdmin && <button onClick={aplicarDesconto} className="text-xs underline text-gray-500">aplicar desconto</button>}
                <div className="flex justify-between font-bold text-base text-araca-azul"><span>Total</span><span>{brl(selecionada.total)}</span></div>
              </div>

              <div className="border-t mt-3 pt-3">
                <h4 className="text-xs font-semibold text-gray-600 mb-1 flex items-center gap-1"><Users size={13} /> Pagamentos</h4>
                {selecionada.pagamentos.map((p) => (
                  <div key={p.id} className="flex justify-between text-sm">
                    <span>{FORMA_LABEL[p.forma]}</span>
                    <span>{brl(p.valor)} <button onClick={() => removerPagamento(p.id)} className="text-red-500 ml-1"><X size={12} className="inline" /></button></span>
                  </div>
                ))}
                <div className="flex justify-between text-sm mt-1"><span>Restante</span><strong className={Number(selecionada.restante) > 0 ? 'text-red-600' : 'text-green-700'}>{brl(selecionada.restante)}</strong></div>

                {Number(selecionada.restante) > 0 && (
                  <div className="mt-2 space-y-2">
                    <div className="flex gap-2">
                      <input value={valorPag} onChange={(e) => setValorPag(e.target.value)} inputMode="decimal" className="border rounded px-2 py-1 w-24 text-sm" />
                      <select value={formaPag} onChange={(e) => setFormaPag(e.target.value)} className="border rounded px-2 py-1 text-sm flex-1">
                        {FORMAS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                      </select>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-600">
                      Dividir por
                      <input type="number" min="1" max="30" value={partes} onChange={(e) => setPartes(e.target.value)} className="border rounded px-1 py-0.5 w-12" />
                      <button onClick={dividir} className="underline">calcular</button>
                    </div>
                    <button onClick={registrarPagamento} className="w-full border border-araca-azul text-araca-azul rounded-lg py-1.5 text-sm font-medium">Registrar pagamento</button>
                  </div>
                )}
              </div>

              <div className="mt-3 flex flex-col gap-2">
                <button onClick={() => imprimirConta(selecionada)} className="border rounded-lg py-1.5 text-sm flex items-center justify-center gap-2"><Receipt size={15} /> Imprimir conta</button>
                <button onClick={fechar} disabled={fechando || itensAtivosDaComanda.length === 0 || Number(selecionada.restante) !== 0}
                  className="bg-araca-azul text-white rounded-lg py-2 text-sm font-semibold disabled:opacity-40">Fechar conta</button>
                {itensAtivosDaComanda.length === 0 && (
                  <button onClick={cancelarComanda} className="text-red-600 text-xs flex items-center justify-center gap-1"><Ban size={13} /> Cancelar comanda</button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {modalNova && (
        <NovaComandaModal
          onFechar={() => setModalNova(false)}
          onCriada={(c) => { atualizarComanda(c); setSelecionadaId(c.id); setCarrinho([]); setModalNova(false) }}
        />
      )}
    </div>
  )
}

function NovaComandaModal({ onFechar, onCriada }) {
  const [modo, setModo] = useState('CLIENTE') // CLIENTE | RESERVA
  const [clientes, setClientes] = useState([])
  const [reservas, setReservas] = useState([])
  const [busca, setBusca] = useState('')
  const [clienteId, setClienteId] = useState('')
  const [reservaId, setReservaId] = useState('')
  const [mesa, setMesa] = useState('')
  const [novo, setNovo] = useState({ nome: '', telefone: '' })
  const [mostrarNovo, setMostrarNovo] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    clienteService.listar().then(setClientes).catch(() => setErro('Não foi possível carregar os clientes.'))
    restauranteService.reservasMesa(format(new Date(), 'yyyy-MM-dd'))
      .then((lista) => setReservas(lista.filter((r) => r.status === 'CONFIRMADA' && !r.comandaId)))
      .catch(() => {})
  }, [])

  const filtrados = useMemo(() => {
    const t = busca.trim().toLowerCase()
    const base = t ? clientes.filter((c) => c.nome.toLowerCase().includes(t) || (c.telefone || '').includes(t)) : clientes
    return base.slice(0, 50)
  }, [clientes, busca])

  const reservaSel = reservas.find((r) => String(r.id) === String(reservaId))

  async function cadastrarCliente() {
    if (!novo.nome.trim() || !novo.telefone.trim()) { setErro('Informe nome e telefone do cliente.'); return }
    try {
      const criado = await clienteService.criar({ nome: novo.nome.trim(), telefone: novo.telefone.trim() })
      setClientes((l) => [criado, ...l])
      setClienteId(String(criado.id))
      setMostrarNovo(false)
      setNovo({ nome: '', telefone: '' })
      setErro('')
    } catch (err) {
      setErro(msg(err, 'Não foi possível cadastrar o cliente.'))
    }
  }

  async function abrir() {
    setErro('')
    if (modo === 'RESERVA' && !mesa.trim() && !reservaSel?.mesa) {
      setErro('Informe a mesa para atender esta reserva.')
      return
    }
    try {
      const dados = modo === 'RESERVA'
        ? { reservaMesaId: Number(reservaId), mesa: mesa.trim() || null }
        : { clienteId: Number(clienteId), mesa: mesa.trim() || null }
      onCriada(await restauranteService.abrirComanda(dados))
    } catch (err) {
      setErro(msg(err, 'Não foi possível abrir a comanda.'))
    }
  }

  const podeAbrir = modo === 'RESERVA' ? !!reservaId : !!clienteId
  const clienteSel = clientes.find((c) => String(c.id) === String(clienteId))

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-araca-azul">Nova comanda</h3>
          <button onClick={onFechar}><X size={18} /></button>
        </div>

        <div className="flex gap-2 mb-3 text-sm">
          <button onClick={() => setModo('CLIENTE')} className={`flex-1 py-1.5 rounded-lg border ${modo === 'CLIENTE' ? 'bg-araca-azul text-white' : ''}`}>Sem reserva (cliente)</button>
          <button onClick={() => setModo('RESERVA')} className={`flex-1 py-1.5 rounded-lg border ${modo === 'RESERVA' ? 'bg-araca-azul text-white' : ''}`}>Reserva de mesa</button>
        </div>

        {modo === 'CLIENTE' ? (
          <div className="space-y-2">
            <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar cliente por nome ou telefone" className="border rounded-lg px-3 py-2 w-full text-sm" />
            <div className="border rounded-lg max-h-44 overflow-y-auto divide-y">
              {filtrados.length === 0 && <p className="text-sm text-gray-500 p-3">Nenhum cliente encontrado.</p>}
              {filtrados.map((c) => (
                <button type="button" key={c.id} onClick={() => setClienteId(String(c.id))}
                  className={`w-full text-left px-3 py-2 text-sm ${String(c.id) === String(clienteId) ? 'bg-araca-azul text-white' : 'hover:bg-gray-50'}`}>
                  {c.nome}{c.telefone ? ` · ${c.telefone}` : ''}
                </button>
              ))}
            </div>
            {clienteSel && <p className="text-xs text-gray-600">Selecionado: <b>{clienteSel.nome}</b></p>}
            {!mostrarNovo ? (
              <button onClick={() => setMostrarNovo(true)} className="text-xs underline text-gray-600">Cliente não cadastrado? Cadastrar agora</button>
            ) : (
              <div className="border rounded-lg p-2 space-y-2">
                <input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} placeholder="Nome" className="border rounded px-2 py-1 w-full text-sm" />
                <input value={novo.telefone} onChange={(e) => setNovo({ ...novo, telefone: e.target.value })} placeholder="Telefone" className="border rounded px-2 py-1 w-full text-sm" />
                <button onClick={cadastrarCliente} className="bg-araca-azul text-white rounded px-3 py-1 text-sm">Cadastrar e selecionar</button>
              </div>
            )}
            <input value={mesa} onChange={(e) => setMesa(e.target.value)} placeholder="Mesa (opcional)" className="border rounded-lg px-3 py-2 w-full text-sm" />
          </div>
        ) : (
          <div className="space-y-2">
            {reservas.length === 0 && <p className="text-sm text-gray-500">Nenhuma reserva de mesa confirmada hoje sem comanda.</p>}
            <div className="border rounded-lg max-h-44 overflow-y-auto divide-y">
              {reservas.map((r) => (
                <button type="button" key={r.id} onClick={() => setReservaId(String(r.id))}
                  className={`w-full text-left px-3 py-2 text-sm ${String(r.id) === String(reservaId) ? 'bg-araca-azul text-white' : 'hover:bg-gray-50'}`}>
                  {format(new Date(r.dataHora), 'HH:mm')} · {r.clienteNome} · {r.pessoas} pessoa(s){r.mesa ? ` · Mesa ${r.mesa}` : ''}
                </button>
              ))}
            </div>
            {reservaSel && !reservaSel.mesa && (
              <input value={mesa} onChange={(e) => setMesa(e.target.value)} placeholder="Mesa para esta reserva (obrigatório)" className="border border-araca-azul rounded-lg px-3 py-2 w-full text-sm" />
            )}
            {reservaSel?.mesa && <p className="text-xs text-gray-500">Mesa reservada: {reservaSel.mesa}</p>}
          </div>
        )}

        {erro && <p className="text-red-600 text-sm mt-2">{erro}</p>}
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onFechar} className="px-4 py-2 text-sm">Cancelar</button>
          <button onClick={abrir} disabled={!podeAbrir} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-40">Abrir comanda</button>
        </div>
      </div>
    </div>
  )
}
