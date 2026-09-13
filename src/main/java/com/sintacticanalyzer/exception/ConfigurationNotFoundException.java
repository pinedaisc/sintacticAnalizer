package com.sintacticanalyzer.exception;

/**
 * Lanzada cuando el archivo de configuración YAML no existe en la ruta especificada.
 * Corresponde al Requisito 1.3.
 */
public class ConfigurationNotFoundException extends SyntaxAnalyzerException {

    public ConfigurationNotFoundException(String path) {
        super("Archivo de configuración no encontrado en la ruta: " + path);
    }

    public ConfigurationNotFoundException(String path, Throwable cause) {
        super("Archivo de configuración no encontrado en la ruta: " + path, cause);
    }
}
