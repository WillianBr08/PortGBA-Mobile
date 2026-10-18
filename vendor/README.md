# Núcleo mGBA

O núcleo é obtido de https://github.com/mgba-emu/mgba no commit `26b7884bc25a5933960f3cdcd98bac1ae14d42e2` por `python3 tools/prepare_core.py` e fica em `build/mgba/`.

`patches/mgba-state-io.patch` preserva a alteração de compressão de estados já usada pelo aplicativo. O patch mantém o formato mGBA e permite configurar o nível de compressão. A preparação verifica a revisão e aplica o patch de forma idempotente.

A fonte upstream é MPL-2.0, com dependências cujas licenças ficam no próprio núcleo. A distribuição Android inclui as licenças em `android/app/src/main/assets/licenses/`. Não são versionados downloads nem fontes duplicadas de terceiros.
