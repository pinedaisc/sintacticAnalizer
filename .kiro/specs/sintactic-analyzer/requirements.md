# Requirements Document

## Introduction

El **Analizador Sintáctico** es una herramienta interactiva en Java 17 que permite a los usuarios construir secuencias de bloques lógicos mediante una interfaz de arrastrar y soltar. Las reglas de sintaxis válida se definen en un archivo de configuración externo. El analizador evalúa en tiempo real si cada bloque está posicionado correctamente según dichas reglas y comunica el resultado mediante retroalimentación visual (cambio de color del bloque).

---

## Glossary

- **Analyzer**: El sistema principal que orquesta la carga de reglas, el análisis sintáctico y la coordinación con la interfaz.
- **Rule_Loader**: Componente responsable de leer e interpretar el archivo de configuración de reglas sintácticas.
- **Grammar**: Conjunto de reglas sintácticas que definen las secuencias válidas de bloques, leídas desde el archivo de configuración.
- **Block**: Unidad atómica que el usuario arrastra y posiciona en el área de trabajo.
- **Canvas**: Área de trabajo interactiva donde el usuario arrastra y ordena los bloques.
- **Syntax_Engine**: Componente que aplica la gramática cargada para evaluar si la secuencia actual de bloques es sintácticamente válida.
- **Visual_Feedback_Controller**: Componente responsable de aplicar los cambios de color sobre los bloques según el resultado del análisis.
- **Configuration_File**: Archivo externo en formato YAML que contiene la definición de la gramática y los tipos de bloques permitidos.
- **SyntaxRule**: Entrada en la Grammar que define un tipo de bloque y sus restricciones de vecindad. Contiene los campos `allowedBefore` y `allowedAfter` (listas de tipos permitidos como vecino inmediato anterior y posterior respectivamente), así como `forbiddenBefore` y `forbiddenAfter` (listas de tipos explícitamente prohibidos como vecino inmediato anterior y posterior respectivamente). El valor especial `"EMPTY"` en `allowedBefore` indica que el bloque puede ocupar la primera posición de la secuencia (sin predecesor); `"EMPTY"` en `allowedAfter` indica que puede ocupar la última posición (sin sucesor). Si `"EMPTY"` no está en la lista correspondiente, el bloque es `INVALID` cuando se encuentra en esa posición extrema.
- **ContainerBlock**: Tipo especial de Block declarado con `container: true` en la Grammar, que puede albergar en su interior una secuencia propia de bloques. La secuencia interna se analiza de forma independiente con las mismas reglas de la Grammar activa. No se permiten ContainerBlocks anidados más de un nivel.

---

## Requirements

### Requisito 1: Carga de reglas sintácticas desde archivo de configuración

**User Story:** Como desarrollador, quiero definir las reglas sintácticas en un archivo de configuración externo, para poder modificar la gramática sin recompilar la aplicación.

#### Criterios de Aceptación

1. WHEN el Analyzer se inicia, THE Rule_Loader SHALL leer el Configuration_File ubicado en la ruta proporcionada como parámetro de inicio del Analyzer.
2. IF el Configuration_File contiene una gramática válida, THEN THE Rule_Loader SHALL cargar todas las reglas en la Grammar y devolver al Analyzer una confirmación de carga exitosa que incluya el número de reglas cargadas. El Configuration_File YAML debe seguir la siguiente estructura por cada tipo de bloque:
   ```yaml
   blocks:
     - type: "A"
       container: false
       allowedBefore: ["EMPTY", "B"]   # EMPTY indica que A puede ser el primer bloque
       allowedAfter:  ["B", "EMPTY"]   # EMPTY indica que A puede ser el último bloque
       forbiddenBefore: []             # tipos que NO pueden preceder a A
       forbiddenAfter:  ["C"]          # tipos que NO pueden seguir a A
     - type: "B"
       container: true                 # B es un bloque de agrupación (ContainerBlock)
       allowedBefore: ["A"]
       allowedAfter:  ["EMPTY"]
       forbiddenBefore: []
       forbiddenAfter:  []
   ```
