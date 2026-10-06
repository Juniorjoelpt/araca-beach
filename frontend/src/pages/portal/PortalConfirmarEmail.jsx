import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { portalAuthService } from '../../services/portalAuthService.js'
import logoIcone from '../../assets/logos/icone-256.png'

export default function PortalConfirmarEmail() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [erro, setErro] = useState('')
  const navigate = useNavigate()
  const jaTentou = useRef(false)

  useEffect(() => {
    if (jaTentou.current) return // o StrictMode executa o efeito 2x em dev; o token so vale uma vez
    jaTentou.current = true
    if (!token) {
      setErro('Link inválido.')
      return
    }
    portalAuthService.confirmarEmail(token)
      .then(() => navigate('/portal', { replace: true }))
      .catch((err) => setErro(err.response?.data?.mensagem || 'Não foi possível confirmar o e-mail.'))
  }, [token, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
      <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm text-center">
        <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
        {erro ? (
          <>
            <p className="text-red-600 text-sm mb-4">{erro}</p>
            <Link to="/portal/entrar" className="text-araca-verde-escuro font-medium hover:underline">Ir para o login</Link>
          </>
        ) : (
          <p className="text-gray-600 text-sm">Confirmando seu e-mail...</p>
        )}
      </div>
    </div>
  )
}
