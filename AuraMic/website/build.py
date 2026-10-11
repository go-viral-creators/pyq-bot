#!/usr/bin/env python3
"""Static site generator: renders the pages below with a shared header/footer into website/*.html.
Run `npm run build` (pages + Tailwind CSS) after editing. Output is plain static HTML - host anywhere."""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent
SITE = "https://auramic.fireclashpro.com"
EMAIL = "support@fireclashpro.com"
# --- Google Play button -------------------------------------------------------------------------------------------
# While the app is not published yet keep PLAY_LIVE = False (the button shows "Coming soon on Google Play").
# When the Play listing is live: set PLAY_LIVE = True, run `python3 build.py`, upload the pages again.
PLAY_LIVE = False
PLAY_URL = "https://play.google.com/store/apps/details?id=com.kaafisir.auramic"
UPDATED = "October 10, 2026"
NAV = [("Features", "/#features"), ("How it works", "/#how"), ("Screenshots", "/#screens"), ("FAQ", "/#faq"), ("Support", "/#support")]

PLAY_ICON = '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3.6 2.3a1.5 1.5 0 0 0-.6 1.3v16.8c0 .5.2 1 .6 1.3l9.4-9.7zM14.4 13.3l2.9 3L5.1 22.4zM17.3 7.7l-2.9 3L5.1 1.6zM21 10.4l-2.6-1.5-3.2 3.1 3.2 3.1 2.6-1.5c.9-.5.9-2.7 0-3.2z"/></svg>'

def play_button():
    if PLAY_LIVE:
        return f'<a href="{PLAY_URL}" target="_blank" rel="noopener" class="btn-primary">{PLAY_ICON} Get it on Google Play</a>'
    return f'<span class="btn-primary cursor-default">{PLAY_ICON} Coming soon on Google Play</span>'

def rel(path):  # works from any directory / file:// preview as well as at the domain root
    return path.lstrip("/") if path != "/" else "index.html"

def link(p):
    return p.replace("/#", "index.html#") if p.startswith("/#") else rel(p)

def page(name, title, desc, body, *, jsonld="", noindex=False):
    nav = "".join(f'<a class="text-sm font-medium text-slate-300 hover:text-white" href="{link(h)}">{t}</a>' for t, h in NAV)
    mnav = "".join(f'<a class="block rounded-xl px-3 py-3 text-slate-200 hover:bg-white/5" href="{link(h)}">{t}</a>' for t, h in NAV)
    html = f"""<!doctype html>
<html lang="en" class="scroll-pt-24">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="{desc}">
<meta name="theme-color" content="#0A0A12">
{'<meta name="robots" content="noindex">' if noindex else ''}
<link rel="canonical" href="{SITE}/{'' if name == 'index' else name + '.html'}">
<link rel="icon" type="image/png" href="assets/img/favicon-64.png">
<link rel="apple-touch-icon" href="assets/img/icon-512.png">
<meta property="og:type" content="website"><meta property="og:title" content="{title}"><meta property="og:description" content="{desc}">
<meta property="og:image" content="{SITE}/assets/img/og-image.png"><meta name="twitter:card" content="summary_large_image">
<link rel="stylesheet" href="assets/styles.css">
{jsonld}
</head>
<body>
<a href="#main" class="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-black">Skip to content</a>
<header class="sticky top-0 z-40 border-b border-white/5 bg-aura-ink/80 backdrop-blur-xl">
  <div class="mx-auto flex max-w-6xl items-center justify-between px-5 py-3.5">
    <a href="index.html" class="flex items-center gap-3 font-bold text-white"><img src="assets/img/icon-512.png" alt="" width="36" height="36" class="h-9 w-9 rounded-xl"> AuraMic AI</a>
    <nav class="hidden items-center gap-7 md:flex" aria-label="Main">{nav}</nav>
    <div class="flex items-center gap-3">
      <a href="index.html#get" class="btn-primary hidden !py-2.5 text-sm sm:inline-flex">Get the app</a>
      <button id="menu-btn" class="rounded-xl border border-white/15 p-2.5 md:hidden" aria-label="Menu" aria-expanded="false" aria-controls="mobile-menu">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg></button>
    </div>
  </div>
  <div id="mobile-menu" class="hidden border-t border-white/5 px-5 pb-4 md:hidden">{mnav}</div>
</header>
<main id="main">
{body}
</main>
<footer class="mt-24 border-t border-white/5 bg-black/20">
  <div class="mx-auto grid max-w-6xl gap-10 px-5 py-14 sm:grid-cols-2 lg:grid-cols-4">
    <div>
      <a href="index.html" class="flex items-center gap-3 font-bold text-white"><img src="assets/img/icon-512.png" alt="" width="36" height="36" class="h-9 w-9 rounded-xl"> AuraMic AI</a>
      <p class="mt-4 max-w-xs text-sm text-slate-400">Clean, studio-style voice notes for WhatsApp, processed on your phone. A FireClash Pro app.</p>
    </div>
    <div><h3 class="text-sm font-semibold text-white">Product</h3><ul class="mt-4 space-y-2.5 text-sm text-slate-400">
      <li><a class="hover:text-white" href="index.html#features">Features</a></li><li><a class="hover:text-white" href="index.html#how">How it works</a></li>
      <li><a class="hover:text-white" href="index.html#screens">Screenshots</a></li><li><a class="hover:text-white" href="index.html#faq">FAQ</a></li></ul></div>
    <div><h3 class="text-sm font-semibold text-white">Support</h3><ul class="mt-4 space-y-2.5 text-sm text-slate-400">
      <li><a class="font-medium text-aura-cyan hover:underline" href="mailto:{EMAIL}">{EMAIL}</a></li>
      <li><a class="hover:text-white" href="mailto:{EMAIL}?subject=AuraMic%20bug%20report">Report a bug</a></li></ul></div>
    <div><h3 class="text-sm font-semibold text-white">Legal</h3><ul class="mt-4 space-y-2.5 text-sm text-slate-400">
      <li><a class="hover:text-white" href="privacy.html">Privacy Policy</a></li><li><a class="hover:text-white" href="terms.html">Terms of Service</a></li>
      <li><a class="hover:text-white" href="accessibility.html">Accessibility Disclaimer</a></li></ul></div>
  </div>
  <div class="border-t border-white/5 px-5 py-6 text-center text-xs leading-relaxed text-slate-500">
    © 2026 FireClash Pro. All rights reserved. AuraMic AI is an independent app and is not affiliated with, endorsed by or sponsored by WhatsApp LLC or Meta Platforms, Inc.
    WhatsApp is a trademark of its owner. Google Play and the Google Play logo are trademarks of Google LLC.
  </div>
</footer>
<script src="assets/site.js" defer></script>
</body></html>"""
    (ROOT / f"{name}.html").write_text(html, encoding="utf-8")
    print("wrote", f"{name}.html")