3. IF el Configuration_File no existe en la ruta especificada, THEN THE Rule_Loader SHALL lanzar una excepción de tipo `ConfigurationNotFoundException` con un mensaje que indique la ruta buscada.
4. IF el Configuration_File tiene un formato inválido o está malformado, THEN THE Rule_Loader SHALL lanzar una excepción de tipo `InvalidConfigurationException` con un mensaje que describa el error de formato y la línea o campo problemático.
5. THE Rule_Loader SHALL leer y parsear el Configuration_File en formato YAML, cargando correctamente todas las reglas presentes en el archivo.
6. THE Rule_Loader SHALL producir una Grammar equivalente al parsear un Configuration_File YAML generado por serialización de esa misma Grammar, donde equivalente significa que el conjunto de reglas resultante contiene las mismas reglas, en nombre y patrón, que la Grammar original.
7. IF la definición de un tipo de bloque en el Configuration_File contiene un mismo valor en `allowedBefore` y en `forbiddenBefore`, o un mismo valor en `allowedAfter` y en `forbiddenAfter`, THEN THE Rule_Loader SHALL lanzar una excepción de tipo `InvalidConfigurationException` con un mensaje que identifique el tipo de bloque afectado y el valor en conflicto.

---

### Requisito 2: Representación e inicialización de bloques

**User Story:** Como usuario, quiero que cada bloque tenga un tipo y un estado visual claro, para poder identificar visualmente su estado sintáctico en cualquier momento.

#### Criterios de Aceptación

1. THE Analyzer SHALL representar cada Block con exactamente los atributos: identificador único, tipo de bloque, posición en el Canvas y estado de validez sintáctica, donde el estado de validez puede ser únicamente `UNCHECKED`, `VALID` o `INVALID`.
2. WHEN un Block es añadido al Canvas, THE Canvas SHALL asignarle un identificador único dentro del Canvas activo e inicializar su estado de validez como `UNCHECKED`.
3. IF la Grammar está cargada, THEN THE Canvas SHALL permitir únicamente la adición de Blocks cuyos tipos estén definidos en la Grammar activa.
4. IF se intenta añadir un Block con un tipo no definido en la Grammar, THEN THE Canvas SHALL rechazar la operación y notificar al usuario mediante un mensaje de error en la interfaz que indique el tipo de bloque no reconocido.
5. IF se intenta añadir un Block al Canvas sin que la Grammar haya sido cargada previamente, THEN THE Canvas SHALL rechazar la operación y notificar al usuario con un mensaje que indique que no hay gramática activa.

---

### Requisito 3: Análisis sintáctico en tiempo real

**User Story:** Como usuario, quiero que el sistema analice automáticamente la posición de cada bloque al arrastrarlo, para recibir retroalimentación inmediata sobre si la secuencia es válida.

#### Criterios de Aceptación

1. WHEN el usuario posiciona o reposiciona un Block en el Canvas, THE Syntax_Engine SHALL iniciar el análisis de la secuencia completa de bloques dentro de los 200 ms siguientes al evento de posicionamiento.
2. WHEN el Syntax_Engine completa el análisis, THE Syntax_Engine SHALL asignar a cada Block uno de los estados: `VALID` si la posición del bloque es sintácticamente correcta según la Grammar, `INVALID` si viola alguna regla de la Grammar, o `UNCHECKED` si el bloque aún no ha sido evaluado en el análisis en curso.
3. WHILE el Syntax_Engine está ejecutando un análisis y el usuario posiciona o reposiciona un Block, THE Syntax_Engine SHALL cancelar el análisis en curso e iniciar un nuevo análisis desde el inicio, manteniendo los estados previos de cada Block hasta que el nuevo análisis concluya.
4. WHEN el Syntax_Engine completa el análisis, THE Canvas SHALL actualizar la representación visual de cada Block para reflejar su estado asignado, distinguiendo de forma observable entre los estados `VALID`, `INVALID` y `UNCHECKED`.
5. IF el Syntax_Engine no completa el análisis dentro de los 200 ms siguientes al evento de posicionamiento, THEN THE Syntax_Engine SHALL mantener los estados previos de cada Block y emitir una indicación de error de tiempo de análisis sin modificar la secuencia de bloques en el Canvas.
6. WHEN la secuencia de bloques en el Canvas está vacía, THE Syntax_Engine SHALL considerar la secuencia como `VALID` y no emitir errores.
7. THE Syntax_Engine SHALL aplicar las reglas de la Grammar para determinar el estado de cada Block según la siguiente lógica, evaluada en el orden indicado (la prohibición tiene precedencia sobre el permiso):
   - Sea `prev` el tipo del bloque inmediatamente anterior al bloque evaluado, o `"EMPTY"` si no existe predecesor.
   - Sea `next` el tipo del bloque inmediatamente posterior al bloque evaluado, o `"EMPTY"` si no existe sucesor.
   - El bloque es `INVALID` si `prev` aparece en `forbiddenBefore` del bloque, o si `next` aparece en `forbiddenAfter` del bloque.
   - En caso contrario, el bloque es `INVALID` si `prev` no aparece en `allowedBefore` del bloque, o si `next` no aparece en `allowedAfter` del bloque.
   - Si ninguna condición de invalidez se cumple, el bloque es `VALID`.

