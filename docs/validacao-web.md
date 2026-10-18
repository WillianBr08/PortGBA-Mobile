# Validação do GBA Studio web

Executada em 17/10/2026 (America/Bahia). Artefatos de teste ficam em `output/playwright/` e `build/`, ignorados pelo Git. Não incluem dados pessoais nos arquivos publicados.

## Resultados

- Compilação da interface Android e mGBA em ARM64, ARMv7 e x86_64, após mover o projeto para `GBA-Studio`: PASS. O gerador Linux criou outro APK do Quetzal no novo caminho.
- TypeScript, build de produção Vite e quatro testes automatizados: PASS. Cobrem extensão/tamanho/cabeçalho, detecção Emerald/FireRed, acentos, nomes inválidos, CRC, alinhamento ZIP, preservação de bytes, exportação/restauração e rejeição de chaves incompatíveis.
- APK sintético criado pelo mesmo empacotador: `apksigner verify` confirma v2; `zipalign -c -P 16 4` confirma alinhamento. Alterar um byte causa rejeição com CHUNKED_SHA256 digest mismatch. Esses testes também estão no workflow.
- Chromium: seleção local do Quetzal de 32 MiB, geração no Worker e download do APK: PASS. Nome `Pokémon Quetzal — Ação` reconhecido pelo Android. ROM incorporada idêntica à entrada, SHA-256 `9883aebc24ac670d2be50a1cc5ce9a652752782a55fed22c4f74ed523197ee3f`. Nenhum save no APK.
- Repetição com logos automática/Emerald, FireRed e GBA: PASS. Backup e restauração da chave pelo site preservam o certificado. Atualização Android por cima da primeira instalação com nova logo: PASS.
- Chromium: arquivo .zip, .gba com cabeçalho inválido e tamanho acima de 32 MiB rejeitados com mensagem clara. Desktop 1365 px e celular 390 px sem transbordamento horizontal. Revisão visual final: ship, nenhum bloqueio material encontrado no escopo da revisão.
- Captura de tráfego durante geração: somente GET do Worker, catálogo JSON e template APK, todos na mesma origem e sem corpo de requisição. ROM e chave não aparecem em URL ou corpo. Não há analytics, CDN, fontes remotas ou endpoint de upload.
- Android 11/API 30 x86_64 emulado: instalação, boot do Quetzal, Start, paisagem/retrato e salvar/carregar automático: PASS. Log GBA-State registrou gravação em 166 ms e carga em 105 ms, sem FAIL ou AndroidRuntime fatal. Não são medições de aparelho físico.
- ELF ARM64 com segmentos LOAD alinhados em 0x4000. Bibliotecas ZIP não comprimidas e alinhadas a 16 KiB.
- `npm audit`: zero vulnerabilidades. Auditoria dos arquivos publicáveis e do ZIP compartilhável: sem ROM, save, chave privada ou APK com jogo. Trecho de ROM e exportações de engenharia reversa antigos foram guardados em `.private/`.

## Limites

Não foram testados aparelhos físicos ARM, um sistema Android com páginas reais de 16 KiB, Safari ou Firefox. A campanha completa do Quetzal não foi jogada. O menu de importação e o seletor Android foram abertos; a rodada atual de automação não concluiu a seleção do arquivo. Salvar e carregar estados automáticos foram efetivamente verificados. A implementação Android de importação é a mesma já existente.

O conteúdo versionado foi extraído em uma pasta limpa e recompilado nas três arquiteturas em 18/10/2026. Preparação sem chave/ROM, instalação das dependências, testes, build web e auditoria também passaram nessa cópia. O projeto foi enviado a [WillianBr08/PortGBA-Mobile](https://github.com/WillianBr08/PortGBA-Mobile), preservando o commit inicial. GitHub Pages usa a origem workflow e a variável `GBA_STUDIO_PUBLISH=true`.

Etapa SHIP concluída em 18/10/2026. O [workflow 39099743331](https://github.com/WillianBr08/PortGBA-Mobile/actions/runs/39099743331), revisão `b06ca04`, terminou com build e deploy bem-sucedidos. O [site público](https://willianbr08.github.io/PortGBA-Mobile/) e o catálogo responderam HTTP 200. Chromium gerou e baixou o APK do Quetzal a partir desse endereço, sem erros de execução ou console. `apksigner` confirmou assinatura v2, `zipalign -c -P 16 4` passou, e a ROM incorporada é idêntica à entrada. Durante a geração foram observados somente GET do Worker, catálogo e base Emerald na mesma origem, todos sem corpo de envio.

O código e a publicação podem ser acompanhados em [Actions](https://github.com/WillianBr08/PortGBA-Mobile/actions/workflows/pages.yml). Atualizações posteriores devem manter essas mesmas verificações.

Para interromper novas publicações, defina `GBA_STUDIO_PUBLISH=false`; isso preserva a versão já publicada. Para reverter uma atualização, reverta o commit problemático em main e execute o workflow. Na primeira publicação, se for necessário retirar o site, use Settings → Pages → Unpublish site. Não acrescente telemetria que possa enviar ROMs ou chaves; a verificação pública usa status HTTP, logs do Actions e o fluxo de geração no navegador.

A primeira execução remota identificou a dependência obsoleta `tools` no padrão de android-actions/setup-android@v3. O workflow fixa explicitamente `packages: platform-tools`; plataforma, Build Tools e NDK continuam instalados com versões declaradas na etapa seguinte.
