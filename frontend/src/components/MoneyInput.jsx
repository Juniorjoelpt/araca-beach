import { useEffect, useRef } from 'react'

// aceita numero, "1234.56" ou "1.234,56"
const paraNumero = (valor) => {
  if (valor === '' || valor === null || valor === undefined) return null
  if (typeof valor === 'string' && valor.includes(',')) return Number(valor.replace(/\./g, '').replace(',', '.'))
  return Number(valor)
}

const fmt = (valor) => {
  const n = paraNumero(valor)
  if (n === null || Number.isNaN(n)) return ''
  return (Math.round(n * 100) / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * Campo de valor em reais com mascara (1.234,56): digita-se so os numeros e os centavos entram
 * da direita para a esquerda. Mantem a mesma interface de um <input>: `value` e um numero/texto
 * decimal ("1234.56") e `onChange` recebe um objeto com `target.value` no mesmo formato
 * (ou '' quando vazio), entao funciona no lugar de <input type="number" step="0.01">.
 */
export default function MoneyInput({ value, onChange, min, className = '', placeholder = '0,00', required, disabled, id, ...resto }) {
  const ref = useRef(null)
  const num = paraNumero(value)

  useEffect(() => {
    if (!ref.current) return
    const abaixo = min !== undefined && num !== null && !Number.isNaN(num) && num < Number(min)
    ref.current.setCustomValidity(abaixo ? `O valor deve ser pelo menos ${fmt(min)}` : '')
  }, [num, min])

  const emitir = (decimal) => onChange?.({ target: { value: decimal } })

  function aoDigitar(e) {
    const digitos = e.target.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '').slice(0, 11)
    emitir(digitos === '' ? '' : (parseInt(digitos, 10) / 100).toFixed(2))
  }

  function aoColar(e) {
    const texto = (e.clipboardData?.getData('text') || '').trim().replace(/^R\$\s*/i, '')
    let n = null
    if (/^\d{1,3}(\.\d{3})*,\d{1,2}$|^\d+,\d{1,2}$/.test(texto)) n = Number(texto.replace(/\./g, '').replace(',', '.'))
    else if (/^\d+(\.\d{1,2})?$/.test(texto)) n = Number(texto)
    if (n !== null && !Number.isNaN(n)) {
      e.preventDefault()
      emitir(n.toFixed(2))
    }
  }

  return (
    <input
      ref={ref}
      id={id}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      className={className}
      placeholder={placeholder}
      value={fmt(value)}
      onChange={aoDigitar}
      onPaste={aoColar}
      required={required}
      disabled={disabled}
      {...resto}
    />
  )
}
