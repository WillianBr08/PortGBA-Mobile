---
name: GBA Studio
description: Do cartucho ao Android, com a identidade índigo do GBA.
colors:
  canvas: "#282344"
  foreground: "#fcfaff"
  secondary: "#c7c1df"
  divider: "#50496d"
  surface: "#fffdfd"
  ink: "#302b48"
  muted: "#68627c"
  line: "#ded9e9"
  soft: "#f4f1fa"
  indigo: "#6250bd"
  indigo-hover: "#4c3c9f"
  yellow: "#f5ca7b"
  success: "#25674c"
  error: "#a13b45"
  device: "#8772cf"
  device-edge: "#5c499b"
  light-canvas: "#ece8f8"
  light-secondary: "#655b82"
  light-divider: "#c9bfdc"
typography:
  display:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "clamp(43px, 4.6vw, 62px)"
    fontWeight: 700
    lineHeight: 1.06
    letterSpacing: "-0.035em"
  title:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "25px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.025em"
  body:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "Outfit, 'Trebuchet MS', sans-serif"
    fontSize: "13px"
    fontWeight: 700
rounded:
  panel: "16px"
  drop-zone: "12px"
  action: "8px"
  field: "7px"
spacing:
  small: "8px"
  medium: "16px"
  section: "24px"
  panel: "32px"
  wide: "48px"
components:
  button-primary:
    backgroundColor: "{colors.indigo}"
    textColor: "#fff"
    rounded: "{rounded.action}"
    padding: "14px 20px"
    width: "100%"
  button-primary-hover:
    backgroundColor: "{colors.indigo-hover}"
  button-download:
    backgroundColor: "{colors.success}"
    textColor: "#fff"
    rounded: "{rounded.action}"
    padding: "14px 20px"
  button-secondary:
    textColor: "{colors.foreground}"
    rounded: "{rounded.field}"
    padding: "10px 12px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.field}"
    padding: "11px 12px"
    height: "48px"
  generator-panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.panel}"
    padding: "32px"
---

# Design System: GBA Studio

## Overview

**Creative North Star: "Do cartucho ao Android"**

O GBA índigo orienta a identidade: fundo profundo, tipografia arredondada e um console geométrico com a prévia do aplicativo. A área branca concentra a tarefa de selecionar a ROM e gerar o APK.

O tema claro troca o entorno por lavanda e conserva a superfície do formulário. O tom é próximo, em português; Outfit é local. PRODUCT.md é a autoridade para capacidades e limites do produto.

**Key Characteristics:**
- Índigo e lavanda com amarelo pontual.
- Formulário claro e controles largos.
- Console geométrico como prévia no desktop.

Documentação da implementação direta; não há comp aprovado registrado. Fontes: `web/src/style.css`, `web/index.html` e `PRODUCT.md`. Capturas: `output/playwright/studio-desktop-final.png`, `studio-mobile-final.png` e `studio-light-settled.png` no mesmo diretório. Parecer final recebido: ship, sem bloqueio material; não equivale a auditoria completa de acessibilidade.

## Colors

O frontmatter contém as cores normativas. Primária: indigo em gerar, foco do formulário e progresso; indigo-hover reforça a ação. Secundária: yellow destaca o título no tema escuro e o foco fora do formulário. Success identifica o download do APK; error acompanha mensagens de falha.

Os neutros canvas, foreground, secondary e divider organizam o entorno escuro. Surface, ink, muted, line e soft organizam o formulário. Device e device-edge dão volume ao console. No tema claro, light-canvas, ink, light-secondary e light-divider substituem os equivalentes do entorno; o destaque do título e o foco passam a indigo. A superfície branca permanece.

## Typography

Outfit local em Regular (400) e Bold (700), com Trebuchet MS e sans-serif de reserva. Display e título do painel seguem os papéis do frontmatter. A introdução usa (18px / 1.65); auxiliares variam de (11px) a (15px). Ações principais usam (16px, 700). No mobile, o título principal passa a (42px / 1.07), a introdução a (16px) e os campos a (16px). Identificador da chave usa numerais tabulares.

