package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando se intenta añadir un bloque cuyo tipo no está definido
 * en la Grammar activa.
 * Corresponde al Requisito 2.4.
 */
public class UnknownBlockTypeException extends SyntaxAnalyzerException {

    private final String blockType;

    public UnknownBlockTypeException(String blockType) {
        super("Tipo de bloque no reconocido en la gramática activa: '" + blockType + "'");
        this.blockType = blockType;
    }

    /**
     * @return el tipo de bloque que no fue encontrado en la Grammar.
     */
    public String getBlockType() {
        return blockType;
    }
}
