import { Navigate } from 'react-router-dom'
import { portalAuthService } from '../services/portalAuthService.js'

export default function PortalProtectedRoute({ children }) {
  if (!portalAuthService.estaAutenticado()) {
    return <Navigate to="/portal/entrar" replace />
  }
  return children
}
