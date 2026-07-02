package com.turnosmart.turnosmart_backend.entity;

public enum AppointmentStatus {
    SOLICITADO, REVISION, REGULARIZAR, CONFORME, REDACCION,
    DOCUMENTOS_ENVIADOS,
    LISTO_FIRMA, FIRMADO, PROTOCOLIZACION, ENTREGADO, CANCELADO,
    PENDIENTE_EVALUACION, PROCESO_DETENIDO;

    public String getLabel() {
        return switch (this) {
            case SOLICITADO          -> "Solicitado";
            case REVISION            -> "En Revisión";
            case REGULARIZAR         -> "Observado - Requiere Subsanación";
            case CONFORME            -> "Aprobado - Subir Documentos";
            case REDACCION           -> "En Redacción";
            case DOCUMENTOS_ENVIADOS -> "Documentos en Validación";
            case LISTO_FIRMA         -> "Listo para Firma";
            case FIRMADO             -> "Firmado";
            case PROTOCOLIZACION     -> "En Protocolización";
            case ENTREGADO           -> "Trámite Completado";
            case CANCELADO           -> "Cancelado";
            case PENDIENTE_EVALUACION -> "En Revisión";
            case PROCESO_DETENIDO    -> "Observado - Requiere Subsanación";
        };
    }

    
    public String getColorClass() {
        return switch (this) {
            case SOLICITADO, PENDIENTE_EVALUACION, REVISION, REDACCION ->
                    "bg-warning-subtle text-warning-emphasis";
            case CONFORME, LISTO_FIRMA, FIRMADO ->
                    "bg-success-subtle text-success-emphasis";
            case REGULARIZAR, PROCESO_DETENIDO ->
                    "bg-danger-subtle text-danger-emphasis";
            case DOCUMENTOS_ENVIADOS, PROTOCOLIZACION ->
                    "bg-info-subtle text-info-emphasis";
            case ENTREGADO ->
                    "bg-success text-white";
            case CANCELADO ->
                    "bg-secondary-subtle text-secondary-emphasis";
        };
    }
}