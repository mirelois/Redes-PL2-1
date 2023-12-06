package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sup extends Packet { //Streaming over UDP Protocol

    static int HEADER_SIZE = 11;

    int frameNumber; // 4
                           
    int sequence_number; //4
    
    int time_stamp; //2

    int streamId; //1

    public Sup(
            int time_stamp,
            int frameNumber,
            int sequence_number,
            int streamId,
            InetAddress address,
            int port,
            int payload_size,
            byte[] payload
            ) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.frameNumber = frameNumber;

        this.sequence_number = sequence_number;

        this.time_stamp = time_stamp;

        this.streamId = streamId;
        
        // System.err.println(this.time_stamp);

        this.header[0] = (byte) (frameNumber >> 24 /* & 0xFF */);
        this.header[1] = (byte) (frameNumber >> 16 /* & 0xFF */);
        this.header[2] = (byte) (frameNumber >> 8  /* & 0xFF */);
        this.header[3] = (byte) (frameNumber       /* & 0xFF */);
        
        this.header[4] = (byte) (sequence_number >> 24 /* & 0xFF */);
        this.header[5] = (byte) (sequence_number >> 16 /* & 0xFF */);
        this.header[6] = (byte) (sequence_number >> 8  /* & 0xFF */);
        this.header[7] = (byte) (sequence_number       /* & 0xFF */);
        
        this.header[8] = (byte) (time_stamp >> 8  /* & 0xFF */ );
        this.header[9] = (byte) (time_stamp       /* & 0xFF */ );

        this.header[10] = (byte) (this.streamId        /* & 0xFF */);

    }

    public Sup(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException{
        
        super(packet, HEADER_SIZE);
        
        this.frameNumber      = (Byte.toUnsignedInt(this.header[0]) << 24) |
                                (Byte.toUnsignedInt(this.header[1]) << 16) |
                                (Byte.toUnsignedInt(this.header[2]) << 8)  |
                                 Byte.toUnsignedInt(this.header[3]);
        
        this.sequence_number  = (Byte.toUnsignedInt(this.header[4]) << 24) |
                                (Byte.toUnsignedInt(this.header[5]) << 16) |
                                (Byte.toUnsignedInt(this.header[6]) << 8)  |
                                 Byte.toUnsignedInt(this.header[7]);

        this.time_stamp       = (Byte.toUnsignedInt(this.header[8]) << 8) |
                                 Byte.toUnsignedInt(this.header[9]);

        this.streamId         =  Byte.toUnsignedInt(this.header[10]);

    }

	public int getTime_stamp() {
		return time_stamp;
	}

	public int getFrameNumber() {
		return frameNumber;
	}

	public int getSequenceNumber() {
		return sequence_number;
	}

	public int getStreamId() {
		return streamId;
	}
}
