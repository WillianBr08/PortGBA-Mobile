# Desempenho e regressões — 4.0

A queixa foi carregamento esperar a animação terminar. O núcleo agora executa um quadro por aquisição do monitor. A solicitação de arquivo incrementa `operations` antes de entrar na fila; o loop suspende a execução no próximo quadro, mesmo em avanço 8×. Compressão, backup e disco ficam fora da thread da interface. Ao restaurar, o JNI apresenta imediatamente o quadro carregado.

`InputState` mantém transições curtas até a próxima consulta. O loop consulta os controles a cada quadro e apresenta somente o último quadro do grupo acelerado.

O PNG continua compatível com mGBA, mantendo captura de tela, SRAM, RTC e metadados. A opção local `stateCompressionLevel=1` reduz compressão de estado e screenshot. O padrão do núcleo externo continua nível 6. Patch documentado em `vendor/mgba/PATCHES-MOBILE.md`.

Benchmark host `tools/state-bench.c`, 25 iterações; tempos de ciclo salvar/carregar, incluindo I/O:

| ROM / formato | Mediana (ms) | p95 (ms) | CPU (ms) | Bytes |
|---|---:|---:|---:|---:|
| Elite / PNG anterior | 7,594 | 7,783 | 6,279 | 67.436 |
| Elite / PNG app 4 | 4,376 | 4,572 | 2,690 | 86.277 |
| Quetzal / PNG anterior | 8,676 | 25,084 | 3,556 | 8.681 |
| Quetzal / PNG app 4 | 9,597 | 19,214 | 2,093 | 16.206 |

O uso de CPU caiu 57% no Elite e 41% no Quetzal. A duração total depende do disco; a mediana total do Quetzal aumentou nesta amostra. O teste determinístico de ida e volta passou em todos os formatos. O formato bruto foi avaliado e descartado: 528.500 bytes por estado.

A regressão `--require-fast` falhava antes da alteração e passou depois, exigindo redução mínima de 35% de CPU. O teste Java de toque curto também falhava antes e passou depois; cobre soltar, manter, diagonais, troca de direção e fontes touch/gamepad.

Android 11/API 30, emulador x86_64: durante a introdução animada, três cargas no Elite em 4× demoraram 34, 36 e 33 ms; no Quetzal em 8×, 30, 78 e 40 ms. A duração Quetzal inclui fila, recuperação e leitura até a conclusão do worker. A gravação Quetzal foi 92 ms. Logs em `captures/v4/*-states.log`.

Essas medidas não substituem teste em celular físico ARM. Foram testadas as cenas citadas; não a campanha completa.
