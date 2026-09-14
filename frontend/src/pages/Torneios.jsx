import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { torneioService, chaveamentoService } from '../services/torneioService.js'
import { Trophy } from 'lucide-react'

const MODALIDADES = [
  { value: 'VOLEI', label: 'Vôlei' },
  { value: 'FUTEVOLEI', label: 'Futevôlei' },
  { value: 'BEACH_TENNIS', label: 'Beach Tennis' },
]

const CATEGORIAS_SUGERIDAS = {
  VOLEI: ['Masculino', 'Feminino', 'Misto'],
  FUTEVOLEI: ['Masculino A', 'Masculino B', 'Feminino', 'Dupla Mista'],
  BEACH_TENNIS: ['Masculino A', 'Masculino B', 'Feminino A', 'Feminino B', 'Dupla Mista'],
}

export default function Torneios() {
  const [torneios, setTorneios] = useState([])
  const [selecionado, setSelecionado] = useState(null)
  const [inscricoes, setInscricoes] = useState([])
  const [erro, setErro] = useState('')

  const [formTorneio, setFormTorneio] = useState({ nome: '', modalidade: 'BEACH_TENNIS', dataInicio: '', dataFim: '', categorias: [] })
  const [categoriaParaAdicionar, setCategoriaParaAdicionar] = useState('')

  const [categoriaChaveamento, setCategoriaChaveamento] = useState('')
  const [confrontos, setConfrontos] = useState([])
  const [carregandoChaveamento, setCarregandoChaveamento] = useState(false)
  const [formInscricao, setFormInscricao] = useState({ participante: '', parceiro: '', categoria: '' })

  async function carregarTorneios() {
    try {
      const lista = await torneioService.listar()
      setTorneios(lista)
    } catch {
      setErro('Não foi possível carregar os torneios.')
    }
  }

  useEffect(() => { carregarTorneios() }, [])

  async function abrirTorneio(torneio) {
    setSelecionado(torneio)
    setCategoriaChaveamento('')
    setConfrontos([])
    try {
      const lista = await torneioService.listarInscricoes(torneio.id)
      setInscricoes(lista)
    } catch {
      setErro('Não foi possível carregar as inscrições.')
    }
  }

  async function handleVerChaveamento(categoria) {
    setCategoriaChaveamento(categoria)
    setCarregandoChaveamento(true)
    setErro('')
    try {
      const lista = await chaveamentoService.listar(selecionado.id, categoria)
      setConfrontos(lista)
    } catch {
      setConfrontos([])
    } finally {
      setCarregandoChaveamento(false)
    }
  }

  async function handleGerarChaveamento() {
    if (!categoriaChaveamento) return
    if (confrontos.length > 0 && !confirm('Já existe um chaveamento para esta categoria. Gerar de novo vai sortear tudo outra vez, apagando os resultados já registrados. Continuar?')) {
      return
    }
    setCarregandoChaveamento(true)
    setErro('')
    try {
      const lista = await chaveamentoService.gerar(selecionado.id, categoriaChaveamento)
      setConfrontos(lista)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível gerar o chaveamento (verifique se há pelo menos 2 inscritos nessa categoria).')
    } finally {
      setCarregandoChaveamento(false)
    }
  }

  async function handleRegistrarResultado(confronto) {
    const opcoes = [confronto.participanteA, confronto.participanteB].filter(Boolean)
    const vencedor = prompt(`Quem venceu?\n1 - ${opcoes[0]}\n2 - ${opcoes[1]}`, '1')
    if (vencedor === null) return
    const nomeVencedor = vencedor.trim() === '2' ? opcoes[1] : opcoes[0]
    const placar = prompt('Placar (opcional):') || ''
    try {
      const lista = await chaveamentoService.registrarResultado(confronto.id, { vencedor: nomeVencedor, placar })
      setConfrontos(lista)
    } catch {
      setErro('Não foi possível registrar o resultado.')
    }
  }

  const rodadas = confrontos.reduce((acc, c) => {
    acc[c.rodada] = acc[c.rodada] || []
    acc[c.rodada].push(c)
    return acc
  }, {})
  const totalRodadas = Object.keys(rodadas).length

  function nomeRodada(rodada) {
    const restantes = totalRodadas - rodada
    if (restantes === 0) return 'Final'
    if (restantes === 1) return 'Semifinal'
    if (restantes === 2) return 'Quartas de final'
    return `Rodada ${rodada}`
  }

  function adicionarCategoria(categoria) {
    const nome = categoria.trim()
    if (!nome || formTorneio.categorias.includes(nome)) return
    setFormTorneio({ ...formTorneio, categorias: [...formTorneio.categorias, nome] })
    setCategoriaParaAdicionar('')
  }

  function removerCategoria(categoria) {
    setFormTorneio({ ...formTorneio, categorias: formTorneio.categorias.filter((c) => c !== categoria) })
  }

  async function handleCriarTorneio(e) {
    e.preventDefault()
    setErro('')
    try {
      await torneioService.criar({ ...formTorneio, dataFim: formTorneio.dataFim || null })
      setFormTorneio({ nome: '', modalidade: 'BEACH_TENNIS', dataInicio: '', dataFim: '', categorias: [] })
      carregarTorneios()
    } catch {
      setErro('Não foi possível criar o torneio.')
    }
  }

  async function handleInscrever(e) {
    e.preventDefault()
    setErro('')
    try {
      await torneioService.inscrever({ torneioId: selecionado.id, ...formInscricao })
      setFormInscricao({ participante: '', parceiro: '', categoria: '' })
      abrirTorneio(selecionado)
    } catch {
      setErro('Não foi possível registrar a inscrição.')
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Trophy size={22} className="text-araca-verde-escuro" />
        Torneios
      </h2>

      <form onSubmit={handleCriarTorneio} className="bg-white rounded-xl shadow p-6 mb-6 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
            <input
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formTorneio.nome}
              onChange={(e) => setFormTorneio({ ...formTorneio, nome: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Modalidade</label>
            <select
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formTorneio.modalidade}
              onChange={(e) => setFormTorneio({ ...formTorneio, modalidade: e.target.value })}
            >
              {MODALIDADES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Início</label>
            <input
              type="date"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formTorneio.dataInicio}
              onChange={(e) => setFormTorneio({ ...formTorneio, dataInicio: e.target.value })}
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Categorias</label>
          <div className="flex flex-wrap gap-2 mb-2">
            {formTorneio.categorias.length === 0 && (
              <span className="text-sm text-gray-400">Nenhuma categoria adicionada ainda.</span>
            )}
            {formTorneio.categorias.map((c) => (
              <span key={c} className="inline-flex items-center gap-1 bg-araca-verde-claro text-araca-azul text-sm px-3 py-1 rounded-full">
                {c}
                <button
                  type="button"
                  onClick={() => removerCategoria(c)}
                  className="font-bold hover:text-red-600"
                  aria-label={`Remover categoria ${c}`}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
          <div className="flex flex-wrap gap-2 items-center">
            <select
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value=""
              onChange={(e) => adicionarCategoria(e.target.value)}
            >
              <option value="">Escolher categoria sugerida...</option>
              {(CATEGORIAS_SUGERIDAS[formTorneio.modalidade] || [])
                .filter((c) => !formTorneio.categorias.includes(c))
                .map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <span className="text-sm text-gray-400">ou</span>
            <input
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="Categoria personalizada"
              value={categoriaParaAdicionar}
              onChange={(e) => setCategoriaParaAdicionar(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault()
                  adicionarCategoria(categoriaParaAdicionar)
                }
              }}
            />
            <button
              type="button"
              onClick={() => adicionarCategoria(categoriaParaAdicionar)}
              className="border rounded-lg px-3 py-2 text-araca-azul hover:bg-gray-50"
            >
              Adicionar
            </button>
          </div>
        </div>

        <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
          Criar torneio
        </button>
      </form>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-xl shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-left">
              <tr>
                <th className="px-4 py-3">Nome</th>
                <th className="px-4 py-3">Modalidade</th>
                <th className="px-4 py-3">Início</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {torneios.length === 0 && (
                <tr><td className="px-4 py-4 text-gray-400" colSpan={5}>Nenhum torneio cadastrado ainda.</td></tr>
              )}
              {torneios.map((t) => (
                <tr key={t.id} className={`border-t ${selecionado?.id === t.id ? 'bg-gray-50' : ''}`}>
                  <td className="px-4 py-3 font-medium text-araca-azul">{t.nome}</td>
                  <td className="px-4 py-3">{MODALIDADES.find(m => m.value === t.modalidade)?.label}</td>
                  <td className="px-4 py-3">{format(new Date(t.dataInicio), 'dd/MM/yyyy')}</td>
                  <td className="px-4 py-3">{t.status}</td>
                  <td className="px-4 py-3 text-right">
                    <button onClick={() => abrirTorneio(t)} className="text-araca-verde-escuro hover:underline">
                      Inscrições
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="bg-white rounded-xl shadow p-6">
          {!selecionado && <p className="text-gray-400 text-sm">Selecione um torneio para ver/gerenciar inscrições.</p>}
          {selecionado && (
            <>
              <h3 className="font-semibold text-araca-azul mb-4">{selecionado.nome}</h3>
              <form onSubmit={handleInscrever} className="space-y-3 mb-4">
                <input
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  placeholder="Participante"
                  value={formInscricao.participante}
                  onChange={(e) => setFormInscricao({ ...formInscricao, participante: e.target.value })}
                  required
                />
                <input
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  placeholder="Parceiro (opcional, para duplas)"
                  value={formInscricao.parceiro}
                  onChange={(e) => setFormInscricao({ ...formInscricao, parceiro: e.target.value })}
                />
                <select
                  className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formInscricao.categoria}
                  onChange={(e) => setFormInscricao({ ...formInscricao, categoria: e.target.value })}
                  required
                >
                  <option value="">Categoria...</option>
                  {(selecionado.categorias || []).map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
                <button type="submit" className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">
                  Inscrever
                </button>
              </form>
              <ul className="text-sm space-y-1">
                {inscricoes.length === 0 && <li className="text-gray-400">Nenhuma inscrição ainda.</li>}
                {inscricoes.map((i) => (
                  <li key={i.id} className="border-t pt-1">
                    {i.participante}{i.parceiro ? ` / ${i.parceiro}` : ''} · {i.categoria}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </div>

      {selecionado && (selecionado.categorias || []).length > 0 && (
        <div className="bg-white rounded-xl shadow p-6 mt-6">
          <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
            <h3 className="font-semibold text-araca-azul">Chaveamento — {selecionado.nome}</h3>
            <div className="flex items-center gap-2">
              <select
                className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={categoriaChaveamento}
                onChange={(e) => handleVerChaveamento(e.target.value)}
              >
                <option value="">Selecione a categoria...</option>
                {(selecionado.categorias || []).map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
              {categoriaChaveamento && (
                <button
                  onClick={handleGerarChaveamento}
                  className="bg-araca-verde text-araca-azul text-sm font-semibold px-3 py-2 rounded-lg hover:opacity-90 transition"
                >
                  {confrontos.length > 0 ? 'Gerar novamente' : 'Gerar chaveamento'}
                </button>
              )}
            </div>
          </div>

          {carregandoChaveamento && <p className="text-gray-400 text-sm">Carregando...</p>}

          {!carregandoChaveamento && categoriaChaveamento && confrontos.length === 0 && (
            <p className="text-gray-400 text-sm">
              Nenhum chaveamento gerado ainda para esta categoria. Clique em "Gerar chaveamento".
            </p>
          )}

          {!carregandoChaveamento && confrontos.length > 0 && (
            <div className="flex gap-6 overflow-x-auto pb-2">
              {Object.keys(rodadas).sort((a, b) => a - b).map((rodada) => (
                <div key={rodada} className="flex flex-col gap-4 min-w-[220px]">
                  <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{nomeRodada(Number(rodada))}</p>
                  {rodadas[rodada]
                    .sort((a, b) => a.posicao - b.posicao)
                    .map((c) => (
                      <div
                        key={c.id}
                        className={`border rounded-lg overflow-hidden ${c.vencedor ? 'border-araca-verde-escuro' : 'border-gray-200'}`}
                      >
                        {[c.participanteA, c.participanteB].map((participante, idx) => (
                          <div
                            key={idx}
                            className={`px-3 py-2 text-sm flex items-center justify-between ${
                              c.vencedor && participante === c.vencedor ? 'bg-araca-verde-claro/40 font-semibold text-araca-azul' : 'text-gray-600'
                            } ${idx === 0 ? 'border-b' : ''}`}
                          >
                            <span className="truncate">{participante || 'A definir'}</span>
                          </div>
                        ))}
                        {!c.vencedor && c.participanteA && c.participanteB && (
                          <button
                            onClick={() => handleRegistrarResultado(c)}
                            className="w-full text-xs text-araca-verde-escuro hover:bg-araca-areia-escura py-1.5 transition"
                          >
                            Registrar resultado
                          </button>
                        )}
                        {c.placar && (
                          <p className="text-[11px] text-gray-400 text-center pb-1">{c.placar}</p>
                        )}
                      </div>
                    ))}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
