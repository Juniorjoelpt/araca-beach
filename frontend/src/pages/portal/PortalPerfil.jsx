import { useEffect, useState } from 'react'
import { portalAuthService } from '../../services/portalAuthService.js'

const input = 'w-full border rounded-lg px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-araca-verde'

export default function PortalPerfil() {
  const [perfil, setPerfil] = useState({ nome: '', email: '', telefone: '' })
  const [senha, setSenha] = useState({ senhaAtual: '', novaSenha: '', confirmar: '' })
  const [msgPerfil, setMsgPerfil] = useState({ tipo: '', texto: '' })
  const [msgSenha, setMsgSenha] = useState({ tipo: '', texto: '' })
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    portalAuthService.perfil()
      .then(setPerfil)
      .catch(() => setMsgPerfil({ tipo: 'erro', texto: 'Não foi possível carregar seus dados.' }))
  }, [])

  async function salvarPerfil(e) {
    e.preventDefault()
    setMsgPerfil({ tipo: '', texto: '' })
    setSalvando(true)
    try {
      const atualizado = await portalAuthService.atualizarPerfil(perfil.nome, perfil.telefone)
      setPerfil(atualizado)
      setMsgPerfil({ tipo: 'ok', texto: 'Dados atualizados.' })
    } catch (err) {
      setMsgPerfil({ tipo: 'erro', texto: err.response?.data?.mensagem || 'Não foi possível salvar.' })
    } finally {
      setSalvando(false)
    }
  }

  async function trocarSenha(e) {
    e.preventDefault()
    setMsgSenha({ tipo: '', texto: '' })
    if (senha.novaSenha !== senha.confirmar) {
      setMsgSenha({ tipo: 'erro', texto: 'A confirmação não confere com a nova senha.' })
      return
    }
    try {
      await portalAuthService.alterarSenha(senha.senhaAtual, senha.novaSenha)
      setSenha({ senhaAtual: '', novaSenha: '', confirmar: '' })
      setMsgSenha({ tipo: 'ok', texto: 'Senha alterada.' })
    } catch (err) {
      setMsgSenha({ tipo: 'erro', texto: err.response?.data?.mensagem || 'Não foi possível alterar a senha.' })
    }
  }

  const cor = (m) => (m.tipo === 'ok' ? 'text-green-700' : 'text-red-600')

  return (
    <div className="space-y-6">
      <h1 className="font-title text-xl text-araca-azul">Meu perfil</h1>

      <form onSubmit={salvarPerfil} className="bg-white rounded-xl shadow p-4 space-y-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Nome completo</label>
          <input className={input} value={perfil.nome} onChange={(e) => setPerfil({ ...perfil, nome: e.target.value })} required />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">E-mail</label>
          <input className={`${input} bg-gray-50 text-gray-500`} value={perfil.email || ''} disabled />
          <p className="text-xs text-gray-400 mt-1">Para trocar o e-mail, fale com a recepção.</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
          <input className={input} value={perfil.telefone || ''} onChange={(e) => setPerfil({ ...perfil, telefone: e.target.value })} required />
        </div>
        {msgPerfil.texto && <p className={`text-sm ${cor(msgPerfil)}`}>{msgPerfil.texto}</p>}
        <button type="submit" disabled={salvando} className="w-full bg-araca-verde text-araca-azul font-semibold py-3 rounded-lg hover:opacity-90 disabled:opacity-50 transition">
          Salvar dados
        </button>
      </form>

      <form onSubmit={trocarSenha} className="bg-white rounded-xl shadow p-4 space-y-3">
        <h2 className="font-semibold text-araca-azul">Alterar senha</h2>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Senha atual</label>
          <input type="password" className={input} value={senha.senhaAtual} onChange={(e) => setSenha({ ...senha, senhaAtual: e.target.value })} required />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Nova senha</label>
          <input type="password" minLength={6} className={input} value={senha.novaSenha} onChange={(e) => setSenha({ ...senha, novaSenha: e.target.value })} required />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar nova senha</label>
          <input type="password" minLength={6} className={input} value={senha.confirmar} onChange={(e) => setSenha({ ...senha, confirmar: e.target.value })} required />
        </div>
        {msgSenha.texto && <p className={`text-sm ${cor(msgSenha)}`}>{msgSenha.texto}</p>}
        <button type="submit" className="w-full bg-araca-azul text-white font-semibold py-3 rounded-lg hover:opacity-90 transition">
          Alterar senha
        </button>
      </form>
    </div>
  )
}
