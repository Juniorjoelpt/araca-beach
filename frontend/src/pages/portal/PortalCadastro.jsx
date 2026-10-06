import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { portalAuthService } from '../../services/portalAuthService.js'
import logoIcone from '../../assets/logos/icone-256.png'

export default function PortalCadastro() {
  const [form, setForm] = useState({ nome: '', email: '', telefone: '', senha: '' })
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [pendente, setPendente] = useState(false)
  const [reenviado, setReenviado] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      const resp = await portalAuthService.registrar(form.nome, form.email, form.telefone, form.senha)
      if (resp.confirmacaoPendente) {
        setPendente(true)
      } else {
        navigate('/portal')
      }
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar sua conta.')
    } finally {
      setCarregando(false)
    }
  }

  async function reenviar() {
    try {
      await portalAuthService.reenviarConfirmacao(form.email)
      setReenviado(true)
    } catch {
      setErro('Não foi possível reenviar agora. Tente novamente em instantes.')
    }
  }

  if (pendente) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
        <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm text-center">
          <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
          <h1 className="font-title text-2xl text-araca-verde-escuro mb-2">Confirme seu e-mail</h1>
          <p className="text-gray-600 text-sm mb-4">
            Enviamos um link para <strong>{form.email}</strong>. Clique nele para ativar sua conta (vale por 24 horas). Confira também o spam.
          </p>
          {reenviado && <p className="text-green-700 text-sm mb-2">Link reenviado.</p>}
          {erro && <p className="text-red-600 text-sm mb-2">{erro}</p>}
          <button onClick={reenviar} className="text-araca-verde-escuro font-medium text-sm hover:underline">Reenviar e-mail</button>
          <p className="text-sm text-gray-500 mt-4">
            <Link to="/portal/entrar" className="text-araca-verde-escuro font-medium hover:underline">Ir para o login</Link>
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
      <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm">
        <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
        <h1 className="font-title text-2xl text-araca-verde-escuro text-center mb-1">Criar conta</h1>
        <p className="text-center text-gray-500 text-sm mb-6">Reserve sua quadra pelo celular</p>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nome completo</label>
            <input
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">E-mail</label>
            <input
              type="email"
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
            <input
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="(00) 00000-0000"
              value={form.telefone}
              onChange={(e) => setForm({ ...form, telefone: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              type="password"
              minLength={6}
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={form.senha}
              onChange={(e) => setForm({ ...form, senha: e.target.value })}
              required
            />
            <p className="text-xs text-gray-400 mt-1">Mínimo de 6 caracteres.</p>
          </div>

          {erro && <p className="text-red-600 text-sm">{erro}</p>}

          <button
            type="submit"
            disabled={carregando}
            className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 disabled:opacity-50 transition"
          >
            {carregando ? 'Criando conta...' : 'Criar conta'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          Já tem conta?{' '}
          <Link to="/portal/entrar" className="text-araca-verde-escuro font-medium hover:underline">Entrar</Link>
        </p>
      </div>
    </div>
  )
}
