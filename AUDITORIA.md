# Auditoria do projeto

Leitura de todos os arquivos de código (106 classes Java) e recursos (46 layouts, valores e
desenhos), mais build, testes e lint (Fase 0). Nenhum travamento (crash) encontrado por leitura:
nulos da API, erros de rede e o ciclo de vida das telas já eram tratados. Os problemas abaixo
são de comportamento sutil, acessibilidade, duplicação e consistência visual.

Gravidade: **crítico** (trava ou perde progresso) · **alto** (comportamento errado provável) ·
**médio** (acessibilidade, manutenção ou visual visível) · **baixo** (detalhe).

## Bugs e comportamento

| # | Arquivo | Problema | Gravidade | Correção | Status |
|---|---|---|---|---|---|
| B1 | game/PlayerStore.java | `change()` recria o objeto do save (lê o JSON de novo) sempre que a mudança "não muda nada" — isso acontece em todo `onResume` da tela inicial (`ensureDailyChallenges`). Telas que guardam o herói ou o estado ficam com uma cópia velha, e são 2 conversões de JSON a mais na thread principal | Alto | Só desfazer quando a mudança alterou algo (comparar o JSON de antes e depois) | Corrigido |
| B2 | ui/ranks/ChestOpenActivity.java | Usa `announceForAccessibility`, obsoleta no Android 16 (aviso do compilador) | Médio | Anunciar o prêmio pela "região viva" do card | Corrigido |
| B3 | ui/ranks/RankTrailActivity.java | O pulso infinito dos baús continua rodando com o app em segundo plano (gasta bateria) | Baixo | Parar em `onStop` (a tela religa ao voltar) | Corrigido |
| B4 | data/repository/CharacterRepository.java | Equipe não encontrada mostra "Personagem não encontrado." | Baixo | Mensagem própria para equipe | Corrigido |
| B5 | game/PlayerState.java | `repair()` não remove nulos e repetidos de `artifacts` e `openedChests` (save editado à mão conta artefato duas vezes) | Baixo | Remover nulos e repetidos, mantendo a ordem | Corrigido |
| B6 | view_player_hud.xml | A pílula da patente é clicável, mas tem 36 dp de altura (toque < 48 dp) | Médio | Área de toque de 48 dp com o mesmo visual | Corrigido |
| B7 | view_search_bar.xml | Botão "X" da busca com 36 dp | Médio | 48 dp | Corrigido |
| B8 | activity_artifacts, activity_battle, activity_character_detail, activity_survival, activity_team_page | Botões Voltar/Fechar com 44 dp | Médio | 48 dp | Corrigido |
| B9 | item_market_hero, item_my_hero, item_artifact, item_rank_card, item_upgrade_row, view_daily_challenges, activity_my_heroes | Botões com altura mínima de 34 a 44 dp e sem margem de toque | Médio | Altura mínima de 48 dp | Corrigido |
| B10 | values/colors.xml | `text_secondary` (#747474) tem contraste 4,1:1 sobre os cards (#0F0F0F), abaixo de 4,5:1 | Médio | Clarear a cor | Corrigido |
| B11 | values/strings.xml | 5 textos com número + palavra sem plural (lint PluralsCandidate: "1 moedas", "1 turnos") | Baixo | Usar `plurals` onde a quantidade varia | Corrigido (1 plural real; 5 falsos positivos marcados) |
| B12 | game/PlayerState.java | Comentário diz "0 = Recruta ... 9 = Entidade Cósmica" (agora são 17 patentes) | Baixo | Atualizar | Corrigido |
| B13 | drawable/ic_chest_lid.xml, drawable/ic_home.xml | Recursos sem uso (lint UnusedResources) | Baixo | Mover para `_to_delete/` | Corrigido |
| B14 | game/AttributeCalculator.java | `clamp` usa 100 fixo em vez de `GameBalance.MAX_ATTRIBUTE` | Baixo | Usar a constante (mesmo valor) | Corrigido |
| B15 | view_artifact_slot.xml / ArtifactsActivity | Espaço de artefato equipado é clicável (remove), mas não dá retorno visual ao toque | Baixo | Ripple no espaço | Conferido (já tinha ripple) |
| B16 | activity_survival.xml | Botão "Tentar de novo" do próximo inimigo usa o estilo dos botões de depuração: 34 dp de altura, sem margem de toque, com cara de botão de teste | Médio | Botão secundário do sistema com toque de 48 dp | Corrigido (achado na Fase 5) |
| B17 | activity_album.xml / AlbumActivity | Quando nenhuma equipe carrega, o aviso de erro aparece por cima das 12 linhas "não carregou" da lista (textos sobrepostos) | Baixo | Esconder a lista enquanto o aviso aparece | Corrigido (achado na Fase 5) |

## Thread principal, rede e memória

| # | Arquivo | Problema | Gravidade | Correção | Status |
|---|---|---|---|---|---|
| T1 | game/PlayerStore.java | Gravação do save com `commit()` síncrono na thread principal | Médio | Manter: o save precisa estar gravado antes da animação (prêmio nunca se perde). O JSON é pequeno, e com B1 há menos conversões | Mantido (decisão) |
| T2 | (todas as telas) | Rede: todas as chamadas usam `enqueue` do Retrofit (fora da thread principal), com cache e tratamento de erro | — | Nada a fazer | Conferido |
| T3 | (todas as telas) | Animações infinitas e timers: todos são cancelados em `onDestroy`/`onPause` (batalha, baú, roleta, trilha, splash, busca, desafios) | — | Nada a fazer (exceto B3) | Conferido |

## Código duplicado e classes grandes

| # | Arquivo | Problema | Gravidade | Correção | Status |
|---|---|---|---|---|---|
| D1 | 14 Activities | `EdgeToEdge.enable(...)` + ajuste das barras do sistema repetidos | Médio | Helper comum `Screens` | Corrigido |
| D2 | 11 lugares | Glide com placeholder, erro e fallback repetidos | Baixo | Helper comum `Images` | Corrigido |
| D3 | CharacterAdapter, MarketAdapter | Rodapé de lista (carregando, erro, "carregar mais") duplicado | Baixo | Classe comum `ListFooter` | Corrigido |
| D4 | MarketActivity, MarketAdapter | Formatação de número pt-BR repetida | Baixo | Usar `PlayerHud.format` | Corrigido |
| D5 | game/Chests.java, game/Roulette.java | Sorteio ponderado idêntico nos dois | Baixo | `WeightedDraw` | Corrigido |
| D6 | 6 layouts | Bloco da navegação inferior repetido | Baixo | `include` de `view_bottom_nav.xml` | Corrigido |
| D7 | activity_main, activity_market, activity_album, activity_team_page, activity_character_detail, activity_battle | 6 versões diferentes do aviso de vazio/erro | Médio | Componente `view_state.xml` + `StateView` | Corrigido (view_state + StateView em 6 telas: lista, detalhe, batalha, Mercado, Álbum, página da equipe) |
| C1 | MainActivity.java (503 linhas) | Lista, busca, filtros, desafios, card da Sobrevivência e herói ativo na mesma classe | Médio | Extrair o topo (desafios + sobrevivência + contagem) para `DailyHeaderController` | Corrigido (503 → 437 linhas) |
| C2 | MarketActivity.java (477 linhas) | Estoque de heróis + roletas na mesma classe | Médio | Extrair a aba de roletas para `RoulettePanel` | Corrigido (477 → 386 linhas) |
| C3 | game/PlayerState.java (529 linhas) | Muitas regras num só lugar | Baixo | Manter: os campos precisam ficar nesta classe para o formato do save não mudar; as regras já delegam para Chests, Survival, DailyChallenges e TeamAlbum | Mantido (decisão: formato do save) |
| C4 | BattleActivity, CharacterDetailActivity, SurvivalActivity, ChestAnimator (400–465 linhas) | Classes grandes, mas cada uma cuida de uma tela ou animação só | Baixo | Reduzir com os helpers D1, D2 e D7 | Reduzido pelos helpers |

## Números fora das constantes

| # | Arquivo | Problema | Gravidade | Correção | Status |
|---|---|---|---|---|---|
| N1 | RankTrailActivity, TeamPageAdapter, BattleActivity, RouletteSpinActivity, AlbumAdapter, TeamPageAdapter | Durações e escalas de animação de interface soltas no código (450, 550, 650, 900, 200 ms; 1,1×; 0,45) | Baixo | Tokens de movimento num lugar só (`Motion`) | Corrigido |
| N2 | game/AttributeCalculator.java | Números da fórmula de atributos ficam na própria classe | — | Manter: o enunciado pede a fórmula numa única classe | Conferido |

## Inconsistências visuais

| # | Onde | Problema | Gravidade | Correção | Status |
|---|---|---|---|---|---|
| V1 | Todas as telas | 15 valores diferentes de margem lateral (20 dp o mais comum), fora da grade de 8 dp | Médio | Tokens de espaçamento 4/8/16/24/32 e margem de tela de 16 dp | Corrigido (tokens em todos os layouts; margem de tela 16 dp; só a cena animada do baú mantém posições próprias) |
| V2 | Todas as telas | 22 tamanhos de texto diferentes (11 a 56 sp) | Médio | Escala tipográfica (`TextAppearance.Marvel.*`) | Corrigido (escala de 7 tamanhos + Bebas 26/34/44; ficaram de fora só números gigantes e rótulos pequenos em negrito) |
| V3 | Cards | Cantos de 16 e 18 dp, sem borda; cards feitos com `bg_card` diferentes dos MaterialCardView | Baixo | Estilo único de card: canto 16 dp + borda sutil de 1 dp | Corrigido (card padrão no tema, canto 16 dp + borda de 1 dp; destaque vermelho por Cards.highlight) |
| V4 | Lista, Mercado, Álbum | Carregamento com spinner ou "Carregando…"; sem skeleton | Médio | Skeletons nas listas | Corrigido (skeleton na lista, Mercado, detalhe e página da equipe; o Álbum mantém a linha "Carregando…" de cada equipe) |
| V5 | Ícones | Mistura de Material Icons com desenhos próprios (baú, selo) | Baixo | Manter: os desenhos próprios seguem o mesmo traço cheio | Conferido |
| V6 | Seleção de oponente | A tela não existe mais: desde a especificação de progressão, a batalha é contra o personagem aberto no detalhe | — | Nada a redesenhar | Conferido |

## Situação final

17 bugs (B1–B17): 16 corrigidos, 1 já estava certo (B15). Dos outros 22 itens: 11 corrigidos ou
reduzidos (D1–D7, C1, C2, C4, N1), 5 conferidos sem nada a fazer (T2, T3, N2, V5, V6),
2 mantidos por decisão (T1, C3) e V1–V4 corrigidos no redesenho. Nenhum item pendente.
Detalhes das decisões em RELATORIO.md.
