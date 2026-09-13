package com.sintacticanalyzer.exception;

import java.nio.file.Path;

/**
 * Lanzada cuando ocurre un error de I/O al guardar o cargar el estado del Canvas.
 * El campo {@code filePath} indica la ruta del archivo involucrado.
 * Corresponde al Requisito 6.3.
 */
public class CanvasPersistenceException extends SyntaxAnalyzerException {

    private final Path filePath;

    public CanvasPersistenceException(Path filePath, String message) {
        super(message);
        this.filePath = filePath;
    }

    public CanvasPersistenceException(Path filePath, String message, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
    }

    /**
     * @return la ruta del archivo donde ocurrió el error de persistencia.
     */
    public Path getFilePath() {
        return filePath;
    }
}
