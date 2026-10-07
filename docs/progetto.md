# Mobile IoT Traffic Analyzer: documento di progetto

Aggiornato al 7 ottobre 2026 · Davide Ciullo

## Panoramica

App Android che simula l'arrivo di traffico IoT (dataset RT-IoT2022) e lo elabora in background, confrontando l'esecuzione sequenziale con quella concorrente a 1, 2 e 4 worker. Il cuore del progetto sono i concetti di Operating Systems: thread, coda, sincronizzazione, scheduling, risorse. La classificazione k-NN serve solo a dare ai worker un carico di lavoro misurabile.

Pipeline: dataset, simulatore (thread produttore), coda a capacità fissa, worker (thread consumatori) che chiamano il classificatore, risultati condivisi, interfaccia. Un monitor misura CPU, memoria e thread del processo.

Schema della pipeline (N vale 1, 2 o 4):

```
Dataset ──> Produttore ──put()──> Coda ──take()──> Worker × N ──> Risultati
(train e     (1 thread)          (capacità        (classify()     (PipelineMetrics)
 stream)                          fissa)           k-NN)                │
                                                                        v
ResourceMonitor (CPU, memoria, thread del processo) ────────────> Schermata (ViewModel)
```

Nella baseline sequenziale non ci sono coda né worker: un solo thread legge e classifica i record in un ciclo.

Chi fa cosa:

- **Davide**: progetto Android, `TrafficRecord`, `DatasetLoader`, `StreamSimulator`, `ProcessingQueue`, `WorkerPool`, stato condiviso dei risultati, esecuzione sequenziale e concorrente, benchmark.
- **Francesco**: `Classifier` (k-NN) e sua taratura, `ResourceMonitor`, servizio in background, `ViewModel` e schermata.
- **Insieme**: architettura, interfacce, integrazione, test, misure sul telefono, analisi, relazione e orale.

Stato: progetto creato e su GitHub, dati caricati e verificati, le quattro interfacce (`Classifier`, `PipelineConfig`, `PipelineMetrics`, `ResourceMonitor`) scritte e compilate. Prossimo passo: approvazione di Francesco, poi ognuno lavora sulle proprie implementazioni.

## Struttura della cartella

Tutto sta in `traffic-analyzer/`, repository Git privato su GitHub, branch `main`. In Android Studio usare la vista **Project** (non Android) per vedere le cartelle vere.

