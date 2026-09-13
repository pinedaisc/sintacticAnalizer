package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando el archivo JSON de estado del Canvas contiene datos inválidos:
 * JSON malformado, campos requeridos ausentes, o tipos de bloque no definidos
 * en la Grammar activa.
 * El campo {@code reason} describe la causa específica del error.
 * Corresponde al Requisito 6.3.
 */
public class InvalidCanvasStateException extends SyntaxAnalyzerException {

    private final String reason;

    public InvalidCanvasStateException(String reason) {
        super("Estado del Canvas inválido: " + reason);
        this.reason = reason;
    }

    public InvalidCanvasStateException(String reason, Throwable cause) {
        super("Estado del Canvas inválido: " + reason, cause);
        this.reason = reason;
    }

    /**
     * @return la descripción de la causa que hizo inválido el estado del Canvas.
     */
    public String getReason() {
        return reason;
    }
}
