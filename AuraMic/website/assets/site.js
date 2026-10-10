// Tiny progressive-enhancement script: mobile menu, hero phone demo loop, before/after toggle.
(function () {
  const $ = (s, r = document) => r.querySelector(s);
  const menuBtn = $('#menu-btn'), menu = $('#mobile-menu');
  if (menuBtn && menu) menuBtn.addEventListener('click', () => {
    const open = menu.classList.toggle('hidden') === false;
    menuBtn.setAttribute('aria-expanded', String(open));
  });

  // ---- hero demo: record -> clean -> sent, on a loop ------------------------------------------
  const demo = $('#demo');
  if (demo) {
    const pill = $('#demo-pill'), disc = $('#demo-disc'), rings = $('#demo-rings'), vn = $('#demo-vn'), icon = $('#demo-icon');
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const MIC = '<path d="M12 14a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3zm5.3-3a5.3 5.3 0 0 1-10.6 0H5a7 7 0 0 0 6 6.72V21h2v-3.28A7 7 0 0 0 19 11z" fill="#fff"/>';
    const SPARK = '<path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z" fill="#fff"/>';
    const CHECK = '<path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>';
    const set = (txt, grad, svg, ringsOn, vnOn) => {
      pill.innerHTML = txt; disc.style.background = grad; icon.innerHTML = svg;
      rings.style.opacity = ringsOn ? 1 : 0; vn.style.opacity = vnOn ? 1 : 0; vn.style.transform = vnOn ? 'none' : 'translateY(12px)';
    };
    const V = 'linear-gradient(135deg,#7C5CFF,#22D3EE)', R = 'linear-gradient(135deg,#7C5CFF,#FF5C93)', G = 'linear-gradient(135deg,#34D399,#10B981)';
    const steps = [
      () => set('Hold to record', V, MIC, false, false), 1400,
      ...[1, 2, 3, 4, 5, 6, 7].flatMap(s => [() => set('<span class="inline-block h-2 w-2 rounded-full bg-aura-pink"></span> 0:0' + s + ' <span class="text-slate-400">◀ Slide to cancel</span>', R, MIC, true, false), 700]),
      () => set('Removing noise…', 'linear-gradient(135deg,#5B3DF5,#7C5CFF)', SPARK, false, false), 1500,
      () => set('Sending…', 'linear-gradient(135deg,#22D3EE,#7C5CFF)', SPARK, false, false), 900,
      () => set('<span class="text-aura-mint">✓</span> Sent!', G, CHECK, false, true), 2600,
    ];
    let i = 0;
    const tick = () => { steps[i](); const d = steps[i + 1]; i = (i + 2) % steps.length; setTimeout(tick, d); };
    if (reduce) set('<span class="text-aura-mint">✓</span> Sent!', G, CHECK, false, true); else tick();
  }

  // ---- before / after ------------------------------------------------------------------------
  const ba = $('#ba');
  if (ba) {
    let seed = 5; const r = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
    const N = 48, noisy = [...Array(N)].map(() => 14 + r() * 40);
    const clean = [...Array(N)].map((_, k) => 4 + Math.sin(k / N * Math.PI * 3) ** 2 * 50);
    const box = $('#ba-bars');
    box.innerHTML = noisy.map((h, k) => `<i class="block w-1 rounded-full transition-all duration-700" data-n="${h}" data-c="${clean[k]}" style="height:${h}px;background:#FF5C93"></i>`).join('');
    const apply = on => {
      box.querySelectorAll('i').forEach(b => { b.style.height = (on ? b.dataset.c : b.dataset.n) + 'px'; b.style.background = on ? '#22D3EE' : '#FF5C93'; });
      $('#ba-label').textContent = on ? 'AuraMic clean' : 'Original';
      $('#ba-btn').setAttribute('aria-pressed', String(on));
      $('#ba-btn').textContent = on ? 'Show original' : 'Remove the noise';
    };
    let on = false; $('#ba-btn').addEventListener('click', () => { on = !on; apply(on); });
    apply(false);
  }
})();
