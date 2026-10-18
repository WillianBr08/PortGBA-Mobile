#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
NDK="${ANDROID_NDK_HOME:-$SDK/ndk/27.2.12479018}"
if [ ! -d "$NDK" ] && [ -d /tmp/elite-toolchain/android-ndk-r27c ]; then NDK=/tmp/elite-toolchain/android-ndk-r27c; fi
JDK="${ELITE_JAVA_HOME:-${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}}"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-36/android.jar"
export JAVA_HOME="$JDK" PATH="$JDK/bin:$PATH"
test -f "$NDK/build/cmake/android.toolchain.cmake" || { echo 'Defina ANDROID_NDK_HOME para o NDK r27c ou posterior.'; exit 1; }
python3 tools/prepare_core.py
mkdir -p build/{classes,dex,res,apk/lib} dist .private
for ABI in ${ELITE_ABIS:-arm64-v8a armeabi-v7a x86_64}; do
    cmake -S . -B "build/native-$ABI" -G Ninja \
        -DMGBA_SOURCE_DIR="$PWD/build/mgba" \
        -DELITE_ENGINE_SOURCE="$PWD/android/app/src/main/cpp/engine.c" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release \
        -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON > "build/config-$ABI.log" 2>&1
    cmake --build "build/native-$ABI" --target eliteredux -j "${ELITE_JOBS:-8}" > "build/compile-$ABI.log" 2>&1
    mkdir -p "build/apk/lib/$ABI"
    cp "build/native-$ABI/libeliteredux.so" "build/apk/lib/$ABI/"
    "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "build/apk/lib/$ABI/libeliteredux.so"
done
rm -f build/res/resources.zip
"$BT/aapt2" compile --dir android/app/src/main/res -o build/res/resources.zip
"$BT/aapt2" link -I "$PLATFORM" --manifest android/app/src/main/AndroidManifest.xml \
    --java build/generated --auto-add-overlay -o build/base.apk build/res/resources.zip
rg --files android/app/src/main/java -g '*.java' > build/sources.txt
rm -rf build/classes
mkdir -p build/classes
"$JDK/bin/javac" -encoding UTF-8 --release 8 -classpath "$PLATFORM" -d build/classes @build/sources.txt
"$JDK/bin/jar" cf build/classes.jar -C build/classes .
"$BT/d8" --release --lib "$PLATFORM" --min-api 26 --output build/dex build/classes.jar
if [ "${1:-}" = --template-only ]; then
    echo 'Interface compilada. Use portar-gba.sh para empacotar sua ROM.'
    exit 0
fi
if [ ! -f .private/elite-redux.jks ]; then
    "$JDK/bin/keytool" -genkeypair -keystore .private/elite-redux.jks -storepass elite-redux-local \
        -keypass elite-redux-local -alias elite-redux -keyalg RSA -keysize 3072 -validity 36500 \
        -dname 'CN=Elite Redux Private, O=Private, C=BR'
    chmod 600 .private/elite-redux.jks
fi
python3 - <<'ROMCHECK'
from pathlib import Path
import json
root=Path('android/app/src/main/assets')
if not (root/'game.json').is_file():raise SystemExit('Use ./portar-gba.sh /caminho/Jogo.gba para criar um APK com sua ROM.')
spec=json.loads((root/'game.json').read_text())
if not (root/'game'/(spec['base']+'.gba')).is_file():raise SystemExit('ROM ausente. Use ./portar-gba.sh /caminho/Jogo.gba')
ROMCHECK
cp build/dex/classes.dex build/apk/
rm -rf build/apk/assets
mkdir -p build/apk/assets
cp -a android/app/src/main/assets/. build/apk/assets/
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_STORED, ZIP_DEFLATED
root = Path('build/apk')
with ZipFile('build/unsigned.apk', 'a') as apk:
    for path in sorted(root.rglob('*')):
        if path.is_file():
            apk.write(path, path.relative_to(root).as_posix(), compress_type=ZIP_STORED if path.suffix == '.so' else ZIP_DEFLATED)
PY
"$BT/zipalign" -f -P 16 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks .private/elite-redux.jks --ks-pass pass:elite-redux-local \
    --key-pass pass:elite-redux-local --out dist/Elite-Redux-Mobile.apk build/aligned.apk
"$BT/apksigner" verify --verbose dist/Elite-Redux-Mobile.apk
sha256sum dist/Elite-Redux-Mobile.apk > dist/Elite-Redux-Mobile.apk.sha256
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
Path('dist/LEIA-ME.txt').write_text(
    'Instale Elite-Redux-Mobile.apk no Android 8 ou posterior.\n'
    'O APK inclui a ROM e não inclui saves pessoais. Atualizar mantém seus saves no celular.\n'
    'Ao abrir, o jogo inicia diretamente usando o progresso da pasta.\n'
    'SALVAR no HUD grava o automático; CARREGAR restaura o automático sem perguntar.\n'
    'Menu permite exportar .sav, importar estados e personalizar cada botão do HUD.\n'
    'Os saves ficam em Android/data/dev.privado.eliteredux/files/Elite Redux/.\n'
    'Exporte os saves antes de desinstalar o aplicativo.\n'
    'Menu tem as abas Jogo, Saves e Ajustes. Paisagem está na aba Jogo.\n', encoding='utf-8')
PY
echo 'APK pronto: dist/Elite-Redux-Mobile.apk'
