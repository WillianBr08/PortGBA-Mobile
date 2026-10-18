# Validação — versão 1.1

Verificado em 18/10/2026. Testes locais usam arquivos sintéticos, sem distribuir jogos ou chaves pessoais.

| Área | Evidência |
| --- | --- |
| Android | Bases ARM64, ARMv7 e x86_64 compiladas também em checkout limpo após reorganizar as pastas. Teste Java de controles aprovado. |
| Empacotador | Cinco testes aprovados; fronteiras de 50.000.000 e 50.000.001 bytes no TypeScript e no Python. Build TypeScript/Vite aprovado. |
| APK real | Gerado no navegador com 50 MB: assinatura v2 aceita por apksigner, alinhamento de 16 KiB aceito por zipalign, tamanho e último byte da ROM preservados. |
| Fluxo web | Seleção, nome/logo, cancelamento, nova geração, download e aviso para ROM maior que 32 MiB. Layouts desktop, celular e tema claro inspecionados. |
| Privacidade | Tráfego observado na geração: somente leitura de assets/bases; nenhum envio da ROM ou chave. Auditoria da pasta pública aprovada. |
| Windows offline | Runner Windows 2022: geração offline, download, reinício e persistência de chave aprovados no desenvolvimento e no executável empacotado. EXE portátil publicado. |
| Site público | Geração/download de APK, backup/restauração pela interface, backup inválido, nome com acentos, logo automática e tema móvel aprovados. Sem erro JavaScript ou requisição com corpo; APK público validado por apksigner/zipalign. |
| Linux offline | AppImage compilado e extraído; runtime empacotado gerou e baixou APK sem rede, reiniciou e preservou a identidade de assinatura. |
| Segurança desktop | Protocolo rejeita origem/caminhos inválidos; renderer isolado sem Node e com sandbox. Teste automatizado aprovado. |
| Dependências | Auditorias npm de produção sem vulnerabilidades altas/críticas. |
| Visual | Revisão final de impeccable: SHIP, sem problemas materiais. |

Publicação em runners limpos aprovada: [Pages](https://github.com/WillianBr08/PortGBA-Mobile/actions/runs/39101759674) e [release Linux/Windows](https://github.com/WillianBr08/PortGBA-Mobile/actions/runs/39101760449). A [release v1.1.0](https://github.com/WillianBr08/PortGBA-Mobile/releases/tag/v1.1.0) contém os dois pacotes e os hashes SHA-256.

## Limites conhecidos

O limite de entrada é 50 MB decimal. O núcleo comum GBA endereça 32 MiB, portanto preservar um arquivo maior não assegura execução. Cabeçalho válido não garante compatibilidade de um hack. Não foi repetida nesta revisão uma instalação em aparelho Android físico, nem uma sessão completa de gameplay/save com Quetzal; assinatura, alinhamento, integridade e controles têm verificações próprias, mas não substituem esses testes de aparelho.

A chave é local a cada perfil/origem. Backups contêm material privado sem senha. A versão Windows portátil não possui assinatura comercial de editor. O AppImage pode precisar de FUSE ou da opção `--appimage-extract-and-run`.
