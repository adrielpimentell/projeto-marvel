# Marvel Battle

App Android nativo em **Java** para o *Marvel API Challenge*. Personagens reais da
[Comic Vine API](https://comicvine.gamespot.com/api/) viram lutadores com atributos
calculados a partir dos dados da própria API.

## Funcionalidades

- **Lista de personagens Marvel** com busca por nome, filtros e rolagem infinita.
- **Detalhes**: imagem grande, resumo, poderes, equipes, filmes e atributos de 0 a 100.
- **Batalha por turnos animada** contra qualquer personagem, com troféus e patentes.
- **Mercado**: compre heróis (preço pelo Overall) e gire roletas de artefatos.
- **Meus Heróis**: escolha o herói ativo, melhore atributos e equipe até 2 artefatos.
- **Trilha de Patentes** com um baú por patente (animação de abertura).
- **Álbum de Equipes**: junte membros de equipes reais (Avengers, X-Men...) e ganhe selos.
- **Desafios do Dia**: 3 desafios sorteados pela data, com moedas e um baú.
- **Modo Sobrevivência**: andares seguidos, a vida não enche e os bônus são escolhidos a cada vitória.
- **Abertura em vídeo** ao abrir o app (um toque pula).
- Tudo fica salvo no aparelho. O que já foi carregado da API fica em cache, e o app funciona offline com o que está no cache.

## Como rodar

1. Crie uma conta gratuita em <https://comicvine.gamespot.com/api/> e copie a sua **API key**.
2. Abra esta pasta no **Android Studio** (versão recente) e espere o Gradle sincronizar.
3. Abra o arquivo `local.properties`, na raiz do projeto (o Android Studio cria esse arquivo
   ao abrir o projeto), e adicione a linha:

   ```
   COMICVINE_API_KEY=cole_sua_chave_aqui
   ```

4. Clique em **Sync Project with Gradle Files** e depois em **Run** para rodar num celular ou
   emulador com **Android 13 ou mais novo**.

A chave nunca fica no código: o Gradle lê do `local.properties`, que não vai para o controle de
versão nem para o .zip. Sem a chave, o app mostra na tela o que falta configurar.

## Testes

Os testes de unidade ficam em `app/src/test`. Para rodar todos, clique com o botão direito
na pasta `app/src/test/java` e escolha **Run 'Tests in...'**, ou use o terminal:

```
gradlew test
```

Os testes cobrem, entre outras coisas:
- simulações de 1000 batalhas e de 10 mil giros de roleta;
- 200 partidas de Sobrevivência por herói;
- cenários em que nenhuma recompensa pode ser paga duas vezes.

## Como os atributos são calculados

A Comic Vine não tem atributos de luta. A classe `game/AttributeCalculator` cria os atributos
só com dados reais da API, então o mesmo personagem sempre recebe os mesmos valores:

- **Vida** = 20 + 8 × log10(aparições em edições + 1) + 5 × poderes de resistência + filmes (máx. +10)
- **Força** = 20 + 9 × poderes de força/energia
- **Velocidade** = 20 + 10 × poderes de mobilidade/sentidos
- **Inteligência** = 20 + 9 × poderes mentais + equipes (máx. +20)
- **Overall** = média dos quatro atributos. Todos os valores ficam entre 0 e 100.

Todos os números de equilíbrio do jogo (moedas, preços, chances, tempos das animações) ficam
em `game/GameBalance`.

## Estrutura

| Pacote | O que tem |
|---|---|
| `data` | Retrofit, modelos da Comic Vine e repositório com cache |
| `game` | Regras em Java puro, testáveis sem Android: batalha, economia, save, baús, desafios, sobrevivência |
| `ui` | Telas (Activities), adapters e animações |

## Ferramentas de depuração

No build **debug** aparecem botões para testar rápido: dar troféus e moedas, "Trocar dia"
nos Desafios, "Pular andar" na Sobrevivência e câmera lenta no baú. Para esconder esses
botões, troque `ENABLED` para `false` em `ui/common/DebugTools.java`.

## Créditos

- Dados e imagens de personagens: [Comic Vine](https://comicvine.gamespot.com/).
- Fonte dos títulos: Bebas Neue, licença SIL Open Font License 1.1.
- Ícones: Material Icons (Google), licença Apache 2.0. O ícone do app foi montado com eles.
