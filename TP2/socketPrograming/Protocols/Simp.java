package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.LocalTime;

public class Simp extends Packet {  //Stream Initiation Management Protocol

    static int HEADER_SIZE = 0;

    // int time_stamp; // 2

    // int latency; //2

    // int throughput; //2 (Max. 9999)

    /*
     * Definir uma Socket para cada uma das funcionalidades deste protocolo:
     * 1a. Requisição de uma Stream: árvore de distribuição
     * O payload leva a string com o nome do ficheiro
     * 1b. Resposta de uma Stream em relação à chegada ao RP
     * Provavelmente o mesmo socket que o anterior
     * Requer distinção entre conseguir, não conseguir e pedir um wait
     * 2. Tratamento idle quando não existe stream em ação
     * Parece carregado andar sempre com um IP inútil para trás e para a frente,
     * podia ir no payload
     * 3????. Acks da receção de pacotes da Stream
     * A ideia era limitar o tamanho da rede, mas precisava do ack# então se calhar
     * não funciona
     */

    public Simp(InetAddress sourceAddress, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        // this.time_stamp = Packet.getCurrTime();

        // this.header[0] = (byte) (this.time_stamp >> 8 /* & 0xFF */);
        // this.header[1] = (byte) (this.time_stamp      /* & 0xFF */);

    }

    public Simp(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException{
        
        super(packet, HEADER_SIZE);

        // this.time_stamp = (Byte.toUnsignedInt(this.header[0]) << 8) |
        //                    Byte.toUnsignedInt(this.header[1]);

        this.checksum = (Byte.toUnsignedInt(this.header[0]) << 8) |
                         Byte.toUnsignedInt(this.header[1]);

    }

	// public int getTime_stamp() {
	// 	return time_stamp;
	// }

	public int getChecksum() {
		return checksum;
	}

}
