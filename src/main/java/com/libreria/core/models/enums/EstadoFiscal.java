package com.libreria.core.models.enums;

public enum EstadoFiscal {
    NO_REQUIERE,    // Ticket X o venta interna
    PENDIENTE,      // Lista para ser enviada al worker de AFIP
    ENVIADA_AFIP,   // En proceso
    FACTURADA_OK,   // CAE obtenido
    ERROR           // Rechazada
}
