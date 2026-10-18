#!/usr/bin/env python3
"""Fail publication if an artifact contains unexpected or private files."""
from pathlib import Path
import sys
from zipfile import ZipFile

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'web/site')
expected = {'index.html', 'icon.svg', 'templates/catalog.json', 'templates/emerald.apk', 'templates/firered.apk', 'templates/gba.apk'}
for path in root.rglob('*'):
    if not path.is_file():
        continue
    name = path.relative_to(root).as_posix()
    allowed = name in expected or (name.startswith('assets/') and path.suffix in ('.js', '.css')) or (name.startswith('fonts/') and path.suffix in ('.ttf', '.txt'))
    if not allowed:
        raise SystemExit('Arquivo não permitido na publicação: ' + name)
    if path.suffix == '.apk':
        with ZipFile(path) as apk:
            for entry in apk.namelist():
                if entry in ('assets/game.json',) or entry.startswith(('assets/game/', 'META-INF/')) or Path(entry).suffix.lower() in ('.gba', '.sav', '.jks', '.keystore'):
                    raise SystemExit('Dados privados no template: ' + entry)
                if not (entry in ('AndroidManifest.xml', 'resources.arsc', 'classes.dex') or entry.startswith(('res/', 'lib/', 'assets/fonts/', 'assets/licenses/'))):
                    raise SystemExit('Arquivo inesperado no template: ' + entry)
missing = expected - {p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file()}
if missing:
    raise SystemExit('Publicação incompleta: ' + ', '.join(sorted(missing)))
print('PASS: publicação contém somente site, fontes e templates sem ROM/chave/saves.')
