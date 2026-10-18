const path = require("node:path");

/** Keep the renderer inside the packaged, immutable site directory. */
function resolveAsset(root, address) {
  const url = new URL(address);
  if (url.protocol !== "studio:" || url.hostname !== "app")
    throw new Error("Unknown origin");
  const pathname = decodeURIComponent(url.pathname);
  if (pathname.includes("\0") || pathname.includes("\\"))
    throw new Error("Invalid path");
  const file = path.resolve(
    root,
    "." + (pathname === "/" ? "/index.html" : pathname),
  );
  if (!file.startsWith(path.resolve(root) + path.sep))
    throw new Error("Path outside application");
  return file;
}
module.exports = { resolveAsset };
