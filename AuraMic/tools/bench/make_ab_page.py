#!/usr/bin/env python3
"""Builds a single-file blind listening test (works offline in a phone browser) from the benchmark folders.

  python make_ab_page.py BENCH_DIR OUT.html  original=clips  Adobe=adobe_max  AuraMic=v_gtafter  Studio=high_resemble ...

The first 'name=folder' pair that has no clean-reference role is the unprocessed input; all other systems are shuffled per clip and
shown as Option A, B, C ... so the listener does not know which is which. Clean references (synthetic clips only) are offered separately.
All audio is level-matched (active-speech level) and encoded to 64 kbps mono MP3.
"""
import sys, json, base64, random, subprocess, pathlib, tempfile
import numpy as np, soundfile as sf

B = pathlib.Path(sys.argv[1]); OUT = pathlib.Path(sys.argv[2]); pairs = [a.split("=", 1) for a in sys.argv[3:]]
orig_name, orig_dir = pairs[0]; systems = pairs[1:]
import os
idx = json.load(open(B / "index.json")); rng = random.Random(int(os.environ.get("AB_SEED", "2026")))
if os.environ.get("AB_ONLY"): idx = [e for e in idx if e["id"][:3] in os.environ["AB_ONLY"].split(",")]


def active(x, w=960):
    n = len(x) // w; e = np.sqrt((x[: n * w].reshape(n, w) ** 2).mean(1) + 1e-12); return np.sqrt(np.mean(e[e > e.max() * 0.056] ** 2))


def mp3(path):
    x, sr = sf.read(path); x = x if x.ndim == 1 else x[:, 0]; x = np.pad(x, (0, max(0, 576000 - len(x))))[:576000]
    x = (x * 0.063 / (active(x) + 1e-9)).clip(-0.98, 0.98)
    with tempfile.TemporaryDirectory() as t:
        w = pathlib.Path(t) / "a.wav"; m = pathlib.Path(t) / "a.mp3"; sf.write(w, x, sr, subtype="PCM_16")
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(w), "-codec:a", "libmp3lame", "-b:a", "64k", "-ac", "1", str(m)], check=True)
        return base64.b64encode(m.read_bytes()).decode()


clips = []
for e in idx:
    cid = e["id"]; letters = "ABCDEFG"[: len(systems)]; order = list(range(len(systems))); rng.shuffle(order)
    opts = [dict(label=f"Option {letters[i]}", sys=systems[k][0], data=mp3(B / systems[k][1] / f"{cid}.wav")) for i, k in enumerate(order)]
    c = dict(id=cid, cond=e["condition"], orig=mp3(B / orig_dir / f"{cid}.wav"), opts=opts)
    ref = B / "refs" / f"{cid}.wav"
    if ref.exists(): c["ref"] = mp3(ref)
    clips.append(c); print(cid, flush=True)

