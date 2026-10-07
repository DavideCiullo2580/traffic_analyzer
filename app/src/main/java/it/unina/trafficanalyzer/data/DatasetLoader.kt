/**
 * Carica i dataset dagli asset dell'app (app/src/main/assets).
 * È l'unica parte legata ad Android: apre il file con l'AssetManager
 * e passa il contenuto a DatasetParser.
 * Legge da disco, quindi va chiamato fuori dal thread principale.
 */

package it.unina.trafficanalyzer.data

import android.content.Context

class DatasetLoader(private val context: Context) {

    fun loadTrain(): List<TrafficRecord> = load("train.csv")

    fun loadStream(): List<TrafficRecord> = load("stream.csv")

    private fun load(fileName: String): List<TrafficRecord> =
        context.assets.open(fileName).bufferedReader().use { DatasetParser.parse(it) }
}