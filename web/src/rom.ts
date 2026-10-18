import { hex, sha256, type Bytes } from "./bytes";
export type Logo = "auto" | "emerald" | "firered" | "gba";
export const MAX_ROM_BYTES = 50_000_000;
export const GBA_ADDRESSABLE_BYTES = 32 * 1024 * 1024;
export function validateRomFile(filename: string, size: number): void {
  if (!/\.gba$/i.test(filename) || size < 192 || size > MAX_ROM_BYTES) {
    throw new Error("Escolha uma ROM .gba descompactada de até 50 MB.");
  }
}
export async function inspectRom(data: Bytes, filename: string) {
  validateRomFile(filename, data.length);
  if (data[0xb2] !== 0x96)
    throw new Error(
      "O arquivo não tem um cabeçalho GBA reconhecível. Escolha outra ROM.",
    );
  const code = new TextDecoder("ascii").decode(data.subarray(0xac, 0xb0));
  return {
    digest: hex(await sha256(data)),
    logo: code.startsWith("BPE")
      ? "emerald"
      : code.startsWith("BPR")
        ? "firered"
        : "gba",
  } as const;
}
export function gameTitle(title: string, filename: string): string {
  const value = (title.trim() || filename.replace(/\.gba$/i, ""))
    .normalize("NFC")
    .trim();
  if (!value || value.length > 100 || /[\u0000-\u001f\u007f]/.test(value))
    throw new Error(
      "Use um nome entre 1 e 100 caracteres, sem caracteres de controle.",
    );
  return value;
}
export const apkFilename = (title: string): string =>
  (title
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^[-._]+|[-._]+$/g, "")
    .slice(0, 80) || "GBA") + ".apk";
