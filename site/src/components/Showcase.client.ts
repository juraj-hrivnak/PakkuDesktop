const sec = document.querySelector<HTMLElement>('.shots');
const stage = sec?.querySelector<HTMLElement>('.shots-stage');
const desk = document.getElementById('shotsRow');
const cli = document.getElementById('cliRow');

if (sec && stage && desk) {
  const rails: HTMLElement[] = cli ? [desk, cli] : [desk];
  const narrow = matchMedia('(max-width: 720px)');
  const reduce = matchMedia('(prefers-reduced-motion: reduce)');

  let travel = 0;
  let view = 0;
  let ticking = false;

  const measure = () => {
    if (narrow.matches) {
      travel = 0;
      sec.style.height = '';
      for (const el of rails) el.style.transform = '';
      return;
    }

    const cs = getComputedStyle(stage);
    const padY = (parseFloat(cs.paddingTop) || 0) + (parseFloat(cs.paddingBottom) || 0);
    view = stage.clientHeight - padY;
    travel = Math.max(0, ...rails.map((el) => el.scrollHeight - view));
    sec.style.height = `${view + padY + travel}px`;
  };

  const apply = () => {
    if (narrow.matches || reduce.matches || travel <= 0) {
      for (const el of rails) el.style.transform = '';
      return;
    }

    const top = sec.getBoundingClientRect().top;
    const prog = Math.min(1, Math.max(0, -top / travel));
    const y = -prog * travel;
    const tx = `translate3d(0, ${y}px, 0)`;
    for (const rail of rails) rail.style.transform = tx;
  };

  const onScroll = () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => {
      apply();
      ticking = false;
    });
  };

  const onResize = () => {
    measure();
    apply();
  };

  const pending: Promise<unknown>[] = [];
  for (const rail of rails) {
    for (const img of rail.querySelectorAll('img')) {
      if (img.complete) continue;
      pending.push(
        img.decode?.().catch(() => undefined) ??
          new Promise<void>((resolve) => img.addEventListener('load', () => resolve(), { once: true })),
      );
    }
  }

  measure();
  apply();
  void Promise.all(pending).then(onResize);

  addEventListener('scroll', onScroll, { passive: true });
  addEventListener('resize', onResize);
  narrow.addEventListener('change', onResize);
  reduce.addEventListener('change', onResize);
}
