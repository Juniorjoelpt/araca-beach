import { Navigate } from 'react-router-dom'

/**
 * Restringe uma rota ao perfil ADMIN. Diferente do ProtectedRoute (que so
 * confere se ha login valido), este guarda tambem confere o perfil do
 * usuario - um Operador logado que tentar acessar a URL diretamente (ex:
 * digitando /financeiro na barra de enderecos) e redirecionado para o
 * Dashboard, sem nem chegar a ver a tela.
 *
 * Isso e so a camada de UX - a seguranca de verdade esta no backend
 * (SecurityConfig), que recusa esses mesmos endpoints para quem nao e ADMIN
 * independente do que o frontend mostra ou esconde.
 */
export default function AdminRoute({ children }) {
  const usuarioRaw = localStorage.getItem('araca_beach_usuario')
  const usuario = usuarioRaw ? JSON.parse(usuarioRaw) : null

  if (usuario?.perfil !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return children
}
