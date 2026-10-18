import { test } from 'node:test';
import assert from 'node:assert/strict';
import { inspectRom, gameTitle, apkFilename } from '../src/rom';
import { alignedZip, crc32 } from '../src/zip';
import { createIdentity, exportIdentity, restoreIdentity, signApk } from '../src/signing';
import { unzipSync } from 'fflate';
import { utf8 } from '../src/bytes';

test('rejects wrong extension, size and header before packaging', async () => {
  const rom = new Uint8Array(192); rom[0xb2] = 0x96;
  await assert.rejects(inspectRom(rom, 'game.zip'));
  await assert.rejects(inspectRom(new Uint8Array(191), 'game.gba'));
  await assert.rejects(inspectRom(new Uint8Array(192), 'game.gba'));
  await assert.rejects(inspectRom(new Uint8Array(32 * 1024 * 1024 + 1), 'game.gba'));
  const result = await inspectRom(rom, 'game.GBA'); assert.equal(result.logo, 'gba');
  rom.set(utf8('BPEE'), 0xac); assert.equal((await inspectRom(rom, 'game.gba')).logo, 'emerald');
  rom.set(utf8('BPRE'), 0xac); assert.equal((await inspectRom(rom, 'game.gba')).logo, 'firered');
});
test('title handles accents, Unicode, defaults and invalid names', () => {
  assert.equal(gameTitle('  Poke\u0301mon — Ação  ', 'x.gba'), 'Pokémon — Ação');
  assert.equal(gameTitle('', 'Quetzal.GBA'), 'Quetzal');
  assert.equal(apkFilename('Pokémon — Ação'), 'Pokemon-Acao.apk');
  assert.throws(() => gameTitle('a'.repeat(101), 'x.gba'));
  assert.throws(() => gameTitle('a\u0000b', 'x.gba'));
});
test('ZIP retains bytes with every native entry at 16 KiB and other entries at 4 bytes', () => {
  const entries = { 'a': utf8('hello'), 'lib/arm64-v8a/test.so': new Uint8Array(17000), 'assets/ação.txt': utf8('ação') };
  const zip = alignedZip(entries); const view = new DataView(zip.files.buffer);
  let offset = 0;
  while (offset < zip.files.length) {
    const nameLength = view.getUint16(offset + 26, true), extra = view.getUint16(offset + 28, true);
    const name = new TextDecoder().decode(zip.files.subarray(offset + 30, offset + 30 + nameLength));
    const start = offset + 30 + nameLength + extra;
    assert.equal(start % (name.endsWith('.so') ? 16384 : 4), 0);
    offset = start + view.getUint32(offset + 18, true);
  }
  assert.equal(crc32(utf8('123456789')), 0xcbf43926);
  assert.throws(() => alignedZip({ '../escape': new Uint8Array() }));
});
test('backup round trip preserves APK signing identity and rejects mismatched/corrupt keys', async () => {
  const a = await createIdentity(), b = await createIdentity();
  const restored = await restoreIdentity(await exportIdentity(a));
  assert.deepEqual(restored.certificate, a.certificate);
  const zip = alignedZip({ 'a.txt': utf8('local') });
  const first = await signApk(zip, a), second = await signApk(zip, restored);
  assert.deepEqual(first, second); assert.deepEqual(unzipSync(first)['a.txt'], utf8('local'));
  const corrupt = JSON.parse(await exportIdentity(a)), other = JSON.parse(await exportIdentity(b));
  corrupt.privateKey = other.privateKey;
  await assert.rejects(restoreIdentity(JSON.stringify(corrupt)));
  await assert.rejects(restoreIdentity('{}')); await assert.rejects(restoreIdentity('a'.repeat(32769)));
});
