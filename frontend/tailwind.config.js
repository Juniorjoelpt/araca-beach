/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Paleta oficial do Manual de Marca Araca Beach
        'araca-azul': '#061268',        // azul marinho - cor primaria
        'araca-verde': '#B7E90C',       // verde limao - cor de destaque/acao
        'araca-azul-claro': '#4B58BE',
        'araca-verde-claro': '#A9E882',
        'araca-verde-escuro': '#2C6906',
        // Complementos "praia" derivados da paleta oficial, para dar
        // aconchego ao fundo geral sem fugir da identidade da marca
        'araca-areia': '#FBF6EC',
        'araca-areia-escura': '#F1E8D6',
        'araca-oceano': '#0B1C7A',
        'araca-oceano-profundo': '#040B3D',
        'araca-ceu': '#C9CDE2',        // fundo das paginas: azul do sidebar, varios tons mais claro
      },
      fontFamily: {
        title: ['"Baloo 2"', '"Ancorli"', 'sans-serif'],  // titulos e campanhas
        body: ['Poppins', 'sans-serif'],                   // texto corrido
      },
      boxShadow: {
        // sombra neutra: precisa se destacar contra o fundo azul-marinho das paginas
        DEFAULT: '0 2px 10px -2px rgba(0, 0, 0, 0.22), 0 1px 3px -1px rgba(0, 0, 0, 0.14)',
        md: '0 4px 16px -4px rgba(0, 0, 0, 0.26), 0 2px 6px -2px rgba(0, 0, 0, 0.15)',
        lg: '0 12px 32px -8px rgba(0, 0, 0, 0.32), 0 4px 12px -4px rgba(0, 0, 0, 0.18)',
        glow: '0 0 0 1px rgba(183, 233, 12, 0.4), 0 0 24px -4px rgba(183, 233, 12, 0.35)',
      },
      backgroundImage: {
        // textura de bolinhas inspirada no padrao pontilhado da raquete no logo
        'dot-grid': 'radial-gradient(currentColor 1.5px, transparent 1.5px)',
        'oceano-gradient': 'linear-gradient(160deg, #0B1C7A 0%, #061268 55%, #040B3D 100%)',
      },
      backgroundSize: {
        'dot-grid': '22px 22px',
      },
      keyframes: {
        'fade-in-up': {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'wave-drift': {
          '0%': { transform: 'translateX(0)' },
          '100%': { transform: 'translateX(-50%)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.5s cubic-bezier(0.16, 1, 0.3, 1) both',
        'wave-drift': 'wave-drift 18s linear infinite',
        shimmer: 'shimmer 1.6s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
