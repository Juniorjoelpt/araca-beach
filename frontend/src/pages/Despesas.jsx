import { useEffect, useState } from 'react'
import { format, startOfMonth, endOfMonth } from 'date-fns'
import { Receipt, Repeat } from 'lucide-react'
import { despesaService } from '../services/despesaService.js'
import { despesaRecorrenteService } from '../services/despesaRecorrenteService.js'

const CATEGORIAS = [
  { value: 'AGUA', label: 'Água' },
  { value: 'LUZ', label: 'Luz' },
  { value: 'INTERNET', label: 'Internet' },
  { value: 'MANUTENCAO', label: 'Manutenção' },
  { value: 'SALARIO', label: 'Salário' },
  { value: 'FORNECEDOR', label: 'Fornecedor' },
  { value: 'ALUGUEL', label: 'Aluguel' },
  { value: 'COMISSAO', label: 'Comissão' },
  { value: 'OUTRO', label: 'Outro' },
]

function categoriaLabel(value) {
  return CATEGORIAS.find((c) => c.value === value)?.label ?? value
}

const vazio = { descricao: '', categoria: 'OUTRO', valor: '', dataVencimento: format(new Date(), 'yyyy-MM-dd'), observacoes: '' }

export default function Despesas() {
  const hoje = new Date()
  const [inicio, setInicio] = useState(format(startOfMonth(hoje), 'yyyy-MM-dd'))
  const [fim, setFim] = useState(format(endOfMonth(hoje), 'yyyy-MM-dd'))
  const [despesas, setDespesas] = useState([])
  const [resumo, setResumo] = useState(null)
  const [form, setForm] = useState(vazio)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)

  const [recorrentes, setRecorrentes] = useState([])
  const [formRecorrente, setFormRecorrente] = useState({ descricao: '', categoria: 'ALUGUEL', valor: '', diaVencimento: '5' })

  async function carregarRecorrentes() {
    try {
      setRecorrentes(await despesaRecorrenteService.listar())
    } catch {
      // lista complementar - nao bloqueia a tela
    }
  }

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const [lista, resumoData] = await Promise.all([
        despesaService.listar(inicio, fim),
        despesaService.resumo(inicio, fim),
      ])
      setDespesas(lista)
      setResumo(resumoData)
    } catch {
      setErro('Não foi possível carregar as despesas.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [inicio, fim]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { carregarRecorrentes() }, [])

  async function handleCriarRecorrente(e) {
    e.preventDefault()
    setErro('')
    try {
      await despesaRecorrenteService.criar({ ...formRecorrente, valor: Number(formRecorrente.valor), diaVencimento: Number(formRecorrente.diaVencimento) })
      setFormRecorrente({ descricao: '', categoria: 'ALUGUEL', valor: '', diaVencimento: '5' })
      carregarRecorrentes()
    } catch {
      setErro('Não foi possível salvar a despesa recorrente.')
    }
  }

  async function handleDesativarRecorrente(id) {
    if (!confirm('Desativar esta despesa recorrente? Ela vai parar de ser lançada automaticamente todo mês.')) return
    try {
      await despesaRecorrenteService.desativar(id)
      carregarRecorrentes()
    } catch {
      setErro('Não foi possível desativar a despesa recorrente.')
    }
  }

  async function handleCriar(e) {
    e.preventDefault()
    setErro('')
    try {
      await despesaService.criar({ ...form, valor: Number(form.valor) })
      setForm(vazio)
      carregar()
    } catch {
      setErro('Não foi possível salvar a despesa.')
    }
  }

  async function handleMarcarPaga(id) {
    try {
      await despesaService.marcarComoPaga(id)
      carregar()
    } catch {
      setErro('Não foi possível marcar a despesa como paga.')
    }
  }

  async function handleRemover(id) {
    if (!confirm('Remover esta despesa?')) return
    try {
      await despesaService.remover(id)
      carregar()
    } catch {
      setErro('Não foi possível remover a despesa.')
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Receipt size={22} className="text-araca-verde-escuro" />
        Despesas
      </h2>

      <div className="bg-white rounded-xl shadow p-6 mb-6 flex flex-wrap items-end gap-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
          <input type="date" className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
          <input type="date" className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
        {resumo && (
          <>
            <div>
              <p className="text-xs text-gray-500">Pago no período</p>
              <p className="text-xl font-bold text-araca-azul">R$ {Number(resumo.totalPago).toFixed(2)}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Pendente</p>
              <p className="text-xl font-bold text-red-600">R$ {Number(resumo.totalPendente).toFixed(2)}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Total do período</p>
              <p className="text-xl font-bold text-araca-azul">R$ {Number(resumo.totalGeral).toFixed(2)}</p>
            </div>
          </>
        )}
      </div>

      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <h3 className="font-semibold text-araca-azul flex items-center gap-2 mb-4">
          <Repeat size={18} className="text-araca-verde-escuro" />
          Despesas recorrentes (lançadas automaticamente todo mês)
        </h3>
        <form onSubmit={handleCriarRecorrente} className="grid grid-cols-1 md:grid-cols-5 gap-4 items-end mb-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
            <input
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="Ex: Aluguel do imóvel"
              value={formRecorrente.descricao}
              onChange={(e) => setFormRecorrente({ ...formRecorrente, descricao: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Categoria</label>
            <select
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formRecorrente.categoria}
              onChange={(e) => setFormRecorrente({ ...formRecorrente, categoria: e.target.value })}
            >
              {CATEGORIAS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Valor (R$)</label>
            <input
              type="number" step="0.01" min="0.01"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formRecorrente.valor}
              onChange={(e) => setFormRecorrente({ ...formRecorrente, valor: e.target.value })}
              required
            />
          </div>
          <div className="flex gap-2">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Dia (1-28)</label>
              <input
                type="number" min="1" max="28"
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formRecorrente.diaVencimento}
                onChange={(e) => setFormRecorrente({ ...formRecorrente, diaVencimento: e.target.value })}
                required
              />
            </div>
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition h-fit">
              Adicionar
            </button>
          </div>
        </form>

        {recorrentes.length === 0 && <p className="text-gray-400 text-sm">Nenhuma despesa recorrente cadastrada.</p>}
        <ul className="text-sm divide-y">
          {recorrentes.map((r) => (
            <li key={r.id} className="py-2 flex items-center justify-between">
              <span>
                {r.descricao} · {categoriaLabel(r.categoria)} · R$ {Number(r.valor).toFixed(2)} · todo dia {r.diaVencimento}
              </span>
              <button onClick={() => handleDesativarRecorrente(r.id)} className="text-red-600 hover:underline">
                Desativar
              </button>
            </li>
          ))}
        </ul>
      </div>

      <form onSubmit={handleCriar} className="bg-white rounded-xl shadow p-6 mb-6 grid grid-cols-1 md:grid-cols-6 gap-4 items-end">
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.descricao}
            onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Categoria</label>
          <select
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.categoria}
            onChange={(e) => setForm({ ...form, categoria: e.target.value })}
          >
            {CATEGORIAS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Valor (R$)</label>
          <input
            type="number" step="0.01" min="0.01"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.valor}
            onChange={(e) => setForm({ ...form, valor: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Vencimento</label>
          <input
            type="date"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.dataVencimento}
            onChange={(e) => setForm({ ...form, dataVencimento: e.target.value })}
            required
          />
        </div>
        <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition h-fit">
          Adicionar
        </button>
      </form>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 text-left">
            <tr>
              <th className="px-4 py-3">Descrição</th>
              <th className="px-4 py-3">Categoria</th>
              <th className="px-4 py-3">Vencimento</th>
              <th className="px-4 py-3">Valor</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && <tr><td className="px-4 py-4 text-gray-400" colSpan={6}>Carregando...</td></tr>}
            {!carregando && despesas.length === 0 && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={6}>Nenhuma despesa nesse período.</td></tr>
            )}
            {despesas.map((d) => (
              <tr key={d.id} className="border-t">
                <td className="px-4 py-3 font-medium text-araca-azul">{d.descricao}</td>
                <td className="px-4 py-3">{categoriaLabel(d.categoria)}</td>
                <td className="px-4 py-3">{format(new Date(`${d.dataVencimento}T00:00:00`), 'dd/MM/yyyy')}</td>
                <td className="px-4 py-3">R$ {Number(d.valor).toFixed(2)}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${d.paga ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {d.paga ? 'Paga' : 'Pendente'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right space-x-3">
                  {!d.paga && (
                    <button onClick={() => handleMarcarPaga(d.id)} className="text-araca-verde-escuro hover:underline">
                      Marcar como paga
                    </button>
                  )}
                  <button onClick={() => handleRemover(d.id)} className="text-red-600 hover:underline">
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
