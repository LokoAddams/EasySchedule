# Validación de la Prueba de Concepto (PoC): Generador de Horarios Inteligente

Esta Prueba de Concepto (PoC) tiene como objetivo demostrar la viabilidad técnica de un sistema automatizado para la generación y optimización de horarios universitarios, basado en restricciones académicas y preferencias personales del estudiante.

## ¿Cómo valida la idea esta PoC?

La PoC confirma que el núcleo lógico del problema puede resolverse eficientemente mediante la combinación de algoritmos clásicos de Ciencias de la Computación:

1. **Fuerza Bruta / Backtracking (Generación de Horarios):**
Valida que es posible iterar sobre todas las combinaciones de materias y paralelos, descartando ramas inválidas tempranamente. Las **restricciones estrictas** (como evitar el cruce de horas, cumplir el límite exacto de materias a tomar, y forzar un paralelo específico si el usuario lo requiere) actúan como filtros eficientes. Esto demuestra que el sistema generará *exclusivamente* horarios reales y matriculables.
2. **Sistema de Puntuación Ponderada (Prioridades):**
Valida el modelo de preferencias. Al asignar un "peso" numérico a factores subjetivos (mañana vs. tarde, puentes cortos, límite de materias por día, preferencia de un paralelo), el sistema transforma deseos cualitativos en una métrica cuantitativa. Esto permite ordenar los resultados en un "Top 10", garantizando que el usuario siempre vea la mejor aproximación matemática a su horario ideal, incluso cuando sus preferencias entran en conflicto.
3. **Búsqueda en Anchura / BFS (Proyección Académica):**
Valida la capacidad del sistema de actuar como un consejero académico. Al utilizar un grafo dirigido para representar los prerrequisitos, el algoritmo BFS simula la aprobación del horario actual y calcula qué nuevos nodos (materias) tendrán un grado de entrada igual a cero en el futuro. Esto demuestra que el sistema puede ofrecer visibilidad a largo plazo sobre la malla curricular.

**Conclusión de la PoC:** La lógica base es sólida. El siguiente paso evolutivo sería extraer los datos "mockeados" hacia una base de datos real (SQL/NoSQL) y conectar estas funciones de C++ a un backend (ej. mediante una API REST) para ser consumidas por una interfaz gráfica amigable.

