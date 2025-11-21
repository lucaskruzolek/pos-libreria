package com.libreria.core.exceptions;

import com.libreria.core.models.dto.PrecioRequest;

public class PrecioNoConfiguradoException extends RuntimeException {
    public PrecioNoConfiguradoException(PrecioRequest req) {
        super(String.format(
                "No existe precio configurado en la Matriz para: [%s | %s | %s | %s]",
                req.tamano(), req.tipoPapel(), req.color(), req.faz()
        ));
    }
}