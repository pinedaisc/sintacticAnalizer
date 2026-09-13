package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando se intenta realizar una operación sobre el Canvas
 * sin que se haya cargado previamente una Grammar.
 * Corresponde al Requisito 2.5.
 */
public class NoGrammarLoadedException extends SyntaxAnalyzerException {

    public NoGrammarLoadedException() {
        super("No hay gramática activa. Cargue un archivo de configuración antes de operar con el Canvas.");
    }

    public NoGrammarLoadedException(String message) {
        super(message);
    }
}
