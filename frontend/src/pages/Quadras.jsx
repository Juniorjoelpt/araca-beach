import { useEffect, useState } from 'react'
import { quadraService } from '../services/quadraService.js'
import { modalidadeService } from '../services/modalidadeService.js'
import { useModalidades } from '../utils/useModalidades.js'
import { MapPin, Plus, Pencil, Trash2, Check, X } from 'lucide-react'

const vazio = { nome: '', tipo: '', valorHora: '', capacidade: '' }
const msgErro = (err, padrao) => err?.response?.data?.mensagem || padrao

export default function Quadras() {
  const [quadras, setQuadras] = useState([])
  const [form, setForm] = useState(vazio)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const { modalidades, rotulo, recarregar: recarregarModalidades } = useModalidades()
  const [painelModalidades, setPainelModalidades] = useState(false)
  const [novaModalidade, setNovaModalidade] = useState('')
  const [editModId, setEditModId] = useState(null)
  const [editModNome, setEditModNome] = useState('')
  const [erroMod, setErroMod] = useState('')

  // Seleciona a primeira modalidade quando a lista carrega ou quando a escolhida some.
  useEffect(() => {
    if (modalidades.length && !modalidades.some((m) => m.value === form.tipo)) {
      setForm((f) => ({ ...f, tipo: modalidades[0].value }))
    }
  }, [modalidades]) // eslint-disable-line react-hooks/exhaustive-deps

  async function carregar() {
    setCarregando(true)
    try {
      const dados = await quadraService.listar()
      setQuadras(dados)
    } catch {
      setErro('Não foi possível carregar as quadras.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    try {
      await quadraService.criar({
        ...form,
        valorHora: Number(form.valorHora),
        capacidade: form.capacidade ? Number(form.capacidade) : null,
      })
      setForm(vazio)
      carregar()
    } catch (err) {
      setErro(msgErro(err, 'Não foi possível salvar a quadra.'))
    }
  }

  async function handleRemover(id) {
    if (!confirm('Remover esta quadra?')) return
    try {
      await quadraService.remover(id)
      carregar()
    } catch {
      setErro('Não foi possível remover a quadra (pode ter reservas vinculadas).')
    }
  }

  async function adicionarModalidade(e) {
    e?.preventDefault()
    setErroMod('')
    const nome = novaModalidade.trim()
    if (!nome) return
    try {
      const criada = await modalidadeService.criar(nome)
      setNovaModalidade('')
      await recarregarModalidades()
      setForm((f) => ({ ...f, tipo: criada.codigo }))
    } catch (err) {
      setErroMod(msgErro(err, 'Não foi possível adicionar a modalidade.'))
    }
  }

  async function salvarNomeModalidade(id) {
    setErroMod('')
    try {
      await modalidadeService.renomear(id, editModNome)
      setEditModId(null)
      await recarregarModalidades()
    } catch (err) {
      setErroMod(msgErro(err, 'Não foi possível renomear a modalidade.'))
    }
  }

  async function removerModalidade(m) {
    if (!confirm(`Remover a modalidade "${m.label}"?`)) return
    setErroMod('')
    try {
      await modalidadeService.excluir(m.id)
      await recarregarModalidades()
    } catch (err) {
      setErroMod(msgErro(err, 'Não foi possível remover a modalidade.'))
    }
  }

  const tipoLabel = rotulo

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <MapPin size={22} className="text-araca-verde-escuro" />
        Quadras
      </h2>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow p-6 mb-6 grid grid-cols-1 md:grid-cols-5 gap-4 items-end">
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Modalidade</label>
          <div className="flex gap-2">
            <select
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.tipo}
              onChange={(e) => setForm({ ...form, tipo: e.target.value })}
              required
            >
              {modalidades.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <button
              type="button"
              onClick={() => setPainelModalidades((v) => !v)}
              title="Adicionar ou gerenciar modalidades"
              className={`shrink-0 border rounded-lg px-3 py-2 hover:bg-gray-50 ${painelModalidades ? 'bg-gray-100' : ''}`}
            >
              <Plus size={18} className="text-araca-azul" />
            </button>
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Valor/hora (R$)</label>
          <input
            type="number"
            step="0.01"
            min="0"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.valorHora}
            onChange={(e) => setForm({ ...form, valorHora: e.target.value })}
            required
          />
        </div>
        <div className="flex gap-2">
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-1">Capacidade</label>
            <input
              type="number"
              min="0"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.capacidade}
              onChange={(e) => setForm({ ...form, capacidade: e.target.value })}
            />
          </div>
          <button
            type="submit"
            className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition h-fit"
          >
            Adicionar
          </button>
        </div>
      </form>

      {painelModalidades && (
        <div className="bg-white rounded-xl shadow p-5 mb-6">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-araca-azul">Modalidades</h3>
            <button type="button" onClick={() => setPainelModalidades(false)} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
          </div>
          <form onSubmit={adicionarModalidade} className="flex gap-2 mb-3">
            <input
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="Nova modalidade (ex.: Altinha, Tênis, Padel)"
              maxLength={60}
              value={novaModalidade}
              onChange={(e) => setNovaModalidade(e.target.value)}
            />
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
              Adicionar modalidade
            </button>
          </form>
          {erroMod && <p className="text-red-600 text-sm mb-2">{erroMod}</p>}
          <ul className="divide-y text-sm">
            {modalidades.map((m) => (
              <li key={m.value} className="flex items-center gap-2 py-2">
                {editModId === m.id ? (
                  <>
                    <input
                      className="flex-1 border rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={editModNome}
                      maxLength={60}
                      autoFocus
                      onChange={(e) => setEditModNome(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') salvarNomeModalidade(m.id); if (e.key === 'Escape') setEditModId(null) }}
                    />
                    <button type="button" onClick={() => salvarNomeModalidade(m.id)} title="Salvar" className="text-green-700 hover:opacity-80"><Check size={18} /></button>
                    <button type="button" onClick={() => setEditModId(null)} title="Cancelar" className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
                  </>
                ) : (
                  <>
                    <span className="flex-1 font-medium text-araca-azul">{m.label}</span>
                    <button type="button" onClick={() => { setEditModId(m.id); setEditModNome(m.label); setErroMod('') }} title="Renomear" className="text-gray-500 hover:text-araca-azul"><Pencil size={16} /></button>
                    <button type="button" onClick={() => removerModalidade(m)} title="Remover" className="text-red-500 hover:text-red-700"><Trash2 size={16} /></button>
                  </>
                )}
              </li>
            ))}
            {modalidades.length === 0 && <li className="py-2 text-gray-400">Nenhuma modalidade cadastrada.</li>}
          </ul>
          <p className="text-xs text-gray-500 mt-3">Só é possível remover uma modalidade que não esteja em uso por quadras, professores, turmas, torneios ou pacotes. Renomear atualiza o nome em todas as telas.</p>
        </div>
      )}

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 text-left">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">Modalidade</th>
              <th className="px-4 py-3">Valor/hora</th>
              <th className="px-4 py-3">Capacidade</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={6}>Carregando...</td></tr>
            )}
            {!carregando && quadras.length === 0 && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={6}>Nenhuma quadra cadastrada ainda.</td></tr>
            )}
            {quadras.map((q) => (
              <tr key={q.id} className="border-t">
                <td className="px-4 py-3 font-medium text-araca-azul">{q.nome}</td>
                <td className="px-4 py-3">{tipoLabel(q.tipo)}</td>
                <td className="px-4 py-3">R$ {Number(q.valorHora).toFixed(2)}</td>
                <td className="px-4 py-3">{q.capacidade ?? '—'}</td>
                <td className="px-4 py-3">{q.status}</td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => handleRemover(q.id)}
                    className="text-red-600 hover:underline"
                  >
                    Remover
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
