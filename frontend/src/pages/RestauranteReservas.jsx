import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { format } from 'date-fns'
import { CalendarClock, Plus, X } from 'lucide-react'
import { restauranteService } from '../services/restauranteService.js'
import { clienteService } from '../services/clienteService.js'

const STATUS = {
  CONFIRMADA: { label: 'Confirmada', cor: 'bg-blue-100 text-blue-700' },
  EM_ATENDIMENTO: { label: 'Em atendimento', cor: 'bg-green-100 text-green-700' },
  CONCLUIDA: { label: 'Concluída', cor: 'bg-gray-100 text-gray-600' },
  CANCELADA: { label: 'Cancelada', cor: 'bg-red-100 text-red-700' },
  NAO_COMPARECEU: { label: 'Não compareceu', cor: 'bg-amber-100 text-amber-700' },
}
const msg = (err, padrao) => err.response?.data?.mensagem || padrao

export default function RestauranteReservas() {
  const navigate = useNavigate()
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [reservas, setReservas] = useState([])
  const [erro, setErro] = useState('')
  const [modal, setModal] = useState(false)

  async function carregar() {
    try {
      setReservas(await restauranteService.reservasMesa(data))
      setErro('')
    } catch (err) {
      setErro(msg(err, 'Não foi possível carregar as reservas.'))
    }
  }

  useEffect(() => { carregar() }, [data]) // eslint-disable-line react-hooks/exhaustive-deps

  async function acao(fn) {
    setErro('')
    try { await fn(); await carregar() } catch (err) { setErro(msg(err, 'Não foi possível concluir a operação.')) }
  }

  async function definirMesa(r) {
    const mesa = window.prompt('Mesa para esta reserva:', r.mesa || '')
    if (mesa === null) return
    acao(() => restauranteService.mesaDaReserva(r.id, mesa))
  }

  async function abrirComanda(r) {
    let mesa = r.mesa
    if (!mesa) {
      mesa = window.prompt('Informe a mesa para atender esta reserva:')
      if (!mesa) return
    }
    setErro('')
    try {
      const comanda = await restauranteService.abrirComanda({ reservaMesaId: r.id, mesa })
      navigate('/restaurante', { state: { comandaId: comanda.id } })
    } catch (err) {
      setErro(msg(err, 'Não foi possível abrir a comanda.'))
    }
  }

  const totalPessoas = reservas.filter((r) => ['CONFIRMADA', 'EM_ATENDIMENTO'].includes(r.status)).reduce((s, r) => s + r.pessoas, 0)

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h2 className="font-title text-2xl text-araca-verde flex items-center gap-2"><CalendarClock size={24} /> Reservas de mesa</h2>
        <div className="flex items-center gap-3">
          <input type="date" value={data} onChange={(e) => setData(e.target.value)} className="border rounded-lg px-3 py-2" />
          <button onClick={() => setModal(true)} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg flex items-center gap-2"><Plus size={18} /> Nova reserva</button>
        </div>
      </div>
      {erro && <p className="text-red-600 text-sm mb-3">{erro}</p>}
      <p className="text-sm text-gray-500 mb-3">{reservas.length} reserva(s) · {totalPessoas} pessoa(s) esperadas/atendidas</p>

      <div className="bg-white rounded-xl shadow overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-600">
            <tr><th className="px-3 py-2">Hora</th><th className="px-3 py-2">Cliente</th><th className="px-3 py-2">Pessoas</th><th className="px-3 py-2">Mesa</th><th className="px-3 py-2">Status</th><th className="px-3 py-2" /></tr>
          </thead>
          <tbody>
            {reservas.map((r) => (
              <tr key={r.id} className="border-t align-top">
                <td className="px-3 py-2 whitespace-nowrap font-medium">{format(new Date(r.dataHora), 'HH:mm')}</td>
                <td className="px-3 py-2">
                  <div>{r.clienteNome}</div>
                  <div className="text-xs text-gray-400">{r.clienteTelefone}</div>
                  {r.observacoes && <div className="text-xs text-gray-500">{r.observacoes}</div>}
                </td>
                <td className="px-3 py-2">{r.pessoas}</td>
                <td className="px-3 py-2">{r.mesa || <span className="text-gray-400">a definir</span>}</td>
                <td className="px-3 py-2"><span className={`text-xs px-2 py-0.5 rounded ${STATUS[r.status].cor}`}>{STATUS[r.status].label}</span></td>
                <td className="px-3 py-2 text-right space-x-3 whitespace-nowrap">
                  {r.status === 'CONFIRMADA' && (
                    <>
                      <button onClick={() => abrirComanda(r)} className="text-araca-azul font-medium underline">Abrir comanda</button>
                      <button onClick={() => definirMesa(r)} className="text-gray-600 underline">Mesa</button>
                      <button onClick={() => acao(() => restauranteService.naoCompareceuMesa(r.id))} className="text-amber-700 underline">Não veio</button>
                      <button onClick={() => window.confirm('Cancelar a reserva?') && acao(() => restauranteService.cancelarReservaMesa(r.id))} className="text-red-600 underline">Cancelar</button>
                    </>
                  )}
                  {r.status === 'EM_ATENDIMENTO' && (
                    <button onClick={() => navigate('/restaurante', { state: { comandaId: r.comandaId } })} className="text-araca-azul font-medium underline">Ver comanda</button>
                  )}
                </td>
              </tr>
            ))}
            {reservas.length === 0 && <tr><td colSpan={6} className="px-3 py-6 text-center text-gray-400">Nenhuma reserva neste dia.</td></tr>}
          </tbody>
        </table>
      </div>

      {modal && <NovaReservaModal dataPadrao={data} onFechar={() => setModal(false)} onCriada={() => { setModal(false); carregar() }} />}
    </div>
  )
}

