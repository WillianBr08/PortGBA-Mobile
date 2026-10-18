# GBA Studio
<!-- impeccable:product-schema 1 -->

## Platform
web

## Stack
HTML, CSS e TypeScript estáticos no mesmo projeto do gerador Linux, conforme o plano solicitado. Publicação por GitHub Pages; repositório de destino ainda não informado.

## Users
Pessoas que têm uma ROM GBA e querem criar um aplicativo Android próprio, sem conta e sem enviar a ROM para um servidor.

## Product Purpose
Selecionar uma ROM, gerar e baixar um APK com a interface atual do projeto e o núcleo mGBA.

## Capabilities and Constraints
ROM .gba de até 32 MiB; nome automático e opcional; logos Automática, Emerald, FireRed e GBA. Montagem em Web Worker, alinhamento de 16 KiB e assinatura APK v2. Chave por navegador com backup e restauração. Identificadores exclusivos para APKs web. ROMs, saves e chaves privadas não são publicados. Saves e estados são importados pelo menu Android.

## Brand Commitments
GBA Studio, português, preservar os controles e configurações do aplicativo existente.

## Evidence on Hand
Interface Android em app/, núcleo em vendor/mgba/, gerador Linux em tools/portar_gba.py. ROMs pessoais ficam fora dos arquivos publicados.
