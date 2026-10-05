# Progresso da revisão completa

Atualizado ao fim de cada item. Se a sessão cair, retomar a partir do primeiro item sem ✅.

## Fase 0 — Rede de segurança ✅
- ✅ Ponto de volta: `_backup/antes-da-refatoracao.zip` (255 arquivos, 5,4 MB, sem `local.properties`
  e sem pastas `build/`). Não usei commit porque o repositório git é a pasta do usuário inteira
  (tem `.aws` e arquivos pessoais já preparados para commit).
- ✅ Estado inicial (`gradlew clean assembleDebug testDebugUnitTest lintDebug`):
  - Build: **passou**. Testes: **113 de 113** passando.
  - Aviso do compilador: `ChestOpenActivity` usa uma API obsoleta.
  - Lint (24 avisos, 0 erros): NotifyDataSetChanged ×7, PluralsCandidate ×6, MergeRootFrame ×3,
    NewerVersionAvailable ×3, GradleDependency ×2, UnusedResources ×2, AndroidGradlePluginVersion ×1.

## Fase 1 — Auditoria ✅
- ✅ Li os 106 arquivos Java e os recursos. AUDITORIA.md: 15 bugs (B1–B15), 3 itens de thread e
  memória (T1–T3), 11 de duplicação e classes grandes (D1–D7, C1–C4), 2 de números (N1–N2) e
  6 visuais (V1–V6). Nenhum crítico.

## Fase 2 — Bugs ✅
- ✅ Críticos: nenhum encontrado. Alto: B1 (save recriado sem mudança).
- ✅ Médios e baixos: B2–B14 corrigidos; B15 já estava certo; T1 mantido (decisão).
- ✅ Build passou, testes 113/113, lint 24 → 16 avisos (sobraram versões de dependência,
  NotifyDataSetChanged e MergeRootFrame, tratados na Fase 3/5).
- ✅ `ic_chest_lid.xml` e `ic_home.xml` movidos para `_to_delete/app/src/main/res/drawable/`.

## Fase 3 — Refatoração ✅ (build ok, 113/113, lint sem avisos novos)
- ✅ D5 `game/WeightedDraw` (baús e roletas; mesma sequência de sorteio, simulações iguais)
- ✅ D2 `ui/common/Images` (10 blocos Glide → 3 métodos)
- ✅ D4 números formatados por `PlayerHud.format` (3 formatadores locais removidos)
- ✅ D1 `ui/common/Screens` (EdgeToEdge em 12 telas, padding das barras em 8)
- ✅ D6 `layout/view_bottom_nav.xml` (5 cópias → 1)
- ✅ N1 `ui/common/UiTokens` (durações, escalas e transparências de interface)
- ✅ D3 `ui/common/ListFooter` (rodapé da grade e do Mercado)
- ✅ C1 `ui/daily/DailyHeader` (MainActivity 503 → 437 linhas)
- ✅ C2 `ui/market/RoulettePanel` (MarketActivity 477 → 386 linhas)
- ✅ C3 mantido (campos do save ficam no PlayerState); C4 reduzido pelos helpers
- ➡ D7 componente de vazio/erro: definido na Fase 4, aplicado na Fase 5

## Fase 4 — Sistema visual ✅ (build ok)
- ✅ Cores: tokens novos `card_stroke`, `skeleton`; `text_secondary` com contraste ≥ 5:1 (Fase 2)
- ✅ Espaçamento/cantos/tamanhos em `dimens.xml` (grade de 8 dp, margem de tela 16 dp, cantos
  8/12/16 dp, toque 48 dp, botão principal 56 dp)
- ✅ Tipografia em `type.xml` (Bebas: 44/34/26; texto: 18/16/14/13/12 + rótulo em caixa alta)
- ✅ Componentes em `components.xml` (Card, Button.Compact, Button.Outlined, IconButton,
  ScreenTitle, ScreenSubtitle, Pill, Pill.Accent, SkeletonBlock) + card padrão no tema
- ✅ Estados: `view_state.xml` + `StateView` (vazio/erro); `view_skeleton_grid.xml`,
  `view_skeleton_list.xml` + `Skeleton` (carregando)

## Fase 5 — Redesenho tela por tela ✅
- ✅ Componentes comuns (topo, moedas, busca, rodapé de lista, estilos antigos alinhados) — build ok
- ✅ Splash — nada a aplicar (vídeo em tela cheia, fundo preto)
- ✅ Lista de personagens — build ok
- ✅ Detalhes — skeleton das seções no carregamento, erro com o componente comum — build ok
- ✅ Seleção de oponente — não existe mais (o oponente é sorteado na própria batalha); nada a aplicar
- ✅ Batalha e resultado (mesma tela) — tokens, erro com o componente comum — build ok
- ✅ Mercado (Heróis e Artefatos/roletas) — skeleton no 1º lote, vazio/erro com ícone, botão
  Comprar compacto, tela do giro e card de prêmio com tokens — build ok
- ✅ Meus Heróis — `ui/common/Cards.highlight` (destaque vermelho ou borda sutil padrão, também
  usado na Trilha e em Artefatos), botões Melhorar (compacto) e Usar/Artefatos (secundário) — build ok
- ✅ Trilha — título/subtítulo do sistema, card com rótulo e botão Abrir compacto, troféus com a
  escala de texto — build ok
- ✅ Abertura de baú — só tipografia e margens (a cena animada não mudou de tamanho); costas do card
  com o mesmo canto da frente (16 dp) — build ok
- ✅ Artefatos — botão voltar e fechar (batalha) com o estilo de ícone, título do sistema, botão
  Equipar compacto — build ok
- ✅ Álbum — título e contador de selos com os estilos do sistema, pílula "Resgatar", aviso de
  erro com o componente comum (a lista some enquanto o aviso aparece, antes ficavam sobrepostos);
  página da equipe com skeleton no carregamento e erro com o componente comum — build ok
- ✅ Desafios do Dia — card com o padrão do sistema, botão Abrir baú compacto, ícones de 24 dp
  alinhados com a dica e a barra, card da Sobrevivência com pílula "Jogar" — build ok
- ✅ Sobrevivência — título do sistema, pílula de poder do inimigo, "Tentar de novo" deixou de usar
  o estilo de depuração (34 dp) e virou botão secundário de 48 dp — build ok
- ✅ Outras partes — barra de depuração, filtro de famílias (margem 16 dp igual ao topo),
  MergeRootFrame ignorado com motivo nos 3 layouts; tokens `card_padding`/`icon_sm`/`icon_lg` em uso
  — build, 113/113 testes, lint 13 avisos (só versões de dependência e NotifyDataSetChanged)

## Fase 6 — Verificação final ✅
- ✅ `gradlew clean assembleDebug testDebugUnitTest lintDebug`: build **passou**; testes **113 de 113**
  (inclui simulações de batalha, roletas, baús e Sobrevivência); lint 0 erros e 13 avisos
  (6 versões de dependência, 7 NotifyDataSetChanged — ver RELATORIO.md, "Decisões")
- ⚠ Capturas de tela: não feitas (emulador travado "offline"; a permissão para reiniciá-lo foi negada)
- ✅ `entrega/MarvelBattle.zip` gerado de novo e conferido: 264 arquivos, sem chave, sem `build/`,
  sem `local.properties`, sem `_backup/`, `_to_delete/` e sem os relatórios da revisão
- ✅ AUDITORIA.md com status final de todos os itens; RELATORIO.md escrito
