import { concat, utf8, type Bytes } from "./bytes";
const table = Uint32Array.from({ length: 256 }, (_, i) => {
  let x = i;
  for (let j = 0; j < 8; j++) x = (x >>> 1) ^ (x & 1 ? 0xedb88320 : 0);
  return x >>> 0;
});
export function crc32(data: Uint8Array): number {
  let x = 0xffffffff;
  for (const b of data) x = table[(x ^ b) & 255] ^ (x >>> 8);
  return (x ^ 0xffffffff) >>> 0;
}
export interface ZipSections {
  files: Bytes;
  central: Bytes;
  end: Bytes;
}

/** ZIP_STORED permits streaming assets; align every entry and mmap native code. */
export function alignedZip(entries: Record<string, Bytes>): ZipSections {
  const locals: Bytes[] = [],
    directory: Bytes[] = [];
  let offset = 0;
  for (const [name, data] of Object.entries(entries).sort(([a], [b]) =>
    a < b ? -1 : 1,
  )) {
    if (!name || name.startsWith("/") || name.split("/").includes(".."))
      throw new Error("Caminho inválido no APK.");
    const filename = utf8(name),
      alignment = name.endsWith(".so") ? 16384 : 4;
    let padding =
      (alignment - ((offset + 30 + filename.length) % alignment)) % alignment;
    if (padding && padding < 4) padding += alignment;
    const header = new Uint8Array(30 + filename.length + padding),
      v = new DataView(header.buffer);
    v.setUint32(0, 0x04034b50, true);
    v.setUint16(4, 20, true);
    v.setUint16(6, 0x800, true);
    v.setUint16(12, 33, true); // 1980-01-01, reproducible ZIP timestamps
    const crc = crc32(data);
    v.setUint32(14, crc, true);
    v.setUint32(18, data.length, true);
    v.setUint32(22, data.length, true);
    v.setUint16(26, filename.length, true);
    v.setUint16(28, padding, true);
    header.set(filename, 30);
    if (padding) {
      v.setUint16(30 + filename.length, 0xd935, true);
      v.setUint16(32 + filename.length, padding - 4, true);
    }
    const central = new Uint8Array(46 + filename.length),
      cv = new DataView(central.buffer);
    cv.setUint32(0, 0x02014b50, true);
    cv.setUint16(4, 20, true);
    cv.setUint16(6, 20, true);
    cv.setUint16(8, 0x800, true);
    cv.setUint16(14, 33, true);
    cv.setUint32(16, crc, true);
    cv.setUint32(20, data.length, true);
    cv.setUint32(24, data.length, true);
    cv.setUint16(28, filename.length, true);
    cv.setUint32(42, offset, true);
    central.set(filename, 46);
    locals.push(header, data);
    directory.push(central);
    offset += header.length + data.length;
  }
  const central = concat(...directory),
    end = new Uint8Array(22),
    ev = new DataView(end.buffer),
    count = directory.length;
  ev.setUint32(0, 0x06054b50, true);
  ev.setUint16(8, count, true);
  ev.setUint16(10, count, true);
  ev.setUint32(12, central.length, true);
  ev.setUint32(16, offset, true);
  return { files: concat(...locals), central, end };
}
