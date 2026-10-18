import { concat, utf8, type Bytes } from "./bytes";

/** Rebuild the Android binary-XML string pool, retaining every string index. */
export function patchManifest(
  input: Bytes,
  replacements: Record<string, string>,
): Bytes {
  const view = new DataView(input.buffer, input.byteOffset, input.byteLength);
  if (view.getUint16(0, true) !== 3 || view.getUint32(4, true) !== input.length)
    throw new Error("Manifesto Android inválido.");
  const seen = new Set<string>();
  let result = input;
  for (let offset = view.getUint16(2, true); offset < input.length;) {
    const type = view.getUint16(offset, true),
      header = view.getUint16(offset + 2, true),
      size = view.getUint32(offset + 4, true);
    if (size < header || offset + size > input.length)
      throw new Error("Manifesto Android truncado.");
    if (type === 1) {
      const count = view.getUint32(offset + 8, true),
        styles = view.getUint32(offset + 12, true);
      const flags = view.getUint32(offset + 16, true),
        start = view.getUint32(offset + 20, true),
        styleStart = view.getUint32(offset + 24, true);
      const isUtf8 = !!(flags & 0x100);
      const chunks: Bytes[] = [],
        offsets: number[] = [];
      let total = 0;
      for (let i = 0; i < count; i++) {
        let pos =
          offset + start + view.getUint32(offset + header + i * 4, true);
        function length(): number {
          if (isUtf8) {
            const a = input[pos++];
            return a & 0x80 ? ((a & 0x7f) << 8) | input[pos++] : a;
          }
          const a = view.getUint16(pos, true);
          pos += 2;
          if (!(a & 0x8000)) return a;
          const b = view.getUint16(pos, true);
          pos += 2;
          return (a & 0x7fff) * 65536 + b;
        }
        const chars = length();
        const bytes = isUtf8 ? length() : chars * 2;
        let text = new TextDecoder(isUtf8 ? "utf-8" : "utf-16le").decode(
          input.subarray(pos, pos + bytes),
        );
        if (Object.hasOwn(replacements, text)) {
          seen.add(text);
          text = replacements[text];
        }
        const data = isUtf8 ? utf8(text) : new Uint8Array(text.length * 2);
        if (!isUtf8) {
          const dv = new DataView(data.buffer);
          for (let j = 0; j < text.length; j++)
            dv.setUint16(j * 2, text.charCodeAt(j), true);
        }
        function encodeLength(n: number): Bytes {
          if (isUtf8)
            return n < 128
              ? new Uint8Array([n])
              : new Uint8Array([(n >> 8) | 0x80, n & 255]);
          const b = new Uint8Array(n < 32768 ? 2 : 4),
            v = new DataView(b.buffer);
          if (b.length === 2) v.setUint16(0, n, true);
          else {
            v.setUint16(0, (n >>> 16) | 0x8000, true);
            v.setUint16(2, n & 65535, true);
          }
          return b;
        }
        const encoded = concat(
          encodeLength(text.length),
          ...(isUtf8 ? [encodeLength(data.length)] : []),
          data,
          new Uint8Array(isUtf8 ? 1 : 2),
        );
        offsets.push(total);
        chunks.push(encoded);
        total += encoded.length;
      }
      const stringData = concat(
        ...chunks,
        new Uint8Array((4 - (total % 4)) % 4),
      );
      const prefix = input.slice(offset, offset + start),
        pv = new DataView(prefix.buffer);
      // Replacement can change lexical order, so clear the sorted-pool flag.
      pv.setUint32(16, flags & ~1, true);
      offsets.forEach((n, i) => pv.setUint32(header + i * 4, n, true));
      const styleData = styleStart
        ? input.slice(offset + styleStart, offset + size)
        : new Uint8Array();
      if (styles) pv.setUint32(24, start + stringData.length, true);
      const pool = concat(prefix, stringData, styleData);
      new DataView(pool.buffer).setUint32(4, pool.length, true);
      result = concat(
        input.subarray(0, offset),
        pool,
        input.subarray(offset + size),
      );
      new DataView(result.buffer).setUint32(4, result.length, true);
      break;
    }
    offset += size;
  }
  if (Object.keys(replacements).some((k) => !seen.has(k)))
    throw new Error(
      "Template incompatível. Atualize a página e tente novamente.",
    );
  return result;
}

/** The package header in resources.arsc stores a fixed-width UTF-16 name. */
export function patchResourcePackage(input: Bytes, packageName: string): Bytes {
  const output = input.slice(),
    v = new DataView(output.buffer);
  if (v.getUint16(0, true) !== 2 || packageName.length > 127)
    throw new Error("Tabela de recursos inválida.");
  let found = false;
  for (let pos = v.getUint16(2, true); pos < output.length;) {
    const size = v.getUint32(pos + 4, true);
    if (size < 8 || pos + size > output.length)
      throw new Error("Tabela de recursos truncada.");
    if (v.getUint16(pos, true) === 0x200) {
      output.fill(0, pos + 12, pos + 268);
      for (let i = 0; i < packageName.length; i++)
        v.setUint16(pos + 12 + i * 2, packageName.charCodeAt(i), true);
      found = true;
    }
    pos += size;
  }
  if (!found) throw new Error("Pacote de recursos ausente.");
  return output;
}
