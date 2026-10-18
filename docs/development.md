# Desenvolvimento

## Preparar bases e site

Requer Linux, Git, Python 3, JDK 17, Node.js 22+, CMake 3.22+, Ninja e ripgrep. Instale o Android SDK com plataforma 36, Build Tools 35.0.0 e NDK 27.2.12479018.

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
# Se necessário: export ELITE_JAVA_HOME=/caminho/do/jdk-17
./build.sh --template-only
python3 tools/prepare_web.py
npm --prefix web ci
npm --prefix web test
npm --prefix web run build
python3 tools/audit_web.py web/site
npm --prefix web run preview
```

`build.sh` obtém o núcleo oficial por commit fixado e aplica somente o patch versionado em `vendor/patches/`. Não é necessário copiar nada de uma pasta de jogos. O build pode acessar a internet para obter dependências. O APK base não contém ROM nem chave privada.

Desenvolvimento: `npm --prefix web run dev`. Use localhost ou HTTPS, com Web Crypto, IndexedDB e Workers. Não abra `index.html` por `file://`. A saída estática é `web/site/`; publique somente essa pasta. O desktop incorpora exatamente essa saída.

## Desktop

Depois de preparar o site:

```bash
npm --prefix desktop ci
npm --prefix desktop test
npm --prefix desktop start
npm --prefix desktop run dist:linux
# Em Windows, com Node.js 22+:
# npm --prefix desktop run dist:windows
```

Pacotes em `dist/desktop/`. O teste de execução é `npm --prefix desktop run test:app`; no Linux sem tela, prefixe com `xvfb-run -a`. Ele usa uma ROM sintética e um perfil temporário, verifica geração offline e persistência da chave. `GBA_STUDIO_EXECUTABLE` permite testar o executável empacotado.

## Android e empacotador

```bash
cd web
npx tsx tests/build-smoke.ts
cd ..
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify --verbose build/web-smoke.apk
"$ANDROID_SDK_ROOT/build-tools/35.0.0/zipalign" -c -P 16 4 build/web-smoke.apk
javac -d build/input-test android/app/src/main/java/dev/privado/eliteredux/InputState.java tests/android/InputStateTest.java
java -cp build/input-test dev.privado.eliteredux.InputStateTest
```

O teste smoke também produz um APK adulterado, que deve falhar na verificação de assinatura. Bibliotecas nativas são armazenadas sem compressão e alinhadas a 16 KiB; a assinatura usa APK v2.

## Publicação

`pages.yml` testa e publica após push na branch principal, quando `GBA_STUDIO_PUBLISH=true`. Pull requests validam sem publicar. `release.yml` prepara as bases e testa os runtimes Linux/Windows em seus sistemas nativos antes de publicar os artefatos de uma tag `v*`. Execução manual compila sem criar release.

Nunca versione `.private/`, ROMs, saves, chaves, `node_modules/`, `build/` ou `dist/`. Para exportar fontes: `python3 tools/empacotar_projeto.py` usa os arquivos rastreados pelo Git. Consulte `docs/plan.md` antes de retomar uma entrega. Para rollback, reverta o commit na branch principal e publique novamente; uma versão anterior permanece no histórico de releases.
