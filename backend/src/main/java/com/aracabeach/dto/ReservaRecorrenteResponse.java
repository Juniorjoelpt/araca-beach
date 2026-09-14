package com.aracabeach.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Retornado ao criar uma recorrencia: alem dos dados da regra, informa
 * quais datas tiveram reserva efetivamente criada e quais ficaram de fora
 * por conflito de horario (para a recepcao decidir o que fazer com elas).
 */
public record ReservaRecorrenteResponse(
        Long id,
        String quadraNome,
        String clienteNome,
        String diaSemana,
        String horaInicio,
        String horaFim,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        boolean ativa,
        List<LocalDate> datasCriadas,
        List<LocalDate> datasComConflito
) {
}
