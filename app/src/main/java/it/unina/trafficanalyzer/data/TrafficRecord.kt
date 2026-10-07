/**
 * Un record di traffico IoT letto dal dataset RT-IoT2022 già preprocessato.
 * Contiene la classe (label), se è un attacco e le 19 feature normalizzate
 * tra 0 e 1 che il classificatore k-NN usa per calcolare le distanze.
 */

package it.unina.trafficanalyzer.data

class TrafficRecord(
    val label: String,
    val isAttack: Boolean,
    val features: DoubleArray
) {
    companion object {
        // 16 feature numeriche + proto_tcp, proto_udp, proto_icmp
        const val FEATURE_COUNT = 19
    }
}