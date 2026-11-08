import { expect, test } from "@playwright/test";

test.describe("pakku primary button", () => {
  test("hover inverts to outline cyan", async ({ page }) => {
    await page.goto("/");

    const btn = page.locator("a.pakku-btn--primary").first();
    await expect(btn).toBeVisible();
    await btn.scrollIntoViewIfNeeded();

    const sample = () =>
      btn.evaluate((el) => {
        const s = getComputedStyle(el);
        return {
          bg: s.backgroundColor,
          border: s.borderTopColor,
          color: s.color,
        };
      });

    const before = await sample();
    expect(before.bg).toBe("rgb(0, 255, 255)");
    expect(before.color).toBe("rgb(0, 25, 26)");

    await btn.hover();
    await expect
      .poll(async () => {
        const s = await sample();
        return `${s.bg}|${s.color}|${s.border}`;
      })
      .toBe("rgba(0, 0, 0, 0)|rgb(0, 255, 255)|rgb(0, 255, 255)");

    await page.mouse.down();
    await expect
      .poll(async () => {
        const s = await sample();
        return `${s.color}|${s.border}`;
      })
      .toBe("rgb(0, 255, 255)|rgb(0, 255, 255)");
    await page.mouse.up();

    const top = await btn.evaluate((el) => {
      const r = el.getBoundingClientRect();
      const hit = document.elementFromPoint(
        r.left + r.width / 2,
        r.top + r.height / 2,
      );
      return hit === el;
    });
    expect(top).toBe(true);
  });
});
