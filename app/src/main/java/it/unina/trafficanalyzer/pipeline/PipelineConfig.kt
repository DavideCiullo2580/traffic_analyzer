/** Come vengono elaborati i record. */

package it.unina.trafficanalyzer.pipeline

enum class ExecutionMode {
    SEQUENTIAL,
    PIPELINE
}

/**
 * Parametri di una singola esecuzione, scelti dall'interfaccia prima di avviarla.
 * È immutabile: una volta creata, i thread la possono leggere senza sincronizzazione.
 */
data class PipelineConfig(
    val mode: ExecutionMode,
    val workerCount: Int = 1,
    val queueCapacity: Int = 100,
    val recordsPerSecond: Int? = null
) {
    init {
        require(workerCount >= 1) { "Serve almeno un worker" }
        require(queueCapacity >= 1) { "La coda deve avere capacità almeno 1" }
        require(recordsPerSecond == null || recordsPerSecond > 0) { "Ritmo non valido" }
        if (mode == ExecutionMode.SEQUENTIAL) {
            require(workerCount == 1) { "La baseline sequenziale usa un solo thread" }
            require(recordsPerSecond == null) { "La baseline sequenziale serve solo per il benchmark" }
        }
    }
}