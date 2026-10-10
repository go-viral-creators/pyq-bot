"""ctypes driver for the native DeepFilterNet front/back end + polish chain, mirroring the Kotlin
DfnEnhancer (same chunking / alignment) so the host test exercises the exact algorithm the app runs."""
import ctypes
import numpy as np
import onnxruntime as ort

CONV_LA = 2
CHUNK, CTX = 800, 200   # frames (10 ms each); keep in sync with DfnEnhancer.kt
F = ctypes.POINTER(ctypes.c_float)


def _p(a):
    return a.ctypes.data_as(F)


class Native:
    def __init__(self, lib_path):
        L = ctypes.CDLL(lib_path)
        L.dfn_create.restype = ctypes.c_void_p
        L.dfn_destroy.argtypes = [ctypes.c_void_p]
        L.dfn_load.restype = ctypes.c_int
        L.dfn_load.argtypes = [ctypes.c_void_p, F, ctypes.c_size_t]
        L.dfn_fill_features.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, F, F]
        L.dfn_apply.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, F, F, F, ctypes.c_float, ctypes.c_float, ctypes.c_float, ctypes.c_float, ctypes.c_float, ctypes.c_int]
        L.dfn_output.argtypes = [ctypes.c_void_p, F]
        L.polish_run.argtypes = [F, ctypes.c_size_t, ctypes.c_int, ctypes.c_int, ctypes.c_float, ctypes.c_float, F]
        L.polish_lufs.restype = ctypes.c_float
        L.polish_lufs.argtypes = [F, ctypes.c_size_t, ctypes.c_int]
        self.L = L

    def lufs(self, x):
        x = np.ascontiguousarray(x, np.float32)
        return self.L.polish_lufs(_p(x), len(x), 48000)

    def polish(self, x, preset=2, normalize=True, target=-16.0, ceiling=-1.5):
        x = np.ascontiguousarray(x, np.float32).copy()
        rep = np.zeros(4, np.float32)
        self.L.polish_run(_p(x), len(x), preset, int(normalize), target, ceiling, _p(rep))
        return x, rep

    def features(self, x):
        h = self.L.dfn_create()
        t = self.L.dfn_load(h, _p(np.ascontiguousarray(x, np.float32)), len(x))
        erb = np.zeros((t, 32), np.float32); spec = np.zeros((2, t, 96), np.float32)
        # fill in chunks like the app does, then compare
        e = np.zeros(t * 32, np.float32); s = np.zeros(2 * t * 96, np.float32)
        self.L.dfn_fill_features(h, 0, t, _p(e), _p(s))
        self.L.dfn_destroy(h)
        return e.reshape(t, 32), s.reshape(2, t, 96)

    def enhance(self, x, models, atten=100.0, use_df=True, post_beta=0.0):
        enc, erb_dec, df_dec = models
        x = np.ascontiguousarray(x, np.float32)
        h = self.L.dfn_create()
        T = self.L.dfn_load(h, _p(x), len(x))
        for s in range(0, T, CHUNK):
            e = min(T, s + CHUNK)
            fs = max(0, s - CTX); fe = e + CONV_LA
            cnt = fe - fs
            erb = np.zeros(cnt * 32, np.float32); spec = np.zeros(2 * cnt * 96, np.float32)
            self.L.dfn_fill_features(h, fs, cnt, _p(erb), _p(spec))
            e0, e1, e2, e3, emb, c0, lsnr = enc.run(None, {"feat_erb": erb.reshape(1, 1, cnt, 32), "feat_spec": spec.reshape(1, 2, cnt, 96)})
            m = erb_dec.run(None, {"emb": emb, "e3": e3, "e2": e2, "e1": e1, "e0": e0})[0][0, 0]
            coefs = df_dec.run(None, {"emb": emb, "c0": c0})[0][0] if use_df else None
            a = (s + CONV_LA) - fs                       # output index of aligned frame s
            n = e - s
            l = np.ascontiguousarray(lsnr[0, a:a + n, 0], np.float32)
            mm = np.ascontiguousarray(m[a:a + n], np.float32)
            cc = np.ascontiguousarray(coefs[a:a + n].reshape(n, 960), np.float32) if use_df else None
            self.L.dfn_apply(h, s, n, _p(l), _p(mm), _p(cc) if cc is not None else None, atten, -10.0, 30.0, 20.0, post_beta, int(use_df))
        out = np.zeros(len(x), np.float32)
        self.L.dfn_output(h, _p(out))
        self.L.dfn_destroy(h)
        return out
