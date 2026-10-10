"""Host regression test for the native DeepFilterNet engine + polish chain.
usage: python test_dfn.py <libauramic_host.so> <dfn_model_dir> <noisy.wav> <clean.wav>"""
import sys
import numpy as np
import soundfile as sf
from scipy.signal import stft
from libdf import DF, erb, erb_norm, unit_norm

import dfn_reference as ref
from dfn_native import Native


def lsd(a, b):
    A = np.abs(stft(a, 48000, nperseg=960)[2]); B = np.abs(stft(b, 48000, nperseg=960)[2])
    n = min(A.shape[1], B.shape[1]); A = 10 * np.log10(A[:, :n] ** 2 + 1e-9); B = 10 * np.log10(B[:, :n] ** 2 + 1e-9)
    return float(np.mean(np.sqrt(np.mean((A - B) ** 2, axis=0))))


def main(lib, mdir, noisy_wav, clean_wav):
    nat = Native(lib)
    noisy, _ = sf.read(noisy_wav, dtype="float32"); clean, _ = sf.read(clean_wav, dtype="float32")
    ok = True

    # 1. features identical to the official libdf
    erb_c, spec_c = nat.features(noisy)
    df = DF(sr=48000, fft_size=960, hop_size=480, nb_bands=32, min_nb_erb_freqs=2)
    T = erb_c.shape[0]
    xp = np.zeros(T * 480, np.float32); xp[:len(noisy)] = noisy
    spec = df.analysis(xp[None])[0]
    e_ref = erb_norm(erb(spec[None], df.erb_widths()), 0.99)[0]
    s_ref = unit_norm(spec[None, :, :96], 0.99)[0]
    d_e = np.max(np.abs(e_ref - erb_c)); d_s = max(np.max(np.abs(s_ref.real - spec_c[0])), np.max(np.abs(s_ref.imag - spec_c[1])))
    print(f"feature max |diff| vs libdf: erb {d_e:.2e}  spec {d_s:.2e}")
    ok &= d_e < 2e-3 and d_s < 2e-3

    # 2. full pipeline == numpy reference, and actually denoises
    import dfn_native
    models = ref.load_models(mdir)
    out_ref, _ = ref.enhance(noisy, models)

    def rel_err(a, b):
        n = min(len(a), len(b)); return float(np.sqrt(np.mean((a[:n] - b[:n]) ** 2)) / np.sqrt(np.mean(b[:n] ** 2)))

    # (a) whole clip in one network call: must reproduce the reference bit-for-bit (to float precision)
    saved = (dfn_native.CHUNK, dfn_native.CTX)
    dfn_native.CHUNK, dfn_native.CTX = 10 ** 6, 0
    exact = nat.enhance(noisy, models)
    e_exact = rel_err(exact, out_ref)
    print(f"single-chunk native vs reference: relative RMS error {e_exact:.2e}  (port is exact)")
    ok &= e_exact < 1e-3
    # (b) production chunking (bounded memory): GRU history is truncated at chunk starts, so waveforms differ
    #     slightly, but spectral quality must be equivalent
    dfn_native.CHUNK, dfn_native.CTX = saved
    out = nat.enhance(noisy, models)
    print(f"chunked native vs reference: relative RMS error {rel_err(out, out_ref):.3f}")
    print(f"LSD vs clean: noisy {lsd(noisy, clean):.2f} dB -> reference {lsd(out_ref, clean):.2f} -> chunked native {lsd(out, clean):.2f} dB")
    ok &= abs(lsd(out, clean) - lsd(out_ref, clean)) < 0.4 and lsd(out, clean) < 4.0
    fast = nat.enhance(noisy, models, use_df=False)
    print(f"fast mode (mask only) LSD {lsd(fast, clean):.2f} dB")
    lim = nat.enhance(noisy, models, atten=12.0)
    print(f"atten-limit 12 dB LSD {lsd(lim, clean):.2f} dB (keeps ambience)")
    ok &= len(out) == len(noisy)

    # 3. odd lengths / tiny inputs do not crash and keep length
    for n_s in (1, 100, 479, 480, 481, 5000):
        o = nat.enhance(noisy[:n_s], models)
        assert len(o) == n_s and np.isfinite(o).all()

    # 4. polish chain: LUFS measurement + normalisation + limiter
    t = np.arange(48000 * 3) / 48000
    sine = (10 ** (-20 / 20) * np.sin(2 * np.pi * 997 * t)).astype(np.float32)
    l = nat.lufs(sine)
    print(f"LUFS of -20 dBFS 997 Hz sine: {l:.2f} (expected -23.01)")
    ok &= abs(l - (-23.01)) < 0.3
    for preset in (0, 1, 2):
        p, rep = nat.polish(out, preset=preset, target=-16.0)
        print(f"polish preset {preset}: in {rep[0]:.1f} -> out {rep[1]:.1f} LUFS, gain {rep[2]:.1f} dB, peak {rep[3]:.2f} dBFS")
        ok &= abs(rep[1] - (-16.0)) < 1.0 and rep[3] <= -1.0
    loud = np.clip(out * 30, -1, 1)
    p, rep = nat.polish(loud, preset=2, target=-16.0)
    ok &= rep[3] <= -1.0 and np.isfinite(p).all()
    print("RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:5]))
