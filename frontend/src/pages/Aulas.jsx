import { useEffect, useRef, useState } from 'react'
import { format } from 'date-fns'
import { professorService } from '../services/professorService.js'
import { quadraService } from '../services/quadraService.js'
import { aulaService } from '../services/aulaService.js'
import { turmaService, matriculaService } from '../services/turmaService.js'
import { clienteService } from '../services/clienteService.js'
import { GraduationCap, Users2 } from 'lucide-react'
import { useModalidades } from '../utils/useModalidades.js'
import { blocosParaHorarios, horariosParaBlocos, resumoHorarios } from '../utils/horariosTurma.js'

const DIAS_SEMANA = [
  { value: 'MONDAY', label: 'Segunda' },
  { value: 'TUESDAY', label: 'Terça' },
  { value: 'WEDNESDAY', label: 'Quarta' },
  { value: 'THURSDAY', label: 'Quinta' },
  { value: 'FRIDAY', label: 'Sexta' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
]

const TIPOS_AULA = [
  { value: 'PARTICULAR', label: 'Particular' },
  { value: 'TURMA', label: 'Turma' },
]

const FORMAS_PAGAMENTO = [
  { value: 'PIX', label: 'Pix' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
]

export default function Aulas() {
  const { modalidades: ESPECIALIDADES } = useModalidades()
  const [aba, setAba] = useState('avulsas')

  const [professores, setProfessores] = useState([])
  const [quadras, setQuadras] = useState([])
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [aulas, setAulas] = useState([])
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)

  const [formProfessor, setFormProfessor] = useState({ nome: '', telefone: '', especialidade: 'BEACH_TENNIS', percentualComissao: '', tipoRemuneracao: 'PERCENTUAL', valorHora: '' })
  const [formAula, setFormAula] = useState({ professorId: '', quadraId: '', tipo: 'PARTICULAR', horaInicio: '08:00', duracao: 60, alunos: '', valor: '', pago: false, formaPagamento: 'PIX', lista: [] })
  const [novoAluno, setNovoAluno] = useState({ nome: '', valor: '', pago: false, forma: 'PIX' })
  const [editPorAluno, setEditPorAluno] = useState(false)
  const [addAluno, setAddAluno] = useState(null) // { aulaId, nome, valor }
  const [formaAluno, setFormaAluno] = useState('PIX')

  const [editProfessorId, setEditProfessorId] = useState(null)
  const [editAulaId, setEditAulaId] = useState(null)
  const [editTurmaId, setEditTurmaId] = useState(null)

  const [comissao, setComissao] = useState(null)
  const [filtroComissao, setFiltroComissao] = useState({
    professorId: '',
    inicio: format(new Date(new Date().getFullYear(), new Date().getMonth(), 1), 'yyyy-MM-dd'),
    fim: format(new Date(), 'yyyy-MM-dd'),
  })
  const [carregandoComissao, setCarregandoComissao] = useState(false)

  // --- Turmas e matrículas ---
  const [turmas, setTurmas] = useState([])
  const [clientes, setClientes] = useState([])
  const [carregandoTurmas, setCarregandoTurmas] = useState(true)
  const blocoVazio = () => ({ dias: ['MONDAY'], horaInicio: '08:00', horaFim: '09:00' })
  const [formTurma, setFormTurma] = useState({
    nome: '', tipo: 'VOLEI', professorId: '', quadraId: '',
    blocos: [blocoVazio()], capacidadeMaxima: '',
  })

  function atualizarBloco(i, mudanca) {
    setFormTurma((f) => ({ ...f, blocos: f.blocos.map((b, j) => (j === i ? { ...b, ...mudanca } : b)) }))
  }
  function alternarDia(i, dia) {
    setFormTurma((f) => ({
      ...f,
      blocos: f.blocos.map((b, j) => {
        if (j !== i) return b
        const dias = b.dias.includes(dia) ? b.dias.filter((d) => d !== dia) : [...b.dias, dia]
        return { ...b, dias }
      }),
    }))
  }
  const [turmaExpandidaId, setTurmaExpandidaId] = useState(null)

  // Se a modalidade padrao dos formularios nao existir (ex.: foi removida), usa a primeira cadastrada.
  useEffect(() => {
    if (!ESPECIALIDADES.length) return
    const existe = (v) => ESPECIALIDADES.some((e) => e.value === v)
    setFormProfessor((f) => (existe(f.especialidade) ? f : { ...f, especialidade: ESPECIALIDADES[0].value }))
    setFormTurma((f) => (existe(f.tipo) ? f : { ...f, tipo: ESPECIALIDADES[0].value }))
  }, [ESPECIALIDADES]) // eslint-disable-line react-hooks/exhaustive-deps
  const [matriculasPorTurma, setMatriculasPorTurma] = useState({})
  const [pagamentosPorMatricula, setPagamentosPorMatricula] = useState({})
  const [formMatricula, setFormMatricula] = useState({ clienteId: '', valorMensal: '', diaVencimento: 10, formaPagamento: 'PIX' })

  async function carregarTurmas() {
    setCarregandoTurmas(true)
    try {
      const lista = await turmaService.listar()
      setTurmas(lista)
    } catch {
      setErro('Não foi possível carregar as turmas.')
    } finally {
      setCarregandoTurmas(false)
    }
  }

  async function handleCriarTurma(e) {
    e.preventDefault()
    setErro('')
    try {
      if (formTurma.blocos.some((b) => b.dias.length === 0)) {
        setErro('Marque ao menos um dia da semana em cada horário.')
        return
      }
      const { blocos, ...resto } = formTurma
      const dados = {
        ...resto,
        horarios: blocosParaHorarios(blocos),
        professorId: Number(formTurma.professorId),
        quadraId: Number(formTurma.quadraId),
        capacidadeMaxima: formTurma.capacidadeMaxima ? Number(formTurma.capacidadeMaxima) : null,
      }
      if (editTurmaId) await turmaService.atualizar(editTurmaId, dados)
      else await turmaService.criar(dados)
      setEditTurmaId(null)
      setFormTurma({ ...formTurma, nome: '', capacidadeMaxima: '', blocos: [blocoVazio()] })
      carregarTurmas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar a turma.')
    }
  }

  function handleEditarTurma(t) {
    setEditTurmaId(t.id)
    setFormTurma({
      nome: t.nome, tipo: t.tipo, professorId: String(t.professorId), quadraId: String(t.quadraId),
      blocos: horariosParaBlocos(t.horarios?.length ? t.horarios : [{ diaSemana: t.diaSemana, horaInicio: t.horaInicio, horaFim: t.horaFim }]),
      capacidadeMaxima: t.capacidadeMaxima ?? '',
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function cancelarEdicaoTurma() {
    setEditTurmaId(null)
    setFormTurma({ nome: '', tipo: ESPECIALIDADES[0]?.value ?? 'VOLEI', professorId: '', quadraId: '', blocos: [blocoVazio()], capacidadeMaxima: '' })
  }

  async function handleExcluirTurma(turma) {
    if (!confirm(`Excluir a turma "${turma.nome}"? Só é possível se ela nunca teve matrículas.`)) return
    setErro('')
    try {
      await turmaService.excluir(turma.id)
      if (editTurmaId === turma.id) cancelarEdicaoTurma()
      carregarTurmas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível excluir a turma.')
    }
  }

  async function handleDesativarTurma(turma) {
    if (!confirm(`Desativar a turma "${turma.nome}"? Os alunos matriculados continuam, mas ela some das turmas ativas.`)) return
    try {
      await turmaService.desativar(turma.id)
      carregarTurmas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível desativar a turma.')
    }
  }

  async function carregarMatriculasDaTurma(turmaId) {
    setErro('')
    try {
      const lista = await matriculaService.listar()
      const doTurma = lista.filter((m) => m.turmaId === turmaId)
      setMatriculasPorTurma((prev) => ({ ...prev, [turmaId]: doTurma }))
      const pagamentos = await Promise.all(doTurma.map((m) => matriculaService.listarPagamentos(m.id)))
      setPagamentosPorMatricula((prev) => {
        const novo = { ...prev }
        doTurma.forEach((m, i) => { novo[m.id] = pagamentos[i] })
        return novo
      })
    } catch {
      setErro('Não foi possível carregar as matrículas dessa turma.')
    }
  }

  async function handleExpandirTurma(turma) {
    if (turmaExpandidaId === turma.id) {
      setTurmaExpandidaId(null)
      return
    }
    setTurmaExpandidaId(turma.id)
    await carregarMatriculasDaTurma(turma.id)
  }

  async function handleMatricular(e, turmaId) {
    e.preventDefault()
    setErro('')
    try {
      await matriculaService.criar({
        turmaId,
        clienteId: Number(formMatricula.clienteId),
        valorMensal: Number(formMatricula.valorMensal),
        diaVencimento: Number(formMatricula.diaVencimento),
        formaPagamento: formMatricula.formaPagamento || null,
      })
      setFormMatricula({ clienteId: '', valorMensal: '', diaVencimento: 10, formaPagamento: 'PIX' })
      carregarTurmas()
      setTurmaExpandidaId(turmaId)
      await carregarMatriculasDaTurma(turmaId)
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível matricular o aluno.')
    }
  }

  async function handleCancelarMatricula(matricula) {
    if (!confirm(`Cancelar a matrícula de ${matricula.clienteNome}?`)) return
    try {
      await matriculaService.cancelar(matricula.id)
      carregarTurmas()
      setMatriculasPorTurma((prev) => ({
        ...prev,
        [matricula.turmaId]: (prev[matricula.turmaId] || []).filter((m) => m.id !== matricula.id),
      }))
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível cancelar a matrícula.')
    }
  }

  function turmaIdDaMatricula(matriculaId) {
    return Object.keys(matriculasPorTurma).find((tid) => (matriculasPorTurma[tid] || []).some((m) => m.id === matriculaId))
  }

  async function handleFormaPagamentoMatricula(matricula, forma) {
    try {
      const atualizada = await matriculaService.atualizarFormaPagamento(matricula.id, forma || null)
      setMatriculasPorTurma((prev) => ({
        ...prev,
        [matricula.turmaId]: (prev[matricula.turmaId] || []).map((m) => (m.id === matricula.id ? atualizada : m)),
      }))
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível alterar a forma de pagamento.')
    }
  }

  async function handlePagarMensalidade(pagamento) {
    try {
      await matriculaService.registrarPagamento(pagamento.id, {
        formaPagamento: (matriculasPorTurma[turmaIdDaMatricula(pagamento.matriculaId)] || []).find((m) => m.id === pagamento.matriculaId)?.formaPagamento || 'DINHEIRO',
      })
      const atualizados = await matriculaService.listarPagamentos(pagamento.matriculaId)
      setPagamentosPorMatricula((prev) => ({ ...prev, [pagamento.matriculaId]: atualizados }))
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível registrar o pagamento.')
    }
  }

  async function carregarBase() {
    try {
      const [listaProfessores, listaQuadras] = await Promise.all([
        professorService.listar(),
        quadraService.listar(),
      ])
      setProfessores(listaProfessores)
      setQuadras(listaQuadras)
    } catch {
      setErro('Não foi possível carregar professores/quadras.')
    }
  }

  async function carregarAulas() {
    setCarregando(true)
    try {
      const lista = await aulaService.listarPorDia(data)
      lista.sort((a, b) => new Date(a.inicio) - new Date(b.inicio))
      setAulas(lista)
    } catch {
      setErro('Não foi possível carregar as aulas do dia.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregarBase() }, [])
  useEffect(() => { carregarAulas() }, [data]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (aba === 'turmas') {
      carregarTurmas()
      clienteService.listar().then(setClientes).catch(() => setErro('Não foi possível carregar os clientes.'))
    }
  }, [aba]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleCriarProfessor(e) {
    e.preventDefault()
    setErro('')
    try {
      const dados = {
        ...formProfessor,
        percentualComissao: formProfessor.tipoRemuneracao === 'POR_HORA' ? null : (formProfessor.percentualComissao ? Number(formProfessor.percentualComissao) : null),
        tipoRemuneracao: formProfessor.tipoRemuneracao,
        valorHora: formProfessor.tipoRemuneracao === 'POR_HORA' && formProfessor.valorHora ? Number(formProfessor.valorHora) : null,
      }
      if (editProfessorId) await professorService.atualizar(editProfessorId, dados)
      else await professorService.criar(dados)
      setEditProfessorId(null)
      setFormProfessor({ nome: '', telefone: '', especialidade: 'BEACH_TENNIS', percentualComissao: '', tipoRemuneracao: 'PERCENTUAL', valorHora: '' })
      carregarBase()
    } catch {
      setErro('Não foi possível salvar o professor.')
    }
  }

  function handleEditarProfessor(p) {
    setEditProfessorId(p.id)
    setFormProfessor({ nome: p.nome, telefone: p.telefone || '', especialidade: p.especialidade, percentualComissao: p.percentualComissao ?? '', tipoRemuneracao: p.tipoRemuneracao || 'PERCENTUAL', valorHora: p.valorHora ?? '' })
  }

  function cancelarEdicaoProfessor() {
    setEditProfessorId(null)
    setFormProfessor({ nome: '', telefone: '', especialidade: 'BEACH_TENNIS', percentualComissao: '', tipoRemuneracao: 'PERCENTUAL', valorHora: '' })
  }

  async function handleExcluirProfessor(p) {
    if (!confirm(`Excluir o professor "${p.nome}"? Só é possível se ele não tiver aulas, turmas ou comissões.`)) return
    setErro('')
    try {
      await professorService.excluir(p.id)
      if (editProfessorId === p.id) cancelarEdicaoProfessor()
      carregarBase()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível excluir o professor.')
    }
  }

  async function handleCriarAula(e) {
    e.preventDefault()
    if (ocupadoRef.current) return // evita enviar o formulario duas vezes
    ocupadoRef.current = true
    try {
      await salvarAula()
    } finally {
      ocupadoRef.current = false
    }
  }

  async function salvarAula() {
    setErro('')
    const inicio = new Date(`${data}T${formAula.horaInicio}:00`)
    const fim = new Date(inicio.getTime() + formAula.duracao * 60000)
    try {
      const dadosAula = {
        professorId: Number(formAula.professorId),
        quadraId: Number(formAula.quadraId),
        tipo: formAula.tipo,
        inicio: format(inicio, "yyyy-MM-dd'T'HH:mm:ss"),
        fim: format(fim, "yyyy-MM-dd'T'HH:mm:ss"),
        valorPorAluno: formAula.valor ? Number(formAula.valor) : null,
      }
      if (!editAulaId) {
        // inclui o aluno digitado que ainda nao foi adicionado com o botao
        let lista = formAula.lista
        const pendente = novoAluno.nome.trim()
        if (pendente) {
          const v = novoAluno.valor !== '' ? Number(novoAluno.valor) : (formAula.valor !== '' ? Number(formAula.valor) : null)
          if (v == null || Number.isNaN(v) || v < 0) { setErro(`Informe o valor de ${pendente} (ou o valor padrão por aluno).`); return }
          lista = [...lista, { nome: pendente, valor: v, pago: novoAluno.pago, forma: novoAluno.forma }]
        }
        dadosAula.participantes = lista.map((x) => ({
          nome: x.nome, valor: x.valor, pago: x.pago, formaPagamento: x.pago ? x.forma : null,
        }))
      } else if (!editPorAluno) {
        dadosAula.alunos = formAula.alunos ? formAula.alunos.split(',').map((s) => s.trim()).filter(Boolean) : []
      }
      if (editAulaId) await aulaService.atualizar(editAulaId, dadosAula)
      else await aulaService.criar(dadosAula)
      setEditAulaId(null)
      setEditPorAluno(false)
      setFormAula({ ...formAula, alunos: '', valor: '', pago: false, lista: [] })
      setNovoAluno({ nome: '', valor: '', pago: false, forma: 'PIX' })
      carregarAulas()
    } catch (err) {
      if (err.response?.status === 409) {
        setErro('Este professor já tem uma aula nesse horário.')
      } else {
        setErro(err.response?.data?.mensagem || 'Não foi possível salvar a aula.')
      }
    }
  }

  function adicionarAlunoNaLista() {
    const nome = novoAluno.nome.trim()
    const valor = novoAluno.valor !== '' ? Number(novoAluno.valor) : (formAula.valor !== '' ? Number(formAula.valor) : null)
    if (!nome) { setErro('Digite o nome do aluno.'); return }
    if (valor == null || Number.isNaN(valor) || valor < 0) { setErro('Informe o valor do aluno (ou o valor padrão por aluno).'); return }
    setErro('')
    setFormAula({ ...formAula, lista: [...formAula.lista, { nome, valor, pago: novoAluno.pago, forma: novoAluno.forma }] })
    setNovoAluno({ ...novoAluno, nome: '', valor: '', pago: false })
  }

  const ocupadoRef = useRef(false)
  async function executarAluno(fn) {
    if (ocupadoRef.current) return // evita clique duplo (pagamento duplicado)
    ocupadoRef.current = true
    setErro('')
    try {
      await fn()
      carregarAulas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível concluir a operação.')
    } finally {
      ocupadoRef.current = false
    }
  }

  function handleAddAlunoAula(a) {
    if (!addAluno?.nome?.trim()) { setErro('Digite o nome do aluno.'); return }
    executarAluno(async () => {
      await aulaService.adicionarAluno(a.id, {
        nome: addAluno.nome.trim(),
        valor: addAluno.valor !== '' ? Number(addAluno.valor) : null,
        pago: !!addAluno.pago,
        formaPagamento: addAluno.pago ? formaAluno : null,
      })
      setAddAluno(null)
    })
  }

  async function handlePagarAula(a) {
    const forma = (window.prompt(`Receber R$ ${Number(a.valor).toFixed(2)} — forma de pagamento (PIX, DINHEIRO, CARTAO_DEBITO, CARTAO_CREDITO):`, 'PIX') || '').trim().toUpperCase()
    if (!forma || ocupadoRef.current) return
    ocupadoRef.current = true
    setErro('')
    try {
      await aulaService.pagar(a.id, forma)
      carregarAulas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível registrar o pagamento.')
    } finally {
      ocupadoRef.current = false
    }
  }

  function handleEditarAula(a) {
    const ini = new Date(a.inicio)
    const fim = new Date(a.fim)
    setEditAulaId(a.id)
    setEditPorAluno(!!a.porAluno)
    setData(format(ini, 'yyyy-MM-dd'))
    setFormAula({
      professorId: String(a.professor?.id ?? ''), quadraId: String(a.quadra?.id ?? ''), tipo: a.tipo,
      horaInicio: format(ini, 'HH:mm'), duracao: Math.round((fim - ini) / 60000),
      alunos: (a.alunos || []).join(', '),
      valor: a.valorPorAluno ?? (a.valor ? Number(a.valor) / Math.max(1, (a.alunos || []).length) : ''),
    })
  }

  function cancelarEdicaoAula() {
    setEditAulaId(null)
    setEditPorAluno(false)
    setFormAula({ ...formAula, alunos: '', valor: '', pago: false, lista: [] })
  }

  async function handleRemover(id) {
    if (!confirm('Remover esta aula?')) return
    try {
      await aulaService.remover(id)
      carregarAulas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível remover a aula.')
    }
  }

  async function handleEstornar(a) {
    if (!confirm('ESTORNAR e remover esta aula?\n\nO(s) pagamento(s) serão retirados do caixa e a aula será apagada para ser refeita. Só o administrador pode fazer isso.')) return
    setErro('')
    try {
      const r = await aulaService.estornar(a.id)
      alert(r.mensagem)
      carregarAulas()
    } catch (err) {
      setErro(err.response?.status === 403 ? 'Somente o administrador pode estornar uma aula paga.' : (err.response?.data?.mensagem || 'Não foi possível estornar a aula.'))
    }
  }

  async function handleConsultarComissao(e) {
    e.preventDefault()
    setErro('')
    if (!filtroComissao.professorId) {
      setErro('Selecione um professor.')
      return
    }
    setCarregandoComissao(true)
    try {
      const resultado = await aulaService.comissao(filtroComissao.professorId, filtroComissao.inicio, filtroComissao.fim)
      setComissao(resultado)
    } catch {
      setErro('Não foi possível calcular a comissão.')
    } finally {
      setCarregandoComissao(false)
    }
  }

  return (
    <div>
      <h2 className="font-title text-2xl text-araca-verde mb-6 flex items-center gap-2">
        <GraduationCap size={22} className="text-araca-verde-escuro" />
        Aulas
      </h2>

      <div className="flex gap-2 mb-6 border-b border-gray-200 flex-wrap">
        <button
          onClick={() => setAba('avulsas')}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition ${
            aba === 'avulsas' ? 'border-araca-verde-escuro text-araca-verde-escuro' : 'border-transparent text-gray-500 hover:text-araca-azul'
          }`}
        >
          Aulas avulsas e comissão
        </button>
        <button
          onClick={() => setAba('turmas')}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition flex items-center gap-1.5 ${
            aba === 'turmas' ? 'border-araca-verde-escuro text-araca-verde-escuro' : 'border-transparent text-gray-500 hover:text-araca-azul'
          }`}
        >
          <Users2 size={15} /> Turmas e matrículas
        </button>
      </div>

      {erro && aba !== 'avulsas' && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {aba === 'avulsas' && (
      <>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <form onSubmit={handleCriarProfessor} className="bg-white rounded-xl shadow p-6 space-y-3">
          <h3 className="font-semibold text-araca-azul">{editProfessorId ? 'Editar professor' : 'Cadastrar professor'}</h3>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            placeholder="Nome"
            value={formProfessor.nome}
            onChange={(e) => setFormProfessor({ ...formProfessor, nome: e.target.value })}
            required
          />
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            placeholder="Telefone"
            value={formProfessor.telefone}
            onChange={(e) => setFormProfessor({ ...formProfessor, telefone: e.target.value })}
          />
          <div className="flex gap-3 flex-wrap">
            <select
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formProfessor.especialidade}
              onChange={(e) => setFormProfessor({ ...formProfessor, especialidade: e.target.value })}
            >
              {ESPECIALIDADES.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
            </select>
          </div>
          <div className="flex gap-3 flex-wrap">
            <select
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formProfessor.tipoRemuneracao}
              onChange={(e) => setFormProfessor({ ...formProfessor, tipoRemuneracao: e.target.value })}
            >
              <option value="PERCENTUAL">Recebe % do valor da aula</option>
              <option value="POR_HORA">Recebe por hora/aula</option>
            </select>
            {formProfessor.tipoRemuneracao === 'POR_HORA' ? (
              <input
                type="number" step="0.01" min="0" required
                className="w-36 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                placeholder="R$ por hora"
                value={formProfessor.valorHora}
                onChange={(e) => setFormProfessor({ ...formProfessor, valorHora: e.target.value })}
              />
            ) : (
              <input
                type="number" step="0.01"
                className="w-32 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                placeholder="% comissão"
                value={formProfessor.percentualComissao}
                onChange={(e) => setFormProfessor({ ...formProfessor, percentualComissao: e.target.value })}
              />
            )}
          </div>
          <div className="flex gap-2 flex-wrap">
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
              {editProfessorId ? 'Salvar alterações' : 'Adicionar professor'}
            </button>
            {editProfessorId && (
              <button type="button" onClick={cancelarEdicaoProfessor} className="px-4 py-2 rounded-lg border text-gray-600">Cancelar</button>
            )}
          </div>
          <ul className="text-sm text-gray-600 pt-2 space-y-1">
            {professores.map((p) => (
              <li key={p.id} className="flex items-center justify-between gap-2">
                <span>{p.nome} · {ESPECIALIDADES.find(e => e.value === p.especialidade)?.label}{p.tipoRemuneracao === 'POR_HORA' ? ` · R$ ${Number(p.valorHora || 0).toFixed(2)}/h` : (p.percentualComissao ? ` · ${p.percentualComissao}%` : '')}</span>
                <span className="flex gap-3 shrink-0">
                  <button type="button" onClick={() => handleEditarProfessor(p)} className="text-araca-azul hover:underline">Editar</button>
                  <button type="button" onClick={() => handleExcluirProfessor(p)} className="text-red-600 hover:underline">Excluir</button>
                </span>
              </li>
            ))}
          </ul>
        </form>

        <form onSubmit={handleCriarAula} className="bg-white rounded-xl shadow p-6 space-y-3">
          <h3 className="font-semibold text-araca-azul">{editAulaId ? 'Editar aula' : 'Agendar aula'}</h3>
          <select
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={formAula.professorId}
            onChange={(e) => setFormAula({ ...formAula, professorId: e.target.value })}
            required
          >
            <option value="">Selecione o professor...</option>
            {professores.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
          </select>
          <select
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={formAula.quadraId}
            onChange={(e) => setFormAula({ ...formAula, quadraId: e.target.value })}
            required
          >
            <option value="">Selecione a quadra...</option>
            {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
          </select>
          <div className="flex flex-wrap gap-3">
            <select
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formAula.tipo}
              onChange={(e) => setFormAula({ ...formAula, tipo: e.target.value })}
            >
              {TIPOS_AULA.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
            <input
              type="date"
              required
              title="Data da aula (pode ser retroativa)"
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={data}
              onChange={(e) => setData(e.target.value)}
            />
            <input
              type="time"
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formAula.horaInicio}
              onChange={(e) => setFormAula({ ...formAula, horaInicio: e.target.value })}
            />
            <select
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formAula.duracao}
              onChange={(e) => setFormAula({ ...formAula, duracao: Number(e.target.value) })}
            >
              {![30, 60, 90].includes(formAula.duracao) && <option value={formAula.duracao}>{formAula.duracao} min</option>}
              <option value={30}>30 min</option>
              <option value={60}>1 hora</option>
              <option value={90}>1h30</option>
            </select>
          </div>
          {(!editAulaId || !editPorAluno) && !editAulaId ? null : (editAulaId && !editPorAluno && (
            <input
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="Alunos (separados por vírgula)"
              value={formAula.alunos}
              onChange={(e) => setFormAula({ ...formAula, alunos: e.target.value })}
            />
          ))}
          <input
            type="number"
            step="0.01"
            min="0"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            placeholder="Valor padrão por aluno (R$)"
            value={formAula.valor}
            onChange={(e) => setFormAula({ ...formAula, valor: e.target.value })}
          />
          {!editAulaId && (
            <div className="border rounded-lg p-3 space-y-2 bg-gray-50">
              <p className="text-sm font-medium text-gray-700">Alunos da aula</p>
              <div className="flex flex-wrap gap-2">
                <input
                  className="flex-1 min-w-[8rem] border rounded-lg px-3 py-2"
                  placeholder="Nome do aluno"
                  value={novoAluno.nome}
                  onChange={(e) => setNovoAluno({ ...novoAluno, nome: e.target.value })}
                  onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); adicionarAlunoNaLista() } }}
                />
                <input
                  type="number" step="0.01" min="0"
                  className="w-28 border rounded-lg px-3 py-2"
                  placeholder={formAula.valor ? `R$ ${Number(formAula.valor).toFixed(2)}` : 'Valor'}
                  value={novoAluno.valor}
                  onChange={(e) => setNovoAluno({ ...novoAluno, valor: e.target.value })}
                />
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input type="checkbox" checked={novoAluno.pago}
                    onChange={(e) => setNovoAluno({ ...novoAluno, pago: e.target.checked })} />
                  Já pagou
                </label>
                {novoAluno.pago && (
                  <select className="border rounded-lg px-2 py-1 text-sm" value={novoAluno.forma}
                    onChange={(e) => setNovoAluno({ ...novoAluno, forma: e.target.value })}>
                    <option value="PIX">Pix</option>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="CARTAO_DEBITO">Cartão de débito</option>
                    <option value="CARTAO_CREDITO">Cartão de crédito</option>
                  </select>
                )}
                <button type="button" onClick={adicionarAlunoNaLista}
                  className="ml-auto bg-araca-azul text-white text-sm px-3 py-1.5 rounded-lg">+ Adicionar aluno</button>
              </div>
              {formAula.lista.length > 0 && (
                <ul className="text-sm divide-y">
                  {formAula.lista.map((x, idx) => (
                    <li key={idx} className="flex items-center justify-between py-1">
                      <span>{x.nome} · R$ {Number(x.valor).toFixed(2)}{x.pago ? ` · pago (${x.forma})` : ' · a receber'}</span>
                      <button type="button" className="text-red-600"
                        onClick={() => setFormAula({ ...formAula, lista: formAula.lista.filter((_, k) => k !== idx) })}>remover</button>
                    </li>
                  ))}
                </ul>
              )}
              {formAula.lista.length > 0 && (
                <p className="text-sm text-gray-600">
                  Total: <strong className="text-araca-azul">R$ {formAula.lista.reduce((t, x) => t + Number(x.valor), 0).toFixed(2)}</strong>
                </p>
              )}
              <p className="text-xs text-gray-500">Cada pagamento marcado entra no caixa na hora de agendar. Dá para adicionar mais alunos e receber depois, na lista de aulas.</p>
            </div>
          )}
          {editAulaId && editPorAluno && (
            <p className="text-xs text-gray-500">Alunos e pagamentos desta aula são gerenciados na lista de aulas ao lado.</p>
          )}
          {erro && <p className="text-red-600 text-sm bg-red-50 rounded-lg px-3 py-2">{erro}</p>}
          <button type="submit" className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">
            {editAulaId ? 'Salvar alterações' : 'Agendar aula'}
          </button>
          {editAulaId && (
            <button type="button" onClick={cancelarEdicaoAula} className="w-full border rounded-lg py-2 text-gray-600">Cancelar edição</button>
          )}
        </form>
      </div>

      <div className="bg-white rounded-xl shadow p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-araca-azul">Aulas do dia</h3>
          <input
            type="date"
            className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            value={data}
            onChange={(e) => setData(e.target.value)}
          />
        </div>
        {carregando && <p className="text-gray-400 text-sm">Carregando...</p>}
        {!carregando && aulas.length === 0 && <p className="text-gray-400 text-sm">Nenhuma aula nesse dia.</p>}
        <ul className="space-y-2">
          {aulas.map((a) => (
            <li key={a.id} className="flex items-start justify-between gap-3 border rounded-lg px-4 py-3">
              <div>
                <p className="font-medium text-araca-azul">
                  {format(new Date(a.inicio), 'HH:mm')} — {format(new Date(a.fim), 'HH:mm')} · {a.professor?.nome}
                </p>
                <p className="text-sm text-gray-500">
                  {a.quadra?.nome} · {TIPOS_AULA.find(t => t.value === a.tipo)?.label}
                  {a.valor ? ` · R$ ${Number(a.valor).toFixed(2)}${!a.porAluno && a.valorPorAluno && a.alunos?.length > 1 ? ` (${a.alunos.length} × ${Number(a.valorPorAluno).toFixed(2)})` : ''}` : ''}
                  {!a.porAluno && a.alunos?.length > 0 ? ` · ${a.alunos.join(', ')}` : ''}
                </p>
                {a.porAluno && (
                  <div className="mt-2 text-sm">
                    <div className="flex items-center gap-2 mb-1 text-xs text-gray-500 flex-wrap">
                      Forma ao receber:
                      <select className="border rounded px-1 py-0.5" value={formaAluno} onChange={(e) => setFormaAluno(e.target.value)}>
                        <option value="PIX">Pix</option>
                        <option value="DINHEIRO">Dinheiro</option>
                        <option value="CARTAO_DEBITO">Débito</option>
                        <option value="CARTAO_CREDITO">Crédito</option>
                      </select>
                    </div>
                    <ul className="space-y-1">
                      {(a.participantes || []).map((p) => (
                        <li key={p.id} className="flex items-center gap-3 flex-wrap">
                          <span className="flex-1">{p.nome} · R$ {Number(p.valor).toFixed(2)}</span>
                          {p.pago
                            ? <span className="text-xs px-2 py-0.5 rounded bg-green-100 text-green-700">Pago ({p.formaPagamento})</span>
                            : (<>
                                {Number(p.valor) > 0 && <button type="button" className="text-green-700 font-semibold hover:underline"
                                  onClick={() => executarAluno(() => aulaService.pagarAluno(a.id, p.id, formaAluno))}>Receber</button>}
                                <button type="button" className="text-red-600 hover:underline"
                                  onClick={() => { if (confirm(`Remover ${p.nome} da aula?`)) executarAluno(() => aulaService.removerAluno(a.id, p.id)) }}>x</button>
                              </>)}
                        </li>
                      ))}
                    </ul>
                    {addAluno?.aulaId === a.id ? (
                      <div className="flex flex-wrap items-center gap-2 mt-2">
                        <input className="border rounded px-2 py-1 flex-1 min-w-[7rem]" placeholder="Nome do aluno" value={addAluno.nome}
                          onChange={(e) => setAddAluno({ ...addAluno, nome: e.target.value })} />
                        <input type="number" step="0.01" min="0" className="border rounded px-2 py-1 w-24"
                          placeholder={a.valorPorAluno ? `R$ ${Number(a.valorPorAluno).toFixed(2)}` : 'Valor'} value={addAluno.valor}
                          onChange={(e) => setAddAluno({ ...addAluno, valor: e.target.value })} />
                        <label className="flex items-center gap-1 text-xs">
                          <input type="checkbox" checked={!!addAluno.pago} onChange={(e) => setAddAluno({ ...addAluno, pago: e.target.checked })} /> já pagou
                        </label>
                        <button type="button" className="bg-araca-azul text-white px-2 py-1 rounded" onClick={() => handleAddAlunoAula(a)}>Salvar</button>
                        <button type="button" className="text-gray-500" onClick={() => setAddAluno(null)}>cancelar</button>
                      </div>
                    ) : (
                      <button type="button" className="mt-2 text-araca-azul hover:underline"
                        onClick={() => setAddAluno({ aulaId: a.id, nome: '', valor: '', pago: false })}>+ Adicionar aluno</button>
                    )}
                  </div>
                )}
              </div>
              <div className="flex gap-3 items-center flex-wrap">
                {!a.porAluno && a.valor > 0 && (a.pago
                  ? <span className="text-xs px-2 py-0.5 rounded bg-green-100 text-green-700">Pago</span>
                  : <button onClick={() => handlePagarAula(a)} className="text-green-700 text-sm font-semibold hover:underline">Receber</button>)}
                <button onClick={() => handleEditarAula(a)} className="text-araca-azul text-sm hover:underline">Editar</button>
                {(a.pago || (a.participantes || []).some((p) => p.pago))
                  ? <button onClick={() => handleEstornar(a)} className="text-red-600 text-sm hover:underline">Estornar e remover</button>
                  : <button onClick={() => handleRemover(a.id)} className="text-red-600 text-sm hover:underline">Remover</button>}
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="bg-white rounded-xl shadow p-6 mt-6">
        <h3 className="font-semibold text-araca-azul mb-4">Comissão do professor</h3>
        <form onSubmit={handleConsultarComissao} className="flex flex-wrap gap-3 items-end mb-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Professor</label>
            <select
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={filtroComissao.professorId}
              onChange={(e) => setFiltroComissao({ ...filtroComissao, professorId: e.target.value })}
              required
            >
              <option value="">Selecione...</option>
              {professores.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">De</label>
            <input
              type="date"
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={filtroComissao.inicio}
              onChange={(e) => setFiltroComissao({ ...filtroComissao, inicio: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Até</label>
            <input
              type="date"
              className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={filtroComissao.fim}
              onChange={(e) => setFiltroComissao({ ...filtroComissao, fim: e.target.value })}
            />
          </div>
          <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
            Calcular
          </button>
        </form>

        {carregandoComissao && <p className="text-gray-400 text-sm">Calculando...</p>}

        {comissao && !carregandoComissao && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-xs text-gray-500">Aulas dadas</p>
              <p className="text-xl font-bold text-araca-azul">{comissao.quantidadeAulas}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Valor total das aulas</p>
              <p className="text-xl font-bold text-araca-azul">R$ {Number(comissao.valorTotalAulas).toFixed(2)}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">{comissao.tipoRemuneracao === 'POR_HORA' ? `Horas (R$ ${Number(comissao.valorHora || 0).toFixed(2)}/h)` : '% comissão'}</p>
              <p className="text-xl font-bold text-araca-azul">{comissao.tipoRemuneracao === 'POR_HORA' ? `${Number(comissao.horas || 0).toFixed(2)} h` : `${Number(comissao.percentualComissao).toFixed(1)}%`}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Comissão devida</p>
              <p className="text-xl font-bold text-araca-verde-escuro">R$ {Number(comissao.valorComissao).toFixed(2)}</p>
            </div>
          </div>
        )}
      </div>
      </>
      )}

      {aba === 'turmas' && (
        <div className="space-y-6">
          <form onSubmit={handleCriarTurma} className="bg-white rounded-xl shadow p-6 space-y-3">
            <h3 className="font-semibold text-araca-azul">{editTurmaId ? 'Editar turma' : 'Nova turma'}</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <input
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                placeholder="Nome da turma (ex.: Vôlei Infantil - Ter/Qui 17h)"
                value={formTurma.nome}
                onChange={(e) => setFormTurma({ ...formTurma, nome: e.target.value })}
                required
              />
              <select
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formTurma.tipo}
                onChange={(e) => setFormTurma({ ...formTurma, tipo: e.target.value })}
              >
                {ESPECIALIDADES.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
              </select>
              <select
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formTurma.professorId}
                onChange={(e) => setFormTurma({ ...formTurma, professorId: e.target.value })}
                required
              >
                <option value="">Selecione o professor...</option>
                {professores.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
              </select>
              <select
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formTurma.quadraId}
                onChange={(e) => setFormTurma({ ...formTurma, quadraId: e.target.value })}
                required
              >
                <option value="">Selecione a quadra...</option>
                {quadras.map((q) => <option key={q.id} value={q.id}>{q.nome}</option>)}
              </select>
              <input
                type="number"
                min="1"
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                placeholder="Capacidade máxima (opcional)"
                value={formTurma.capacidadeMaxima}
                onChange={(e) => setFormTurma({ ...formTurma, capacidadeMaxima: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-gray-700">Horários da turma</p>
              {formTurma.blocos.map((b, i) => (
                <div key={i} className="border rounded-lg p-3 flex flex-wrap items-center gap-3">
                  <div className="flex flex-wrap gap-1">
                    {DIAS_SEMANA.map((d) => (
                      <button
                        type="button"
                        key={d.value}
                        onClick={() => alternarDia(i, d.value)}
                        className={`px-2.5 py-1 rounded-full text-xs border ${b.dias.includes(d.value) ? 'bg-araca-azul text-white border-araca-azul' : 'bg-white text-gray-600 hover:bg-gray-50'}`}
                      >
                        {d.label.slice(0, 3)}
                      </button>
                    ))}
                  </div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <input
                      type="time"
                      className="border rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={b.horaInicio}
                      onChange={(e) => atualizarBloco(i, { horaInicio: e.target.value })}
                      required
                    />
                    <span className="text-gray-400">às</span>
                    <input
                      type="time"
                      className="border rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                      value={b.horaFim}
                      onChange={(e) => atualizarBloco(i, { horaFim: e.target.value })}
                      required
                    />
                  </div>
                  {formTurma.blocos.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setFormTurma((f) => ({ ...f, blocos: f.blocos.filter((_, j) => j !== i) }))}
                      className="text-red-600 text-sm hover:underline ml-auto"
                    >
                      Remover
                    </button>
                  )}
                </div>
              ))}
              <button
                type="button"
                onClick={() => setFormTurma((f) => {
                  const ultimo = f.blocos[f.blocos.length - 1]
                  return { ...f, blocos: [...f.blocos, { dias: [...ultimo.dias], horaInicio: ultimo.horaFim, horaFim: ultimo.horaFim }] }
                })}
                className="text-araca-azul text-sm font-medium hover:underline"
              >
                + Adicionar outro horário
              </button>
              <p className="text-xs text-gray-500">Marque os dias e informe início e fim. Use "Adicionar outro horário" para horários separados (ex.: 17h, 18h e 20h nos mesmos dias).</p>
            </div>
            <div className="flex gap-2 flex-wrap">
              <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
                {editTurmaId ? 'Salvar alterações' : 'Criar turma'}
              </button>
              {editTurmaId && (
                <button type="button" onClick={cancelarEdicaoTurma} className="px-4 py-2 rounded-lg border text-gray-600">Cancelar</button>
              )}
            </div>
          </form>

          <div className="bg-white rounded-xl shadow p-6">
            <h3 className="font-semibold text-araca-azul mb-4">Turmas ativas</h3>
            {carregandoTurmas && <p className="text-gray-400 text-sm">Carregando...</p>}
            {!carregandoTurmas && turmas.length === 0 && (
              <p className="text-gray-400 text-sm">Nenhuma turma cadastrada ainda.</p>
            )}
            <ul className="space-y-3">
              {turmas.map((t) => (
                <li key={t.id} className="border rounded-lg overflow-hidden">
                  <div className="flex items-center justify-between px-4 py-3">
                    <div>
                      <p className="font-medium text-araca-azul">{t.nome}</p>
                      <p className="text-sm text-gray-500">
                        {resumoHorarios(t.horarios?.length ? t.horarios : [{ diaSemana: t.diaSemana, horaInicio: t.horaInicio, horaFim: t.horaFim }])} ·{' '}
                        {t.professorNome} · {t.quadraNome} · {t.matriculasAtivas}
                        {t.capacidadeMaxima ? `/${t.capacidadeMaxima}` : ''} aluno(s)
                      </p>
                    </div>
                    <div className="flex items-center gap-3 flex-wrap">
                      <button
                        onClick={() => handleExpandirTurma(t)}
                        className="text-araca-verde-escuro text-sm hover:underline"
                      >
                        {turmaExpandidaId === t.id ? 'Fechar' : 'Ver alunos'}
                      </button>
                      <button onClick={() => handleEditarTurma(t)} className="text-araca-azul text-sm hover:underline">Editar</button>
                      <button onClick={() => handleExcluirTurma(t)} className="text-red-600 text-sm hover:underline">Excluir</button>
                      <button
                        onClick={() => handleDesativarTurma(t)}
                        className="text-red-600 text-sm hover:underline"
                      >
                        Desativar
                      </button>
                    </div>
                  </div>

                  {turmaExpandidaId === t.id && (
                    <div className="bg-araca-areia/40 border-t px-4 py-4 space-y-4">
                      <form onSubmit={(e) => handleMatricular(e, t.id)} className="flex flex-wrap gap-3 items-end">
                        <div className="flex-1 min-w-[180px]">
                          <label className="block text-xs text-gray-500 mb-1">Cliente</label>
                          <select
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                            value={formMatricula.clienteId}
                            onChange={(e) => setFormMatricula({ ...formMatricula, clienteId: e.target.value })}
                            required
                          >
                            <option value="">Selecione...</option>
                            {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Valor mensal (R$)</label>
                          <input
                            type="number"
                            step="0.01"
                            min="0.01"
                            className="w-32 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                            value={formMatricula.valorMensal}
                            onChange={(e) => setFormMatricula({ ...formMatricula, valorMensal: e.target.value })}
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Dia de vencimento</label>
                          <input
                            type="number"
                            min="1"
                            max="28"
                            className="w-24 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                            value={formMatricula.diaVencimento}
                            onChange={(e) => setFormMatricula({ ...formMatricula, diaVencimento: e.target.value })}
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Forma de pagamento</label>
                          <select
                            className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-araca-verde"
                            value={formMatricula.formaPagamento}
                            onChange={(e) => setFormMatricula({ ...formMatricula, formaPagamento: e.target.value })}
                          >
                            {FORMAS_PAGAMENTO.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                          </select>
                        </div>
                        <button type="submit" className="bg-araca-verde text-araca-azul text-sm font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
                          Matricular
                        </button>
                      </form>

                      {(matriculasPorTurma[t.id] || []).length === 0 && (
                        <p className="text-gray-400 text-sm">Nenhum aluno matriculado ainda.</p>
                      )}
                      <ul className="space-y-2">
                        {(matriculasPorTurma[t.id] || []).map((m) => (
                          <li key={m.id} className="bg-white border rounded-lg px-4 py-3">
                            <div className="flex items-center justify-between">
                              <div>
                                <p className="font-medium text-araca-azul text-sm">{m.clienteNome}</p>
                                <p className="text-xs text-gray-500">
                                  R$ {Number(m.valorMensal).toFixed(2)}/mês · vencimento dia {m.diaVencimento}
                                </p>
                                <label className="text-xs text-gray-500 flex items-center gap-1 mt-1">
                                  Pagamento:
                                  <select
                                    className="border rounded px-1 py-0.5 text-xs"
                                    value={m.formaPagamento || ''}
                                    onChange={(e) => handleFormaPagamentoMatricula(m, e.target.value)}
                                  >
                                    <option value="">Não definida</option>
                                    {FORMAS_PAGAMENTO.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                                  </select>
                                </label>
                              </div>
                              <button
                                onClick={() => handleCancelarMatricula(m)}
                                className="text-red-600 text-xs hover:underline"
                              >
                                Cancelar matrícula
                              </button>
                            </div>
                            {(pagamentosPorMatricula[m.id] || []).length > 0 && (
                              <ul className="mt-2 pt-2 border-t space-y-1">
                                {(pagamentosPorMatricula[m.id] || []).map((p) => (
                                  <li key={p.id} className="flex items-center justify-between text-xs">
                                    <span className="text-gray-500">
                                      {p.referenciaMes} · vence {format(new Date(p.vencimento), 'dd/MM/yyyy')}
                                    </span>
                                    {p.pago ? (
                                      <span className="text-araca-verde-escuro font-medium">Pago</span>
                                    ) : (
                                      <button
                                        onClick={() => handlePagarMensalidade(p)}
                                        className="text-araca-azul font-medium hover:underline"
                                      >
                                        Marcar como pago
                                      </button>
                                    )}
                                  </li>
                                ))}
                              </ul>
                            )}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}
