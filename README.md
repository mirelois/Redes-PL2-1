# Redes-PL2-1

## Base Decisions

### Protocolo

#### Existentes

 * UDP
   * Simplesmente usado na camada de transporte, faz o que tem de fazer
 * TCP
   * **Sequence #**
     * Depende se o pacote é um *SYN* ou se é o resto da sequência
     * Se o *SYN* estiver a 1, então é o número inicial da sessão
     * Se o *SYN* estiver a 0, então é o número acumulado desde o primeiro byte
     * Serve para conseguir *Reliable transmission* - Retransmission
       * Retransmission Ambiguity quando um *ACK* é recebido depois de uma retransmissão (qual??)
       * **Dupack-based retransmission** - Os *ACK* dos pacotes guardam até ao pacote mais recente recebido
         * Quando um pacote for falhado 3x (não está presente em 3 *ACKs*) ele é retransmitido
         * Parece útil quando é necessária fidelidade completa (muito demorada)
       * **Timeout-based retransmission** - Depois de certo tempo, um segmento é retransimitido
         * Esse tempo repete, com *exponential backoff*, até ao *ACK* desse segmento
         * O tempo baseia-se no **RTT** que tem de ser bem calculado (muitos problemas de estimativa)
   * **Ack #**
     * *ACK* a 1 -> valor que a *source* está à espera de receber de seguida
     * Seguido de um *SYN* Será o Sequence # do *SYN* anterior mais 1
   * **Data Offset**
     * Tamanho do TCP Packet em 32-bit words, que também é o offset até ao packet
     * Permite até 40 bytes de opções, com 20 bytes do header
   * **Flags**
     * **CWR** - Congestion Window Reduced, respondeu com Congestion Control Mechanism em resposta a um **ECE** a 1
     * **ECE** - ECN-Echo (dá Echo do IP), dual role
       * *SYN* 1 -> TCP peer is **ECN** capable
       * *SYN* 0 -> Recebeu-se **ECN** no *IP*, o que indica network congestion
     * **URG** - Urgent Pointer Field é significativo
     * **ACK** - O campo Ack # é significante
     * **PSH** - Pede para fazer push à data no buffer para a aplicação
     * **RST** - Reset Connection
     * **SYN** - Apenas o primeiro packet de cada lado da conexão deve ter esta flag
     * **FIN** - Último packet do sender
   * **Window size** - Tamanho da *receive window* (até ao espaço livre no buffer de quem envia, mas pode ser menos)
     * Se o **Window Size** for 0, o sender não envia mais dados e começa um *persist timer* (que evita deadlock)
     * Envia um pequeno depois para recomeçar o envio completo, recebendo um *ACK* para indicar o novo *window size*
   * **Checksum**
   * **Urgent Pointer** - Aponta para o último data byte urgente com offset do *sequence #*
   * **Options** - tamanho das options depende do data offset
     * **Window Scaling** - Dar scale ao window size, apenas durante o *SYN* da conexão
     * **Maximum segment size** - Tamanho máximo da data que se pode transmitir
       * Tenta-se que seja pequeno o suficiente para evitar *IP fragmentation*
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

#### Protocolos a usar
 * **RAP**, Real-Time Application-Level Protocol
   * Protocolo para enviar os dados da *stream* propriamente dita
   * **Sequence #**
   * **Timestamp**
   * ****
 * **INTP**, Idle Node Tick Protocol
   * OSPF-like para hellos (manter os nós vivos na rede)
 * **NAP**, Node Addition Protocol
   * Protocolo para adicionar nodos, antes e depois do arranque
 * **BOP**, Bootstrapper Opening Protocol
   * Protocolo para iniciar a rede *overlay*
   * **ACK Flag** - *Flag* utilizada pelo *Bootstrapper Client* para distinguir os pedidos das receções
     * Provavelmente inútil, máquina de estados resolveria isto
   * Conjugado com retransmissão por *timeout*, na primeira e na segunda mensagem
   
#### Linguagem

~~Java~~ ~~Python~~ Java

#### Métricas
 * De quanto em quanto tempo se devem atualizar as métricas (tempo de envio das RTCP)?
   * 20s
 * Foco: como é uma *stream* de multimedia, throughput é a métrica rainha, seguida da latência
   * Seguir as *guidelines* das teóricas (jogos: latência, etc...)
   * Avaliado por um Djikstra (se centralizado) ou ponto a ponto
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

