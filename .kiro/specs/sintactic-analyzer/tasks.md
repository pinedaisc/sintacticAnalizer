# Implementation Plan: Analizador Sintáctico

## Overview

Implementación completa de la aplicación Java 17 + JavaFX 21 del Analizador Sintáctico. El plan sigue la arquitectura en capas definida en el diseño: dominio puro → motor de análisis → carga YAML → persistencia JSON → UI JavaFX con drag & drop. Los tests de propiedades con jqwik se incorporan como sub-tareas opcionales junto a cada componente.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.2", "4.2"] },
    { "id": 3, "tasks": ["2.4", "3.3", "4.3", "5.1"] },
    { "id": 4, "tasks": ["2.5", "3.4", "4.4", "5.2"] },
    { "id": 5, "tasks": ["6.1", "6.2"] },
    { "id": 6, "tasks": ["6.3", "6.4", "7.1"] },
    { "id": 7, "tasks": ["6.5", "7.2"] },
    { "id": 8, "tasks": ["6.6", "8.1"] },
    { "id": 9, "tasks": ["6.7", "8.2"] },
    { "id": 10, "tasks": ["8.3"] },
    { "id": 11, "tasks": ["9.1"] },
    { "id": 12, "tasks": ["9.2"] }
  ]
}
```

---

## Tasks

- [ ] 1. Estructura del proyecto y fundamentos
  - [x] 1.1 Crear estructura Maven y jerarquía de paquetes
    - Crear `pom.xml` con Java 17, JavaFX 21 (via `javafx-maven-plugin`), Jackson 2.x + `jackson-dataformat-yaml`, SLF4J + Logback, JUnit 5, jqwik 1.8.4, AssertJ
    - Crear estructura de paquetes: `com.sintacticanalyzer.{analyzer,domain,engine,config,persistence,ui,exception}`
    - Crear estructura de paquetes de test: `com.sintacticanalyzer.{properties,unit,integration}`
    - Configurar `module-info.java` abriendo los paquetes necesarios a jqwik y JUnit
    - _Requirements: transversal_
  - [x] 1.2 Crear jerarquía de excepciones
    - Crear clase abstracta `SyntaxAnalyzerException extends RuntimeException`
    - Crear subclases: `ConfigurationNotFoundException`, `InvalidConfigurationException(String fieldOrLine)`, `UnknownBlockTypeException(String blockType)`, `NoGrammarLoadedException`, `ContainerNestingException`, `CanvasPersistenceException(Path filePath)`, `InvalidCanvasStateException(String reason)`
    - _Requirements: 1.3, 1.4, 1.7, 2.4, 2.5, 6.3, 7.1, 8.6_

- [ ] 2. Modelos de dominio y Grammar
  - [ ] 2.1 Implementar `ValidityState`, `SyntaxRule` y `Grammar`
    - Crear enum `ValidityState { UNCHECKED, VALID, INVALID }`
    - Crear record `SyntaxRule(String blockType, boolean container, List<String> allowedBefore, List<String> allowedAfter, List<String> forbiddenBefore, List<String> forbiddenAfter)` — listas inmutables
    - Crear clase `Grammar` con `Map<String, SyntaxRule> rulesByType` inmutable, métodos `getRuleFor`, `getKnownTypes`, `isContainerType`, `size`
    - _Requirements: 1.2, 2.1, 2.3, 3.7_
  - [ ]* 2.2 Escribir tests de propiedades para `Grammar` y `SyntaxRule`
    - **Property 5: Rechazo de tipos desconocidos** — para cualquier `Grammar` y cualquier `String` fuera de `getKnownTypes()`, `getRuleFor` debe devolver `Optional.empty()`
    - **Validates: Requirements 2.3, 2.4**
    - _Clase: `CanvasBlockTypeValidationTest`_ — 200 iteraciones
  - [ ] 2.3 Implementar `Block` y `ContainerBlock`
    - Crear clase `Block` con `id` (UUID), `blockType`, `position`, `ObjectProperty<ValidityState> validity` inicializado en `UNCHECKED`
    - Crear clase `ContainerBlock extends Block` con `ObservableList<Block> innerSequence`, método `addInnerBlock(Block, int)` que lanza `ContainerNestingException` si el bloque a añadir es `ContainerBlock`, método `removeInnerBlock(String blockId)`
    - _Requirements: 2.1, 2.2, 8.1, 8.6_
  - [ ]* 2.4 Escribir tests de propiedades para `Block` y `ContainerBlock`
    - **Property 4: Invariante de bloque recién creado** — cualquier `Block` nuevo debe tener `id` no nulo, `ValidityState.UNCHECKED` y posición coherente con la solicitada
    - **Validates: Requirements 2.1, 2.2**
    - **Property 8: Rechazo de anidamiento de ContainerBlocks** — cualquier intento de añadir un `ContainerBlock` a la `innerSequence` de otro `ContainerBlock` debe lanzar `ContainerNestingException` y dejar `innerSequence` sin cambios
    - **Validates: Requirements 8.6**
    - _Clases: `CanvasBlockCreationTest`, `ContainerNestingRejectionTest`_ — 200 iteraciones cada una
  - [ ] 2.5 Implementar `Canvas`
    - Crear clase `Canvas` con `ObservableList<Block> blocks`, referencia a `Grammar` (nullable)
    - Implementar `addBlock(String blockType, int position)`: lanza `NoGrammarLoadedException` si no hay gramática, lanza `UnknownBlockTypeException` si el tipo no está en la gramática, crea el `Block` (o `ContainerBlock` si `isContainerType`), lo inserta en la posición, actualiza los `position` de los bloques desplazados, devuelve el bloque creado
    - Implementar `moveBlock(String blockId, int newPosition)`: reubica el bloque, actualiza posiciones
    - Implementar `removeBlock(String blockId)`: elimina el bloque y ajusta posiciones
    - Implementar `getBlocks()`, `setGrammar(Grammar)`, `addSequenceChangeListener(SequenceChangeListener)`
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 5.3, 5.4_

- [ ] 3. Motor de análisis sintáctico
  - [ ] 3.1 Implementar `SyntaxValidator`
    - Crear clase `SyntaxValidator` sin estado
    - Implementar `evaluate(SyntaxRule rule, String prevType, String nextType)`: aplicar el algoritmo de precedencia (forbidden primero, luego allowed) devolviendo `VALID` o `INVALID`
    - Implementar `validateSequence(List<Block> blocks, Grammar grammar)`: para cada bloque calcular `prev` y `next` (o `"EMPTY"` en extremos), llamar a `evaluate`, devolver `List<BlockValidationResult>`
    - Implementar `aggregateContainerState(List<ValidityState> innerStates)`: `INVALID` si alguno es `INVALID`; `VALID` si todos son `VALID` o la lista está vacía; `UNCHECKED` si todos son `UNCHECKED`
    - _Requirements: 3.2, 3.6, 3.7, 8.2, 8.3, 8.4_
  - [ ]* 3.2 Escribir tests de propiedades para `SyntaxValidator`
    - **Property 6: Corrección del algoritmo de validación sintáctica** — para cualquier `Grammar` válida y cualquier secuencia, `validateSequence` asigna el estado correcto a cada bloque según el algoritmo de precedencia
    - **Validates: Requirements 3.2, 3.7**
    - **Property 7: Agregación de estado de ContainerBlock** — para cualquier `List<ValidityState>`, `aggregateContainerState` devuelve el estado correcto según las reglas de precedencia
    - **Validates: Requirements 8.3, 8.4**
    - _Clases: `SyntaxValidatorCorrectnessTest` (500 iter.), `ContainerStateAggregationTest` (200 iter.)_
  - [ ] 3.3 Implementar `DebouncedSyntaxEngine`
    - Crear interfaz `SyntaxEngine` con `scheduleAnalysis`, `cancelPendingAnalysis`, `addResultListener`
    - Crear interfaz `AnalysisResultListener` con `onAnalysisComplete(List<BlockValidationResult>)` y `onAnalysisError(Exception)`
    - Implementar `DebouncedSyntaxEngine` usando `ScheduledExecutorService`: `scheduleAnalysis` cancela el `ScheduledFuture` pendiente y planifica uno nuevo con 200 ms de retardo
    - En la tarea planificada: invocar `SyntaxValidator.validateSequence` (y analizar `ContainerBlock`s internos); en caso de excepción, notificar `onAnalysisError` sin modificar estados; notificar listeners en el hilo JavaFX vía `Platform.runLater`
    - _Requirements: 3.1, 3.3, 3.5, 5.5, 5.6, 7.1, 7.2, 7.3_
  - [ ]* 3.4 Escribir tests para `DebouncedSyntaxEngine`
    - Test unitario: verificar que un segundo `scheduleAnalysis` dentro del intervalo de debounce cancela el anterior (mock de `SyntaxValidator`)
    - Test unitario: verificar que una excepción interna del `SyntaxValidator` invoca `onAnalysisError` y no `onAnalysisComplete`
    - **Property 11: Estados previos preservados ante error interno** — ante cualquier excepción en el engine, los `ValidityState` de los bloques no se modifican
    - **Validates: Requirements 7.3**
    - _Clase: `SyntaxEngineErrorPreservationTest`_ — 200 iteraciones

- [ ] 4. Carga de configuración YAML
  - [ ] 4.1 Implementar `YamlConfigParser` y DTOs
    - Crear clases `GrammarConfigDto` y `SyntaxRuleDto` con anotaciones Jackson
    - Crear `YamlConfigParser` con `ObjectMapper` + `YAMLFactory`
    - En `parse(Path path)`: lanzar `ConfigurationNotFoundException` si el archivo no existe; lanzar `InvalidConfigurationException` (con mensaje de campo/línea) si Jackson no puede deserializar
    - _Requirements: 1.1, 1.3, 1.4, 1.5_
  - [ ]* 4.2 Escribir tests de propiedades para `YamlConfigParser`
    - **Property 3: YAML inválido siempre lanza excepción** — para cualquier cadena que no cumpla el esquema esperado (campo requerido ausente, tipo incorrecto), `parse` debe lanzar `InvalidConfigurationException`
    - **Validates: Requirements 1.4**
    - _Clase: `YamlParserInvalidInputTest`_ — 200 iteraciones
  - [ ] 4.3 Implementar `YamlRuleLoader`
    - Crear clase `YamlRuleLoader implements RuleLoader`
    - Invocar `YamlConfigParser.parse`, convertir cada `SyntaxRuleDto` a `SyntaxRule`
    - Validar conflictos: si `allowedBefore ∩ forbiddenBefore ≠ ∅` o `allowedAfter ∩ forbiddenAfter ≠ ∅`, lanzar `InvalidConfigurationException` indicando el tipo de bloque y el valor en conflicto
    - Construir y devolver `LoadResult(Grammar grammar, int ruleCount)`
    - _Requirements: 1.1, 1.2, 1.7_
  - [ ]* 4.4 Escribir tests de propiedades para `YamlRuleLoader`
    - **Property 1: Round-trip de gramática YAML** — para cualquier `Grammar` válida, serializar a YAML y parsear de vuelta debe producir una `Grammar` equivalente
    - **Validates: Requirements 1.2, 1.5, 1.6**
    - **Property 2: Conflicto allowed/forbidden lanza excepción** — para cualquier definición con intersección no vacía entre allowed y forbidden, `load` debe lanzar `InvalidConfigurationException`
    - **Validates: Requirements 1.7**
    - _Clases: `GrammarYamlRoundTripTest` (200 iter.), `RuleLoaderConflictTest` (200 iter.)_

- [ ] 5. Persistencia JSON del Canvas
  - [ ] 5.1 Implementar `JsonCanvasSerializer` y DTOs
    - Crear clases `CanvasStateDto` (con campo `version = "1.0"`) y `BlockStateDto` con anotaciones Jackson
    - Implementar `serialize(List<Block> blocks, Path outputPath)`: mapear bloques a DTOs, serializar con `ObjectMapper`; lanzar `CanvasPersistenceException` en caso de `IOException`
    - Implementar `deserialize(Path inputPath, Grammar grammar)`: leer y mapear DTOs a objetos `Block`/`ContainerBlock`; lanzar `CanvasPersistenceException` si `IOException`; lanzar `InvalidCanvasStateException` si JSON malformado, falta `id`/`blockType`/`position`, o el `blockType` no está en la `Grammar`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  - [ ]* 5.2 Escribir tests de propiedades para `JsonCanvasSerializer`
    - **Property 9: Round-trip de persistencia del Canvas** — para cualquier estado del Canvas (incluyendo `ContainerBlock`s), `serialize` seguido de `deserialize` debe producir la misma secuencia (mismos `id`, `blockType`, `position`, `innerSequence`)
    - **Validates: Requirements 6.1, 6.2, 6.4**
    - **Property 10: Canvas invariante ante JSON inválido** — para cualquier archivo JSON malformado o con campos ausentes, `deserialize` lanza la excepción correcta sin modificar el estado del Canvas
    - **Validates: Requirements 6.3**
    - _Clases: `CanvasJsonRoundTripTest` (200 iter.), `CanvasLoadInvalidJsonTest` (200 iter.)_

- [ ] 6. Checkpoint — Lógica de dominio y backend completos
  - Asegurarse de que todos los tests pasan; preguntar al usuario si hay dudas antes de continuar con la UI.

- [ ] 7. Orquestador `Analyzer`
  - [ ] 7.1 Implementar clase `Analyzer`
    - Crear `Analyzer` que componga `RuleLoader`, `Canvas`, `DebouncedSyntaxEngine` y `JsonCanvasSerializer`
    - Implementar `loadGrammar(Path configPath)`: delegar en `RuleLoader`, establecer la gramática en el `Canvas`
    - Implementar `addBlock`, `moveBlock`, `removeBlock`: delegar en `Canvas`, llamar a `SyntaxEngine.scheduleAnalysis` después de cada cambio
    - Implementar `saveCanvas(Path outputPath)`: delegar en `JsonCanvasSerializer.serialize`
    - Implementar `loadCanvas(Path inputPath)`: delegar en `JsonCanvasSerializer.deserialize`, reemplazar la secuencia del `Canvas`
    - Registrar el manejador de excepciones no controladas (`Thread.setDefaultUncaughtExceptionHandler`) con log `ERROR` + notificación
    - _Requirements: 1.1, 2.1, 5.1–5.7, 6.1–6.4, 7.1, 7.4_
  - [ ]* 7.2 Escribir tests de integración para `Analyzer`
    - Test: carga de gramática válida → `LoadResult.ruleCount()` correcto
    - Test: carga de gramática con archivo inexistente → `ConfigurationNotFoundException`
    - Test: flujo completo — añadir bloques, analizar, guardar, cargar, verificar estado restaurado
    - Test: análisis de secuencia con `ContainerBlock` — estados internos y externos correctos
    - Test: error interno en `SyntaxEngine` → estados preservados, sesión activa
    - _Clase: `AnalyzerIntegrationTest`_

- [ ] 8. UI JavaFX
  - [ ] 8.1 Implementar `BlockView` y `VisualFeedbackController`
    - Crear `BlockView extends StackPane`: muestra la etiqueta del tipo del bloque; aplica clases CSS `block-valid`, `block-invalid`, `block-unchecked` según `ValidityState`
    - Crear `syntax-analyzer.css` con las tres reglas de color definidas en el diseño
    - Crear `VisualFeedbackController`: método `bind(BlockView view, Block block)` que añade un listener al `validityProperty()` del bloque para actualizar la clase CSS en el hilo JavaFX; método `unbind(BlockView view)`
    - Garantizar que en ningún momento el `BlockView` tenga más de una clase CSS de estado activa simultáneamente
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [ ]* 8.2 Escribir tests de propiedades para `VisualFeedbackController`
    - **Property 12: Mapeo biyectivo estado→color** — para cualquier bloque en cualquier `ValidityState`, el `BlockView` debe tener exactamente una clase CSS de estado activa, correspondiente al estado dado
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.5**
    - _Clase: `VisualFeedbackMappingTest`_ — 100 iteraciones
  - [ ] 8.3 Implementar `CanvasView` y `DragAndDropHandler`
    - Crear `CanvasView.fxml` + `CanvasView.java` (`AnchorPane` principal con `ListView` o `FlowPane` para la secuencia de `BlockView`)
    - Crear panel de paleta de bloques (lista de tipos disponibles en la `Grammar`) desde el que el usuario arrastra bloques
    - Crear `DragAndDropHandler`: gestionar eventos `setOnDragDetected`, `setOnDragOver`, `setOnDragDropped`, `setOnDragDone`
      - Soltar desde paleta sobre Canvas → `Analyzer.addBlock`
      - Reubicar bloque existente → `Analyzer.moveBlock`
      - Soltar fuera del Canvas → `Analyzer.removeBlock`
      - Mostrar marcador de posición de inserción (`Separator` o `Rectangle`) entre bloques durante el arrastre
    - Implementar cancelación con Escape: `setOnKeyPressed` + `cancelDrag()` restaura posición original sin modificar secuencia
    - Implementar botón/tecla Delete sobre bloque seleccionado → `Analyzer.removeBlock`
    - Conectar `SequenceChangeListener` del `Canvas` con la actualización de la `CanvasView` en el hilo JavaFX
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_
  - [ ] 8.4 Implementar UI de inicio y manejo de errores visual
    - Crear diálogo de inicio (`FileChooser`) para seleccionar el `Configuration_File` YAML
    - Mostrar errores de carga de gramática (`ConfigurationNotFoundException`, `InvalidConfigurationException`) en un `Alert` bloqueante antes de activar el editor
    - Crear `NotificationService`: muestra mensajes no bloqueantes (`Tooltip`-style o `Popup`) que desaparecen automáticamente tras 5 s
    - Usar `NotificationService` para errores de `UnknownBlockTypeException`, `NoGrammarLoadedException`, `ContainerNestingException`, errores del `SyntaxEngine`, y errores de persistencia
    - Añadir barra de herramientas con botones "Guardar" y "Cargar" que invoquen `Analyzer.saveCanvas` / `Analyzer.loadCanvas` via `FileChooser`
    - _Requirements: 2.4, 2.5, 6.1, 6.2, 6.3, 7.1, 7.2, 7.4, 8.6_
  - [ ] 8.5 Implementar soporte de `ContainerBlock` en la UI
    - Mostrar `ContainerBlock` como un `BlockView` expandible (subpanel) que expone su propia `CanvasView` interna con drag & drop
    - Reutilizar `DragAndDropHandler` para la secuencia interna del `ContainerBlock`
    - Rechazar visualmente (sin operación, con mensaje de `NotificationService`) el intento de arrastrar un `ContainerBlock` a la secuencia interna de otro `ContainerBlock`
    - El estado del `ContainerBlock` (color del borde o cabecera) debe reflejar el estado agregado de su contenido
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [ ] 9. Integración final y checkpoint
  - [ ] 9.1 Conectar `Analyzer` con la `Application` JavaFX
    - Crear clase `SyntaxAnalyzerApp extends Application` como punto de entrada `main`
    - Inicializar `Analyzer`, registrar el manejador global de excepciones no controladas
    - Arrancar con diálogo de selección de gramática; configurar la escena principal con `CanvasView`
    - Pasar la instancia de `Analyzer` a los controladores de UI mediante inyección de dependencias simple (constructor o `AppContext`)
    - _Requirements: 1.1, 7.4_
  - [ ]* 9.2 Escribir tests de integración de extremo a extremo
    - **Property 13: Inserción de bloque incrementa la secuencia en 1** — para cualquier secuencia y posición válida, `Analyzer.addBlock` incrementa el tamaño del Canvas exactamente en 1
    - **Validates: Requirements 5.1**
    - **Property 14: Eliminación de bloque decrementa la secuencia en 1** — para cualquier secuencia no vacía, `Analyzer.removeBlock` decrementa el tamaño en 1 y el bloque ya no está presente
    - **Validates: Requirements 5.3, 5.4**
    - **Property 15: Cancelación de arrastre no modifica la secuencia** — para cualquier estado del Canvas, cancelar una operación de arrastre con Escape deja la secuencia idéntica
    - **Validates: Requirements 5.7**
    - Test de integración: flujo completo con `ContainerBlock` (análisis interno independiente, estado agregado correcto)
    - _Clases: `CanvasInsertionTest`, `CanvasRemovalTest`, `DragCancelTest`, `ContainerIntegrationTest`_

- [ ] 10. Checkpoint final — Todos los tests pasan
  - Ejecutar `mvn test` para verificar que todos los tests (unitarios, propiedades e integración) pasan sin errores. Preguntar al usuario si hay alguna duda antes de dar por cerrada la implementación.

---

## Notes

- Las sub-tareas marcadas con `*` son opcionales y pueden omitirse para una entrega MVP más rápida, pero se recomienda implementarlas para verificar las propiedades de corrección del diseño.
- El orden de las tareas garantiza que no hay código huérfano: cada componente se integra en el `Analyzer` antes de pasar a la UI.
- Los tests de propiedades con jqwik requieren mínimo 100 iteraciones (200 por defecto en este plan, 500 para la Property 6 por su complejidad).
- Para ejecutar solo los tests sin modo watch: `mvn test`.
- Cada test de propiedad debe anotarse con `@Tag("Feature: sintactic-analyzer, Property N: <texto>")`  y `@Property(tries = N)` tal como se muestra en el diseño técnico.
- La UI de JavaFX no se puede testear con jqwik de forma significativa (propiedades de rendering); se usa `TestFX` opcionalmente para tests de interacción si el entorno lo permite, o se mockea el `Analyzer` en tests de controlador.
- Para la Property 15 (cancelación de arrastre), el test debe operar sobre el `Canvas`/`Analyzer` directamente, no sobre la UI gráfica, verificando que `moveBlock` nunca se llama cuando se cancela el arrastre.
