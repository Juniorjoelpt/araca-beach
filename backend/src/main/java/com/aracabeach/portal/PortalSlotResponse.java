package com.aracabeach.portal;

import java.time.LocalDateTime;

public record PortalSlotResponse(
        LocalDateTime inicio,
        LocalDateTime fim,
        boolean disponivel
) {
}
