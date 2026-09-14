import { Navigate } from 'react-router-dom'
import { authService } from '../services/authService.js'

export default function ProtectedRoute({ children }) {
  if (!authService.estaAutenticado()) {
    return <Navigate to="/login" replace />
  }
  return children
}
