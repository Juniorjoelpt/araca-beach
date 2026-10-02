# ---- Etapa 1: build dos arquivos estaticos com Vite ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---- Etapa 2: servir os arquivos com Nginx ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

# nginx.conf.template fica em /etc/nginx/templates/ - a imagem oficial do
# Nginx roda envsubst automaticamente nesses arquivos ao subir o
# container, substituindo ${BACKEND_HOST} pelo valor real da variavel de
# ambiente antes de gerar o /etc/nginx/conf.d/default.conf definitivo.
# Isso permite usar o mesmo Dockerfile/imagem tanto no Docker Compose
# (local/Hostinger) quanto na Railway, so trocando o valor da variavel.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