## Layout

Conteúdo limitado a (1184px), com (40px) de margem lateral. A seção inicial tem duas colunas (0.95fr / 1.05fr), gap de (72px) e padding vertical de (58px 40px). Introdução e console ficam à esquerda; formulário à direita. Chave e saves dividem a faixa seguinte; downloads vêm abaixo, também em duas colunas.

Até (1000px), margens caem para (24px), gaps para (36px), painel para (26px) de padding e campos se empilham. Até (720px), margens são (18px), as seções usam uma coluna e o painel usa (24px 20px). Console, legenda e reforço local são ocultados; formulário, chave, saves e downloads continuam disponíveis. O primeiro link da navegação é ocultado, preservando GitHub e tema.

## Elevation & Depth

O painel branco usa sombra difusa (0 24px 60px #120c292b). O console usa uma borda projetada (0 12px 0 var(--device-edge)) e sombra (0 34px 45px #110d2e66). Sua inclinação é fixa (-6deg); não há animação de flutuação. O entorno recebe uma luz radial discreta. Chave e downloads usam bordas e contraste tonal.

Foco visível usa contorno de (3px), afastado (5px), amarelo no entorno escuro e índigo no claro e no formulário. A área de ROM usa afastamento (4px); restauração usa (3px).

## Shapes

Painel com cantos suaves; área de ROM e links de plataforma com cantos intermediários; campos e ações mais contidos. O botão de tema é circular (44px). A silhueta do console usa cantos inferiores ampliados (38px 38px 78px 78px), tela escura e botões geométricos. A área de upload é retangular, com borda tracejada, sem o cartucho usado no sistema anterior.

## Components

### Buttons

Gerar APK é índigo, com altura mínima (54px), texto forte e seta. Hover escurece; disabled usa fundo (#8e84ae) e opacidade (.5). Download do APK é verde. Ações secundárias de chave têm contorno, fundo transparente e altura mínima (44px); hover usa divider. Ações pressionadas deslocam (1px) para baixo. Tema usa botão circular com nome acessível e estado pressionado.

### Inputs / Fields

Nome e logo têm altura (48px), fundo surface, borda line e rótulos explícitos. A seleção da ROM tem fundo soft, borda tracejada de (1.5px), ícone e instrução; hover e arraste usam (#eae4f8) e borda indigo. O input de arquivo transparente cobre o alvo. O nome quebra palavras longas. Erros usam mensagem textual; avisos de compatibilidade usam fundo (#fff3d8). Status é anunciado e o progresso tem altura (6px).

### Cards / Containers

O painel de geração é a superfície elevada principal. Os links de download Linux e Windows usam contorno, ícone, nome e descrição, com altura mínima (85px), reduzida para (82px) no mobile; hover usa divider. O limite de arquivo é um pequeno rótulo informativo, sem comportamento de chip selecionável.

### Navigation

Cabeçalho com marca de console, link para downloads, GitHub e tema. Links têm alvo mínimo (44px); texto secundário ganha foreground no hover. Rodapé repete marca e acesso ao código. Preserve skip link para o gerador.

### Preview and key

A prévia mostra logo e título na tela do console, com truncamento para nomes longos. A chave usa details/summary nativo; abertura gira a seta (180deg), revelando backup e restauração. Não substitua a semântica desses controles ao estender a interface.

Mudanças de estado duram entre (.15s) e (.2s); a área de ROM usa cubic-bezier(0.2, 0.8, 0.2, 1). `prefers-reduced-motion: reduce` remove animações e transições e desativa rolagem suave.

## Do's and Don'ts

### Do:
- Do preservar o contraste entre o entorno e o formulário claro.
- Do manter foco visível, mensagens textuais e movimento reduzido.
- Do usar Outfit local e atualizar a prévia com nome e logo.

### Don't:
- Don't recuperar o fundo anterior de bancada azulada e verde.
- Don't substituir o formulário por elementos decorativos do console.
- Don't ocultar erros ou limitações de compatibilidade apenas em cor.
