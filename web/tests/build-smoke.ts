// CI proof uses a synthetic header, never a copyrighted game or a personal key.
import { writeFile, readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { buildApk } from "../src/apk";
import { createIdentity } from "../src/signing";
const publicDir = resolve("public");
globalThis.fetch = (async (input: string | URL | Request) => {
  const url = new URL(String(input));
  const file = resolve(publicDir, "." + decodeURIComponent(url.pathname));
  if (!file.startsWith(publicDir + "/"))
    throw new Error("Invalid template path");
  return new Response(await readFile(file));
}) as typeof fetch;
const rom = new Uint8Array(192);
rom[0xb2] = 0x96;
const result = await buildApk(
  {
    rom,
    filename: "Teste.gba",
    title: "Pokémon — Ação",
    logo: "auto",
    identity: await createIdentity(),
    baseUrl: "https://local.invalid/",
  },
  () => {},
);
await writeFile("../build/web-smoke.apk", result.apk);
const tampered = result.apk.slice();
tampered[10000] ^= 1;
await writeFile("../build/web-tampered.apk", tampered);
console.log("APK de validação criado: build/web-smoke.apk");
