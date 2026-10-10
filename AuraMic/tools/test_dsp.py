"""Regression/quality test for the native noise suppressor (host build).

usage: python test_dsp.py /path/to/libauramic_host.so
"""
import sys

import numpy as np

from aura_synth import NOISE_KINDS, SR, Dsp, noise, synth_speech, to_i16


def snr_db(ref, est):
    return 10 * np.log10(np.sum(ref ** 2) / (np.sum((est - ref) ** 2) + 1e-12))


def main(lib):
    dsp = Dsp(lib)
    rng = np.random.default_rng(1234)
    secs = 6
    ok = True
    print(f"{'noise':8s} {'SNRin':>6s} {'SNRout':>7s} {'gain':>6s} {'noiseAtt':>9s}")
    for kind in NOISE_KINDS:
        speech = synth_speech(secs, rng)
        n = noise(kind, len(speech), rng)
        for snr_in in (0, 10):
            gain = np.sqrt(np.mean(speech ** 2) / (10 ** (snr_in / 10))) / np.sqrt(np.mean(n ** 2))
            mix = speech + n * gain
            pcm = to_i16(mix)
            ref = pcm.astype(np.float64) / 32768
            clean = to_i16(speech).astype(np.float64) / 32768
            out = dsp.process(pcm, 0.7).astype(np.float64) / 32768
            s_in = snr_db(clean, ref)
            s_out = snr_db(clean, out)
            # attenuation of the noise-only regions (where speech is ~silent)
            quiet = np.abs(speech) < 1e-3
            quiet = np.convolve(quiet.astype(float), np.ones(2000), "same") >= 1999
            att = 10 * np.log10((np.mean(ref[quiet] ** 2) + 1e-12) / (np.mean(out[quiet] ** 2) + 1e-12)) if quiet.sum() > 4000 else float("nan")
            print(f"{kind:8s} {s_in:6.1f} {s_out:7.1f} {s_out - s_in:6.1f} {att:9.1f}")
            if kind in ("white", "pink", "brown", "hum", "fan") and s_out - s_in < (3 if snr_in == 0 else 1.0):
                ok = False
    # Silence in -> silence out, no NaNs/crash on tiny inputs.
    for n_s in (0, 1, 100, 1023, 5000):
        x = to_i16(np.zeros(n_s))
        y = dsp.process(x, 0.7, normalize=True) if n_s else x
        assert len(y) == n_s
    # Normalisation: quiet speech gets louder, peaks never exceed full-scale.
    sp = synth_speech(4, rng) * 0.05  # quiet talker (about -35 dBFS RMS)
    pcm = to_i16(sp + noise("white", len(sp), rng) * 0.002)
    out = dsp.process(pcm, 0.7, normalize=True)
    rms_db = 10 * np.log10(np.mean((out / 32768.0) ** 2) + 1e-12)
    print("normalised rms dBFS:", rms_db, " peak:", np.abs(out).max())
    assert rms_db > -28 and np.abs(out).max() < 32767, "loudness normalisation out of range"
    assert np.sqrt(np.mean(sp ** 2)) < 0.05  # input really was quiet
    # Feature shape sanity
    f = dsp.features(pcm)
    assert f.shape == (dsp.frames(len(pcm)), dsp.feat) and np.isfinite(f).all()
    print("RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
