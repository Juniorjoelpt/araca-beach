import { useEffect, useState } from 'react'
import { quadraService } from '../services/quadraService.js'
import { MapPin } from 'lucide-react'

const TIPOS = [
  { value: 'VOLEI', label: 'Vôlei' },
  { value: 'FUTEVOLEI', label: 'Futevôlei' },
  { value: 'BEACH_TENNIS', label: 'Beach Tennis' },
]

const vazio = { nome: '', tipo: 'BEACH_TENNIS', valorHora: '', capacidade: '' }

export default function Quadras() {
  const [quadras, setQuadras] = useState([])
  const [form, setForm] = useState(vazio)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

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
    } catch {
      setErro('Não foi possível salvar a quadra.')
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

  const tipoLabel = (tipo) => TIPOS.find((t) => t.value === tipo)?.label ?? tipo

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
          <label className="block text-sm font-medium text-gray-700 mb-1">Tipo</label>
          <select
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.tipo}
            onChange={(e) => setForm({ ...form, tipo: e.target.value })}
          >
            {TIPOS.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
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

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 text-left">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">Tipo</th>
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