def check():
    return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#34D399" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>'

def feature(icon, title, text, color):
    return f"""<article class="card transition hover:-translate-y-1 hover:border-aura-violet/50">
      <div class="mb-4 grid h-12 w-12 place-items-center rounded-2xl" style="background:{color}22;color:{color}">{icon}</div>
      <h3 class="text-lg font-semibold text-white">{title}</h3><p class="mt-2 text-sm leading-relaxed text-slate-400">{text}</p></article>"""

I = lambda p: f'<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{p}</svg>'
FEATURES = [
    feature(I('<path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z"/>'), "Two quality levels", "Medium Quality (free) is fast and light; High Quality (AuraMic Pro, with 10 free tries) is studio-clean. Traffic, fans, crowds, keyboards and hum disappear while your voice stays natural. Everything runs on your phone.", "#7C5CFF"),
    feature(I('<rect x="7" y="2" width="10" height="20" rx="2"/><path d="M11 18h2"/>'), "100% on-device", "Everything runs on your phone — no cloud, no account, no upload. Works in airplane mode.", "#22D3EE"),
    feature(I('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'), "Hold, speak, release", "A floating mic on top of WhatsApp. One gesture records, cleans and sends. Slide left to cancel.", "#FF5C93"),
    feature(I('<path d="M22 2L11 13"/><path d="M22 2l-7 20-4-9-9-4z"/>'), "Auto-send", "AuraMic opens the share sheet, picks the same chat and taps Send for you. Prefer manual? Turn it off.", "#34D399"),
    feature(I('<path d="M3 12h3l3-8 4 16 3-8h5"/>'), "Podcast polish", "EQ, de-esser and gentle compression, then loudness mastering to −16 LUFS (BS.1770) with a look-ahead limiter. Presets: Off, Natural, Podcast.", "#FFB84D"),
    feature(I('<path d="M4 6h16M4 12h6M14 12h6M4 18h16"/>'), "Auto-cut silence", "Shorten long pauses and trim the start and end. Gentle or Tight, applied to the cleaned signal so cuts are clean.", "#F97316"),
    feature(I('<path d="M2 12h4l2-6 4 12 3-9 2 3h5"/>'), "Separate the background", "Hear the voice and the removed noise as separate takes, or mix a little ambience back for a natural feel.", "#22D3EE"),
    feature(I('<rect x="3" y="4" width="18" height="14" rx="3"/><path d="M8 21h8M12 18v3"/>'), "WhatsApp and WhatsApp Business", "The mic sits exactly on WhatsApp's own voice button inside a chat, works like it (hold to record, slide left to cancel) and can send the note for you.", "#34D399"),
    feature(I('<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>'), "7 languages", "English, हिन्दी, Hinglish, Español, Português, العربية and Bahasa Indonesia — switch any time, instantly.", "#9B83FF"),
]
STEPS = [
    ("1", "Open a chat", "Open any conversation in WhatsApp or WhatsApp Business. The AuraMic mic appears on the voice button; it hides when you leave the chat."),
    ("2", "Hold &amp; speak", "Press and hold the mic. Watch the live waveform and timer. Slide left to cancel."),
    ("3", "Release", "AuraMic cleans the audio on-device, picks the same chat, and taps Send. Done in seconds."),
]
SHOTS = [("phone_01_hold_to_record", "Hold. Speak. Release."), ("phone_02_noise_removal", "Studio Lab: before / after"), ("phone_03_auto_send", "Sent automatically"),
         ("phone_04_privacy", "Private by design"), ("phone_05_setup", "Guided setup"), ("phone_06_languages", "Seven languages"),
         ("phone_07_quality_levels", "Two quality levels"), ("phone_08_whatsapp", "Built for WhatsApp")]
