# Plano e retomada

Etapa atual: REVIEW → SHIP. DEFINE e PLAN concluídos. Spec: [spec.md](spec.md). Design confirmado: refazer do zero preservando referências GBA; usuário delegou os detalhes visuais.

1. **repository** — arquivar runtime/relatórios sem uso, mGBA como dependência obtida por commit fixado, fontes e comandos organizados. Aceite: Git enxuto e build Android sem dependências ocultas.
2. **file-limit** — testes vermelhos para 50 MB, validador compartilhado na UI/Worker e equivalente Python. Aceite: fronteiras, aviso de compatibilidade, bytes preservados.
3. **generator-ui** — fundo índigo, área clara, prévia, geração/cancelamento/backup e downloads. Aceite: desktop/celular, teclado, contraste e fluxo real.
4. **desktop** — protocolo local seguro com origem estável, AppImage Linux x64 e EXE Windows x64. Aceite: iniciar, gerar APK offline, chave persistir e auditoria de pacote.
5. **delivery** — README visual, mapa, workflows, release 1.1.0 e Pages. Aceite: artefatos públicos, CI verde, site público testado.

Ordem: 1 → 2 → 3 → 4 → 5. As validações acompanham cada fatia. Backups privados não são distribuídos. Rollback: reverter os commits de mudança e republicar Pages; releases anteriores permanecem disponíveis.

Progresso: limpeza e núcleo fixado implementados; patch de I/O anterior preservado; Android recompilado. Teste 50 MB vermelho antes da mudança e verde depois (cinco testes). Novo visual revisado (impeccable: SHIP). AppImage compilado e runtime empacotado verificado offline, inclusive persistência de chave. README e documentação reescritos. Evidências em docs/testing.md. Próximo: validação em checkout limpo, commit e publicação; Windows depende do runner nativo.
