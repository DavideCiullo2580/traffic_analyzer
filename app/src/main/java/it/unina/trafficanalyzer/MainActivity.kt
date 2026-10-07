package it.unina.trafficanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.unina.trafficanalyzer.ui.theme.TrafficAnalyzerTheme
import android.util.Log
import it.unina.trafficanalyzer.data.DatasetLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // PROVVISORIO: legge i file sul thread principale, da togliere dopo la verifica
        val loader = DatasetLoader(this)
        val train = loader.loadTrain()
        val stream = loader.loadStream()
        Log.d("Dataset", "train=${train.size} stream=${stream.size} attacchi=${stream.count { it.isAttack }}")
        enableEdgeToEdge()
        setContent {
            TrafficAnalyzerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrafficAnalyzerTheme {
        Greeting("Android")
    }
}