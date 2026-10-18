import { backup, getIdentity, restoreBackup, fingerprint } from "./identity";
import {
  gameTitle,
  validateRomFile,
  GBA_ADDRESSABLE_BYTES,
  type Logo,
} from "./rom";

const el = <T extends HTMLElement>(id: string) =>
  document.getElementById(id) as T;
const rom = el<HTMLInputElement>("rom");
const title = el<HTMLInputElement>("title");
const logo = el<HTMLSelectElement>("logo");
const options = el<HTMLFieldSetElement>("options");
const generate = el<HTMLButtonElement>("generate");
const panel = el("status-panel");
const status = el("status");
const error = el("error");
const fileError = el("file-error");
const progress = el<HTMLProgressElement>("progress");
const download = el<HTMLAnchorElement>("download");
const cancel = el<HTMLButtonElement>("cancel");
let selected: File | undefined;
let objectUrl: string | undefined;
let worker: Worker | undefined;
let busy = false;
let generation = 0;
let automaticLogo: Logo = "gba";

function discardResult() {
  if (objectUrl) URL.revokeObjectURL(objectUrl);
  objectUrl = undefined;
  download.hidden = true;
  download.removeAttribute("href");
  el("install-note").hidden = true;
}

function showError(message: string) {
  panel.hidden = false;
  status.textContent = "";
  error.textContent = message;
  error.hidden = false;
  progress.hidden = true;
}

function updatePreview() {
  el("preview-title").textContent =
    title.value.trim() ||
    selected?.name.replace(/\.gba$/i, "") ||
    "Seu próximo jogo";
  const chosen = logo.value === "auto" ? automaticLogo : logo.value;
  const preview = el<HTMLImageElement>("preview-logo");
  preview.src = new URL(
    chosen === "gba" ? "icon.svg" : `logos/${chosen}.webp`,
    document.baseURI,
  ).href;
}

async function selectFile(file?: File) {
  discardResult();
  error.hidden = true;
  fileError.hidden = true;
  panel.hidden = true;
  selected = undefined;
  generate.disabled = true;
  el("compatibility-note").hidden = true;
  el("rom-name").textContent = "Escolher uma ROM";
  el("rom-detail").textContent =
    "Arraste seu arquivo .gba ou clique para escolher · até 50 MB";
  if (!file) return;
  try {
    validateRomFile(file.name, file.size);
  } catch (error) {
    fileError.textContent = (error as Error).message;
    fileError.hidden = false;
    rom.setAttribute("aria-invalid", "true");
    rom.focus();
    return;
  }
  rom.removeAttribute("aria-invalid");
  selected = file;
  title.value = file.name.replace(/\.gba$/i, "").slice(0, 100);
  el("rom-name").textContent = file.name;
  el("rom-detail").textContent =
    `${(file.size / 1_000_000).toLocaleString("pt-BR", { maximumFractionDigits: 2 })} MB · clique para trocar`;
  el("compatibility-note").hidden = file.size <= GBA_ADDRESSABLE_BYTES;
  generate.disabled = false;
  automaticLogo = "gba";
  updatePreview();
  let header: Uint8Array;
  try {
    header = new Uint8Array(await file.slice(0, 192).arrayBuffer());
  } catch {
    if (selected !== file) return;
    selected = undefined;
    generate.disabled = true;
    fileError.textContent =
      "Não foi possível ler o arquivo. Selecione a ROM novamente.";
    fileError.hidden = false;
    return;
  }
  if (selected !== file) return;
  const code = new TextDecoder("ascii").decode(header.subarray(0xac, 0xb0));
  automaticLogo = code.startsWith("BPE")
    ? "emerald"
    : code.startsWith("BPR")
      ? "firered"
      : "gba";
  updatePreview();
}

rom.addEventListener("change", () => void selectFile(rom.files?.[0]));
const drop = el("drop-zone");
drop.addEventListener("dragover", (event) => {
  event.preventDefault();
  if (!busy) drop.classList.add("dragging");
});
drop.addEventListener("dragleave", () => drop.classList.remove("dragging"));
drop.addEventListener("drop", (event) => {
  event.preventDefault();
  drop.classList.remove("dragging");
  if (!busy) void selectFile(event.dataTransfer?.files[0]);
});
for (const input of [title, logo]) {
  input.addEventListener("input", () => {
    discardResult();
    panel.hidden = true;
    updatePreview();
  });
}

function unlock() {
  busy = false;
  options.disabled = false;
  generate.disabled = !selected;
  el<HTMLButtonElement>("export-key").disabled = false;
  el<HTMLInputElement>("restore-key").disabled = false;
  cancel.hidden = true;
  worker?.terminate();
  worker = undefined;
}

cancel.addEventListener("click", () => {
  generation++;
  unlock();
  progress.hidden = true;
  status.textContent = "Geração cancelada. Sua ROM não foi alterada.";
  generate.focus();
});

