# Bugs — revisão final

Estado inicial (commit `ba57818`): build debug e release passando, 150 testes passando, lint com 0 erros e 16 avisos.

Estado final: build debug e release passando (clean build), 158 testes passando (8 novos em `GameRulesTest`), lint com 0 erros e os mesmos 16 avisos (versões de dependência, `notifyDataSetChanged` e recursos sem uso listados abaixo).

Resultado: 8 bugs encontrados e corrigidos (0 crítico, 1 alto, 2 médios, 5 baixos). Nenhum travamento por campo nulo, lista vazia ou tela fechada antes da resposta foi encontrado na leitura.

Gravidade: **crítico** (trava ou perde progresso), **alto** (prêmio, compra ou recurso inacessível), **médio** (tela ou fluxo errado), **baixo** (caso raro ou detalhe).

| # | Arquivo:linha | Problema | Como reproduzir | Gravidade | Correção | Status |
|---|---|---|---|---|---|---|
| 1 | ui/season/SeasonEndActivity.java:32 | A tela "Fim da temporada" só aparece uma vez enquanto o processo do app estiver vivo. Um prêmio novo da semana seguinte, ou de outra conta no mesmo celular, não abre a tela, e o baú da temporada fica sem nenhum lugar para ser aberto | Ver a tela numa semana, deixar o app em segundo plano até a segunda-feira seguinte e voltar | Alto | Lembrar qual temporada e qual conta já viram a tela, no lugar de um sim/não fixo | Corrigido |
| 2 | res/layout/dialog_change_name.xml, dialog_change_password.xml, dialog_delete_account.xml | Os diálogos de Conta não rolam. Em celular pequeno com o teclado aberto, campos e botões ficam escondidos | Celular pequeno: Perfil → Excluir conta → tocar no campo de senha | Médio | Conteúdo dos diálogos dentro de um NestedScrollView | Corrigido |
| 3 | MainActivity.java:105 | Conta sem nome de jogador (cadastro interrompido depois de criar o login) abre direto no Início. O jogador nunca entra no ranking e não tem onde escolher o nome, porque o atalho de Meus Heróis saiu | Fechar o app logo depois de tocar "Criar conta", antes do fim; abrir de novo | Médio | No Início, se a conta não tiver nome, abrir a tela de escolher nome | Corrigido |
| 4 | data/auth/AuthRepository.java (renamePlayer) | Se a reserva do nome antigo não existir mais (dado inconsistente), a regra recusa a exclusão e a troca de nome falha sempre com "Não deu certo" | Apagar a reserva em `usernames` pelo console e tentar trocar o nome | Baixo | Ler a reserva antiga na transação e só excluir se ela existir e for do jogador | Corrigido |
| 5 | game/PlayerState.java:244 | Voltar o relógio do celular para uma semana anterior zera os troféus da semana e reabre os baús da Trilha. Ao voltar o relógio, zera de novo | Ajustes do celular → data uma semana antes → abrir o app | Baixo | A temporada salva só anda para frente, como os Desafios do Dia | Corrigido |
| 6 | ui/auth/LoginActivity.java:113 | "Esqueci minha senha" mostra o carregamento dentro do botão "Entrar", como se estivesse fazendo login | Tela de login → digitar e-mail → "Esqueci minha senha" | Baixo | Durante o envio, só desabilitar os campos, sem o carregamento no "Entrar" | Corrigido |
| 7 | game/PlayerState.java:55 | Um resultado de temporada corrompido no save (raridade ou semana inválida) trava a tela "Fim da temporada" | Save editado ou de versão antiga com campo faltando | Baixo | `repair()` descarta resultado inválido | Corrigido |
| 8 | ui/ranking/RankingViews.java (bindPillMessage) | Sem o nome guardado no aparelho, a linha fixa mostra só " (você)" | Conta sem nome em cache abrindo o Ranking sem internet | Baixo | Mostrar "Você" quando não houver nome | Corrigido |

## Precisa da sua decisão

