# Il dataset originale pesa 55 MB, ha 85 colonne e 123.117 righe. 
# Metterlo così nell'app sarebbe scomodo: file grosso, valori con scale molto diverse, 
# colonne inutili. preprocess.py lo trasforma una volta sola, sul computer, in due file 
# piccoli e già pronti da leggere dall'app Android. Così il codice Kotlin dovrà solo
# leggere righe di numeri, senza fare pulizia dei dati.

import csv
import math
import os
import random

SEED = 42
PER_CLASS = 400
STREAM_SIZE = 20000
NORMAL = {"Thing_Speak", "MQTT_Publish", "Wipro_bulb"}

NUM_FEATURES = [
    "id.resp_p", "flow_duration", "fwd_pkts_tot", "bwd_pkts_tot",
    "fwd_pkts_per_sec", "bwd_pkts_per_sec", "flow_pkts_per_sec", "down_up_ratio",
    "flow_SYN_flag_count", "flow_ACK_flag_count", "flow_RST_flag_count",
    "fwd_pkts_payload.avg", "bwd_pkts_payload.avg", "flow_iat.avg",
    "fwd_init_window_size", "bwd_init_window_size",
]
PROTOS = ["tcp", "udp", "icmp"]

records = []
with open("RT_IOT2022.csv", newline="") as f:
    for r in csv.DictReader(f):
        values = [math.log1p(float(r[c])) for c in NUM_FEATURES]
        records.append((r["Attack_type"], r["proto"], values))
print("righe lette:", len(records))

n = len(NUM_FEATURES)
lo = [min(rec[2][i] for rec in records) for i in range(n)]
hi = [max(rec[2][i] for rec in records) for i in range(n)]
span = [(h - l) or 1.0 for l, h in zip(lo, hi)]


def to_row(rec):
    label, proto, values = rec
    scaled = [(v - lo[i]) / span[i] for i, v in enumerate(values)]
    onehot = [1 if proto == p else 0 for p in PROTOS]
    is_attack = 0 if label in NORMAL else 1
    return [label, is_attack] + ["%.5f" % x for x in scaled] + onehot


random.seed(SEED)
by_class = {}
for i, rec in enumerate(records):
    by_class.setdefault(rec[0], []).append(i)

train_idx = []
for label, idx in by_class.items():
    train_idx += random.sample(idx, min(PER_CLASS, len(idx) // 2))
train_set = set(train_idx)
rest = [i for i in range(len(records)) if i not in train_set]
stream_idx = random.sample(rest, STREAM_SIZE)
assert not train_set & set(stream_idx)

header = ["label", "is_attack"] + NUM_FEATURES + ["proto_" + p for p in PROTOS]


def save(idx, path):
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for i in idx:
            w.writerow(to_row(records[i]))
    print(path, (len(idx), len(header)))


os.makedirs("assets", exist_ok=True)
save(train_idx, "assets/train.csv")
save(stream_idx, "assets/stream.csv")