package it.unina.trafficanalyzer.pipeline

import it.unina.trafficanalyzer.data.TrafficRecord
import java.util.concurrent.ArrayBlockingQueue
/**
 * Coda a capacità fissa tra il produttore (StreamSimulator) e i worker.
 * Se è piena il produttore si blocca (backpressure), se è vuota si bloccano i worker.
 * La fine del flusso si segnala con una "poison pill" per ogni worker.
 */
class ProcessingQueue(capacity: Int) {

    private val queue = ArrayBlockingQueue<TrafficRecord>(capacity)

    fun put(record: TrafficRecord) {
        queue.put(record)
    }

    fun take(): TrafficRecord = queue.take()

    fun sendPoisonPills(workerCount: Int) {
        repeat(workerCount) { queue.put(POISON_PILL) }
    }

    companion object {
        val POISON_PILL = TrafficRecord("POISON_PILL", false, DoubleArray(0))

        fun isPoisonPill(record: TrafficRecord): Boolean = record === POISON_PILL
    }
}