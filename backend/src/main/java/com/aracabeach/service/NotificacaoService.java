package com.aracabeach.service;

import com.aracabeach.domain.produto.Produto;
import com.aracabeach.domain.reserva.Reserva;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envia notificacoes por e-mail para o cliente (confirmacao, cancelamento,
 * lembrete). Falhas de envio (SMTP nao configurado, credenciais invalidas,
 * cliente sem e-mail cadastrado, etc.) NUNCA devem interromper a operacao
 * de negocio (criar/cancelar reserva) - por isso todo envio e protegido
 * por try/catch e apenas registrado em log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;

    @Value("${araca-beach.notificacoes.email.remetente}")
    private String remetente;

    @Value("${araca-beach.notificacoes.email.destinatario-alertas:}")
    private String destinatarioAlertas;

    /**
     * Envia um resumo dos produtos com estoque baixo/zerado para o e-mail
     * interno configurado (araca-beach.notificacoes.email.destinatario-alertas).
     * Se nao houver destinatario configurado, nao tenta enviar.
     */
    public void enviarAlertaEstoqueBaixo(List<Produto> produtos) {
        if (destinatarioAlertas == null || destinatarioAlertas.isBlank() || produtos.isEmpty()) {
            return;
        }

        StringBuilder lista = new StringBuilder();
        for (Produto p : produtos) {
            lista.append("- %s: %d unidade(s)%n".formatted(p.getNome(), p.getEstoque()));
        }

        String assunto = "Alerta de estoque baixo - Araça Beach";
        String corpo = """
                Os seguintes produtos estão com estoque baixo ou zerado:

                %s
                Acesse o sistema para repor o estoque.
                Araça Beach - Vôlei, Futevôlei e Beach Tennis
                """.formatted(lista);

        enviar(destinatarioAlertas, assunto, corpo);
    }

    public void enviarConfirmacaoReserva(Reserva reserva) {
        String email = emailDoCliente(reserva);
        if (email == null) return;

        String assunto = "Reserva confirmada - Araça Beach";
        String corpo = """
                Olá, %s!

                Sua reserva na Araça Beach foi confirmada:

                Quadra: %s
                Data e horário: %s
                Valor: R$ %s

                Nos vemos lá!
                Araça Beach - Vôlei, Futevôlei e Beach Tennis
                """.formatted(
                reserva.getCliente().getNome(),
                reserva.getQuadra().getNome(),
                reserva.getInicio().format(FORMATO_DATA_HORA),
                reserva.getValorTotal() != null ? reserva.getValorTotal().toString() : "a combinar"
        );

        enviar(email, assunto, corpo);
    }

    public void enviarCancelamentoReserva(Reserva reserva) {
        String email = emailDoCliente(reserva);
        if (email == null) return;

        String assunto = "Reserva cancelada - Araça Beach";
        String corpo = """
                Olá, %s!

                Sua reserva na Araça Beach foi cancelada:

                Quadra: %s
                Data e horário: %s

                Qualquer dúvida, fale com a nossa recepção.
                Araça Beach - Vôlei, Futevôlei e Beach Tennis
                """.formatted(
                reserva.getCliente().getNome(),
                reserva.getQuadra().getNome(),
                reserva.getInicio().format(FORMATO_DATA_HORA)
        );

        enviar(email, assunto, corpo);
    }

    public void enviarLembrete(Reserva reserva) {
        String email = emailDoCliente(reserva);
        if (email == null) return;

        String assunto = "Lembrete: sua reserva é hoje - Araça Beach";
        String corpo = """
                Olá, %s!

                Passando para lembrar da sua reserva de hoje:

                Quadra: %s
                Horário: %s

                Até já!
                Araça Beach - Vôlei, Futevôlei e Beach Tennis
                """.formatted(
                reserva.getCliente().getNome(),
                reserva.getQuadra().getNome(),
                reserva.getInicio().format(FORMATO_DATA_HORA)
        );

        enviar(email, assunto, corpo);
    }

    private String emailDoCliente(Reserva reserva) {
        String email = reserva.getCliente() != null ? reserva.getCliente().getEmail() : null;
        if (email == null || email.isBlank()) {
            log.debug("Cliente da reserva {} não tem e-mail cadastrado - notificação não enviada.", reserva.getId());
            return null;
        }
        return email;
    }

    private void enviar(String destinatario, String assunto, String corpo) {
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(destinatario);
            mensagem.setFrom(remetente);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            mailSender.send(mensagem);
            log.info("E-mail '{}' enviado para {}", assunto, destinatario);
        } catch (MailException e) {
            log.warn("Não foi possível enviar e-mail '{}' para {}: {}", assunto, destinatario, e.getMessage());
        } catch (Exception e) {
            log.warn("Erro inesperado ao enviar e-mail para {}: {}", destinatario, e.getMessage());
        }
    }
}
