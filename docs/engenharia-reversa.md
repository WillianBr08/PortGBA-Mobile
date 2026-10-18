# Engenharia reversa e escopo da adaptação

## Artefato selecionado

ROM: `Elite Redux (2.65.4.1b).gba`, 33554432 bytes.

SHA-256: `926098fa9f6b29b469fa351e80e4f018285006ad10a4eb5bc255d14d6d54acfc`.

O REA abriu a ROM completa como binário ARM bruto, com offset zero mapeado em `0x08000000`. A consulta subsequente ao provedor Ghidra excedeu o limite de 300 segundos antes de disponibilizar uma análise completa. Nenhum resultado parcial dessa tentativa é tratado como decompilação concluída.

Para inspecionar o caminho de entrada de forma delimitada, foram copiados os primeiros **8192 bytes**, sem modificações, para o arquivo privado `.private/rom-boot-8k.bin` (fora da publicação). A origem, offset, extensão e hashes estão em `rom-inspection.json`. O REA abriu esse recorte com o mesmo endereço-base e arquitetura; o Ghidra 12.1.4 concluiu a análise desse recorte.

## Observações do REA

- `.private/rea-entry.json`, Evidence `ev_c6db73cfac0e299c83261e260ec283901bf57dc36cf376d61edffc66933d327b`: em `0x08000000`, instrução ARM `b 0x080001fc`. É uma transferência para o código de inicialização presente no próprio recorte.
- `.private/rea-reset.json`, Evidence `ev_2485c212c6e76a7fccfd0cfc17eef6c57f1b4fd3a6c9a27686b75e7c06265342`: inicialização de modos CPSR e ponteiros de pilha, chamada `swi 0x10000`, desvio indireto por `bx r1` e retorno ao caminho de inicialização. A existência dessas instruções é observada estaticamente; sua execução não foi observada pelo REA.
- `.private/rea-header.json`, Evidence `ev_6d21dccf1eda05e2f1fdeac3d7d7e1d0ed0841fbbf22a7ced46f3b46020601c9`: os 32 bytes do cabeçalho em `0x080000a0` foram lidos integralmente pelo provedor. Contêm os identificadores `POKEMON EMER` e `BPEE`.

Uma inspeção adicional com Python, separada das evidências REA, verificou o tamanho, SHA-256 e checksum do cabeçalho. O cabeçalho identifica a base Emerald; sozinho, não comprova a versão da hackrom informada no nome do arquivo.

## Decisão de implementação

As instruções de inicialização dependem do ambiente GBA, incluindo modos de CPU, memória e chamadas ao BIOS. A adaptação executa a ROM intacta usando o núcleo mGBA nativo e oferece interface, áudio, armazenamento e controles Android. Não recompila pseudocódigo do Ghidra nem alega ter recuperado o código-fonte completo da hackrom.

A compatibilidade dos quatro save states fornecidos foi verificada executando cópias no núcleo compilado para o computador. Cada um foi carregado e produziu imagem de gameplay. Um novo estado passou por teste determinístico de salvar, avançar, restaurar e avançar novamente. Os testes também cobrem produção de amostras de áudio, gravação/importação de `.sav`, rejeição de estado inválido e reabertura do núcleo.

## Limitações

O recorte analisado não cobre os 32 MiB da ROM, lógica de batalhas ou todos os caminhos de execução. O provedor utilizou linguagem genérica `ARM:LE:32:v7`; isso não altera o fato de o núcleo emular o hardware ARM7TDMI do GBA. A decodificação genérica do REA e o pseudocódigo não garantem tipagem correta nem equivalência ao código-fonte, especialmente em chamadas indiretas, transições ARM/Thumb e código copiado para RAM. O teste executável do núcleo é uma evidência distinta da análise estática. Medições de velocidade no computador não representam desempenho em um celular físico.

## Captura Android pelo REA

Após instalar o APK final no emulador, `capture_adb_screen` registrou uma imagem 1080×1920 em `captures/rea-runtime/screen.png`, com Evidence `ev_5b1f3cbc719413a5629413f27efde48b2423ab4febf59016843b23d3f4f20804`. O registro completo está em `rea-android-screen.json`. Essa observação comprova somente o quadro capturado naquele momento; a ferramenta não vinculou a captura à identidade do APK nem mediu desempenho. A instalação e os fluxos funcionais foram verificados separadamente pelos testes Android.
