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

O workflow foi validado localmente quanto à sintaxe e às etapas de compilação, montagem, assinatura e auditoria. Ainda não foi executado no GitHub: falta definir o repositório remoto e habilitar Pages. Publicação permanece condicionada à variável `GBA_STUDIO_PUBLISH=true`.
