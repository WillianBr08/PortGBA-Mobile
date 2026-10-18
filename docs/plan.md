# Plano e retomada

Etapa atual: PLAN. Spec: [spec.md](spec.md). Design confirmado: refazer do zero preservando referências GBA; usuário delegou os detalhes visuais.

1. **repository** — arquivar runtime/relatórios sem uso, mGBA submódulo fixado, fontes e comandos organizados. Aceite: Git enxuto e build Android sem dependências ocultas.
2. **file-limit** — testes vermelhos para 50 MB, validador compartilhado na UI/Worker e equivalente Python. Aceite: fronteiras, aviso de compatibilidade, bytes preservados.
3. **generator-ui** — fundo índigo, área clara, prévia, geração/cancelamento/backup e downloads. Aceite: desktop/celular, teclado, contraste e fluxo real.
4. **desktop** — protocolo local seguro com origem estável, AppImage Linux x64 e EXE Windows x64. Aceite: iniciar, gerar APK offline, chave persistir e auditoria de pacote.
5. **delivery** — README visual, mapa, workflows, release 1.1.0 e Pages. Aceite: artefatos públicos, CI verde, site público testado.

Ordem: 1 → 2 → 3 → 4 → 5. As validações acompanham cada fatia. Backups privados não são distribuídos. Rollback: reverter os commits de mudança e republicar Pages; releases anteriores permanecem disponíveis.
