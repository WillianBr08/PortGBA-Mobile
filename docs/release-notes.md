GBA Studio 1.1.0 traz um site reconstruído com identidade inspirada no GBA e versões desktop offline.

- Nova bancada responsiva, prévia do aplicativo, temas claro/escuro e cancelamento.
- Entrada `.gba` de até 50 MB, com aviso para arquivos acima do mapeamento comum de 32 MiB do núcleo.
- AppImage Linux x64 e EXE portátil Windows x64; bases Android incluídas, sem SDK ou conta.
- Chave local com backup/restauração, assinatura APK v2 e alinhamento de 16 KiB.
- Código organizado por plataforma e núcleo mGBA obtido por revisão fixada.

A ROM é preservada no APK e processada localmente. Arquivos acima de 32 MiB podem não funcionar no núcleo. Use o menu Saves do aplicativo Android para importar seu progresso. O EXE não possui assinatura comercial de editor.

Baixe o pacote da sua plataforma e confira `SHA256SUMS.txt`. No Linux: `chmod +x GBA-Studio-linux-x64.AppImage` e execute; sem FUSE, use `--appimage-extract-and-run`.
