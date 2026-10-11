#!/usr/bin/env python3
"""Builds the AuraMic voice-enhancement benchmark from public, openly licensed data (internal evaluation only).

  speech   EARS (anechoic, 48 kHz; CC BY-NC 4.0 -> evaluation only, never ship or publish)
  noise    DEMAND (48 kHz, CC BY 4.0)
  rooms    MIT Environmental Impulse Response Survey (real rooms, CC BY 4.0)
  real     DNS Challenge 2020 dev test, real recordings (16 kHz, CC BY 4.0) - no clean reference

Output (OUT dir):
  clips/<id>.wav        noisy/reverberant input clips (48 kHz mono 16-bit, ~12 s)
  refs/<id>.wav         clean reference where one exists
  bench_all.wav         every clip joined with 1.5 s gaps - ONE file to upload to a reference enhancer (e.g. Adobe Enhance Speech)
  index.json            id, condition, start/end seconds inside bench_all.wav

  python build_bench.py OUT_DIR
"""
import io, json, sys, pathlib
import numpy as np, soundfile as sf, scipy.signal as ss
import pyarrow.parquet as pq
from remotezip import RemoteZip

SR = 48_000
LEN = 12 * SR
rng = np.random.default_rng(7)
OUT = pathlib.Path(sys.argv[1]); (OUT / "clips").mkdir(parents=True, exist_ok=True); (OUT / "refs").mkdir(exist_ok=True)
RAW = OUT / "raw"; RAW.mkdir(exist_ok=True)


def fetch(url, name):
    p = RAW / name
    if not p.exists():
        import urllib.request
        urllib.request.urlretrieve(url, p)
    return p


def active_rms(x, win=int(0.02 * SR)):
    n = len(x) // win; f = x[: n * win].reshape(n, win); e = np.sqrt((f ** 2).mean(1) + 1e-12)
    return np.sqrt(np.mean(e[e > e.max() * 10 ** (-25 / 20)] ** 2))


def mix(speech, noise, snr_db):
    g = active_rms(speech) / (np.sqrt(np.mean(noise ** 2)) * 10 ** (snr_db / 20))
    return speech + g * noise


def finish(x, level_db=-24):
    return (x * (10 ** (level_db / 20) / np.sqrt(np.mean(x ** 2) + 1e-12))).clip(-0.98, 0.98)


def resample(x, sr):
    return x if sr == SR else ss.resample_poly(x, SR, sr)


def retry(fn, tries=6):
    import time
    for i in range(tries):
        try: return fn()
        except Exception as e:
            print("retry", i + 1, type(e).__name__, flush=True); time.sleep(3 * (i + 1))
    raise RuntimeError("download failed")


def cached(name, make):
    p = RAW / f"{name}.npy"
    if not p.exists(): np.save(p, retry(make))
    return np.load(p)


# ---------------- speech (EARS) ----------------
speech = []
def ears(spk, k):
    z = RemoteZip(f"https://huggingface.co/datasets/zhu-han/EARS/resolve/main/{spk}.zip")
    names = sorted(n for n in z.namelist() if "sentences_" in n and n.endswith("_regular.wav"))
    x, sr = sf.read(io.BytesIO(z.read(names[k])), always_2d=False); return resample(x if x.ndim == 1 else x[:, 0], sr)

for spk in ["p003", "p010", "p025", "p050", "p070", "p095"]:
    for k, name in enumerate(["a", "b"]):
        x = cached(f"ears_{spk}_{k}", lambda: ears(spk, k))
        # drop the long leading/trailing silence, keep the first 12 s of speech
        e = np.convolve(np.abs(x), np.ones(2400) / 2400, "same"); idx = np.where(e > e.max() * 0.04)[0]
        x = x[idx[0]: idx[0] + LEN]
        if len(x) >= LEN * 0.9: speech.append(np.pad(x, (0, LEN - len(x)))); print("speech", spk, k, flush=True)
speech = speech[:12]

# ---------------- noise (DEMAND) ----------------
def demand_raw(name):
    z = RemoteZip(f"https://zenodo.org/records/1227121/files/{name}_48k.zip?download=1")
    n = sorted(k for k in z.namelist() if k.endswith(".wav"))[0]
    x, sr = sf.read(io.BytesIO(z.read(n))); x = x if x.ndim == 1 else x[:, 0]
    return resample(x, sr)[: 60 * SR]


def demand(name):
    x = cached(f"demand_{name}", lambda: demand_raw(name)); s = int(rng.integers(0, len(x) - LEN)); print("noise", name, flush=True); return x[s: s + LEN]

noise = {k: demand(k) for k in ["DLIVING", "STRAFFIC", "SCAFE", "PSTATION", "TCAR", "OOFFICE", "DWASHING", "NPARK"]}

# ---------------- real rooms (MIT IR survey) ----------------
t = pq.read_table(fetch("https://huggingface.co/datasets/benjamin-paine/mit-impulse-response-survey/resolve/main/data/train-00000-of-00001.parquet", "mit_ir.parquet")).to_pylist()
def rt60(h, sr):
    e = np.cumsum((h ** 2)[::-1])[::-1]; d = 10 * np.log10(e / e[0] + 1e-12)
    try: i5 = np.where(d < -5)[0][0]; i25 = np.where(d < -25)[0][0]
    except IndexError: return 0
    return 3 * (i25 - i5) / sr
rirs = []
for r in t:
    h, sr = sf.read(io.BytesIO(r["audio"]["bytes"])); h = h if h.ndim == 1 else h[:, 0]; h = resample(h, sr) if sr != SR else h
    h = h[np.argmax(np.abs(h)):]; rirs.append((rt60(h, SR), h / np.max(np.abs(h))))
