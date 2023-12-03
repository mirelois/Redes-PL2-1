package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Rip extends Packet { // Response and Idle Protocol
                                  
    static int HEADER_SIZE = 4;

    int acknowledgment; // 4

	public Rip(int acknowledgment, InetAddress address, int port) {

        super(HEADER_SIZE, null, 0, address, port);

        this.acknowledgment = acknowledgment;

        this.header[0] = (byte) (acknowledgment >> 24 /* & 0xFF */);
        this.header[1] = (byte) (acknowledgment >> 16 /* & 0xFF */);
        this.header[2] = (byte) (acknowledgment >> 8  /* & 0xFF */);
        this.header[3] = (byte) (acknowledgment       /* & 0xFF */);
    }

    public Rip(DatagramPacket packet) throws PacketSizeException{

        super(packet, HEADER_SIZE);

        // this.latency = (Byte.toUnsignedInt(this.header[0]) << 8) |
        //                 Byte.toUnsignedInt(this.header[1]);

        this.acknowledgment = (Byte.toUnsignedInt(this.header[0]) << 24) |
                              (Byte.toUnsignedInt(this.header[1]) << 16) |
                              (Byte.toUnsignedInt(this.header[2]) << 8)  |
                               Byte.toUnsignedInt(this.header[3]);
    }

    public int getAcknowledgment() {
        return acknowledgment;
    }

}
