package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando se intenta añadir un ContainerBlock dentro de la secuencia
 * interna de otro ContainerBlock (anidamiento de más de un nivel no permitido).
 * Corresponde al Requisito 8.6.
 */
public class ContainerNestingException extends SyntaxAnalyzerException {

    public ContainerNestingException() {
        super("No se permiten agrupaciones anidadas más de un nivel. "
                + "No es posible añadir un ContainerBlock dentro de otro ContainerBlock.");
    }

    public ContainerNestingException(String message) {
        super(message);
    }
}
