import {
  concat,
  u32,
  u64,
  lp,
  sha256,
  utf8,
  base64,
  unbase64,
  type Bytes,
} from "./bytes";
import type { ZipSections } from "./zip";
export const RSA = { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" } as const;
export interface Identity {
  privateKey: CryptoKey;
  certificate: Bytes;
  publicKey: Bytes;
}
function der(tag: number, ...parts: Bytes[]): Bytes {
  const data = concat(...parts);
  let n = data.length;
  const lengths: number[] = [];
  do {
    lengths.unshift(n & 255);
    n >>>= 8;
  } while (n);
  return concat(
    new Uint8Array([
      tag,
      ...(data.length < 128
        ? [data.length]
        : [0x80 | lengths.length, ...lengths]),
    ]),
    data,
  );
}
const seq = (...parts: Bytes[]) => der(0x30, ...parts);
const algorithm = () =>
  seq(
    der(6, new Uint8Array([0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 1, 1, 11])),
    der(5, new Uint8Array()),
  );
export async function createIdentity(): Promise<Identity> {
  const keys = await crypto.subtle.generateKey(
    { ...RSA, modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]) },
    true,
    ["sign", "verify"],
  );
  const publicKey = new Uint8Array(
    await crypto.subtle.exportKey("spki", keys.publicKey),
  );
  const serial = crypto.getRandomValues(new Uint8Array(16));
  serial[0] = (serial[0] & 0x7f) | 1;
  const name = seq(
    der(
      0x31,
      seq(
        der(6, new Uint8Array([0x55, 4, 3])),
        der(12, utf8("GBA Studio Web")),
      ),
    ),
  );
  const tbs = seq(
    der(0xa0, der(2, new Uint8Array([2]))),
    der(2, serial),
    algorithm(),
    name,
    seq(der(0x18, utf8("20200101000000Z")), der(0x18, utf8("21200101000000Z"))),
    name,
    publicKey,
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign(RSA, keys.privateKey, tbs),
  );
  return {
    privateKey: keys.privateKey,
    publicKey,
    certificate: seq(
      tbs,
      algorithm(),
      der(3, concat(new Uint8Array([0]), signature)),
    ),
  };
}

export async function signApk(
  zip: ZipSections,
  identity: Identity,
): Promise<Bytes> {
  // Android v2: hash each section independently in chunks no larger than 1 MiB.
  const hashes: Bytes[] = [];
  for (const section of [zip.files, zip.central, zip.end]) {
    for (let offset = 0; offset < section.length; offset += 1048576) {
      const chunk = section.subarray(offset, offset + 1048576);
      hashes.push(
        await sha256(concat(new Uint8Array([0xa5]), u32(chunk.length), chunk)),
      );
    }
  }
  const digest = await sha256(
    concat(new Uint8Array([0x5a]), u32(hashes.length), ...hashes),
  );
  const signedData = concat(
    lp(lp(concat(u32(0x0103), lp(digest)))),
    lp(lp(identity.certificate)),
    lp(new Uint8Array()),
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign(RSA, identity.privateKey, signedData),
  );
  const signer = concat(
    lp(signedData),
    lp(lp(concat(u32(0x0103), lp(signature)))),
    lp(identity.publicKey),
  );
  const v2 = lp(lp(signer));
  const pair = concat(u64(4 + v2.length), u32(0x7109871a), v2);
  const size = pair.length + 24;
  const block = concat(u64(size), pair, u64(size), utf8("APK Sig Block 42"));
  const end = zip.end.slice();
  new DataView(end.buffer).setUint32(16, zip.files.length + block.length, true);
  return concat(zip.files, block, zip.central, end);
}

export async function exportIdentity(identity: Identity): Promise<string> {
  return JSON.stringify(
    {
      format: "gba-studio-key",
      version: 1,
      privateKey: base64(
        new Uint8Array(
          await crypto.subtle.exportKey("pkcs8", identity.privateKey),
        ),
      ),
      certificate: base64(identity.certificate),
    },
    null,
    2,
  );
}

// Strict, bounded DER reader used to bind a restored certificate to its key.
function readDer(
  data: Bytes,
  pos = 0,
): { tag: number; start: number; end: number; raw: Bytes; value: Bytes } {
  const start = pos,
    tag = data[pos++];
  let length = data[pos++];
  if (length & 0x80) {
    const count = length & 0x7f;
    if (!count || count > 4) throw new Error("DER inválido");
    length = 0;
    for (let i = 0; i < count; i++) length = length * 256 + data[pos++];
  }
  const end = pos + length;
  if (!Number.isSafeInteger(end) || end > data.length || pos > data.length)
    throw new Error("DER truncado");
  return {
    tag,
    start,
    end,
    raw: data.slice(start, end),
    value: data.slice(pos, end),
  };
}
function children(data: Bytes): ReturnType<typeof readDer>[] {
  const result = [];
  for (let p = 0; p < data.length;) {
    const item = readDer(data, p);
    result.push(item);
    p = item.end;
  }
  return result;
}
export async function restoreIdentity(text: string): Promise<Identity> {
  try {
    if (text.length > 32768) throw new Error("Backup muito grande");
    const obj = JSON.parse(text);
    if (
      obj.format !== "gba-studio-key" ||
      obj.version !== 1 ||
      typeof obj.privateKey !== "string" ||
      typeof obj.certificate !== "string"
    )
      throw new Error("Formato inválido");
    const certificate = unbase64(obj.certificate),
      cert = readDer(certificate);
    if (cert.tag !== 0x30 || cert.end !== certificate.length)
      throw new Error("Certificado inválido");
    const certParts = children(cert.value),
      tbs = children(certParts[0].value);
    const publicKey = tbs[tbs[0].tag === 0xa0 ? 6 : 5].raw;
    const publicCrypto = await crypto.subtle.importKey(
      "spki",
      publicKey,
      RSA,
      true,
      ["verify"],
    );
    const privateKey = await crypto.subtle.importKey(
      "pkcs8",
      unbase64(obj.privateKey),
      RSA,
      true,
      ["sign"],
    );
    if ((privateKey.algorithm as RsaKeyAlgorithm).modulusLength < 2048)
      throw new Error("Chave fraca");
    const probe = utf8("GBA Studio backup validation");
    const proof = await crypto.subtle.sign(RSA, privateKey, probe);
    if (!(await crypto.subtle.verify(RSA, publicCrypto, proof, probe)))
      throw new Error("Chave e certificado diferentes");
    if (
      certParts[2].tag !== 3 ||
      certParts[2].value[0] !== 0 ||
      !(await crypto.subtle.verify(
        RSA,
        publicCrypto,
        certParts[2].value.slice(1),
        certParts[0].raw,
      ))
    )
      throw new Error("Certificado inválido");
    return { privateKey, publicKey, certificate };
  } catch {
    throw new Error(
      "Backup de chave inválido. Escolha o JSON exportado pelo GBA Studio.",
    );
  }
}
