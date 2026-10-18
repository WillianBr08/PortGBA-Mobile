#!/usr/bin/env python3
"""Gera o pacote compartilhável: fonte e gerador, sem ROMs, saves ou chave local."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import argparse,hashlib
ROOT=Path(__file__).resolve().parents[1]
DENIED={'.gba','.gb','.gbc','.sav','.jks','.keystore','.apk','.zip'}
def allowed(p):
 parts=p.relative_to(ROOT).parts
 return not ('cinema' in parts and parts[:2]==('vendor','mgba')) and not any(x in parts for x in ['.git','.private','build','dist','captures','__pycache__','node_modules','site','output','.playwright-cli']) and p.suffix.lower() not in DENIED and not p.suffix.lower().startswith('.ss') and not p.name.endswith('.working')
def package(destination):
 destination=Path(destination).expanduser().resolve();destination.parent.mkdir(parents=True,exist_ok=True)
 candidates=[ROOT/n for n in ['CMakeLists.txt','build.sh','portar-gba.sh','.gitignore','app/src/main/AndroidManifest.xml']]
 candidates.extend(ROOT/'web'/n for n in ['index.html','package.json','package-lock.json','tsconfig.json','vite.config.ts'])
 candidates.append(ROOT/'web/public/icon.svg')
 candidates.extend(ROOT/n for n in ['PRODUCT.md','DESIGN.md','docs/validacao-web.md'] if (ROOT/n).exists())
 for name in ['app/src/main/java','app/src/main/cpp','app/src/main/res','app/src/main/assets/fonts','app/src/main/assets/licenses','tools','vendor/mgba','web/src','web/tests','.github','.impeccable']:
  candidates.extend(p for p in (ROOT/name).rglob('*') if p.is_file())
 candidates.extend(p for p in (ROOT/'docs/compatibility').iterdir() if p.suffix in ['.S','.ld','.bin'])
 with ZipFile(destination,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
  z.write(ROOT/'README.md','GBA-Studio/README.md')
  z.write(ROOT/'README-COMPARTILHAR.md','GBA-Studio/README-COMPARTILHAR.md')
  for p in sorted(set(candidates)):
   if allowed(p):z.write(p,'GBA-Studio/'+p.relative_to(ROOT).as_posix())
 with ZipFile(destination) as z:
  for n in z.namelist():
   p=Path(n);assert p.suffix.lower() not in DENIED and '.private' not in p.parts and '.git' not in p.parts,n
 destination.with_suffix(destination.suffix+'.sha256').write_text(hashlib.sha256(destination.read_bytes()).hexdigest()+'  '+destination.name+'\n')
 print(destination)
if __name__=='__main__':
 a=argparse.ArgumentParser(description=__doc__);a.add_argument('--output',default=ROOT/'dist/GBA-Studio-Projeto.zip');package(a.parse_args().output)
