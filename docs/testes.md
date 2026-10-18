# Validação

Executado em 16/10/2026. Os arquivos originais do computador foram conferidos por SHA-256 e permanecem idênticos.

## Núcleo nativo no computador

`tools/core-smoke.c`, compilado em Release com o mesmo wrapper C usado pelo APK:

- Inicialização da ROM, produção de framebuffer e amostras de áudio.
- Carregamento dos quatro save states fornecidos.
- Novo estado salvo e restaurado, com comparação determinística do framebuffer depois do mesmo quadro de entrada.
- Gravação, importação e reabertura de save de progresso.
- Rejeição de estado inválido.
- Avanço rápido com renderização reduzida.

Resultado: PASS. O benchmark nessa máquina ficou em aproximadamente 1500 quadros emulados por segundo, sem a interface Android. Essa medição não é uma previsão para celulares.

## Aplicativo Android

Emulador Android 11/API 30 x86_64, tela 1080×1920 a 420 dpi, app executado com UID normal:

- Instalação do APK assinado localmente e inicialização da ROM.
- Carregamento dos estados originais 1, 2, 3 e 4 em cenas de gameplay.
- Gravação e leitura de novo estado no slot 5.
- Alternância entre 1×, 2× e 4×.
- Controles presentes em retrato e paisagem.
- Estado automático gravado ao ir para segundo plano.
- Retomada após encerrar o processo e abrir novamente.
- Exportação de ZIP pelo seletor Android; conteúdo conferido contra os arquivos originais, com `.sav`, estados e backups.
- Importação de `.sav` pelo seletor Android, com comparação byte a byte dos 128 KiB de progresso.
- Importação de save state pelo seletor Android, carregado e copiado integralmente para o slot selecionado.

As capturas e registros detalhados ficam em `captures/`, incluindo `android-tests.json`. O acesso root ao ADB do emulador foi utilizado somente pelo teste para inspecionar e comparar arquivos em `Android/data`; o aplicativo não solicita nem utiliza root.

## Empacotamento

- Assinaturas APK v2 e v3 verificadas por `apksigner`.
- Alinhamento ZIP de 16 KiB verificado por `zipalign`.
- Segmentos ELF ARM64 alinhados em `0x4000` (16 KiB).
- ARM64, ARMv7 e x86_64 compilados com o Android NDK r27c.
- ROM, `.sav` e quatro estados no APK comparados byte a byte com os assets e com os hashes dos arquivos originais.
- Manifesto sem permissões, incluindo ausência de `INTERNET`.

Não houve teste em celular físico ARM, medição de consumo/bateria ou teste em versões Android diferentes da indicada. Essas condições continuam a depender do aparelho.

## Versão 2.0 — 16/10/2026

- Regressão Flame Burst: ROM original + estado 2, 1600 quadros sem ajuste → falha determinística no quadro 1489; 3600 quadros com ajuste em memória → batalha prossegue. Frames 0, 60, 120, 300 e 480 idênticos antes do efeito. Detalhes em `compatibility/README.md` e montagem visual em `compatibility/batalha-corrigida.png`.
- Core smoke repetido com a correção: boot, áudio, leitura dos quatro estados, save state determinístico, bateria, rejeição de estado inválido e reabertura passaram. Aproximadamente 1413 FPS no host; não é medição de celular.
- Android 11/API 30: interface nova, editor, arraste de A, ajuste individual, mover painel, salvar slot 5, carregar estado 2, retrato/paisagem e retomada após force-stop. Preferências no disco confirmam coordenadas do A salvas separadamente para retrato.
- Replay Android por toque com avanço 4× confirmou o efeito secundário de Flame Burst e progressão da batalha. As primeiras tentativas via eventos de tecla instantâneos não acionaram A; a validação utilizou pressão por 100 ms no botão visível.
- Gerador Linux: janela 760×800 com ação Criar APK visível; CLI criou APK de homebrew independente, instalável e com pacote derivado do SHA256. Inspeção ZIP confirmou apenas a ROM selecionada e ausência de ROM/saves do Elite Redux.
- APK Elite Redux: ROM, save e quatro estados conferidos byte a byte; hashes dos seis arquivos de gameplay originais continuam iguais.
- Conferência final do APK instalado: avanço configurado para 6×, toggle de FPS pela linha inteira, orientação fixa em paisagem/retrato e Select/Start separados. Preferências persistidas; o menu também é redimensionado ao mudar a orientação com as configurações abertas.
- ARM64, ARMv7 e x86_64 recompilados. Sem validação em celular físico ARM; desempenho real depende do aparelho.

