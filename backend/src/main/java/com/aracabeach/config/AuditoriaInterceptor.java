package com.aracabeach.config;

import com.aracabeach.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;

/**
 * Registra na trilha de auditoria toda escrita autenticada bem-sucedida
 * (POST/PUT/PATCH/DELETE em /api/**), com usuario, acao legivel, recurso, id e IP.
 * Login e fluxos de autenticacao do portal ficam de fora (nao ha usuario autenticado).
 */
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final Map<String, String> VERBOS = Map.ofEntries(
            Map.entry("cancelar", "Cancelou"),
            Map.entry("nao-compareceu", "Marcou não comparecimento de"),
            Map.entry("pagar", "Registrou pagamento de"),
            Map.entry("presenca", "Registrou presença em"),
            Map.entry("avisar-falta", "Avisou falta em"),
            Map.entry("confirmar-email", "Confirmou e-mail de"),
            Map.entry("fechar", "Fechou"),
            Map.entry("pedidos", "Lançou pedido em"),
            Map.entry("mesa", "Alterou a mesa de"),
            Map.entry("taxa-servico", "Alterou a taxa de serviço de"),
            Map.entry("desconto", "Aplicou desconto em"),
            Map.entry("ficha", "Alterou a ficha técnica de"),
            Map.entry("pausa", "Alterou a disponibilidade de"),
            Map.entry("movimentos", "Movimentou estoque de"),
            Map.entry("status", "Alterou status de"),
            Map.entry("senha", "Trocou senha de"),
            Map.entry("ajuste", "Ajustou"),
            Map.entry("aulas", "Agendou aula em"),
            Map.entry("vendas", "Vendeu"),
            Map.entry("desativar", "Desativou"),
            Map.entry("baixa", "Deu baixa em"));

    private static final Map<String, String> RECURSOS = Map.ofEntries(
            Map.entry("reservas", "reserva"),
            Map.entry("pagamentos", "pagamento"),
            Map.entry("clientes", "cliente"),
            Map.entry("usuarios", "usuário"),
            Map.entry("despesas", "despesa"),
            Map.entry("despesas-recorrentes", "despesa recorrente"),
            Map.entry("mensalidades", "mensalidade"),
            Map.entry("matriculas", "matrícula"),
            Map.entry("pacotes", "pacote de aulas"),
            Map.entry("comissoes", "comissão"),
            Map.entry("regras", "regra de reserva"),
            Map.entry("quadras", "quadra"),
            Map.entry("professores", "professor"),
            Map.entry("aulas", "aula"),
            Map.entry("turmas", "turma"),
            Map.entry("torneios", "torneio"),
            Map.entry("produtos", "produto"),
            Map.entry("estoque", "estoque"),
            Map.entry("comandas", "comanda"),
            Map.entry("lista-espera", "lista de espera"),
            Map.entry("reservas-recorrentes", "reserva recorrente"),
            Map.entry("reservas-mesa", "reserva de mesa"),
            Map.entry("insumos", "insumo"),
            Map.entry("categorias", "categoria do cardápio"));

    private final AuditoriaService auditoriaService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String metodo = request.getMethod();
            String caminho = request.getRequestURI();
            if (!METODOS.contains(metodo) || caminho == null || !caminho.startsWith("/api/")) return;
            if (caminho.startsWith("/api/auth/") || caminho.startsWith("/api/portal/auth/")) return;

            int status = response.getStatus();
            if (ex != null || status >= 400) return;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return;

            String[] seg = caminho.substring("/api/".length()).split("/");
            boolean portal = seg.length > 1 && "portal".equals(seg[0]);
            boolean restaurante = seg.length > 1 && "restaurante".equals(seg[0]);
            int base = portal || restaurante ? 1 : 0;
            if (restaurante && seg.length > 2 && "gestao".equals(seg[1])) base = 2;
            String recursoSeg = seg.length > base ? seg[base] : "";
            String recursoId = null;
            String ultimoVerbo = null;
            for (int i = base + 1; i < seg.length; i++) {
                if (seg[i].chars().allMatch(Character::isDigit)) {
                    if (recursoId == null) recursoId = seg[i];
                } else {
                    ultimoVerbo = seg[i];
                }
            }

            String nomeRecurso = RECURSOS.getOrDefault(recursoSeg, recursoSeg);
            String acao;
            if ("itens".equals(ultimoVerbo) && !"restaurante".equals(seg[0])) {
                acao = ("DELETE".equals(metodo) ? "Removeu item de " : "Adicionou item em ") + nomeRecurso;
            } else if ("restaurante".equals(seg[0]) && "itens".equals(ultimoVerbo) && recursoSeg.equals("comandas")) {
                acao = "Cancelou item de " + nomeRecurso;
            } else if ("pagamentos".equals(ultimoVerbo)) {
                acao = ("DELETE".equals(metodo) ? "Removeu pagamento de " : "Registrou pagamento em ") + nomeRecurso;
            } else if (ultimoVerbo != null && VERBOS.containsKey(ultimoVerbo)) {
                acao = VERBOS.get(ultimoVerbo) + " " + nomeRecurso;
            } else {
                String verbo = switch (metodo) {
                    case "POST" -> "Criou";
                    case "DELETE" -> "Removeu";
                    default -> "Alterou";
                };
                acao = verbo + " " + nomeRecurso;
            }
            if (portal) acao += " (portal)";

            Object detalhe = request.getAttribute(AuditoriaService.ATRIBUTO_DETALHE);
            auditoriaService.registrarRequisicao(auth, metodo, caminho, status, acao,
                    portal ? "portal/" + recursoSeg : restaurante ? "restaurante/" + recursoSeg : recursoSeg, recursoId,
                    detalhe != null ? detalhe.toString() : null, ipDe(request));
        } catch (RuntimeException e) {
            // Auditar nunca pode quebrar a resposta ja concluida.
        }
    }

    public static String ipDe(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
