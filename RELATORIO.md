# Relatório da revisão completa

## Resumo

Fiz a revisão completa do app nas seis fases. A auditoria encontrou 17 bugs e 22 outros pontos,
todos com status final no AUDITORIA.md. Corrigi 16 bugs. Nenhum era crítico e um era alto: o
save era recriado sem necessidade. Os outros eram de acessibilidade, textos e detalhes. O código
repetido virou helpers comuns, e as duas maiores telas encolheram. Criei um sistema visual em
recursos: espaçamento, tipografia, card, botões, pílulas e os estados de carregando, vazio e erro.
Apliquei esse sistema em 13 telas. Nenhuma regra ou número do jogo mudou, e as chaves do save
são as mesmas. O build limpo passou, os 113 testes passaram (incluindo as simulações de batalha,
roletas, baús e Sobrevivência) e o lint terminou com 0 erros e 13 avisos, todos explicados abaixo.
Não vi o app rodando, então não sei se ficou bonito. A lista do que conferir está no fim.

## Bugs corrigidos

| # | O que era | Correção |
|---|---|---|
| B1 (alto) | `PlayerStore.change()` relia o save do JSON mesmo quando nada mudava. Isso acontecia em todo `onResume` da tela inicial, e as telas ficavam com cópias velhas do estado | Só desfaz quando a mudança alterou algo |
| B2 | `ChestOpenActivity` usava `announceForAccessibility`, obsoleta | O card do prêmio virou uma "região viva" que o leitor de tela anuncia |
| B3 | O pulso dos baús da Trilha continuava rodando com o app em segundo plano | Para em `onStop` |
| B4 | Equipe inexistente mostrava "Personagem não encontrado." | Mensagem própria para equipe |
| B5 | `repair()` não limpava nulos e repetidos de artefatos e baús abertos | Limpa, mantendo a ordem |
| B6 | A pílula da patente (clicável) tinha 36 dp | Área de toque de 48 dp com o mesmo visual |
| B7 | O "X" da busca tinha 36 dp | 48 dp |
| B8 | Os botões Voltar e Fechar tinham 44 dp | 48 dp |
| B9 | Botões dentro de cards tinham altura mínima de 34 a 44 dp | Toque de 48 dp em todos (botão compacto) |
| B10 | `text_secondary` tinha contraste de 4,1:1 | Cor mais clara, contraste ≥ 5:1 |
| B11 | "em 1 turnos" | Texto com `plurals`. Os 5 falsos positivos do lint ficaram marcados |
| B12 | Comentário do `PlayerState` citava 10 patentes | Atualizado para 17 |
| B13 | `ic_chest_lid.xml` e `ic_home.xml` sem uso | Movidos para `_to_delete/` |
| B14 | `clamp` usava 100 fixo | Usa `GameBalance.MAX_ATTRIBUTE` (mesmo valor) |
| B16 | Sobrevivência: "Tentar de novo" usava o estilo de depuração (34 dp) | Botão secundário de 48 dp |
| B17 | Álbum: o aviso de erro aparecia por cima das linhas "não carregou" | A lista some enquanto o aviso aparece |

## Refatorações

- `game/WeightedDraw`: o sorteio ponderado de baús e roletas agora é um só. A sequência de sorteios
  não mudou, e as simulações dão os mesmos números.
- `ui/common/Images`: 10 blocos de Glide viraram 3 métodos.
- `ui/common/Screens`: o código de edge-to-edge e das barras do sistema estava repetido em 12 telas e agora fica num lugar só.
- `ui/common/UiTokens`: durações, escalas e transparências da interface.
- `ui/common/ListFooter`: o rodapé da grade e do Mercado (skeleton, carregando, mensagem).
- `ui/common/StateView` e `Skeleton`: os avisos de vazio/erro e os blocos de carregamento, iguais em todas as telas.
- `ui/common/Cards`: o destaque de card (borda vermelha ou borda padrão) em Meus Heróis, Trilha e Artefatos.
- `ui/daily/DailyHeader`: tirado da `MainActivity`, que caiu de 503 para 437 linhas.
- `ui/market/RoulettePanel`: tirado da `MarketActivity`, que caiu de 477 para 386 linhas.
- `layout/view_bottom_nav.xml`: substitui 5 cópias da navegação inferior.
- `PlayerHud.format`: substitui 3 formatadores de número locais.

## Sistema visual

- **Espaçamento** (`dimens.xml`): grade de 8 dp (4/8/16/24/32), margem de tela de 16 dp e padding
  de card de 16 dp (12 dp no compacto).
