"""Trains the TFLite band-mask refinement model shipped in app/src/main/assets/aura_mask.tflite.

Pipeline (all features come from the *same C++ code* the app runs, via ctypes):
  synthetic speech + noise (7 noise families, -5..20 dB SNR)
    -> aura_extract_features()  : per frame 32 SNR features + 32 DSP gains
    -> stack +-2 frames          : 320-dim input
  target: ideal ratio mask per mel band  sqrt(S / (S + N))
  model : 320 -> 128 -> 96 -> 32 (sigmoid)      ~55k params, ~220 kB float32

usage: python train_mask_model.py <libauramic_host.so> <out.tflite> [clips]

HONEST NOTE: the training audio is synthetic (formant-filtered glottal pulse trains +
fricatives). The native DSP is the main denoiser; this model is a light second opinion that is
blended in geometrically (exponent 0.5). Swap in a model trained on real speech with the same
I/O contract (see MaskModel.kt) for better results; no app code changes are needed.
"""
import sys

import numpy as np
import tensorflow as tf

from aura_synth import NOISE_KINDS, SR, Dsp, noise, synth_speech, to_i16

CONTEXT = 2


def stack_context(feat):
    f = feat.shape[0]
    idx = np.clip(np.arange(f)[:, None] + np.arange(-CONTEXT, CONTEXT + 1)[None, :], 0, f - 1)
    return feat[idx].reshape(f, -1)


def make_clip(dsp, rng, seconds=4.0):
    speech = synth_speech(seconds, rng)
    kind = NOISE_KINDS[rng.integers(len(NOISE_KINDS))]
    n = noise(kind, len(speech), rng)
    snr = rng.uniform(-5, 20)
    g = np.sqrt(np.mean(speech ** 2) / (10 ** (snr / 10))) / np.sqrt(np.mean(n ** 2))
    noise_sig = n * g
    level = rng.uniform(0.15, 0.9)  # random overall level so the model is level-agnostic
    mix = to_i16((speech + noise_sig) * level)
    feat = dsp.features(mix)
    es = dsp.band_energies(to_i16(speech * level)) + 1e-12
    en = dsp.band_energies(to_i16(noise_sig * level)) + 1e-12
    irm = np.sqrt(es / (es + en)).astype(np.float32)
    return stack_context(feat), irm


def build_dataset(dsp, clips, seed):
    rng = np.random.default_rng(seed)
    xs, ys = [], []
    for i in range(clips):
        x, y = make_clip(dsp, rng)
        xs.append(x); ys.append(y)
    return np.concatenate(xs).astype(np.float32), np.concatenate(ys).astype(np.float32)


def main(lib, out, clips=240):
    tf.random.set_seed(7)
    dsp = Dsp(lib)
    in_dim = (2 * CONTEXT + 1) * dsp.feat
    print("building dataset ...")
    x, y = build_dataset(dsp, clips, seed=1)
    xv, yv = build_dataset(dsp, 24, seed=99)
    print("train", x.shape, y.shape, " val", xv.shape)

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(in_dim,), name="features"),
        tf.keras.layers.Dense(128, activation="relu"),
        tf.keras.layers.Dense(96, activation="relu"),
        tf.keras.layers.Dense(dsp.bands, activation="sigmoid", name="mask"),
    ])
    model.compile(optimizer=tf.keras.optimizers.Adam(2e-3), loss="mse")
    model.fit(x, y, validation_data=(xv, yv), epochs=24, batch_size=512, verbose=2,
              callbacks=[tf.keras.callbacks.ReduceLROnPlateau(patience=3, factor=0.5, verbose=1)])
    base = float(np.mean((yv - yv.mean(0)) ** 2))
    print(f"val mse {model.evaluate(xv, yv, verbose=0):.5f}  (predict-the-mean baseline {base:.5f})")

    conv = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite = conv.convert()
    with open(out, "wb") as f:
        f.write(tflite)
    print(f"wrote {out} ({len(tflite) / 1024:.0f} kB)")
    validate(dsp, out)


def validate(dsp, path):
    """End-to-end check with the exported TFLite file: DSP only vs DSP + model mask."""
    itp = tf.lite.Interpreter(model_path=path)
    in_idx = itp.get_input_details()[0]["index"]
    out_idx = itp.get_output_details()[0]["index"]

    def mask_for(pcm):
        x = stack_context(dsp.features(pcm)).astype(np.float32)
        itp.resize_tensor_input(in_idx, [len(x), x.shape[1]])
        itp.allocate_tensors()
        itp.set_tensor(in_idx, x)
        itp.invoke()
        return itp.get_tensor(out_idx).copy()

    def snr(ref, est):
        return 10 * np.log10(np.sum(ref ** 2) / (np.sum((est - ref) ** 2) + 1e-12))

    rng = np.random.default_rng(2024)
    print(f"\n{'noise':8s} {'in':>6s} {'dsp':>6s} {'dsp+model':>10s}   (SNR dB, 0 dB input, fresh clips)")
    tot_d = tot_m = 0
    for kind in NOISE_KINDS:
        sp = synth_speech(5, rng)
        nz = noise(kind, len(sp), rng)
        g = np.sqrt(np.mean(sp ** 2) / 1.0) / np.sqrt(np.mean(nz ** 2))
        pcm = to_i16(sp + nz * g)
        ref = to_i16(sp).astype(np.float64) / 32768
        inp = pcm.astype(np.float64) / 32768
        d = dsp.process(pcm, 0.7).astype(np.float64) / 32768
        m = dsp.process(pcm, 0.7, mask=mask_for(pcm), mask_weight=0.5).astype(np.float64) / 32768
        print(f"{kind:8s} {snr(ref, inp):6.1f} {snr(ref, d):6.1f} {snr(ref, m):10.1f}")
        tot_d += snr(ref, d); tot_m += snr(ref, m)
    print(f"mean     {'':6s} {tot_d / len(NOISE_KINDS):6.1f} {tot_m / len(NOISE_KINDS):10.1f}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 240)
