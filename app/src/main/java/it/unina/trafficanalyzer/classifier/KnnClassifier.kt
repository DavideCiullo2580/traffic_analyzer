package it.unina.trafficanalyzer.classifier

import it.unina.trafficanalyzer.data.TrafficRecord
import java.util.PriorityQueue

/**
 * Classificatore k-Nearest Neighbors (k-NN).
 *
 * @param trainSet Dataset di addestramento immutabile.
 * @param k Numero di vicini da considerare per la votazione.
 */
class KnnClassifier(
    private val trainSet: List<TrafficRecord>, private val k: Int
) : Classifier {
    init {
        require(k > 0) { "k deve essere maggiore di 0" }
        require(trainSet.isNotEmpty()) { "Il dataset di training non può essere vuoto" }
        require(k <= trainSet.size) { "k non può superare la dimensione del training set" }
    }

    // Struttura di supporto locale per memorizzare vicino e distanza
    private data class Neighbor(val record: TrafficRecord, val distanceSquared: Double)

    override fun classify(record: TrafficRecord): Prediction {

        // Max-Heap di dimensione fissa k per raccogliere i k vicini più prossimi
        // Mantiene in cima il vicino attualmente più lontano tra i migliori k
        val maxHeap = PriorityQueue<Neighbor>(k) { a, b ->
            b.distanceSquared.compareTo(a.distanceSquared)
        }

        var bestLabel: String = ""
        val predictedIsAttack: Boolean
        val targetFeature = record.features

        val labelVotes = HashMap<String, Int>(k)
        var attackVotes = 0
        var maxVotes = -1

        for (candidate in trainSet) {
            val distSquared = euclideanDistanceSquared(targetFeature, candidate.features)

            if (maxHeap.size < k) {
                maxHeap.offer(Neighbor(candidate, distSquared))
            } else {
                if (maxHeap.peek() == null) TODO("NULL POINTER EXCEPTION")

                if (distSquared < maxHeap.peek().distanceSquared) {
                    maxHeap.poll()
                    maxHeap.offer(Neighbor(candidate, distSquared))
                }
            }
        }

        // Votazione a maggioranza sui k vicini
        while (maxHeap.isNotEmpty()) {
            val neighbor = maxHeap.poll().record
            labelVotes[neighbor.label] = (labelVotes[neighbor.label] ?: 0) + 1
            if (neighbor.isAttack) attackVotes++
        }

        // Determina l'etichetta più frequente
        for ((label, count) in labelVotes) {
            if (count > maxVotes) {
                maxVotes = count
                bestLabel = label
            }
        }

        predictedIsAttack = attackVotes > (k / 2)

        return Prediction(label = bestLabel, isAttack = predictedIsAttack)
    }

    private fun euclideanDistanceSquared(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        val size = a.size

        repeat(size, fun(i: Int) {
            val diff = a[i] - b[i]
            sum += diff * diff
        })

        return sum
    }
}