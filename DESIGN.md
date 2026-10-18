---
name: GBA Studio
description: Ferramenta de bancada para levar uma ROM GBA ao Android.
colors:
  paper: "#eef2f4"
  white: "#fff"
  ink: "#18374b"
  muted: "#4c6372"
  line: "#becbd3"
  shell: "#d7e1e7"
  accent: "#176452"
  error: "#a02e35"
  gold: "#a28b54"
  dragging: "#bbd7ca"
  label-hover: "#f9fcfb"
typography:
  display:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "clamp(36px, 4.1vw, 50px)"
    fontWeight: 700
    lineHeight: 1.09
    letterSpacing: "-.03em"
  headline:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "24px"
    fontWeight: 700
    letterSpacing: "-.02em"
  body:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "16px"
    fontWeight: 700
rounded:
  control: "6px"
  shell: "12px"
  cartridge-corner: "26px"
spacing:
  base: "8px"
  field-gap: "25px"
  column-gap: "76px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.white}"
    rounded: "{rounded.control}"
    padding: "15px 20px"
    width: "100%"
  button-primary-hover:
    backgroundColor: "{colors.accent}"
  button-download:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.white}"
    rounded: "{rounded.control}"
    padding: "15px 20px"
  button-secondary:
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "10px 14px"
  input:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "12px 14px"
    width: "100%"
  cartridge:
    backgroundColor: "{colors.shell}"
    textColor: "{colors.ink}"
    padding: "16px 18px 0"
---

# Design System: GBA Studio

## Overview

**Creative North Star: "Operate"**

Operate: uma ferramenta de bancada que transforma a seleção da ROM em um gesto de inserir um cartucho. Prata azulada, tinta azul profunda e contatos dourados dão materialidade à interface; o verde marca processamento local, foco e conclusão.

A composição é calma e direta, com tipografia Outfit local, superfícies planas e hierarquia clara entre instrução, formulário e ajuda. A identidade visível é GBA Studio, em português; PRODUCT.md continua sendo a autoridade de fatos do produto.

**Key Characteristics:**
- Bancada clara com um cartucho como elemento de identidade.
- Texto escuro e verde funcional sobre superfícies frias.
- Densidade moderada, ajuda contextual e controles largos.

A documentação descreve a implementação direta do plano confirmado. Não há comp visual aprovado registrado. Fontes: `web/src/style.css` e `web/index.html`; capturas finais: `output/playwright/desktop-final.png` e `output/playwright/mobile-final.png`. A revisão final informou “ship”, sem bloqueio material; isso não estabelece uma auditoria completa de acessibilidade.

## Colors

A paleta normativa está no frontmatter. Primária: ink para texto e ação de gerar, accent para estados funcionais. Secundária: gold restrita aos contatos físicos do cartucho. Neutros: paper no fundo, shell no casco, white nos campos e etiqueta, muted nos apoios e line nos separadores. Error identifica falhas; dragging e label-hover são estados da seleção de ROM.

**The Functional Green Rule.** O verde identifica foco, processamento local, progresso e download; a ação de gerar começa na tinta escura.

A cor de navegador em `web/index.html` é (#25485e); é metadado do navegador e não uma cor de superfície do CSS.

## Typography

Outfit é carregada localmente em Regular (400) e Bold (700), com Trebuchet MS e sans-serif de reserva. O título usa a escala fluida do frontmatter; na largura compacta passa a (38px), com largura máxima de (18ch). A introdução usa (19px) no desktop e (17px) no compacto. Rótulos e textos de ação são fortes; auxiliares ficam entre (12px) e (15px). Não há fonte monoespaçada: o identificador da chave usa numerais tabulares.

## Layout

Cabeçalho, conteúdo e rodapé compartilham largura máxima de (1160px). O conteúdo tem duas colunas na proporção (1fr / 1.08fr), com espaço amplo entre introdução e bancada. No desktop, o padding principal é (64px 36px 40px). A chave aparece abaixo da bancada; a ajuda de saves cruza ambas as colunas, com um separador fino.

No breakpoint (760px), o conteúdo vira uma coluna e usa padding (32px 22px). A sequência é introdução, formulário, chave e ajuda. A lista redundante de etapas e o texto auxiliar do cabeçalho são ocultados; controles mantêm a largura disponível. O rodapé pode quebrar linha. A interface é uma única superfície responsiva, sem navegação de múltiplas telas.

## Elevation & Depth

Não há box-shadow. A profundidade vem do contraste tonal entre fundo, casco e etiqueta, dos separadores e do desenho dos contatos. O foco usa contorno verde de (3px) com afastamento de (5px); a restauração de chave usa afastamento de (3px). Isso comunica interação, sem simular elevação.

## Shapes

Campos e botões têm cantos contidos. O cartucho usa o raio de casco em três cantos e o canto inferior direito ampliado, como assinatura. O símbolo da marca também usa uma silhueta assimétrica. Pontos e marcadores das etapas são circulares; eles indicam estado ou sequência, sem introduzir componentes de chip.

## Components

### Buttons

A ação primária é escura, larga, com texto claro em (18px), peso (700) e seta à direita. Hover passa ao verde em (.16s ease). Desabilitado usa opacidade (.5), cursor padrão e conserva o fundo escuro no hover. O download usa a mesma forma, com fundo verde. Ações de chave são secundárias, com borda fina, fundo transparente, texto (14px) e altura mínima (44px); hover usa shell.

### Inputs / Fields

Campos de texto e seleção têm fundo branco, borda de (1px), altura mínima de (50px) e texto (16px). Rótulos destacam o nome do campo, com “opcional” menor e regular. Placeholder conserva muted com opacidade total. O foco visível é compartilhado; mensagens de erro têm texto próprio e a cor error. A geração exibe progresso fino (7px) e mensagens de estado.

### Cartridge

**The Cartridge Rule.** A área de ROM concentra a metáfora física: casco, etiqueta clara e contatos; os demais campos permanecem controles simples.

O casco envolve uma etiqueta branca com altura mínima de (168px), ícone simples, nome forte e instrução auxiliar. Hover clareia a etiqueta; arrastar muda o casco para dragging em (.2s ease). Focus-within aplica o contorno verde. Os contatos usam faixas douradas (6px) intercaladas por casco (4px), sem imagem externa. O nome da ROM quebra palavras para acomodar arquivos longos.

### Header and supporting details

A marca tem símbolo geométrico e texto forte (23px); o cabeçalho usa separador fino. A chave usa o elemento nativo details/summary, com alvo mínimo de (44px), descrição e ações de backup. A ajuda de saves tem título (24px), texto (15px) e separador. Preserve a semântica desses elementos ao estender a interface.

Todas as transições e animações são removidas com `prefers-reduced-motion: reduce`.

## Do's and Don'ts

### Do:
- Do preservar a hierarquia entre introdução, formulário e ajuda.
- Do manter os focos visíveis e respeitar movimento reduzido.
- Do usar o dourado nos contatos do cartucho.

### Don't:
- Don't substituir Outfit local por fontes externas.
- Don't esconder erros apenas em cor: preserve a mensagem textual.
- Don't transformar a ajuda e a chave em cartões com elevação decorativa.
