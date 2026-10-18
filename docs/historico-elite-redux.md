# Elite Redux Mobile 4.0 — privado

Instale `dist/Elite-Redux-Mobile.apk` no Android 8 ou posterior. Instale por cima da versão anterior para manter os arquivos do celular. A chave local `.private/elite-redux.jks` continua a mesma. Não há conta, token, serviços Google, anúncios, telemetria nem permissão de internet.

O aplicativo executa a ROM pelo mGBA 0.10.5 nativo; não é uma conversão do código GBA para Android. O projeto fica separado da sua pasta de gameplay.

## Início e saves

O aplicativo abre diretamente no jogo, sem perguntas. Usa o `.sav` da pasta e deixa o carregamento de estado por sua conta. Uma instalação nova começa em paisagem; a orientação escolhida fica salva.

O menu tem três abas: **Jogo**, **Saves** e **Ajustes**. Em Jogo ficam os atalhos do estado automático, Paisagem/Retrato/Automática e o editor de botões. Em Saves ficam importação, exportação e slots opcionais. Em Ajustes ficam tela, som, controles e velocidade. Voltar fecha o menu; dentro de uma subpágina, retorna a Ajustes.

- **Importar progresso (.sav)** aceita EEPROM, SRAM e Flash, incluindo arquivos de 128 KiB com RTC. O progresso é o SAVE feito dentro do jogo.
- **Importar estado** carrega um estado compatível com mGBA e o guarda como automático. Estados de outros emuladores não têm conversão universal.
- **Exportar progresso (.sav)** cria um arquivo `.sav` diretamente na pasta escolhida.
- **Exportar todos os saves** cria um ZIP com os saves e estados, sem incluir a ROM.

No HUD, **SALVAR grava o estado automático** e **CARREGAR restaura o automático**, diretamente e sem escolher slot ou confirmar. A gravação explícita também sincroniza o `.sav`. Ao sair do jogo ou trocar de aplicativo, o estado automático é atualizado se essa opção estiver ativada.

O APK não inclui seu `.sav` nem seus estados pessoais. Foram retiradas as cópias do projeto e dos arquivos de distribuição. Seus arquivos originais de gameplay no PC e os arquivos já instalados no celular são preservados. A instalação copia apenas assets ausentes, sem apagar os saves existentes.

Arquivos do aplicativo:

```text
Android/data/dev.privado.eliteredux/files/Elite Redux/
    Elite Redux (2.65.4.1b).gba
    Elite Redux (2.65.4.1b).sav
    Elite Redux (2.65.4.1b).ss0       automático do HUD
    Elite Redux (2.65.4.1b).ss1–9    slots manuais opcionais
    antes-de-carregar.ss            recuperação da sessão anterior
    backups/                       versões substituídas
```

A cópia `.sav.working` é o arquivo de trabalho do núcleo. O `.sav` é sincronizado por gravação temporária e renomeação atômica. Exportar pelo aplicativo permite acessar os arquivos mesmo quando o Android restringe `Android/data`. Exporte antes de desinstalar: a desinstalação apaga a pasta do aplicativo.

## HUD e configurações

D-pad com diagonais e multitoque, A/B/L/R/Select/Start. **Menu → Jogo → Editar os botões** abre o editor. Arraste cada controle; toque para mudar seu tamanho, opacidade ou visibilidade. **Mover painel** libera os botões que estiverem atrás do editor; **Concluir** grava o layout. Retrato e paisagem têm configurações independentes.

**Ajustes → Controles → Botões visíveis** permite tirar ou trazer de volta qualquer botão, inclusive Menu. O botão Voltar do Android abre o menu. Há também tamanho e opacidade gerais, vibração e opção de esconder todo o HUD.

Vídeo: proporção 3:2, tela esticada, pixels inteiros, orientação, FPS e manter tela acesa. Áudio: volume e silenciar. Desempenho: avanço de 2×, 3×, 4×, 6× ou 8× e descarte de renderização. O jogo pausa em menus e seletores. O avanço rápido silencia o áudio e sua velocidade efetiva depende do aparelho. Os nove slots manuais continuam na aba Saves, separados dos atalhos automáticos do HUD.

## Correção da Ninetales no PC e no APK

A ROM usada no PC, na pasta acima deste projeto, foi corrigida no próprio caminho. A original foi preservada como:

```text
Elite Redux (2.65.4.1b).gba.antes-ninetales.bak
```

Feche e reabra a ROM no emulador do PC para ativar os bytes corrigidos; um processo já aberto pode continuar usando a cópia antiga na memória. Use um estado anterior ao travamento. A correção conserva Flame Burst e seu dano secundário.

O defeito foi reproduzido com o estado 2 e corrigido no retorno do script de batalha. O teste da ROM corrigida no disco passou por 3600 quadros com a correção do wrapper desativada. Os `.sav` e `.ss1` a `.ss4` do PC não foram modificados. Detalhes em `docs/compatibility/README.md` e hashes antes/depois em `docs/compatibility/pc-patch.json`.

