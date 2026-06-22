package com.pucrs.plus_ms_categorias.exception;

import java.time.OffsetDateTime;

public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}