#### Retransmissão
 * Como o protocolo é *RTP*, a necessidade de novos *packets* é maior que a de todos
 * Pode haver uma margem relativamente grande de *buffering* dos pacotes para depois apresentar
   * Implicava **margem de *display*** maior que algumas vezes o **RTT**
 * Seria parecido com **TCP**, mas sem ter em conta Triple-*ACK* (ou os *RTT* seriam demasiados)
   * Enviar *ACK* dos pacotes até então recebidos
   * Se um pacote não for *ACK-ed* então é retransmitido por *Timeout*
     * Não faz sentido fazer *Dupack-based* porque dependeria de pelo menos 3 *ACKs*
     * O *Timeout*, do lado do Servidor/RP pode ter em consideração os tempos de latência (parar de retransmitir)

#### Nodo pede Stream
 * Nodo sobe a árvore de transferência por algum nodo que já esteja fazer *stream* daquele ficheiro
 * Se nenhum se encontrar a transferir o ficheiro, chega até ao RP
 * Um Protocolo auxiliar para pedir ligação
   * Este pedido pode ser por *flood* aos vizinhos ou pode ser especial pelos melhores caminhos
   * Nunca se sabe como está o estado superior, um pior caminho próximo pode ser seguido pelo melhor de cima: *flood*
 * O Nodo deve dar avisar que ainda está vivo e a ouvir a stream
   * **RTCP**-like avisa que ainda está a ouvir
   * Mensagem *Opt-in* para sair ativamente

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
 * RUNTIME: Protocolo **NAP** (Node Addition Protocol)
 * RUNTIME: Vizinhos (folhas)
   * Simples, apenas se pergunta o caminho da stream e esta folha entende quais estão ligados ao RP
 * RUNTIME: Vizinhos, downstream também (nós intermédios, >=2 ligações)
   * Complicado, pode ser um novo ponto de acesso ao RP que tem de propagar para as folhas da árvore
   * Identificar o seu tipo (Middle ou End-Node)
   * Propagar pelos vizinhos uma pergunta semelhante à inicial, mas com a capacidade de fazer para trás para os clientes
     * Encontrar uma **árvore**, **cliente** e/ou **RP** indica que vai fazer parte da árvore de todos esses clientes 
     * Tem de ser sempre propagado, tal como no iniciar
     * Marcar cada um dos adjacentes como *downstream* ou *upstream*
     * **Árvore** implica conexão com **RP** e com todos os **Clientes** nele ligados (qualquer outro também seria árvore de)
 
#### Remoção de Nodos
 * O RP NUNCA morre, não se faz eleição de líder
   * ...unless... (última das últimas coisas)
 * Graciosa
   * Pedir saída (IGMPv2, Leave Group)
 * Não-Graciosa
   * Levou com um raio (IGMPv1, escolhas)
     * Ativa (continuamente perguntar se está vivo)
       * parece pior porque tem 2x o número de mensagens
     * Passivo (broadcast da sua vivicitude, vivicidencia, vivicidade, viviciderencia)
   * Levou com um raio e era super importante (alguém ficou com **0 vizinhos** na rede)
     * Precisamos de refazer o *overlay*
     * Pode ser enviado também nos momentos em que não é catastrófica
       * Para evitar tráfego extra pode usar a técnica de *random wait time* para enviar para quem for decidido que é para se enviar
     * **Vizinhos dos vizinhos** - Cada nodo sabe uma lista dos vizinhos dos seus vizinhos
       * No caso de falha, tenta conectar-se a esses vizinhos para saber quais estão disponíveis na *Overlay*
       * Gasta aproximadamente *N^2* espaço e não compensa contra duas falhas (adjacentes)
     * **Traceroute** - *Flood* que é captado pela rede *Overlay* e define os novos vizinhos
       * Este *flood* imita o *flood* na criação da árvore de distribuição mas para sempre inteligentemente
       * Pode encher muito os *routers* que não são parte da rede *Overlay*
     * **Perguntar ao Bootstraper** - sabe os vizinhos todos de quem morreu, basta perguntar
       * Não é uma solução real porque não necessariamente existe alguém com conhecimento geral 

