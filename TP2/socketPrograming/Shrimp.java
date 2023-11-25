import java.net.DatagramPacket;
import java.net.InetAddress;

public class Shrimp extends Packet{ //Stream Hard Response Initiation Management Protocol

    static int HEADER_SIZE = 7;

    //Se estiver a 0, a stream não existe
    int streamId; //1
                  
    InetAddress sourceAddress; //4
                               
    int timeStamp; //2;

	public Shrimp(int timeStamp, InetAddress sourceAddress, int streamId, int port, InetAddress address, int payload_size, byte[] payload) {
        super(HEADER_SIZE, payload, payload_size, address, port);

        this.timeStamp = timeStamp;

        this.streamId = streamId;

        this.sourceAddress = sourceAddress;
        
        String[] ip_values = sourceAddress.getHostAddress().split("\\.", 4);

        this.header[0] = (byte) (this.streamId /* & 0xFF */);
        
        this.header[1] = Byte.parseByte(ip_values[0]);
        this.header[2] = Byte.parseByte(ip_values[1]);
        this.header[3] = Byte.parseByte(ip_values[2]);
        this.header[4] = Byte.parseByte(ip_values[3]);

        this.header[5] = (byte) (this.time_stamp >> 8 /* & 0xFF */);
        this.header[6] = (byte) (this.time_stamp      /* & 0xFF */);
    }

    public Shrimp(DatagramPacket packet) throws java.net.UnknownHostException, PacketSizeException {
        
        super(packet, HEADER_SIZE);

        this.streamId = Byte.toUnsignedInt(this.header[0]);

        StringBuilder ip = new StringBuilder(15);

        ip.append(this.header[1]);
        ip.append('.');
        ip.append(this.header[2]);
        ip.append('.');
        ip.append(this.header[3]);
        ip.append('.');
        ip.append(this.header[4]);

        this.sourceAddress = InetAddress.getByName(ip.toString());
        
        this.checksum = (Byte.toUnsignedInt(this.header[5]) << 8) |
                         Byte.toUnsignedInt(this.header[6]);

    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public int getStreamId() {
		return streamId;
	}

	public InetAddress getSourceAddress() {
		return sourceAddress;
	}

}
