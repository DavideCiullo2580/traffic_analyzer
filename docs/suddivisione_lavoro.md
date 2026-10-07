# Suddivisione del lavoro

Progetto: **Mobile IoT Traffic Analyzer** (Operating Systems for Mobile, Cloud and IoT)

Studenti: Davide Ciullo, Francesco Rogo

---

## Davide Ciullo

### Progetto e dati
- Creazione del progetto Android Studio.
- `TrafficRecord`: la classe che rappresenta un record del flusso.
- `DatasetLoader`: legge `train.csv` e `stream.csv` dagli asset.

### Nucleo di concorrenza
- `StreamSimulator`: il thread produttore che invia i record a ritmo regolabile.
- `ProcessingQueue`: la coda a capacità fissa dove i record aspettano di essere elaborati.
- `WorkerPool`: i thread consumatori, in numero configurabile, e la loro chiusura pulita.
- Stato condiviso dei risultati: contatori aggiornati dai worker in modo sicuro.

### Esecuzione e benchmark
- Esecuzione sequenziale (un solo thread, senza coda) e concorrente (con coda e worker).
- Codice dei benchmark: lancia le prove con 1, 2 e 4 worker e raccoglie i tempi.

**Perché a Davide:** è la parte centrale di Operating Systems (thread, coda, sincronizzazione) e produce i risultati sperimentali.

---

## Francesco Rogo

### Analisi del traffico
- `Classifier`: il classificatore k-NN scritto a mano.
- Taratura del costo del classificatore (valore di `k` e dimensione del training), perché il carico dei worker sia misurabile.

### Misurazione delle risorse
- `ResourceMonitor`: campiona CPU, memoria e numero di thread del processo durante l'elaborazione.

### Android e interfaccia
- Servizio in primo piano: tiene l'elaborazione in background e mostra la notifica.
- `ViewModel`: passa lo stato dell'elaborazione alla schermata.
- Schermata: avvio della prova, scelta della modalità e del numero di worker, risultati, anomalie e metriche.

**Perché a Francesco:** dà il carico di lavoro ai worker, misura le risorse e gestisce l'esecuzione in background su Android.

---

## Svolto insieme

- Architettura e interfacce iniziali.
- Integrazione delle parti.
- Test.
- Misure sul telefono.
- Analisi dei risultati.
- Relazione e preparazione dell'orale.

---

## Interfacce da scrivere insieme all'inizio

Sono i contratti tra le parti: servono a lavorare in parallelo senza aspettarsi.

- `TrafficRecord`: un record del flusso, con etichetta, `is_attack` e le 19 feature numeriche.
- `Classifier`: cosa riceve (un record) e cosa restituisce (la classe prevista).
- `PipelineConfig`: numero di worker, ritmo di invio, modalità sequenziale o concorrente.
- `PipelineMetrics`: tempo totale, throughput, record elaborati, anomalie rilevate.
- `ResourceMonitor`: funzioni per avviare, fermare e leggere i campioni di CPU, memoria e thread.

---

## Fasi

1. **Insieme:** progetto Android Studio, copia dei due CSV negli asset, interfacce.
2. **In parallelo:** ognuno scrive la propria parte contro le interfacce.
3. **Insieme:** integrazione, prove sul telefono, correzioni.
4. **Insieme:** benchmark con 1, 2 e 4 worker, analisi dei risultati, relazione.

---

## Per l'orale

Il professore può chiedere di qualsiasi parte del codice, anche di quella dell'altro. Ognuno spiega all'altro il proprio pezzo prima dell'integrazione, e si legge insieme il documento di progetto, dove ogni file ha la sua spiegazione.
