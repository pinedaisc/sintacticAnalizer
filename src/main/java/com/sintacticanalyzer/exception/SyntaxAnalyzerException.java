package com.sintacticanalyzer.exception;

/**
 * Clase base abstracta para todas las excepciones del Analizador Sintáctico.
 * Todas las subclases son unchecked (RuntimeException).
 */
public abstract class SyntaxAnalyzerException extends RuntimeException {

    protected SyntaxAnalyzerException(String message) {
        super(message);
    }

    protected SyntaxAnalyzerException(String message, Throwable cause) {
        super(message, cause);
    }
}
