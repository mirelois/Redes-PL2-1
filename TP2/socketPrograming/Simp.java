import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.LocalTime;

public class Simp extends Packet {

    static int HEADER_SIZE = 10;

    int time_stamp; // 2

    // int latency; //2

    // int throughput; //2 (Max. 9999)

    InetAddress sourceAddress; // 4

    int checksum; // 4

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

        LocalTime now = LocalTime.now();

        this.time_stamp = now.getSecond() * 1000 + now.getNano() / 1000000;

        this.sourceAddress = sourceAddress;

        this.checksum = 0;

        String[] ip_values = sourceAddress.getHostAddress().split("\\.", 4);

        this.header[0] = (byte) (this.time_stamp >> 8 /* & 0xFF */);
        this.header[1] = (byte) (this.time_stamp      /* & 0xFF */);
        
        this.header[2] = Byte.parseByte(ip_values[0]);
        this.header[3] = Byte.parseByte(ip_values[1]);
        this.header[4] = Byte.parseByte(ip_values[2]);
        this.header[5] = Byte.parseByte(ip_values[3]);

        this.header[6] = (byte) (this.checksum >> 24 /* & 0xFF */);
        this.header[7] = (byte) (this.checksum >> 16 /* & 0xFF */);
        this.header[8] = (byte) (this.checksum >> 8  /* & 0xFF */);
        this.header[9] = (byte) (this.checksum       /* & 0xFF */);

    }

    public Simp(DatagramPacket packet) throws java.net.UnknownHostException{
        super(packet, HEADER_SIZE);

        this.time_stamp = (this.header[0] << 8) | this.header[1];

        StringBuilder ip = new StringBuilder(15);

        ip.append(this.header[2]);
        ip.append('.');
        ip.append(this.header[3]);
        ip.append('.');
        ip.append(this.header[4]);
        ip.append('.');
        ip.append(this.header[5]);

        this.sourceAddress = InetAddress.getByName(ip.toString());

        this.checksum = (this.header[6] << 24) | (this.header[7] << 16) | (this.header[8] << 8) | this.header[9];

    }

	public int getTime_stamp() {
		return time_stamp;
	}

	public InetAddress getSourceAddress() {
		return sourceAddress;
	}

	public int getChecksum() {
		return checksum;
	}

}
