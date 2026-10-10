"""Synthetic speech-like and noise signal generators + ctypes wrapper for the native DSP.

Used by test_dsp.py (regression test of the C++ engine) and train_mask_model.py (to build
a training set for the TFLite band-mask model). No real recordings are required.
"""
import ctypes
import os

import numpy as np
from scipy.signal import lfilter

SR = 48000

# Rough first three formants (Hz) of a handful of vowels.
VOWELS = [(730, 1090, 2440), (270, 2290, 3010), (300, 870, 2240), (530, 1840, 2480), (570, 840, 2410)]


def _resonator(freq, bw, sr):
    r = np.exp(-np.pi * bw / sr)
    theta = 2 * np.pi * freq / sr
    return [1 - r], [1, -2 * r * np.cos(theta), r * r]


def synth_speech(seconds, rng, sr=SR, level=0.25):
    """Voiced syllables (glottal pulse train -> formant filters) + fricative bursts + pauses."""
    n = int(seconds * sr)
    out = np.zeros(n)
    pos = int(0.2 * sr * rng.random())
    f0_base = rng.uniform(95, 230)
    while pos < n:
        dur = int(rng.uniform(0.12, 0.38) * sr)
        gap = int(rng.uniform(0.05, 0.45) * sr)
        end = min(n, pos + dur)
        m = end - pos
        if m <= 0:
            break
        t = np.arange(m) / sr
        if rng.random() < 0.8:  # voiced syllable
            f0 = f0_base * (1 + 0.15 * np.sin(2 * np.pi * rng.uniform(2, 6) * t) + rng.uniform(-0.1, 0.1) * t / max(t[-1], 1e-3))
            phase = 2 * np.pi * np.cumsum(f0) / sr
            src = np.zeros(m)
            for h in range(1, int(min(sr / 2, 5000) / f0_base)):
                src += np.sin(h * phase) / (h ** 1.1)
            sig = src
            f1, f2, f3 = VOWELS[rng.integers(len(VOWELS))]
            for f, bw in ((f1, 80), (f2, 110), (f3, 160)):
                b, a = _resonator(f * rng.uniform(0.95, 1.05), bw, sr)
                sig = lfilter(b, a, sig)
        else:  # fricative: high-passed noise
            sig = lfilter([1, -0.95], [1], rng.standard_normal(m)) * 0.3
        env = np.hanning(m) ** 0.7
        sig = sig * env
        out[pos:end] += sig / (np.max(np.abs(sig)) + 1e-9)
        pos = end + gap
    out *= level / (np.sqrt(np.mean(out ** 2)) + 1e-9) * 3.0
    return np.clip(out, -0.95, 0.95)


def noise(kind, n, rng, sr=SR):
    if kind == "white":
        x = rng.standard_normal(n)
    elif kind == "pink":
        x = lfilter([0.049922035, -0.095993537, 0.050612699, -0.004408786],
                    [1, -2.494956002, 2.017265875, -0.522189400], rng.standard_normal(n))
    elif kind == "brown":
        x = np.cumsum(rng.standard_normal(n))
        x -= np.convolve(x, np.ones(2000) / 2000, mode="same")
    elif kind == "hum":
        t = np.arange(n) / sr
        f = rng.choice([50.0, 60.0])
        x = sum(np.sin(2 * np.pi * f * h * t + rng.random() * 6.28) / h for h in range(1, 12))
        x += 0.05 * rng.standard_normal(n)
    elif kind == "fan":
        b, a = _resonator(rng.uniform(300, 900), 400, sr)
        x = lfilter(b, a, rng.standard_normal(n))
        x *= 1 + 0.2 * np.sin(2 * np.pi * rng.uniform(0.3, 1.5) * np.arange(n) / sr)
    elif kind == "babble":
        x = sum(synth_speech(n / sr, rng, sr, 0.2) for _ in range(5))
    elif kind == "clicks":
        x = 0.02 * rng.standard_normal(n)
        for _ in range(int(n / sr * 6)):
            p = rng.integers(0, n - 200)
            x[p:p + 120] += rng.standard_normal(120) * np.exp(-np.arange(120) / 20.0) * 8
    else:
        raise ValueError(kind)
    return x / (np.sqrt(np.mean(x ** 2)) + 1e-12)


NOISE_KINDS = ["white", "pink", "brown", "hum", "fan", "babble", "clicks"]


def to_i16(x):
    return np.clip(np.round(x * 32767), -32768, 32767).astype(np.int16)


class Dsp:
    """ctypes wrapper over libauramic_host.so (built by cmake from app/src/main/cpp)."""

    def __init__(self, lib_path, sr=SR):
        self.lib = ctypes.CDLL(lib_path)
        L = self.lib
        L.aura_create.restype = ctypes.c_void_p
        L.aura_create.argtypes = [ctypes.c_int]
        L.aura_destroy.argtypes = [ctypes.c_void_p]
        L.aura_frame_count.restype = ctypes.c_int
        L.aura_frame_count.argtypes = [ctypes.c_size_t]
        L.aura_bands.restype = ctypes.c_int
        L.aura_features_per_frame.restype = ctypes.c_int
        P16 = ctypes.POINTER(ctypes.c_int16)
        PF = ctypes.POINTER(ctypes.c_float)
        L.aura_extract_features.restype = ctypes.c_int
        L.aura_extract_features.argtypes = [ctypes.c_void_p, P16, ctypes.c_size_t, PF, ctypes.c_float]
        L.aura_process.argtypes = [ctypes.c_void_p, P16, ctypes.c_size_t, P16, ctypes.c_float, PF, ctypes.c_float, ctypes.c_int]
        L.aura_band_energies.restype = ctypes.c_int
        L.aura_band_energies.argtypes = [ctypes.c_void_p, P16, ctypes.c_size_t, PF]
        self.h = L.aura_create(sr)
        self.bands = L.aura_bands()
        self.feat = L.aura_features_per_frame()

    def frames(self, n):
        return self.lib.aura_frame_count(n)

    @staticmethod
    def _p16(a):
        return a.ctypes.data_as(ctypes.POINTER(ctypes.c_int16))

    @staticmethod
    def _pf(a):
        return a.ctypes.data_as(ctypes.POINTER(ctypes.c_float))

    def features(self, pcm, strength=0.7):
        f = self.frames(len(pcm))
        out = np.zeros((f, self.feat), dtype=np.float32)
        self.lib.aura_extract_features(self.h, self._p16(pcm), len(pcm), self._pf(out), strength)
        return out

    def process(self, pcm, strength=0.7, mask=None, mask_weight=0.5, normalize=False):
        out = np.zeros_like(pcm)
        mp = None
        if mask is not None:
            mask = np.ascontiguousarray(mask, dtype=np.float32)
            mp = self._pf(mask)
        self.lib.aura_process(self.h, self._p16(pcm), len(pcm), self._p16(out), strength, mp, mask_weight, int(normalize))
        return out

    def band_energies(self, pcm):
        f = self.frames(len(pcm))
        out = np.zeros((f, self.bands), dtype=np.float32)
        self.lib.aura_band_energies(self.h, self._p16(pcm), len(pcm), self._pf(out))
        return out