---

### Requisito 4: Retroalimentación visual por cambio de color

**User Story:** Como usuario, quiero que los bloques cambien de color según su estado sintáctico, para identificar de un vistazo qué bloques están mal posicionados.

#### Criterios de Aceptación

1. WHEN un Block recibe el estado `VALID` del Syntax_Engine, THE Visual_Feedback_Controller SHALL mostrar ese Block en color verde.
2. WHEN un Block recibe el estado `INVALID` del Syntax_Engine, THE Visual_Feedback_Controller SHALL mostrar ese Block en color rojo.
3. WHILE un Block tiene el estado `UNCHECKED`, THE Visual_Feedback_Controller SHALL mostrar ese Block en color gris neutro, incluyendo desde el momento en que el Block es creado hasta que el Syntax_Engine emita un nuevo estado para ese Block.
4. WHEN el estado de un Block cambia, THE Visual_Feedback_Controller SHALL aplicar el cambio de color dentro de los 100 ms siguientes a la recepción del nuevo estado; IF el cambio de color no se aplica dentro de ese intervalo, THEN THE Visual_Feedback_Controller SHALL descartar el estado intermedio y aplicar el color correspondiente al estado más reciente disponible.
5. THE Visual_Feedback_Controller SHALL garantizar que en ningún momento un mismo Block muestre simultáneamente más de un color de estado.
6. IF el Syntax_Engine emite más de un cambio de estado para un mismo Block dentro de un intervalo de 100 ms, THEN THE Visual_Feedback_Controller SHALL aplicar únicamente el color correspondiente al último estado recibido en ese intervalo.

---

### Requisito 5: Interacción de arrastrar y soltar en el Canvas

**User Story:** Como usuario, quiero arrastrar bloques hacia el área de trabajo y reordenarlos libremente, para construir y modificar secuencias sintácticas de forma intuitiva.

#### Criterios de Aceptación

1. WHEN el usuario suelta un Block arrastrado desde la paleta de bloques sobre el área del Canvas, THE Canvas SHALL insertar el Block en la posición destino indicada y mostrarlo como parte de la secuencia activa.
2. WHEN el usuario suelta un Block ya colocado sobre una nueva posición dentro del Canvas, THE Canvas SHALL reubicar el Block en esa posición y actualizar el orden de la secuencia en consecuencia.
3. WHEN el usuario arrastra un Block fuera del área del Canvas y lo suelta, THE Canvas SHALL eliminar ese Block de la secuencia activa.
4. WHEN el usuario activa el control de eliminación de un Block seleccionado (botón de eliminar visible sobre el bloque o tecla Delete), THE Canvas SHALL eliminar ese Block de la secuencia activa.
5. WHEN un Block es eliminado del Canvas, THE Syntax_Engine SHALL re-analizar la secuencia resultante dentro de los 200 ms siguientes a la eliminación.
6. WHEN un Block es reubicado dentro del Canvas, THE Syntax_Engine SHALL re-analizar la secuencia resultante dentro de los 200 ms siguientes al reposicionamiento.
7. IF el usuario cancela una operación de arrastre mediante la tecla Escape, THEN THE Canvas SHALL devolver el Block a su posición original sin modificar la secuencia activa.
8. WHILE el usuario está arrastrando un Block, THE Canvas SHALL mostrar un marcador de posición en el punto de inserción destino que indique entre qué bloques existentes se insertará el bloque arrastrado.

---

### Requisito 6: Persistencia y carga del estado del Canvas

