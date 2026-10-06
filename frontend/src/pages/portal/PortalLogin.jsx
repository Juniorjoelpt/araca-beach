import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { portalAuthService } from '../../services/portalAuthService.js'
import logoIcone from '../../assets/logos/icone-256.png'

export default function PortalLogin() {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [emailPendente, setEmailPendente] = useState(false)
  const [reenviado, setReenviado] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      await portalAuthService.login(email, senha)
      navigate('/portal')
    } catch (err) {
      if (err.response?.status === 403) {
        setEmailPendente(true)
        setErro(err.response?.data?.mensagem || 'Confirme seu e-mail para entrar.')
      } else {
        setEmailPendente(false)
        setErro('E-mail ou senha inválidos.')
      }
    } finally {
      setCarregando(false)
    }
  }

  async function reenviar() {
    try {
      await portalAuthService.reenviarConfirmacao(email)
      setReenviado(true)
    } catch {
      setErro('Não foi possível reenviar agora.')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
      <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm">
        <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
        <h1 className="font-title text-2xl text-araca-verde-escuro text-center mb-1">ARAÇA BEACH</h1>
        <p className="text-center text-gray-500 text-sm mb-6">Agende sua quadra pelo celular</p>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">E-mail</label>
            <input
              type="email"
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              type="password"
              className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </div>

          {erro && <p className="text-red-600 text-sm">{erro}</p>}
          {emailPendente && (
            <div className="text-sm">
              {reenviado
                ? <p className="text-green-700">Link reenviado. Confira seu e-mail (e o spam).</p>
                : <button type="button" onClick={reenviar} className="text-araca-verde-escuro font-medium hover:underline">Reenviar e-mail de confirmação</button>}
            </div>
          )}

          <button
            type="submit"
            disabled={carregando}
            className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 disabled:opacity-50 transition"
          >
            {carregando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          <Link to="/portal/esqueci-senha" className="text-araca-verde-escuro font-medium hover:underline">Esqueci minha senha</Link>
        </p>
        <p className="text-center text-sm text-gray-500 mt-4">
          Ainda não tem conta?{' '}
          <Link to="/portal/cadastro" className="text-araca-verde-escuro font-medium hover:underline">Criar conta</Link>
        </p>
      </div>
    </div>
  )
}
