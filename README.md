# GBA Studio

Crie um aplicativo Android com sua ROM Game Boy Advance, a interface deste projeto e o núcleo mGBA 0.10.5. O projeto é independente da pasta de gameplay do Elite Redux. Os arquivos de jogo e saves continuam fora do código compartilhável.

Há dois geradores no mesmo projeto:

- **Web:** selecionar ROM → gerar → baixar APK, sem conta. A ROM e a chave ficam no navegador. O site baixa apenas bases Android estáticas; não existe endpoint de upload.
- **Linux:** `./portar-gba.sh` abre a janela. `./portar-gba.sh /caminho/Jogo.gba --title 'Meu jogo' --logo auto` gera pela linha de comando. O APK fica em `ports/` por padrão.

Os aplicativos usam Android 8 ou posterior, com ARM64, ARMv7 e x86_64. Compatibilidade e desempenho dependem do jogo e do aparelho.

## Site local

Dependências de preparação: Linux, JDK 17, Python 3, Node.js 22+, CMake 3.22+, Ninja e ripgrep. Android SDK com plataforma 36, Build Tools 35.0.0 e NDK 27.2.12479018. O núcleo já está em `vendor/mgba`.

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
export ELITE_JAVA_HOME=/caminho/do/jdk-17
./build.sh --template-only
python3 tools/prepare_web.py
cd web
npm ci
npm test
npm run build
npm run preview
```

Abra o endereço local mostrado no terminal. Para desenvolver use `npm run dev`. O navegador precisa de HTTPS, ou localhost, Web Crypto, IndexedDB e Web Workers. Não abra o HTML por `file://`. SDK, Java e NDK são usados só na preparação; quem visita o site não precisa deles.

O site final fica em `web/site/`. Os templates gerados em `web/public/templates/` não têm ROM, saves nem assinatura. A publicação inclui apenas HTML, CSS, JavaScript, fontes, licenças das fontes e essas bases. `python3 tools/audit_web.py web/site` confere os arquivos publicados. Não publique a pasta do projeto inteira.

## GitHub Pages

O workflow `.github/workflows/pages.yml` compila as três arquiteturas, prepara as bases sem ROM, testa a montagem e assinatura, verifica o APK com `apksigner` e `zipalign`, audita o site e cria um artefato Pages. Ele funciona em `main` e `master` e pode ser executado manualmente.

Repositório de destino: [WillianBr08/PortGBA-Mobile](https://github.com/WillianBr08/PortGBA-Mobile). Site publicado: [abrir GBA Studio](https://willianbr08.github.io/PortGBA-Mobile/).

Para configurar a publicação:

1. Envie este projeto, respeitando `.gitignore`. Não envie `.private/`, `build/`, `ports/`, `dist/`, ROMs ou saves.
2. Em **Settings → Pages**, selecione **GitHub Actions** como origem.
3. Crie a variável de repositório **GBA_STUDIO_PUBLISH** com valor **true** em **Settings → Secrets and variables → Actions → Variables**.
4. Execute o workflow. A URL será exibida no ambiente `github-pages`.

A publicação fica desativada até essa variável ser configurada. Não é necessário cadastrar uma chave de assinatura ou secret. URLs de assets são relativas para suportar páginas de projeto como `/GBA-Studio/`.

## Chave e atualizações

A primeira geração web cria uma chave RSA local e a guarda no IndexedDB do navegador, associada à origem do site. Em **Sua chave de atualização**, exporte o JSON de backup e guarde-o em local seguro. Ele contém a chave privada sem senha. É possível restaurá-lo em outro navegador. Limpar os dados do site, usar outro perfil ou mudar o endereço do site pode exigir restaurar o backup. O modo privado pode descartar a chave ao fechar.

A mesma ROM recebe o mesmo identificador `dev.gba.studio.web.g<sha256 parcial>`. A mesma chave permite reinstalar/atualizar por cima, inclusive após mudar nome ou logo. Uma ROM modificada tem outro identificador. Outra chave para o mesmo identificador impede atualização por cima: restaure a chave original. Exporte os saves antes de desinstalar.

Os APKs web têm identificação própria e coexistem com os APKs do gerador Linux e com o Elite Redux antigo. A chave Linux existente em `.private/` permanece local e não é usada no navegador. A assinatura web segue [APK Signature Scheme v2](https://source.android.com/docs/security/features/apksigning/v2), com bibliotecas não comprimidas alinhadas a 16 KiB conforme [zipalign](https://developer.android.com/tools/zipalign).

## ROM, logos e saves

Selecione uma ROM `.gba` descompactada de até 32 MiB. O cabeçalho GBA é validado; não há garantia de que todo jogo ou hack funcione. Nome e logo são opcionais. **Automática** detecta Emerald por BPE e FireRed por BPR no cabeçalho; demais jogos usam GBA. Também há escolha explícita de Emerald, FireRed e GBA.

O APK web contém somente a ROM escolhida, sem saves pessoais. No aplicativo, use **Menu → Saves** para importar `.sav` e estados mGBA, ou para exportar seu progresso. O HUD, editor de botões, orientações, áudio e configurações são os mesmos do aplicativo atual. O caminho interno estável `game/game.gba` preserva os saves quando só nome ou logo mudam.

O gerador Linux também aceita `--save`, `--state` e `--include-saves` quando você escolher incluir progresso explicitamente. Gere um pacote compartilhável sem jogos/chaves com `python3 tools/empacotar_projeto.py`.

## Organização

- `app/`: interface Android e wrapper nativo.
- `vendor/mgba/`: núcleo e licenças, revisão `26b7884bc25a5933960f3cdcd98bac1ae14d42e2`.
- `tools/`: gerador Linux, preparação e auditoria web, ferramentas de validação.
- `web/`: HTML, CSS, TypeScript e testes do empacotador.
- `.github/workflows/pages.yml`: preparação e publicação opcional.
- `docs/validacao-web.md`: resultados e limites dos testes desta entrega.

O histórico específico do Elite Redux está em `docs/historico-elite-redux.md`. A antiga cópia dos assets do jogo foi preservada em `.private/legacy-assets/`; os originais de gameplay não foram movidos.