FAQ = [
    ("Does AuraMic upload my voice?", "No. Recording, noise removal and encoding all happen on your phone. We have no servers that receive your audio, messages or contacts. See the <a class='text-aura-cyan underline' href='privacy.html'>Privacy Policy</a>."),
    ("Which chat apps work?", "WhatsApp and WhatsApp Business."),
    ("Is AuraMic free?", "Yes: Medium Quality is free. High Quality is part of AuraMic Pro (monthly or yearly subscription, cancel any time in Google Play) and you get 10 free High Quality voice notes to try it. The app works offline; the internet is only used by Google Play for the subscription and for updates."),
    ("How does it compare to cloud voice enhancers?", "AuraMic uses on-device neural noise removal that keeps your actual voice and removes everything else, then levels the volume and masters the loudness. It is not affiliated with or a copy of any other product; generative tools that re-synthesise speech need the cloud, AuraMic stays on your phone."),
    ("Why does it need the Accessibility service?", "To select the chat you were in and tap the chat app's own Send button after your voice note is ready — nothing else. It only works inside WhatsApp and WhatsApp Business, also finds where WhatsApp's voice button is so the mic can sit on it, and never reads or stores your messages. Read the <a class='text-aura-cyan underline' href='accessibility.html'>Accessibility Disclaimer</a>."),
    ("Why “Usage access”?", "Android's Usage access lets AuraMic know which app is in front, so the floating mic shows only in WhatsApp and disappears everywhere else."),
    ("Can Android block the Accessibility switch?", "If you installed the APK outside the Play Store, Android 13+ may grey it out. Open Settings → Apps → AuraMic → ⋮ → “Allow restricted settings”, then enable the service."),
    ("Which noise does it remove best?", "Steady noise: traffic, fans, AC, hum, wind, engines. Very loud or voice-like noise (a packed crowd) is reduced but not eliminated. Try the in-app Studio Lab to hear it with your own recording."),
    ("It stopped appearing after a while. Why?", "Some phones (Xiaomi, Oppo, Vivo, Samsung…) put background apps to sleep. Set AuraMic's battery usage to Unrestricted — the app's setup screen has a shortcut."),
    ("Is AuraMic affiliated with WhatsApp?", "No. AuraMic AI is an independent app by FireClash Pro. WhatsApp is a trademark of WhatsApp LLC."),
    ("What does it cost?", "See the Google Play listing for current pricing. No account is ever required."),
]

