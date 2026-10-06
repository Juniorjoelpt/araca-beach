# Araça Beach - Sistema de Gestão da Arena

Sistema web para gestão da arena **Araça Beach** (Vôlei, Futevôlei e Beach Tennis),
cobrindo reservas de quadras, financeiro, aulas, torneios e loja/consumo.

Uso interno da equipe da arena (admin e recepção) — sem portal externo para clientes nesta versão.

## Estrutura do projeto

```
araca-beach/
├── backend/     API REST em Spring Boot 3 (Java 17)
└── frontend/    Painel web em React (Vite) + Tailwind
```

## Módulos (v1)

| Módulo      | Status neste scaffold                                             |
|-------------|---------------------------------------------------------------------|
| Quadras     | Entidade, repositório e API REST completos                         |
| Reservas    | Entidade, service com regra de conflito de horário, API REST       |
| Clientes    | Entidade, repositório e API REST completos                         |
| Usuários    | Login/autenticação JWT, perfis ADMIN e RECEPÇÃO                    |
| Financeiro  | Entidades modeladas (Pagamento); service e endpoints a implementar |
| Aulas       | Entidades modeladas (Professor, Aula); service e endpoints a implementar |
| Torneios    | Entidades modeladas (Torneio, Inscrição); service e endpoints a implementar |
| Loja        | Entidades modeladas (Produto, Comanda, ItemComanda); service e endpoints a implementar |
| Dashboard   | Tela criada no frontend; indicadores a conectar com o backend      |

## Backend

**Requisitos:** Java 17+, Maven instalado (`mvn -version` para conferir), MySQL Server rodando localmente (ou acessível via rede).

### Banco de dados (MySQL Workbench)

1. Abra o MySQL Workbench e conecte na sua instância local do MySQL.
2. Rode o script `backend/sql/01-criar-banco.sql` (cria o banco `aracabeach`).
3. Inicie o backend pelo menos uma vez (veja abaixo) — o Hibernate cria as
   tabelas automaticamente na primeira execução.
4. O usuário administrador (`login: admin` / `senha: admin123`) é criado
   **automaticamente** na primeira vez que o backend sobe (veja
   `DataSeeder.java`) — não é mais necessário rodar `02-usuario-admin.sql`
   manualmente. Troque a senha pela tela **Usuários** após o primeiro acesso.
5. Se sua senha/porta/usuário do MySQL forem diferentes do padrão
   (`root`/`root`/`3306`), copie `backend/.env.example` para `.env` (ou
   configure variáveis de ambiente) com os valores corretos.

### Rodando pelo terminal

```bash
cd backend
mvn spring-boot:run
```

### Rodando pelo NetBeans

1. Abra o NetBeans → **File → Open Project** → selecione a pasta `backend`
   (a que contém o `pom.xml`). O NetBeans reconhece automaticamente como
   projeto Maven, baixa as dependências e monta a árvore de pacotes.
2. Confirme que o NetBeans está usando o **JDK 17** (Tools → Java Platforms;
   se não tiver o 17 instalado, adicione o path da instalação).
3. Clique com o botão direito no projeto → **Run** (ou o ícone de play verde).
   A classe principal é `com.aracabeach.AracaBeachApplication`.
4. O console do NetBeans mostra o log do Spring Boot; procure por
   `Started AracaBeachApplication` confirmando que subiu em `http://localhost:8080`.

- Documentação da API (Swagger): http://localhost:8080/swagger-ui.html

### Autenticação

Não há endpoint de cadastro de usuário ainda — o primeiro usuário (admin) deve
ser inserido diretamente no banco (ou via um `CommandLineRunner` a ser criado)
com a senha já em hash BCrypt. Depois disso, `POST /api/auth/login` retorna o
token JWT a ser usado no header `Authorization: Bearer <token>`.

### Regra de negócio já implementada

`ReservaService` impede a criação de reservas com **sobreposição de horário**
na mesma quadra, calcula o valor total da reserva (`valorHora x duração`) e
lança `ConflitoHorarioException` (HTTP 409) quando há choque de agenda.

### Notificações automáticas por e-mail

O sistema envia e-mail automaticamente para o cliente (usando o campo
`email` cadastrado em Clientes) em três momentos:
- **Confirmação**, ao criar uma reserva avulsa
- **Cancelamento**, ao cancelar uma reserva
- **Lembrete**, enviado por uma tarefa agendada (`LembreteReservaScheduler`)
  que roda a cada 15 minutos e dispara o lembrete configurado em
  `araca-beach.notificacoes.email.horas-antes-lembrete` (padrão: 2 horas
  antes do horário da reserva)

Isso **não é obrigatório configurar** — se as variáveis `MAIL_*` não forem
definidas, o sistema tenta enviar, falha silenciosamente e registra apenas
um aviso no log, sem interromper a criação/cancelamento da reserva. Veja
`backend/.env.example` para o passo a passo de configuração com Gmail.
Clientes sem e-mail cadastrado simplesmente não recebem nada (sem erro).

