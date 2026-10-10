import { Navigate, useLocation } from 'react-router-dom'
import { authService } from '../services/authService.js'

export default function ProtectedRoute({ children }) {
  const location = useLocation()
  if (!authService.estaAutenticado()) {
    return <Navigate to="/login" replace />
  }
  // Garcom so usa a tela /garcom (o backend tambem bloqueia o resto).
  let perfil = null
  try { perfil = JSON.parse(localStorage.getItem('araca_beach_usuario'))?.perfil } catch { /* ignora */ }
  if (perfil === 'GARCOM' && !location.pathname.startsWith('/garcom')) {
    return <Navigate to="/garcom" replace />
  }
  return children
}
