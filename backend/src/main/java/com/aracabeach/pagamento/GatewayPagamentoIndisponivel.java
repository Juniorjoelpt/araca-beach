package com.aracabeach.pagamento;

import com.aracabeach.domain.reserva.Reserva;
import org.springframework.stereotype.Service;

/**
 * Implementacao provisoria: nenhum gateway de pagamento foi configurado
 * ainda (o sistema, por enquanto, so oferece agendamento sem pagamento
 * online - o pagamento continua sendo feito na recepcao, como hoje).
 * Nao esta ligada a nenhum fluxo em uso no momento; existe apenas como
 * scaffolding para quando o pagamento online for implementado (veja as
 * instrucoes em GatewayPagamento).
 */
@Service
public class GatewayPagamentoIndisponivel implements GatewayPagamento {

    @Override
    public DadosCobranca gerarCobranca(Reserva reserva) {
        throw new UnsupportedOperationException(
                "Pagamento online ainda não foi configurado neste sistema. " +
                "Veja as instruções em GatewayPagamento.java para implementar a integração."
        );
    }
}
