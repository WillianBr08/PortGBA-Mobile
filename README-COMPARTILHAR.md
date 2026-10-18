# GBA Studio 4.0

Gerador local de APKs Android com mGBA nativo, controles sobre o jogo, avanço rápido, saves e estados. O APK abre diretamente no jogo. Menu: **Jogo**, **Saves**, **Ajustes**. A orientação **Paisagem** fica na primeira aba.

Este pacote contém código, fontes, ícones e dependências do núcleo. Não contém ROMs, saves pessoais, APKs prontos nem a chave de assinatura do autor. Cada pessoa escolhe sua própria ROM GBA já preparada.

## Dependências para gerar os APKs

O gerador e os scripts de compilação são para Linux x86_64:

- Python 3 (com tkinter para a janela), JDK 17, CMake 3.22+, Ninja e ripgrep.
- Android SDK plataforma 36 e Build Tools 35.0.0.
- Android NDK r27c (`27.2.12479018`).
- O launcher prefere Tkinter do sistema; em algumas distribuições é preciso instalar `python3-tk`. Tcl/Tk 8.6 local é incluído como alternativa para Linux compatível; licenças em `tools/runtime`.

Configure os caminhos se necessário:

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
export ELITE_JAVA_HOME=/caminho/do/jdk-17
```

Nenhum token, registro de aplicativo Google ou serviço remoto de build é utilizado. Depois de instalar o SDK/NDK, a compilação utiliza os arquivos locais. O APK resultante funciona em Android 8 ou posterior, ARM64, ARMv7 e x86_64.

## Usar

Abra um terminal na pasta extraída:

```bash
chmod +x build.sh portar-gba.sh
./portar-gba.sh
```

Escolha a ROM `.gba`, nome do aplicativo e pasta de saída. A logo pode ser Automática, Emerald, FireRed ou GBA. Saves não entram por padrão; podem ser escolhidos explicitamente. **Criar APK** fica sempre disponível no rodapé; o formulário rola em telas menores.

Pelo terminal:

```bash
./portar-gba.sh /caminho/Jogo.gba --title 'Meu jogo' --output /caminho/saida
./portar-gba.sh /caminho/Jogo.gba --logo emerald
./portar-gba.sh /caminho/Jogo.gba --save /caminho/Jogo.sav --state /caminho/Jogo.ss1
```

A primeira compilação prepara as três arquiteturas. As próximas são incrementais. `./build.sh --template-only` compila a interface sem precisar de uma ROM nem produzir um APK incompleto. O gerador chama esse modo automaticamente e inclui a ROM somente no APK escolhido.

O programa cria uma chave local nova em `.private/elite-redux.jks`. Guarde essa pasta para assinar suas futuras atualizações. Uma atualização precisa da mesma chave e do mesmo identificador do APK instalado. Não substitua a chave de uma instalação existente e não compartilhe sua chave privada.

## No celular

O jogo abre diretamente com seu `.sav`; use Continue dentro do jogo. **SALVAR** e **CARREGAR** do HUD usam o estado automático `.ss0`. O carregamento tem prioridade no próximo quadro, sem esperar uma animação terminar. Os estados são PNG mGBA com compressão rápida e continuam incluindo imagem, progresso e RTC.

**Menu → Jogo → Paisagem** muda a orientação. **Ajustes → Controles** move, oculta e muda o tamanho de cada botão, com layouts separados por orientação. Voltar do Android abre o menu, mesmo com o HUD oculto.

**Saves** reúne importar e exportar. `.sav` é progresso salvo dentro do jogo; estado é uma captura instantânea. Importações de estado exigem compatibilidade com mGBA e a ROM correspondente. Exporte seus saves antes de desinstalar. Para atualizar, instale por cima com a mesma assinatura.

## Fonte e licenças

mGBA 0.10.5, commit `26b7884bc25a5933960f3cdcd98bac1ae14d42e2`, MPL-2.0. A alteração de compressão está descrita em `vendor/mgba/PATCHES-MOBILE.md`, com código disponível no pacote. Licenças das fontes, núcleo e créditos de arte estão em `app/src/main/assets/licenses/`. Os ícones Pokémon usam referências e artes oficiais; a opção GBA é genérica.

O gerador não modifica a ROM de entrada. Compatibilidade e velocidade dependem do jogo e do aparelho. Não implementa troca por link, cheats ou serviços de nuvem. Não converte código GBA em código Android: utiliza emulação nativa com nossa interface.
