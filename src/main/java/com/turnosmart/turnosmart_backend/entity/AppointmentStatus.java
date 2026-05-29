package com.turnosmart.turnosmart_backend.entity;

public enum AppointmentStatus {
    SOLICITADO,
    REVISION,
    REGULARIZAR,
    CONFORME,
    REDACCION,
    LISTO_FIRMA,
    FIRMADO,
    PROTOCOLIZACION,
    ENTREGADO,
    CANCELADO;

    public String getLabel() {
        return switch (this) {
            case SOLICITADO      -> "Solicitado";
            case REVISION        -> "En Revisión";
            case REGULARIZAR     -> "Por Regularizar";
            case CONFORME        -> "Conforme / Aprobado";
            case REDACCION       -> "En Redacción";
            case LISTO_FIRMA     -> "Listo para Firma";
            case FIRMADO         -> "Firmado";
            case PROTOCOLIZACION -> "En Protocolización";
            case ENTREGADO       -> "Entregado";
            case CANCELADO       -> "Cancelado";
        };
    }
}