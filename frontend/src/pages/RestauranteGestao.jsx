import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { format, startOfMonth, endOfMonth } from 'date-fns'
import { ChefHat, Plus, X, Pause, Play, Pencil, FlaskConical, Search } from 'lucide-react'
import { restauranteService } from '../services/restauranteService.js'

const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const num = (v, casas = 3) => Number(v || 0).toLocaleString('pt-BR', { maximumFractionDigits: casas })
const msg = (err, padrao) => err.response?.data?.mensagem || padrao
// Busca sem acento/maiusculas; varias palavras precisam aparecer todas.
const norm = (t) => String(t || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const casa = (texto, busca) => norm(busca).split(/\s+/).filter(Boolean).every((t) => norm(texto).includes(t))

function CampoBusca({ valor, onChange, placeholder, resultados }) {
  return (
    <div className="mb-4">
      <div className="relative max-w-md">
        <Search size={16} className="absolute left-3 top-2.5 text-gray-400" />
        <input value={valor} onChange={(e) => onChange(e.target.value)} onKeyDown={(e) => { if (e.key === 'Escape') onChange('') }}
          placeholder={placeholder} className="border rounded-lg pl-9 pr-9 py-2 w-full text-sm bg-white" />
        {valor && <button onClick={() => onChange('')} title="Limpar busca" className="absolute right-2 top-2 text-gray-400 hover:text-gray-600"><X size={18} /></button>}
      </div>
      {valor.trim() && <p className="text-xs text-gray-500 mt-1">{resultados} resultado(s)</p>}
    </div>
  )
}

const DIAS = ['', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

// Insumos em KG/L sao digitados em g/ml na ficha tecnica e nas movimentacoes.
const FATOR = { UN: 1, KG: 1000, L: 1000, FARDO: 1, PACOTE: 1 }
const MENOR = { UN: 'un', KG: 'g', L: 'ml', FARDO: 'fardo', PACOTE: 'pacote' }
const BASE = { UN: 'un', KG: 'kg', L: 'L', FARDO: 'fardo', PACOTE: 'pacote' }

export default function RestauranteGestao() {
  const [aba, setAba] = useState('cardapio')
  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-4 flex items-center gap-2"><ChefHat size={24} /> Gestão do restaurante</h2>
      <div className="flex gap-2 mb-5">
        {[['cardapio', 'Cardápio e fichas'], ['insumos', 'Insumos (estoque)'], ['relatorio', 'Relatório']].map(([k, l]) => (
          <button key={k} onClick={() => setAba(k)} className={`px-4 py-2 rounded-lg text-sm border ${aba === k ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600'}`}>{l}</button>
        ))}
      </div>
      {aba === 'cardapio' && <AbaCardapio />}
      {aba === 'insumos' && <AbaInsumos />}
      {aba === 'relatorio' && <AbaRelatorio />}
    </div>
  )
}

/* ============================ CARDÁPIO ============================ */

function AbaCardapio() {
  const [itens, setItens] = useState([])
  const [categorias, setCategorias] = useState([])
  const [insumos, setInsumos] = useState([])
  const [erro, setErro] = useState('')
  const [editando, setEditando] = useState(null) // item | {} (novo)
  const [fichaDe, setFichaDe] = useState(null)
  const [busca, setBusca] = useState('')
  const location = useLocation()
  const abriuPorCodigo = useRef(false)

  const carregar = useCallback(async () => {
    try {
      const [i, c, ins] = await Promise.all([restauranteService.itens(), restauranteService.categorias(), restauranteService.insumos()])
      setItens(i); setCategorias(c); setInsumos(ins); setErro('')
    } catch (err) { setErro(msg(err, 'Não foi possível carregar o cardápio.')) }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  // Vindo do caixa (codigo lido que nao existe): abre o cadastro ja com o codigo preenchido.
  useEffect(() => {
    const codigo = location.state?.codigoBarras
    if (codigo && categorias.length > 0 && !abriuPorCodigo.current) {
      abriuPorCodigo.current = true
      setEditando({ codigoBarras: codigo })
    }
  }, [location.state, categorias])

  async function pausar(item) {
    try { await restauranteService.pausarItem(item.id, !item.pausado); carregar() } catch (err) { setErro(msg(err, 'Falha ao alterar o item.')) }
  }

  async function novaCategoria() {
    const nome = window.prompt('Nome da nova categoria:')
    if (!nome) return
    try { await restauranteService.criarCategoria({ nome, ordem: categorias.length + 1, ativa: true }); carregar() } catch (err) { setErro(msg(err, 'Falha ao criar a categoria.')) }
  }

  const porCategoria = useMemo(() => {
    const mapa = new Map(categorias.map((c) => [c.id, { ...c, itens: [] }]))
    itens.forEach((i) => mapa.get(i.categoriaId)?.itens.push(i))
    return [...mapa.values()]
  }, [itens, categorias])

  // Com busca: so itens que casam (nome, descricao, porcao, codigo de barras ou categoria); categorias vazias somem.
  const categoriasVisiveis = useMemo(() => {
    if (!busca.trim()) return porCategoria
    return porCategoria
      .map((c) => ({ ...c, itens: c.itens.filter((i) => casa(`${i.nome} ${i.descricao || ''} ${i.porcao || ''} ${i.codigoBarras || ''} ${c.nome}`, busca)) }))
      .filter((c) => c.itens.length > 0)
  }, [porCategoria, busca])
  const totalEncontrados = categoriasVisiveis.reduce((n, c) => n + c.itens.length, 0)

  return (
    <div>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      <div className="flex gap-3 mb-4">
        <button onClick={() => setEditando({})} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg flex items-center gap-2"><Plus size={16} /> Novo item</button>
        <button onClick={novaCategoria} className="bg-white text-araca-azul border px-4 py-2 rounded-lg text-sm font-medium hover:bg-araca-areia">Nova categoria</button>
      </div>

      <CampoBusca valor={busca} onChange={setBusca} placeholder="Buscar produto por nome, categoria ou código de barras…" resultados={totalEncontrados} />
      {busca.trim() && totalEncontrados === 0 && <p className="text-gray-400 text-sm mb-4">Nenhum produto encontrado.</p>}

      {categoriasVisiveis.map((c) => (
        <div key={c.id} className="bg-white rounded-xl shadow mb-4 overflow-x-auto">
          <div className="px-4 py-2 border-b font-semibold text-araca-azul flex items-center justify-between">
            <span>{c.nome}{!c.ativa && <span className="ml-2 text-xs text-gray-400">(oculta)</span>}</span>
            <button className="text-xs underline text-gray-500" onClick={async () => {
              try { await restauranteService.atualizarCategoria(c.id, { nome: c.nome, ordem: c.ordem, ativa: !c.ativa }); carregar() } catch (err) { setErro(msg(err, 'Falha ao alterar a categoria.')) }
            }}>{c.ativa ? 'ocultar' : 'mostrar'}</button>
          </div>
          <table className="w-full text-sm">
            <tbody>
              {c.itens.map((i) => (
                <tr key={i.id} className={`border-t ${!i.ativo ? 'opacity-50' : ''}`}>
                  <td className="px-4 py-2">
                    <div className="font-medium">{i.nome}{i.pausado && <span className="ml-2 text-xs text-red-600">pausado</span>}{!i.ativo && <span className="ml-2 text-xs text-gray-500">inativo</span>}</div>
                    {i.porcao && <div className="text-xs text-araca-verde-escuro">{i.porcao}</div>}
                    {i.codigoBarras && <div className="text-xs text-gray-400">cód. {i.codigoBarras}</div>}
                  </td>
                  <td className="px-2 py-2 text-xs text-gray-500">{i.praca === 'BAR' ? 'Bar' : 'Cozinha'}</td>
                  <td className="px-2 py-2 text-right whitespace-nowrap">{brl(i.preco)}</td>
                  <td className="px-2 py-2 text-right text-xs whitespace-nowrap">
                    {i.temFicha ? <span title={`Custo ${brl(i.custo)}`}>custo {brl(i.custo)} · <strong className={Number(i.margemPercentual) < 40 ? 'text-red-600' : 'text-green-700'}>{num(i.margemPercentual, 1)}%</strong></span> : <span className="text-gray-400">sem ficha</span>}
                  </td>
                  <td className="px-3 py-2 text-right whitespace-nowrap space-x-2">
                    <button title={i.pausado ? 'Voltar ao cardápio' : 'Pausar (acabou)'} onClick={() => pausar(i)}>{i.pausado ? <Play size={16} className="inline text-green-700" /> : <Pause size={16} className="inline text-amber-600" />}</button>
                    <button title="Ficha técnica" onClick={() => setFichaDe(i)}><FlaskConical size={16} className="inline text-araca-azul" /></button>
                    <button title="Editar" onClick={() => setEditando(i)}><Pencil size={16} className="inline text-gray-600" /></button>
                  </td>
                </tr>
              ))}
              {c.itens.length === 0 && <tr><td className="px-4 py-3 text-gray-400">Sem itens.</td></tr>}
            </tbody>
          </table>
        </div>
      ))}

      {editando && <ItemModal item={editando} categorias={categorias} onFechar={() => setEditando(null)} onSalvo={() => { setEditando(null); carregar() }} />}
      {fichaDe && <FichaModal item={fichaDe} insumos={insumos} onFechar={() => setFichaDe(null)} onSalvo={() => { setFichaDe(null); carregar() }} />}
    </div>
  )
}

function Modal({ titulo, onFechar, children, largo }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className={`bg-white rounded-xl shadow-xl w-full ${largo ? 'max-w-2xl' : 'max-w-md'} p-5 max-h-[90vh] overflow-y-auto`}>
        <div className="flex items-center justify-between mb-3"><h3 className="font-semibold text-araca-azul">{titulo}</h3><button onClick={onFechar}><X size={18} /></button></div>
        {children}
      </div>
    </div>
  )
}

function ItemModal({ item, categorias, onFechar, onSalvo }) {
  const novo = !item.id
  const [f, setF] = useState({
    categoriaId: item.categoriaId || categorias[0]?.id || '',
    nome: item.nome || '', descricao: item.descricao || '', porcao: item.porcao || '',
    preco: item.preco ?? '', praca: item.praca || 'COZINHA', ativo: item.ativo ?? true,
    tempoPreparoMin: item.tempoPreparoMin ?? '', ordem: item.ordem ?? 0,
    codigoBarras: item.codigoBarras || '',
  })
  const [erro, setErro] = useState('')
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }))

  async function salvar() {
    setErro('')
    const dados = {
      ...f, categoriaId: Number(f.categoriaId), preco: Number(String(f.preco).replace(',', '.')),
      tempoPreparoMin: f.tempoPreparoMin === '' ? null : Number(f.tempoPreparoMin), ordem: Number(f.ordem) || 0,
      pausado: item.pausado ?? false,
    }
    try {
      if (novo) await restauranteService.criarItem(dados); else await restauranteService.atualizarItem(item.id, dados)
      onSalvo()
    } catch (err) { setErro(msg(err, 'Não foi possível salvar o item.')) }
  }

  return (
    <Modal titulo={novo ? 'Novo item' : 'Editar item'} onFechar={onFechar}>
      <div className="space-y-2 text-sm">
        <select value={f.categoriaId} onChange={(e) => set('categoriaId', e.target.value)} className="border rounded-lg px-3 py-2 w-full">
          {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
        <input value={f.nome} onChange={(e) => set('nome', e.target.value)} placeholder="Nome" className="border rounded-lg px-3 py-2 w-full" />
        <input value={f.codigoBarras} onChange={(e) => set('codigoBarras', e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') e.preventDefault() }}
          placeholder="Código de barras (clique aqui e leia com o leitor)" inputMode="numeric" className="border rounded-lg px-3 py-2 w-full" />
        <textarea value={f.descricao} onChange={(e) => set('descricao', e.target.value)} placeholder="Descrição / ingredientes" rows={2} className="border rounded-lg px-3 py-2 w-full" />
        <div className="grid grid-cols-2 gap-2">
          <input value={f.preco} onChange={(e) => set('preco', e.target.value)} placeholder="Preço (R$)" inputMode="decimal" className="border rounded-lg px-3 py-2" />
          <input value={f.porcao} onChange={(e) => set('porcao', e.target.value)} placeholder="Porção (ex.: Serve 2)" className="border rounded-lg px-3 py-2" />
          <select value={f.praca} onChange={(e) => set('praca', e.target.value)} className="border rounded-lg px-3 py-2"><option value="COZINHA">Cozinha</option><option value="BAR">Bar</option></select>
          <input value={f.tempoPreparoMin} onChange={(e) => set('tempoPreparoMin', e.target.value)} placeholder="Preparo (min)" inputMode="numeric" className="border rounded-lg px-3 py-2" />
          <input value={f.ordem} onChange={(e) => set('ordem', e.target.value)} placeholder="Ordem" inputMode="numeric" className="border rounded-lg px-3 py-2" />
          <label className="flex items-center gap-2"><input type="checkbox" checked={f.ativo} onChange={(e) => set('ativo', e.target.checked)} /> Ativo no cardápio</label>
        </div>
        {erro && <p className="text-red-600">{erro}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button onClick={onFechar} className="px-4 py-2">Cancelar</button>
          <button onClick={salvar} disabled={!f.nome || f.preco === '' || !f.categoriaId} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-40">Salvar</button>
        </div>
      </div>
    </Modal>
  )
}

function FichaModal({ item, insumos, onFechar, onSalvo }) {
  // linhas: { insumoId, valor } onde valor esta na unidade menor (g/ml/un)
  const [linhas, setLinhas] = useState(item.ficha.map((l) => ({ insumoId: String(l.insumoId), valor: String(Math.round(Number(l.quantidade) * FATOR[l.unidade] * 10000) / 10000) })))
  const [erro, setErro] = useState('')
  const porId = useMemo(() => Object.fromEntries(insumos.map((i) => [String(i.id), i])), [insumos])

  const custo = linhas.reduce((s, l) => {
    const ins = porId[l.insumoId]
    return ins ? s + (Number(String(l.valor).replace(',', '.')) / FATOR[ins.unidade]) * Number(ins.custoUnitario) : s
  }, 0)
  const preco = Number(item.preco)

  async function salvar() {
    setErro('')
    try {
      const payload = linhas.filter((l) => l.insumoId && l.valor !== '').map((l) => ({
        insumoId: Number(l.insumoId),
        quantidade: Number(String(l.valor).replace(',', '.')) / FATOR[porId[l.insumoId].unidade],
      }))
      await restauranteService.definirFicha(item.id, payload)
      onSalvo()
    } catch (err) { setErro(msg(err, 'Não foi possível salvar a ficha.')) }
  }

  return (
    <Modal titulo={`Ficha técnica — ${item.nome}`} onFechar={onFechar} largo>
      <p className="text-xs text-gray-500 mb-3">Quanto de cada insumo vai em 1 unidade vendida. A venda dá baixa automática no estoque e alimenta o custo e a margem do prato.</p>
      <div className="space-y-2">
        {linhas.map((l, idx) => {
          const ins = porId[l.insumoId]
          return (
            <div key={idx} className="flex gap-2 items-center">
              <select value={l.insumoId} onChange={(e) => setLinhas((x) => x.map((y, i) => (i === idx ? { ...y, insumoId: e.target.value } : y)))} className="border rounded-lg px-2 py-1.5 flex-1 text-sm">
                <option value="">Escolha o insumo</option>
                {insumos.filter((i) => i.ativo).map((i) => <option key={i.id} value={i.id}>{i.nome} ({BASE[i.unidade]})</option>)}
              </select>
              <input value={l.valor} onChange={(e) => setLinhas((x) => x.map((y, i) => (i === idx ? { ...y, valor: e.target.value } : y)))} inputMode="decimal" className="border rounded-lg px-2 py-1.5 w-24 text-sm" placeholder="qtd" />
              <span className="text-xs text-gray-500 w-8">{ins ? MENOR[ins.unidade] : ''}</span>
              <button onClick={() => setLinhas((x) => x.filter((_, i) => i !== idx))} className="text-red-500"><X size={16} /></button>
            </div>
          )
        })}
      </div>
      <button onClick={() => setLinhas((x) => [...x, { insumoId: '', valor: '' }])} className="text-sm underline text-araca-azul mt-2">+ adicionar insumo</button>
      {insumos.length === 0 && <p className="text-xs text-amber-700 mt-2">Cadastre os insumos na aba "Insumos (estoque)" primeiro.</p>}
      <div className="mt-4 text-sm flex justify-between border-t pt-3">
        <span>Custo: <strong>{brl(custo)}</strong></span>
        <span>Preço: {brl(preco)} · Margem: <strong>{preco ? num(((preco - custo) / preco) * 100, 1) : 0}%</strong></span>
      </div>
      {erro && <p className="text-red-600 text-sm mt-2">{erro}</p>}
      <div className="flex justify-end gap-2 mt-3">
        <button onClick={onFechar} className="px-4 py-2 text-sm">Cancelar</button>
        <button onClick={salvar} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg">Salvar ficha</button>
      </div>
    </Modal>
  )
}

/* ============================ INSUMOS ============================ */

function AbaInsumos() {
  const [insumos, setInsumos] = useState([])
  const [erro, setErro] = useState('')
  const [editando, setEditando] = useState(null)
  const [movendo, setMovendo] = useState(null)
  const [busca, setBusca] = useState('')

  const carregar = useCallback(async () => {
    try { setInsumos(await restauranteService.insumos()); setErro('') } catch (err) { setErro(msg(err, 'Não foi possível carregar os insumos.')) }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  const visiveis = useMemo(() => (busca.trim() ? insumos.filter((i) => casa(i.nome, busca)) : insumos), [insumos, busca])

  return (
    <div>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      <button onClick={() => setEditando({})} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg flex items-center gap-2 mb-4"><Plus size={16} /> Novo insumo</button>
      <CampoBusca valor={busca} onChange={setBusca} placeholder="Buscar insumo por nome…" resultados={visiveis.length} />
      <div className="bg-white rounded-xl shadow overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-600"><tr><th className="px-3 py-2">Insumo</th><th className="px-3 py-2 text-right">Estoque</th><th className="px-3 py-2 text-right">Mínimo</th><th className="px-3 py-2 text-right">Custo</th><th className="px-3 py-2" /></tr></thead>
          <tbody>
            {visiveis.map((i) => (
              <tr key={i.id} className={`border-t ${!i.ativo ? 'opacity-50' : ''}`}>
                <td className="px-3 py-2">{i.nome}{i.abaixoDoMinimo && <span className="ml-2 text-xs bg-red-100 text-red-700 px-1.5 py-0.5 rounded">estoque baixo</span>}</td>
                <td className={`px-3 py-2 text-right ${Number(i.estoqueAtual) < 0 ? 'text-red-600 font-semibold' : ''}`}>{num(i.estoqueAtual)} {BASE[i.unidade]}</td>
                <td className="px-3 py-2 text-right">{num(i.estoqueMinimo)} {BASE[i.unidade]}</td>
                <td className="px-3 py-2 text-right">{brl(i.custoUnitario)}/{BASE[i.unidade]}</td>
                <td className="px-3 py-2 text-right space-x-3 whitespace-nowrap">
                  <button onClick={() => setMovendo(i)} className="underline text-araca-azul">Movimentar</button>
                  <button onClick={() => setEditando(i)} className="underline text-gray-600">Editar</button>
                </td>
              </tr>
            ))}
            {visiveis.length === 0 && <tr><td colSpan={5} className="px-3 py-6 text-center text-gray-400">{insumos.length === 0 ? 'Nenhum insumo cadastrado.' : 'Nenhum insumo encontrado.'}</td></tr>}
          </tbody>
        </table>
      </div>
      {editando && <InsumoModal insumo={editando} onFechar={() => setEditando(null)} onSalvo={() => { setEditando(null); carregar() }} />}
      {movendo && <MovimentoModal insumo={movendo} onFechar={() => setMovendo(null)} onSalvo={() => { setMovendo(null); carregar() }} />}
    </div>
  )
}

function InsumoModal({ insumo, onFechar, onSalvo }) {
  const novo = !insumo.id
  const [f, setF] = useState({ nome: insumo.nome || '', unidade: insumo.unidade || 'KG', estoqueMinimo: insumo.estoqueMinimo ?? 0, custoUnitario: insumo.custoUnitario ?? 0, ativo: insumo.ativo ?? true })
  const [erro, setErro] = useState('')
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }))
  async function salvar() {
    setErro('')
    const dados = { ...f, estoqueMinimo: Number(String(f.estoqueMinimo).replace(',', '.')) || 0, custoUnitario: Number(String(f.custoUnitario).replace(',', '.')) || 0 }
    try { novo ? await restauranteService.criarInsumo(dados) : await restauranteService.atualizarInsumo(insumo.id, dados); onSalvo() } catch (err) { setErro(msg(err, 'Não foi possível salvar o insumo.')) }
  }
  return (
    <Modal titulo={novo ? 'Novo insumo' : 'Editar insumo'} onFechar={onFechar}>
      <div className="space-y-2 text-sm">
        <input value={f.nome} onChange={(e) => set('nome', e.target.value)} placeholder="Nome (ex.: Picanha, Queijo coalho)" className="border rounded-lg px-3 py-2 w-full" />
        <select value={f.unidade} onChange={(e) => set('unidade', e.target.value)} className="border rounded-lg px-3 py-2 w-full">
          <option value="KG">Quilo (kg)</option><option value="L">Litro (L)</option><option value="UN">Unidade</option><option value="FARDO">Fardo</option><option value="PACOTE">Pacote</option>
        </select>
        <div className="grid grid-cols-2 gap-2">
          <div><label className="text-xs text-gray-600">Estoque mínimo ({BASE[f.unidade]})</label><input value={f.estoqueMinimo} onChange={(e) => set('estoqueMinimo', e.target.value)} inputMode="decimal" className="border rounded-lg px-3 py-2 w-full" /></div>
          <div><label className="text-xs text-gray-600">Custo (R$ por {BASE[f.unidade]})</label><input value={f.custoUnitario} onChange={(e) => set('custoUnitario', e.target.value)} inputMode="decimal" className="border rounded-lg px-3 py-2 w-full" /></div>
        </div>
        <label className="flex items-center gap-2"><input type="checkbox" checked={f.ativo} onChange={(e) => set('ativo', e.target.checked)} /> Ativo</label>
        {erro && <p className="text-red-600">{erro}</p>}
        <div className="flex justify-end gap-2"><button onClick={onFechar} className="px-4 py-2">Cancelar</button><button onClick={salvar} disabled={!f.nome} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-40">Salvar</button></div>
      </div>
    </Modal>
  )
}

function MovimentoModal({ insumo, onFechar, onSalvo }) {
  const [tipo, setTipo] = useState('ENTRADA')
  const [qtd, setQtd] = useState('')
  const [custo, setCusto] = useState('')
  const [obs, setObs] = useState('')
  const [hist, setHist] = useState([])
  const [erro, setErro] = useState('')
  useEffect(() => { restauranteService.movimentos(insumo.id).then(setHist).catch(() => {}) }, [insumo.id])

  async function salvar() {
    setErro('')
    const quantidade = Number(String(qtd).replace(',', '.')) / FATOR[insumo.unidade]
    try {
      await restauranteService.movimentarInsumo(insumo.id, {
        tipo, quantidade, observacao: obs || null,
        custoUnitario: tipo === 'ENTRADA' && custo ? Number(String(custo).replace(',', '.')) : null,
      })
      onSalvo()
    } catch (err) { setErro(msg(err, 'Não foi possível registrar o movimento.')) }
  }

  const rotulos = { ENTRADA: 'Entrada (compra)', PERDA: 'Perda / desperdício', AJUSTE: 'Ajuste (contagem do estoque)' }
  return (
    <Modal titulo={`Movimentar — ${insumo.nome}`} onFechar={onFechar}>
      <p className="text-xs text-gray-500 mb-2">Estoque atual: <strong>{num(insumo.estoqueAtual)} {BASE[insumo.unidade]}</strong>. Informe a quantidade em {MENOR[insumo.unidade]}.</p>
      <div className="space-y-2 text-sm">
        <select value={tipo} onChange={(e) => setTipo(e.target.value)} className="border rounded-lg px-3 py-2 w-full">{Object.entries(rotulos).map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select>
        <input value={qtd} onChange={(e) => setQtd(e.target.value)} inputMode="decimal" placeholder={tipo === 'AJUSTE' ? `Novo estoque total (${MENOR[insumo.unidade]})` : `Quantidade (${MENOR[insumo.unidade]})`} className="border rounded-lg px-3 py-2 w-full" />
        {tipo === 'ENTRADA' && <input value={custo} onChange={(e) => setCusto(e.target.value)} inputMode="decimal" placeholder={`Novo custo (R$ por ${BASE[insumo.unidade]}) — opcional`} className="border rounded-lg px-3 py-2 w-full" />}
        <input value={obs} onChange={(e) => setObs(e.target.value)} placeholder="Observação (fornecedor, motivo...)" className="border rounded-lg px-3 py-2 w-full" />
        {erro && <p className="text-red-600">{erro}</p>}
        <div className="flex justify-end gap-2"><button onClick={onFechar} className="px-4 py-2">Cancelar</button><button onClick={salvar} disabled={qtd === ''} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-40">Registrar</button></div>
      </div>
      {hist.length > 0 && (
        <div className="mt-4 border-t pt-3">
          <h4 className="text-xs font-semibold text-gray-600 mb-1">Últimos movimentos</h4>
          <div className="max-h-40 overflow-y-auto text-xs space-y-0.5">
            {hist.map((m) => (
              <div key={m.id} className="flex justify-between">
                <span>{format(new Date(m.criadoEm), 'dd/MM HH:mm')} · {m.tipo.replace('_', ' ').toLowerCase()}{m.observacao ? ` · ${m.observacao}` : ''}</span>
                <span className={Number(m.quantidade) < 0 ? 'text-red-600' : 'text-green-700'}>{num(m.quantidade)} {BASE[insumo.unidade]}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </Modal>
  )
}

/* ============================ RELATÓRIO ============================ */

function AbaRelatorio() {
  const hoje = new Date()
  const [inicio, setInicio] = useState(format(startOfMonth(hoje), 'yyyy-MM-dd'))
  const [fim, setFim] = useState(format(endOfMonth(hoje), 'yyyy-MM-dd'))
  const [r, setR] = useState(null)
  const [erro, setErro] = useState('')

  const carregar = useCallback(async () => {
    try { setR(await restauranteService.relatorio(inicio, fim)); setErro('') } catch (err) { setErro(msg(err, 'Não foi possível gerar o relatório.')) }
  }, [inicio, fim])
  useEffect(() => { carregar() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const maxHora = r ? Math.max(1, ...r.porHora.map((h) => Number(h.receita))) : 1

  return (
    <div>
      <div className="bg-white rounded-xl shadow p-4 mb-4 flex flex-wrap gap-4 items-end">
        <div><label className="block text-sm text-gray-600 mb-1">De</label><input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} className="border rounded-lg px-3 py-2" /></div>
        <div><label className="block text-sm text-gray-600 mb-1">Até</label><input type="date" value={fim} onChange={(e) => setFim(e.target.value)} className="border rounded-lg px-3 py-2" /></div>
        <button onClick={carregar} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg">Aplicar</button>
      </div>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      {r && (
        <>
          <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 mb-4">
            {[['Receita total', brl(r.receitaTotal)], ['Comandas fechadas', r.comandas], ['Ticket médio', brl(r.ticketMedio)], ['Taxa de serviço', brl(r.taxaServico)],
              ['Receita de itens', brl(r.receitaItens)], ['Descontos', brl(r.descontos)], ['Custo (fichas)', brl(r.custoTotal)], ['Margem (itens − custo)', brl(r.margemTotal)]].map(([l, v]) => (
              <div key={l} className="bg-white rounded-xl shadow p-4"><p className="text-xs text-gray-500">{l}</p><p className="text-xl font-bold text-araca-azul">{v}</p></div>
            ))}
          </div>
          {r.cortesias > 0 && (
            <p className="text-xs text-purple-800 bg-purple-50 border border-purple-200 rounded-lg px-3 py-2 mb-3">
              {r.cortesias} cortesia(s) no período (fora da receita): valor de referência {brl(r.valorCortesias)}, custo estimado {brl(r.custoCortesias)}.
            </p>
          )}
          {r.itensSemFicha > 0 && <p className="text-xs text-amber-700 mb-3">{r.itensSemFicha} item(ns) vendido(s) sem ficha técnica: o custo e a margem acima ficam superestimados até cadastrar as fichas.</p>}
          {r.insumosAbaixoDoMinimo.length > 0 && <p className="text-xs text-red-700 mb-3">Estoque baixo: {r.insumosAbaixoDoMinimo.join(', ')}.</p>}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="bg-white rounded-xl shadow p-4 overflow-x-auto">
              <h3 className="font-semibold text-araca-azul mb-2">Mais vendidos</h3>
              <table className="w-full text-sm">
                <thead className="text-left text-gray-500 text-xs"><tr><th>Item</th><th className="text-right">Qtd</th><th className="text-right">Receita</th><th className="text-right">Margem</th></tr></thead>
                <tbody>
                  {r.maisVendidos.map((i) => (
                    <tr key={i.itemId} className="border-t"><td className="py-1">{i.nome}</td><td className="text-right">{i.quantidade}</td><td className="text-right">{brl(i.receita)}</td><td className="text-right">{brl(i.margem)}</td></tr>
                  ))}
                  {r.maisVendidos.length === 0 && <tr><td colSpan={4} className="py-3 text-gray-400">Sem vendas no período.</td></tr>}
                </tbody>
              </table>
            </div>
            <div className="space-y-4">
              <div className="bg-white rounded-xl shadow p-4">
                <h3 className="font-semibold text-araca-azul mb-2">Receita por horário do pedido</h3>
                {r.porHora.map((h) => (
                  <div key={h.chave} className="flex items-center gap-2 text-xs mb-1">
                    <span className="w-8">{String(h.chave).padStart(2, '0')}h</span>
                    <div className="flex-1 bg-gray-100 h-3 rounded"><div className="bg-araca-verde h-3 rounded" style={{ width: `${(Number(h.receita) / maxHora) * 100}%` }} /></div>
                    <span className="w-20 text-right">{brl(h.receita)}</span>
                  </div>
                ))}
                {r.porHora.length === 0 && <p className="text-gray-400 text-sm">Sem vendas no período.</p>}
              </div>
              <div className="bg-white rounded-xl shadow p-4">
                <h3 className="font-semibold text-araca-azul mb-2">Por dia da semana</h3>
                {r.porDiaSemana.map((d) => (<div key={d.chave} className="flex justify-between text-sm py-0.5"><span>{DIAS[d.chave]}</span><span>{d.quantidade} itens · {brl(d.receita)}</span></div>))}
                {r.porDiaSemana.length === 0 && <p className="text-gray-400 text-sm">Sem vendas no período.</p>}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
