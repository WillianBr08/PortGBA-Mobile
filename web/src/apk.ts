import { unzipSync } from "fflate";
import { hex, sha256, utf8, type Bytes } from "./bytes";
import { patchManifest, patchResourcePackage } from "./manifest";
import { alignedZip } from "./zip";
import { signApk, type Identity } from "./signing";
import { inspectRom, gameTitle, apkFilename, type Logo } from "./rom";
export interface Catalog {
  version: number;
  package: string;
  title: string;
  templates: Record<string, { file: string; sha256: string; size: number }>;
}
export interface BuildRequest {
  rom: Bytes;
  filename: string;
  title: string;
  logo: Logo;
  identity: Identity;
  baseUrl: string;
}
export async function buildApk(
  request: BuildRequest,
  progress: (value: number, message: string) => void,
) {
  progress(5, "Validando sua ROM…");
  const info = await inspectRom(request.rom, request.filename),
    title = gameTitle(request.title, request.filename);
  const logo = request.logo === "auto" ? info.logo : request.logo;
  if (!["emerald", "firered", "gba"].includes(logo))
    throw new Error("Escolha uma logo disponível.");
  const packageName = "dev.gba.studio.web.g" + info.digest.slice(0, 24);
  progress(15, "Baixando a base do aplicativo…");
  const response = await fetch(
    new URL("templates/catalog.json", request.baseUrl),
  );
  if (!response.ok)
    throw new Error(
      "Bases Android indisponíveis. Verifique a conexão e tente novamente.",
    );
  const catalog: Catalog = await response.json();
  if (
    catalog.version !== 1 ||
    catalog.package !== "dev.gba.studio.web.template" ||
    catalog.title !== "GBA_STUDIO_WEB_TITLE"
  )
    throw new Error("Catálogo incompatível. Atualize a página.");
  const item = catalog.templates[logo];
  if (!item || item.file !== logo + ".apk")
    throw new Error("Base da logo indisponível. Atualize a página.");
  const templateResponse = await fetch(
    new URL("templates/" + item.file, request.baseUrl),
  );
  if (!templateResponse.ok)
    throw new Error(
      "Não foi possível baixar a base. Verifique a conexão e tente novamente.",
    );
  const template = new Uint8Array(await templateResponse.arrayBuffer());
  if (
    template.length !== item.size ||
    hex(await sha256(template)) !== item.sha256
  )
    throw new Error(
      "Base incompleta ou corrompida. Atualize a página e tente novamente.",
    );
  progress(40, "Montando seu aplicativo…");
  const entries: Record<string, Bytes> = {};
  for (const [name, data] of Object.entries(unzipSync(template))) {
    if (
      !["AndroidManifest.xml", "resources.arsc", "classes.dex"].includes(
        name,
      ) &&
      !name.startsWith("res/") &&
      !/^lib\/(arm64-v8a|armeabi-v7a|x86_64)\/libeliteredux\.so$/.test(name) &&
      !name.startsWith("assets/fonts/") &&
      !name.startsWith("assets/licenses/")
    )
      throw new Error("Arquivo inesperado na base Android.");
    entries[name] = new Uint8Array(data);
  }
  if (!entries["classes.dex"] || !entries["lib/arm64-v8a/libeliteredux.so"])
    throw new Error("Base Android incompleta.");
  entries["AndroidManifest.xml"] = patchManifest(
    entries["AndroidManifest.xml"],
    { [catalog.package]: packageName, [catalog.title]: title },
  );
  entries["resources.arsc"] = patchResourcePackage(
    entries["resources.arsc"],
    packageName,
  );
  // Stable paths keep existing Android saves intact when title or logo changes.
  entries["assets/game.json"] = utf8(
    JSON.stringify({
      title,
      base: "game",
      folder: "game",
      description: "Game Boy Advance",
      rom_sha256: info.digest,
      icon: logo,
    }),
  );
  entries["assets/game/game.gba"] = request.rom;
  progress(60, "Alinhando arquivos e controles…");
  const zip = alignedZip(entries);
  progress(80, "Assinando com sua chave local…");
  const apk = await signApk(zip, request.identity);
  progress(100, "Seu APK está pronto.");
  return {
    apk,
    filename: apkFilename(title),
    packageName,
    romSha256: info.digest,
    logo,
  };
}
