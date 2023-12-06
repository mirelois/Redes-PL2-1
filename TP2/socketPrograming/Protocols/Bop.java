package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Bop extends Packet{

    static int HEADER_SIZE = 2;

    int checksum; // 2

    public Bop(byte[] payload, int payload_size, InetAddress address, int port) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.checksum = ~this.getPayloadChecksum();
        System.out.println("before putting in BOP: " + this.checksum);
        this.header[0] = (byte) (checksum >> 8);
        this.header[1] = (byte) (checksum);

    }

    public Bop(DatagramPacket packet) throws PacketSizeException {

        super(packet, HEADER_SIZE);

        // TODO: check packet_size

        this.checksum = (Byte.toUnsignedInt( this.header[0] ) << 8) | 
                         Byte.toUnsignedInt( this.header[1] ) ;
        
    }

    public int getChecksum() {
        return checksum;
    }

}