def index():
    feats = "\n".join(FEATURES)
    steps = "".join(f"""<li class="card relative"><span class="mb-4 grid h-10 w-10 place-items-center rounded-full bg-gradient-to-br from-aura-violet to-aura-cyan font-bold text-white">{n}</span>
      <h3 class="text-lg font-semibold text-white">{t}</h3><p class="mt-2 text-sm leading-relaxed text-slate-400">{d}</p></li>""" for n, t, d in STEPS)
    shots = "".join(f"""<figure class="w-56 shrink-0 snap-center sm:w-60"><img loading="lazy" width="540" height="960" src="assets/img/{s}.webp" alt="{c}" class="rounded-3xl border border-aura-line shadow-2xl shadow-black/50"><figcaption class="mt-3 text-center text-sm text-slate-400">{c}</figcaption></figure>""" for s, c in SHOTS)
    faq = "".join(f"""<details class="group card !p-0"><summary class="flex cursor-pointer list-none items-center justify-between gap-4 px-6 py-5 font-semibold text-white">{q}<span class="text-aura-cyan transition group-open:rotate-45 text-2xl leading-none" aria-hidden="true">+</span></summary><p class="px-6 pb-6 text-sm leading-relaxed text-slate-400">{a}</p></details>""" for q, a in FAQ)
    jsonld = f"""<script type="application/ld+json">{{"@context":"https://schema.org","@type":"SoftwareApplication","name":"AuraMic AI","operatingSystem":"Android 9+","applicationCategory":"UtilitiesApplication","description":"On-device AI noise removal for voice notes with a floating mic.","publisher":{{"@type":"Organization","name":"FireClash Pro","email":"{EMAIL}"}},"url":"{SITE}/"}}</script>
<script type="application/ld+json">{{"@context":"https://schema.org","@type":"FAQPage","mainEntity":[{",".join('{"@type":"Question","name":"'+q.replace('"','')+'","acceptedAnswer":{"@type":"Answer","text":"'+__import__('re').sub('<[^>]+>','',a).replace('"','')+'"}}' for q,a in FAQ)}]}}</script>"""
    body = f"""
<section class="relative overflow-hidden">
  <div class="pointer-events-none absolute -left-40 -top-40 h-[34rem] w-[34rem] rounded-full bg-aura-violet/30 blur-[120px]"></div>
  <div class="pointer-events-none absolute -right-40 top-40 h-[30rem] w-[30rem] rounded-full bg-aura-cyan/20 blur-[120px]"></div>
  <div class="relative mx-auto grid max-w-6xl items-center gap-12 px-5 pb-20 pt-16 lg:grid-cols-2 lg:pt-24">
    <div>
      <p class="pill"><span class="h-2 w-2 rounded-full bg-aura-mint shadow-[0_0_10px_#34D399]"></span> 100% on-device AI</p>
      <h1 class="mt-6 text-5xl font-extrabold leading-[1.02] tracking-tight text-white sm:text-6xl">Studio-quality<br><span class="grad-text">voice notes.</span></h1>
      <p class="mt-6 max-w-xl text-lg leading-relaxed text-slate-300">A floating mic on top of your chat apps. Hold it, speak, let go — the background noise is removed on your phone and your voice note is sent for you, with even volume.</p>
      <div id="get" class="mt-8 flex flex-wrap items-center gap-4">
        {play_button()}
        <a href="#how" class="btn-ghost">See how it works</a>
      </div>
      <ul class="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-400">
        <li class="flex items-center gap-2">{check()} No account</li><li class="flex items-center gap-2">{check()} Works offline</li><li class="flex items-center gap-2">{check()} Nothing uploaded</li><li class="flex items-center gap-2">{check()} Android 9+</li></ul>
    </div>
    <!-- live demo phone -->
    <div class="relative mx-auto w-full max-w-[330px]" aria-hidden="true">
      <div class="floaty relative rounded-[3rem] border-[6px] border-[#2b2b44] bg-[#0E0E18] p-3 shadow-2xl shadow-aura-violet/20">
        <div id="demo" class="relative h-[600px] overflow-hidden rounded-[2.3rem] bg-aura-ink">
          <div class="flex items-center gap-3 bg-aura-s3 px-4 py-3.5"><div class="h-9 w-9 rounded-full bg-gradient-to-br from-aura-violet to-aura-cyan"></div><div><p class="text-sm font-bold text-white">Aarav</p><p class="text-[11px] text-aura-mint">online</p></div></div>
          <div class="space-y-3 p-4 text-[13px]">
            <p class="w-44 rounded-2xl rounded-bl-md bg-aura-s3 px-4 py-2.5 text-slate-200">Hey! Can you send me the details?</p>
            <p class="ml-auto w-48 rounded-2xl rounded-br-md bg-[#2A1F6B] px-4 py-2.5 text-slate-100">Sure — sending a voice note 🎙️</p>
            <div id="demo-vn" class="ml-auto flex w-52 items-center gap-2 rounded-2xl rounded-br-md bg-[#2A1F6B] px-4 py-3 transition duration-500" style="opacity:0">
              <span class="border-y-[6px] border-l-[10px] border-y-transparent border-l-white"></span>
              <span class="flex h-7 flex-1 items-center gap-[2px]">{''.join(f'<i class="block w-[3px] rounded-full bg-aura-violet2" style="height:{h}px"></i>' for h in [8,14,22,12,26,18,10,24,16,9,20,13])}</span>
              <span class="text-[11px] text-violet-200">0:07</span></div>
          </div>
          <div class="absolute inset-x-4 bottom-4 h-11 rounded-full bg-aura-s3"></div>
          <div id="demo-pill" class="absolute left-1/2 top-[330px] -translate-x-1/2 whitespace-nowrap rounded-full bg-black/80 px-4 py-2 text-xs font-semibold text-white shadow-xl"></div>
          <div class="absolute right-6 top-[390px] h-[72px] w-[72px]">
            <div id="demo-rings" class="transition-opacity duration-300"><span class="ring absolute inset-0 rounded-full border-2 border-aura-pink/60"></span><span class="ring d2 absolute inset-0 rounded-full border-2 border-aura-pink/40"></span><span class="ring d3 absolute inset-0 rounded-full border-2 border-aura-pink/20"></span></div>
            <div id="demo-disc" class="absolute inset-0 grid place-items-center rounded-full shadow-xl shadow-aura-violet/50 transition-all duration-500" style="background:linear-gradient(135deg,#7C5CFF,#22D3EE)">
              <svg id="demo-icon" width="34" height="34" viewBox="0 0 24 24"></svg></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<section id="features" class="mx-auto max-w-6xl px-5 pt-10">
  <div class="mx-auto max-w-2xl text-center"><p class="text-sm font-semibold uppercase tracking-widest text-aura-violet2">Why AuraMic AI</p>
    <h2 class="mt-3 text-3xl font-extrabold text-white sm:text-4xl">Sound like you're in a studio — wherever you are</h2></div>
  <div class="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{feats}</div>
</section>

<section id="how" class="mx-auto mt-28 max-w-6xl px-5">
  <div class="mx-auto max-w-2xl text-center"><p class="text-sm font-semibold uppercase tracking-widest text-aura-violet2">How it works</p>
    <h2 class="mt-3 text-3xl font-extrabold text-white sm:text-4xl">Three steps. Zero extra taps.</h2></div>
  <ol class="mt-12 grid gap-5 md:grid-cols-3">{steps}</ol>

  <div class="card mt-10 grid items-center gap-8 md:grid-cols-2">
    <div class="min-w-0"><h3 class="text-2xl font-bold text-white">Hear (and see) the difference</h3>
      <p class="mt-3 text-slate-400">Illustration of the effect: steady background noise is pulled down while speech peaks stay. Open <strong class="text-white">Studio Lab</strong> in the app to try it with your own voice.</p>
      <ul class="mt-5 space-y-2 text-sm text-slate-300"><li class="flex gap-2">{check()} Neural noise removal on the full 48 kHz band (High) or a light, fast model (Medium)</li><li class="flex gap-2">{check()} Auto-cut of long pauses, even-volume voice chain</li><li class="flex gap-2">{check()} Many times faster than real time; fully offline</li></ul></div>
    <div id="ba" class="min-w-0 overflow-hidden rounded-2xl border border-aura-line bg-aura-ink p-5">
      <div class="flex items-center justify-between text-sm"><span id="ba-label" class="font-semibold text-white" aria-live="polite">Original</span><span class="text-slate-500">illustration</span></div>
      <div id="ba-bars" class="my-6 flex h-[70px] items-center justify-center gap-[2px] overflow-hidden" aria-hidden="true"></div>
      <button id="ba-btn" class="btn-primary w-full" aria-pressed="false">Remove the noise</button></div>
  </div>
</section>

<section class="mx-auto mt-28 max-w-6xl px-5">
  <div class="grid gap-5 rounded-3xl border border-aura-line bg-gradient-to-br from-aura-s2 to-aura-s1 p-8 sm:grid-cols-2 lg:grid-cols-4">
    <div><h3 class="font-bold text-white">Private by design</h3><p class="mt-2 text-sm text-slate-400">AI runs on your phone. No audio ever reaches a server.</p></div>
    <div><h3 class="font-bold text-white">Narrow permissions</h3><p class="mt-2 text-sm text-slate-400">Accessibility is limited to WhatsApp and used only to tap Send.</p></div>
    <div><h3 class="font-bold text-white">No ads, no account</h3><p class="mt-2 text-sm text-slate-400">Open the app, finish the guided setup, and go.</p></div>
    <div><h3 class="font-bold text-white">Light on battery</h3><p class="mt-2 text-sm text-slate-400">Foreground checks pause when the screen is off.</p></div>
  </div>
</section>

<section id="screens" class="mt-28">
  <div class="mx-auto max-w-2xl px-5 text-center"><p class="text-sm font-semibold uppercase tracking-widest text-aura-violet2">Screenshots</p>
    <h2 class="mt-3 text-3xl font-extrabold text-white sm:text-4xl">A look inside</h2></div>
  <div class="mx-auto mt-12 flex max-w-6xl snap-x gap-6 overflow-x-auto px-5 pb-6">{shots}</div>
</section>

<section id="faq" class="mx-auto mt-28 max-w-3xl px-5">
  <div class="text-center"><p class="text-sm font-semibold uppercase tracking-widest text-aura-violet2">FAQ</p><h2 class="mt-3 text-3xl font-extrabold text-white sm:text-4xl">Good questions</h2></div>
  <div class="mt-10 space-y-3">{faq}</div>
</section>

<section id="support" class="mx-auto mt-28 max-w-4xl px-5">
  <div class="relative overflow-hidden rounded-[2rem] border border-aura-line bg-gradient-to-br from-[#2A1F6B] via-aura-s2 to-[#0E3A44] p-10 text-center">
    <h2 class="text-3xl font-extrabold text-white sm:text-4xl">Questions? We read every email.</h2>
    <p class="mx-auto mt-4 max-w-xl text-slate-300">Bug, idea, or something not working on your phone? Tell us the phone model, Android version and what happened.</p>
    <a class="btn-primary mt-8" href="mailto:{EMAIL}">{EMAIL}</a>
  </div>
</section>"""
    page("index", "AuraMic AI — Studio-quality voice notes with on-device AI", "AuraMic AI puts a floating mic on top of WhatsApp. Hold, speak, release: on-device AI removes background noise and your voice note is sent automatically.", body, jsonld=jsonld)