def drr(h):
    k = int(0.0025 * SR); d = np.sum(h[:k] ** 2); return 10 * np.log10(d / (np.sum(h[k:] ** 2) + 1e-12) + 1e-12)
rirs = [r for r in rirs if r[0] > 0 and drr(r[1]) > -3]          # keep speech intelligible: skip IRs that are nearly all tail
def pick(target): return min(rirs, key=lambda r: abs(r[0] - target))
def reverb(x, h): y = ss.fftconvolve(x, h)[: len(x)]; return y * (np.sqrt(np.mean(x ** 2)) / np.sqrt(np.mean(y ** 2)))

echo_ir = np.zeros(int(0.5 * SR)); echo_ir[0] = 1; echo_ir[int(0.14 * SR)] = 0.45; echo_ir[int(0.28 * SR)] = 0.2

# ---------------- clip list ----------------
import itertools
S = itertools.cycle(speech)
conds = []
def add(cid, desc, noisy, clean=None): conds.append((cid, desc, noisy, clean))
a = next(S); add("s01_room_light_20dB", "quiet living room, light noise (20 dB SNR)", mix(a, noise["DLIVING"], 20), a)
a = next(S); add("s02_traffic_10dB", "street traffic, 10 dB SNR", mix(a, noise["STRAFFIC"], 10), a)
a = next(S); add("s03_traffic_3dB", "street traffic, 3 dB SNR (hard)", mix(a, noise["STRAFFIC"], 3), a)
a = next(S); add("s04_cafe_8dB", "cafe babble, 8 dB SNR", mix(a, noise["SCAFE"], 8), a)
a = next(S); add("s05_station_crowd_5dB", "station crowd, 5 dB SNR", mix(a, noise["PSTATION"], 5), a)
a = next(S); add("s06_car_5dB", "inside a car, 5 dB SNR", mix(a, noise["TCAR"], 5), a)
a = next(S); add("s07_office_10dB", "office, 10 dB SNR", mix(a, noise["OOFFICE"], 10), a)
a = next(S); add("s08_washing_machine_5dB", "washing machine, 5 dB SNR", mix(a, noise["DWASHING"], 5), a)
a = next(S); add("s09_park_10dB", "park (wind, birds), 10 dB SNR", mix(a, noise["NPARK"], 10), a)
for tgt, tag in [(0.4, "small_room"), (0.8, "large_room"), (1.5, "hall")]:
    a = next(S); r, h = pick(tgt); add(f"s1{[0.4,0.8,1.5].index(tgt)}_reverb_{tag}", f"reverberant {tag}, RT60 ~{r:.1f} s, no noise", reverb(a, h), a)
a = next(S); r, h = pick(0.6); add("s13_reverb_noise_10dB", f"reverb RT60 ~{r:.1f} s + living-room noise 10 dB", mix(reverb(a, h), noise["DLIVING"], 10), a)
a = next(S); r, h = pick(1.0); add("s14_reverb_traffic_8dB", f"reverb RT60 ~{r:.1f} s + traffic 8 dB", mix(reverb(a, h), noise["STRAFFIC"], 8), a)
a = speech[3]; add("s15_echo_140ms", "slap echo 140 ms / 280 ms, no noise", ss.fftconvolve(a, echo_ir)[: len(a)], a)
a = speech[5]; add("s16_echo_noise_10dB", "slap echo + cafe noise 10 dB", mix(ss.fftconvolve(a, echo_ir)[: len(a)], noise["SCAFE"], 10), a)

# ---------------- real recordings (DNS) ----------------
rows = pq.read_table(fetch("https://huggingface.co/datasets/nkdem/DNS-Challenge-2020-DevTest-16k/resolve/main/data/real_recordings-00000-of-00001.parquet", "dns_real.parquet")).to_pylist()
cand = []
for r in rows:
    x, sr = sf.read(io.BytesIO(r["noisy"]["bytes"])); x = x if x.ndim == 1 else x[:, 0]
    if 8 <= len(x) / sr <= 15:
        n = int(0.02 * sr); f = x[: len(x) // n * n].reshape(-1, n); e = 10 * np.log10((f ** 2).mean(1) + 1e-10)
        if np.percentile(e, 90) - np.percentile(e, 10) > 12 and np.percentile(e, 5) > -90 and len(x) / sr >= 10: cand.append((r["fileid"], resample(x, sr)))
for k in rng.permutation(len(cand))[:6]:
    cid, x = cand[k]; x = np.pad(x[:LEN], (0, max(0, LEN - len(x)))) if len(x) < LEN else x[:LEN]
    add(f"r{len([c for c in conds if c[0].startswith('r')]) + 1:02d}_real_{cid.split('.')[0][-8:]}", "real noisy recording (DNS Challenge, 16 kHz source)", x, None)

# ---------------- write ----------------
gap = np.zeros(int(1.5 * SR)); big = [gap]; idx = []; pos = 1.5
for cid, desc, noisy, clean in conds:
    y = finish(noisy); sf.write(OUT / "clips" / f"{cid}.wav", y, SR, subtype="PCM_16")
    if clean is not None: sf.write(OUT / "refs" / f"{cid}.wav", finish(clean, -24), SR, subtype="PCM_16")
    big += [y, gap]; idx.append(dict(id=cid, condition=desc, start=round(pos, 3), end=round(pos + len(y) / SR, 3), has_reference=clean is not None)); pos += len(y) / SR + 1.5
sf.write(OUT / "bench_all.wav", np.concatenate(big), SR, subtype="PCM_16")
json.dump(idx, open(OUT / "index.json", "w"), indent=1)
print(len(idx), "clips,", round(pos / 60, 1), "min ->", OUT / "bench_all.wav")
