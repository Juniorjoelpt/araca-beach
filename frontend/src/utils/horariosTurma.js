export const DIAS_TURMA = [
  { value: 'MONDAY', curto: 'Seg', label: 'Segunda' },
  { value: 'TUESDAY', curto: 'Ter', label: 'Terça' },
  { value: 'WEDNESDAY', curto: 'Qua', label: 'Quarta' },
  { value: 'THURSDAY', curto: 'Qui', label: 'Quinta' },
  { value: 'FRIDAY', curto: 'Sex', label: 'Sexta' },
  { value: 'SATURDAY', curto: 'Sáb', label: 'Sábado' },
  { value: 'SUNDAY', curto: 'Dom', label: 'Domingo' },
]

const ORDEM = Object.fromEntries(DIAS_TURMA.map((d, i) => [d.value, i]))
const hhmm = (h) => String(h).slice(0, 5)

/** Lista de horarios da turma ([{diaSemana,horaInicio,horaFim}]) -> blocos de formulario ([{dias,horaInicio,horaFim}]). */
export function horariosParaBlocos(horarios) {
  const mapa = new Map()
  for (const h of horarios || []) {
    const chave = `${hhmm(h.horaInicio)}-${hhmm(h.horaFim)}`
    if (!mapa.has(chave)) mapa.set(chave, { dias: [], horaInicio: hhmm(h.horaInicio), horaFim: hhmm(h.horaFim) })
    mapa.get(chave).dias.push(h.diaSemana)
  }
  const blocos = [...mapa.values()]
  blocos.forEach((b) => b.dias.sort((a, c) => ORDEM[a] - ORDEM[c]))
  blocos.sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
  return blocos
}

/** Blocos de formulario -> lista de horarios para a API (um por dia). */
export function blocosParaHorarios(blocos) {
  return blocos.flatMap((b) => b.dias.map((dia) => ({ diaSemana: dia, horaInicio: b.horaInicio, horaFim: b.horaFim })))
}

/** "Seg, Qua e Sex: 17:00–18:00, 18:00–19:00" (agrupa os dias que tem os mesmos horarios). */
export function resumoHorarios(horarios) {
  const porDia = new Map()
  for (const h of horarios || []) {
    if (!porDia.has(h.diaSemana)) porDia.set(h.diaSemana, [])
    porDia.get(h.diaSemana).push(`${hhmm(h.horaInicio)}–${hhmm(h.horaFim)}`)
  }
  const grupos = new Map()
  for (const [dia, faixas] of porDia) {
    faixas.sort()
    const chave = faixas.join(', ')
    if (!grupos.has(chave)) grupos.set(chave, [])
    grupos.get(chave).push(dia)
  }
  const nomes = (dias) => {
    const curtos = dias.sort((a, b) => ORDEM[a] - ORDEM[b]).map((d) => DIAS_TURMA.find((x) => x.value === d)?.curto ?? d)
    return curtos.length > 1 ? `${curtos.slice(0, -1).join(', ')} e ${curtos[curtos.length - 1]}` : curtos[0]
  }
  return [...grupos.entries()].map(([faixas, dias]) => `${nomes(dias)}: ${faixas}`).join(' · ')
}
