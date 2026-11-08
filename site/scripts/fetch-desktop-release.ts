#!/usr/bin/env node
/**
 * Fetches the newest non-draft PakkuDesktop GitHub release (including
 * prereleases) and writes site/src/data/desktop-downloads.json.
 *
 * CI / networked builds: fetch fails → exit 1.
 * Offline local builds: if a previous manifest exists, keep it and warn.
 */
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

type GhAsset = {
  name: string;
  browser_download_url: string;
  size: number;
};

type GhRelease = {
  tag_name: string;
  published_at: string;
  draft: boolean;
  prerelease: boolean;
  assets: GhAsset[];
};

type Manifest = {
  tag: string;
  version: string;
  publishedAt: string;
  prerelease: boolean;
  assets: { name: string; url: string; size: number }[];
};

const __dirname = dirname(fileURLToPath(import.meta.url));
const outPath = join(__dirname, '../src/data/desktop-downloads.json');
const apiUrl =
  'https://api.github.com/repos/juraj-hrivnak/PakkuDesktop/releases?per_page=10';

const skipAsset = (name: string) =>
  name.endsWith('.blockmap') ||
  name.endsWith('.yml') ||
  name.endsWith('.yaml') ||
  name === 'java.exe' ||
  name === 'javaw.exe' ||
  name.endsWith('.asc');

async function fetchRelease(): Promise<Manifest> {
  const headers: Record<string, string> = {
    Accept: 'application/vnd.github+json',
    'User-Agent': 'pakku-website-build',
  };
  if (process.env.GITHUB_TOKEN) {
    headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
  }

  const res = await fetch(apiUrl, { headers });
  if (!res.ok) {
    throw new Error(`GitHub API ${res.status}: ${await res.text()}`);
  }

  const releases = (await res.json()) as GhRelease[];
  const release = releases.find((r) => !r.draft);
  if (!release) {
    throw new Error('No non-draft PakkuDesktop release found');
  }

  const assets = release.assets
    .filter((a) => !skipAsset(a.name))
    .map((a) => ({
      name: a.name,
      url: a.browser_download_url,
      size: a.size,
    }));

  if (assets.length === 0) {
    throw new Error(`Release ${release.tag_name} has no downloadable assets`);
  }

  return {
    tag: release.tag_name,
    version: release.tag_name.replace(/^v/, ''),
    publishedAt: release.published_at,
    prerelease: release.prerelease,
    assets,
  };
}

async function main() {
  const ci = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';

  try {
    const manifest = await fetchRelease();
    await mkdir(dirname(outPath), { recursive: true });
    await writeFile(outPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
    console.log(
      `Wrote ${outPath} (${manifest.tag}, ${manifest.assets.length} assets)`,
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);

    if (ci) {
      console.error(`fetch-desktop-release failed in CI: ${message}`);
      process.exit(1);
    }

    try {
      await readFile(outPath, 'utf8');
      console.warn(
        `fetch-desktop-release failed (${message}); keeping existing manifest.`,
      );
    } catch {
      console.error(
        `fetch-desktop-release failed (${message}) and no existing manifest at ${outPath}`,
      );
      process.exit(1);
    }
  }
}

main();
