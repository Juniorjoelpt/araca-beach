import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { portalAuthService } from '../../services/portalAuthService.js'
import logoIcone from '../../assets/logos/icone-256.png'

export default function PortalRedefinirSenha() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [concluido, setConcluido] = useState(false)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    if (novaSenha !== confirmar) {
      setErro('A confirmação não confere com a nova senha.')
      return
    }
    setCarregando(true)
    try {
      await portalAuthService.redefinirSenha(token, novaSenha)
      setConcluido(true)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível redefinir a senha.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-oceano-gradient p-4">
      <div className="bg-white rounded-2xl shadow-lg p-6 w-full max-w-sm">
        <img src={logoIcone} alt="Araça Beach" className="w-16 h-16 rounded-full mx-auto mb-3" />
        <h1 className="font-title text-2xl text-araca-verde-escuro text-center mb-4">Nova senha</h1>

        {concluido ? (
          <>
            <p className="text-center text-green-700 text-sm mb-4">Senha redefinida! Já pode entrar com a nova senha.</p>
            <Link to="/portal/entrar" className="block text-center w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg">Entrar</Link>
          </>
        ) : !token ? (
          <p className="text-center text-red-600 text-sm">Link inválido. Peça um novo em "Esqueci minha senha".</p>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3">
            <input type="password" minLength={6} placeholder="Nova senha (mín. 6 caracteres)" className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde" value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} required />
            <input type="password" minLength={6} placeholder="Confirmar nova senha" className="w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde" value={confirmar} onChange={(e) => setConfirmar(e.target.value)} required />
            {erro && <p className="text-red-600 text-sm">{erro}</p>}
            <button type="submit" disabled={carregando} className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 disabled:opacity-50 transition">
              {carregando ? 'Salvando...' : 'Salvar nova senha'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
