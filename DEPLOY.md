# Deploy da Araça Beach em produção (VPS + Docker)

Guia passo a passo para colocar o sistema no ar numa VPS Ubuntu, usando
Docker. Sem domínio configurado ainda — o sistema fica acessível pelo
**IP da VPS** por enquanto; quando o DNS estiver pronto, é só apontar o
domínio pro mesmo IP e (opcionalmente) adicionar HTTPS depois (veja o
final deste guia).

## 1. Instalar o Docker na VPS

Conecte via SSH na VPS e rode:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

Depois do `usermod`, **saia e entre de novo no SSH** (para o grupo
`docker` valer pro seu usuário sem precisar de `sudo` em todo comando).

Confirme que instalou certo:

```bash
docker --version
docker compose version
```

## 2. Enviar o projeto para a VPS

Na sua máquina (Windows), com o projeto já na pasta
`C:\Users\DELL\Documents\araca-beach`, comprima a pasta inteira num
`.zip` e envie com `scp` (rode isso no PowerShell, ajustando o IP):

```powershell
scp -r C:\Users\DELL\Documents\araca-beach usuario@SEU_IP_AQUI:~/araca-beach
```

(Troque `usuario` pelo usuário que você usa pra conectar na VPS.)

Se preferir, também dá pra usar um cliente gráfico como o **WinSCP** e
arrastar a pasta.

## 3. Configurar as variáveis de ambiente

Já na VPS, dentro da pasta do projeto:

```bash
cd ~/araca-beach
cp .env.example .env
nano .env
```

Preencha:
- `DB_PASSWORD` e `DB_ROOT_PASSWORD` — senhas fortes e diferentes uma da
  outra
- `JWT_SECRET` — gere uma chave longa e aleatória com:
  ```bash
  openssl rand -base64 48
  ```
- `MAIL_*` e `EMAIL_ALERTAS` — só se for usar as notificações por e-mail
  (pode deixar em branco por enquanto)

Salve com `Ctrl+O`, `Enter`, e saia com `Ctrl+X`.

## 4. Subir o sistema

```bash
docker compose up -d --build
```

A primeira vez demora alguns minutos (baixa as imagens, compila o
backend, builda o frontend). Depois disso, acompanhe os logs:

```bash
docker compose logs -f
```

(`Ctrl+C` só sai do acompanhamento de logs — os containers continuam
rodando em segundo plano.)

## 5. Liberar a porta 80 no firewall

Se a VPS tiver o `ufw` ativo:

```bash
sudo ufw allow 80/tcp
sudo ufw allow OpenSSH
sudo ufw enable
```

## 6. Acessar o sistema

Abra no navegador: `http://SEU_IP_AQUI`

O usuário administrador padrão (`admin` / `admin123`) é criado
automaticamente na primeira subida — troque a senha pela tela de
Usuários assim que acessar.

## Comandos úteis do dia a dia

```bash
docker compose ps                 # ver status dos containers
docker compose logs -f backend    # ver só os logs do backend
docker compose restart backend    # reiniciar só o backend
docker compose down               # parar tudo (os dados do banco continuam salvos)
docker compose up -d --build      # reconstruir e subir de novo (após alterar código)
```

## Quando o domínio estiver pronto

1. Aponte o DNS do domínio (registro tipo `A`) pro IP da VPS.
2. Para adicionar HTTPS gratuito, a forma mais simples é instalar o
   [Certbot](https://certbot.eff.org/) direto na VPS (fora do Docker) e
   apontar ele pro Nginx do container `frontend`, ou trocar o
   `frontend/Dockerfile` para usar um proxy reverso com
   [nginx-proxy + acme-companion](https://github.com/nginx-proxy/acme-companion).
   Quando chegar nessa etapa, me chama que ajusto os arquivos certinho
   pro seu domínio.