function NovaReservaModal({ dataPadrao, onFechar, onCriada }) {
  const [clientes, setClientes] = useState([])
  const [busca, setBusca] = useState('')
  const [form, setForm] = useState({ clienteId: '', dataHora: `${dataPadrao}T20:00`, pessoas: 2, mesa: '', observacoes: '' })
  const [erro, setErro] = useState('')

  useEffect(() => { clienteService.listar().then(setClientes).catch(() => setErro('Não foi possível carregar os clientes.')) }, [])

  const filtrados = useMemo(() => {
    const t = busca.trim().toLowerCase()
    return (t ? clientes.filter((c) => c.nome.toLowerCase().includes(t) || (c.telefone || '').includes(t)) : clientes).slice(0, 50)
  }, [clientes, busca])

  async function salvar() {
    setErro('')
    try {
      await restauranteService.criarReservaMesa({
        clienteId: Number(form.clienteId),
        dataHora: form.dataHora,
        pessoas: Number(form.pessoas),
        mesa: form.mesa || null,
        observacoes: form.observacoes || null,
      })
      onCriada()
    } catch (err) {
      setErro(msg(err, 'Não foi possível criar a reserva.'))
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-5 space-y-3">
        <div className="flex items-center justify-between"><h3 className="font-semibold text-araca-azul">Nova reserva de mesa</h3><button onClick={onFechar}><X size={18} /></button></div>
        <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar cliente por nome ou telefone" className="border rounded-lg px-3 py-2 w-full text-sm" />
        <select value={form.clienteId} onChange={(e) => setForm({ ...form, clienteId: e.target.value })} size={4} className="border rounded-lg w-full text-sm">
          {filtrados.map((c) => <option key={c.id} value={c.id}>{c.nome}{c.telefone ? ` · ${c.telefone}` : ''}</option>)}
        </select>
        <div className="grid grid-cols-2 gap-2">
          <div><label className="text-xs text-gray-600">Data e hora</label><input type="datetime-local" value={form.dataHora} onChange={(e) => setForm({ ...form, dataHora: e.target.value })} className="border rounded-lg px-2 py-2 w-full text-sm" /></div>
          <div><label className="text-xs text-gray-600">Pessoas</label><input type="number" min="1" value={form.pessoas} onChange={(e) => setForm({ ...form, pessoas: e.target.value })} className="border rounded-lg px-2 py-2 w-full text-sm" /></div>
        </div>
        <input value={form.mesa} onChange={(e) => setForm({ ...form, mesa: e.target.value })} placeholder="Mesa (pode definir depois)" className="border rounded-lg px-3 py-2 w-full text-sm" />
        <input value={form.observacoes} onChange={(e) => setForm({ ...form, observacoes: e.target.value })} placeholder="Observações" className="border rounded-lg px-3 py-2 w-full text-sm" />
        {erro && <p className="text-red-600 text-sm">{erro}</p>}
        <div className="flex justify-end gap-2">
          <button onClick={onFechar} className="px-4 py-2 text-sm">Cancelar</button>
          <button onClick={salvar} disabled={!form.clienteId || !form.dataHora} className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg disabled:opacity-40">Salvar</button>
        </div>
      </div>
    </div>
  )
}
