"""Reference (numpy + official libdf) implementation of the offline DeepFilterNet3 pipeline that the
Android app re-implements in C++/Kotlin. Used to validate alignment and quality.

Streaming semantics (libDF/src/tract.rs) re-expressed for whole-clip processing:
  * features for frame t go through the (causal) ONNX encoder; conv_lookahead=2, so the mask/coefs/lsnr
    that belong to frame o are the network outputs at index o+2  (features zero-padded at the end);
  * mask is applied to X[o] on all bins; deep filtering fills bins < nb_df from noisy frames o-2 .. o+2.
"""
import numpy as np
import onnxruntime as ort
from libdf import DF, erb, erb_norm, unit_norm

SR, FFT, HOP, NB_ERB, NB_DF, ORDER, LOOKAHEAD, CONV_LA = 48000, 960, 480, 32, 96, 5, 2, 2
ALPHA = 0.99


def load_models(d):
    so = ort.SessionOptions(); so.intra_op_num_threads = 4
    return [ort.InferenceSession(f"{d}/{n}.onnx", so, providers=["CPUExecutionProvider"]) for n in ("enc", "erb_dec", "df_dec")]


def enhance(x, models, atten_lim_db=100.0, min_db=-10.0, max_erb=30.0, max_df=20.0, use_df=True, la_shift=CONV_LA):
    enc, erb_dec, df_dec = models
    df = DF(sr=SR, fft_size=FFT, hop_size=HOP, nb_bands=NB_ERB, min_nb_erb_freqs=2)
    n = len(x)
    frames = int(np.ceil(n / HOP)) + 3
    xp = np.zeros(frames * HOP, np.float32); xp[:n] = x
    spec = df.analysis(xp[None, :])[0]                     # [T, 481] complex
    T = spec.shape[0]
    e_feat = erb_norm(erb(spec[None], df.erb_widths()), ALPHA)       # [1,T,32]
    s_feat = unit_norm(spec[None, :, :NB_DF], ALPHA)                 # [1,T,96] complex
    fe = e_feat[:, None].astype(np.float32)                           # [1,1,T,32]
    fs = np.stack([s_feat.real, s_feat.imag], 1).astype(np.float32)   # [1,2,T,96]
    # pad conv lookahead zeros at the end
    fe = np.concatenate([fe, np.zeros((1, 1, la_shift, NB_ERB), np.float32)], 2)
    fs = np.concatenate([fs, np.zeros((1, 2, la_shift, NB_DF), np.float32)], 2)
    e0, e1, e2, e3, emb, c0, lsnr = enc.run(None, {"feat_erb": fe, "feat_spec": fs})
    m = erb_dec.run(None, {"emb": emb, "e3": e3, "e2": e2, "e1": e1, "e0": e0})[0][0, 0]       # [T+la, 32]
    coefs = df_dec.run(None, {"emb": emb, "c0": c0})[0][0].reshape(-1, NB_DF, ORDER, 2)         # [T+la, 96, 5, 2]
    lsnr = lsnr[0, :, 0]
    m, coefs, lsnr = m[la_shift:la_shift + T], coefs[la_shift:la_shift + T], lsnr[la_shift:la_shift + T]
    widths = np.asarray(df.erb_widths(), dtype=np.int64)
    out = np.zeros_like(spec)
    for o in range(T):
        l = lsnr[o]
        if l < min_db:        stage_gain, stage_zero, stage_df = False, True, False
        elif l > max_erb:     stage_gain, stage_zero, stage_df = False, False, False
        elif l > max_df:      stage_gain, stage_zero, stage_df = True, False, False
        else:                 stage_gain, stage_zero, stage_df = True, False, True
        y = spec[o].copy()
        if stage_gain:
            g = np.repeat(m[o], widths); y = y * g
        elif stage_zero:
            y = y * 0
        if stage_df and use_df:
            acc = np.zeros(NB_DF, np.complex64)
            for k in range(ORDER):
                t = o - (ORDER - 1 - LOOKAHEAD) + k - 0
                t = o - 2 + k
                if 0 <= t < T:
                    c = coefs[o, :, k, 0] + 1j * coefs[o, :, k, 1]
                    acc += spec[t, :NB_DF] * c
            y[:NB_DF] = acc
        if atten_lim_db < 100:
            lim = 10 ** (-atten_lim_db / 20); y = y * (1 - lim) + spec[o] * lim
        out[o] = y
    enh = df.synthesis(out[None])[0]
    # libdf analysis/synthesis delay: one hop (480 samples)
    return enh[HOP:HOP + n], lsnr


def snr(ref, est):
    n = min(len(ref), len(est)); ref, est = ref[:n], est[:n]
    return 10 * np.log10(np.sum(ref ** 2) / (np.sum((est - ref) ** 2) + 1e-12))
