import java.net.DatagramPacket;
import java.net.InetAddress;

public class Bop extends Packet{

    public int checksum(byte[] data){
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }
        return ~sum;
    }

    static int HEADER_SIZE = 2;

    int checksum; // 2

    public Bop(/* int sequence_number, */boolean ack, byte[] payload, int payload_size, InetAddress address, int port) {

        super(HEADER_SIZE, payload, payload_size, address, port);

            this.header[1] = (byte) (checksum >> 8);
            this.header[2] = (byte) (checksum);

    }

    public Bop(DatagramPacket packet) {

        super(packet, HEADER_SIZE);

        // TODO: check packet_size

        this.checksum = (Byte.toUnsignedInt( this.header[0] ) << 8) | 
                         Byte.toUnsignedInt( this.header[1] ) ;
        
    }

    public int getChecksum() {
        return checksum;
    }

}
