import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  LayoutDashboard, CalendarDays, MapPin, Users, Wallet,
  GraduationCap, Trophy, ShoppingBag, UserCog, LogOut, Receipt, FileText, Boxes, CalendarCheck2, Package, SlidersHorizontal, Percent, ShieldCheck, LineChart, UtensilsCrossed, CalendarClock, ChefHat, History, TrendingUp,
} from 'lucide-react'
import { authService } from '../services/authService.js'
import logoIcone from '../assets/logos/icone-256.png'
import logoMarcaDagua from '../assets/logos/logo-transparente.png'

const PERFIL_LABEL = { ADMIN: 'Administrador', RECEPCAO: 'Operador', GARCOM: 'Garçom' }

const menuBase = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/agenda', label: 'Agenda / Reservas', icon: CalendarDays },
  { to: '/quadras', label: 'Quadras', icon: MapPin },
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/financeiro', label: 'Financeiro', icon: Wallet },
  { to: '/despesas', label: 'Despesas', icon: Receipt, soAdmin: true },
  { to: '/mensalidades', label: 'Mensalidades', icon: CalendarCheck2 },
  { to: '/pacotes', label: 'Pacotes de aulas', icon: Package },
  { to: '/relatorios', label: 'Relatórios', icon: FileText },
  { to: '/lucro', label: 'Lucro das vendas', icon: TrendingUp, soAdmin: true },
  { to: '/caixa', label: 'Histórico de caixa', icon: History },
  { to: '/aulas', label: 'Aulas', icon: GraduationCap },
  { to: '/torneios', label: 'Torneios', icon: Trophy },
  { to: '/restaurante', label: 'Restaurante', icon: UtensilsCrossed },
  { to: '/restaurante/reservas', label: 'Reservas de mesa', icon: CalendarClock },
  { to: '/restaurante/gestao', label: 'Gestão do restaurante', icon: ChefHat, soAdmin: true },
  { to: '/loja', label: 'Loja', icon: ShoppingBag },
  { to: '/estoque', label: 'Estoque', icon: Boxes, soAdmin: true },
  { to: '/comissoes', label: 'Comissões', icon: Percent, soAdmin: true },
  { to: '/regras', label: 'Regras de reserva', icon: SlidersHorizontal, soAdmin: true },
  { to: '/gerencial', label: 'Painel gerencial', icon: LineChart, soAdmin: true },
  { to: '/auditoria', label: 'Auditoria', icon: ShieldCheck, soAdmin: true },
  { to: '/usuarios', label: 'Usuários', icon: UserCog, soAdmin: true },
]

export default function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const usuarioRaw = localStorage.getItem('araca_beach_usuario')
  const usuario = usuarioRaw ? JSON.parse(usuarioRaw) : null

  const menu = menuBase.filter((item) => !item.soAdmin || usuario?.perfil === 'ADMIN')

  // Item ativo = o de rota mais especifica que casa com a URL (ex.: /restaurante/reservas
  // vence /restaurante, que tambem e prefixo dela).
  const ativoIndex = menu.reduce((melhor, item, i) => {
    const casa = item.to === '/'
      ? location.pathname === '/'
      : location.pathname === item.to || location.pathname.startsWith(`${item.to}/`)
    if (!casa) return melhor
    return melhor === -1 || item.to.length > menu[melhor].to.length ? i : melhor
  }, -1)

  function handleLogout() {
    authService.logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex">
      <aside className="relative w-64 h-screen sticky top-0 bg-oceano-gradient text-white flex flex-col overflow-hidden">
        {/* Textura ambiente: padrao de bolinhas extraido do logo, bem sutil */}
        <div className="absolute inset-0 textura-pontilhada text-white opacity-[0.05] pointer-events-none" />

        <div className="relative z-10 p-6 border-b border-white/10 flex items-center gap-3">
          <img src={logoIcone} alt="Araça Beach" className="w-11 h-11 rounded-full shadow-glow" />
          <div>
            <h1 className="font-title text-xl tracking-wide leading-none text-white">
              ARAÇA <span className="text-araca-verde">BEACH</span>
            </h1>
            <p className="text-[11px] text-white/50 mt-1">Painel de gestão</p>
          </div>
        </div>

        <nav className="relative z-10 flex-1 p-4 space-y-1 overflow-y-auto">
          {menu.map((item, index) => {
            const Icon = item.icon
            const ativo = index === ativoIndex
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className="relative flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm transition-colors"
              >
                {ativo && (
                  <motion.div
                    layoutId="nav-ativo"
                    className="absolute inset-0 bg-araca-verde rounded-xl"
                    transition={{ type: 'spring', stiffness: 400, damping: 32 }}
                  />
                )}
                <Icon size={18} className={`relative z-10 ${ativo ? 'text-araca-azul' : 'text-white/70'}`} />
                <span className={`relative z-10 ${ativo ? 'text-araca-azul font-semibold' : 'text-white/80'}`}>
                  {item.label}
                </span>
              </NavLink>
            )
          })}
        </nav>

        <div className="relative z-10 p-4 border-t border-white/10">
          {usuario && (
            <p className="text-xs text-white/50 mb-2">
              {usuario.nome} · {PERFIL_LABEL[usuario.perfil] || usuario.perfil}
            </p>
          )}
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 text-sm text-white/80 hover:text-araca-verde transition-colors"
          >
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 p-8 bg-araca-azul relative overflow-hidden">
        {/* Marca d'agua: logo grande, fixa e discreta ao fundo de todas as paginas */}
        <img
          src={logoMarcaDagua}
          alt=""
          aria-hidden="true"
          className="pointer-events-none select-none fixed bottom-0 right-0 z-0 w-[560px] max-w-[55vw] opacity-[0.10] -mb-10 -mr-10"
        />
        <div className="relative z-10">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </div>
      </main>
    </div>
  )
}