HTML = r'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>AuraMic blind test __N__</title>
<style>
:root{--bg:#0b0b12;--card:#14141f;--line:#26263a;--t:#ececf8;--m:#9a9ab6;--v:#7c5cff;--c:#22d3ee;--ok:#34d399}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--t);font:16px/1.45 system-ui,Segoe UI,Roboto,Arial,sans-serif;padding:16px 16px 120px;max-width:720px;margin:auto}
h1{font-size:24px;margin:8px 0}p.s{color:var(--m);margin:6px 0 14px}.bar{position:sticky;top:0;background:var(--bg);padding:10px 0;z-index:5;display:flex;gap:10px;align-items:center;border-bottom:1px solid var(--line)}
.bar b{flex:1}button{font:inherit;color:var(--t);background:#20202f;border:1px solid var(--line);border-radius:12px;padding:10px 14px}button.on{background:var(--v);border-color:var(--v)}
button.pri{background:linear-gradient(90deg,var(--v),var(--c));border:0;font-weight:700}
.card{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:14px;margin:14px 0}.card h3{margin:0 0 2px;font-size:17px}.card small{color:var(--m)}
.sw{display:flex;gap:8px;flex-wrap:wrap;margin:10px 0}.sw button{flex:1 1 70px}
input[type=range]{width:100%;accent-color:var(--v)}.row{display:flex;align-items:center;gap:10px;margin:8px 0}.row .n{width:78px;color:var(--m);font-size:14px}
.stars{display:flex;gap:2px}.stars span{font-size:28px;line-height:1;cursor:pointer;color:#3a3a52;user-select:none;padding:2px}.stars span.f{color:#ffc857}
pre{white-space:pre-wrap;background:#0f0f18;border:1px solid var(--line);border-radius:12px;padding:12px;font-size:13px}
.done{color:var(--ok)}
</style></head><body>
<h1>AuraMic blind test - round __N__</h1>
<p class="s">Use headphones. For every clip, play <b>Original</b>, then switch between Options (they keep the same position in time). The options are shuffled and unnamed: rate each 1-5 stars for how <b>clean AND natural</b> the voice sounds (same voice, no robotic/underwater sound, no leftover noise or echo). Your ratings are saved on this device. At the end press <b>Finish &amp; copy results</b> and paste them to Claude.</p>
<div class="bar"><b id="prog">0 / 0 rated</b><button class="pri" id="fin">Finish &amp; copy results</button></div>
<div id="list"></div><pre id="out" hidden></pre>
<script>
const CLIPS=__DATA__;const BUILD='__BUILD__';
const store=(()=>{try{return window.localStorage}catch(e){return null}})();
let rate=JSON.parse((store&&store.getItem('auramic_ab_'+BUILD))||'{}');
const save=()=>{try{store&&store.setItem('auramic_ab_'+BUILD,JSON.stringify(rate))}catch(e){}};
const urls={};function url(b64){if(!urls[b64]){const bin=atob(b64),u=new Uint8Array(bin.length);for(let i=0;i<bin.length;i++)u[i]=bin.charCodeAt(i);urls[b64]=URL.createObjectURL(new Blob([u],{type:'audio/mpeg'}))}return urls[b64]}
const list=document.getElementById('list');let current=null;
CLIPS.forEach((c,ci)=>{
 const card=document.createElement('div');card.className='card';
 card.innerHTML=`<h3>Clip ${ci+1} of ${CLIPS.length}</h3><small>${c.cond}</small><div class="sw"></div><input type="range" min="0" max="1000" value="0"><div class="rt"></div>`;
 const sw=card.querySelector('.sw'),rng=card.querySelector('input'),rt=card.querySelector('.rt');
 const players=[{k:'orig',label:'Original',b:c.orig}].concat(c.opts.map(o=>({k:o.sys,label:o.label,b:o.data})));
 if(c.ref)players.push({k:'ref',label:'Clean ref',b:c.ref});
 let act=null;const audio=new Audio();audio.preload='auto';
 function play(p,btn){const t=audio.currentTime||0;const was=!audio.paused;
  if(current&&current!==audio){current.pause()}current=audio;audio.pause();audio.src=url(p.b);audio.currentTime=t;audio.play().catch(()=>{});
  sw.querySelectorAll('button').forEach(b=>b.classList.remove('on'));btn.classList.add('on');act=p}
 players.forEach(p=>{const b=document.createElement('button');b.textContent=p.label;b.onclick=()=>play(p,b);sw.appendChild(b)});
 audio.ontimeupdate=()=>{if(audio.duration)rng.value=Math.round(audio.currentTime/audio.duration*1000)};
 rng.oninput=()=>{if(audio.duration)audio.currentTime=rng.value/1000*audio.duration};
 audio.onended=()=>{audio.currentTime=0};
 c.opts.forEach((o,oi)=>{const r=document.createElement('div');r.className='row';r.innerHTML=`<span class="n">${o.label}</span><div class="stars"></div>`;
  const st=r.querySelector('.stars');const key=c.id+'|'+oi;
  const paint=()=>{[...st.children].forEach((s,i)=>s.classList.toggle('f',i<(rate[key]||0)))};
  for(let i=1;i<=5;i++){const s=document.createElement('span');s.textContent='★';s.onclick=()=>{rate[key]=(rate[key]===i)?0:i;save();paint();count()};st.appendChild(s)}
  paint();rt.appendChild(r)});
 list.appendChild(card)});
function count(){let n=0;CLIPS.forEach(c=>{if(c.opts.every((o,i)=>rate[c.id+'|'+i]>0))n++});const p=document.getElementById('prog');p.textContent=n+' / '+CLIPS.length+' rated';p.className=n==CLIPS.length?'done':''}
count();
document.getElementById('fin').onclick=()=>{
 const tot={},cnt={},wins={};const lines=[];
 CLIPS.forEach(c=>{const rs=c.opts.map((o,i)=>rate[c.id+'|'+i]||0);if(rs.every(v=>v===0))return;const mx=Math.max(...rs);
  c.opts.forEach((o,i)=>{if(!rs[i])return;tot[o.sys]=(tot[o.sys]||0)+rs[i];cnt[o.sys]=(cnt[o.sys]||0)+1;if(rs[i]===mx)wins[o.sys]=(wins[o.sys]||0)+1/rs.filter(v=>v===mx).length});
  lines.push(c.id+' ['+c.cond+']: '+c.opts.map((o,i)=>o.sys+'='+rs[i]).join(', '))});
 let t='AURAMIC BLIND TEST RESULTS\nsystem: avg stars (clips rated) | wins\n';
 Object.keys(tot).forEach(k=>{t+=k+': '+(tot[k]/cnt[k]).toFixed(2)+' ('+cnt[k]+') | wins '+(wins[k]||0).toFixed(1)+'\n'});
 t+='\nper clip:\n'+lines.join('\n');
 const o=document.getElementById('out');o.hidden=false;o.textContent=t;o.scrollIntoView();
 (navigator.clipboard&&navigator.clipboard.writeText?navigator.clipboard.writeText(t):Promise.reject()).catch(()=>{const r=document.createRange();r.selectNodeContents(o);const s=getSelection();s.removeAllRanges();s.addRange(r)})};
</script></body></html>'''
OUT.write_text(HTML.replace("__N__", os.environ.get("AB_ROUND", "3")).replace("__BUILD__", "r"+os.environ.get("AB_ROUND", "3")+"_s"+os.environ.get("AB_SEED", "2026")).replace("__DATA__", json.dumps(clips, separators=(",", ":"))))
print("wrote", OUT, round(OUT.stat().st_size / 1e6, 1), "MB")
