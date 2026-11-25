package it.unina.trafficanalyzer.classifier

import it.unina.trafficanalyzer.data.DatasetParser
import it.unina.trafficanalyzer.data.TrafficRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class KnnClassifierTest {
    @Test
    fun testClassifyWithSynthetucData() {
        val f1 = DoubleArray(19) { 0.1 }
        val f2 = DoubleArray(19) { 0.9 }

        val normalRecord = TrafficRecord("Thing_speack", false, f1)
        val attackRecord = TrafficRecord("DOS_SYN_Hping", false, f2)

        val clasifier = KnnClassifier(listOf(normalRecord, attackRecord), k = 1)


        val query = TrafficRecord("Unknown", false, DoubleArray(19) { 0.12 })
        val pred = clasifier.classify(query)

        assertEquals("Thing_speack", pred.label)
        assertEquals(false, pred.isAttack)
    }

    @Test
    fun testKnnOnRealDatasetAndMeasureCost() {
        // carica
        val trainFile = File("src/main/assets/train.csv")
        val streamFile = File("src/main/assets/stream.csv")

        // controllo di sicurezza
        assertTrue("ERRORE: train.csv non trovato nella cartella assets", trainFile.exists())
        assertTrue("ERRORE: stream.csv non trovato nella cartella assets", streamFile.exists())

        // carica
        val trainSet: List<TrafficRecord> =
            trainFile.bufferedReader().use { DatasetParser.parse(it) }
        val streamSet: List<TrafficRecord> =
            streamFile.bufferedReader().use { DatasetParser.parse(it) }

        // misuriamo
        val k = 3
        val classifier = KnnClassifier(trainSet, k)
        val sampleSize = 100
        val sampleStream = streamSet.take(sampleSize)

        val start = System.nanoTime()
        for (record in sampleStream) classifier.classify(record)
        val tempo = (System.nanoTime() - start) / 1_000_000.0

        assertTrue("Tesmpo calcolo < 0, -> possibilmente non avvenuto ", tempo > 0)
    }

    @Test
    fun test4ThreadSafetyUnder10kLoad() {
        // setto
        val f1 = DoubleArray(19) { 0.1 }
        val f2 = DoubleArray(19) { 0.9 }
        val normal = TrafficRecord("Normal", false, f1)
        val attack = TrafficRecord("Attack", true, f2)
        val classifier = KnnClassifier(listOf(normal, attack), k = 1)

        // 4 worker dell'app
        val threadCount = 4
        val executor = Executors.newFixedThreadPool(threadCount)
        val iterations = 10_000
        val errorCount = AtomicInteger(0)

        val query = TrafficRecord("Query", false, DoubleArray(19) { 0.12 })

        // fai @iterations classificazioni in parallelo sulla STESSA istanza
        for (i in 0 until iterations) executor.submit {
            try {
                val pred = classifier.classify(query)
                if (pred.label != "Normal") {
                    errorCount.incrementAndGet()
                }
            } catch (e: Exception) {
                errorCount.incrementAndGet()
            }
        }


        // Chiudiamo il pool e aspettiamo che finiscano tutti
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // Se l'istanza non fosse thread-safe, avremmo errori o eccezioni
        assertEquals(
            "Error: Ci sono state race condition o errori nei thread!",
            0,
            errorCount.get()
        )
    }

    @Test
    fun testInvalidConstructorArgumentsKUguale0() {
        val record = TrafficRecord("Test", false, DoubleArray(19))
        val trainSet = listOf(record)

        assertThrows(IllegalArgumentException::class.java) {
            KnnClassifier(trainSet, k = 0)
        }
    }

    @Test
    fun testInvalidConstructorArgumentsKMinore0() {
        val record = TrafficRecord("Test", false, DoubleArray(19))
        val trainSet = listOf(record)

        assertThrows(IllegalArgumentException::class.java) {
            KnnClassifier(trainSet, k = -1)
        }
    }

    @Test
    fun testInvalidConstructorArgumentsKMaggioreDimTrainSet() {
        val record = TrafficRecord("Test", false, DoubleArray(19))
        val trainSet = listOf(record)

        assertThrows(IllegalArgumentException::class.java) {
            KnnClassifier(trainSet, k = 5) // Il dataset ha size 1
        }
    }

    @Test
    fun testInvalidConstructorArgumentsDatasetVuoto() {
        val record = TrafficRecord("Test", false, DoubleArray(19))
        val trainSet = listOf(record)

        assertThrows(IllegalArgumentException::class.java) {
            KnnClassifier(emptyList(), k = 1)
        }
    }

    @Test
    fun testTieBreakingLogic() {
        val f1 = DoubleArray(19) { 0.1 }
        val f2 = DoubleArray(19) { 0.2 }

        val normal1 = TrafficRecord("Normal", false, f1)
        val normal2 = TrafficRecord("Normal", false, f1)
        val attack1 = TrafficRecord("Attack", true, f2)
        val attack2 = TrafficRecord("Attack", true, f2)

        // con 2 attacchi equidistanti
        val classifier = KnnClassifier(listOf(normal1, attack1, normal2, attack2), k = 4)
        val query = TrafficRecord("Query", false, DoubleArray(19) { 0.15 })

        val pred = classifier.classify(query)

        // assicuriamoci l'etichetta sia una delle 2 valide
        val isValidLabel = pred.label == "Normal" || pred.label == "Attack"
        assertTrue("In caso di pareggio deve comunque restituire un'etichetta valida", isValidLabel)
    }

    @Test
    fun testImmutabilityOfTrainSet() {
        val features = DoubleArray(19) { it.toDouble() } // Array [0.0, 1.0, 2.0 ...]
        val trainRecord = TrafficRecord("Normal", false, features)
        val classifier = KnnClassifier(listOf(trainRecord), k = 1)

        val queryFeatures = DoubleArray(19) { 0.5 }
        val query = TrafficRecord("Query", false, queryFeatures)

        classifier.classify(query)

        for (i in 0 until 19) {
            assertEquals(
                "Il training set è stato modificato!",
                i.toDouble(),
                trainRecord.features[i],
                0.0001
            )
        }
    }

    @Test
    fun testImmutabilityOfQuery() {
        val features = DoubleArray(19) { it.toDouble() } // Array [0.0, 1.0, 2.0 ...]
        val trainRecord = TrafficRecord("Normal", false, features)
        val classifier = KnnClassifier(listOf(trainRecord), k = 1)

        val queryFeatures = DoubleArray(19) { 0.5 }
        val query = TrafficRecord("Query", false, queryFeatures)

        classifier.classify(query)

        for (i in 0 until 19) {
            assertEquals("La query è stata modificata!", 0.5, query.features[i], 0.0001)
        }
    }
}