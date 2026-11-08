#!/usr/bin/env node
/**
 * Resolves browser via PLAYWRIGHT_CHROMIUM, then `chromium` on PATH, then
 * `nix-build '<nixpkgs>' -A chromium`. Clears LD_LIBRARY_PATH so a nix
 * develop shell does not break Chromium's own library wrapper.
 */
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

function resolveChromium(): string | null {
  if (process.env.PLAYWRIGHT_CHROMIUM && existsSync(process.env.PLAYWRIGHT_CHROMIUM)) {
    return process.env.PLAYWRIGHT_CHROMIUM;
  }

  const which = spawnSync('bash', ['-lc', 'command -v chromium'], {
    encoding: 'utf8',
  });
  const onPath = which.stdout?.trim();
  if (onPath && existsSync(onPath)) return onPath;

  const built = spawnSync(
    'nix-build',
    ['<nixpkgs>', '-A', 'chromium', '--no-out-link'],
    { encoding: 'utf8' },
  );
  if (built.status === 0) {
    const root = built.stdout.trim().split('\n').at(-1);
    const bin = `${root}/bin/chromium`;
    if (existsSync(bin)) return bin;
  }

  return null;
}

const chromium = resolveChromium();
const env = { ...process.env };
delete env.LD_LIBRARY_PATH;
if (chromium) {
  env.PLAYWRIGHT_CHROMIUM = chromium;
  console.log(`Using Chromium: ${chromium}`);
} else {
  console.warn(
    "No nix/system Chromium found; falling back to Playwright's bundled browser.",
  );
}

const cli = require.resolve('@playwright/test/cli');
const result = spawnSync(process.execPath, [cli, 'test', ...process.argv.slice(2)], {
  stdio: 'inherit',
  env,
});
process.exit(result.status ?? 1);
