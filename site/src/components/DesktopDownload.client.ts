import type { ArchId, OsId } from '../lib/desktopDownloads';
import { isOsId } from '../lib/desktopDownloads';

function detectOs(): OsId {
  const ua = navigator.userAgent;
  const platform =
    (navigator as Navigator & { userAgentData?: { platform?: string } })
      .userAgentData?.platform ??
    navigator.platform ??
    '';

  if (/Win/i.test(platform) || /Windows/i.test(ua)) return 'windows';
  if (/Mac/i.test(platform) || /Mac OS|Macintosh/i.test(ua)) return 'macos';
  return 'linux';
}

function detectArch(): ArchId {
  const ua = navigator.userAgent;
  const uaData = (
    navigator as Navigator & {
      userAgentData?: { architecture?: string };
    }
  ).userAgentData;

  const archHint = uaData?.architecture;
  if (typeof archHint === 'string') {
    if (/arm/i.test(archHint)) return 'arm64';
    if (/x86|x64|amd64/i.test(archHint)) return 'x64';
  }

  if (/aarch64|arm64|Apple Silicon/i.test(ua)) return 'arm64';
  if (
    /Mac/i.test(navigator.platform || '') &&
    !(navigator as Navigator & { userAgentData?: unknown }).userAgentData
  ) {
    return 'arm64';
  }
  return 'x64';
}

function readSection(): OsId | null {
  const value = new URLSearchParams(window.location.search).get('section');
  return isOsId(value) ? value : null;
}

function writeSection(os: OsId) {
  const url = new URL(window.location.href);
  url.searchParams.set('section', os);
  window.history.replaceState({}, '', url);
}

function init() {
  const root = document.querySelector('[data-desktop-download]');
  if (!(root instanceof HTMLElement)) return;

  const tabs = root.querySelectorAll<HTMLElement>('[data-os-tab]');
  const panels = root.querySelectorAll<HTMLElement>('[data-os-panel]');
  const archButtons = root.querySelectorAll<HTMLElement>('[data-arch]');
  const linuxNotes = root.querySelectorAll<HTMLElement>('[data-linux-note]');

  const detectedOs = detectOs();
  const detectedArch = detectArch();
  let selectedOs: OsId = readSection() ?? detectedOs;
  const archByOs: Record<OsId, ArchId> = {
    windows: detectedArch,
    macos: detectedArch,
    linux: detectedArch,
  };

  const paint = () => {
    for (const tab of tabs) {
      const os = tab.dataset.osTab;
      if (!isOsId(os)) continue;
      const active = os === selectedOs;
      tab.classList.toggle('is-active', active);
      tab.setAttribute('aria-selected', active ? 'true' : 'false');
      tab.tabIndex = active ? 0 : -1;
    }

    for (const panel of panels) {
      const os = panel.dataset.osPanel;
      if (!isOsId(os)) continue;
      const active = os === selectedOs;
      panel.hidden = !active;
      panel.classList.toggle('is-active', active);
    }

    for (const note of linuxNotes) {
      note.hidden = selectedOs !== 'linux';
    }

    for (const btn of archButtons) {
      const os = btn.dataset.os as OsId | undefined;
      const arch = btn.dataset.arch as ArchId | undefined;
      if (!isOsId(os) || (arch !== 'x64' && arch !== 'arm64')) continue;
      const active = archByOs[os] === arch;
      btn.classList.toggle('pakku-btn--primary', active);
      btn.setAttribute('aria-pressed', active ? 'true' : 'false');
    }

    root.querySelectorAll<HTMLElement>('[data-arch-panel]').forEach((panel) => {
      const os = panel.dataset.os as OsId | undefined;
      const arch = panel.dataset.archPanel as ArchId | undefined;
      if (!isOsId(os) || (arch !== 'x64' && arch !== 'arm64')) return;
      panel.hidden = !(os === selectedOs && archByOs[os] === arch);
    });
  };

  for (const tab of tabs) {
    tab.addEventListener('click', () => {
      const os = tab.dataset.osTab;
      if (!isOsId(os)) return;
      selectedOs = os;
      writeSection(os);
      paint();
    });
  }

  for (const btn of archButtons) {
    btn.addEventListener('click', () => {
      const os = btn.dataset.os as OsId | undefined;
      const arch = btn.dataset.arch as ArchId | undefined;
      if (!isOsId(os) || (arch !== 'x64' && arch !== 'arm64')) return;
      archByOs[os] = arch;
      paint();
    });
  }

  writeSection(selectedOs);
  paint();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
