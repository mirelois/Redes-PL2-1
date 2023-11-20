import java.net.DatagramPacket;
import java.net.InetAddress;

public class Rip extends Packet { // Response and Idle Protocol
                                  
    static int HEADER_SIZE = 8;

    int latency; // 2
                 
    int acknowledgment; // 4

	int lossRate; // 2 (technicaly only 14 bits are used)
                    
	public Rip(int latency, int lossRate, InetAddress address, int port) {

        super(HEADER_SIZE, null, 0, address, port);

        this.latency = latency;
        this.lossRate = lossRate;

        this.header[0] = (byte) (latency >> 8 /* & 0xFF */);
        this.header[1] = (byte) (latency      /* & 0xFF */);
        
        this.header[2] = (byte) (acknowledgment >> 24 /* & 0xFF */);
        this.header[3] = (byte) (acknowledgment >> 16 /* & 0xFF */);
        this.header[4] = (byte) (acknowledgment >> 8  /* & 0xFF */);
        this.header[5] = (byte) (acknowledgment       /* & 0xFF */);
                                                     
        this.header[6] = (byte) (lossRate >> 8    & 0x3F   );
        this.header[7] = (byte) (lossRate      /* & 0xFF */);
        
    }

    public Rip(DatagramPacket packet){

        super(packet, HEADER_SIZE);

        this.latency = (Byte.toUnsignedInt(this.header[0]) << 8) |
                        Byte.toUnsignedInt(this.header[1]);

        this.acknowledgment = (Byte.toUnsignedInt(this.header[2]) << 24) |
                              (Byte.toUnsignedInt(this.header[3]) << 16) |
                              (Byte.toUnsignedInt(this.header[4]) << 8)  |
                               Byte.toUnsignedInt(this.header[5]);
        
        this.lossRate = (Byte.toUnsignedInt(this.header[6]) << 8) |
                         Byte.toUnsignedInt(this.header[7]);
    }
    
    public int getLatency() {
        return latency;
    }
    
    public int getLossRate() {
        return lossRate;
    }

    public int getAcknowledgment() {
        return acknowledgment;
    }

}
