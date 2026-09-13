package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando el archivo de configuración YAML tiene formato inválido,
 * está malformado, o contiene conflictos entre listas allowed/forbidden.
 * El campo {@code fieldOrLine} identifica el campo o línea problemática.
 * Corresponde a los Requisitos 1.4 y 1.7.
 */
public class InvalidConfigurationException extends SyntaxAnalyzerException {

    private final String fieldOrLine;

    public InvalidConfigurationException(String fieldOrLine, String message) {
        super(message);
        this.fieldOrLine = fieldOrLine;
    }

    public InvalidConfigurationException(String fieldOrLine, String message, Throwable cause) {
        super(message, cause);
        this.fieldOrLine = fieldOrLine;
    }

    /**
     * @return el nombre del campo o la línea del YAML donde se detectó el error.
     */
    public String getFieldOrLine() {
        return fieldOrLine;
    }
}
