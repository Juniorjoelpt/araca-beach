import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { professorService } from '../services/professorService.js'
import { quadraService } from '../services/quadraService.js'
import { aulaService } from '../services/aulaService.js'
import { turmaService, matriculaService } from '../services/turmaService.js'
import { clienteService } from '../services/clienteService.js'
import { GraduationCap, Users2 } from 'lucide-react'

const DIAS_SEMANA = [
  { value: 'MONDAY', label: 'Segunda' },
  { value: 'TUESDAY', label: 'Terça' },
  { value: 'WEDNESDAY', label: 'Quarta' },
  { value: 'THURSDAY', label: 'Quinta' },
  { value: 'FRIDAY', label: 'Sexta' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
]

const ESPECIALIDADES = [
  { value: 'VOLEI', label: 'Vôlei' },
  { value: 'FUTEVOLEI', label: 'Futevôlei' },
  { value: 'BEACH_TENNIS', label: 'Beach Tennis' },
]

const TIPOS_AULA = [
  { value: 'PARTICULAR', label: 'Particular' },
  { value: 'TURMA', label: 'Turma' },
]

export default function Aulas() {
  const [aba, setAba] = useState('avulsas')

  const [professores, setProfessores] = useState([])
  const [quadras, setQuadras] = useState([])
  const [data, setData] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [aulas, setAulas] = useState([])
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)

  const [formProfessor, setFormProfessor] = useState({ nome: '', telefone: '', especialidade: 'BEACH_TENNIS', percentualComissao: '' })
  const [formAula, setFormAula] = useState({ professorId: '', quadraId: '', tipo: 'PARTICULAR', horaInicio: '08:00', duracao: 60, alunos: '', valor: '' })

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
  const [formTurma, setFormTurma] = useState({
    nome: '', tipo: 'VOLEI', professorId: '', quadraId: '', diaSemana: 'MONDAY',
    horaInicio: '08:00', horaFim: '09:00', capacidadeMaxima: '',
  })
  const [turmaExpandidaId, setTurmaExpandidaId] = useState(null)
  const [matriculasPorTurma, setMatriculasPorTurma] = useState({})
  const [pagamentosPorMatricula, setPagamentosPorMatricula] = useState({})
  const [formMatricula, setFormMatricula] = useState({ clienteId: '', valorMensal: '', diaVencimento: 10 })

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
      await turmaService.criar({
        ...formTurma,
        professorId: Number(formTurma.professorId),
        quadraId: Number(formTurma.quadraId),
        capacidadeMaxima: formTurma.capacidadeMaxima ? Number(formTurma.capacidadeMaxima) : null,
      })
      setFormTurma({ ...formTurma, nome: '', capacidadeMaxima: '' })
      carregarTurmas()
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Não foi possível criar a turma.')
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
      })
      setFormMatricula({ clienteId: '', valorMensal: '', diaVencimento: 10 })
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

  async function handlePagarMensalidade(pagamento) {
    try {
      await matriculaService.registrarPagamento(pagamento.id, { formaPagamento: 'DINHEIRO' })
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
      await professorService.criar({
        ...formProfessor,
        percentualComissao: formProfessor.percentualComissao ? Number(formProfessor.percentualComissao) : null,
      })
      setFormProfessor({ nome: '', telefone: '', especialidade: 'BEACH_TENNIS', percentualComissao: '' })
      carregarBase()
    } catch {
      setErro('Não foi possível salvar o professor.')
    }
  }

  async function handleCriarAula(e) {
    e.preventDefault()
    setErro('')
    const inicio = new Date(`${data}T${formAula.horaInicio}:00`)
    const fim = new Date(inicio.getTime() + formAula.duracao * 60000)
    try {
      await aulaService.criar({
        professorId: Number(formAula.professorId),
        quadraId: Number(formAula.quadraId),
        tipo: formAula.tipo,
        inicio: format(inicio, "yyyy-MM-dd'T'HH:mm:ss"),
        fim: format(fim, "yyyy-MM-dd'T'HH:mm:ss"),
        alunos: formAula.alunos ? formAula.alunos.split(',').map((s) => s.trim()).filter(Boolean) : [],
        valor: formAula.valor ? Number(formAula.valor) : null,
      })
      setFormAula({ ...formAula, alunos: '', valor: '' })
      carregarAulas()
    } catch (err) {
      if (err.response?.status === 409) {
        setErro('Este professor já tem uma aula nesse horário.')
      } else {
        setErro('Não foi possível criar a aula.')
      }
    }
  }

  async function handleRemover(id) {
    if (!confirm('Remover esta aula?')) return
    try {
      await aulaService.remover(id)
      carregarAulas()
    } catch {
      setErro('Não foi possível remover a aula.')
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

      <div className="flex gap-2 mb-6 border-b border-gray-200">
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

      {erro && <p className="text-red-600 text-sm mb-4">{erro}</p>}

      {aba === 'avulsas' && (
      <>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <form onSubmit={handleCriarProfessor} className="bg-white rounded-xl shadow p-6 space-y-3">
          <h3 className="font-semibold text-araca-azul">Cadastrar professor</h3>
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
          <div className="flex gap-3">
            <select
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formProfessor.especialidade}
              onChange={(e) => setFormProfessor({ ...formProfessor, especialidade: e.target.value })}
            >
              {ESPECIALIDADES.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
            </select>
            <input
              type="number"
              step="0.01"
              className="w-32 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              placeholder="% comissão"
              value={formProfessor.percentualComissao}
              onChange={(e) => setFormProfessor({ ...formProfessor, percentualComissao: e.target.value })}
            />
          </div>
          <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
            Adicionar professor
          </button>
          <ul className="text-sm text-gray-600 pt-2 space-y-1">
            {professores.map((p) => (
              <li key={p.id}>{p.nome} · {ESPECIALIDADES.find(e => e.value === p.especialidade)?.label}</li>
            ))}
          </ul>
        </form>

        <form onSubmit={handleCriarAula} className="bg-white rounded-xl shadow p-6 space-y-3">
          <h3 className="font-semibold text-araca-azul">Agendar aula</h3>
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
          <div className="flex gap-3">
            <select
              className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
              value={formAula.tipo}
              onChange={(e) => setFormAula({ ...formAula, tipo: e.target.value })}
            >
              {TIPOS_AULA.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
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
              <option value={30}>30 min</option>
              <option value={60}>1 hora</option>
              <option value={90}>1h30</option>
            </select>
          </div>
          <input
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            placeholder="Alunos (separados por vírgula)"
            value={formAula.alunos}
            onChange={(e) => setFormAula({ ...formAula, alunos: e.target.value })}
          />
          <input
            type="number"
            step="0.01"
            min="0"
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
            placeholder="Valor da aula (R$) — opcional, usado no cálculo de comissão"
            value={formAula.valor}
            onChange={(e) => setFormAula({ ...formAula, valor: e.target.value })}
          />
          <button type="submit" className="w-full bg-araca-verde text-araca-azul font-semibold py-2 rounded-lg hover:opacity-90 transition">
            Agendar aula
          </button>
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
            <li key={a.id} className="flex items-center justify-between border rounded-lg px-4 py-3">
              <div>
                <p className="font-medium text-araca-azul">
                  {format(new Date(a.inicio), 'HH:mm')} — {format(new Date(a.fim), 'HH:mm')} · {a.professor?.nome}
                </p>
                <p className="text-sm text-gray-500">
                  {a.quadra?.nome} · {TIPOS_AULA.find(t => t.value === a.tipo)?.label}
                  {a.valor ? ` · R$ ${Number(a.valor).toFixed(2)}` : ''}
                  {a.alunos?.length > 0 ? ` · ${a.alunos.join(', ')}` : ''}
                </p>
              </div>
              <button onClick={() => handleRemover(a.id)} className="text-red-600 text-sm hover:underline">
                Remover
              </button>
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
              <p className="text-xs text-gray-500">% comissão</p>
              <p className="text-xl font-bold text-araca-azul">{Number(comissao.percentualComissao).toFixed(1)}%</p>
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
            <h3 className="font-semibold text-araca-azul">Nova turma</h3>
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
              <select
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                value={formTurma.diaSemana}
                onChange={(e) => setFormTurma({ ...formTurma, diaSemana: e.target.value })}
              >
                {DIAS_SEMANA.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
              </select>
              <div className="flex gap-3">
                <input
                  type="time"
                  className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formTurma.horaInicio}
                  onChange={(e) => setFormTurma({ ...formTurma, horaInicio: e.target.value })}
                />
                <input
                  type="time"
                  className="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                  value={formTurma.horaFim}
                  onChange={(e) => setFormTurma({ ...formTurma, horaFim: e.target.value })}
                />
              </div>
              <input
                type="number"
                min="1"
                className="border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-araca-verde"
                placeholder="Capacidade máxima (opcional)"
                value={formTurma.capacidadeMaxima}
                onChange={(e) => setFormTurma({ ...formTurma, capacidadeMaxima: e.target.value })}
              />
            </div>
            <button type="submit" className="bg-araca-verde text-araca-azul font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition">
              Criar turma
            </button>
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
                        {DIAS_SEMANA.find((d) => d.value === t.diaSemana)?.label} · {t.horaInicio}–{t.horaFim} ·{' '}
                        {t.professorNome} · {t.quadraNome} · {t.matriculasAtivas}
                        {t.capacidadeMaxima ? `/${t.capacidadeMaxima}` : ''} aluno(s)
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleExpandirTurma(t)}
                        className="text-araca-verde-escuro text-sm hover:underline"
                      >
                        {turmaExpandidaId === t.id ? 'Fechar' : 'Ver alunos'}
                      </button>
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
