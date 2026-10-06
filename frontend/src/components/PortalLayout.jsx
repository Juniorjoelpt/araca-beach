import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { CalendarPlus, CalendarCheck, GraduationCap, Wallet, LogOut } from 'lucide-react'
import { portalAuthService } from '../services/portalAuthService.js'
import logoIcone from '../assets/logos/icone-256.png'

const abas = [
  { to: '/portal', label: 'Agendar', icon: CalendarPlus, end: true },
  { to: '/portal/minhas-reservas', label: 'Reservas', icon: CalendarCheck },
  { to: '/portal/aulas', label: 'Aulas', icon: GraduationCap },
  { to: '/portal/conta', label: 'Conta', icon: Wallet },
]

export default function PortalLayout() {
  const navigate = useNavigate()
  const cliente = portalAuthService.dadosCliente()

  function handleSair() {
    portalAuthService.logout()
    navigate('/portal/entrar')
  }

  return (
    <div className="min-h-screen flex flex-col bg-araca-areia">
      {/* Cabecalho fixo no topo */}
      <header className="bg-araca-azul text-white px-4 py-3 flex items-center justify-between sticky top-0 z-10 shadow-md">
        <div className="flex items-center gap-2">
          <img src={logoIcone} alt="Araça Beach" className="w-9 h-9 rounded-full" />
          <div>
            <p className="font-title text-sm leading-none">ARAÇA <span className="text-araca-verde">BEACH</span></p>
            {cliente && <p className="text-[11px] text-white/60 mt-0.5">Olá, {cliente.nome.split(' ')[0]}</p>}
          </div>
        </div>
        <button onClick={handleSair} aria-label="Sair" className="text-white/70 hover:text-araca-verde transition-colors">
          <LogOut size={20} />
        </button>
      </header>

      {/* Conteudo da pagina */}
      <main className="flex-1 p-4 pb-24 max-w-lg mx-auto w-full">
        <Outlet />
      </main>

      {/* Navegacao inferior fixa (padrao de app mobile) */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t flex z-10">
        {abas.map((aba) => {
          const Icon = aba.icon
          return (
            <NavLink
              key={aba.to}
              to={aba.to}
              end={aba.end}
              className={({ isActive }) =>
                `flex-1 flex flex-col items-center gap-1 py-2.5 text-xs font-medium transition-colors ${
                  isActive ? 'text-araca-verde-escuro' : 'text-gray-400'
                }`
              }
            >
              <Icon size={22} />
              {aba.label}
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}
