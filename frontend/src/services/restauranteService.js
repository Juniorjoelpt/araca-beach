import api from './api.js'

const base = '/restaurante'
const gestao = '/restaurante/gestao'

export const restauranteService = {
  // operacao (caixa)
  cardapio: () => api.get(`${base}/cardapio`).then((r) => r.data),
  comandasAbertas: () => api.get(`${base}/comandas/abertas`).then((r) => r.data),
  comanda: (id) => api.get(`${base}/comandas/${id}`).then((r) => r.data),
  abrirComanda: (dados) => api.post(`${base}/comandas`, dados).then((r) => r.data),
  lancarPedido: (id, itens) => api.post(`${base}/comandas/${id}/pedidos`, { itens }).then((r) => r.data),
  cancelarItem: (id, itemId, motivo) =>
    api.delete(`${base}/comandas/${id}/itens/${itemId}`, { params: motivo ? { motivo } : {} }).then((r) => r.data),
  trocarMesa: (id, mesa) => api.patch(`${base}/comandas/${id}/mesa`, { mesa }).then((r) => r.data),
  taxaServico: (id, percentual) => api.patch(`${base}/comandas/${id}/taxa-servico`, { percentual }).then((r) => r.data),
  pagar: (id, valor, forma) => api.post(`${base}/comandas/${id}/pagamentos`, { valor, forma }).then((r) => r.data),
  removerPagamento: (id, pagamentoId) => api.delete(`${base}/comandas/${id}/pagamentos/${pagamentoId}`).then((r) => r.data),
  fechar: (id) => api.patch(`${base}/comandas/${id}/fechar`).then((r) => r.data),
  cancelarComanda: (id, motivo) =>
    api.patch(`${base}/comandas/${id}/cancelar`, null, { params: motivo ? { motivo } : {} }).then((r) => r.data),

  // reservas de mesa
  reservasMesa: (data) => api.get(`${base}/reservas-mesa`, { params: data ? { data } : {} }).then((r) => r.data),
  criarReservaMesa: (dados) => api.post(`${base}/reservas-mesa`, dados).then((r) => r.data),
  mesaDaReserva: (id, mesa) => api.patch(`${base}/reservas-mesa/${id}/mesa`, { mesa }).then((r) => r.data),
  cancelarReservaMesa: (id) => api.patch(`${base}/reservas-mesa/${id}/cancelar`).then((r) => r.data),
  naoCompareceuMesa: (id) => api.patch(`${base}/reservas-mesa/${id}/nao-compareceu`).then((r) => r.data),

  // gestao (ADMIN)
  categorias: () => api.get(`${gestao}/categorias`).then((r) => r.data),
  criarCategoria: (d) => api.post(`${gestao}/categorias`, d).then((r) => r.data),
  atualizarCategoria: (id, d) => api.put(`${gestao}/categorias/${id}`, d).then((r) => r.data),
  itens: () => api.get(`${gestao}/itens`).then((r) => r.data),
  criarItem: (d) => api.post(`${gestao}/itens`, d).then((r) => r.data),
  atualizarItem: (id, d) => api.put(`${gestao}/itens/${id}`, d).then((r) => r.data),
  pausarItem: (id, pausado) => api.patch(`${gestao}/itens/${id}/pausa`, { pausado }).then((r) => r.data),
  definirFicha: (id, linhas, custoProducao) => api.put(`${gestao}/itens/${id}/ficha`, { linhas, custoProducao }).then((r) => r.data),
  insumos: () => api.get(`${gestao}/insumos`).then((r) => r.data),
  criarInsumo: (d) => api.post(`${gestao}/insumos`, d).then((r) => r.data),
  atualizarInsumo: (id, d) => api.put(`${gestao}/insumos/${id}`, d).then((r) => r.data),
  movimentos: (id) => api.get(`${gestao}/insumos/${id}/movimentos`).then((r) => r.data),
  movimentarInsumo: (id, d) => api.post(`${gestao}/insumos/${id}/movimentos`, d).then((r) => r.data),
  desconto: (id, valor, motivo) => api.patch(`${gestao}/comandas/${id}/desconto`, { valor, motivo }).then((r) => r.data),
  relatorio: (inicio, fim) => api.get(`${gestao}/relatorio`, { params: { inicio, fim } }).then((r) => r.data),
}
