// DEVI IMPLEMENTARLO TU FRANCE
/**
 * Risultato della classificazione di un record:
 * la classe prevista e se quella classe è un attacco.
 */

package it.unina.trafficanalyzer.classifier

import it.unina.trafficanalyzer.data.TrafficRecord

class Prediction(
    val label: String,
    val isAttack: Boolean
)

/**
 * Classifica un record di traffico. È il carico di lavoro di ogni worker.
 * Contratto: classify() viene chiamato da più thread contemporaneamente,
 * quindi un'implementazione non deve modificare il proprio stato dopo la costruzione.
 */
interface Classifier {
    fun classify(record: TrafficRecord): Prediction
}