LEGAL_HEAD = lambda t, sub: f'<section class="mx-auto max-w-3xl px-5 pt-16"><p class="text-sm font-semibold uppercase tracking-widest text-aura-violet2">Legal</p><h1 class="mt-3 text-4xl font-extrabold text-white">{t}</h1><p class="mt-3 text-slate-400">{sub}</p><p class="mt-1 text-sm text-slate-500">Last updated: {UPDATED}</p></section>'

def privacy():
    body = LEGAL_HEAD("Privacy Policy", "AuraMic AI is built so that your voice stays on your phone.") + f"""
<article class="prose-legal mx-auto max-w-3xl px-5 pb-8">
<h2>1. Who we are</h2><p>AuraMic AI (“the app”) is published by FireClash Pro (“we”, “us”). Contact: <a href="mailto:{EMAIL}">{EMAIL}</a>.</p>
<h2>2. The short version</h2>
<ul><li>All recording, noise removal and encoding happen <strong>on your device</strong>.</li><li>We do <strong>not</strong> collect, upload, sell or share your audio, messages, contacts or other personal data.</li>
<li>The app has no account system, no advertising SDKs and no analytics SDKs.</li><li>The app needs no internet connection to work.</li></ul>
<h2>3. Data the app handles</h2>
<table><thead><tr><th>Data</th><th>Why</th><th>Where it goes</th></tr></thead><tbody>
<tr><td>Microphone audio you record with the floating mic or in Studio Lab</td><td>To remove noise and create the voice note</td><td>Processed in memory on your device. The cleaned file is written to the app's private cache, shared with WhatsApp only when you send, and deleted automatically (about 10 minutes after sharing; leftovers are purged on next start). Studio Lab audio is never saved to disk.</td></tr>
<tr><td>Name of the open chat (read through the Accessibility service)</td><td>To select the same chat in WhatsApp's share screen</td><td>Held in memory for the few seconds the hand-off takes; never stored or transmitted.</td></tr>
<tr><td>Foreground app name (Usage access)</td><td>To show the AuraMic mic only while WhatsApp is open</td><td>Evaluated on the device; never stored or transmitted.</td></tr>
<tr><td>App settings (language, noise strength, bubble position, counters of cleaned notes)</td><td>To remember your preferences and show your stats</td><td>Stored locally on your device only. Excluded from cloud backup.</td></tr></tbody></table>
<h2>4. Permissions</h2>
<ul><li><strong>Display over other apps</strong> — draws the floating mic.</li><li><strong>Microphone</strong> — records while you hold the button.</li>
<li><strong>Usage access</strong> — detects whether WhatsApp is in the foreground.</li><li><strong>Accessibility service</strong> — selects the chat, taps Send, and finds where WhatsApp's voice button is so the mic can sit on it (WhatsApp and WhatsApp Business only); see the <a href="accessibility.html">Accessibility Disclaimer</a>.</li>
<li><strong>Notifications / foreground service</strong> — shows the status notification that Android requires while the microphone service runs.</li><li><strong>Vibration</strong> — optional haptic feedback.</li></ul>
<h2>5. Third parties</h2><p>We do not share data with third parties. Files you choose to send are handed to WhatsApp, which processes them under its own terms and privacy policy. AuraMic AI is not affiliated with WhatsApp LLC or Meta Platforms, Inc.</p>
<h2>6. Children</h2><p>The app is not directed to children under 13 and we do not knowingly collect their data (we collect no personal data at all).</p>
<h2>7. Security</h2><p>Because audio never leaves your device, there is no server-side copy to protect. Temporary files live in the app's private storage and are shared through a time-limited content URI granted only to WhatsApp.</p>
<h2>8. Your rights</h2><p>Since we hold no personal data about you, there is nothing for us to access, correct or delete. Uninstalling the app removes all local data. If you believe we hold data about you, email <a href="mailto:{EMAIL}">{EMAIL}</a>.</p>
<h2>9. Changes</h2><p>If this policy changes we will update the date above and, for material changes, tell you in the app or on the Google Play listing.</p>
<h2>10. Contact</h2><p><a href="mailto:{EMAIL}">{EMAIL}</a></p>
</article>"""
    page("privacy", "Privacy Policy — AuraMic AI", "How AuraMic AI handles your data: everything stays on your device.", body)

