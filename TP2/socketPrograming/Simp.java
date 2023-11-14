import java.net.InetAddress;

public class Simp {
    int time_stamp; //2
    int latency; //2
    int throughput; //2 (Max. 9999)
    InetAddress sourceAddress; //4
    int checksum; //4
    byte[] header;
    byte[] payload;

    /*
    Definir uma Socket para cada uma das funcionalidades deste protocolo:
    1a. Requisição de uma Stream: árvore de distribuição
        O payload leva a string com o nome do ficheiro
    1b. Resposta de uma Stream em relação à chegada ao RP
        Provavelmente o mesmo socket que o anterior
        Requer distinção entre conseguir, não conseguir e pedir um wait
    2. Tratamento idle quando não existe stream em ação
        Parece carregado andar sempre com um IP inútil para trás e para a frente, podia ir no payload
    3????. Acks da receção de pacotes da Stream
        A ideia era limitar o tamanho da rede, mas precisava do ack# então se calhar não funciona
    */
}
