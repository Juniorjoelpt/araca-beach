import { useCallback, useEffect, useMemo, useState } from 'react'
import { modalidadeService } from '../services/modalidadeService.js'

/** Modalidades cadastradas (Quadras → Modalidades). `value` e o codigo gravado nos cadastros; `label` e o nome exibido. */
export function useModalidades() {
  const [lista, setLista] = useState([])

  const recarregar = useCallback(
    () => modalidadeService.listar().then((d) => { setLista(d); return d }).catch(() => []),
    []
  )

  useEffect(() => { recarregar() }, [recarregar])

  const modalidades = useMemo(() => lista.map((m) => ({ id: m.id, value: m.codigo, label: m.nome })), [lista])
  const rotulo = useCallback((codigo) => modalidades.find((m) => m.value === codigo)?.label ?? codigo ?? '', [modalidades])

  return { modalidades, rotulo, recarregar }
}
