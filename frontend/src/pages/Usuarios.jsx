import { useEffect, useState } from 'react'
import { usuarioService } from '../services/usuarioService.js'
import { UserCog } from 'lucide-react'

const PERFIS = [
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'RECEPCAO', label: 'Operador' },
]

const vazio = { nome: '', login: '', senha: '', perfil: 'RECEPCAO' }

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([])
  const [form, setForm] = useState(vazio)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [senhaParaTrocar, setSenhaParaTrocar] = useState(null)
  const [novaSenha, setNovaSenha] = useState('')

  async function carregar() {
    setCarregando(true)
    try {
      const dados = await usuarioService.listar()
      setUsuarios(dados)
    } catch {
      setErro('Não foi possível carregar os usuários. Verifique se você está logado como administrador.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  async function handleCriar(e) {
    e.preventDefault()
    setErro('')
    setSucesso('')
    try {
      await usuarioService.criar(form)
      setForm(vazio)
      setSucesso('Usuário criado com sucesso.')
      carregar()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar o usuário.')
    }
  }

  async function handleAlternarStatus(usuario) {
    try {
      await usuarioService.alterarStatus(usuario.id, !usuario.ativo)
      carregar()
    } catch {
      setErro('Não foi possível alterar o status do usuário.')
    }
  }

  async function handleTrocarSenha(e) {
    e.preventDefault()
    setErro('')
    try {
      await usuarioService.trocarSenha(senhaParaTrocar.id, novaSenha)
      setSenhaParaTrocar(null)
      setNovaSenha('')
      setSucesso('Senha atualizada com sucesso.')
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível trocar a senha.')
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <UserCog size={22} className="text-araca-verde-escuro" />
        Usuários
      </h2>

      <form onSubmit={handleCriar} className="bg-white rounded-xl shadow p-6 mb-6 grid grid-cols-1 md:grid-cols-5 gap-4 items-end">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Login</label>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.login}
            onChange={(e) => setForm({ ...form, login: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Senha</label>
          <input
            type="password"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.senha}
            onChange={(e) => setForm({ ...form, senha: e.target.value })}
            minLength={6}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Perfil</label>
          <select
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={form.perfil}
            onChange={(e) => setForm({ ...form, perfil: e.target.value })}
          >
            {PERFIS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
          </select>
        </div>
        <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition h-fit">
          Adicionar usuário
        </button>
      </form>

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}
      {sucesso && <p className="text-green-700 text-sm mb-4">{sucesso}</p>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 text-left">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">Login</th>
              <th className="px-4 py-3">Perfil</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && <tr><td className="px-4 py-4 text-gray-400" colSpan={5}>Carregando...</td></tr>}
            {!carregando && usuarios.length === 0 && (
              <tr><td className="px-4 py-4 text-gray-400" colSpan={5}>Nenhum usuário cadastrado.</td></tr>
            )}
            {usuarios.map((u) => (
              <tr key={u.id} className="border-t">
                <td className="px-4 py-3 font-medium text-araca-azul">{u.nome}</td>
                <td className="px-4 py-3">{u.login}</td>
                <td className="px-4 py-3">{PERFIS.find((p) => p.value === u.perfil)?.label}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${u.ativo ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                    {u.ativo ? 'Ativo' : 'Inativo'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right space-x-3">
                  <button
                    onClick={() => { setSenhaParaTrocar(u); setNovaSenha('') }}
                    className="text-araca-verde-escuro hover:underline"
                  >
                    Trocar senha
                  </button>
                  <button
                    onClick={() => handleAlternarStatus(u)}
                    className="text-red-600 hover:underline"
                  >
                    {u.ativo ? 'Desativar' : 'Ativar'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {senhaParaTrocar && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <form onSubmit={handleTrocarSenha} className="bg-white rounded-xl shadow-xl p-6 w-full max-w-sm space-y-4">
            <h3 className="font-semibold text-araca-azul">Trocar senha — {senhaParaTrocar.nome}</h3>
            <input
              type="password"
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="Nova senha (mín. 6 caracteres)"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              minLength={6}
              required
            />
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setSenhaParaTrocar(null)}
                className="flex-1 border rounded-lg py-2 text-gray-600 hover:bg-gray-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="flex-1 bg-araca-verde text-araca-azul font-semibold rounded-lg py-2 hover:opacity-90"
              >
                Confirmar
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
