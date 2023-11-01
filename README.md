# Redes-PL2-1

## Base Decisions

#### Protocolo

 * UDP?
 * RTP?? já usa UDP, podemos inspirarmo-nos
   * **Payload type** - tipo de ficheiro (inútil, vai ser sempre mpeg)
     * Lixo, se apenas houver um tipo de *encoding*
   * **Sequence #** - detetar sequência/perda de packets 
     * necessário, não vamos dar display a frames anteriores do vídeo 
     * loss 
     * não vamos esperar pelos anteriores? podemos definir uma **margem de *display*** parte da latência total
       * *buffer* do packet 2 enquanto espera pelo 1 MAS até ao limite da latência
   * **Timestamp** - instante de sample do primeiro byte
     * Útil para calcular diferença para a latência
     * Se der para deduzir do *Sequence #* kinda inútil
       * Os packets podem ter tamanho diferente uns dos outros dependendo da bitrate (DASH*)
   * **SSRC** - Identifica a fonte da RTP Stream
     * Lixo
 * RTCP, TCP para Real-Time
   * Enviado por todos os participantes de um RTP, ao invés de apenas o Servidor, para todos os outros participantes (*flood* para informar o estado da rede)
   * Normalmente usa multicast addressing
   * Para distinguir RTP e RTCP usa-se diferentes port numbers para cada um
   * Para limitar o tráfego, quantos mais utilizadores há menos mensagens RTCP se envia
   * Limitar o tamanho dos envios de Controlo para 5% da largura de banda total para o RTP mesmo, 75% receivers e 25% senders
 * Se virmos que faz sentido TUDO do TCP mais vale trocar de protocolo em certas alturas (reduz carga aplicacional)
  
 * **INP**, Idle-Node Protocol
   * OSPF-like para hellos (manter os nós vivos na rede)

#### Linguagem

~~Java~~ Python

#### Métricas
 * Foco: como é uma *stream* de multimedia, throughput é a métrica rainha, seguida da latência
   * Seguir as *guidelines* das teóricas (jogos: latência, etc...)
 * Apenas são atualizadas pelo caminho da Árvore construída no arranque
 * *Jitter*
   * Variância da latência
 * *Bandwidth* disponível
   * Provavelmente a mais complicada de gerir 
   * Menor *bandwidth* não impede necessariamente o envio de dados (optimizar a rede)
   * Ineramente dependente da *bandwidth* total física (underlay)
   * Combina necessariamente com Latência
   * Como lidar com *Fairness* da banda-larga?
     * Dividir igualmente? SJF? FIFO?
 * Latência 
   * Tempo de viagem dos pacotes, entre envio/receção
   * Medido com OKs do tempo quando acontece uma receção
   * Inclui outros tipos de delay
     * Nós Overloaded
     * Algoritmos de *routing*
 * Loss
   * Marcar cada *link* como *lossy* ou *lossless* dependendo de respostas UDP
   * | ; | |; | |; | —
 * Nº *hops* lógicos
   * Última métrica, pouco relevante, desempate
 * O que é que cada nó guarda?
   * As métricas comulativas de todos os nós mais para cima (decidir o caminho é mais complicado mais em baixo)

### Funcionalidades

#### Nodo pede Stream
 * Nodo sobe a árvore de transferência por algum nodo que já esteja fazer *stream* daquele ficheiro
 * Se nenhum se encontrar a transferir o ficheiro, chega até ao RP
 * Um Protocolo auxiliar para pedir ligação
   * Este pedido pode ser por *flood* aos vizinhos ou pode ser especial pelos melhores caminhos
   * Nunca se sabe como está o estado superior, um pior caminho próximo pode ser seguido pelo melhor de cima: *flood*

#### RP pede ao Servidor o conteúdo
 * Terá de ter um Protocolo específico, o mais leve possível
 * Um Request content e um Stop content que o RP controla sempre para escolher entre Servidores

#### RP envia stream para o Cliente
 * O RP conhece a árvore inteira, mas não necessariamente o estado inteiro dos caminhos.
   * *Full Centralized*: Se o RP conhecer a árvore inteira pode fazer Djikstra a cada intervalo de tempo
     * Pode maximizar o uso das larguras de rede & trocar qual o Servidor que serve com esses dados
     * Tem muito mais *overhead*: todos as informações sobre a rede têm de chegar ao RP
   * *Step-by-Step*: cada nó toma a decisão por onde enviar os pacotes em cada passo, o RP não sabe de nada
     * Pode desperdiçar muita rede por tomar caminhos menos úteis
     * Pode ser conjugado com conhecimento para baixo em cada um dos nodos, tornando-os responsáveis por conhecer os melhores caminhos
     * Poupa as mensagens a ser enviadas, para não serem inúteis ao longo do caminho e não sobrecarregarem o RP
     * Cada *router* pode saber o seu **best throughput/latência/jitter/loss** ao longo do seu ramo e apenas informar isso com "RTCP"

#### Decisão entre Servidores
 * Relevante quando mais que um Servidor têm o mesmo conteúdo
 * O enunciado sugere marcar uma métrica para cada um dos Servidores
   * Pode ser um valor de desempate, onde o RP guarda dinamicamente qual o melhor Servidor a usar para cada momento
   * Pode ser uma métrica junto de cada ficheiro em vez do Servidor, onde um Servidor pode ter um ficheiro maior e não outro

#### Adição de Servidores
 * O RP precisa de conseguir receber informação sobre o novo servidor adicionado
   * Quais os vídeos neles guardados
   * Quais as métricas? definidas
 * O RP precisa de saber o caminho até ao servidor

#### Adição de Nodos
 * Árvore hard-coded
   * apenas pare testar o resto das funcionalidades
 * ARRANQUE: *Bootstrapper*
   * pergunta-se ao *bootstrapper* quais é que são os vizinhos do novo nó
   * o *bootstrapper* sabe à partida quais são os vizinhos, hard-coded
 * RUNTIME: Vizinhos (folhas)
   * Simples, apenas se pergunta o caminho da stream e esta folha entende quais estão ligados ao RP
 * RUNTIME: ???? (nós intermédios)
   * Complicado, pode ser um novo ponto de acesso ao RP que tem de propagar para as folhas da árvore
 
#### Remoção de Nodos
 * O RP NUNCA morre, não se faz eleição de líder
   * ...unless... (última das últimas coisas)
 * Graciosa
   * Pedir saída (IGMPv2, Leave Group)
 * Não-Graciosa
   * Levou com um raio (IGMPv1, escolhas)
     * Ativa (continuamente perguntar se está vivo)
       * parece pior porque tem 2x o número de mensagens
     * Passivo (broadcast da sua vivicitude)

#### Criação da Árvore de Distribuição
 * Para cada pedido de acesso à Stream, deve-se criar uma rapidamente
 * Ativamente, durante o processo de Stream 
 * O que é que cada nó guarda?
   * Um único caminho para os acessos ao RP

#### Recuperação de Perda
 * Baseado na diferença entre as frames, fazer uma interpolação?
  
### Extras
 * Vídeos visualmente distintos para facilitar a apresentação
   * escrever a letra A, B, C no paint para o vídeo A, B, C...