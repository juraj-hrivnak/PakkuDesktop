import { expect, test } from "@playwright/test";

test.describe("two-column showcase, one-sided features", () => {
  test("renders two columns; pinned text on one side only; CI/CD is CLI-only", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator(".shots-col")).toHaveCount(2);
    await expect(page.locator(".shots-h", { hasText: "CLI" })).toBeAttached();
    await expect(page.locator(".shots-h", { hasText: "Desktop" })).toBeAttached();
    // only the side labels are pinned; feature texts are not pinned,
    // they sit in the gaps between screenshots alternating left/right
    await expect(page.locator(".shots-h h2")).toHaveCount(0);
    await expect(page.locator(".shots-note")).toHaveCount(5);
    await expect(page.locator(".shots-note").first()).toHaveCSS("position", "static");
    await expect(page.locator(".shots-cli .shots-note")).toHaveCount(3);
    await expect(page.locator(".shots-desk .shots-note")).toHaveCount(2);
    await expect(page.locator(".shots-cli .shots-note").first()).toHaveCSS("text-align", "left");
    await expect(page.locator(".shots-desk .shots-note").first()).toHaveCSS("text-align", "right");
    // panes are figures; notes are divs — CI/CD has export + Actions on CLI, 4 Desktop panes
    await expect(page.locator(".shots-cli figure.shots-item")).toHaveCount(6);
    await expect(page.locator(".shots-desk figure.shots-item")).toHaveCount(4);
    // CI/CD lives only on the CLI side
    const ciItems = page.locator('figure.shots-item[data-feat="4"]');
    await expect(ciItems).toHaveCount(2);
    await expect(ciItems.first().locator("..")).toHaveClass(/shots-cli/);
    await expect(page.locator(".shots-desk figure.shots-item[data-feat='4']")).toHaveCount(0);
    await expect(page.locator('.shots-cli figure.shots-item[data-feat="4"]').last()).toContainText("gh-workflow-run.png");
    // real screenshots + skeleton placeholders for the missing ones
    await expect(page.locator(".shots-cli img.shots-pane").first()).toBeVisible();
    await expect(page.locator(".pane.ph").first()).toContainText("needed ·");
    // yeeted sections are gone
    await expect(page.locator(".sec")).toHaveCount(0);
    await expect(page.locator(".steps")).toHaveCount(0);
    await expect(page.locator(".timeline")).toHaveCount(0);
    await expect(page.locator(".card")).toHaveCount(0);
  });

  test("sticky headers are plain text: sticky, no background box, readable in both themes", async ({ page }) => {
    await page.goto("/");
    const h = page.locator(".shots-h").first();
    await expect(h).toHaveCSS("position", "sticky");
    await expect(h).toHaveCSS("background-color", "rgba(0, 0, 0, 0)");
    await expect(h).toHaveCSS("border-top-width", "0px");
    await page.evaluate(() => document.documentElement.setAttribute("data-theme", "pakku-yellow"));
    await expect(h).toHaveCSS("background-color", "rgba(0, 0, 0, 0)");
    await expect(h.locator(".badge")).toHaveCSS("color", "rgb(16, 16, 16)");
  });

  test("scrolling highlights the nearest pane in 3D and moves the pinned text", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator(".shots")).toHaveClass(/has-spy/);
    const activeFeat = () => page.evaluate(() => document.querySelector(".shots-item.is-active")?.dataset.feat ?? null);
    // 3D pop highlight, no border/outline change (first pair: real images both sides)
    const styles = await page.evaluate(() => {
      const active = document.querySelector(".shots-item.is-active img.shots-pane") as HTMLElement;
      const other = document.querySelector(".shots-item:not(.is-active) img.shots-pane") as HTMLElement;
      const a = getComputedStyle(active);
      const o = getComputedStyle(other);
      return { transform: a.transform, aBorder: a.borderTopColor, oBorder: o.borderTopColor };
    });
    expect(styles.transform).not.toBe("none");
    expect(styles.aBorder).toBe(styles.oBorder);
    expect(await activeFeat()).toBe("0");
    // scroll to the CI/CD feature on the CLI side
    await page.evaluate(() => {
      const el = document.querySelector('.shots-cli .shots-item[data-feat="4"]')!;
      window.scrollTo(0, el.getBoundingClientRect().top + window.scrollY - innerHeight * 0.4);
    });
    await page.waitForTimeout(300);
    expect(await activeFeat()).toBe("4");
    // still two columns, no center column
    const layout = await page.evaluate(() => ({
      cols: getComputedStyle(document.querySelector(".shots-stage")!).gridTemplateColumns.split(" ").length,
      children: document.querySelector(".shots-stage")!.children.length,
    }));
    expect(layout.cols).toBe(2);
    expect(layout.children).toBe(2);
    // panes are never dimmed — only the active one changes size
    const opacities = await page.evaluate(() =>
      [...document.querySelectorAll(".shots-item")].map((el) => getComputedStyle(el).opacity),
    );
    expect(opacities.every((o) => o === "1")).toBe(true);
  });

  test("click opens a lightbox; second click zooms; backdrop and X close it", async ({ page }) => {
    await page.goto("/");
    const pane = page.locator(".shots-cli img.shots-pane").first();
    await pane.scrollIntoViewIfNeeded();
    await pane.click();
    const lb = page.locator("#lightbox");
    await expect(lb).toBeVisible();
    await expect(lb).toHaveClass(/open/); // animated open
    await expect(page.locator("#lightboxImg")).toHaveAttribute("src", /screenshot_ls/);
    await expect(lb).not.toHaveClass(/zoomed/);
    // backdrop fades in
    await expect.poll(async () => page.locator("#lightbox").evaluate((el) => getComputedStyle(el).opacity)).toBe("1");
    // lightbox uses the largest srcset variant, not the pixelated pane thumbnail
    const natural = await page.locator("#lightboxImg").evaluate((img: HTMLImageElement) => img.naturalWidth);
    expect(natural).toBeGreaterThan(1500);
    // second click on the image zooms (visibly larger), third un-zooms
    const fitWidth = await page.locator("#lightboxImg").evaluate((img) => img.getBoundingClientRect().width);
    await page.locator("#lightboxImg").click();
    await expect(lb).toHaveClass(/zoomed/);
    // zoom animates via transform
    await expect.poll(async () => page.locator("#lightboxImg").evaluate((img) => (img as HTMLElement).style.transform)).toContain("scale");
    const zoomWidth = await page.locator("#lightboxImg").evaluate((img) => img.getBoundingClientRect().width);
    expect(zoomWidth).toBeGreaterThan(fitWidth);
    await page.locator("#lightboxImg").click();
    await expect(lb).not.toHaveClass(/zoomed/);
    // click away (backdrop) closes
    await page.mouse.click(8, 8);
    await expect(lb).toBeHidden();
    // X button closes too
    await pane.click();
    await expect(lb).toBeVisible();
    await page.locator("#lightboxClose").click();
    await expect(lb).toBeHidden();
  });

  test("columns sit at the outer edges, not the center", async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => {
      const s = document.querySelector(".shots")!;
      window.scrollTo(0, s.getBoundingClientRect().top + window.scrollY + innerHeight * 0.5);
    });
    await page.waitForTimeout(300);
    const pos = await page.evaluate(() => {
      const cli = document.querySelector(".shots-cli .shots-item")!.getBoundingClientRect();
      const desk = document.querySelector(".shots-desk .shots-item")!.getBoundingClientRect();
      return { cliLeft: cli.left, deskRight: desk.right, vw: innerWidth };
    });
    expect(pos.cliLeft).toBeLessThan(pos.vw * 0.2);
    expect(pos.deskRight).toBeGreaterThan(pos.vw * 0.8);
  });

  test("dolphin is untouched: canvas above content, drag hitbox interactive", async ({ page }) => {
    await page.goto("/");
    const z = await page.evaluate(() => ({
      gl: getComputedStyle(document.getElementById("gl")!).zIndex,
      hit: getComputedStyle(document.getElementById("dolphin-hit")!).zIndex,
      main: getComputedStyle(document.querySelector(".home-main")!).zIndex,
    }));
    expect(z.gl).toBe("30");
    expect(z.hit).toBe("33");
    expect(z.main).toBe("auto");
  });

  test("no horizontal overflow on mobile, columns stack", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    const state = await page.evaluate(() => ({
      overflow: document.documentElement.scrollWidth > window.innerWidth,
      stageDisplay: getComputedStyle(document.querySelector(".shots-stage")!).display,
    }));
    expect(state.overflow).toBe(false);
    expect(state.stageDisplay).toBe("flex"); // single column
  });
});
