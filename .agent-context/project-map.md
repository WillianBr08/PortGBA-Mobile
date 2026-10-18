# Mapa do GBA Studio

Verificado em 18/10/2026, entrega v1.1.0 (código 1b29bf7). Escopo: gerador web, interface Android, preparação de bases e empacotamento desktop. Fontes são autoridade; este mapa é um índice.

| Tarefa | Responsável | Próximo arquivo/comando |
|---|---|---|
| Planejamento e estado | docs/ | spec.md, plan.md |
| Geração no navegador | web/src/ | main.ts → worker.ts → apk.ts |
| Limites de ROM | web/src/rom.ts | inspectRom e validação de tamanho |
| ZIP e assinatura | web/src/zip.ts, signing.ts | alignedZip e signApk |
| Chave por navegador | web/src/identity.ts | getIdentity, backup, restoreBackup |
| Android | android/app/src/main/ | Java em java/, JNI e núcleo em cpp/ |
| Bases sem ROM | build.sh, tools/prepare_web.py | compilar → templates → catálogo com hashes |
| Gerador Linux legado | tools/portar_gba.py | make_port; portar-gba.sh |
| Publicação | .github/workflows/ | pages.yml e release.yml |
| Dependência nativa | tools/prepare_core.py, vendor/patches/ | fonte fixada obtida em build/mgba/ |

Fluxo: arquivo local → validação → Worker baixa base estática → configura manifesto/recursos → ZIP alinhado → assinatura v2 pela chave local → Blob de download. A interface Android lê assets/game.json e copia o jogo para sua pasta de dados. Não existe serviço de upload. Fonts/licenças entram nas bases por allowlist; .private/, ROMs, saves e saídas de build ficam fora do Git.

Comandos descobertos e já executados antes desta mudança: `./build.sh --template-only`; `python3 tools/prepare_web.py`; em web/: `npm ci`, `npm test`, `npm run build`, `npm run preview`. Auditoria: `python3 tools/audit_web.py web/site`. O site usa URLs relativas para GitHub Pages sob /PortGBA-Mobile/. Comandos desktop estão descritos abaixo e em docs/development.md.

Limite observado no núcleo: GBALoadROM, build/mgba/src/gba/gba.c, mapeia ROMs comuns até SIZE_CART0 (32 MiB). Aceitar arquivo de 50 MB no empacotador não modifica essa limitação. O SHA da ROM define o identificador web; alterar nome ou logo preserva o identificador e os saves. Navegador e aplicativo desktop precisam de backups de chave para compartilhar identidade.

Áreas excluídas das leituras rotineiras: builds, dependências instaladas, runtime legado e relatórios históricos privados. Consulte docs/plan.md para a etapa atual; este mapa não replica o histórico da sessão.

Desktop: desktop/main.cjs serve web/site via protocolo local studio://app, com secure context, renderer sandbox e origem estável para IndexedDB. desktop/protocol.cjs limita a resolução de caminhos à pasta empacotada. Empacotamento: em desktop/, npm run dist:linux ou npm run dist:windows. Testes: npm test e npm run test:app (sob xvfb-run no Linux headless). Arquivos legados foram arquivados em .private/cleanup-1.1/.
