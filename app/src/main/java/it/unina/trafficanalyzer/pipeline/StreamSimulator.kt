package it.unina.trafficanalyzer.pipeline

import it.unina.trafficanalyzer.data.TrafficRecord
/**
 * Il produttore: invia i record dello stream nella coda, uno alla volta,
 * al ritmo scelto (simulazione) oppure il più veloce possibile (benchmark).
 * Gira su un thread dedicato, creato da chi avvia la pipeline.
 */
class StreamSimulator(
    private val records: List<TrafficRecord>,
    private val queue: ProcessingQueue,
    private val recordsPerSecond: Int?, // null = benchmark, nessuna attesa
    private val workerCount: Int
) : Runnable {

    override fun run() {
        try {
            val intervalNanos = recordsPerSecond?.let { 1_000_000_000L / it }
            val startNanos = System.nanoTime()

            for ((i, record) in records.withIndex()) {
                if (intervalNanos != null) {
                    waitUntil(startNanos + i * intervalNanos)
                }
                queue.put(record) // se la coda è piena il thread si blocca qui
            }
            queue.sendPoisonPills(workerCount)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun waitUntil(targetNanos: Long) {
        val waitNanos = targetNanos - System.nanoTime()
        if (waitNanos > 0) {
            Thread.sleep(waitNanos / 1_000_000, (waitNanos % 1_000_000).toInt())
        }
    }
}