import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { authService } from '../services/authService.js'
import logoIcone from '../assets/logos/icone-256.png'

export default function Login() {
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    try {
      await authService.login(login, senha)
      navigate('/')
    } catch {
      setErro('Login ou senha inválidos.')
    }
  }

  return (
    <div className="min-h-screen relative flex items-center justify-center bg-oceano-gradient overflow-hidden">
      {/* Textura ambiente de bolinhas, ecoando o padrao da raquete no logo */}
      <div className="absolute inset-0 textura-pontilhada text-white opacity-[0.06] pointer-events-none" />

      {/* "Ondas" decorativas ao fundo, em degrade lima, remetendo a arena de praia */}
      <div className="absolute -bottom-24 left-0 w-[200%] h-64 opacity-20 animate-wave-drift" aria-hidden="true">
        <svg viewBox="0 0 1440 220" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
          <path
            fill="#B7E90C"
            d="M0,110 C240,180 480,40 720,110 C960,180 1200,40 1440,110 L1440,220 L0,220 Z"
          />
        </svg>
      </div>
      <div className="absolute -bottom-16 left-0 w-[200%] h-56 opacity-10 animate-wave-drift" style={{ animationDuration: '26s', animationDirection: 'reverse' }} aria-hidden="true">
        <svg viewBox="0 0 1440 220" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
          <path
            fill="#4B58BE"
            d="M0,140 C240,60 480,190 720,120 C960,50 1200,170 1440,100 L1440,220 L0,220 Z"
          />
        </svg>
      </div>

      <motion.form
        onSubmit={handleSubmit}
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 bg-white/95 backdrop-blur rounded-2xl shadow-lg p-10 w-full max-w-sm"
      >
        <motion.img
          src={logoIcone}
          alt="Araça Beach"
          initial={{ scale: 0.7, opacity: 0, rotate: -8 }}
          animate={{ scale: 1, opacity: 1, rotate: 0 }}
          transition={{ duration: 0.6, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
          className="w-16 h-16 rounded-full mx-auto mb-4 shadow-glow"
        />
        <h1 className="font-title text-3xl text-araca-verde-escuro text-center mb-1">
          ARAÇA <span className="text-araca-verde-escuro">BEACH</span>
        </h1>
        <p className="text-center text-gray-500 text-sm mb-6">Painel da equipe</p>

        <label className="block text-sm font-medium text-gray-700 mb-1">Login</label>
        <input
          className="w-full border rounded-lg px-3 py-2 mb-4 focus:outline-none focus:ring-2 focus:ring-araca-verde transition-shadow"
          value={login}
          onChange={(e) => setLogin(e.target.value)}
          required
        />

        <label className="block text-sm font-medium text-gray-700 mb-1">Senha</label>
        <input
          type="password"
          className="w-full border rounded-lg px-3 py-2 mb-4 focus:outline-none focus:ring-2 focus:ring-araca-verde transition-shadow"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          required
        />

        {erro && (
          <motion.p
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-red-600 text-sm mb-4"
          >
            {erro}
          </motion.p>
        )}

        <motion.button
          type="submit"
          whileTap={{ scale: 0.97 }}
          whileHover={{ scale: 1.01 }}
          className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg transition-shadow hover:shadow-glow"
        >
          Entrar
        </motion.button>
      </motion.form>
    </div>
  )
}
