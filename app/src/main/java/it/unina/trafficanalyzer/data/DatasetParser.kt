/**
 * Trasforma il testo di un CSV (train.csv o stream.csv) in una lista di TrafficRecord.
 * Non dipende da Android: riceve un BufferedReader, quindi si può testare sul computer.
 * Se una riga è malformata lancia un'eccezione invece di produrre dati sbagliati.
 */

package it.unina.trafficanalyzer.data

import java.io.BufferedReader

object DatasetParser {

    // label + is_attack + feature
    private const val EXPECTED_COLUMNS = 2 + TrafficRecord.FEATURE_COUNT

    fun parse(reader: BufferedReader): List<TrafficRecord> {
        val records = ArrayList<TrafficRecord>()
        reader.readLine() // salta l'intestazione
        var line = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                records.add(parseLine(line))
            }
            line = reader.readLine()
        }
        return records
    }

    private fun parseLine(line: String): TrafficRecord {
        val parts = line.split(",")
        require(parts.size == EXPECTED_COLUMNS) {
            "Riga con ${parts.size} colonne invece di $EXPECTED_COLUMNS: $line"
        }
        val label = parts[0]
        val isAttack = when (parts[1]) {
            "1" -> true
            "0" -> false
            else -> throw IllegalArgumentException("is_attack non valido: ${parts[1]}")
        }
        val features = DoubleArray(TrafficRecord.FEATURE_COUNT) { i -> parts[i + 2].toDouble() }
        return TrafficRecord(label, isAttack, features)
    }
}