- `app/src/main/java/it/unina/trafficanalyzer/`: codice Kotlin dell'app, package `it.unina.trafficanalyzer`.
    - `data/`: lettura del dataset (`TrafficRecord`, `DatasetParser`, `DatasetLoader`).
    - `classifier/`: interfaccia `Classifier` (il k-NN di Francesco andrà qui).
    - `pipeline/`: `PipelineConfig` e `PipelineMetrics` (worker, coda e simulatore andranno qui).
    - `monitor/`: interfaccia `ResourceMonitor` (l'implementazione di Francesco andrà qui).
    - `MainActivity.kt`: per ora contiene la verifica provvisoria del caricamento.
- `app/src/main/assets/`: `train.csv` e `stream.csv`, impacchettati nell'APK e letti per nome.
- `app/src/main/keepRules/rules.keep`: regole per R8 create dal modello di Android Studio, solo commenti, da lasciare.
- `app/src/test/`: test unitari sul computer (parser, coda, worker, k-NN).
- `tools/`: `preprocess.py` e `tools/assets/` con i CSV prodotti dallo script. `RT_IOT2022.csv` (55 MB) **non** è su Git: chi deve rieseguire lo script lo scarica da UCI (archive.ics.uci.edu/dataset/942/rt-iot2022) e lo mette in `tools/` con quel nome.
- `docs/`: note in markdown (`preprocess.md`, `suddivisione_lavoro.md`, questo file).

I CSV stanno in due posti: `tools/assets` è l'uscita dello script, `app/src/main/assets` è la copia usata dall'app. Se lo script viene rieseguito, vanno ricopiati.

## File per file

I tre file del package `data` sono scritti e verificati: caricano 3.625 record di training e 20.000 di stream. Il flusso è: chi chiama `DatasetLoader` riceve una lista di `TrafficRecord`; il loader apre il file, il parser legge riga per riga e crea un `TrafficRecord` per riga, nello stesso ordine del file.

**`data/TrafficRecord.kt`**: un record del dataset (lo "stampo" dei dati).

- Campi: `label` (classe, es. `DOS_SYN_Hping`), `isAttack`, `features` (19 numeri normalizzati tra 0 e 1).
- `FEATURE_COUNT = 19`: 16 feature numeriche più `proto_tcp`, `proto_udp`, `proto_icmp`.
- `DoubleArray` e non `List<Double>`: array di primitivi contigui, senza un oggetto per numero; conta nel k-NN, che fa milioni di sottrazioni.
- Classe normale e non `data class`: l'`equals` generato confronterebbe gli array per riferimento, non per contenuto.

**`data/DatasetParser.kt`**: trasforma il testo di un CSV in una lista di `TrafficRecord`.

- `object`: una sola istanza, il parser non ha stato.
- Riceve un `BufferedReader`, non un nome di file: non dipende da Android ed è testabile sul computer.
- Salta l'intestazione e le righe vuote; `readLine()` gestisce anche gli a capo di Windows.
- Ogni riga deve avere 21 colonne e `is_attack` deve valere `0` o `1`, altrimenti lancia un'eccezione con la riga sbagliata. Meglio fermarsi subito che lavorare su dati sbagliati in silenzio.
- La feature `i` si trova alla colonna `i + 2`, perché le prime due sono `label` e `is_attack`.

**`data/DatasetLoader.kt`**: apre i CSV dagli asset e li passa al parser.

- Unica parte legata ad Android: usa il `Context` per arrivare all'`AssetManager`.
- `use { }` chiude il file anche in caso di eccezione (come il try-with-resources di Java).
- Legge da disco: va chiamato fuori dal thread principale.

**`MainActivity.kt`**: per ora contiene solo una verifica **provvisoria** che carica i dati e scrive i conteggi in Logcat (tag `Dataset`). Legge sul thread principale, quindi va tolta quando il caricamento passa in background.

Ogni file ha in cima un commento KDoc (`/** ... */`) che dice cosa fa.

## Interfacce

Le interfacce sono i contratti tra la parte di Davide e quella di Francesco: dicono *cosa* fa un componente, non *come*. Una volta concordate, ognuno può lavorare in parallelo. Regola comune: tutto ciò che viene condiviso tra thread è **immutabile** dopo la creazione, così si può leggere da più thread senza lock.

### `Classifier` (approvata)

File `classifier/Classifier.kt`. La scrive Davide, la implementa Francesco con `KnnClassifier(train, k)`, la chiamano i worker.

```kotlin
class Prediction(val label: String, val isAttack: Boolean)

interface Classifier {
    fun classify(record: TrafficRecord): Prediction
}
```

- `Prediction` contiene la classe prevista (per l'accuratezza per classe) e `isAttack` (per contare le anomalie).
- **Contratto sui thread**: `classify()` viene chiamato da 2 o 4 worker contemporaneamente. L'implementazione non deve modificare il proprio stato dopo la costruzione: più letture contemporanee di dati immutabili non creano race condition, quindi niente lock. Un contatore o un array di appoggio condiviso richiederebbe sincronizzazione e rallenterebbe i worker.

### `PipelineConfig` (approvata)

File `pipeline/PipelineConfig.kt`. La crea il `ViewModel` di Francesco, la legge la pipeline di Davide.

```kotlin
enum class ExecutionMode { SEQUENTIAL, PIPELINE }

data class PipelineConfig(
    val mode: ExecutionMode,
    val workerCount: Int = 1,
    val queueCapacity: Int = 100,
    val recordsPerSecond: Int? = null // null = benchmark
)
```

- `SEQUENTIAL`: baseline, un ciclo su un thread senza coda. `PIPELINE`: produttore, coda, `workerCount` worker.
- `recordsPerSecond`: un numero = simulazione con ritmo; `null` = benchmark, il produttore non aspetta mai (altrimenti il throughput misurerebbe il produttore).
- `queueCapacity`: capacità della `ArrayBlockingQueue`, decide quando scatta la backpressure. 100 è un valore provvisorio da tarare.
- Il blocco `init` rifiuta configurazioni insensate (0 worker, baseline con più worker o con ritmo).

### `PipelineMetrics` (approvata)

File `pipeline/PipelineMetrics.kt`. La riempie la pipeline di Davide alla fine dell'esecuzione, la mostra la schermata di Francesco.

```kotlin
data class PipelineMetrics(
    val config: PipelineConfig,
    val recordsProcessed: Long,
    val attacksDetected: Long,
    val correctPredictions: Long,
    val recordsPerWorker: List<Long>,
    val elapsedNanos: Long
)
```

- Più tre valori calcolati con `get()`: `elapsedMillis`, `throughput` (record al secondo), `accuracy`. Essendo calcolati, non possono risultare incoerenti con i contatori.
- È il risultato **finale**: si crea solo dopo che l'ultimo worker ha finito, quindi nessuno lo modifica mentre altri lo leggono.
- Contatori locali per worker o `AtomicLong`: sono due modi con cui `WorkerPool` arriva a questi numeri; entrambi producono lo stesso oggetto, quindi si possono confrontare senza cambiare l'interfaccia.
- `recordsPerWorker` mostra come lo scheduler ha distribuito il lavoro tra i thread.
- Il tempo si misura con `System.nanoTime()`, orologio monotono adatto agli intervalli; l'ora di sistema può saltare.
- `correctPredictions` (classe prevista uguale a quella vera) serve a tarare `k`.

### `ResourceMonitor` (approvata)

File `monitor/ResourceMonitor.kt`. La scrive Davide, la implementa Francesco, la usa il `ViewModel` durante un'esecuzione.

```kotlin
class ResourceSample(
    val timeMillis: Long,
    val cpuTimeMillis: Long,
    val usedMemoryBytes: Long,
    val threadCount: Int
)

interface ResourceMonitor {
    fun start(intervalMillis: Long)
    fun stop()
    fun samples(): List<ResourceSample>
}
```

- `cpuTimeMillis` è il tempo CPU **cumulativo** del processo. La percentuale di CPU si calcola dopo, dalla differenza tra due campioni divisa per il tempo trascorso: un valore oltre il 100% significa che il processo usa più core.
- `usedMemoryBytes`: memoria heap Java usata. `threadCount`: thread attivi nel processo.
- **Contratto sui thread**: `start()` avvia un thread separato che campiona a intervalli regolari. `samples()` può essere chiamato da un altro thread mentre il monitor scrive, quindi l'implementazione deve sincronizzare l'accesso alla lista e restituirne una copia.
- Il thread del monitor stesso compare nel conteggio dei thread e consuma un po' di CPU: va dichiarato nell'analisi.

## Registro delle decisioni

Ogni voce: la decisione e il perché, così si può difendere all'orale.

- **Kotlin, API minima 26, nessuna libreria esterna** oltre a quelle standard di Android: meno dipendenze da spiegare.
- **Thread espliciti** (`Thread`, `ExecutorService`, `BlockingQueue`) e non coroutine: all'orale bisogna poter dire cosa fa ogni thread. `StateFlow` solo per passare lo stato alla UI.
- **Coda a capacità fissa** (`ArrayBlockingQueue`): se i worker sono lenti il produttore si blocca su `put()` (backpressure). Chiusura dei worker con "poison pill".
- **Risultati**: contatori locali per worker uniti alla fine, oppure `AtomicLong`; si possono confrontare le due soluzioni.
- **k-NN scritto a mano**, distanza euclidea su feature normalizzate. Il costo per record si regola con `k` e con la dimensione del training: deve essere misurabile, altrimenti 1, 2 e 4 worker darebbero lo stesso tempo.
- **Due modalità**: simulazione con ritmo e benchmark senza ritmo (in `PipelineConfig`, `recordsPerSecond = null`).
- **Baseline sequenziale vera**: un ciclo su un thread senza coda, accanto alla pipeline con 1 worker.
- **Sviluppo sull'emulatore, misure solo sul telefono reale**: CPU e thread dell'emulatore non sono affidabili.
- **Dataset negli asset** e non in `res/raw`: file letti per nome, impacchettati così come sono.
- **Parser separato dal loader**: il parser non dipende da Android ed è testabile sul computer.
- **Parser rigido**: righe con colonne sbagliate o `is_attack` diverso da `0`/`1` fermano il caricamento.
- **Oggetti condivisi immutabili** (`TrafficRecord`, `PipelineConfig`, `PipelineMetrics`, `ResourceSample`, training del k-NN): leggibili da più thread senza lock.
- **Interfacce scritte da Davide, implementazioni divise**: `KnnClassifier` e l'implementazione di `ResourceMonitor` sono di Francesco.
- **Classi considerate traffico normale** (assunzione fatta sui nomi, da confermare): `Thing_Speak`, `MQTT_Publish`, `Wipro_bulb`. Le altre 9 sono attacchi.
- **Interfaccia con Jetpack Compose**, una schermata e un `ViewModel`: proposta, da confermare.

## Misure e note per l'orale

Unica misura finora, sull'emulatore (Medium Phone API 37), il 7 ottobre 2026: `train=3625 stream=20000 attacchi=18127`. Gli attacchi sono il 90,6% dello stream, coerente con il fatto che `DOS_SYN_Hping` è il 77% del dataset originale. Nessuna misura di prestazioni è stata ancora fatta.

Domande probabili e risposta breve:

- *Perché i worker possono usare lo stesso classificatore senza lock?* Perché il training set è immutabile dopo la costruzione: letture contemporanee di dati che nessuno modifica non creano race condition.
- *Perché una coda a capacità fissa?* Limita la memoria e rallenta il produttore quando i consumatori non tengono il passo (backpressure).
- *Perché il benchmark senza ritmo?* Con un ritmo imposto il throughput sarebbe quello del produttore, non dei worker.
- *Perché una baseline senza coda?* Per separare il costo della coda e dei thread in più dal guadagno del parallelismo.
- *Perché misure sul telefono e non sull'emulatore?* L'emulatore condivide la CPU del computer: core e thread non rispecchiano un dispositivo reale.
- *Perché il caricamento non può restare in `MainActivity`?* Leggere file sul thread principale blocca l'interfaccia; andrà in un thread in background.
- *Come si vede che i worker girano davvero in parallelo?* Dalla CPU del processo: tempo CPU cresciuto più del tempo reale (oltre il 100%) vuol dire più core usati insieme.

## Per Francesco: come iniziare

Quando le quattro interfacce sono approvate puoi iniziare in parallelo a Davide.

1. Accetta l'invito al repository GitHub e clonalo con Android Studio (File, New, Project from Version Control).
2. Usa la vista Project e avvia l'app sull'emulatore: in Logcat, filtro `tag:Dataset`, devi vedere `train=3625 stream=20000`.
3. Leggi questo documento, in particolare i contratti sui thread di `Classifier` e `ResourceMonitor`.
4. Lavora sulle tue parti: `KnnClassifier.kt` nel package `classifier` (dichiara `: Classifier`), poi l'implementazione di `ResourceMonitor` nel package `monitor` (per esempio `AndroidResourceMonitor.kt`, dichiara `: ResourceMonitor`), servizio in background, `ViewModel` e schermata.

Prossimi passi di Davide: `StreamSimulator`, `ProcessingQueue` e `WorkerPool`.

Domande aperte:

- Modello e versione Android del telefono per le misure.
- Conferma delle 3 classi considerate traffico normale.
- Conferma dell'interfaccia con Compose e `ViewModel`.
