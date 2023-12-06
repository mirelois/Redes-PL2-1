package Protocols;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Sup extends Packet { //Streaming over UDP Protocol

    static int HEADER_SIZE = 19;

    int lossRate; //2 
    
    int time_stamp; //2
                    
    int video_time_stamp; // 4 in ms
                          
    int frameNumber; // 4
                           
    int sequence_number; //4
    
    int streamId; //1

    int checksum; //2
                  
    public Sup(
            int lossRate,
            int time_stamp,
            int video_time_stamp,
            int frameNumber,
            int sequence_number,
            int streamId,
            InetAddress address,
            int port,
            int payload_size,
            byte[] payload
            ) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        this.lossRate = lossRate;

        this.time_stamp = time_stamp;

        this.video_time_stamp = video_time_stamp;

        this.frameNumber = frameNumber;

        this.sequence_number = sequence_number;

        this.streamId = streamId;
        
        this.checksum = 0; //TODO make good checksum, not bad checksum, only good checksum

        // System.err.println(this.time_stamp);

        this.header[0]  = (byte) (lossRate >> 8  /* & 0xFF */ );
        this.header[1]  = (byte) (lossRate       /* & 0xFF */ );

        this.header[2]  = (byte) (time_stamp >> 8  /* & 0xFF */ );
        this.header[3]  = (byte) (time_stamp       /* & 0xFF */ );

        this.header[4]  = (byte) (video_time_stamp >> 24 /* & 0xFF */);
        this.header[5]  = (byte) (video_time_stamp >> 16 /* & 0xFF */);
        this.header[6]  = (byte) (video_time_stamp >> 8  /* & 0xFF */);
        this.header[7]  = (byte) (video_time_stamp       /* & 0xFF */);
        
        this.header[8]  = (byte) (frameNumber >> 24 /* & 0xFF */);
        this.header[9]  = (byte) (frameNumber >> 16 /* & 0xFF */);
        this.header[10] = (byte) (frameNumber >> 8  /* & 0xFF */);
        this.header[11] = (byte) (frameNumber       /* & 0xFF */);
        
        this.header[12] = (byte) (sequence_number >> 24 /* & 0xFF */);
        this.header[13] = (byte) (sequence_number >> 16 /* & 0xFF */);
        this.header[14] = (byte) (sequence_number >> 8  /* & 0xFF */);
        this.header[15] = (byte) (sequence_number       /* & 0xFF */);
        
        this.header[16] = (byte) (this.streamId        /* & 0xFF */);
        
        this.header[17] = (byte) (this.checksum >> 8   /* & 0xFF */);
        this.header[18] = (byte) (this.checksum        /* & 0xFF */);

    }

    public Sup(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException{
        
        super(packet, HEADER_SIZE);
        
        this.lossRate         = (Byte.toUnsignedInt(this.header[0]) << 8) |
                                 Byte.toUnsignedInt(this.header[1]);

        this.time_stamp       = (Byte.toUnsignedInt(this.header[2]) << 8) |
                                 Byte.toUnsignedInt(this.header[3]);

        this.video_time_stamp = (Byte.toUnsignedInt(this.header[4]) << 24) |
                                (Byte.toUnsignedInt(this.header[5]) << 16) |
                                (Byte.toUnsignedInt(this.header[6]) << 8)  |
                                 Byte.toUnsignedInt(this.header[7]);
        
        this.frameNumber      = (Byte.toUnsignedInt(this.header[8]) << 24) |
                                (Byte.toUnsignedInt(this.header[9]) << 16) |
                                (Byte.toUnsignedInt(this.header[10]) << 8)  |
                                 Byte.toUnsignedInt(this.header[11]);
        
        this.sequence_number  = (Byte.toUnsignedInt(this.header[12]) << 24) |
                                (Byte.toUnsignedInt(this.header[13]) << 16) |
                                (Byte.toUnsignedInt(this.header[14]) << 8)  |
                                 Byte.toUnsignedInt(this.header[15]);

        this.streamId         =  Byte.toUnsignedInt(this.header[16]);
                        
        this.checksum         = (Byte.toUnsignedInt(this.header[17]) << 8)  |
                                 Byte.toUnsignedInt(this.header[18]);

    }

	public int getLossRate() {
		return lossRate;
	}

	public int getTime_stamp() {
		return time_stamp;
	}

	public int getVideo_time_stamp() {
		return video_time_stamp;
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

	public int getChecksum() {
		return checksum;
	}
}