**User Story:** Como usuario, quiero guardar el estado actual del Canvas y cargarlo en sesiones posteriores, para no perder el trabajo realizado.

#### Criterios de Aceptación

1. WHEN el usuario solicita guardar el estado del Canvas, THE Analyzer SHALL serializar la secuencia actual de bloques (tipos, posiciones e identificadores) en un archivo con formato JSON en la ruta indicada por el usuario.
2. WHEN el usuario solicita cargar un estado guardado, THE Analyzer SHALL deserializar el archivo JSON y reemplazar la secuencia de bloques actual del Canvas por la secuencia restaurada del archivo.
3. IF el archivo de estado guardado contiene JSON malformado, carece de campos requeridos por bloque (tipo, posición o identificador), o incluye tipos de bloque no definidos en la Grammar activa, THEN THE Analyzer SHALL notificar al usuario con un mensaje de error que indique la causa y mantener el estado actual del Canvas sin modificaciones.
4. THE Analyzer SHALL producir un Canvas con los mismos bloques, en el mismo orden y en las mismas posiciones, al deserializar un archivo generado por serialización de ese mismo estado del Canvas.

---

### Requisito 7: Manejo de errores y robustez

**User Story:** Como usuario, quiero que la aplicación gestione los errores de forma controlada, para que un fallo puntual no interrumpa mi sesión de trabajo.

#### Criterios de Aceptación

1. IF el Syntax_Engine encuentra un error interno durante el análisis, THEN THE Analyzer SHALL registrar el error en el log de la aplicación con nivel `ERROR` incluyendo el tipo de excepción, el mensaje y la traza de llamadas completa hasta el punto de origen.
2. IF el Syntax_Engine encuentra un error interno durante el análisis, THEN THE Analyzer SHALL notificar al usuario mediante un mensaje en la interfaz que no bloquee la interacción con el editor, visible durante al menos 5 segundos antes de desaparecer automáticamente.
3. IF el Syntax_Engine encuentra un error interno durante el análisis, THEN THE Analyzer SHALL preservar el último estado de validez calculado para cada Block existente sin modificarlo, de modo que cada Block retenga el resultado de validación previo al error.
4. IF se produce una excepción no controlada en el Analyzer, THEN THE Analyzer SHALL registrar el evento en el log con nivel `ERROR` antes de absorber la excepción, dejando la sesión de trabajo activa e interactiva para el usuario.

---

### Requisito 8: Bloques de agrupación (container blocks)

**User Story:** Como usuario, quiero poder agrupar bloques dentro de un bloque contenedor, para organizar secuencias anidadas y que el sistema las valide de forma independiente con las mismas reglas sintácticas.

#### Criterios de Aceptación

1. WHERE un tipo de bloque está declarado con `container: true` en la Grammar activa, THE Canvas SHALL tratar ese Block como un ContainerBlock capaz de albergar una secuencia interna de bloques.
2. WHEN el Syntax_Engine analiza un ContainerBlock, THE Syntax_Engine SHALL analizar la secuencia interna de forma independiente aplicando las mismas reglas de la Grammar activa que rigen las secuencias del Canvas principal.
3. WHEN el Syntax_Engine completa el análisis de un ContainerBlock, THE Syntax_Engine SHALL asignar al ContainerBlock el estado resultante de su contenido según la siguiente precedencia: `INVALID` si al menos un bloque interno es `INVALID`; `VALID` si todos los bloques internos son `VALID`; `UNCHECKED` si todos los bloques internos son `UNCHECKED`.
4. WHEN un ContainerBlock está vacío (secuencia interna de longitud cero), THE Syntax_Engine SHALL asignar al ContainerBlock el estado `VALID` sin emitir errores.
5. WHEN el Syntax_Engine evalúa la vecindad de un ContainerBlock en el Canvas principal, THE Syntax_Engine SHALL tratar el ContainerBlock como cualquier otro Block, evaluando su tipo contra las listas `allowedBefore`, `allowedAfter`, `forbiddenBefore` y `forbiddenAfter` de sus vecinos directos en la secuencia principal.
6. IF el usuario intenta añadir un ContainerBlock dentro de la secuencia interna de otro ContainerBlock, THEN THE Canvas SHALL rechazar la operación y notificar al usuario con un mensaje que indique que no se permiten agrupaciones anidadas más de un nivel.
