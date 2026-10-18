#!/usr/bin/env python3
"""Export only versioned project sources; game inputs and build output stay private."""
import argparse
import hashlib
from pathlib import Path
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
DENIED = {'.gba', '.gb', '.gbc', '.sav', '.jks', '.keystore', '.apk', '.pk8', '.exe', '.appimage'}


def package(destination):
    destination = Path(destination).expanduser().resolve()
    destination.parent.mkdir(parents=True, exist_ok=True)
    files = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).split(b'\0')
    with ZipFile(destination, 'w', compression=ZIP_DEFLATED) as archive:
        for raw in files:
            if not raw:
                continue
            relative = Path(raw.decode())
            source = ROOT / relative
            if source.is_file() and relative.suffix.lower() not in DENIED and not relative.name.endswith('.working'):
                archive.write(source, 'GBA-Studio/' + relative.as_posix())
    destination.with_suffix(destination.suffix + '.sha256').write_text(hashlib.sha256(destination.read_bytes()).hexdigest() + '  ' + destination.name + '\n')
    print(destination)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', default=ROOT / 'dist/GBA-Studio-Projeto.zip')
    package(parser.parse_args().output)