| # | Assunto | Situação | Minha recomendação |
|---|---|---|---|
| D1 | Dia dos Desafios do Dia | O dia vira à meia-noite do fuso do celular. A temporada vira no fuso de Brasília. Para quem está fora de Brasília, os dois viram em horas diferentes | Manter assim: cada jogador ganha desafios novos à meia-noite local. Mudar para Brasília altera quando os desafios renovam |
| D2 | Semana pelo relógio do celular | Quem adianta a data do celular vê a virada da semana antes e recebe o prêmio antes da hora (uma vez por semana) | Aceitar no projeto. A correção completa exige a hora do servidor nas regras ou Cloud Functions (plano pago) |
| D3 | Sair sem internet | Troféus de batalhas feitas sem internet ainda estão na fila. Ao sair da conta antes de sincronizar, eles se perdem no ranking | Mostrar um aviso no "Sair" quando houver troféus não enviados. É um aviso novo, então muda o fluxo |
| D4 | Prêmio da temporada no meio do caminho | O prêmio é marcado no Firestore e só depois entregue no aparelho. Se o app fechar exatamente entre os dois, o prêmio se perde | Guardar um "prêmio pendente" em `users/{uid}` até o baú ser aberto. Isso muda a estrutura do Firestore, por isso não apliquei |

## Não resolvido

- **Troféus por chamada direta à API:** quem souber usar a API do Firestore consegue somar +10 sem batalhar. As regras limitam o passo (+10/−5), mas não conseguem provar que houve batalha. Resolver exige Cloud Functions, que só funcionam no plano pago (Blaze).
- **Cache do Firestore entre contas:** os documentos de uma conta continuam no cache local depois de sair. O app nunca os mostra para outra conta. Apagar exige `clearPersistence` ao sair, o que muda o fluxo.

## Sugestões para depois

- **Posição no ranking:** calcular com uma única contagem que use o mesmo índice e a mesma ordem da lista. O jeito atual usa duas contagens e um segundo índice, e em empate exato de horário pode diferir da lista por 1 posição.
- **Baú da temporada:** criar um atalho para abrir o baú quando o jogador sai da tela "Fim da temporada" sem abri-lo. Hoje a tela volta na próxima vez que o app é aberto.
- **Listas:** trocar `notifyDataSetChanged` por `ListAdapter` + `DiffUtil`.
- **Dependências:** atualizar o AGP e as bibliotecas depois da entrega.
- **Arquivos sem uso para você decidir:**
  - `RankingRows.java`, `item_ranking_row.xml` e a medida `ranking_position_size`;
  - a medida `corner_sm`;
  - a pasta `_to_delete/`;
  - os arquivos `PROGRESSO.md`, `AUDITORIA.md` e `RELATORIO.md`.

## Como voltar ao estado anterior

O commit `ba57818` ("antes da revisao final") marca o ponto de volta.

- **Desfazer só as correções, mantendo o histórico:** `git revert` em cada commit feito depois de `ba57818`.
- **Voltar tudo de uma vez:** `git reset --hard ba57818`. Isso apaga mudanças locais ainda não salvas em commit.

## O que eu devo testar no celular antes de gravar o vídeo

1. **Login:** "Esqueci minha senha" mostra o aviso sem o carregamento no botão "Entrar".
2. **Cadastro:** a conta nova cai no Início e aparece no Ranking depois da primeira batalha.
3. **Trocar nome:** no Perfil, tentar o nome de outra conta dá erro. Trocar para um nome livre mostra o novo nome no Ranking.
4. **Diálogos em celular pequeno:** com o teclado aberto, os campos e botões de "Alterar senha" e "Excluir conta" rolam.
5. **Excluir conta (conta de teste):** volta ao login. Outra conta no mesmo celular continua com o próprio progresso.
6. **Ranking:** pódio, a minha linha (sem aparecer duas vezes) e puxar para atualizar.
7. **Batalha sem internet:** depois de conectar e voltar ao Início, os troféus sobem no Ranking.
8. **Mercado:** comprar herói e girar roleta tocando duas vezes rápido cobra uma vez só.
9. **Baús:** Trilha, baú do dia e Sobrevivência abrem a animação normalmente, sem botões de teste.
10. **Splash:** o vídeo abre e depois vai para o login (sem conta) ou para o Início (com conta).
