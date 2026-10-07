/**
 * Risultato di un'esecuzione completa (pipeline o baseline sequenziale).
 * Viene creato una sola volta, quando tutti i worker hanno finito, e poi
 * solo letto dall'interfaccia: è immutabile, quindi non serve sincronizzazione.
 */

package it.unina.trafficanalyzer.pipeline

data class PipelineMetrics(
    val config: PipelineConfig,
    val recordsProcessed: Long,
    val attacksDetected: Long,
    val correctPredictions: Long,
    val recordsPerWorker: List<Long>,
    val elapsedNanos: Long
) {
    val elapsedMillis: Double
        get() = elapsedNanos / 1_000_000.0

    /** Record elaborati al secondo. */
    val throughput: Double
        get() = if (elapsedNanos > 0) recordsProcessed * 1_000_000_000.0 / elapsedNanos else 0.0

    /** Frazione di record la cui classe prevista coincide con quella vera. */
    val accuracy: Double
        get() = if (recordsProcessed > 0) correctPredictions.toDouble() / recordsProcessed else 0.0
}