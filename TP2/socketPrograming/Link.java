import java.net.DatagramPacket;
import java.net.InetAddress;

public class Link extends Packet { //link initiation and negation kontrol (kool e dinamico)
                    
    static int HEADER_SIZE = 2;
                    
    boolean activate;   // \ 
                        //  |-> 1
    boolean deactivate; // /

    int streamId; //1
    
    public Link(boolean activate, boolean deactivate, int streamId, InetAddress address, int port, int payload_size, byte[] payload) {

        super(HEADER_SIZE, payload, payload_size, address, port);

        int flags = 0x00;

        if(deactivate) flags |= 0x01;

        if(activate) flags |= 0x02; 

        this.streamId = streamId;

        this.activate = activate;

        this.deactivate = deactivate;

        this.header[0] = (byte) (flags    /* & 0xFF */);
        this.header[1] = (byte) (streamId /* & 0xFF */);

    }

    public Link(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException{
        
        super(packet, HEADER_SIZE);

        // this.time_stamp = (Byte.toUnsignedInt(this.header[0]) << 8) |
        //                    Byte.toUnsignedInt(this.header[1]);

        int flags = Byte.toUnsignedInt(this.header[0]);

        this.activate   = (flags & 0x02) == 0x02;
        this.deactivate = (flags & 0x01) == 0x01;
        
        int streamId = Byte.toUnsignedInt(this.header[1]);

    }

	public boolean isActivate() {
		return activate;
	}

	public boolean isDeactivate() {
		return deactivate;
	}

	public int getStreamId() {
		return streamId;
	}



}
