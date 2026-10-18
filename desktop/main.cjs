const {
  app,
  BrowserWindow,
  protocol,
  net,
  shell,
  dialog,
  session,
} = require("electron");
const path = require("node:path");
const { pathToFileURL } = require("node:url");
const { resolveAsset } = require("./protocol.cjs");

protocol.registerSchemesAsPrivileged([
  {
    scheme: "studio",
    privileges: {
      standard: true,
      secure: true,
      supportFetchAPI: true,
      corsEnabled: true,
    },
  },
]);
app.setName("GBA Studio");
// A fixed scheme/origin and partition preserve IndexedDB keys across every launch.
if (process.env.GBA_STUDIO_TEST_PROFILE)
  app.setPath("userData", process.env.GBA_STUDIO_TEST_PROFILE);
const siteRoot = app.isPackaged
  ? path.join(process.resourcesPath, "site")
  : path.resolve(__dirname, "../web/site");
let window;

function openExternal(address) {
  try {
    const url = new URL(address);
    if (url.protocol === "https:" && url.hostname === "github.com")
      void shell.openExternal(url.href);
  } catch {
    /* Ignore invalid external destinations. */
  }
}

async function createWindow() {
  window = new BrowserWindow({
    width: 1320,
    height: 950,
    minWidth: 380,
    minHeight: 620,
    backgroundColor: "#282344",
    title: "GBA Studio",
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
    },
  });
  window.webContents.setWindowOpenHandler(({ url }) => {
    openExternal(url);
    return { action: "deny" };
  });
  window.webContents.on("will-navigate", (event, address) => {
    const url = new URL(address);
    if (url.protocol !== "studio:" || url.hostname !== "app") {
      event.preventDefault();
      openExternal(address);
    }
  });
  window.webContents.on("will-attach-webview", (event) =>
    event.preventDefault(),
  );
  await window.loadURL("studio://app/index.html");
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (window) {
      if (window.isMinimized()) window.restore();
      window.focus();
    }
  });
  app.whenReady().then(async () => {
    protocol.handle("studio", async (request) => {
      if (!["GET", "HEAD"].includes(request.method))
        return new Response("Method not allowed", { status: 405 });
      try {
        return await net.fetch(
          pathToFileURL(resolveAsset(siteRoot, request.url)).href,
        );
      } catch {
        return new Response("Asset not found", { status: 404 });
      }
    });
    session.defaultSession.setPermissionRequestHandler(
      (_contents, _permission, callback) => callback(false),
    );
    session.defaultSession.on(
      "will-download",
      async (_event, item, contents) => {
        const parent = BrowserWindow.fromWebContents(contents);
        item.setSaveDialogOptions({
          title: "Salvar arquivo do GBA Studio",
          defaultPath: path.join(app.getPath("downloads"), item.getFilename()),
        });
        item.once("done", (_event, state) => {
          if (state === "interrupted" && parent)
            void dialog.showMessageBox(parent, {
              type: "error",
              message:
                "Não foi possível salvar. Tente gerar e baixar novamente.",
            });
        });
      },
    );
    await createWindow();
    app.on("activate", () => {
      if (!BrowserWindow.getAllWindows().length) void createWindow();
    });
  });
  app.on("window-all-closed", () => app.quit());
}