Capturas da interface 2.0 em `captures/v2/`. Os registros anteriores acima descrevem os testes da primeira versão; esta seção registra o conjunto adicional executado na 2.0.

Regressão final de rotação: APK atualizado reinstalado; menu fechado com Jogar após Paisagem e após Retrato. PASS. Capturas `21-rotation-landscape-fixed.png` e `22-rotation-portrait-fixed.png`.

## Versão 3.0 — saves automáticos, página inicial e ROM do PC

- Abertura normal, force-stop/reabertura e atualização por cima da instalação existente mostram a página inicial, sem pergunta ou retomada automática.
- Hash do `.ss0` existente preservado ao abrir o aplicativo e ao editar o HUD a partir da página inicial.
- Botão B ajustado para 153% e ocultado; preferências persistidas. O editor mostra os controles ocultos para permitir restaurá-los. Seu painel fica acima dos botões.
- SALVAR no HUD atualiza `.ss0`, não cria `.ss5` e não abre confirmação. CARREGAR restaura `.ss0` sem seletor ou confirmação.
- Importação de `.sav` pelo seletor Android e exportação direta de `.sav`: 131072 bytes de progresso idênticos ao arquivo do PC. A extensão RTC pode mudar com o relógio do núcleo.
- Importação de `.ss2` pelo seletor Android: carregado e copiado byte a byte para `.ss0`. O teste precisou remover uma entrada obsoleta do MediaStore do emulador, referente ao `.sav` exportado e depois apagado pelo ADB; o aplicativo recusou corretamente a seleção desse arquivo inexistente.
- Jogar pelo `.sav` usa uma cópia do arquivo selecionado antes de sincronizar o estado ativo. Progresso conferido byte a byte após o carregamento.
- Gerador gráfico oferece Automática, Emerald, FireRed e GBA. Janela e ação Criar APK visíveis. APKs Emerald/FireRed gerados; FireRed instalado e aberto com o ícone escolhido. Saves não entram por padrão, inclusive com um `.sav` sintético ao lado da ROM de teste.
- APK principal inclui somente a ROM em `assets/game/`, sem save ou estado pessoal. As cópias de saves, dumps RAM e ZIP de testes foram retiradas do projeto; os saves de gameplay do PC permaneceram com os hashes originais.
- ROM do PC corrigida no próprio caminho com backup original. Reprodução de 3600 quadros com `ELITE_DISABLE_COMPAT=1` passou, comprovando a correção no arquivo GBA sem depender do wrapper Android.
- Compilação limpa das classes Java e resources, três ABIs nativas, assinatura local e alinhamento de 16 KiB verificados. Capturas em `captures/v3/`, relatório em `validation-v3.json`.

Os testes Android continuam limitados ao Android 11/API 30 x86_64 emulado. Não houve teste em celular físico ARM.

## Versão 4.0

- Início direto no jogo, sem perguntas, inclusive ao reabrir o processo.
- Três abas fixas; Voltar das subpáginas retorna a Ajustes, depois ao jogo. Retrato e paisagem testados.
- Salvar automático e carregar durante animação, incluindo avanço 8× no Quetzal: logs em captures/v4.
- Benchmark com requisito de redução de CPU: vermelho antes, verde depois; PNG/raw com SRAM e RTC determinísticos.
- InputState: toque curto vermelho antes; toque curto, hold, diagonais, troca de direção e touch/gamepad verdes depois.
- Core-smoke host: boot, áudio, quatro estados originais, gravação/leitura determinística, .sav e reabertura passaram.
- Gerador executado com PokemonQuetzalPtBrAlpha9v0.gba. APK instalado no emulador API30, Start chegou a NOVO JOGO, rotação, fast-forward e estados passaram. ROM incorporada é idêntica à entrada.
- Interface do gerador em 680×600: rolagem, botão de gerar fixo e quatro opções de logo.
- Fonte compartilhável extraída e compilada em x86_64 sem ROM; manifesto e licenças incluídos. Sem saves, ROM, chave privada ou .git.
- APKs: assinatura, zipalign 16 KiB, três ABIs e ausência de saves pessoais conferidos. Saves originais do PC conferidos por SHA256.
- Medições e limitações em performance-v4.md; auditoria em artifact-audit-v4.json. Não testado em aparelho físico nem por toda a campanha.

- Quetzal: seletor Android exportou .sav de 131.088 bytes e ZIP com estado automático; ZIP sem ROM. Reimportação do estado via DocumentsUI passou (79 ms).
- Reimportação do .sav exportado via DocumentsUI também passou (63 ms).
