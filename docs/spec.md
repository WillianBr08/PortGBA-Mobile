# GBA Studio 1.1 — especificação

## Objetivo e escopo confirmado
Refazer o site do zero com referências ao GBA, conforme o pedido e a confirmação do usuário em 18/10/2026. Melhorar README, organizar o repositório e disponibilizar o gerador em AppImage e, se viável, EXE Windows. Preservar geração local, interface Android, assinatura e dados existentes.

| Capacidade | Responsabilidade | Dependência |
|---|---|---|
| repository | Fonte enxuta, mGBA oficial fixado, scripts e documentação | — |
| file-limit | Validar e preservar ROMs até 50.000.000 bytes (50 MB) | repository |
| generator-ui | Nova identidade, prévia, formulários, estados e downloads | file-limit |
| desktop | Gerador offline com bases incluídas, AppImage e EXE | generator-ui |
| delivery | README, testes, release pública e Pages | desktop |

## Requisitos e aceitação
- Site novo responsivo em português; referência GBA reconhecível; geração acessível por teclado, sem cadastro/upload.
- Até 50 MB aceitos na web e no gerador Linux. Acima disso, rejeitar antes da leitura completa. Testar fronteiras de tamanho e preservar bytes dentro do APK.
- Acima de 32 MiB, informar que o mGBA mapeia ROMs comuns até 32 MiB; não prometer compatibilidade ampliada nem truncar a entrada ao empacotar.
- Nome/logo opcionais, prévia, progresso, cancelar geração, erros recuperáveis, download e backup/restauração preservados.
- AppImage e EXE executam o mesmo gerador offline, sem SDK/JDK/NDK instalados pelo usuário. Chave persiste entre reinícios por origem estável; backups permitem migração.
- Downloads reais em GitHub Releases e links no site; EXE sem assinatura comercial identificado na documentação.
- Manter somente fontes, dependências declaradas, licenças, testes relevantes e documentação atual. Remover runtimes duplicados e relatórios históricos do Git atual; preservar cópia privada local.
- mGBA obtido do repositório oficial no commit existente; preparação aplica o patch de I/O já usado pelo projeto. Não reescrever histórico nem publicar ROMs, saves ou chaves.

## Definition of Done
Testes de limites e assinatura, TypeScript/build, compilação Android limpa, auditoria de arquivos, geração no navegador desktop/mobile, AppImage executado, Windows produzido e verificado em runner Windows, pipeline/release/Pages concluídos. Registrar evidências e limites reais em docs/testing.md. Publicar apenas depois das verificações relevantes.

## Fora de escopo
Aumentar o espaço de endereçamento do GBA, converter ROMs, alterar a interface Android ou garantir compatibilidade de jogos acima de 32 MiB.
