# preprocess.py

Script che prepara i dati dell'app: trasforma il dataset RT-IoT2022 originale in due file piccoli, già normalizzati e pronti da leggere dall'app Android. Si esegue una sola volta, sul computer.

## Ingresso e uscita

| | File | Contenuto |
| --- | --- | --- |
| Ingresso | `RT_IOT2022.csv` | dataset originale: 123.117 righe, 85 colonne, circa 55 MB |
| Uscita | `assets/train.csv` | 3.625 record di riferimento per il classificatore k-NN |
| Uscita | `assets/stream.csv` | 20.000 record da inviare all'app come flusso di traffico |

Ogni riga dei due file in uscita ha 21 colonne: `label`, `is_attack`, 16 feature numeriche tra 0 e 1 e 3 colonne del protocollo (`proto_tcp`, `proto_udp`, `proto_icmp`).

## Cosa fa, in ordine

1. **Legge il dataset** riga per riga e tiene solo il tipo di traffico (`Attack_type`), il protocollo e 16 colonne numeriche scelte. Le altre colonne vengono ignorate.
2. **Applica il logaritmo** (`log1p`) ai 16 valori numerici, per avvicinare i valori molto grandi a quelli piccoli.
3. **Normalizza** ogni colonna tra 0 e 1 (min-max), usando minimo e massimo calcolati sull'intero dataset.
4. **Trasforma il protocollo** in tre colonne 0/1, una per `tcp`, `udp` e `icmp`.
5. **Assegna l'etichetta `is_attack`:** 0 per le classi `Thing_Speak`, `MQTT_Publish` e `Wipro_bulb`, 1 per tutte le altre.
6. **Sceglie i record di `train.csv`:** per ogni classe estrae a caso al massimo 400 record, e al massimo metà dei record della classe se è rara.
7. **Sceglie i record di `stream.csv`:** estrae a caso 20.000 record tra quelli non finiti in train.
8. **Controlla** che nessun record sia in entrambi i file.
9. **Scrive i due file** con una riga di intestazione, e stampa quante righe e colonne ha scritto in ciascuno.

## Elementi principali

| Elemento | Cosa fa |
| --- | --- |
| `SEED` | fissa il generatore casuale: il campionamento è ripetibile |
| `PER_CLASS` | massimo di record per classe in train (400) |
| `STREAM_SIZE` | numero di record dello stream (20.000) |
| `NORMAL` | insieme delle classi considerate traffico normale |
| `NUM_FEATURES` | elenco delle 16 colonne numeriche tenute |
| `PROTOS` | i tre protocolli che diventano colonne 0/1 |
| `to_row(rec)` | trasforma un record in una riga di output: etichetta, `is_attack`, 16 valori normalizzati, 3 valori del protocollo |
| `save(idx, path)` | scrive su file i record indicati e stampa righe e colonne |

## Come si esegue

Dalla cartella `tools`, con `RT_IOT2022.csv` accanto allo script:

```
python preprocess.py
```

Uscita attesa:

```
righe lette: 123117
assets/train.csv (3625, 21)
assets/stream.csv (20000, 21)
```

Usa solo la libreria standard di Python (`csv`, `math`, `random`, `os`): non serve installare nulla.
