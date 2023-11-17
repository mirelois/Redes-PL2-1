import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sup extends Packet {

    static int HEADER_SIZE = 8;

    long time_stamp; //2
    int sequence_number; //4
    // int latency; //2
    int checksum; //2

    public Sup(int sequence_number, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.sequence_number = sequence_number;


        this.time_stamp = Packet.getCurrTime();

        this.checksum = 0; //TODO make good checksum, not bad checksum, only good checksum

        // System.err.println(this.time_stamp);

        this.header[0] = (byte) (this.time_stamp >> 8  & 0xFF );
        this.header[1] = (byte) (this.time_stamp       & 0xFF );

        // System.err.printf("%02X", this.header[0]);
        // System.err.printf("%02X\n", this.header[1]);
        
        this.header[2] = (byte) (sequence_number >> 24 /* & 0xFF */);
        this.header[3] = (byte) (sequence_number >> 16 /* & 0xFF */);
        this.header[4] = (byte) (sequence_number >> 8  /* & 0xFF */);
        this.header[5] = (byte) (sequence_number       /* & 0xFF */);
        
        this.header[6] = (byte) (this.checksum >> 8   /* & 0xFF */);
        this.header[7] = (byte) (this.checksum        /* & 0xFF */);

    }

    public Sup(DatagramPacket packet) throws java.net.UnknownHostException{
        
        super(packet, HEADER_SIZE);

        this.time_stamp = 
                        (Byte.toUnsignedInt(this.header[0]) << 8) |
                         Byte.toUnsignedInt(this.header[1]);
        
        this.sequence_number = 
                        (Byte.toUnsignedInt(this.header[2]) << 24) |
                        (Byte.toUnsignedInt(this.header[3]) << 16) |
                        (Byte.toUnsignedInt(this.header[4]) << 8)  |
                         Byte.toUnsignedInt(this.header[5]);
                        
        this.checksum = 
                        (Byte.toUnsignedInt(this.header[6]) << 8)  |
                         Byte.toUnsignedInt(this.header[7]);

    }

	public long getTime_stamp() {
		return time_stamp;
	}

	public int getSequence_number() {
		return sequence_number;
	}

	public int getChecksum() {
		return checksum;
	}
}
