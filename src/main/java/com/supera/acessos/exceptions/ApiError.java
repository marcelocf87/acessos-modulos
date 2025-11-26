package com.supera.acessos.exceptions;

import java.time.LocalDateTime;

public record ApiError(
        String mensagem,
        int status,
        LocalDateTime timestamp
) {}
