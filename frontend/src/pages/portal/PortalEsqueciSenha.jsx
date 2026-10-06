import { useState } from 'react'
import { Link } from 'react-router-dom'
import { portalAuthService } from '../../services/portalAuthService.js'
import logoIcone from '../../assets/logos/icone-256.png'

export default function PortalEsqueciSenha() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      await portalAuthService.esqueciSenha(email)
      setEnviado(true)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível enviar o e-mail agora.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
      <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm">
        <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
        <h1 className="font-title text-2xl text-araca-verde-escuro text-center mb-1">Recuperar senha</h1>

        {enviado ? (
          <p className="text-center text-gray-600 text-sm my-6">
            Se existir uma conta com esse e-mail, enviamos um link para redefinir a senha (vale por 1 hora). Confira também o spam.
          </p>
        ) : (
          <>
            <p className="text-center text-gray-500 text-sm mb-6">Informe o e-mail da sua conta</p>
            <form onSubmit={handleSubmit} className="space-y-3">
              <input
                type="email"
                className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu@email.com"
                required
              />
              {erro && <p className="text-red-600 text-sm">{erro}</p>}
              <button type="submit" disabled={carregando} className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 disabled:opacity-50 transition">
                {carregando ? 'Enviando...' : 'Enviar link'}
              </button>
            </form>
          </>
        )}

        <p className="text-center text-sm text-gray-500 mt-4">
          <Link to="/portal/entrar" className="text-araca-verde-escuro font-medium hover:underline">Voltar ao login</Link>
        </p>
      </div>
    </div>
  )
}
