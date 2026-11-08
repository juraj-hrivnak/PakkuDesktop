const root = document.querySelector<HTMLElement>('[data-showcase]');

if (root) {
  const items = Array.from(root.querySelectorAll<HTMLElement>('.shots-item'));
  root.classList.add('has-spy');

  let activeIdx = -1;
  let ticking = false;

  const update = () => {
    ticking = false;
    if (items.length === 0) return;
    // highlight the block nearest the middle of the viewport
    const mid = innerHeight * 0.5;
    let best = Infinity;
    let idx = 0;
    items.forEach((el, i) => {
      const r = el.getBoundingClientRect();
      const d = Math.abs(r.top + r.height / 2 - mid);
      if (d < best) { best = d; idx = i; }
    });
    if (idx !== activeIdx) {
      activeIdx = idx;
      items.forEach((el, i) => el.classList.toggle('is-active', i === idx));
    }
  };

  const onScroll = () => {
    if (!ticking) { ticking = true; requestAnimationFrame(update); }
  };

  addEventListener('scroll', onScroll, { passive: true });
  addEventListener('resize', onScroll);
  update();

  // lightbox: click a screenshot to view it, click it again to zoom,
  // click the backdrop or the X to close
  const lb = document.getElementById('lightbox');
  const lbImg = document.getElementById('lightboxImg') as HTMLImageElement | null;
  const lbX = document.getElementById('lightboxClose');
  if (lb && lbImg) {
    // use the largest srcset candidate so the lightbox is not pixelated
    const fullSrc = (img: HTMLImageElement) => {
      const ss = img.getAttribute('srcset');
      if (!ss) return img.currentSrc || img.src;
      let best = ss.split(',')[0].trim().split(/\s+/);
      for (const part of ss.split(',')) {
        const p = part.trim().split(/\s+/);
        if (parseInt(p[1] ?? '0', 10) > parseInt(best[1] ?? '0', 10)) best = p;
      }
      return new URL(best[0], location.href).href;
    };
    let closeTimer: number | undefined;
    const open = (img: HTMLImageElement) => {
      window.clearTimeout(closeTimer);
      lbImg.src = fullSrc(img);
      lbImg.alt = img.alt;
      lbImg.style.transform = '';
      lbImg.style.transition = '';
      lb.classList.remove('zoomed', 'closing');
      lb.hidden = false;
      document.body.style.overflow = 'hidden';
      // animate in on the next frame
      requestAnimationFrame(() => lb.classList.add('open'));
      lbX?.focus();
    };
    const close = () => {
      if (lb.hidden) return;
      lb.classList.remove('open');
      lb.classList.add('closing');
      window.clearTimeout(closeTimer);
      closeTimer = window.setTimeout(() => {
        lb.hidden = true;
        lb.classList.remove('closing', 'zoomed');
        lbImg.style.transform = '';
        lbImg.style.transition = '';
        document.body.style.overflow = '';
      }, 180);
    };
    const toggleZoom = () => {
      if (!lb.classList.contains('zoomed')) {
        const fitW = lbImg.getBoundingClientRect().width;
        const natural = lbImg.naturalWidth || fitW;
        lb.classList.add('zoomed'); // layout switches to natural size…
        // …then animate from the fit size up to it
        const k = Math.min(fitW / Math.max(1, natural), 1);
        lbImg.style.transition = 'none';
        lbImg.style.transform = `scale(${k})`;
        lbImg.style.transformOrigin = 'center top';
        requestAnimationFrame(() => {
          lbImg.style.transition = 'transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1)';
          lbImg.style.transform = 'scale(1)';
        });
      } else {
        lb.classList.remove('zoomed');
        lbImg.style.transition = 'transform 0.25s ease';
        lbImg.style.transform = '';
      }
    };
    root.querySelectorAll<HTMLImageElement>('img.shots-pane').forEach((img) => {
      img.addEventListener('click', () => open(img));
    });
    lb.addEventListener('click', (e) => {
      if (e.target === lb) close();
      else if (e.target === lbImg) toggleZoom();
    });
    lbX?.addEventListener('click', close);
    addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && !lb.hidden) close();
    });
  }
}
