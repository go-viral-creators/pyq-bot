# Listening-test results and decisions

Blind A/B listening tests (one listener, headphones/phone, 1-5 stars for "clean AND natural"). Pages are built with
`make_ab_page.py` (options shuffled per clip, ratings kept per build in localStorage). Objective scores come from
`score.py`. Rule learned the hard way: **metrics alone are not enough** (GTCRN-after-DFN scored well on DNSMOS/PESQ and
was rated 1-2 stars because it cut everything above 8 kHz), so every change is listened to before it ships.

Product focus: WhatsApp voice notes. Noise removal, loudness and **speed** matter; echo/reverb removal is optional.

## Round 3 - Adobe Enhance Speech vs open-source candidates (12 clips, noise + reverb)

| system | avg stars | wins |
|---|---|---|
| Adobe MAX | 4.75 | 11.5 |
| MossFormer2 (ClearerVoice, Apache-2.0, 55M params) | 3.00 | 0.5 |
| VoiceFixer | 3.00 | 0 |
| AuraMic High (DFN3) | 2.75 | 0 |

Adobe wins by re-synthesising speech (removes reverb/echo, rebuilds highs); suppression-only models cannot. No open model
tested comes close, and the ones that run on a phone (MossFormer2) are not better than DFN3. Adobe-level quality would
need a server-side generative model (e.g. Resemble Enhance, MIT) - **not pursued**: on-device speed is the priority.

## Round 4 - pause-noise expander ("gate") on High, 8 noise clips

| system | avg stars |
|---|---|
| Adobe MAX | 4.63 |
| High + gate (12 dB) | 3.38 |
| High (shipped) | 3.25 |

Gate cut residual noise in pauses -45.9 -> -50.6 dB (Adobe ~ -52) with no change in speech frames and unchanged DNSMOS,
but listeners gained only +0.13 star (better on 3 clips, worse on 1, equal on 4) = noise. **Decision: left OFF**
(`kGateDepthDb = 0` in `polish.cpp`; env `AURA_GATE_DEPTH` / `AURA_GATE_BELOW` only for benchmark experiments).

## Round 5 - Free (Medium, GTCRN) vs Paid (High, DFN3), 7 noise-only clips

| tier | avg stars | wins |
|---|---|---|
| Paid (High) | 4.14 | 6.0 |
| Free (Medium) | 3.29 | 1.0 |

Paid never lost. Largest gaps on non-stationary / babble noise (traffic 4 vs 2, cafe 3 vs 2); stationary noise (washing
machine) and the real recordings are close. One clip (car rumble) was unrated: the noise is below ~150 Hz and inaudible
on phone speakers - do not use sub-150 Hz-only noise in listening tests.

## Parameter checks (host, 27 benchmark clips)

* Attenuation limit 12/18/24/36 dB vs 100 dB: SIG (speech quality) does not improve, BAK (noise) drops on every clip
  (e.g. r04 BAK 4.13 -> 2.64 at 12 dB). **Keep 100 dB.**
* Loudness: inputs span -30..-21 LUFS (sd 1.9); outputs -16.5 LUFS, sd 0.31, no outlier.
* Speed on the 4-core host: High ~11x, Medium ~11.6x realtime (a phone is slower, but the two tiers are equal).

## Known issue (not fixed)

`PipelineHostTest.mediumPipelineHasNoPauseLongerThanThreeHundredMs` fails on some hard clips: Medium leaves pauses of
360 / 410 / 470 ms on traffic-10dB / traffic-3dB / cafe (limit 330 ms); other clips are 160-270 ms. Only affects how
much a pause is shortened, not noise, loudness or speed. Present before the gate work (gate is off).
