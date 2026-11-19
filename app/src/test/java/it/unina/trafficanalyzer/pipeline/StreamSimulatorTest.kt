package it.unina.trafficanalyzer.pipeline

import it.unina.trafficanalyzer.data.TrafficRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong
/**
 * Test sul computer (senza Android) di StreamSimulator e ProcessingQueue:
 * nessun record perso o duplicato, i worker escono con la poison pill,
 * il ritmo della simulazione viene rispettato.
 */
class StreamSimulatorTest {

    private fun fakeRecords(n: Int) =
        List(n) { TrafficRecord("R$it", false, DoubleArray(TrafficRecord.FEATURE_COUNT)) }

    @Test
    fun tuttiIRecordArrivanoUnaVoltaSola() {
        val records = fakeRecords(10_000)
        val queue = ProcessingQueue(capacity = 10) // piccola, così scatta la backpressure
        val received = AtomicLong(0)
        val workers = List(4) {
            Thread {
                while (true) {
                    val r = queue.take()
                    if (ProcessingQueue.isPoisonPill(r)) break
                    received.incrementAndGet()
                }
            }
        }
        workers.forEach { it.start() }
        val producer = Thread(StreamSimulator(records, queue, null, workers.size))
        producer.start()

        producer.join(5_000)
        workers.forEach { it.join(5_000) }

        assertTrue("i worker devono essere usciti", workers.none { it.isAlive })
        assertEquals(10_000L, received.get())
    }

    @Test
    fun ilRitmoVieneRispettato() {
        val queue = ProcessingQueue(capacity = 100)
        val start = System.nanoTime()
        StreamSimulator(fakeRecords(50), queue, 100, 1).run()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("troppo veloce: $elapsedMs ms", elapsedMs >= 490)
    }
}