As reservas geradas por **recorrência** não disparam e-mail de confirmação
individual (evita gerar dezenas de e-mails de uma vez), mas participam
normalmente do lembrete automático.

## Frontend

**Requisitos:** Node.js 18+.

```bash
cd frontend
npm install
npm run dev
```

- Acesse http://localhost:5173
- O Vite já está configurado com proxy de `/api` para `http://localhost:8080`.
- Identidade visual da Araça Beach aplicada em `tailwind.config.js`
  (`araca-azul`, `araca-verde`, etc.) e em `src/theme/colors.js`.
- Logos e o manual de marca estão em `src/assets/logos/` para referência.
- Fontes do manual de marca (Ancorli para títulos, Poppins para texto corrido)
  já configuradas no Tailwind — falta importar os arquivos de fonte reais
  (Google Fonts ou arquivos próprios) em `index.css`.

## Portal do Cliente (mobile)

Além do painel interno da equipe, o sistema tem um portal separado onde o
próprio cliente se cadastra, entra e reserva uma quadra pelo navegador do
celular (não é um app nativo — é uma web app responsiva, acessada pelo
mesmo endereço do sistema).

- **Cadastro**: `/portal/cadastro`
- **Login**: `/portal/entrar`
- **Agendar**: `/portal` (após login)
- **Minhas reservas**: `/portal/minhas-reservas`

O cliente usa e-mail + senha, separado do login da equipe (`admin`/`admin123`
etc.) — são sistemas de autenticação independentes, com tokens diferentes,
mesmo compartilhando o mesmo backend. Se um cliente já existia no cadastro
interno (criado pela recepção) e se cadastra no portal com o mesmo e-mail,
o sistema aproveita o registro existente em vez de duplicar.

**Horário de funcionamento do portal** (janela de horários que o cliente
pode reservar) é configurável em `araca-beach.portal.*` no
`application.yml` — por padrão, das 6h às 23h, em slots de 1 hora.

**Pagamento**: por enquanto, a reserva feita pelo portal já é confirmada
sem pagamento online (o cliente paga na recepção, como as reservas feitas
por telefone). A estrutura para adicionar pagamento online (Pix/cartão) já
está no código — veja os comentários em
`backend/src/main/java/com/aracabeach/pagamento/GatewayPagamento.java`
para o passo a passo de como plugar um gateway (Mercado Pago, Asaas, etc.)
quando for a hora.

## Regras de reserva, pacotes de aulas e comissões

- **Regras de reserva** (menu *Regras de reserva*, só ADMIN): cancelamento gratuito até X horas antes,
  multa de cancelamento tardio e de no-show, desconto de mensalista, preço por horário (pico/fora de pico),
  bloqueio de quadra (manutenção/evento) e lista de espera (e-mail quando o horário libera).
  Padrões: 24h grátis, multa 50%, no-show 100%, desconto 0%.
- **Pacotes de aulas** (menu *Pacotes de aulas*): planos, venda, saldo de aulas, agendamento (reserva a quadra),
  chamada/presença e reposição (falta avisada devolve o crédito).
- **Comissões** (menu *Comissões*, só ADMIN): lançadas automaticamente por aula avulsa, mensalidade de turma paga
  e aula de pacote realizada; pagamento em lote com lançamento opcional em Despesas.
- **Portal do cliente**: preço por horário, lista de espera, aviso de multa ao cancelar, aba *Aulas* (pacotes, saldo,
  avisar falta) e aba *Conta* (cobranças em aberto).
- **Contas do portal**: o cadastro exige confirmar o e-mail (link válido por 24h) antes do primeiro login, e há
  recuperação de senha por e-mail. Precisa de SMTP configurado (`MAIL_*`) e da variável `PORTAL_URL` (endereço público
  do site, usado nos links). Contas criadas antes desta versão continuam entrando normalmente. Se o e-mail não chegar,
  a recepção confirma manualmente em *Clientes → Confirmar e-mail*. Para desligar a exigência:
  `PORTAL_EXIGIR_CONFIRMACAO_EMAIL=false`.
- Ao atualizar um banco existente, rode `backend/sql/03-migracao-regras-pacotes-comissao.sql` uma vez.

## Deploy em produção

Para colocar o sistema no ar numa VPS com Docker, veja o guia completo
em [`DEPLOY.md`](./DEPLOY.md).

## Próximos passos sugeridos

1. Implementar os services e controllers dos módulos Financeiro, Aulas,
   Torneios e Loja (entidades e repositórios já prontos).
2. Construir a tela de calendário/agenda no frontend (Módulo Agenda),
   consumindo `GET /api/reservas/agenda`.
3. Criar um `CommandLineRunner` (ou endpoint protegido) para cadastro do
   primeiro usuário administrador.
4. Adicionar testes automatizados (unitários no `ReservaService`, principalmente
   a regra de conflito de horário).
5. Definir o fluxo de fechamento de caixa e relatórios do módulo Financeiro.
