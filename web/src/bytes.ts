export type Bytes = Uint8Array<ArrayBuffer>;
export const utf8 = (s: string): Bytes => new TextEncoder().encode(s);
export function concat(...parts: Uint8Array[]): Bytes {
  const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0));
  let offset = 0;
  for (const part of parts) {
    out.set(part, offset);
    offset += part.length;
  }
  return out;
}
export function u32(n: number): Bytes {
  const b = new Uint8Array(4);
  new DataView(b.buffer).setUint32(0, n, true);
  return b;
}
export function u64(n: number): Bytes {
  return concat(u32(n), u32(Math.floor(n / 2 ** 32)));
}
export const lp = (b: Uint8Array): Bytes => concat(u32(b.length), b);
export const hex = (b: Uint8Array): string =>
  Array.from(b, (x) => x.toString(16).padStart(2, "0")).join("");
export const sha256 = async (b: Bytes): Promise<Bytes> =>
  new Uint8Array(await crypto.subtle.digest("SHA-256", b));
export function base64(b: Uint8Array): string {
  let value = "";
  for (let i = 0; i < b.length; i += 8192)
    value += String.fromCharCode(...b.subarray(i, i + 8192));
  return btoa(value);
}
export function unbase64(s: string): Bytes {
  return Uint8Array.from(atob(s), (c) => c.charCodeAt(0));
}
