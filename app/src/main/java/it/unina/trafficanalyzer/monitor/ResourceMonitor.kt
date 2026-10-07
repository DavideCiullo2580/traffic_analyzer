/**
 * Le risorse del processo misurate in un istante.
 * Immutabile: creato dal thread del monitor, letto dall'interfaccia.
 */

package it.unina.trafficanalyzer.monitor
class ResourceSample(
    val timeMillis: Long,
    val cpuTimeMillis: Long,
    val usedMemoryBytes: Long,
    val threadCount: Int
)

/**
 * Misura CPU, memoria e thread del processo mentre la pipeline lavora.
 * Contratto: start() avvia un thread separato che campiona ogni intervalMillis.
 * samples() può essere chiamato da un altro thread mentre il monitor scrive,
 * quindi l'implementazione deve sincronizzare l'accesso e restituire una copia.
 */
interface ResourceMonitor {
    fun start(intervalMillis: Long)
    fun stop()
    fun samples(): List<ResourceSample>
}