def terms():
    body = LEGAL_HEAD("Terms of Service", "Please read these terms before using AuraMic AI.") + f"""
<article class="prose-legal mx-auto max-w-3xl px-5 pb-8">
<h2>1. Agreement</h2><p>By installing or using AuraMic AI (“the app”) you agree to these Terms. If you do not agree, do not use the app. The app is provided by FireClash Pro (“we”, “us”).</p>
<h2>2. What the app does</h2><p>AuraMic AI records audio when you hold its floating button, reduces background noise on your device, and hands the resulting file to WhatsApp / WhatsApp Business, optionally tapping WhatsApp's Send button for you through an Android Accessibility service.</p>
<h2>3. Licence</h2><p>We grant you a personal, non-exclusive, non-transferable, revocable licence to use the app on devices you own or control. You may not copy, modify, reverse engineer (except where law allows), sell, sublicense or redistribute the app.</p>
<h2>4. Your responsibilities</h2>
<ul><li>Only record people where you have the right to do so; obey local recording and privacy laws.</li><li>Do not use the app to harass, deceive or send unlawful content.</li>
<li>You are responsible for what you send. Auto-send sends immediately after you release the button; use the cancel gesture (slide left) or turn auto-send off if you want to review first.</li>
<li>Do not use the app to circumvent security measures of any service.</li></ul>
<h2>5. Third-party services</h2><p>AuraMic AI is an independent product and is not affiliated with, endorsed by or sponsored by WhatsApp LLC or Meta Platforms, Inc. WhatsApp is a trademark of WhatsApp LLC. Your use of WhatsApp is governed by WhatsApp's own terms. WhatsApp may change its app at any time, which can temporarily break automation features; we will work to restore them but cannot guarantee compatibility with every WhatsApp version.</p>
<h2>6. Noise reduction is best-effort</h2><p>Audio enhancement results depend on the recording environment and device. We do not guarantee that all noise will be removed or that speech will be unaffected.</p>
<h2>7. Privacy</h2><p>See our <a href="privacy.html">Privacy Policy</a>. In short: audio is processed on your device and not collected by us.</p>
<h2>8. Disclaimer of warranties</h2><p>The app is provided “as is” and “as available”, without warranties of any kind, express or implied, including merchantability, fitness for a particular purpose and non-infringement, to the fullest extent permitted by law.</p>
<h2>9. Limitation of liability</h2><p>To the fullest extent permitted by law, FireClash Pro will not be liable for indirect, incidental, special, consequential or punitive damages, or for loss of data, profits or goodwill, arising from your use of the app, including messages sent to an unintended chat. Nothing in these Terms limits liability that cannot be limited by law.</p>
<h2>10. Termination</h2><p>You may stop using the app at any time by uninstalling it. We may suspend or end access if you breach these Terms.</p>
<h2>11. Changes</h2><p>We may update these Terms; the date above shows the latest version. Continued use after an update means you accept it.</p>
<h2>12. Contact</h2><p>Questions: <a href="mailto:{EMAIL}">{EMAIL}</a></p>
</article>"""
    page("terms", "Terms of Service — AuraMic AI", "The terms that apply to using AuraMic AI.", body)