- **Cantos**: 8, 12 e 16 dp. **Toque**: 48 dp. **Botão principal**: 56 dp. **Ícones**: 16, 24 e 40 dp.
- **Cores**: `card_stroke` (borda sutil) e `skeleton` são novas; `text_secondary` ficou mais clara (contraste).
- **Tipografia** (`type.xml`): Bebas Neue em 44, 34 e 26 sp para títulos. Fonte do sistema em
  18 e 16 sp (negrito), 16, 14 e 13 sp (texto), 12 sp (legenda) e 12 sp em caixa alta (rótulo).
- **Componentes** (`components.xml`):
  - Card padrão aplicado pelo tema a todos os MaterialCardView: fundo cinza-escuro, canto de 16 dp, borda de 1 dp, sem sombra, ripple vermelho.
  - Botão compacto (desenho de 40 dp, toque de 48 dp).
  - Botão secundário (contorno) e botão de ícone de 48 dp.
  - Título e subtítulo de tela.
  - Pílulas cinza e vermelha.
  - Bloco de skeleton.
- **Estados**:
  - `view_state.xml` + `StateView`: ícone, título opcional, mensagem e botão.
  - Skeletons em grade, lista e seções. Eles pulsam e param sozinhos quando a tela fecha.
- **Raridade e barras**: as cores de raridade e o estilo `AttributeBar` já existiam e continuaram iguais.

## Telas redesenhadas

1. **Lista de personagens (início)**: skeleton da grade, aviso comum, títulos da escala e card no padrão.
2. **Detalhes**: skeleton das seções enquanto carrega e erro com o componente comum.
3. **Batalha e resultado**: tokens, erro comum, botões Fechar e Pular no padrão, Pular com a mesma altura do principal.
4. **Mercado (Heróis e Artefatos)**: skeleton no primeiro lote, vazio com ícone (carrinho ou lupa) e Comprar compacto.
5. **Giro da roleta**: espaçamentos e card de prêmio no padrão.
6. **Meus Heróis**: destaque do herói ativo pelo helper, Melhorar compacto e Usar/Artefatos como secundários.
7. **Trilha**: título do sistema, rótulo da patente, Abrir compacto e troféus na escala.
8. **Abertura de baú**: só tipografia e margens (a cena animada não mudou) e costas do card com o mesmo canto da frente.
9. **Artefatos**: botão de ícone, título do sistema e Equipar compacto.
10. **Álbum**: pílula de selos e de "Resgatar", e erro comum sem sobrepor a lista.
11. **Página da equipe**: skeleton no carregamento e erro com o componente comum.
12. **Desafios do Dia**: card no padrão, alinhado aos cards da grade, com ícones e barras alinhados e Abrir baú compacto.
13. **Sobrevivência**: título, pílula de poder e "Tentar de novo" de 48 dp.

A splash já é o vídeo em tela cheia com fundo preto, então não havia o que aplicar.

## Decisões que tomei

- **Ponto de volta**: usei um zip de backup em vez de um commit. O repositório git é a sua pasta de
  usuário inteira (tem `.aws` e arquivos pessoais já preparados), e um commit ali seria arriscado.
- **T1**: o save continua usando `commit()` na thread principal. O retorno dele é o que permite
  desfazer uma mudança que falhou, e o JSON é pequeno.
- **C3**: não dividi o `PlayerState`. Dividir mudaria o formato do save e exigiria uma migração.
- **Versões do AGP e das bibliotecas**: não atualizei (6 avisos do lint). Atualizar perto da entrega pode quebrar o build.
- **`notifyDataSetChanged`**: mantido (7 avisos do lint). As listas são trocadas inteiras, e o
  `DiffUtil` mudaria as animações das listas.
- **MergeRootFrame**: ignorado com `tools:ignore` em 3 layouts. A raiz tem id, recebe toques ou é
  incluída com ids diferentes, e trocar por `<merge>` quebraria isso.
- **Fonte do texto**: é a fonte do sistema. Bebas Neue fica só nos títulos. Não baixei fonte nova.
- **Margem lateral**: passou de 20 para 16 dp em todas as telas, para ficar na grade de 8 dp.
- **Rótulos pequenos em negrito** (pílulas, números, "Patente 3"): mantive o tamanho de 11 a 13 sp.
  Os números gigantes (selo LENDÁRIO!, dano) também ficaram como estavam.
- **Carregamento**: o skeleton aparece dentro da própria lista, pelo rodapé. O Álbum manteve a linha
  "Carregando…" de cada equipe, porque cada equipe carrega separada.
- **Cards de bônus da Sobrevivência**: mantêm a borda um pouco mais clara, porque são uma escolha tocável.
- **Seleção de oponente**: essa tela não existe desde a especificação de progressão (a batalha é
  contra o personagem aberto), então não havia o que redesenhar.
