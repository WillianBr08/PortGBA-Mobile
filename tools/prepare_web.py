#!/usr/bin/env python3
"""Prepare only allowlisted, ROM-free browser templates. Never reads .private/."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from zipfile import ZipFile, ZIP_DEFLATED, ZIP_STORED

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
PACKAGE = 'dev.gba.studio.web.template'
TITLE = 'GBA_STUDIO_WEB_TITLE'


def prepare():
    sdk = Path(os.environ.get('ANDROID_SDK_ROOT', os.environ.get('ANDROID_HOME', str(Path.home() / 'Android/Sdk'))))
    aapt = sdk / 'build-tools/35.0.0/aapt2'
    platform = sdk / 'platforms/android-36/android.jar'
    output = ROOT / 'web/public/templates'
    output.mkdir(parents=True, exist_ok=True)
    fonts = ROOT / 'web/public/fonts'
    fonts.mkdir(parents=True, exist_ok=True)
    for p in (ROOT / 'app/src/main/assets/fonts').iterdir():
        if p.is_file():
            shutil.copy2(p, fonts / p.name)
    for p in (ROOT / 'app/src/main/assets/licenses').glob('*OFL.txt'):
        shutil.copy2(p, fonts / p.name)
    libraries = [ROOT / 'build/apk/lib' / abi / 'libeliteredux.so' for abi in ('arm64-v8a', 'armeabi-v7a', 'x86_64')]
    for p in libraries + [ROOT / 'build/dex/classes.dex', ROOT / 'build/res/resources.zip']:
        if not p.is_file():
            raise SystemExit(f'Compile ./build.sh --template-only primeiro: {p.relative_to(ROOT)} ausente')
    catalog = {'version': 1, 'package': PACKAGE, 'title': TITLE, 'templates': {}}
    with tempfile.TemporaryDirectory(prefix='gba-web-') as tmp:
        for logo in ('emerald', 'firered', 'gba'):
            manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
            manifest.getroot().set('package', PACKAGE)
            app = manifest.getroot().find('application')
            app.set(ANDROID + 'label', TITLE)
            app.set(ANDROID + 'icon', '@drawable/icon_' + logo)
            source = Path(tmp) / 'AndroidManifest.xml'
            manifest.write(source, encoding='utf-8', xml_declaration=True)
            target = output / (logo + '.apk')
            subprocess.run([str(aapt), 'link', '-I', str(platform), '--manifest', str(source), '--auto-add-overlay', '-o', str(target), str(ROOT / 'build/res/resources.zip')], check=True)
            with ZipFile(target, 'a') as z:
                z.write(ROOT / 'build/dex/classes.dex', 'classes.dex', compress_type=ZIP_DEFLATED)
                for p in libraries:
                    z.write(p, 'lib/' + p.parent.name + '/' + p.name, compress_type=ZIP_STORED)
                for folder in ('fonts', 'licenses'):
                    for p in sorted((ROOT / 'app/src/main/assets' / folder).rglob('*')):
                        if p.is_file():
                            z.write(p, 'assets/' + p.relative_to(ROOT / 'app/src/main/assets').as_posix(), compress_type=ZIP_DEFLATED)
            # Fail closed: no source game directory, game.json, saves or signing keys.
            with ZipFile(target) as z:
                for name in z.namelist():
                    allowed = name in ('AndroidManifest.xml', 'resources.arsc', 'classes.dex') or name.startswith(('res/', 'lib/', 'assets/fonts/', 'assets/licenses/'))
                    if not allowed or Path(name).suffix.lower() in ('.gba', '.sav', '.jks', '.keystore'):
                        raise RuntimeError(f'Arquivo não permitido no template: {name}')
            catalog['templates'][logo] = {'file': target.name, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest(), 'size': target.stat().st_size}
    (output / 'catalog.json').write_text(json.dumps(catalog, indent=2) + '\n')
    print('Templates sem ROM preparados em web/public/templates/')


if __name__ == '__main__':
    prepare()
