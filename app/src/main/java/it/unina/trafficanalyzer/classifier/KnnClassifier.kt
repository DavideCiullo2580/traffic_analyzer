package it.unina.trafficanalyzer.classifier

import it.unina.trafficanalyzer.data.TrafficRecord

class KnnClassifier(
    private val trainset: List<TrafficRecord>,
    private val k: Int
) : Classifier {
    init {
        require(k > 0) { "k deve essere maggiore di 0" }
        require(trainset.isNotEmpty()) { "Il dataset di training non può essere vuoto" }
        require(k <= trainset.size) { "k non può superare la dimensione del training set" }
    }

    override fun classify(record: TrafficRecord): Prediction {
        TODO("Not yet implemented")
    }

    private fun euclideanDistanceSquared(a: DoubleArray, b: DoubleArray): Double {
        return 0.01
    }
}