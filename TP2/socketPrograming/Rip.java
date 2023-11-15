import java.net.DatagramPacket;
import java.net.InetAddress;

public class Rip extends Packet { // Responce information protocol
                                  
    static int HEADER_SIZE = 4;

    int latency; // 2

	int throughput; // 2 (technicaly only 14 bits are used)
                    
	public Rip(int latency, int throughput, InetAddress address, int port) {

        super(HEADER_SIZE, null, 0, address, port);

        this.latency = latency;
        this.throughput = throughput;

        this.header[0] = (byte) (latency >> 8 /* & 0xFF */);
        this.header[1] = (byte) (latency      /* & 0xFF */);
        
        this.header[2] = (byte) (throughput >> 8 /* & 0xFF */);
        this.header[3] = (byte) (throughput      /* & 0xFF */);
        
    }

    public Rip(DatagramPacket packet){

        super(packet, HEADER_SIZE);

        this.latency = (this.header[0] << 8) | this.header[1];
        
        this.throughput = (this.header[2] << 8) | this.header[3];
    }
    
    public int getLatency() {
        return latency;
    }
    
    public int getThroughput() {
        return throughput;
    }
}