O APK mantém a correção em memória para a versão original conhecida, funcionando também nas instalações existentes que já possuem a ROM. A correção não é aplicada a outras ROMs.

## GBA Studio — gerar outro APK

Abra `GBA-Studio.desktop` ou execute `./portar-gba.sh` para usar a janela Linux. Escolha ROM, nome, pasta de saída e a logo:

- **Automática**: detecta Emerald/FireRed pelo código do cabeçalho; usa ícone GBA para os demais.
- **Emerald · Rayquaza**: ícone elaborado a partir da capa oficial.
- **FireRed · Charizard**: arte oficial de Charizard sobre um emblema rubi e âmbar.
- **GBA · neutra**: ícone genérico.

A escolha define o ícone do aplicativo gerado. Nenhum save entra por padrão. Você pode selecionar explicitamente um `.sav`, estados ou marcar a inclusão dos saves ao lado da ROM.

```bash
./portar-gba.sh /caminho/Jogo.gba --title 'Meu jogo' --output /caminho/Meu-APK --logo emerald
./portar-gba.sh /caminho/Jogo.gba --logo firered
./portar-gba.sh /caminho/Jogo.gba --save /caminho/Jogo.sav --state /caminho/Jogo.ss1
./portar-gba.sh /caminho/Jogo.gba --include-saves
```

O gerador aceita ROMs GBA descompactadas de até 32 MiB e cria identificadores derivados do SHA256, permitindo aplicativos separados. A mesma ROM mantém seu identificador e a assinatura para atualização. A saída inclui APK, SHA256, `port.json` e instruções. Não sobrescreve um APK existente nem altera a ROM selecionada. Compatibilidade de cada jogo depende do mGBA.

A janela usa o Tk do sistema (`python3-tk`) ou a cópia local em `tools/runtime` como alternativa. O gerador compila a interface atual de forma incremental e inclui somente a ROM e os saves selecionados, sem carregar o jogo Elite Redux para outros APKs.

## Compilação e evidências

Linux, JDK 17, Python 3, CMake 3.22+, Ninja, ripgrep; Android SDK plataforma 36, Build Tools 35.0.0 e NDK r27c:

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
./build.sh
```

ARM64, ARMv7 e x86_64; alinhamento de 16 KiB; sem Gradle, Maven ou serviços remotos. O núcleo está fixado no commit `26b7884bc25a5933960f3cdcd98bac1ae14d42e2`. Guarde a chave local para futuras atualizações.

Engenharia reversa com REA/Ghidra em `docs/engenharia-reversa.md`; diagnóstico e teste de Flame Burst em `docs/compatibility/`. A direção visual anterior usa a skill canvas-design. O ícone Emerald usa imagegen com a capa como referência; a opção FireRed compõe a arte oficial de Charizard com um fundo nativo do Android, após recusa da variante pelo serviço de geração. Fontes, núcleo e respectivas licenças ficam nos assets; referências das capas em `docs/icon-references/`.

Os testes Android usam emulador Android 11/API 30. Ainda não houve medição em celular físico ARM. Os relatórios por versão registram o comportamento testado naquela versão; as capturas atuais ficam em `captures/v4/`.

## Desempenho da versão 4

Salvar e carregar usam uma fila de arquivos fora da interface. Uma solicitação suspende a emulação no próximo quadro, sem esperar a animação terminar. A imagem restaurada é apresentada imediatamente. O avanço rápido consulta os controles a cada quadro; toques curtos ficam registrados até a próxima leitura.

Os estados continuam PNG padrão mGBA, com captura de tela, SRAM e RTC. A compressão nível 1 reduz trabalho de CPU. No benchmark Elite, a mediana de CPU do ciclo salvar/carregar caiu de 6,279 para 2,690 ms; no Quetzal, de 3,556 para 2,093 ms. O tempo de disco varia. No Android emulado, três cargas durante a animação do Quetzal em 8× levaram 30, 78 e 40 ms. Não são medições de um celular físico.

## Quetzal e projeto para compartilhar

O gerador foi testado com `PokemonQuetzalPtBrAlpha9v0.gba`, sem alterar a ROM. APK em `dist/Pokemon-Quetzal/Pokemon-Quetzal.apk`: ROM incluída, logo Emerald automática e nenhum save pessoal. Foram verificados inicialização, Start, rotação, avanço 8× e salvar/carregar durante a animação. Não foi testada a campanha inteira.

`dist/GBA-Studio-Projeto.zip` contém o código, núcleo, interface e gerador. Não inclui ROM, saves, chave privada ou histórico Git. Uma extração foi compilada com `./build.sh --template-only`, sem ROM, usando x86_64. Cada pessoa escolhe sua ROM e gera sua própria chave local. Instruções portáteis em `README-COMPARTILHAR.md`. Os APKs prontos incluem suas respectivas ROMs conforme solicitado.

Evidências desta versão: `docs/validation-v4.json`, `docs/performance-v4.md` e `docs/testes.md`.
