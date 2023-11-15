import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.LocalTime;

public class Sup extends Packet {

    static int HEADER_SIZE = 14;

    int time_stamp; //2
    int sequence_number; //4
    int acknowledgment_number; //4
    // int latency; //2
    int checksum; //4

    public Sup(int sequence_number, int acknowledgment_number, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.sequence_number = sequence_number;

        this.acknowledgment_number = acknowledgment_number;

        this.time_stamp = Packet.getCurrTime();

        this.checksum = 0; //TODO make good checksum, not bad checksum, only good checksum

        this.header[0] = (byte) (this.time_stamp >> 8 /* & 0xFF */);
        this.header[1] = (byte) (this.time_stamp      /* & 0xFF */);
        
        this.header[2] = (byte) (sequence_number >> 24 /* & 0xFF */);
        this.header[3] = (byte) (sequence_number >> 16 /* & 0xFF */);
        this.header[4] = (byte) (sequence_number >> 8  /* & 0xFF */);
        this.header[5] = (byte) (sequence_number       /* & 0xFF */);
        
        this.header[6] = (byte) (acknowledgment_number >> 24 /* & 0xFF */);
        this.header[7] = (byte) (acknowledgment_number >> 16 /* & 0xFF */);
        this.header[8] = (byte) (acknowledgment_number >> 8  /* & 0xFF */);
        this.header[9] = (byte) (acknowledgment_number       /* & 0xFF */);
        
        this.header[10] = (byte) (this.checksum >> 24 /* & 0xFF */);
        this.header[11] = (byte) (this.checksum >> 16 /* & 0xFF */);
        this.header[12] = (byte) (this.checksum >> 8  /* & 0xFF */);
        this.header[13] = (byte) (this.checksum       /* & 0xFF */);

    }

    public Sup(DatagramPacket packet) throws java.net.UnknownHostException{
        
        super(packet, HEADER_SIZE);

        this.time_stamp = 
                        (this.header[0] << 8)  |
                         this.header[1];

        this.sequence_number = 
                        (this.header[2] << 24) |
                        (this.header[3] << 16) |
                        (this.header[4] << 8)  |
                         this.header[5];
                        
        this.acknowledgment_number = 
                        (this.header[6] << 24) |
                        (this.header[7] << 16) |
                        (this.header[8] << 8)  |
                         this.header[9];

        this.checksum = 
                        (this.header[10] << 24) |
                        (this.header[11] << 16) |
                        (this.header[12] << 8)  |
                         this.header[13];

    }

	public int getTime_stamp() {
		return time_stamp;
	}

	public int getSequence_number() {
		return sequence_number;
	}

	public int getAcknowledgment_number() {
		return acknowledgment_number;
	}

	public int getChecksum() {
		return checksum;
	}
}
