#!/usr/bin/env python3
"""Scores systems on the benchmark built by build_bench.py.

  python score.py BENCH_DIR DNSMOS_DIR system1 system2 ...      (each system = a folder of <clip id>.wav inside BENCH_DIR; 'clips' = unprocessed input)

Reports per clip and as group averages (all outputs are level-matched to the same active-speech level first):
  SIG / BAK / OVRL  DNSMOS P.835 (non-intrusive, higher is better)      - needs sig_bak_ovr.onnx in DNSMOS_DIR
  P808              DNSMOS P.808 overall                                   - needs model_v8.onnx in DNSMOS_DIR
  SRMR              speech-to-reverberation modulation ratio (non-intrusive; higher = less reverberant)
  PESQ / STOI       against the clean reference (synthetic clips only)
  HF                8-16 kHz level vs the clean reference in dB (0 = same, -20 = muffled) - the 16 kHz metrics above cannot see this
  resid             level of the output in frames where the clean reference is silent, dB relative to speech (lower = less noise left)
"""
import sys, json, pathlib, warnings
import numpy as np, soundfile as sf, scipy.signal as ss, onnxruntime as ort, librosa
warnings.filterwarnings("ignore")

B = pathlib.Path(sys.argv[1]); D = pathlib.Path(sys.argv[2]); systems = sys.argv[3:]
so = ort.SessionOptions(); so.intra_op_num_threads = 4
sess = ort.InferenceSession(str(D / "sig_bak_ovr.onnx"), so, providers=["CPUExecutionProvider"])
p808 = ort.InferenceSession(str(D / "model_v8.onnx"), so, providers=["CPUExecutionProvider"])


def dnsmos(x, sr=48000):
    a = librosa.resample(x, orig_sr=sr, target_sr=16000); L = int(9.01 * 16000)
    while len(a) < L: a = np.append(a, a)
    S, Bk, O, P = [], [], [], []
    for i in range(max(int(np.floor(len(a) / 16000 - 9.01)) + 1, 1)):
        seg = a[i * 16000: int((i + 9.01) * 16000)]
        if len(seg) < L: continue
        r = sess.run(None, {"input_1": seg[None].astype(np.float32)})[0][0]
        S.append(np.poly1d([-0.08397278, 1.22083953, 0.0052439])(r[0])); Bk.append(np.poly1d([-0.13166888, 1.60915514, -0.39604546])(r[1]))
        O.append(np.poly1d([-0.06766283, 1.11546468, 0.04602535])(r[2]))
        mel = librosa.feature.melspectrogram(y=seg[:-160], sr=16000, n_fft=321, hop_length=160, n_mels=120)
        P.append(p808.run(None, {"input_1": ((librosa.power_to_db(mel, ref=np.max) + 40) / 40).T[None].astype(np.float32)})[0][0][0])
    return np.mean(S), np.mean(Bk), np.mean(O), np.mean(P)


def active(x, w=960):
    n = len(x) // w; e = np.sqrt((x[: n * w].reshape(n, w) ** 2).mean(1) + 1e-12); return np.sqrt(np.mean(e[e > e.max() * 0.056] ** 2))


def level(x, target=0.063):  # -24 dB active-speech level
    return (x * target / (active(x) + 1e-9)).clip(-1, 1)


def srmr(x):
    import torch
    from torchmetrics.functional.audio import speech_reverberation_modulation_energy_ratio as f
    return float(f(torch.tensor(librosa.resample(x, orig_sr=48000, target_sr=16000)), 16000))


def load(p, n=None):
    x, sr = sf.read(p); x = x if x.ndim == 1 else x[:, 0]
    return x if n is None else np.pad(x, (0, max(0, n - len(x))))[:n]


def pesq_stoi(x, ref):
    from pesq import pesq; from pystoi import stoi
    a = librosa.resample(x, orig_sr=48000, target_sr=16000); r = librosa.resample(ref, orig_sr=48000, target_sr=16000)
    try: p = pesq(16000, r, a, "wb")
    except Exception: p = np.nan
    return p, stoi(r, a, 16000)


def hf_db(x, ref):
    """Level of the 8-16 kHz band relative to the clean reference, dB (0 = same; large negative = muffled). DNSMOS/PESQ only see < 8 kHz."""
    def band(y):
        f, P = ss.welch(y, 48000, nperseg=4096); return 10 * np.log10(P[(f >= 8000) & (f < 16000)].mean() + 1e-20) - 10 * np.log10(P[(f >= 100) & (f < 1000)].mean() + 1e-20)
    n = min(len(x), len(ref)); return band(x[:n]) - band(ref[:n])


def resid(x, ref):
    w = 960; n = min(len(x), len(ref)) // w
    fx = 10 * np.log10((x[: n * w].reshape(n, w) ** 2).mean(1) + 1e-12); fr = 10 * np.log10((ref[: n * w].reshape(n, w) ** 2).mean(1) + 1e-12)
    return np.mean(fx[fr < np.percentile(fr, 20)]) - np.percentile(fx, 90)


idx = json.load(open(B / "index.json")); rows = []
for e in idx:
    cid = e["id"]; ref = load(B / "refs" / f"{cid}.wav") if e["has_reference"] else None
    for sname in systems:
        x = level(load(B / sname / f"{cid}.wav", 576000)[:576000])
        S, Bk, O, P = dnsmos(x)
        r = dict(id=cid, system=sname, group=cid[:1], SIG=S, BAK=Bk, OVRL=O, P808=P, SRMR=srmr(x))
        if ref is not None:
            r["PESQ"], r["STOI"] = pesq_stoi(x, level(ref)[: len(x)]); r["resid"] = resid(x, level(ref)); r["HF"] = hf_db(x, level(ref))
        rows.append(r); print(cid, sname, " ".join(f"{k}={v:.2f}" for k, v in r.items() if isinstance(v, float)), flush=True)
json.dump(rows, open(B / "scores.json", "w"), indent=1, default=float)
