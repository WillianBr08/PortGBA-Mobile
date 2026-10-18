# Mapa do GBA Studio

Verificado em 18/10/2026, a partir de b5e5519. Escopo: gerador web, interface Android, preparação de bases e empacotamento desktop. Fontes são autoridade; este mapa é um índice.

| Tarefa | Responsável | Próximo arquivo/comando |
|---|---|---|
| Planejamento e estado | docs/ | spec.md, plan.md |
| Geração no navegador | web/src/ | main.ts → worker.ts → apk.ts |
| Limites de ROM | web/src/rom.ts | inspectRom e validação de tamanho |
| ZIP e assinatura | web/src/zip.ts, signing.ts | alignedZip e signApk |
| Chave por navegador | web/src/identity.ts | getIdentity, backup, restoreBackup |
| Android | app/src/main/ | Java em java/, JNI e núcleo em cpp/ |
| Bases sem ROM | build.sh, tools/prepare_web.py | compilar → templates → catálogo com hashes |
| Gerador Linux legado | tools/portar_gba.py | make_port; portar-gba.sh |
| Publicação | .github/workflows/ | pages.yml |
| Dependência nativa | vendor/mgba | revisão fixada, não editar como código próprio |

Fluxo: arquivo local → validação → Worker baixa base estática → configura manifesto/recursos → ZIP alinhado → assinatura v2 pela chave local → Blob de download. A interface Android lê assets/game.json e copia o jogo para sua pasta de dados. Não existe serviço de upload. Fonts/licenças entram nas bases por allowlist; .private/, ROMs, saves e saídas de build ficam fora do Git.

Comandos descobertos e já executados antes desta mudança: `./build.sh --template-only`; `python3 tools/prepare_web.py`; em web/: `npm ci`, `npm test`, `npm run build`, `npm run preview`. Auditoria: `python3 tools/audit_web.py web/site`. O site usa URLs relativas para GitHub Pages sob /PortGBA-Mobile/. Novos comandos desktop serão acrescentados conforme implementados.

Limite observado no núcleo: GBALoadROM, vendor/mgba/src/gba/gba.c, mapeia ROMs comuns até SIZE_CART0 (32 MiB). Aceitar arquivo de 50 MB no empacotador não modifica essa limitação. O SHA da ROM define o identificador web; alterar nome ou logo preserva o identificador e os saves. Navegador e aplicativo desktop precisam de backups de chave para compartilhar identidade.

Áreas excluídas das leituras rotineiras: builds, dependências instaladas, runtime legado e relatórios históricos privados. Consulte docs/plan.md para a etapa atual; este mapa não replica o histórico da sessão.