#### Criação da Árvore de Distribuição
 * Para cada pedido de acesso à Stream, deve-se criar uma rapidamente
 * Ativamente, durante o processo de Stream 
 * O que é que cada nó guarda?
   * Um único caminho para os acessos ao RP

#### Recuperação de Perda
 * Baseado na diferença entre as frames, fazer uma interpolação?
  
#### Routing da Stream
 * **Tabela de Ativação** - Cada nodo avisa os seus vizinhos se o *link* está disponível, para uma dada *stream*
   * Precisa de uma tabela que associa a cada *stream* os seus vizinhos ativos
   * Os pacotes têm de identificar qual a sua *stream*
   * Os nodos enviam as streams que recebem para os seus links ativos
   * Quando um nodo decide ativar outro link, avisa a ativação ao seu superior
     * O seu superior ativa algum *link* até ao RP (se desativado)
       * Se já tivesse ativo, podia haver o problema de estar a receber *streams* desnecessárias
         * Apenas se o caminho até ao RP estivesse ativo
           * O RP não pode ser mais esperto que isso, implicava comunicação até ele na mesma
       * Ativar os caminhos todos exige um certo flood em todos os ramos 
   * Também tem de avisar a desativação do anterior
     * Se esse anterior não tiver a servir mais ninguém, desativa também
   * Pros:
     * 
   * Cons:
     * Exige métricas cumulativas
     * WRONG: Escala "mal" com o aumentar de streams diferentes
       * Not really, basta ter uma lista e manda-se para toda a gente na mesma
     * Existe uma espécie de flood para braços diferentes, de modo a desativar/ativar partes da rede
 * **Identificação de Destino** - Cada nodo sabe que vizinhos levam a certos nodos especiais
   * Precisa de uma tabela que associa a cada vizinho quais os nodos especiais nesse trajeto
   * Os pacotes têm de identificar qual o seu Cliente destino **(pode ser mais do que um)**
   * Os nodos enviam para o vizinho que determinarem com melhor métrica, desde que siga o trajeto correto
     * Uma Thread em cada nodo pode decidir ativar ou desativar links, como em cima
     * Esta ativação e desativação não utiliza a rede
   * Pros:
     * 
   * Cons:
     * Exige métricas cumulativas
     * Escala mal com o aumento do *multicasting*
       * Os pacotes têm de identificar **todos** os seus destinos
         * Eventualmente, uma bifurcação exige a retirada desses detalhes
         * 

#### Troca de Servidor
 * Cada servidor irá guardar certos vídeos
 * O RP tem a responsabilidade de pedir ao Servidor com melhor métricas
 * Quando decide trocar, envia o último pacote cumulativo ao novo servidor e um Opt-Out ao antigo
 * Vai receber pacotes repetidos de ambas as streams, guarda o mais recente

### Extras
 * Vídeos visualmente distintos para facilitar a apresentação
   * escrever a letra A, B, C no paint para o vídeo A, B, C...

## Implementation

### Iniciar a Topologia
 * Usar **ONode IP Bootstrapper** para indicar todos exceto os *Bootstrappers*
 * O *Bootstrapper* recebe o ficheiro onde os vizinhos se encontram
 * O protocolo utilizado é o *BOP*:
   * Os *Bootstrapper Clients* mandam um *BOP* packet recorrente, por *timeout*, a pedir os dados;
     * Termina quando receberem os dados do *Bootstrapper*
   * O *Bootstrapper* envia um *BOP* packet com os vizinhos do *Client*;
     * Envia **sempre** que receber o Pedido dos Dados
     * Termina quando receber a acusação de receção do *Client* **matando o BootSrapper**
   * O *Cliente* envia um *BOP* packet a acusar a receção.
     * Envia apenas uma vez e depois sai, os *Client* morrem sempre

### Estado dos Nodos
 * Vizinhos do *Overlay*
   * Segundos vizinhos do *Overlay*?
 * Métricas de cada caminho seguindo os vizinhos

### Conexões *Overlay*
 * Para cada um dos vizinhos, criar um ***Listener*** *Thread* que gere a conexão, com um *Socket* para cada
 * O ***Listener*** deve comunicar ao vizinho designado qual a porta que foi aberta para a sua conversação
