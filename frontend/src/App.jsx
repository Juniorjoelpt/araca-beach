import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import AdminRoute from './components/AdminRoute.jsx'
import PortalProtectedRoute from './components/PortalProtectedRoute.jsx'
import PortalLayout from './components/PortalLayout.jsx'
import Login from './pages/Login.jsx'
import Agenda from './pages/Agenda.jsx'
import Quadras from './pages/Quadras.jsx'
import Clientes from './pages/Clientes.jsx'
import Financeiro from './pages/Financeiro.jsx'
import Aulas from './pages/Aulas.jsx'
import Torneios from './pages/Torneios.jsx'
import Loja from './pages/Loja.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Usuarios from './pages/Usuarios.jsx'
import Despesas from './pages/Despesas.jsx'
import Mensalidades from './pages/Mensalidades.jsx'
import Relatorios from './pages/Relatorios.jsx'
import HistoricoCaixa from './pages/HistoricoCaixa.jsx'
import Estoque from './pages/Estoque.jsx'
import PortalCadastro from './pages/portal/PortalCadastro.jsx'
import PortalLogin from './pages/portal/PortalLogin.jsx'
import PortalAgendar from './pages/portal/PortalAgendar.jsx'
import PortalMinhasReservas from './pages/portal/PortalMinhasReservas.jsx'
import PortalPerfil from './pages/portal/PortalPerfil.jsx'
import PortalConfirmarEmail from './pages/portal/PortalConfirmarEmail.jsx'
import PortalEsqueciSenha from './pages/portal/PortalEsqueciSenha.jsx'
import PortalRedefinirSenha from './pages/portal/PortalRedefinirSenha.jsx'
import PortalAulas from './pages/portal/PortalAulas.jsx'
import PortalConta from './pages/portal/PortalConta.jsx'
import Pacotes from './pages/Pacotes.jsx'
import Regras from './pages/Regras.jsx'
import Comissoes from './pages/Comissoes.jsx'
import Gerencial from './pages/Gerencial.jsx'
import Lucro from './pages/Lucro.jsx'
import Garcom from './pages/Garcom.jsx'
import Restaurante from './pages/Restaurante.jsx'
import RestauranteReservas from './pages/RestauranteReservas.jsx'
import RestauranteGestao from './pages/RestauranteGestao.jsx'
import Auditoria from './pages/Auditoria.jsx'

export default function App() {
  return (
    <Routes>
      {/* Painel interno da equipe */}
      <Route path="/login" element={<Login />} />
      <Route path="/garcom" element={<ProtectedRoute><Garcom /></ProtectedRoute>} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/agenda" element={<Agenda />} />
        <Route path="/quadras" element={<Quadras />} />
        <Route path="/clientes" element={<Clientes />} />
        <Route path="/financeiro" element={<Financeiro />} />
        <Route path="/aulas" element={<Aulas />} />
        <Route path="/torneios" element={<Torneios />} />
        <Route path="/loja" element={<Loja />} />
        <Route path="/estoque" element={<AdminRoute><Estoque /></AdminRoute>} />
        <Route path="/despesas" element={<AdminRoute><Despesas /></AdminRoute>} />
        <Route path="/mensalidades" element={<Mensalidades />} />
        <Route path="/pacotes" element={<Pacotes />} />
        <Route path="/regras" element={<AdminRoute><Regras /></AdminRoute>} />
        <Route path="/comissoes" element={<AdminRoute><Comissoes /></AdminRoute>} />
        <Route path="/relatorios" element={<Relatorios />} />
        <Route path="/lucro" element={<AdminRoute><Lucro /></AdminRoute>} />
        <Route path="/caixa" element={<HistoricoCaixa />} />
        <Route path="/restaurante" element={<Restaurante />} />
        <Route path="/restaurante/reservas" element={<RestauranteReservas />} />
        <Route path="/restaurante/gestao" element={<AdminRoute><RestauranteGestao /></AdminRoute>} />
        <Route path="/gerencial" element={<AdminRoute><Gerencial /></AdminRoute>} />
        <Route path="/auditoria" element={<AdminRoute><Auditoria /></AdminRoute>} />
        <Route path="/usuarios" element={<AdminRoute><Usuarios /></AdminRoute>} />
      </Route>

      {/* Portal do cliente (mobile) */}
      <Route path="/portal/entrar" element={<PortalLogin />} />
      <Route path="/portal/cadastro" element={<PortalCadastro />} />
      <Route path="/portal/confirmar-email" element={<PortalConfirmarEmail />} />
      <Route path="/portal/esqueci-senha" element={<PortalEsqueciSenha />} />
      <Route path="/portal/redefinir-senha" element={<PortalRedefinirSenha />} />
      <Route
        element={
          <PortalProtectedRoute>
            <PortalLayout />
          </PortalProtectedRoute>
        }
      >
        <Route path="/portal" element={<PortalAgendar />} />
        <Route path="/portal/minhas-reservas" element={<PortalMinhasReservas />} />
        <Route path="/portal/aulas" element={<PortalAulas />} />
        <Route path="/portal/conta" element={<PortalConta />} />
        <Route path="/portal/perfil" element={<PortalPerfil />} />
      </Route>
    </Routes>
  )
}
