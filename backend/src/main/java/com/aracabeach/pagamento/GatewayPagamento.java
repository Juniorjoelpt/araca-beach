package com.aracabeach.pagamento;

import com.aracabeach.domain.reserva.Reserva;

/**
 * Contrato para integracao com um gateway de pagamento (Mercado Pago,
 * Asaas, etc), preparado para quando o pagamento online do portal do
 * cliente for implementado.
 *
 * PARA IMPLEMENTAR NO FUTURO:
 * 1. Criar uma classe (ex: GatewayMercadoPago) que implementa esta
 *    interface, usando o SDK/API REST do gateway escolhido.
 * 2. Adicionar as credenciais (access token, chave publica, etc) como
 *    variaveis de ambiente no application.yml, no mesmo padrao usado
 *    para MAIL_* e JWT_SECRET (nunca hardcoded no codigo).
 * 3. Marcar a nova classe como o bean primario (@Primary) no lugar de
 *    GatewayPagamentoIndisponivel, para o Spring passar a usa-la.
 * 4. Criar um endpoint de webhook (ex: POST /api/portal/pagamentos/webhook)
 *    que o gateway chama para avisar quando um pagamento e aprovado -
 *    e so nesse momento que a reserva deve ser confirmada de fato.
 * 5. Ajustar PortalReservaService.criarReserva para, em vez de confirmar
 *    a reserva na hora, cria-la com um status de "aguardando pagamento" e
 *    chamar gerarCobranca() logo em seguida, devolvendo o link/QR code
 *    Pix para o app mostrar ao cliente.
 */
public interface GatewayPagamento {

    /** Gera a cobranca (link de pagamento e/ou Pix) referente ao valor de uma reserva. */
    DadosCobranca gerarCobranca(Reserva reserva);
}
