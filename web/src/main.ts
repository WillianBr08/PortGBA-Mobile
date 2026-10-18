import { backup, getIdentity, restoreBackup, fingerprint } from './identity';
import { gameTitle, type Logo } from './rom';
const el = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const rom = el<HTMLInputElement>('rom'), title = el<HTMLInputElement>('title'), logo = el<HTMLSelectElement>('logo');
const options = el<HTMLFieldSetElement>('options'), generate = el<HTMLButtonElement>('generate');
const panel = el('status-panel'), status = el('status'), error = el('error'), progress = el<HTMLProgressElement>('progress');
const download = el<HTMLAnchorElement>('download');
let selected: File | undefined, objectUrl: string | undefined, worker: Worker | undefined, busy = false;
function discardResult() {
  if (objectUrl) { URL.revokeObjectURL(objectUrl); objectUrl = undefined; }
  download.hidden = true; download.removeAttribute('href'); el('install-note').hidden = true;
}
function showError(message: string) { panel.hidden = false; status.textContent = ''; error.textContent = message; error.hidden = false; progress.hidden = true; }
function selectFile(file?: File) {
  discardResult(); error.hidden = true; panel.hidden = true; selected = undefined; generate.disabled = true;
  if (!file) return;
  if (!/\.gba$/i.test(file.name) || file.size < 192 || file.size > 32 * 1024 * 1024) { showError('Escolha uma ROM .gba descompactada, entre 192 bytes e 32 MiB.'); return; }
  selected = file; title.value = file.name.replace(/\.gba$/i, '').slice(0, 100);
  el('rom-name').textContent = file.name; el('rom-detail').textContent = (file.size / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 2 }) + ' MiB · clique para trocar';
  generate.disabled = false;
}
rom.addEventListener('change', () => selectFile(rom.files?.[0]));
const drop = el('drop-zone');
drop.addEventListener('dragover', e => { e.preventDefault(); if (!busy) drop.classList.add('dragging'); });
drop.addEventListener('dragleave', () => drop.classList.remove('dragging'));
drop.addEventListener('drop', e => { e.preventDefault(); drop.classList.remove('dragging'); if (!busy) selectFile(e.dataTransfer?.files[0]); });
for (const input of [title, logo]) input.addEventListener('input', () => { discardResult(); panel.hidden = true; });
function unlock() { busy = false; options.disabled = false; generate.disabled = !selected; el<HTMLButtonElement>('export-key').disabled = false; el<HTMLInputElement>('restore-key').disabled = false; worker?.terminate(); worker = undefined; }
el<HTMLFormElement>('generator').addEventListener('submit', async event => {
  event.preventDefault(); if (busy || !selected) return;
  busy = true; discardResult(); error.hidden = true; options.disabled = true;
  el<HTMLButtonElement>('export-key').disabled = true; el<HTMLInputElement>('restore-key').disabled = true;
  panel.hidden = false; progress.hidden = false; progress.value = 0; status.textContent = 'Preparando sua chave local…';
  try {
    if (!window.isSecureContext || !crypto.subtle || !window.Worker || !window.indexedDB) throw new Error('Use um navegador atualizado em HTTPS para gerar o APK.');
    const filename = selected.name, gameName = gameTitle(title.value, filename), chosenLogo = logo.value as Logo;
    const identity = await getIdentity(); el('key-label').textContent = await fingerprint(identity);
    const data = new Uint8Array(await selected.arrayBuffer());
    worker = new Worker(new URL('./worker.ts', import.meta.url), { type: 'module' });
    worker.onerror = () => { showError('O processamento foi interrompido. Feche outras abas para liberar memória e tente novamente.'); unlock(); };
    worker.onmessage = event => {
      const result = event.data;
      if (result.type === 'progress') { progress.value = result.value; status.textContent = result.message; }
      if (result.type === 'error') { showError(result.message); unlock(); }
      if (result.type === 'done') {
        objectUrl = URL.createObjectURL(new Blob([result.apk], { type: 'application/vnd.android.package-archive' }));
        download.href = objectUrl; download.download = result.filename; download.hidden = false;
        el('install-note').hidden = false; progress.hidden = true; status.textContent = 'Pronto. Seu jogo já pode ir para o Android.'; unlock(); download.focus();
      }
    };
    worker.postMessage({ rom: data, filename, title: gameName, logo: chosenLogo, identity, baseUrl: new URL(import.meta.env.BASE_URL, document.baseURI).href }, [data.buffer]);
  } catch (e) { showError(e instanceof Error ? e.message : 'Não foi possível gerar o APK. Tente novamente.'); unlock(); }
});
function saveFile(content: string, name: string) {
  const url = URL.createObjectURL(new Blob([content], { type: 'application/json' }));
  const link = document.createElement('a'); link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 30000);
}
el('export-key').addEventListener('click', async () => {
  const message = el('key-status');
  try { saveFile(await backup(), 'gba-studio-chave.json'); el('key-label').textContent = await fingerprint(await getIdentity()); message.textContent = 'Backup exportado. Guarde sua chave em um lugar seguro.'; }
  catch (e) { message.textContent = e instanceof Error ? e.message : 'Não foi possível exportar a chave.'; }
});
el<HTMLInputElement>('restore-key').addEventListener('change', async event => {
  const input = event.target as HTMLInputElement, file = input.files?.[0]; if (!file || busy) return;
  const message = el('key-status');
  try {
    if (file.size > 32768) throw new Error('Backup inválido. Escolha o JSON exportado pelo GBA Studio.');
    const identity = await restoreBackup(await file.text()); el('key-label').textContent = await fingerprint(identity);
    discardResult(); panel.hidden = true; message.textContent = 'Chave restaurada. Os próximos APKs usarão esta chave.';
  } catch (e) { message.textContent = e instanceof Error ? e.message : 'Não foi possível restaurar a chave.'; }
  finally { input.value = ''; }
});
window.addEventListener('pagehide', () => { worker?.terminate(); discardResult(); });
