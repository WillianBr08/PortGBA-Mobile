# Flame Burst — Elite Redux 2.65.4.1b

Reprodução: estado original `.ss2`, pulso de A por quatro quadros a cada 45 quadros. Oponente Ninetales usa Flame Burst; a mensagem de dano secundário em Ledian aparece e a batalha deixa de responder. A mesma reprodução falha com mGBA 0.10.5 e com o master consultado em 16/10/2026.

A inspeção da ROM e a execução instrumentada localizaram o desvio em `0x0809db66`: o efeito substitui o ponteiro de instrução pelo script `0x082881f6` sem empilhar o retorno. No fim, `return` em `0x08288211` retira um endereço inexistente. O ponteiro cai para zero e depois para `0x0000000a`; o jogo continua produzindo quadros idênticos.

A correção avança o ponteiro original em um byte e chama a rotina existente `0x080c4ef4`, que empilha corretamente o retorno. Retoma a limpeza original em `0x0809ca44`. Não elimina Flame Burst nem seu dano secundário. O payload Thumb ARM7TDMI ocupa 44 bytes em `0x09fff000` e a entrada usa dez bytes em `0x0809db66`.

`app/src/main/cpp/compatibility.c` aplica o ajuste somente na memória após verificar tamanho de 32 MiB, CRC32 `1d230210`, assinatura dos dez bytes originais e região de destino preenchida com FF. A ROM no disco e no APK permanece idêntica à original, SHA256 `926098fa9f6b29b469fa351e80e4f018285006ad10a4eb5bc255d14d6d54acfc`. Outros jogos não recebem o ajuste. Um estado criado depois de o script já ter perdido seu retorno pode continuar travado; carregue um estado anterior à falha.

## Regressão reproduzível

Na raiz deste projeto:

```bash
cmake -S . -B build/host -G Ninja -DCMAKE_BUILD_TYPE=Release -DBUILD_LTO=OFF
cmake --build build/host --target battle-replay -j8
ELITE_DISABLE_COMPAT=1 build/host/battle-replay 'app/src/main/assets/game/Elite Redux (2.65.4.1b).gba' 'app/src/main/assets/game/Elite Redux (2.65.4.1b).ss2' build/repro-sem-correcao 1600
build/host/battle-replay 'app/src/main/assets/game/Elite Redux (2.65.4.1b).gba' 'app/src/main/assets/game/Elite Redux (2.65.4.1b).ss2' build/repro-corrigido 3600
```

Sem correção: saída 3, quadro 1489, imagem invariável por 900 quadros. Com correção: saída 0 em 3600 quadros; inspeção das imagens confirmou novos ataques, dano secundário, desmaios e seleção de substitutos. O teste salva imagens PPM a cada 60 quadros e possui timeout por quadro. A comparação dos quadros antes do efeito confirma a mesma sequência até o ponto do defeito. Esta é uma regressão específica da batalha, não um teste exaustivo de todas as habilidades da hackrom.

`flame-burst.S` e `flame-burst.ld` registram o assembly e os endereços. Ao extrair a seção `.entry`, os dois bytes de alinhamento iniciais devem ser o NOP Thumb `c0 46`; o restante deve corresponder a `flame-entry.bin`. `flame-payload.bin` contém os 44 bytes do payload. Os arrays em `compatibility.c` são a referência executada pelo APK.

## Aplicação na ROM de gameplay do PC (versão 3.0)

A pedido do usuário, `tools/corrigir_ninetales.py` aplicou os mesmos dez bytes de entrada e 44 bytes de payload ao arquivo GBA na pasta de gameplay. A ferramenta exige o SHA256 original conhecido e guarda uma cópia `.gba.antes-ninetales.bak` antes da substituição atômica. Os saves não são escritos por ela. O registro completo está em `pc-patch.json`.

Regressão executada com `ELITE_DISABLE_COMPAT=1` e a ROM do PC já corrigida: 3600 quadros, saída zero e progressão da batalha. Assim o resultado independe do ajuste do wrapper Android. Reabrir a ROM no emulador do PC é necessário para que um processo já aberto deixe de usar os bytes antigos em memória.

O APK continua incluindo a ROM original com ajuste em memória para manter compatibilidade com as instalações existentes. A partir da versão 3.0, nenhum save pessoal é incluído nos assets. Para reproduzir o teste, use o estado original que permanece na pasta de gameplay do PC; os comandos acima descrevem os assets da versão 2.0, antes da remoção solicitada.