def a11y():
    body = LEGAL_HEAD("Accessibility Service Disclaimer", "Required disclosure for Google Play's AccessibilityService API policy.") + f"""
<article class="prose-legal mx-auto max-w-3xl px-5 pb-8">
<div class="card !border-aura-mint/40"><p class="!mt-0 font-semibold text-white">In plain words</p>
<p>AuraMic AI uses Android's Accessibility Service for <strong>one purpose only</strong>: after you record a voice note with the floating mic, it chooses the chat you were in and taps the <strong>Send</strong> button inside WhatsApp or WhatsApp Business so the note is delivered without extra taps. It also looks up where WhatsApp's own voice button is on the screen, so the AuraMic microphone can sit exactly on top of it. It does not read, collect, store or share your messages, contacts or any personal data.</p></div>
<h2>1. Why the Accessibility API is needed</h2><p>Chat apps offer no public API for sending a file into the conversation you have open. Android's share sheet opens WhatsApp's chat picker and then requires you to choose a chat and tap Send. AuraMic automates exactly those two on-screen taps. There is no other way to deliver this core feature, and the feature is optional: turn off <em>Auto-send</em> in Settings and AuraMic simply opens the share screen for you to finish manually.</p>
<h2>2. What the service can see and do</h2>
<ul><li><strong>Scope:</strong> limited by its configuration to <code>com.whatsapp</code> and <code>com.whatsapp.w4b</code>. Windows of other apps are never inspected.</li>
<li><strong>When:</strong> while WhatsApp is open it checks whether a chat is open and where the voice button is (position only), and after you release the mic button it acts for a few seconds (at most 25 seconds).</li>
<li><strong>What it reads:</strong> the title of the open chat, and the labels of buttons / chat names on the share screen, solely to find the right row and the Send button. Message text is not read. If auto-send fails, a shortened structure dump (names cut to a few characters) is kept in memory and copied only when you tap “Copy diagnostics”.</li>
<li><strong>What it does:</strong> scrolls the picker list, types the chat name into WhatsApp's own search box when needed, and clicks the matching chat and the Send button. It never taps Send unless it has itself selected the chat, so a half-typed message in another conversation is never sent by accident.</li>
<li><strong>What it never does:</strong> it does not log keystrokes, capture screen content, monitor passwords or payments, change settings, install software, or send any information off your device.</li></ul>
<h2>3. Consent and control</h2><p>Before the system settings screen is opened, the app shows an in-app disclosure that you must accept. The service is off until you enable it in <em>Settings → Accessibility</em>, and you can disable it there at any time. Disabling it only disables auto-send.</p>
<h2>4. Not an accessibility tool</h2><p>AuraMic AI is not designed to help people with disabilities and does not declare itself an accessibility tool. We use the API solely for the automation described above.</p>
<h2>5. Restricted settings (sideloaded installs)</h2><p>On Android 13 and later, apps installed outside the Play Store may find the switch greyed out. Open <em>Settings → Apps → AuraMic → ⋮ → Allow restricted settings</em>, then enable the service.</p>
<h2>6. Contact</h2><p>Questions about this disclosure: <a href="mailto:{EMAIL}">{EMAIL}</a>. See also the <a href="privacy.html">Privacy Policy</a>.</p>
</article>"""
    page("accessibility", "Accessibility Service Disclaimer — AuraMic AI", "Exactly how and why AuraMic AI uses Android's Accessibility Service.", body)

def notfound():
    body = '<section class="mx-auto max-w-xl px-5 py-32 text-center"><h1 class="text-5xl font-extrabold text-white">404</h1><p class="mt-4 text-slate-400">That page took a quiet break.</p><a class="btn-primary mt-8" href="index.html">Back home</a></section>'
    page("404", "Page not found — AuraMic AI", "Page not found", body, noindex=True)

if __name__ == "__main__":
    index(); privacy(); terms(); a11y(); notfound()
    (ROOT / "robots.txt").write_text(f"User-agent: *\nAllow: /\nSitemap: {SITE}/sitemap.xml\n")
    urls = "".join(f"<url><loc>{SITE}/{p}</loc></url>" for p in ["", "privacy.html", "terms.html", "accessibility.html"])
    (ROOT / "sitemap.xml").write_text(f'<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">{urls}</urlset>')