- **Abertura de baú**: não mexi em tamanhos nem posições da cena animada, só no texto e nas margens da borda.
- **Tokens sem uso**: `card_padding`, `icon_sm` e `icon_lg` passaram a ser usados onde o valor já era o mesmo, sem mudança visual.
- **Emulador**: havia um emulador travado ("offline") desde ontem. Pedi para reiniciá-lo e a
  permissão foi negada. Por isso não há capturas de tela e não faço nenhuma afirmação sobre o visual.

## Não resolvido

- **Visual não conferido em aparelho ou emulador**: todo o redesenho foi checado só por build,
  lint e leitura dos layouts. Veja a lista no fim.
- **13 avisos do lint de propósito**: 6 são de versões de dependência e 7 de `notifyDataSetChanged`. Veja "Decisões".
- **Tentativas que falharam 3 vezes**: nenhuma. Nenhum item precisou ser desfeito.

## Arquivos movidos para `_to_delete/`

- `app/src/main/res/drawable/ic_chest_lid.xml`: o lint o marcou como sem uso, e não há referência a ele no código nem nos layouts.
- `app/src/main/res/drawable/ic_home.xml`: o lint o marcou como sem uso, e não há referência a ele no código nem nos layouts.

## Sugestões para depois

- **Números do jogo**: nenhum causou bug, então nenhum ficou anotado aqui.
- **Dependências**: atualizar o AGP, o Material, o Glide, o Retrofit e o OkHttp depois da entrega, com um build completo para conferir.
- **Listas**: trocar `notifyDataSetChanged` por `ListAdapter` + `DiffUtil` (AndroidX), para animar só o que mudou.
- **Licença da fonte**: incluir o arquivo de licença da Bebas Neue (`OFL.txt`). Isso depende da sua autorização para baixar.
- **Ícones do launcher**: os `.webp` das pastas `mipmap-*dpi` nunca são usados com minSdk 33 (vale sempre o ícone adaptativo) e podem ir para `_to_delete/`.
- **Save**: gravar em segundo plano (`apply()`) guardando uma cópia em memória para desfazer, e depois separar o `PlayerState` com migração.
- **Álbum**: skeleton por linha no lugar do texto "Carregando…".
- **Testes de interface**: Espresso (AndroidX) para as telas principais e testes de tamanho de fonte grande.
- **Ideias de funcionalidade** (não implementei):
  - modo claro;
  - transições animadas entre telas;
  - "puxar para atualizar" na lista inicial.

## Como voltar ao estado anterior

1. Feche o Android Studio.
2. Renomeie a pasta atual, por exemplo `Marvel` → `Marvel-revisado`.
3. Extraia `Marvel-revisado/_backup/antes-da-refatoracao.zip` dentro de `AndroidStudioProjects`. O zip
   já contém a pasta `Marvel/`.
4. Copie `local.properties` da pasta `Marvel-revisado` para a nova `Marvel`. O backup não tem esse
   arquivo, porque ele guarda a chave da API.
5. Abra `Marvel` no Android Studio e rode o build.

Depois da revisão, a seu pedido, tirei todos os comentários do projeto. Para voltar só essa etapa
(com a revisão e os comentários), use os mesmos passos com `_backup/antes-de-tirar-comentarios.zip`.

## O que eu devo conferir no celular

1. **Início**: ao abrir sem cache, aparecem cards cinza pulsando. O card de Desafios fica alinhado com os cards de personagem.
2. **Cards**: a borda cinza de 1 dp aparece em todas as telas, mas discreta.
3. **Meus Heróis**: o herói ativo tem borda vermelha e os outros borda cinza. Melhorar, Usar e Artefatos cabem sem cortar.
4. **Trilha**: nos cards das patentes, nome, descrição, baú e botão Abrir cabem sem cortar.
5. **Abertura de baú**: o card gira sem mudar o canto. Título e dica ficam no lugar.
6. **Artefatos**: um nome longo (2 linhas) cabe no espaço de 96 dp. O botão Equipar está legível.
7. **Álbum e página da equipe**: sem internet e sem cache, aparece só o aviso com "Tentar de novo". Com internet, aparece o skeleton ao abrir uma equipe.
8. **Mercado**: uma busca sem resultado mostra a lupa. O primeiro lote mostra linhas cinza pulsando.
9. **Sobrevivência**: sem internet, o "Tentar de novo" do inimigo é fácil de tocar e funciona.
10. **Fonte grande do Android**: títulos, pílulas e botões não cortam nem sobrepõem.
