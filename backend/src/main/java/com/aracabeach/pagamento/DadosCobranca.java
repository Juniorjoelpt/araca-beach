package com.aracabeach.pagamento;

import java.math.BigDecimal;

/**
 * Dados devolvidos por um gateway de pagamento ao gerar uma cobranca -
 * link de pagamento (ex: checkout do Mercado Pago) e/ou o codigo Pix
 * "copia e cola" para o cliente pagar direto pelo app do banco.
 */
public record DadosCobranca(
        String idExterno,
        BigDecimal valor,
        String linkPagamento,
        String pixCopiaECola
) {
}
