// Runs unchanged on Linux and Windows; never reads a real game or a user key.
const { _electron } = require("playwright-core");
const path = require("node:path");
const os = require("node:os");
const fs = require("node:fs/promises");
const assert = require("node:assert/strict");

(async () => {
  const profile = await fs.mkdtemp(path.join(os.tmpdir(), "gba-studio-test-"));
  const output =
    process.env.GBA_STUDIO_SMOKE_APK || path.join(profile, "smoke.apk");
  const executablePath =
    process.env.GBA_STUDIO_EXECUTABLE || require("electron");
  async function launch() {
    return _electron.launch({
      executablePath,
      args: process.env.GBA_STUDIO_EXECUTABLE
        ? []
        : [path.resolve(__dirname, "..")],
      env: { ...process.env, GBA_STUDIO_TEST_PROFILE: profile },
      timeout: 45000,
    });
  }
  let application;
  try {
    application = await launch();
    let page = await application.firstWindow();
    await page.waitForLoadState("domcontentloaded");
    assert.equal(
      await page.evaluate(
        () => isSecureContext && !!crypto.subtle && !("require" in window),
      ),
      true,
    );
    await application.evaluate(({ session }, filename) => {
      session.defaultSession.on("will-download", (_event, item) =>
        item.setSavePath(filename),
      );
    }, output);
    await application.context().setOffline(true);
    const rom = Buffer.alloc(192);
    rom[0xb2] = 0x96;
    await page.locator("#rom").setInputFiles({
      name: "Offline.gba",
      mimeType: "application/octet-stream",
      buffer: rom,
    });
    await page.locator("#generate").click();
    await page
      .locator("#download")
      .waitFor({ state: "visible", timeout: 45000 });
    const fingerprint = await page.locator("#key-label").textContent();
    await page.locator("#download").click();
    for (let i = 0; i < 50; i++) {
      try {
        if ((await fs.stat(output)).size > 0) break;
      } catch {
        /* Download is pending. */
      }
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    assert.ok((await fs.stat(output)).size > 1000000);
    await application.close();
    application = await launch();
    page = await application.firstWindow();
    await page.waitForLoadState("domcontentloaded");
    await page.locator("#rom").setInputFiles({
      name: "Offline.gba",
      mimeType: "application/octet-stream",
      buffer: rom,
    });
    await page.locator("#generate").click();
    await page
      .locator("#download")
      .waitFor({ state: "visible", timeout: 45000 });
    assert.equal(await page.locator("#key-label").textContent(), fingerprint);
    console.log(
      "PASS: secure offline origin, Worker APK generation, download and persistent signing identity.",
    );
  } finally {
    if (application) await application.close();
    await fs.rm(profile, { recursive: true, force: true });
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
