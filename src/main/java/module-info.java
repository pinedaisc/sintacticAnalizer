/**
 * Módulo principal del Analizador Sintáctico.
 *
 * <p>Cada paquete se exporta y se abre a JUnit y jqwik a medida que se implementan
 * sus clases en tareas sucesivas. Esta configuración base exporta únicamente el
 * paquete {@code exception}, que ya contiene tipos, y se amplía en cada tarea.
 */
module com.sintacticanalyzer {

    // ── Dependencias de producción ───────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.slf4j;

    // ── Exports: se añaden al implementar cada paquete ────────────────────────
    exports com.sintacticanalyzer.exception;

    // ── Opens para reflexión de JUnit 5 y jqwik ─────────────────────────────
    opens com.sintacticanalyzer.exception to org.junit.platform.commons, net.jqwik.engine;
}
