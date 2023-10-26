# Redes-PL2-1

## Base Decisions

#### Protocolo

UDP

#### Linguagem

Java

#### Métricas

 * *Jitter*
   * Variância da latência
 * *Bandwidth* disponível
   * Provavelmente a mais complicada de gerir 
   * Menor *bandwidth* não impede necessariamente o envio de dados (optimizar a rede)
   * Ineramente dependente da *bandwidth* total física (underlay)
   * Combina necessariamente com Latência
 * Latência 
   * Tempo de viagem dos pacotes, entre envio/receção
   * Medido com OKs do tempo quando acontece uma receção
   * Inclui outros tipos de delay
     * Nós Overloaded
     * Algoritmos de *routing*
 * Loss
   * Marcar cada *link* como *lossy* ou *lossless* dependendo de respostas
   * | ; | |; | |; | —
 * Nº *hops* lógicos
   * Última métrica, pouco relevante, desempate



### Funcionalidades

#### Adição de Nodos
   * Adição de Nodos
     * 
   * Remoção de Nodos