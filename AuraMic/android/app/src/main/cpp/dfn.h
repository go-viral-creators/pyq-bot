// DeepFilterNet3 front/back end (everything except the neural network, which runs in ONNX Runtime).
//
// Faithful port of libDF (DFState + the frame logic of tract.rs), re-expressed for whole-clip processing:
//   load()         : STFT (960 / hop 480, Vorbis window), ERB + complex features with running normalisation
//   fillFeatures() : hands a frame range to the network (zero padded past the end)
//   applyFrames()  : per-frame stage logic from the network's lsnr / ERB mask / deep-filter taps, DF, iSTFT
//   output()       : enhanced samples, aligned with the input
// See tools/dfn_reference.py for the numpy reference this is tested against.
#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>

#include "mixed_fft.h"

namespace aura {

constexpr int kDfnSr = 48000;
constexpr int kDfnFft = 960;
constexpr int kDfnHop = 480;
constexpr int kDfnBins = kDfnFft / 2 + 1;  // 481
constexpr int kDfnErb = 32;
constexpr int kDfnDf = 96;
constexpr int kDfnOrder = 5;
constexpr int kDfnLookahead = 2;  // df_lookahead == conv_lookahead

struct DfnParams {
    float attenLimDb = 100.f;     // >= 100: unlimited noise reduction
    float minDb = -10.f;          // lsnr below: frame is noise only -> zero mask
    float maxErbDb = 30.f;        // lsnr above: clean speech -> leave untouched
    float maxDfDb = 20.f;         // lsnr above: skip deep filtering
    float postFilterBeta = 0.f;   // 0 = off
    bool useDf = true;            // false = fast mode (ERB mask only)
};

class DfnEngine {
public:
    DfnEngine();

    // Analyse `n` samples (48 kHz mono, float -1..1). Returns the number of STFT frames T.
    int load(const float *pcm, size_t n);
    int frames() const { return T_; }

    // Features for frames [start, start+count): erb[count*32], spec[2][count][96] (plane major).
    void fillFeatures(int start, int count, float *erb, float *spec) const;

    // Apply network results for aligned frames [start, start+count). Must be called in order.
    // lsnr[count], mask[count*32], coefs[count*96*5*2] (may be null when !useDf).
    void applyFrames(int start, int count, const float *lsnr, const float *mask, const float *coefs,
                     const DfnParams &p);

    // Copies the enhanced signal (n samples, aligned with the input) to `out`.
    void output(float *out) const;

    static int frameCountFor(size_t n) { return static_cast<int>((n + kDfnHop - 1) / kDfnHop) + 3; }

private:
    void analysis(const float *hop, Cpx *spec);
    void synthesis(const Cpx *spec, float *out);

    MixedFft fft_;
    std::vector<float> window_;
    std::vector<int> erbWidths_;
    size_t n_ = 0;
    int T_ = 0;
    std::vector<float> spec_;      // T * 481 * 2 (re,im interleaved)
    std::vector<float> featErb_;   // T * 32
    std::vector<float> featSpec_;  // 2 * T * 96
    std::vector<float> outPcm_;    // T * 480
    std::vector<float> synthMem_;  // 480
    std::vector<float> anaMem_;    // 480
    int nextFrame_ = 0;
};

}  // namespace aura
