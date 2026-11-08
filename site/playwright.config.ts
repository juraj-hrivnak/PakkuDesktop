import { defineConfig, devices } from "@playwright/test";

const executablePath = process.env.PLAYWRIGHT_CHROMIUM || undefined;

/**
 * On NixOS, point PLAYWRIGHT_CHROMIUM at a nixpkgs Chromium and unset
 * LD_LIBRARY_PATH (nix develop injects libs that break Chromium's wrapper).
 * See scripts/run-e2e.ts.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: [["list"]],
  use: {
    ...devices["Desktop Chrome"],
    baseURL: "http://127.0.0.1:4321",
    trace: "on-first-retry",
    launchOptions: {
      ...(executablePath ? { executablePath } : {}),
      args: ["--no-sandbox", "--disable-gpu"],
    },
  },
  webServer: {
    command: "node --experimental-strip-types scripts/serve-dist.ts",
    url: "http://127.0.0.1:4321",
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
});
