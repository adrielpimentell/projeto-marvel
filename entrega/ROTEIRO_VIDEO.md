# Roteiro do vídeo (anônimo) — Marvel Battle

Duração alvo: **cerca de 4 min 30 s**. Grave só a tela do celular, sem rosto e sem nome.
As falas abaixo podem virar legendas na tela; se usar a sua voz, grave a narração de novo
em estúdio ou use voz sintética.

## Antes de gravar (checklist)

- [ ] **Prepare um save interessante** no build debug. Use +50.000 moedas e compre 4 ou 5
      heróis do começo do Mercado (Wolverine, Magneto, Captain America...). Equipe 2 artefatos,
      suba algumas patentes com +1.000 troféus e deixe 1 baú da Trilha sem abrir.
- [ ] **Esconda os botões de depuração**: em `ui/common/DebugTools.java`, troque `ENABLED` para
      `false` e rode de novo pelo Android Studio. O save continua no celular.
- [ ] Deixe 1 desafio do dia quase completo, para completar ao vivo.
- [ ] Ative o **Não perturbe** (sem notificações com nomes) e feche os outros apps.
- [ ] Use o gravador de tela do próprio celular. Não mostre o Android Studio (o caminho da
      pasta tem o seu nome de usuário) nem contas do Google.
- [ ] Internet ligada na primeira passada. Depois, as telas já abertas carregam do cache.

## Cenas

| Tempo | Tela | O que fazer | Legenda / fala |
|---|---|---|---|
| 0:00–0:15 | Ícone → tela inicial | Toque no ícone e mostre a abertura e a lista | "Marvel Battle: personagens reais da Comic Vine viram lutadores." |
| 0:15–0:45 | Lista | Busque "spider", troque o filtro, role até carregar mais | "Busca, filtros e rolagem infinita, com cache para economizar a API." |
| 0:45–1:15 | Detalhes | Abra um herói famoso e role até os atributos | "Os atributos saem dos dados reais: poderes, aparições, filmes e equipes." |
| 1:15–1:50 | Batalha | Toque em "Batalhar" e deixe a animação rodar | "Batalha por turnos: velocidade decide quem começa e a inteligência dá críticos." |
| 1:50–2:10 | Resultado | Mostre moedas, troféus e patente | "Cada vitória dá moedas e troféus." |
| 2:10–2:35 | Mercado | Mostre as equipes nos cards e gire uma roleta | "Heróis custam pelo Overall. As roletas sorteiam artefatos." |
| 2:35–2:55 | Meus Heróis → Artefatos | Melhore um atributo e equipe um artefato | "Até 2 artefatos, com efeitos como veneno, escudo e roubo de vida." |
| 2:55–3:20 | Patentes | Role a trilha e abra o baú guardado | "Cada patente tem um baú, e a abertura muda com a raridade." |
| 3:20–3:40 | Álbum | Abra uma equipe e resgate um selo | "O Álbum usa as equipes reais da API: junte membros e ganhe selos." |
| 3:40–3:55 | Desafios do Dia | Complete o desafio que ficou quase pronto | "Três desafios novos por dia, sorteados pela data." |
| 3:55–4:20 | Sobrevivência | Comece, vença um andar e escolha um bônus | "Na Sobrevivência a vida não enche e o inimigo fica mais forte a cada andar." |
| 4:20–4:30 | Tela inicial | Volte ao topo | "Feito em Java, com 101 testes automáticos. Obrigado!" |

## Dicas

- O botão "Pular" encurta a batalha e um toque pula a abertura do baú. Deixe **uma** batalha
  e **um** baú rodarem inteiros.
- Se a internet travar, siga para a próxima cena; o app mostra um aviso e não fecha.
- Corte os tempos de carregamento na edição.
