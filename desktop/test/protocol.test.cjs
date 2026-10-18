const { test } = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const { resolveAsset } = require("../protocol.cjs");
const root = path.resolve("example-site");
test("resolves only local application assets", () => {
  assert.equal(
    resolveAsset(root, "studio://app/"),
    path.join(root, "index.html"),
  );
  assert.equal(
    resolveAsset(root, "studio://app/assets/main.js?v=1"),
    path.join(root, "assets/main.js"),
  );
  assert.throws(() => resolveAsset(root, "https://example.com/"));
  assert.throws(() => resolveAsset(root, "studio://other/index.html"));
  assert.throws(() => resolveAsset(root, "studio://app/%2e%2e%2fprivate/key"));
  assert.throws(() => resolveAsset(root, "studio://app/%00file"));
});