el<HTMLFormElement>("generator").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy || !selected) return;
  const job = ++generation;
  const file = selected;
  busy = true;
  discardResult();
  error.hidden = true;
  options.disabled = true;
  el<HTMLButtonElement>("export-key").disabled = true;
  el<HTMLInputElement>("restore-key").disabled = true;
  panel.hidden = false;
  progress.hidden = false;
  progress.value = 0;
  cancel.hidden = false;
  status.textContent = "Preparando sua chave local…";
  try {
    if (
      !window.isSecureContext ||
      !crypto.subtle ||
      !window.Worker ||
      !window.indexedDB
    ) {
      throw new Error("Use um navegador atualizado em HTTPS para gerar o APK.");
    }
    const gameName = gameTitle(title.value, file.name);
    const chosenLogo = logo.value as Logo;
    const identity = await getIdentity();
    if (job !== generation) return;
    el("key-label").textContent =
      `Chave local · ${await fingerprint(identity)}`;
    const data = new Uint8Array(await file.arrayBuffer());
    if (job !== generation) return;
    worker = new Worker(new URL("./worker.ts", import.meta.url), {
      type: "module",
    });
    worker.onerror = () => {
      if (job !== generation) return;
      showError(
        "O processamento foi interrompido. Feche outras abas para liberar memória e tente novamente.",
      );
      unlock();
    };
    worker.onmessage = (event) => {
      if (job !== generation) return;
      const result = event.data;
      if (result.type === "progress") {
        progress.value = result.value;
        status.textContent = result.message;
      } else if (result.type === "error") {
        showError(result.message);
        unlock();
        generate.focus();
      } else if (result.type === "done") {
        objectUrl = URL.createObjectURL(
          new Blob([result.apk], {
            type: "application/vnd.android.package-archive",
          }),
        );
        download.href = objectUrl;
        download.download = result.filename;
        download.hidden = false;
        el("install-note").hidden = false;
        progress.hidden = true;
        status.textContent = "Pronto. A próxima partida é no Android.";
        unlock();
        download.focus();
      }
    };
    worker.postMessage(
      {
        rom: data,
        filename: file.name,
        title: gameName,
        logo: chosenLogo,
        identity,
        baseUrl: new URL(import.meta.env.BASE_URL, document.baseURI).href,
      },
      [data.buffer],
    );
  } catch (error) {
    if (job !== generation) return;
    showError(
      error instanceof Error
        ? error.message
        : "Não foi possível gerar o APK. Tente novamente.",
    );
    unlock();
    generate.focus();
  }
});

function saveFile(content: string, name: string) {
  const url = URL.createObjectURL(
    new Blob([content], { type: "application/json" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
}

el("export-key").addEventListener("click", async () => {
  const message = el("key-status");
  try {
    saveFile(await backup(), "gba-studio-chave.json");
    el("key-label").textContent =
      `Chave local · ${await fingerprint(await getIdentity())}`;
    message.textContent =
      "Backup exportado. Guarde sua chave em um lugar seguro.";
  } catch (error) {
    message.textContent =
      error instanceof Error
        ? error.message
        : "Não foi possível exportar a chave.";
  }
});

el<HTMLInputElement>("restore-key").addEventListener(
  "change",
  async (event) => {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || busy) return;
    const message = el("key-status");
    const button = el<HTMLButtonElement>("generate");
    busy = true;
    options.disabled = true;
    el<HTMLButtonElement>("export-key").disabled = true;
    button.disabled = true;
    input.disabled = true;
    try {
      if (file.size > 32768)
        throw new Error(
          "Backup inválido. Escolha o JSON exportado pelo GBA Studio.",
        );
      const identity = await restoreBackup(await file.text());
      el("key-label").textContent =
        `Chave local · ${await fingerprint(identity)}`;
      discardResult();
      panel.hidden = true;
      message.textContent =
        "Chave restaurada. Os próximos APKs usarão esta chave.";
    } catch (error) {
      message.textContent =
        error instanceof Error
          ? error.message
          : "Não foi possível restaurar a chave.";
    } finally {
      input.value = "";
      unlock();
    }
  },
);

function setTheme(theme: string) {
  const light = theme === "light";
  document.documentElement.dataset.theme = light ? "light" : "dark";
  const toggle = el<HTMLButtonElement>("theme-toggle");
  toggle.setAttribute(
    "aria-label",
    light ? "Ativar tema escuro" : "Ativar tema claro",
  );
  toggle.setAttribute("aria-pressed", String(light));
}
try {
  setTheme(localStorage.getItem("gba-studio-theme") || "dark");
} catch {
  setTheme("dark");
}
el("theme-toggle").addEventListener("click", () => {
  const theme =
    document.documentElement.dataset.theme === "dark" ? "light" : "dark";
  setTheme(theme);
  try {
    localStorage.setItem("gba-studio-theme", theme);
  } catch {
    /* Preference is optional. */
  }
});
window.addEventListener("pagehide", () => {
  generation++;
  unlock();
  discardResult();
});
