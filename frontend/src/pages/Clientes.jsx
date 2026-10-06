import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { clienteService } from '../services/clienteService.js'
import { Users, X, History, Trash2 } from 'lucide-react'

const vazio = { nome: '', telefone: '', email: '' }

const STATUS_LABEL = {
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  CONCLUIDA: 'Concluída',
  NAO_COMPARECEU: 'Não compareceu',
}

export default function Clientes() {
  const [clientes, setClientes] = useState([])
  const [form, setForm] = useState(vazio)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [historicoAberto, setHistoricoAberto] = useState(false)
  const [clienteHistorico, setClienteHistorico] = useState(null)
  const [carregandoHistorico, setCarregandoHistorico] = useState(false)

  async function carregar() {
    setCarregando(true)
    try {
      const dados = await clienteService.listar()
      setClientes(dados)
    } catch {
      setErro('Não foi possível carregar os clientes.')
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
      await clienteService.criar(form)
      setForm(vazio)
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível salvar o cliente.')
    }
  }

  async function handleExcluir(cliente) {
    if (!confirm(`Excluir o cliente "${cliente.nome}"? Essa ação não pode ser desfeita.`)) return
    setErro('')
    try {
      await clienteService.deletar(cliente.id)
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível excluir o cliente.')
    }
  }

  async function handleVerHistorico(cliente) {
    setHistoricoAberto(true)
    setCarregandoHistorico(true)
    setClienteHistorico(null)
    try {
      const dados = await clienteService.historico(cliente.id)
      setClienteHistorico(dados)
    } catch {
      setErro('Não foi possível carregar o histórico do cliente.')
      setHistoricoAberto(false)
    } finally {
      setCarregandoHistorico(false)
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <Users size={22} className="text-araca-verde-escuro" />
        Clientes
      </h2>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow p-6 mb-6 grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
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
          <label className="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.telefone}
            onChange={(e) => setForm({ ...form, telefone: e.target.value })}
            placeholder="(00) 00000-0000"
          />
        </div>
        <div className="flex gap-2">
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-1">E-mail</label>
            <input
              type="email"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
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
              <th className="px-4 py-3">Telefone</th>
              <th className="px-4 py-3">E-mail</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={4}>Carregando...</td></tr>
            )}
            {!carregando && clientes.length === 0 && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={4}>Nenhum cliente cadastrado ainda.</td></tr>
            )}
            {clientes.map((c) => (
              <tr key={c.id} className="border-t">
                <td className="px-4 py-3 font-medium text-araca-azul">
                  {c.nome}
                  {(c.origemCadastro === 'PORTAL' || c.possuiAcessoPortal) && (
                    <span className="ml-2 px-2 py-0.5 rounded-full text-[11px] font-medium bg-blue-100 text-blue-700">
                      {c.possuiAcessoPortal ? 'Portal' : 'Portal (sem acesso)'}
                    </span>
                  )}
                </td>
                <td className="px-4 py-3">{c.telefone || '—'}</td>
                <td className="px-4 py-3">{c.email || '—'}</td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-3">
                    <button
                      onClick={() => handleVerHistorico(c)}
                      className="flex items-center gap-1 text-araca-verde-escuro hover:underline"
                    >
                      <History size={14} /> Histórico
                    </button>
                    <button
                      onClick={() => handleExcluir(c)}
                      className="flex items-center gap-1 text-red-600 hover:underline"
                    >
                      <Trash2 size={14} /> Excluir
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {historicoAberto && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-2xl max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-araca-azul">
                Histórico — {clienteHistorico?.clienteNome || '...'}
              </h3>
              <button onClick={() => setHistoricoAberto(false)} className="text-gray-400 hover:text-gray-600">
                <X size={18} />
              </button>
            </div>

            {carregandoHistorico && <p className="text-gray-400 text-sm">Carregando...</p>}

            {clienteHistorico && !carregandoHistorico && (
              <>
                <div className="grid grid-cols-2 gap-4 mb-6">
                  <div className="bg-araca-areia rounded-lg p-4">
                    <p className="text-xs text-gray-500">Total gasto em reservas</p>
                    <p className="text-xl font-bold text-araca-azul">R$ {Number(clienteHistorico.totalGastoReservas).toFixed(2)}</p>
                  </div>
                  <div className="bg-araca-areia rounded-lg p-4">
                    <p className="text-xs text-gray-500">Total gasto na loja</p>
                    <p className="text-xl font-bold text-araca-azul">R$ {Number(clienteHistorico.totalGastoLoja).toFixed(2)}</p>
                  </div>
                </div>

                <h4 className="font-medium text-araca-azul mb-2">Reservas</h4>
                {clienteHistorico.reservas.length === 0 && (
                  <p className="text-gray-400 text-sm mb-4">Nenhuma reserva ainda.</p>
                )}
                <ul className="text-sm divide-y mb-6">
                  {clienteHistorico.reservas.map((r) => (
                    <li key={r.reservaId} className="py-2 flex items-center justify-between">
                      <span>
                        {format(new Date(r.inicio), 'dd/MM/yyyy HH:mm')} · {r.quadraNome}
                      </span>
                      <span className="flex items-center gap-2">
                        <span className="text-xs text-gray-500">{STATUS_LABEL[r.status] || r.status}</span>
                        {r.valorTotal && <span className="font-medium text-araca-azul">R$ {Number(r.valorTotal).toFixed(2)}</span>}
                      </span>
                    </li>
                  ))}
                </ul>

                <h4 className="font-medium text-araca-azul mb-2">Comandas da loja</h4>
                {clienteHistorico.comandas.length === 0 && (
                  <p className="text-gray-400 text-sm">Nenhuma comanda ainda.</p>
                )}
                <ul className="text-sm divide-y">
                  {clienteHistorico.comandas.map((c) => (
                    <li key={c.comandaId} className="py-2 flex items-center justify-between">
                      <span>
                        {format(new Date(c.criadoEm), 'dd/MM/yyyy HH:mm')} · Comanda #{c.comandaId}
                        {!c.fechada && <span className="text-amber-600 text-xs ml-2">(aberta)</span>}
                      </span>
                      <span className="font-medium text-araca-azul">R$ {Number(c.total).toFixed(2)}</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
