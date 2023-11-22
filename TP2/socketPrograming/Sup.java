import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sup extends Packet {

    static int HEADER_SIZE = 12;

    
    int time_stamp; //2
    int video_time_stamp; // 4 in ms
    int sequence_number; //4
    // int latency; //2
    int checksum; //2
    int streamId; //1

    public Sup(int streamId, int video_time_stamp, int sequence_number, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.sequence_number = sequence_number;
        
        this.video_time_stamp = video_time_stamp;
        
        this.time_stamp = Packet.getCurrTime();
        
        this.checksum = 0; //TODO make good checksum, not bad checksum, only good checksum
        
        this.streamId = streamId;

        // System.err.println(this.time_stamp);

        this.header[0] = (byte) (this.time_stamp >> 8  /* & 0xFF */ );
        this.header[1] = (byte) (this.time_stamp       /* & 0xFF */ );

        this.header[2] = (byte) (video_time_stamp >> 24 /* & 0xFF */);
        this.header[3] = (byte) (video_time_stamp >> 16 /* & 0xFF */);
        this.header[4] = (byte) (video_time_stamp >> 8  /* & 0xFF */);
        this.header[5] = (byte) (video_time_stamp       /* & 0xFF */);
        
        this.header[6] = (byte) (sequence_number >> 24 /* & 0xFF */);
        this.header[7] = (byte) (sequence_number >> 16 /* & 0xFF */);
        this.header[8] = (byte) (sequence_number >> 8  /* & 0xFF */);
        this.header[9] = (byte) (sequence_number       /* & 0xFF */);
        
        this.header[10] = (byte) (this.checksum >> 8   /* & 0xFF */);
        this.header[11] = (byte) (this.checksum        /* & 0xFF */);

        this.header[12] = (byte) (this.streamId        /* & 0xFF */);
    }

    public Sup(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException{
        
        super(packet, HEADER_SIZE);

        this.time_stamp = (Byte.toUnsignedInt(this.header[0]) << 8) |
                           Byte.toUnsignedInt(this.header[1]);

        this.video_time_stamp = (Byte.toUnsignedInt(this.header[2]) << 24) |
                                (Byte.toUnsignedInt(this.header[3]) << 16) |
                                (Byte.toUnsignedInt(this.header[4]) << 8)  |
                                 Byte.toUnsignedInt(this.header[5]);
        
        this.sequence_number = (Byte.toUnsignedInt(this.header[6]) << 24) |
                               (Byte.toUnsignedInt(this.header[7]) << 16) |
                               (Byte.toUnsignedInt(this.header[8]) << 8)  |
                                Byte.toUnsignedInt(this.header[9]);
                        
        this.checksum = (Byte.toUnsignedInt(this.header[10]) << 8)  |
                         Byte.toUnsignedInt(this.header[11]);

        this.streamId = Byte.toUnsignedInt(this.header[12]);

    }

	public int getTime_stamp() {
		return time_stamp;
	}

	public int getSequence_number() {
		return sequence_number;
	}

    public int getVideo_time_stamp(){
        return video_time_stamp;
    }

	public int getChecksum() {
		return checksum;
	}

    public int getStreamId() {
		return checksum;
	}
}
